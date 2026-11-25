package com.penny.wallet.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.penny.wallet.platform.crash.CrashReporter
import com.penny.wallet.core.ui.components.ButtonKind
import com.penny.wallet.core.ui.components.EmptyState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.data.prefs.HomeSection
import com.penny.wallet.core.ui.common.TxRowItem
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.components.clickableNoRipple
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny

@Composable
fun HomeScreen(
    onOpenAccount: (String) -> Unit,
    onOpenRecurring: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onOpenBackup: () -> Unit,
    onAddExpense: () -> Unit,
) {
    val vm = pennyViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val prefs = Penny.prefs
    val snack = LocalSnack.current
    var addAccount by rememberSaveable { mutableStateOf(false) }
    var confirmRemoveSample by rememberSaveable { mutableStateOf(false) }
    val sections = remember(prefs.homeSections, prefs.hiddenHomeSections) {
        prefs.homeSections.filterNot { it in prefs.hiddenHomeSections }
    }
    val gutter = Penny.dims.gutter
    val deletedText = stringResource(R.string.deleted)
    val recentLabel = stringResource(R.string.home_recent)
    val recentEmpty = stringResource(R.string.home_recent_empty)
    val context = LocalContext.current
    var crashReport by remember { mutableStateOf(CrashReporter.pending(context)) }
    val noMailApp = stringResource(R.string.crash_no_mail_app)

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
    ) {
        // Items composed for the empty loading state would keep their scroll position when the banners and
        // accounts arrive, so Home opened scrolled past the banner and to the end of the accounts row.
        if (state.loading) return@LazyColumn
        if (state.firstRun) {
            item(key = "welcome") {
                WelcomeCard(
                    onAddAccount = { addAccount = true }, onTrySample = vm::addSampleData,
                    modifier = Modifier.padding(horizontal = gutter, vertical = 24.dp),
                )
            }
        }
        if (state.hasSampleData) {
            item(key = "sample") { SampleBanner({ confirmRemoveSample = true }, Modifier.padding(horizontal = gutter, vertical = 4.dp)) }
        }
        crashReport?.let { report ->
            item(key = "crash") {
                CrashBanner(
                    onSend = {
                        if (CrashReporter.send(context, report)) CrashReporter.clear(context) else snack.show(noMailApp)
                        crashReport = CrashReporter.pending(context)
                    },
                    onDismiss = { CrashReporter.clear(context); crashReport = null },
                    modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp),
                )
            }
        }
        if (state.backupWarning) {
            item(key = "backup") { BackupBanner(onOpenBackup, Modifier.padding(horizontal = gutter, vertical = 4.dp)) }
        }
        if (!state.firstRun) sections.forEach { section ->
            when (section) {
                HomeSection.HERO -> item(key = "hero") { HomeHero(state, Modifier.padding(horizontal = gutter)) }
                HomeSection.MONTH -> item(key = "month") { MonthCard(state, Modifier.padding(horizontal = gutter)) }
                HomeSection.ACCOUNTS -> item(key = "accounts") {
                    AccountCarousel(state.accounts, onOpenAccount, onAdd = { addAccount = true })
                }
                HomeSection.UPCOMING -> if (state.upcoming.isNotEmpty()) item(key = "upcoming") {
                    UpcomingStrip(state.upcoming, state.overdueCount, onOpenRecurring)
                }
                HomeSection.RECENT -> {
                    item(key = "recent_label") { SectionLabel(recentLabel, Modifier.padding(horizontal = gutter)) }
                    if (state.recent.isEmpty()) {
                        item(key = "recent_empty") {
                            EmptyState(
                                recentEmpty, Modifier.padding(horizontal = gutter),
                                action = stringResource(R.string.expenses_add), kind = ButtonKind.PRIMARY, onAction = onAddExpense,
                            )
                        }
                    }
                    items(state.recent, key = { "tx_" + it.tx.id }) { row ->
                        TxRowItem(
                            row,
                            Modifier.padding(horizontal = gutter).animateItem(),
                            onClick = { onEditTransaction(row.tx.id) },
                            onDelete = { vm.delete(row.tx) { t -> snack.undo(deletedText) { vm.restore(t) } } },
                        )
                    }
                }
            }
        }
    }

    if (addAccount) {
        AddAccountSheet(
            onDismiss = { addAccount = false },
            currencyCode = state.currencyCode.takeIf { state.firstRun },
            onCurrencyChange = vm::setCurrency,
            onSave = { name, icon, cents -> vm.addAccount(name, icon, cents); addAccount = false },
        )
    }

    if (confirmRemoveSample) {
        ConfirmDialog(
            stringResource(R.string.sample_remove_title), stringResource(R.string.sample_remove_body),
            stringResource(R.string.sample_remove), danger = true,
            onDismiss = { confirmRemoveSample = false },
            onConfirm = { vm.removeSampleData(); confirmRemoveSample = false },
        )
    }
}

@Composable
private fun CrashBanner(onSend: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    Column(modifier.fillMaxWidth().clip(Penny.dims.smallShape).background(c.surface1).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(stringResource(R.string.crash_banner), style = Penny.type.label, color = c.textPrimary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.crash_dismiss), color = c.textSecondary) }
            TextButton(onClick = onSend) { Text(stringResource(R.string.crash_send), color = c.accent) }
        }
    }
}

@Composable
private fun BackupBanner(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    Row(
        modifier.fillMaxWidth().clip(Penny.dims.smallShape).background(c.warning.copy(alpha = 0.12f))
            .clickableNoRipple(onClick).padding(horizontal = 14.dp, vertical = 10.dp).animateContentSize(sizeSpec()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Warning, null, tint = c.warning, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.home_backup_warning), style = Penny.type.label, color = c.warning, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.home_back_up), style = Penny.type.label, color = c.warning)
    }
}
