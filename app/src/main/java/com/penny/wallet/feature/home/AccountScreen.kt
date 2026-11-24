package com.penny.wallet.feature.home

import com.penny.wallet.core.ui.components.EmptyState
import com.penny.wallet.core.util.dateText
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.container
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import com.penny.wallet.data.repo.AccountDeleteResult
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.ui.common.TxRowItem
import com.penny.wallet.core.ui.components.AnimatedAmount
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny
import java.time.LocalDate

@Composable
fun AccountScreen(id: String, onBack: () -> Unit, onEditTransaction: (String) -> Unit) {
    val vm = pennyViewModel(key = "account-$id") { AccountViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val container = LocalContext.current.container
    var left by remember { mutableStateOf(false) }
    val leave = { if (!left) { left = true; onBack() } }
    var action by remember { mutableStateOf<AccountAction?>(null) }
    var dialog by remember { mutableStateOf<AccountDialog?>(null) }

    LaunchedEffect(state.missing) { if (state.missing) leave() }
    val account = state.account ?: return
    val c = Penny.colors
    val restoredText = stringResource(R.string.home_account_restored)
    val archivedText = stringResource(R.string.home_account_archived)
    val txDeletedText = stringResource(R.string.home_transaction_deleted)
    val accountDeletedText = stringResource(R.string.home_account_deleted)
    val historyLabel = stringResource(R.string.home_history)
    val emptyText = stringResource(R.string.home_no_transactions)

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        AccountTopBar(
            name = account.name, archived = account.archived, onBack = leave,
            onRename = { dialog = AccountDialog.RENAME },
            onArchive = { vm.setArchived(!account.archived) { err -> snack.show(err ?: if (account.archived) restoredText else archivedText) } },
            onDelete = { dialog = AccountDialog.CONFIRM_DELETE },
        )
        val groups = remember(state.rows) { state.rows.groupBy { it.tx.date }.toSortedMap(compareByDescending<LocalDate> { it }) }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = Penny.dims.gutter).navigationBarsPadding()) {
            item("hero") { AccountHero(state) }
            item("actions") { ActionRow { action = it } }
            item("historyLabel") { SectionLabel(historyLabel, Modifier.padding(top = 16.dp)) }
            if (groups.isEmpty()) item("empty") {
                EmptyState(emptyText)
            }
            groups.forEach { (day, rows) ->
                item("day-$day") {
                    Text(dateText().day(day), style = Penny.type.label, color = c.textSecondary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
                items(rows, key = { it.tx.id }) { row ->
                    TxRowItem(
                        row, Modifier.animateItem(),
                        onClick = { onEditTransaction(row.tx.id) },
                        onDelete = { vm.deleteTx(row.tx) { t -> snack.undo(txDeletedText) { vm.restoreTx(t) } } },
                    )
                }
            }
            item("bottom") { Spacer(Modifier.height(32.dp)) }
        }
    }

    action?.let { a ->
        val success = stringResource(a.successMessage())
        AccountActionSheet(a, state, onDismiss = { action = null }) { cents, date, note, toId ->
            vm.perform(a, cents, date, note, toId) { err -> snack.show(err ?: success) }
            action = null
        }
    }
    when (dialog) {
        AccountDialog.RENAME -> RenameSheet(account.name, account.icon, onDismiss = { dialog = null }) { n, i -> vm.rename(n, i); dialog = null }
        AccountDialog.CONFIRM_DELETE -> ConfirmDialog(
            title = stringResource(R.string.home_delete_account_title, account.name), text = stringResource(R.string.home_delete_account_body),
            confirm = stringResource(R.string.action_delete), danger = true,
            onDismiss = { dialog = null },
        ) {
            dialog = null
            vm.delete { r ->
                when (r) {
                    is AccountDeleteResult.Deleted -> {
                        // Restore through the container: this screen's ViewModel is gone once we navigate back.
                        snack.undo(accountDeletedText) { container.accounts.restore(r.account) }
                        leave()
                    }
                    AccountDeleteResult.MustArchive -> dialog = AccountDialog.MUST_ARCHIVE
                }
            }
        }
        AccountDialog.MUST_ARCHIVE -> ConfirmDialog(
            title = stringResource(R.string.home_cant_delete_title), text = stringResource(R.string.home_cant_delete_body),
            confirm = stringResource(R.string.action_archive), danger = false,
            onDismiss = { dialog = null },
        ) {
            dialog = null
            vm.setArchived(true) { err -> snack.show(err ?: archivedText) }
        }
        null -> Unit
    }
}

internal enum class AccountDialog { RENAME, CONFIRM_DELETE, MUST_ARCHIVE }

@StringRes
private fun AccountAction.successMessage() = when (this) {
    AccountAction.INCOME -> R.string.home_income_added
    AccountAction.WITHDRAW -> R.string.home_withdrawal_recorded
    AccountAction.SET_BALANCE -> R.string.home_balance_updated
    AccountAction.TRANSFER -> R.string.home_transfer_done
}

@Composable
private fun AccountHero(state: AccountState) {
    val c = Penny.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp).then(if (Penny.motion.enabled) Modifier.animateContentSize() else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.home_balance), style = Penny.type.label, color = c.textSecondary)
        AnimatedAmount(state.money.balance, Penny.type.hero, if (state.money.balance < 0) c.danger else c.textPrimary)
        val avail = state.money.available
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_detail_available), style = Penny.type.label, color = c.textSecondary)
            Text(money(avail), style = Penny.type.label, color = if (avail < 0) c.danger else c.accent)
            Text(stringResource(R.string.home_detail_in_goals, money(state.money.allocated)), style = Penny.type.label, color = c.textSecondary)
        }
    }
}

@Composable
private fun ActionRow(onAction: (AccountAction) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        RoundAction(Icons.Rounded.Add, stringResource(R.string.home_action_add_income)) { onAction(AccountAction.INCOME) }
        RoundAction(Icons.Rounded.Remove, stringResource(R.string.home_action_withdraw)) { onAction(AccountAction.WITHDRAW) }
        RoundAction(Icons.Rounded.Tune, stringResource(R.string.home_action_set_balance)) { onAction(AccountAction.SET_BALANCE) }
        RoundAction(Icons.Rounded.SwapHoriz, stringResource(R.string.type_transfer)) { onAction(AccountAction.TRANSFER) }
    }
}

@Composable
private fun RoundAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = Penny.colors
    Column(
        Modifier.clip(Penny.dims.smallShape).clickable(onClick = onClick).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(c.surface1).border(0.75.dp, c.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = c.accent, modifier = Modifier.size(26.dp)) }
        Text(label, style = Penny.type.caption, color = c.textPrimary)
    }
}
