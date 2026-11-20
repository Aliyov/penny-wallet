package com.penny.wallet.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.prefs.SettingsStore
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.data.repo.ChangeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

/** Serialize, write via SAF, prune, restore (SPEC §6). Fully local. */
class BackupManager(
    private val context: Context,
    private val db: PennyDatabase,
    private val settings: SettingsStore,
    private val listener: ChangeListener,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true; coerceInputValues = true }

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

    fun encode(b: Backup): String = json.encodeToString(Backup.serializer(), b)

    /** Persist access to the folder the user picked with ACTION_OPEN_DOCUMENT_TREE. */
    suspend fun setFolder(tree: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(tree, flags) }
        // release the previous folder's grant
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
        val tree = p.backupFolderUri?.let(Uri::parse) ?: return@withContext BackupResult.Failed("Pick a backup folder first")
        if (!hasFolderAccess(p)) return@withContext BackupResult.Failed("Lost access to the backup folder. Pick it again.")
        val dir = DocumentFile.fromTreeUri(context, tree) ?: return@withContext BackupResult.Failed("Folder not found")
        runCatching {
            val data = encode(snapshot()).toByteArray()
            val name = fileName()
            dir.findFile(name)?.delete()
            val file = dir.createFile("application/json", name.removeSuffix(".json"))
                ?: error("Couldn't create the file")
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { it.write(data) } ?: error("Couldn't write the file")
            prune(dir, p.backupKeep)
            val now = System.currentTimeMillis()
            settings.update { it.copy(lastBackupAt = now) }
            BackupResult.Ok(file.name ?: name, now)
        }.getOrElse { BackupResult.Failed(it.message ?: "Backup failed") }
    }

    /** Keep the newest [keep] backups (names sort by date), delete older ones. Never touches other files. */
    private fun prune(dir: DocumentFile, keep: Int) {
        dir.listFiles().filter { it.isFile && it.name?.startsWith(PREFIX) == true && it.name?.endsWith(".json") == true }
            .sortedByDescending { it.name }
            .drop(keep.coerceAtLeast(1))
            .forEach { it.delete() }
    }

    /** Reads and validates a backup file chosen with ACTION_OPEN_DOCUMENT. Throws with a readable message. */
    suspend fun read(uri: Uri): Pair<Backup, BackupSummary> = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: throw IllegalArgumentException("Couldn't open the file")
        val b = runCatching { json.decodeFromString(Backup.serializer(), text) }
            .getOrElse { throw IllegalArgumentException("This isn't a Penny Wallet backup") }
        require(b.schemaVersion in 1..Backup.CURRENT_SCHEMA) {
            "Backup version ${b.schemaVersion} is newer than this app understands (${Backup.CURRENT_SCHEMA})"
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
        val csv = Csv.transactions(b)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(csv.toByteArray()) } ?: error("Couldn't write the file")
    }
}
