package com.penny.wallet.feature.settings

import com.penny.wallet.core.util.dateText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Euro
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.feature.home.ConfirmDialog
import com.penny.wallet.feature.home.pennyFieldColors
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.ItemPalette
import com.penny.wallet.core.ui.theme.Penny
import androidx.compose.ui.graphics.Color

/** Settings hub (SPEC §8 Settings). */
@Composable
fun SettingsScreen(onOpen: (String) -> Unit) {
    val backupVm = pennyViewModel { BackupViewModel(it) }
    val manageVm = pennyViewModel { ManageViewModel(it) }
    val backup by backupVm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val c = Penny.colors
    var resetStep by rememberSaveable { mutableIntStateOf(0) }

    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = Penny.dims.gutter),
    ) {
        Text("Settings", style = Penny.type.amountLarge, color = c.textPrimary, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))

        SectionLabel("Money")
        Group {
            SettingsRow(Icons.Rounded.Category, "Expense categories", { onOpen("categories") }, "Add, edit, reorder, archive", tint = Color(ItemPalette[3]))
            RowDivider()
            SettingsRow(Icons.Rounded.AccountBalance, "Accounts", { onOpen("accounts") }, "Reorder, archive, add", tint = Color(ItemPalette[1]))
            RowDivider()
            SettingsRow(Icons.Rounded.Repeat, "Recurring expenses", { onOpen("recurring") }, "Bills, subscriptions, reminders", tint = Color(ItemPalette[4]))
        }

        SectionLabel("App", Modifier.padding(top = 12.dp))
        Group {
            SettingsRow(Icons.Rounded.Palette, "Personalise", { onOpen("personalise") }, "Theme, layout, motion, privacy", tint = Color(ItemPalette[2]))
            RowDivider()
            val last = backup.lastBackupAt
            val sub = when {
                backup.folderUri == null -> "Not set up"
                last == null -> "Never backed up"
                else -> "Last backup: ${dateText().relativeDays(last)}"
            }
            SettingsRow(Icons.Rounded.Backup, "Backup", { onOpen("backup") }, sub, tint = if (backup.folderUri != null && !backup.folderOk) c.warning else Color(ItemPalette[7]))
            RowDivider()
            SettingsRow(Icons.Rounded.Euro, "Number formatting", { onOpen("personalise") }, "EUR · ${money(123456, ignorePrivacy = true)}", tint = Color(ItemPalette[10]))
        }

        SectionLabel("Danger zone", Modifier.padding(top = 12.dp))
        Group {
            SettingsRow(Icons.Rounded.DeleteForever, "Reset all data", { resetStep = 1 }, "Deletes everything on this phone", danger = true, chevron = false)
        }

        Text(
            "Penny Wallet 1.0 · all data stays on this phone", style = Penny.type.caption, color = c.textSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        Spacer(Modifier.height(120.dp))
    }

    when (resetStep) {
        1 -> ConfirmDialog(
            title = "Reset all data?", text = "Accounts, categories, goals, transactions and recurring expenses will be deleted. Make a backup first if you might want them back.",
            confirm = "Continue", danger = true, onDismiss = { resetStep = 0 },
        ) { resetStep = 2 }
        2 -> TypeResetDialog(onDismiss = { resetStep = 0 }) {
            resetStep = 0
            manageVm.resetAll { snack.show("All data cleared") }
        }
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    PennyCard(Modifier.fillMaxWidth(), padding = 0.dp) { Column(Modifier.padding(vertical = 4.dp)) { content() } }
}

/** Second confirmation: the user has to type RESET. */
@Composable
private fun TypeResetDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val c = Penny.colors
    var typed by remember { mutableStateOf("") }
    val ok = typed.trim() == "RESET"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface2, titleContentColor = c.textPrimary, textContentColor = c.textSecondary,
        shape = Penny.dims.cardShape,
        title = { Text("Are you absolutely sure?", style = Penny.type.title) },
        text = {
            Column {
                Text("This can't be undone. Type RESET to confirm.", style = Penny.type.body)
                OutlinedTextField(
                    typed, { typed = it }, Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true,
                    placeholder = { Text("RESET", color = c.textSecondary) },
                    textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = ok) {
                Text("Delete everything", color = if (ok) c.danger else c.textSecondary, style = Penny.type.bodyStrong)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) } },
    )
}
