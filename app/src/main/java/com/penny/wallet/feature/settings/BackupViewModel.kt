package com.penny.wallet.feature.settings

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
    /** Just finished "Back up now" → show ✓ + file name. */
    val justBackedUp: String? = null,
    val error: String? = null,
    /** Parsed file waiting for the double confirm. */
    val pendingRestore: Pair<Backup, BackupSummary>? = null,
)

class BackupViewModel(private val c: AppContainer) : ViewModel() {
    private val transient = MutableStateFlow(BackupUi())

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

    /** Step 1 of restore: read + validate, then the UI shows the summary and asks twice. */
    fun onRestoreFilePicked(uri: Uri?) = viewModelScope.launch {
        uri ?: return@launch
        transient.update { it.copy(busy = true, error = null) }
        val r = runCatching { c.backup.read(uri) }
        transient.update { r.fold({ ok -> it.copy(busy = false, pendingRestore = ok) }, { e -> it.copy(busy = false, error = e.message) }) }
    }

    fun cancelRestore() = transient.update { it.copy(pendingRestore = null) }

    /** Step 2 (after the second confirmation): replace everything in one Room transaction. */
    fun confirmRestore(done: (String?) -> Unit) = viewModelScope.launch {
        val b = transient.value.pendingRestore?.first ?: return@launch
        transient.update { it.copy(busy = true) }
        val err = runCatching { c.backup.restore(b) }.exceptionOrNull()?.message
        transient.update { it.copy(busy = false, pendingRestore = null, error = err) }
        done(err)
    }

    fun exportCsv(uri: Uri?, done: (String?) -> Unit) = viewModelScope.launch {
        uri ?: return@launch
        done(runCatching { c.backup.exportCsv(uri) }.exceptionOrNull()?.message)
    }

    fun clearMessages() = transient.update { it.copy(error = null, justBackedUp = null) }

    companion object { val MIME_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream") }
}

