package com.penny.wallet.feature.recurring

import com.penny.wallet.R
import androidx.compose.ui.res.stringResource
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

    PennySheet(onDismiss = onDismiss, title = stringResource(if (editing) R.string.recurring_edit_title else R.string.recurring_new_title)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Penny.dims.gap)) {
            OutlinedTextField(
                d.name, { d = d.copy(name = it); error = null }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.label_name)) }, placeholder = { Text(stringResource(R.string.recurring_name_hint), color = c.textSecondary) },
                textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
            )
            AmountDisplay(Formatters.digitsToCents(digits), modifier = Modifier.clickableNoRipple { showPad = !showPad })
            if (showPad) {
                Numpad(digits, { digits = it; error = null }, keyHeight = 48)
            } else {
                Text(stringResource(R.string.recurring_tap_amount), style = Penny.type.caption, color = c.textSecondary)
            }

            FieldLabel(stringResource(R.string.label_category))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { cat -> PennyChip(cat.name, d.categoryId == cat.id, { d = d.copy(categoryId = cat.id); error = null }) }
            }
            FieldLabel(stringResource(R.string.label_account))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.accounts.forEach { a -> PennyChip(a.name, d.accountId == a.id, { d = d.copy(accountId = a.id); error = null }) }
            }

            FieldLabel(stringResource(R.string.recurring_repeats))
            val freqs = listOf(Frequency.WEEKLY, Frequency.MONTHLY, Frequency.YEARLY)
            SegmentedControl(listOf(stringResource(R.string.recurring_weekly), stringResource(R.string.recurring_monthly), stringResource(R.string.recurring_yearly)), freqs.indexOf(d.frequency), { d = d.copy(frequency = freqs[it]) })
            SchedulePicker(d) { d = it }

            FieldLabel(stringResource(R.string.recurring_remind_me))
            RemindStepper(d.remindDaysBefore) { d = d.copy(remindDaysBefore = it) }
            SwitchRow(
                stringResource(R.string.recurring_auto_pay),
                stringResource(if (d.autoPay) R.string.recurring_auto_pay_on else R.string.recurring_auto_pay_off),
                d.autoPay,
            ) { d = d.copy(autoPay = it) }
            if (editing) SwitchRow(
                stringResource(R.string.recurring_active),
                stringResource(if (d.active) R.string.recurring_active_on else R.string.recurring_active_off),
                d.active,
            ) {
                d = d.copy(active = it)
            }

            error?.let { Text(it, style = Penny.type.label, color = c.danger) }
            PennyButton(
                stringResource(if (editing) R.string.action_save else R.string.recurring_add),
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
