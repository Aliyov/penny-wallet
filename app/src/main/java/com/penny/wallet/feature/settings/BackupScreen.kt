package com.penny.wallet.feature.settings

import androidx.compose.ui.platform.LocalResources
import com.penny.wallet.core.util.dateText
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.data.prefs.BackupFrequency
import com.penny.wallet.data.prefs.LastImport
import com.penny.wallet.core.ui.components.NavRow
import java.time.Instant
import java.time.ZoneId
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.feature.home.ConfirmDialog
import com.penny.wallet.core.ui.components.ButtonKind
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.components.SegmentedControl
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny

@Composable
fun BackupScreen(onBack: () -> Unit, onOpenImport: () -> Unit) {
    val vm = pennyViewModel { BackupViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val c = Penny.colors
    val res = LocalResources.current
    var secondConfirm by remember { mutableStateOf(false) }
    var confirmUndoImport by remember { mutableStateOf(false) }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { vm.onFolderPicked(it) }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { vm.onRestoreFilePicked(it) }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        vm.exportCsv(uri) { err -> snack.show(err?.let { res.getString(R.string.backup_export_failed, it) } ?: res.getString(R.string.backup_export_done)) }
    }

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        SettingsTopBar(stringResource(R.string.settings_backup), onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Penny.dims.gutter).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
        ) {
            StatusCard(s)
            Text(
                stringResource(R.string.backup_tip),
                style = Penny.type.caption, color = c.textSecondary,
            )
            s.error?.takeIf { s.restoreNeedsPassword == null }?.let { Text(it, style = Penny.type.label, color = c.danger) }

            PennyButton(stringResource(if (s.folderUri == null) R.string.backup_pick_folder else R.string.backup_change_folder), { pickFolder.launch(null) }, Modifier.fillMaxWidth(),
                kind = if (s.folderUri == null) ButtonKind.PRIMARY else ButtonKind.SECONDARY)
            if (s.folderUri != null) BackupNowButton(s, vm::backupNow)

            SectionLabel(stringResource(R.string.backup_automatic), Modifier.padding(top = 8.dp))
            val freqs = BackupFrequency.entries
            SegmentedControl(listOf(stringResource(R.string.backup_daily), stringResource(R.string.backup_weekly), stringResource(R.string.backup_monthly)), freqs.indexOf(s.frequency).coerceAtLeast(0), { vm.setFrequency(freqs[it]) })
            KeepStepper(s.keep, vm::setKeep)
            PasswordCard(s.passwordOn, vm::setPassword, vm::removePassword)

            SectionLabel(stringResource(R.string.backup_restore_export), Modifier.padding(top = 8.dp))
            PennyButton(stringResource(R.string.backup_restore_file), { pickRestore.launch(BackupViewModel.MIME_TYPES) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, enabled = !s.busy)
            PennyButton(stringResource(R.string.backup_export_csv), { createCsv.launch(vm.csvFileName) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)

            SectionLabel(stringResource(R.string.import_section), Modifier.padding(top = 8.dp))
            ImportCard(s.lastImport, onOpenImport) { confirmUndoImport = true }
            Spacer(Modifier.height(32.dp))
        }
    }

    val last = s.lastImport
    if (confirmUndoImport && last != null) {
        ConfirmDialog(
            title = stringResource(R.string.import_undo_confirm_title),
            text = pluralStringResource(R.plurals.import_undo_confirm_body, last.count, last.count),
            confirm = stringResource(R.string.import_undo_confirm), danger = true,
            onDismiss = { confirmUndoImport = false },
        ) {
            confirmUndoImport = false
            vm.undoLastImport { n -> snack.show(res.getQuantityString(R.plurals.import_undone, n, n)) }
        }
    }

    s.restoreNeedsPassword?.let {
        RestorePasswordDialog(s.error, s.busy, onDismiss = vm::cancelRestore, onSubmit = vm::submitRestorePassword)
    }

    val pending = s.pendingRestore
    if (pending != null && !secondConfirm) {
        RestoreSummarySheet(pending.second, onDismiss = vm::cancelRestore) { secondConfirm = true }
    }
    if (pending != null && secondConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.backup_replace_title), text = stringResource(R.string.backup_replace_body),
            confirm = stringResource(R.string.backup_replace), danger = true,
            onDismiss = { secondConfirm = false; vm.cancelRestore() },
        ) {
            secondConfirm = false
            vm.confirmRestore { err -> snack.show(err?.let { res.getString(R.string.backup_restore_failed, it) } ?: res.getString(R.string.backup_restore_done)) }
        }
    }
}

