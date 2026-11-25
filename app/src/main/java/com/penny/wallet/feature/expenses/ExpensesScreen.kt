package com.penny.wallet.feature.expenses

import com.penny.wallet.core.ui.components.EmptyState
import com.penny.wallet.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.core.ui.common.TxRowItem
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny

@Composable
fun ExpensesScreen(onOpenRecurring: () -> Unit, onEditTransaction: (String) -> Unit, onAddExpense: () -> Unit) {
    val vm = pennyViewModel { ExpensesViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val gutter = Penny.dims.gutter
    val c = Penny.colors
    val deletedText = stringResource(R.string.deleted)
    // Selected donut slice; cleared whenever the month changes.
    var selected by rememberSaveable(state.period?.label?.toString()) { mutableStateOf<Int?>(null) }
    val selectedIdx = selected?.takeIf { it in state.slices.indices }
    val hPad = Modifier.padding(horizontal = gutter)

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
    ) {
        item(key = "header") { ExpensesHeader(onOpenRecurring, hPad) }
        item(key = "month") { MonthSwitcher(state, vm::prevMonth, vm::nextMonth) }
        item(key = "chart") { ChartCard(state, selectedIdx, { selected = it }, hPad.animateContentSize()) }

        if (state.slices.isNotEmpty() || state.unspentBudgets.isNotEmpty()) {
            item(key = "cat_label") { SectionLabel(stringResource(R.string.expenses_categories), hPad) }
            items(state.slices + state.unspentBudgets, key = { "cat_" + (it.categoryId ?: "none") }) { s ->
                val active = state.filterCategoryId != null && state.filterCategoryId == s.categoryId
                CategoryRow(
                    s, active = active, dimmed = state.filterCategoryId != null && !active,
                    onClick = { vm.filterCategory(s.categoryId) }, modifier = Modifier.padding(horizontal = gutter - 8.dp),
                )
            }
        }

        if (state.history.isNotEmpty()) {
            item(key = "hist_label") { SectionLabel(pluralStringResource(R.plurals.expenses_last_months, state.history.size, state.history.size), hPad) }
            item(key = "hist") { HistoryChart(state.history, vm::selectMonth, hPad) }
        }

        item(key = "tx_label") { SectionLabel(stringResource(R.string.expenses_transactions), hPad) }
        item(key = "search") { SearchField(state.query, vm::search, hPad) }
        state.filterCategoryId?.let { id ->
            val slice = (state.slices + state.unspentBudgets).firstOrNull { it.categoryId == id }
            item(key = "filter") {
                CategoryFilterChip(slice?.name ?: stringResource(R.string.label_category), slice?.let { Color(it.color) } ?: c.accent, { vm.filterCategory(id) }, hPad)
            }
        }
        if (state.days.isEmpty()) {
            item(key = "tx_empty") {
                val filtered = state.query.isNotBlank() || state.filterCategoryId != null
                EmptyState(
                    stringResource(if (filtered) R.string.expenses_none_matching else R.string.expenses_none_month), hPad,
                    action = if (filtered) null else stringResource(R.string.expenses_add), onAction = onAddExpense,
                )
            }
        }
        state.days.forEach { day ->
            item(key = "day_${day.date}") { DayHeader(day.date, day.total, hPad) }
            items(day.rows, key = { "tx_" + it.tx.id }) { row ->
                TxRowItem(
                    row, hPad,
                    onClick = { onEditTransaction(row.tx.id) },
                    onDelete = { vm.delete(row.tx) { t -> snack.undo(deletedText) { vm.restore(t) } } },
                )
            }
        }
    }
}
