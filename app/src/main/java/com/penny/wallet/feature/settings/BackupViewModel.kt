package com.penny.wallet.feature.settings

import com.penny.wallet.data.backup.PasswordNeededException
import com.penny.wallet.core.ui.userMessage
import com.penny.wallet.core.ui.moneyText
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.data.backup.Backup
import com.penny.wallet.data.backup.BackupResult
import com.penny.wallet.data.backup.BackupSummary
import com.penny.wallet.data.prefs.BackupFrequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BackupUi(
    val folderUri: String? = null,
    val folderOk: Boolean = false,
    val lastBackupAt: Long? = null,
    val frequency: BackupFrequency = BackupFrequency.WEEKLY,
    val keep: Int = 8,
    val busy: Boolean = false,
    val justBackedUp: String? = null,
    val error: String? = null,
    val pendingRestore: Pair<Backup, BackupSummary>? = null,
    val passwordOn: Boolean = false,
    /** A password-protected file waiting for its password. */
    val restoreNeedsPassword: Uri? = null,
)

class BackupViewModel(private val c: AppContainer) : ViewModel() {
    private val transient = MutableStateFlow(BackupUi(passwordOn = c.backup.password.isSet))
    /** Password that opened the pending restore; kept on the phone afterwards if none is set yet. */
    private var restorePassword: CharArray? = null

    val state: StateFlow<BackupUi> = combine(c.settings.prefs, transient) { p, t ->
        t.copy(folderUri = p.backupFolderUri, folderOk = c.backup.hasFolderAccess(p), lastBackupAt = p.lastBackupAt,
            frequency = p.backupFrequency, keep = p.backupKeep)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUi())

    val csvFileName: String get() = "penny-wallet-transactions-${LocalDate.now()}.csv"

    fun onFolderPicked(uri: Uri?) = viewModelScope.launch {
        uri ?: return@launch
        c.backup.setFolder(uri)
        backupNow() // first backup right away, so the folder is proven writable
    }

    fun backupNow() = viewModelScope.launch {
        transient.update { it.copy(busy = true, error = null, justBackedUp = null) }
        val r = c.backup.backupNow()
        transient.update {
            when (r) {
                is BackupResult.Ok -> it.copy(busy = false, justBackedUp = r.fileName)
                is BackupResult.Failed -> it.copy(busy = false, error = r.reason)
            }
        }
    }

    fun setFrequency(f: BackupFrequency) = viewModelScope.launch { c.settings.update { it.copy(backupFrequency = f) } }
    fun setKeep(n: Int) = viewModelScope.launch { c.settings.update { it.copy(backupKeep = n.coerceIn(1, 60)) } }

    fun onRestoreFilePicked(uri: Uri?) = openRestore(uri, null)

    fun submitRestorePassword(pw: CharArray) = openRestore(transient.value.restoreNeedsPassword, pw)

    private fun openRestore(uri: Uri?, pw: CharArray?) = viewModelScope.launch {
        uri ?: return@launch
        transient.update { it.copy(busy = true, error = null) }
        val r = runCatching { c.backup.read(uri, pw) }
        restorePassword = pw.takeIf { r.isSuccess }
        transient.update {
            r.fold(
                { ok -> it.copy(busy = false, pendingRestore = ok, restoreNeedsPassword = null) },
                { e ->
                    if (e is PasswordNeededException) it.copy(busy = false, restoreNeedsPassword = uri)
                    else it.copy(busy = false, error = c.userMessage(e), restoreNeedsPassword = if (pw != null) uri else null)
                },
            )
        }
    }

    fun cancelRestore() {
        restorePassword = null
        transient.update { it.copy(pendingRestore = null, restoreNeedsPassword = null) }
    }

    fun setPassword(pw: CharArray) = viewModelScope.launch {
        c.backup.password.set(pw)
        pw.fill(' ')
        transient.update { it.copy(passwordOn = true) }
        if (state.value.folderUri != null) backupNow()
    }

    fun removePassword() {
        c.backup.password.clear()
        transient.update { it.copy(passwordOn = false) }
    }

    fun confirmRestore(done: (String?) -> Unit) = viewModelScope.launch {
        val b = transient.value.pendingRestore?.first ?: return@launch
        transient.update { it.copy(busy = true) }
        val err = runCatching { c.backup.restore(b) }.exceptionOrNull()?.let(c::userMessage)
        restorePassword?.let { pw ->
            if (err == null && !c.backup.password.isSet) {
                c.backup.password.set(pw)
                transient.update { it.copy(passwordOn = true) }
            }
            pw.fill(' ')
        }
        restorePassword = null
        transient.update { it.copy(busy = false, pendingRestore = null, error = err) }
        done(err)
    }

    fun exportCsv(uri: Uri?, done: (String?) -> Unit) = viewModelScope.launch {
        uri ?: return@launch
        done(runCatching { c.backup.exportCsv(uri) }.exceptionOrNull()?.let(c::userMessage))
    }

    fun clearMessages() = transient.update { it.copy(error = null, justBackedUp = null) }

    companion object { val MIME_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream") }
}