@Composable
private fun ImportCard(last: LastImport?, onOpenImport: () -> Unit, onUndo: () -> Unit) {
    PennyCard(Modifier.fillMaxWidth(), padding = 12.dp) {
        NavRow(stringResource(R.string.import_open), null, onOpenImport, subtitle = stringResource(R.string.import_open_body))
        if (last != null) {
            val day = Instant.ofEpochMilli(last.at).atZone(ZoneId.systemDefault()).toLocalDate()
            NavRow(
                stringResource(R.string.import_undo_last), null, onUndo,
                subtitle = pluralStringResource(R.plurals.import_undo_last_body, last.count, last.count, dateText().shortDate(day)),
            )
        }
    }
}

@Composable
private fun StatusCard(s: BackupUi) {
    val c = Penny.colors
    PennyCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Folder, null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(folderName(s.folderUri) ?: stringResource(R.string.backup_no_folder), style = Penny.type.bodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f), maxLines = 1)
            if (s.folderUri != null) {
                val ok = s.folderOk
                Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, stringResource(if (ok) R.string.backup_access_ok else R.string.backup_access_lost),
                    tint = if (ok) c.accent else c.warning, modifier = Modifier.size(20.dp))
            }
        }
        if (s.folderUri != null && !s.folderOk) {
            Text(stringResource(R.string.backup_access_lost_body), style = Penny.type.caption, color = c.warning, modifier = Modifier.padding(top = 6.dp))
        }
        Text(
            stringResource(R.string.backup_last, s.lastBackupAt?.let { dateText().relativeDays(it) } ?: stringResource(R.string.backup_never)),
            style = Penny.type.label, color = c.textSecondary, modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun BackupNowButton(s: BackupUi, onClick: () -> Unit) {
    val c = Penny.colors
    if (s.busy) {
        Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.backup_working), style = Penny.type.label, color = c.textSecondary)
        }
    } else {
        PennyButton(stringResource(R.string.backup_now), onClick, Modifier.fillMaxWidth())
    }
    s.justBackedUp?.let { name ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = c.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.backup_saved, name), style = Penny.type.caption, color = c.accent)
        }
    }
}

@Composable
private fun KeepStepper(keep: Int, onSet: (Int) -> Unit) {
    val c = Penny.colors
    PennyCard(Modifier.fillMaxWidth(), padding = 8.dp) {
        Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.backup_keep_last), style = Penny.type.body, color = c.textPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = { onSet((keep - 1).coerceIn(1, 30)) }, enabled = keep > 1) {
                Icon(Icons.Rounded.Remove, stringResource(R.string.backup_fewer), tint = if (keep > 1) c.accent else c.textSecondary)
            }
            Text(pluralStringResource(R.plurals.backup_keep_files, keep, keep), style = Penny.type.bodyStrong, color = c.textPrimary)
            IconButton(onClick = { onSet((keep + 1).coerceIn(1, 30)) }, enabled = keep < 30) {
                Icon(Icons.Rounded.Add, stringResource(R.string.backup_more), tint = if (keep < 30) c.accent else c.textSecondary)
            }
        }
    }
}

/** "content://…/tree/primary%3ADocuments%2FPenny" → "Penny". */
private fun folderName(uri: String?): String? {
    uri ?: return null
    val seg = runCatching { Uri.parse(uri).lastPathSegment }.getOrNull() ?: return uri
    return seg.substringAfterLast('/').substringAfterLast(':').ifBlank { seg }
}
