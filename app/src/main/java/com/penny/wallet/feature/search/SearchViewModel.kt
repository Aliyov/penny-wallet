package com.penny.wallet.feature.search

import com.penny.wallet.R
import com.penny.wallet.data.db.entities.TxType
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.core.ui.common.TxRow
import com.penny.wallet.core.ui.common.TxRows
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.domain.Search
import com.penny.wallet.domain.SearchFilter
import com.penny.wallet.domain.SearchKind
import com.penny.wallet.feature.expenses.DayGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class SearchPeriod { ANY, THIS_MONTH, LAST_3_MONTHS, THIS_YEAR, LAST_YEAR, CUSTOM }

data class SearchState(
    val filter: SearchFilter = SearchFilter(),
    val period: SearchPeriod = SearchPeriod.ANY,
    val days: List<DayGroup> = emptyList(),
    val more: Boolean = false,
    val count: Int = 0,
    val spent: Long = 0,
    val received: Long = 0,
    val loading: Boolean = true,
)

private data class Ledger(val txs: List<Transaction>, val accounts: List<Account>, val categories: List<Category>, val goals: List<Goal>)

internal const val SEARCH_ROW_LIMIT = 300

@OptIn(FlowPreview::class)
class SearchViewModel(private val c: AppContainer) : ViewModel() {
    // Rows without a note are titled by their kind ("Transfer"), so the kind must be findable too.
    private val typeLabels = mapOf(
        TxType.EXPENSE to R.string.type_expense, TxType.INCOME to R.string.type_income, TxType.TRANSFER to R.string.type_transfer,
        TxType.WITHDRAWAL to R.string.ui_tx_withdrawal, TxType.ADJUSTMENT to R.string.ui_tx_balance_adjusted,
    ).mapValues { c.context.getString(it.value) }

    private val queryInput = MutableStateFlow("")
    val query: StateFlow<String> = queryInput.asStateFlow()

    /** Query lives in [queryInput]; from/to here are only used for [SearchPeriod.CUSTOM]. */
    private val filter = MutableStateFlow(SearchFilter())
    private val period = MutableStateFlow(SearchPeriod.ANY)

    private val ledger = combine(c.transactions.observe(), c.accounts.observe(), c.categories.observe(), c.goals.observe(), ::Ledger)

    val state: StateFlow<SearchState> = combine(ledger, c.settings.prefs, filter, period, queryInput.debounce(150)) { l, prefs, f, p, q ->
        val effective = resolve(f.copy(query = q), p, prefs.monthStartDay, LocalDate.now())
        if (effective.isEmpty) return@combine SearchState(effective, p, loading = false)
        val result = Search.run(l.txs, l.accounts, l.categories, l.goals, effective, typeLabels)
        val rows = TxRows.build(result.rows.take(SEARCH_ROW_LIMIT), l.accounts, l.categories, l.goals)
        val days = rows.groupBy { it.tx.date }.map { (d, r) -> DayGroup(d, dayTotal(r), r) }
        SearchState(effective, p, days, result.rows.size > SEARCH_ROW_LIMIT, result.rows.size, result.spent, result.received, loading = false)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    fun setQuery(q: String) { queryInput.value = q }

    fun setPeriod(p: SearchPeriod) {
        period.value = p
        if (p != SearchPeriod.CUSTOM) filter.update { it.copy(from = null, to = null) }
    }

    fun setCustom(from: LocalDate, to: LocalDate) {
        filter.update { it.copy(from = minOf(from, to), to = maxOf(from, to)) }
        period.value = SearchPeriod.CUSTOM
    }

    fun setAmount(min: Long?, max: Long?) {
        val lo = if (min != null && max != null) minOf(min, max) else min
        val hi = if (min != null && max != null) maxOf(min, max) else max
        filter.update { it.copy(minCents = lo, maxCents = hi) }
    }

    fun setKind(k: SearchKind) = filter.update { it.copy(kind = k) }

    fun clearFilters() {
        filter.value = SearchFilter()
        period.value = SearchPeriod.ANY
    }

    // App scope: Search is a pushed screen, and the Undo snackbar outlives it.
    fun delete(t: Transaction, onDeleted: (Transaction) -> Unit) = c.appScope.launch {
        c.transactions.delete(t)
        withContext(Dispatchers.Main) { onDeleted(t) }
    }
    fun restore(t: Transaction) = c.appScope.launch { c.transactions.restore(t) }
}

/** What was spent that day, like the day headers in Expenses; income is in the summary line. */
private fun dayTotal(rows: List<TxRow>): Long = rows.filter { it.tx.type == TxType.EXPENSE }.sumOf { it.tx.amountCents }

/** THIS_MONTH follows the user's budgeting period; the other presets are calendar based. */
internal fun resolve(f: SearchFilter, p: SearchPeriod, monthStartDay: Int, today: LocalDate): SearchFilter = when (p) {
    SearchPeriod.ANY -> f.copy(from = null, to = null)
    SearchPeriod.CUSTOM -> f
    SearchPeriod.THIS_MONTH -> MoneyCalculator.periodOf(today, monthStartDay).let { f.copy(from = it.start, to = it.end) }
    SearchPeriod.LAST_3_MONTHS -> f.copy(from = today.withDayOfMonth(1).minusMonths(2), to = today.withDayOfMonth(today.lengthOfMonth()))
    SearchPeriod.THIS_YEAR -> f.copy(from = today.withDayOfYear(1), to = today.withDayOfYear(today.lengthOfYear()))
    SearchPeriod.LAST_YEAR -> today.minusYears(1).let { y -> f.copy(from = y.withDayOfYear(1), to = y.withDayOfYear(y.lengthOfYear())) }
}
