package com.penny.wallet.feature.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.feature.home.pennyFieldColors
import com.penny.wallet.core.ui.components.AmountDisplay
import com.penny.wallet.core.ui.components.Numpad
import com.penny.wallet.core.ui.components.NumpadLogic
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyChip
import com.penny.wallet.core.ui.components.PennySheet
import com.penny.wallet.core.ui.components.SegmentedControl
import com.penny.wallet.core.ui.components.clickableNoRipple
import com.penny.wallet.core.ui.theme.Penny

/** Add / edit sheet for a recurring rule. [onSave] gets the draft and a callback to show a validation error inline. */
@Composable
internal fun RuleSheet(
    initial: RuleDraft, state: RecurringState, onDismiss: () -> Unit,
    onSave: (RuleDraft, showError: (String) -> Unit) -> Unit,
) {
    val c = Penny.colors
    var d by rememberSaveable(stateSaver = RuleDraftSaver) { mutableStateOf(initial.withDefaults(state)) }
    var digits by rememberSaveable { mutableStateOf(NumpadLogic.fromCents(initial.amountCents)) }
    var showPad by rememberSaveable { mutableStateOf(initial.amountCents <= 0) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val editing = initial.id != null

    PennySheet(onDismiss = onDismiss, title = if (editing) "Edit recurring" else "New recurring") {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Penny.dims.gap)) {
            OutlinedTextField(
                d.name, { d = d.copy(name = it); error = null }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Name") }, placeholder = { Text("e.g. Phone credit", color = c.textSecondary) },
                textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
            )
            AmountDisplay(Formatters.digitsToCents(digits), modifier = Modifier.clickableNoRipple { showPad = !showPad })
            if (showPad) {
                Numpad(digits, { digits = it; error = null }, keyHeight = 48)
            } else {
                Text("Tap the amount to edit", style = Penny.type.caption, color = c.textSecondary)
            }

            FieldLabel("Category")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { cat -> PennyChip(cat.name, d.categoryId == cat.id, { d = d.copy(categoryId = cat.id); error = null }) }
            }
            FieldLabel("Account")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.accounts.forEach { a -> PennyChip(a.name, d.accountId == a.id, { d = d.copy(accountId = a.id); error = null }) }
            }

            FieldLabel("Repeats")
            val freqs = listOf(Frequency.WEEKLY, Frequency.MONTHLY, Frequency.YEARLY)
            SegmentedControl(listOf("Weekly", "Monthly", "Yearly"), freqs.indexOf(d.frequency), { d = d.copy(frequency = freqs[it]) })
            SchedulePicker(d) { d = it }

            FieldLabel("Remind me")
            RemindStepper(d.remindDaysBefore) { d = d.copy(remindDaysBefore = it) }
            SwitchRow(
                "Auto-pay",
                if (d.autoPay) "Recorded automatically on the due date" else "You'll get a Mark as paid reminder",
                d.autoPay,
            ) { d = d.copy(autoPay = it) }
            if (editing) SwitchRow("Active", if (d.active) "Reminders and payments are on" else "Paused: nothing happens until resumed", d.active) {
                d = d.copy(active = it)
            }

            error?.let { Text(it, style = Penny.type.label, color = c.danger) }
            PennyButton(
                if (editing) "Save" else "Add recurring expense",
                { onSave(d.copy(amountCents = Formatters.digitsToCents(digits))) { error = it } },
                Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(text, style = Penny.type.label, color = Penny.colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
}

/** Preselect the only category / account so the common case needs fewer taps. */
private fun RuleDraft.withDefaults(s: RecurringState) = copy(
    categoryId = categoryId ?: s.categories.singleOrNull()?.id,
    accountId = accountId ?: s.accounts.firstOrNull()?.id,
)
