package com.penny.wallet.feature.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.AmountSheet
import com.penny.wallet.core.ui.components.ButtonKind
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.components.NavRow
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyDatePickerDialog
import com.penny.wallet.core.ui.components.PennySheet
import com.penny.wallet.core.ui.components.SegmentedControl
import com.penny.wallet.core.ui.components.SwitchRow
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.core.util.dateText
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.data.db.entities.DebtPayment
import com.penny.wallet.domain.DebtStatus
import com.penny.wallet.feature.home.pennyFieldColors
import java.time.LocalDate

/** Add (initial == null) or edit a debt. */
@Composable
internal fun DebtEditSheet(
    initial: Debt?,
    onDismiss: () -> Unit,
    onSave: (person: String, direction: DebtDirection, amount: Long, dueDate: LocalDate?, remind: Boolean, note: String) -> Unit,
) {
    val c = Penny.colors
    var person by rememberSaveable { mutableStateOf(initial?.person ?: "") }
    var direction by rememberSaveable { mutableStateOf(initial?.direction ?: DebtDirection.I_OWE) }
    var amount by rememberSaveable { mutableLongStateOf(initial?.amountCents ?: 0L) }
    var dueDate by rememberSaveable { mutableStateOf(initial?.dueDate) }
    var remind by rememberSaveable { mutableStateOf(initial?.remind ?: false) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var editAmount by rememberSaveable { mutableStateOf(false) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val dates = dateText()

    PennySheet(onDismiss = onDismiss, title = stringResource(if (initial == null) R.string.debts_new else R.string.debts_edit)) {
        Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                person, { person = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.debts_person)) },
                textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
            )
            SegmentedControl(
                listOf(stringResource(R.string.debts_you_owe), stringResource(R.string.debts_owes_you)),
                if (direction == DebtDirection.I_OWE) 0 else 1,
                { direction = if (it == 0) DebtDirection.I_OWE else DebtDirection.OWED_TO_ME },
            )
            NavRow(stringResource(R.string.label_amount), if (amount > 0) money(amount, ignorePrivacy = true) else null, { editAmount = true })
            NavRow(
                stringResource(R.string.debts_due_date),
                dueDate?.let { dates.day(it) } ?: stringResource(R.string.debts_due_none),
                { pickDate = true },
            )
            if (dueDate != null) {
                TextButton(onClick = { dueDate = null; remind = false }) {
                    Text(stringResource(R.string.debts_remove_due), color = c.danger)
                }
                SwitchRow(stringResource(R.string.debts_remind), remind, { remind = it }, subtitle = stringResource(R.string.debts_remind_sub))
            }
            OutlinedTextField(
                note, { note = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.label_note)) },
                textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
            )
            PennyButton(
                stringResource(R.string.action_save),
                { onSave(person.trim(), direction, amount, dueDate, remind && dueDate != null, note) },
                Modifier.fillMaxWidth().padding(top = 12.dp),
                enabled = person.isNotBlank() && amount > 0,
            )
        }
    }
    if (editAmount) {
        AmountSheet(
            stringResource(R.string.label_amount), onDismiss = { editAmount = false }, confirmLabel = stringResource(R.string.action_save),
            initialCents = amount,
        ) { cents -> amount = cents; editAmount = false }
    }
    if (pickDate) {
        PennyDatePickerDialog(dueDate ?: LocalDate.now(), onDismiss = { pickDate = false }) { dueDate = it; pickDate = false }
    }
}

/** One debt: what's left, the repayments so far, and every action on it. */
@Composable
internal fun DebtDetailSheet(
    status: DebtStatus,
    payments: List<DebtPayment>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onAddPayment: (amount: Long, accountId: String?, onDone: () -> Unit) -> Unit,
    onDeletePayment: (DebtPayment) -> Unit,
    onSetSettled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = Penny.colors
    val d = status.debt
    val dates = dateText()
    var paying by rememberSaveable { mutableStateOf(false) }
    PennySheet(onDismiss = onDismiss, title = d.person) {
        Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(directionLabel(d.direction)), style = Penny.type.caption, color = c.textSecondary)
            MoneyText(status.remaining, Penny.type.amountLarge, if (d.direction == DebtDirection.I_OWE) c.danger else c.accent)
            Text(stringResource(R.string.debts_of_total, money(d.amountCents)), style = Penny.type.caption, color = c.textSecondary)
            if (status.settled) Text(stringResource(R.string.debts_settled), style = Penny.type.bodyStrong, color = c.textSecondary)
            d.note?.let { Text(it, style = Penny.type.body, color = c.textSecondary) }

            Text(stringResource(R.string.debts_payments), style = Penny.type.bodyStrong, color = c.textPrimary, modifier = Modifier.padding(top = 8.dp))
            if (payments.isEmpty()) Text(stringResource(R.string.debts_no_payments), style = Penny.type.caption, color = c.textSecondary)
            payments.forEach { p ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(dates.day(p.date), style = Penny.type.body, color = c.textPrimary)
                        if (p.transactionId != null) {
                            Text(stringResource(R.string.debts_payment_recorded), style = Penny.type.caption, color = c.textSecondary)
                        }
                    }
                    MoneyText(p.amountCents, Penny.type.bodyStrong, c.textPrimary)
                    IconButton(onClick = { onDeletePayment(p) }) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.debts_remove_payment), tint = c.textSecondary)
                    }
                }
            }

            if (!status.settled) {
                PennyButton(stringResource(R.string.debts_record_payment), { paying = true }, Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            // Fully repaid can't be reopened by a flag: remove a payment or raise the amount instead.
            if (status.remaining > 0) {
                PennyButton(
                    stringResource(if (status.settled) R.string.debts_reopen else R.string.debts_mark_settled),
                    { onSetSettled(!status.settled) }, Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PennyButton(stringResource(R.string.action_edit), onEdit, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
                PennyButton(stringResource(R.string.action_delete), onDelete, Modifier.weight(1f), kind = ButtonKind.DANGER)
            }
        }
    }
    if (paying) {
        PaymentSheet(status.remaining, accounts, onDismiss = { paying = false }) { amount, accountId ->
            onAddPayment(amount, accountId) { paying = false }
        }
    }
}
