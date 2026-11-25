package com.penny.wallet.feature.debts

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.core.util.dateText
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.domain.DebtStatus
import com.penny.wallet.domain.DebtTotals

@Composable
fun DebtsScreen(onBack: () -> Unit) {
    val vm = pennyViewModel { DebtsViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val c = Penny.colors
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    val deletedText = stringResource(R.string.debts_deleted)
    val paymentRemovedText = stringResource(R.string.debts_payment_removed)

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = c.textPrimary) }
            Text(stringResource(R.string.debts_title), style = Penny.type.title, color = c.textPrimary)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Penny.dims.gutter).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
        ) {
            TotalsCard(state.totals)
            PennyButton(stringResource(R.string.debts_add), { adding = true }, Modifier.fillMaxWidth())
            if (!state.loading && state.statuses.isEmpty()) {
                Text(stringResource(R.string.debts_empty), style = Penny.type.caption, color = c.textSecondary)
            }
            state.statuses.forEach { s -> DebtRow(s) { detailId = s.debt.id } }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (adding || editingId != null) {
        val initial = editingId?.let { id -> state.statuses.firstOrNull { it.debt.id == id }?.debt }
        val close = { adding = false; editingId = null }
        DebtEditSheet(initial, onDismiss = close) { person, direction, amount, due, remind, note ->
            vm.save(initial, person, direction, amount, due, remind, note, onDone = close, onError = snack::show)
        }
    }

    val detail = detailId?.let { id -> state.statuses.firstOrNull { it.debt.id == id } }
    if (detail != null) {
        val id = detail.debt.id
        DebtDetailSheet(
            status = detail,
            payments = state.paymentsByDebt[id].orEmpty(),
            accounts = state.accounts,
            onDismiss = { detailId = null },
            onAddPayment = { amount, account, done -> vm.addPayment(id, amount, account, onDone = done, onError = snack::show) },
            onDeletePayment = { vm.deletePayment(it, onDeleted = { gone -> snack.undo(paymentRemovedText) { vm.restorePayment(gone) } }, onError = snack::show) },
            onSetSettled = { vm.setSettled(id, it, onError = snack::show) },
            onEdit = { detailId = null; editingId = id },
            onDelete = {
                detailId = null
                vm.delete(id, onDeleted = { gone -> snack.undo(deletedText) { vm.restore(gone) } }, onError = snack::show)
            },
        )
    }
}

@Composable
private fun TotalsCard(t: DebtTotals) {
    val c = Penny.colors
    PennyCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Penny.dims.gap)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.debts_you_owe), style = Penny.type.caption, color = c.textSecondary)
                MoneyText(t.iOwe, Penny.type.amount, c.danger)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.debts_owed_to_you), style = Penny.type.caption, color = c.textSecondary)
                MoneyText(t.owedToMe, Penny.type.amount, c.accent)
            }
        }
        if (t.open > 0) {
            Text(
                pluralStringResource(R.plurals.debts_open_count, t.open, t.open), style = Penny.type.caption, color = c.textSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun DebtRow(s: DebtStatus, onClick: () -> Unit) {
    val c = Penny.colors
    val dates = dateText()
    val d = s.debt
    PennyCard(Modifier.fillMaxWidth().alpha(if (s.settled) 0.6f else 1f), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(d.person, style = Penny.type.bodyStrong, color = c.textPrimary)
                Text(stringResource(directionLabel(d.direction)), style = Penny.type.caption, color = c.textSecondary)
                when {
                    s.settled -> Text(stringResource(R.string.debts_settled), style = Penny.type.caption, color = c.textSecondary)
                    s.paid > 0 -> Text(
                        stringResource(R.string.debts_left_of, money(s.remaining), money(d.amountCents)),
                        style = Penny.type.caption, color = c.textSecondary,
                    )
                }
                val due = d.dueDate
                if (due != null && !s.settled) {
                    if (s.overdue) Text(stringResource(R.string.debts_overdue_since, dates.day(due)), style = Penny.type.caption, color = c.danger)
                    else Text(stringResource(R.string.debts_due, dates.day(due)), style = Penny.type.caption, color = c.textSecondary)
                }
            }
            MoneyText(
                if (s.settled) d.amountCents else s.remaining, Penny.type.bodyStrong,
                when { s.settled -> c.textSecondary; d.direction == DebtDirection.I_OWE -> c.danger; else -> c.accent },
            )
        }
    }
}

internal fun directionLabel(d: DebtDirection) = if (d == DebtDirection.I_OWE) R.string.debts_you_owe else R.string.debts_owes_you
