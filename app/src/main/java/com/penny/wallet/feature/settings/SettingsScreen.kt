package com.penny.wallet.feature.settings

import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.ItemPalette
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.core.util.dateText

@Composable
fun SettingsScreen(onOpen: (String) -> Unit) {
    val backupVm = pennyViewModel { BackupViewModel(it) }
    val prefsVm = pennyViewModel { PersonaliseViewModel(it) }
    val backup by backupVm.state.collectAsStateWithLifecycle()
    val prefs by prefsVm.prefs.collectAsStateWithLifecycle()
    val x = PrefsCtx(prefs, prefsVm.defaults, prefsVm::set)
    val c = Penny.colors

    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = Penny.dims.gutter),
    ) {
        Text(stringResource(R.string.settings_title), style = Penny.type.amountLarge, color = c.textPrimary, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))

        SectionLabel(stringResource(R.string.settings_section_money))
        RowGroup {
            SettingsRow(Icons.Rounded.AccountBalance, stringResource(R.string.settings_accounts), { onOpen("accounts") }, stringResource(R.string.settings_accounts_sub), tint = Color(ItemPalette[1]))
            RowDivider()
            SettingsRow(Icons.Rounded.Category, stringResource(R.string.settings_categories), { onOpen("categories") }, stringResource(R.string.settings_categories_sub), tint = Color(ItemPalette[3]))
            RowDivider()
            SettingsRow(Icons.Rounded.Repeat, stringResource(R.string.settings_recurring), { onOpen("recurring") }, stringResource(R.string.settings_recurring_sub), tint = Color(ItemPalette[4]))
            RowDivider()
            BudgetSetting(x)
        }

        SectionLabel(stringResource(R.string.settings_section_app), Modifier.padding(top = 12.dp))
        PennyCard(Modifier.fillMaxWidth()) {
            ThemeSetting(x)
            LanguageSetting()
            AccentPicker(x)
            CurrencySetting(x)
            MonthStartSetting(x)
            ReminderTimeSetting(x)
            AppLockSetting(x)
        }

        Spacer(Modifier.height(Penny.dims.gap))
        RowGroup {
            val last = backup.lastBackupAt
            val sub = when {
                backup.folderUri == null -> stringResource(R.string.settings_backup_not_set_up)
                last == null -> stringResource(R.string.settings_backup_never)
                else -> stringResource(R.string.backup_last, dateText().relativeDays(last))
            }
            SettingsRow(Icons.Rounded.Backup, stringResource(R.string.settings_backup), { onOpen("backup") }, sub, tint = if (backup.folderUri != null && !backup.folderOk) c.warning else Color(ItemPalette[7]))
            RowDivider()
            SettingsRow(Icons.Rounded.Tune, stringResource(R.string.settings_more), { onOpen("personalise") }, stringResource(R.string.settings_more_sub), tint = Color(ItemPalette[2]))
        }

        Spacer(Modifier.height(Penny.dims.gap))
        RowGroup {
            SettingsRow(Icons.Rounded.Shield, stringResource(R.string.privacy_title), { onOpen("privacy") }, stringResource(R.string.privacy_sub), tint = Color(ItemPalette[10]))
            RowDivider()
            SettingsRow(Icons.Rounded.Favorite, stringResource(R.string.support_title), { onOpen("support") }, stringResource(R.string.support_sub), tint = Color(ItemPalette[0]))
        }

        Text(
            stringResource(R.string.settings_footer, appVersion()), style = Penny.type.caption, color = c.textSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun RowGroup(content: @Composable () -> Unit) {
    PennyCard(Modifier.fillMaxWidth(), padding = 0.dp) { Column(Modifier.padding(vertical = 4.dp)) { content() } }
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
}
