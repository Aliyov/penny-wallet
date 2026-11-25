package com.penny.wallet.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.R
import com.penny.wallet.core.ui.common.TxRowItem
import com.penny.wallet.core.ui.components.EmptyState
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.feature.expenses.DayHeader
import com.penny.wallet.feature.expenses.SearchField

@Composable
fun SearchScreen(onBack: () -> Unit, onEditTransaction: (String) -> Unit) {
    val vm = pennyViewModel { SearchViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val c = Penny.colors
    val hPad = Modifier.padding(horizontal = Penny.dims.gutter)
    val deletedText = stringResource(R.string.deleted)
    val focus = remember { FocusRequester() }
    var focusedOnce by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(Penny.dims.gap),
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = Penny.dims.gutter, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = c.textPrimary) }
                SearchField(query, vm::setQuery, Modifier.weight(1f).focusRequester(focus), placeholder = stringResource(R.string.search_field_hint))
            }
            // Inside the item: the field (and its FocusRequester) only exists once the lazy item is composed.
            LaunchedEffect(Unit) {
                if (!focusedOnce) runCatching { focus.requestFocus() }.onSuccess { focusedOnce = true }
            }
        }
        item(key = "filters") {
            SearchFilterChips(
                state, onPeriod = vm::setPeriod, onCustom = vm::setCustom, onAmount = vm::setAmount,
                onKind = vm::setKind, onClear = vm::clearFilters,
            )
        }
        when {
            state.loading -> Unit
            state.filter.isEmpty -> item(key = "hint") { EmptyState(stringResource(R.string.search_hint), hPad) }
            state.count == 0 -> item(key = "none") { EmptyState(stringResource(R.string.search_none), hPad) }
            else -> {
                item(key = "summary") { Text(summaryText(state), style = Penny.type.label, color = c.textSecondary, modifier = hPad) }
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
                if (state.more) {
                    item(key = "more") {
                        Text(
                            stringResource(R.string.search_more, SEARCH_ROW_LIMIT), style = Penny.type.caption, color = c.textSecondary,
                            textAlign = TextAlign.Center, modifier = hPad.fillMaxWidth().padding(vertical = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun summaryText(state: SearchState): String = listOfNotNull(
    pluralStringResource(R.plurals.search_results, state.count, state.count),
    if (state.spent != 0L) stringResource(R.string.search_spent, money(state.spent)) else null,
    if (state.received != 0L) stringResource(R.string.search_received, money(state.received)) else null,
).joinToString(" · ")
