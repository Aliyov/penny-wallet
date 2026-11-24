package com.penny.wallet.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.penny.wallet.R
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.prefs.SettingsStore
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.data.repo.ChangeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.LocalDate

class PasswordNeededException : Exception()

class BackupManager(
    private val context: Context,
    private val db: PennyDatabase,
    private val settings: SettingsStore,
    private val listener: ChangeListener,
) {
    val password = BackupPasswordStore(context)

    companion object {
        const val PREFIX = "penny-wallet-backup-"
        fun fileName(date: LocalDate = LocalDate.now()) = "$PREFIX$date.json"
    }

    suspend fun snapshot(): Backup = db.withTransaction {
        Backup(
            schemaVersion = Backup.CURRENT_SCHEMA,
            exportedAt = Instant.now().toString(),
            accounts = db.accounts().getAll(),
            categories = db.categories().getAll(),
            goals = db.goals().getAll(),
            transactions = db.transactions().getAll(),
            recurring = db.recurring().getAll(),
            settings = settings.current(),
        )
    }

    fun encode(b: Backup): String = BackupCodec.encode(b)

    /** Persist access to the folder the user picked with ACTION_OPEN_DOCUMENT_TREE. */
    suspend fun setFolder(tree: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(tree, flags) }
        settings.current().backupFolderUri?.let(Uri::parse)?.takeIf { it != tree }?.let { old ->
            runCatching { context.contentResolver.releasePersistableUriPermission(old, flags) }
        }
        settings.update { it.copy(backupFolderUri = tree.toString()) }
    }

    fun hasFolderAccess(p: UserPrefs): Boolean {
        val uri = p.backupFolderUri?.let(Uri::parse) ?: return false
        val granted = context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isWritePermission }
        return granted && DocumentFile.fromTreeUri(context, uri)?.canWrite() == true
    }

    /** "Back up now" and the scheduled worker share this path. Same-day file is replaced; keeps the newest N. */
    suspend fun backupNow(): BackupResult = withContext(Dispatchers.IO) {
        val p = settings.current()
        val tree = p.backupFolderUri?.let(Uri::parse) ?: return@withContext BackupResult.Failed(context.getString(R.string.backup_err_no_folder))
        if (!hasFolderAccess(p)) return@withContext BackupResult.Failed(context.getString(R.string.backup_err_lost_access))
        val dir = DocumentFile.fromTreeUri(context, tree) ?: return@withContext BackupResult.Failed(context.getString(R.string.backup_err_folder_not_found))
        runCatching {
            val plain = encode(snapshot())
            val data = if (password.isSet) {
                val pw = password.get() ?: error(context.getString(R.string.backup_err_password_lost))
                BackupCrypto.encrypt(plain, pw).also { pw.fill(' ') }
            } else {
                plain
            }.toByteArray()
            val name = fileName()
            dir.findFile(name)?.delete()
            val file = dir.createFile("application/json", name.removeSuffix(".json"))
                ?: error(context.getString(R.string.backup_err_create_file))
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { it.write(data) } ?: error(context.getString(R.string.backup_err_write_file))
            prune(dir, p.backupKeep)
            val now = System.currentTimeMillis()
            settings.update { it.copy(lastBackupAt = now) }
            BackupResult.Ok(file.name ?: name, now)
        }.getOrElse { BackupResult.Failed(it.message ?: context.getString(R.string.backup_err_failed)) }
    }

    /** Keep the newest [keep] backups (names sort by date), delete older ones. Never touches other files. */
    private fun prune(dir: DocumentFile, keep: Int) {
        dir.listFiles().filter { it.isFile && it.name?.startsWith(PREFIX) == true && it.name?.endsWith(".json") == true }
            .sortedByDescending { it.name }
            .drop(keep.coerceAtLeast(1))
            .forEach { it.delete() }
    }

    /** Reads and validates a backup file chosen with ACTION_OPEN_DOCUMENT. Throws [PasswordNeededException] for a
     *  password-protected file when [pw] is null, otherwise an exception with a readable message. */
    suspend fun read(uri: Uri, pw: CharArray? = null): Pair<Backup, BackupSummary> = withContext(Dispatchers.IO) {
        val raw = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } ?: throw IllegalArgumentException(context.getString(R.string.backup_err_open_file))
        val b = try {
            BackupCodec.decode(raw, pw)
        } catch (e: BackupProblem) {
            throw when (e) {
                is BackupProblem.PasswordNeeded -> PasswordNeededException()
                is BackupProblem.WrongPassword -> IllegalArgumentException(context.getString(R.string.backup_err_wrong_password))
                is BackupProblem.TooNew -> IllegalArgumentException(context.getString(R.string.backup_err_too_new, e.version, Backup.CURRENT_SCHEMA))
                is BackupProblem.NotABackup -> IllegalArgumentException(context.getString(R.string.backup_err_not_backup))
            }
        }
        b to summarize(b)
    }

    fun summarize(b: Backup): BackupSummary {
        val acc = b.accounts.map { it.id }.toSet() + MoneyCalculator.EXTERNAL
        val cat = b.categories.map { it.id }.toSet()
        val goal = b.goals.map { it.id }.toSet()
        val dangling = b.transactions.count { t ->
            t.accountId !in acc || (t.toAccountId != null && t.toAccountId !in acc) ||
                (t.categoryId != null && t.categoryId !in cat) || (t.goalId != null && t.goalId !in goal)
        } + b.recurring.count { it.accountId !in acc || it.categoryId !in cat }
        val dates = b.transactions.map { it.date }
        return BackupSummary(b.exportedAt, b.schemaVersion, b.accounts.size, b.categories.size, b.goals.size,
            b.transactions.size, b.recurring.size, dates.minOrNull()?.toString(), dates.maxOrNull()?.toString(), dangling)
    }

    /**
     * Replaces ALL data inside one Room transaction. Settings are restored too, except device-specific ones
     * (backup folder, last backup time, permission flag) which stay as they are on this phone.
     */
    suspend fun restore(b: Backup) {
        db.withTransaction {
            db.transactions().clear(); db.recurring().clear(); db.goals().clear(); db.categories().clear(); db.accounts().clear()
            db.accounts().upsertAll(b.accounts)
            db.categories().upsertAll(b.categories)
            db.goals().upsertAll(b.goals)
            db.transactions().upsertAll(b.transactions)
            db.recurring().upsertAll(b.recurring)
        }
        b.settings?.let { restored ->
            settings.update { cur ->
                restored.copy(
                    currencyCode = restored.currencyCode.ifEmpty { Formatters.DEFAULT_CURRENCY },
                    backupFolderUri = cur.backupFolderUri, lastBackupAt = cur.lastBackupAt,
                    notificationPermissionAsked = cur.notificationPermissionAsked, seeded = true,
                )
            }
        }
        listener.onDataChanged()
    }

    /** Wipes everything (Settings → Reset all data). Settings are kept; seed is not re-run. */
    suspend fun resetAll() {
        db.withTransaction {
            db.transactions().clear(); db.recurring().clear(); db.goals().clear(); db.categories().clear(); db.accounts().clear()
        }
        settings.update { it.copy(lastAccountId = null, lastCategoryId = null, fixedAccountId = null) }
        listener.onDataChanged()
    }

    /** "Export transactions as CSV" to a file the user created with ACTION_CREATE_DOCUMENT. */
    suspend fun exportCsv(uri: Uri) = withContext(Dispatchers.IO) {
        val b = snapshot()
        val csv = Csv.transactions(b, context.getString(R.string.data_csv_outside))
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(csv.toByteArray()) } ?: error(context.getString(R.string.backup_err_write_file))
    }
}
