package com.penny.wallet.feature.expenses

import com.penny.wallet.R
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.data.prefs.ChartStyle
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.domain.Period
import com.penny.wallet.core.ui.common.TxRow
import com.penny.wallet.core.ui.common.TxRows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CategorySlice(val categoryId: String?, val name: String, val icon: String, val color: Long, val amount: Long, val share: Float)
data class MonthBar(val label: YearMonth, val amount: Long, val selected: Boolean)
data class DayGroup(val date: LocalDate, val total: Long, val rows: List<TxRow>)

data class ExpensesState(
    val period: Period? = null,
    val isCurrent: Boolean = true,
    val total: Long = 0,
    val slices: List<CategorySlice> = emptyList(),
    val history: List<MonthBar> = emptyList(),
    val days: List<DayGroup> = emptyList(),
    val filterCategoryId: String? = null,
    val query: String = "",
    val chart: ChartStyle = ChartStyle.DONUT,
)

private data class Filters(val month: YearMonth?, val category: String?, val query: String)

class ExpensesViewModel(private val c: AppContainer) : ViewModel() {
    /** month null = the period containing today. */
    private val filters = MutableStateFlow(Filters(null, null, ""))

    val state: StateFlow<ExpensesState> = combine(
        c.transactions.observe(), c.categories.observe(), c.accounts.observe(), c.settings.prefs, filters,
    ) { txs, cats, accounts, prefs, f ->
        val uncategorised = c.context.getString(R.string.expenses_uncategorised)
        val startDay = prefs.monthStartDay
        val current = MoneyCalculator.periodOf(LocalDate.now(), startDay)
        val period = f.month?.let { MoneyCalculator.period(it, startDay) } ?: current
        val total = MoneyCalculator.monthSpent(txs, period)
        val byId = cats.associateBy { it.id }
        val slices = MoneyCalculator.spentByCategory(txs, period).map { (id, amt) ->
            val cat = id?.let(byId::get)
            CategorySlice(id, cat?.name ?: uncategorised, cat?.icon ?: "other", cat?.color ?: 0xFF8E8E93, amt,
                MoneyCalculator.categoryShare(amt, total))
        }.sortedByDescending { it.amount }
        val history = MoneyCalculator.spentHistory(txs, period, prefs.historyMonths.coerceIn(3, 12), startDay)
            .map { (p, amt) -> MonthBar(p.label, amt, p.label == period.label) }
        val q = f.query.trim().lowercase()
        val expenses = txs.filter { t ->
            t.type == TxType.EXPENSE && t.date in period &&
                (f.category == null || t.categoryId == f.category) &&
                (q.isEmpty() || t.note.orEmpty().lowercase().contains(q) || byId[t.categoryId]?.name?.lowercase()?.contains(q) == true)
        }
        val rows = TxRows.build(expenses, accounts, cats, emptyList())
        val days = rows.groupBy { it.tx.date }.map { (d, r) -> DayGroup(d, r.sumOf { it.tx.amountCents }, r) }.sortedByDescending { it.date }
        ExpensesState(period, period.label == current.label, total, slices, history, days, f.category, f.query, prefs.defaultChart)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesState())

    fun prevMonth() = shift(-1)
    fun nextMonth() = shift(1)
    fun selectMonth(m: YearMonth) = filters.update { it.copy(month = m) }
    private fun shift(by: Long) {
        val cur = state.value.period?.label ?: return
        filters.update { it.copy(month = cur.plusMonths(by)) }
    }

    fun filterCategory(id: String?) = filters.update { it.copy(category = if (it.category == id) null else id) }
    fun search(q: String) = filters.update { it.copy(query = q) }

    fun delete(t: Transaction, onDeleted: (Transaction) -> Unit) = viewModelScope.launch { c.transactions.delete(t); onDeleted(t) }
    fun restore(t: Transaction) = viewModelScope.launch { c.transactions.restore(t) }
}
