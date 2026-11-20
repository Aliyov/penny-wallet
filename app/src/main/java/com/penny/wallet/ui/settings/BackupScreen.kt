package com.penny.wallet.ui.settings

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.data.prefs.BackupFrequency
import com.penny.wallet.domain.Formatters
import com.penny.wallet.ui.account.ConfirmDialog
import com.penny.wallet.ui.components.ButtonKind
import com.penny.wallet.ui.components.LocalSnack
import com.penny.wallet.ui.components.PennyButton
import com.penny.wallet.ui.components.PennyCard
import com.penny.wallet.ui.components.SectionLabel
import com.penny.wallet.ui.components.SegmentedControl
import com.penny.wallet.ui.pennyViewModel
import com.penny.wallet.ui.theme.Penny

/** Settings → Backup (SPEC §6). */
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val vm = pennyViewModel { BackupViewModel(it) }
    val s by vm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val c = Penny.colors
    var secondConfirm by remember { mutableStateOf(false) }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { vm.onFolderPicked(it) }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { vm.onRestoreFilePicked(it) }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        vm.exportCsv(uri) { err -> snack.show(err?.let { "Export failed: $it" } ?: "CSV exported ✓") }
    }

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        SettingsTopBar("Backup", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Penny.dims.gutter).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
        ) {
            StatusCard(s)
            Text(
                "Tip: pick a folder that your Google Drive or Files app syncs if you want an off-phone copy.",
                style = Penny.type.caption, color = c.textSecondary,
            )
            s.error?.let { Text(it, style = Penny.type.label, color = c.danger) }

            PennyButton(if (s.folderUri == null) "Pick folder" else "Change folder", { pickFolder.launch(null) }, Modifier.fillMaxWidth(),
                kind = if (s.folderUri == null) ButtonKind.PRIMARY else ButtonKind.SECONDARY)
            if (s.folderUri != null) BackupNowButton(s, vm::backupNow)

            SectionLabel("Automatic backup", Modifier.padding(top = 8.dp))
            val freqs = BackupFrequency.entries
            SegmentedControl(listOf("Daily", "Weekly", "Monthly"), freqs.indexOf(s.frequency).coerceAtLeast(0), { vm.setFrequency(freqs[it]) })
            KeepStepper(s.keep, vm::setKeep)

            SectionLabel("Restore & export", Modifier.padding(top = 8.dp))
            PennyButton("Restore from file", { pickRestore.launch(BackupViewModel.MIME_TYPES) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY, enabled = !s.busy)
            PennyButton("Export transactions as CSV", { createCsv.launch(vm.csvFileName) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY)
            Spacer(Modifier.height(32.dp))
        }
    }

    val pending = s.pendingRestore
    if (pending != null && !secondConfirm) {
        RestoreSummarySheet(pending.second, onDismiss = vm::cancelRestore) { secondConfirm = true }
    }
    if (pending != null && secondConfirm) {
        ConfirmDialog(
            title = "Replace all data?", text = "This replaces everything on this phone. Continue?", confirm = "Replace", danger = true,
            onDismiss = { secondConfirm = false; vm.cancelRestore() },
        ) {
            secondConfirm = false
            vm.confirmRestore { err -> snack.show(err?.let { "Restore failed: $it" } ?: "Restored ✓") }
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
            Text(folderName(s.folderUri) ?: "No folder", style = Penny.type.bodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f), maxLines = 1)
            if (s.folderUri != null) {
                val ok = s.folderOk
                Icon(if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, if (ok) "Access OK" else "Access lost",
                    tint = if (ok) c.accent else c.warning, modifier = Modifier.size(20.dp))
            }
        }
        if (s.folderUri != null && !s.folderOk) {
            Text("Folder access was lost. Pick the folder again.", style = Penny.type.caption, color = c.warning, modifier = Modifier.padding(top = 6.dp))
        }
        Text(
            "Last backup: " + (s.lastBackupAt?.let { Formatters.relativeDays(it) } ?: "Never"),
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
            Text("Working…", style = Penny.type.label, color = c.textSecondary)
        }
    } else {
        PennyButton("Back up now", onClick, Modifier.fillMaxWidth())
    }
    s.justBackedUp?.let { name ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = c.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Saved $name", style = Penny.type.caption, color = c.accent)
        }
    }
}

@Composable
private fun KeepStepper(keep: Int, onSet: (Int) -> Unit) {
    val c = Penny.colors
    PennyCard(Modifier.fillMaxWidth(), padding = 8.dp) {
        Row(Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Keep last", style = Penny.type.body, color = c.textPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = { onSet((keep - 1).coerceIn(1, 30)) }, enabled = keep > 1) {
                Icon(Icons.Rounded.Remove, "Fewer", tint = if (keep > 1) c.accent else c.textSecondary)
            }
            Text("$keep files", style = Penny.type.bodyStrong, color = c.textPrimary)
            IconButton(onClick = { onSet((keep + 1).coerceIn(1, 30)) }, enabled = keep < 30) {
                Icon(Icons.Rounded.Add, "More", tint = if (keep < 30) c.accent else c.textSecondary)
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
