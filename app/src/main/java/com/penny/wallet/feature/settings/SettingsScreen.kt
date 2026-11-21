package com.penny.wallet.feature.settings

import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
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

@Composable
fun SettingsScreen(onOpen: (String) -> Unit) {
    val backupVm = pennyViewModel { BackupViewModel(it) }
    val manageVm = pennyViewModel { ManageViewModel(it) }
    val backup by backupVm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val c = Penny.colors
    var resetStep by rememberSaveable { mutableIntStateOf(0) }
    val clearedMsg = stringResource(R.string.settings_reset_done)

    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = Penny.dims.gutter),
    ) {
        Text(stringResource(R.string.settings_title), style = Penny.type.amountLarge, color = c.textPrimary, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))

        SectionLabel(stringResource(R.string.settings_section_money))
        Group {
            SettingsRow(Icons.Rounded.Category, stringResource(R.string.settings_categories), { onOpen("categories") }, stringResource(R.string.settings_categories_sub), tint = Color(ItemPalette[3]))
            RowDivider()
            SettingsRow(Icons.Rounded.AccountBalance, stringResource(R.string.settings_accounts), { onOpen("accounts") }, stringResource(R.string.settings_accounts_sub), tint = Color(ItemPalette[1]))
            RowDivider()
            SettingsRow(Icons.Rounded.Repeat, stringResource(R.string.settings_recurring), { onOpen("recurring") }, stringResource(R.string.settings_recurring_sub), tint = Color(ItemPalette[4]))
        }

        SectionLabel(stringResource(R.string.settings_section_app), Modifier.padding(top = 12.dp))
        Group {
            SettingsRow(Icons.Rounded.Palette, stringResource(R.string.settings_personalise), { onOpen("personalise") }, stringResource(R.string.settings_personalise_sub), tint = Color(ItemPalette[2]))
            RowDivider()
            val last = backup.lastBackupAt
            val sub = when {
                backup.folderUri == null -> stringResource(R.string.settings_backup_not_set_up)
                last == null -> stringResource(R.string.settings_backup_never)
                else -> stringResource(R.string.backup_last, dateText().relativeDays(last))
            }
            SettingsRow(Icons.Rounded.Backup, stringResource(R.string.settings_backup), { onOpen("backup") }, sub, tint = if (backup.folderUri != null && !backup.folderOk) c.warning else Color(ItemPalette[7]))
            RowDivider()
            SettingsRow(Icons.Rounded.Euro, stringResource(R.string.settings_number_format), { onOpen("personalise") }, "${Penny.prefs.currencyCode} · ${money(123456, ignorePrivacy = true)}", tint = Color(ItemPalette[10]))
        }

        SectionLabel(stringResource(R.string.settings_section_danger), Modifier.padding(top = 12.dp))
        Group {
            SettingsRow(Icons.Rounded.DeleteForever, stringResource(R.string.settings_reset), { resetStep = 1 }, stringResource(R.string.settings_reset_sub), danger = true, chevron = false)
        }

        Text(
            stringResource(R.string.settings_footer, appVersion()), style = Penny.type.caption, color = c.textSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        Spacer(Modifier.height(120.dp))
    }

    when (resetStep) {
        1 -> ConfirmDialog(
            title = stringResource(R.string.settings_reset_title), text = stringResource(R.string.settings_reset_body),
            confirm = stringResource(R.string.settings_reset_continue), danger = true, onDismiss = { resetStep = 0 },
        ) { resetStep = 2 }
        2 -> TypeResetDialog(onDismiss = { resetStep = 0 }) {
            resetStep = 0
            manageVm.resetAll { snack.show(clearedMsg) }
        }
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    PennyCard(Modifier.fillMaxWidth(), padding = 0.dp) { Column(Modifier.padding(vertical = 4.dp)) { content() } }
}

/** Second confirmation: the user has to type the reset word. */
@Composable
private fun TypeResetDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val c = Penny.colors
    var typed by remember { mutableStateOf("") }
    val word = stringResource(R.string.settings_reset_word)
    val ok = typed.trim() == word
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface2, titleContentColor = c.textPrimary, textContentColor = c.textSecondary,
        shape = Penny.dims.cardShape,
        title = { Text(stringResource(R.string.settings_reset_sure_title), style = Penny.type.title) },
        text = {
            Column {
                Text(stringResource(R.string.settings_reset_sure_body, word), style = Penny.type.body)
                OutlinedTextField(
                    typed, { typed = it }, Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true,
                    placeholder = { Text(word, color = c.textSecondary) },
                    textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = ok) {
                Text(stringResource(R.string.settings_reset_confirm), color = if (ok) c.danger else c.textSecondary, style = Penny.type.bodyStrong)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = c.textSecondary) } },
    )
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
}
