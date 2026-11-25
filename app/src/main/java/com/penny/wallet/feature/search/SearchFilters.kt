package com.penny.wallet.feature.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.AmountSheet
import com.penny.wallet.core.ui.components.ButtonKind
import com.penny.wallet.core.ui.components.NavRow
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyChip
import com.penny.wallet.core.ui.components.PennyDatePickerDialog
import com.penny.wallet.core.ui.components.PennySheet
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.core.util.dateText
import com.penny.wallet.domain.SearchKind
import java.time.LocalDate

private enum class AmountStep { NONE, MENU, MIN, MAX }

@Composable
internal fun SearchFilterChips(
    state: SearchState,
    onPeriod: (SearchPeriod) -> Unit,
    onCustom: (LocalDate, LocalDate) -> Unit,
    onAmount: (Long?, Long?) -> Unit,
    onKind: (SearchKind) -> Unit,
    onClear: () -> Unit,
) {
    val f = state.filter
    var amountStep by rememberSaveable { mutableStateOf(AmountStep.NONE) }
    var pickFrom by rememberSaveable { mutableStateOf(false) }
    var customFrom by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    val gutter = Penny.dims.gutter

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeriodChip(state, onPeriod, onCustom = { pickFrom = true })
        PennyChip(amountLabel(f.minCents, f.maxCents), f.minCents != null || f.maxCents != null, { amountStep = AmountStep.MENU })
        KindChip(f.kind, onKind)
        if (state.period != SearchPeriod.ANY || f.minCents != null || f.maxCents != null || f.kind != SearchKind.ALL) {
            PennyChip(stringResource(R.string.search_clear), false, onClear)
        }
    }

    if (pickFrom) {
        PennyDatePickerDialog(f.from ?: LocalDate.now(), onDismiss = { pickFrom = false }) { pickFrom = false; customFrom = it }
    }
    customFrom?.let { from ->
        PennyDatePickerDialog(maxOf(f.to ?: LocalDate.now(), from), onDismiss = { customFrom = null }) { customFrom = null; onCustom(from, it) }
    }

    when (amountStep) {
        AmountStep.NONE -> Unit
        AmountStep.MENU -> PennySheet(onDismiss = { amountStep = AmountStep.NONE }, title = stringResource(R.string.search_amount)) {
            val any = stringResource(R.string.search_amount_any)
            NavRow(stringResource(R.string.search_amount_min), f.minCents?.let { money(it) } ?: any, { amountStep = AmountStep.MIN })
            NavRow(stringResource(R.string.search_amount_max), f.maxCents?.let { money(it) } ?: any, { amountStep = AmountStep.MAX })
            Spacer(Modifier.height(Penny.dims.gap))
            PennyButton(
                stringResource(R.string.search_clear), { onAmount(null, null); amountStep = AmountStep.NONE },
                Modifier.fillMaxWidth(), kind = ButtonKind.SECONDARY,
            )
        }
        AmountStep.MIN -> AmountSheet(
            stringResource(R.string.search_amount_min), { amountStep = AmountStep.MENU }, stringResource(R.string.action_ok),
            initialCents = f.minCents ?: 0, allowZero = true,
        ) { onAmount(it.takeIf { c -> c > 0 }, f.maxCents); amountStep = AmountStep.MENU } // 0 = no lower bound
        AmountStep.MAX -> AmountSheet(
            stringResource(R.string.search_amount_max), { amountStep = AmountStep.MENU }, stringResource(R.string.action_ok),
            initialCents = f.maxCents ?: 0, allowZero = true,
        ) { onAmount(f.minCents, it.takeIf { c -> c > 0 }); amountStep = AmountStep.MENU } // 0 = no upper bound
    }
}

@Composable
private fun PeriodChip(state: SearchState, onPeriod: (SearchPeriod) -> Unit, onCustom: () -> Unit) {
    val c = Penny.colors
    var open by rememberSaveable { mutableStateOf(false) }
    val f = state.filter
    val label = if (state.period == SearchPeriod.CUSTOM && f.from != null && f.to != null) {
        stringResource(R.string.search_range, dateText().shortDate(f.from), dateText().shortDate(f.to))
    } else stringResource(periodLabel(state.period))
    Box {
        PennyChip(label, state.period != SearchPeriod.ANY, { open = true })
        DropdownMenu(open, { open = false }, containerColor = c.surface2) {
            SearchPeriod.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text(stringResource(periodLabel(p)), color = if (p == state.period) c.accent else c.textPrimary) },
                    onClick = { open = false; if (p == SearchPeriod.CUSTOM) onCustom() else onPeriod(p) },
                )
            }
        }
    }
}

@Composable
private fun KindChip(kind: SearchKind, onKind: (SearchKind) -> Unit) {
    val c = Penny.colors
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        PennyChip(stringResource(kindLabel(kind)), kind != SearchKind.ALL, { open = true })
        DropdownMenu(open, { open = false }, containerColor = c.surface2) {
            SearchKind.entries.forEach { k ->
                DropdownMenuItem(
                    text = { Text(stringResource(kindLabel(k)), color = if (k == kind) c.accent else c.textPrimary) },
                    onClick = { open = false; onKind(k) },
                )
            }
        }
    }
}

@Composable
private fun amountLabel(min: Long?, max: Long?): String = when {
    min != null && max != null -> stringResource(R.string.search_range, money(min), money(max))
    min != null -> stringResource(R.string.search_amount_from, money(min))
    max != null -> stringResource(R.string.search_amount_up_to, money(max))
    else -> stringResource(R.string.search_amount)
}

private fun periodLabel(p: SearchPeriod): Int = when (p) {
    SearchPeriod.ANY -> R.string.search_period_any
    SearchPeriod.THIS_MONTH -> R.string.search_period_this_month
    SearchPeriod.LAST_3_MONTHS -> R.string.search_period_last_3_months
    SearchPeriod.THIS_YEAR -> R.string.search_period_this_year
    SearchPeriod.LAST_YEAR -> R.string.search_period_last_year
    SearchPeriod.CUSTOM -> R.string.search_period_custom
}

private fun kindLabel(k: SearchKind): Int = when (k) {
    SearchKind.ALL -> R.string.search_kind_all
    SearchKind.EXPENSES -> R.string.search_kind_expenses
    SearchKind.INCOME -> R.string.search_kind_income
}
