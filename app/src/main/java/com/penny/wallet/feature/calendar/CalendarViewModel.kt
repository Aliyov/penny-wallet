package com.penny.wallet.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.core.ui.common.TxRow
import com.penny.wallet.core.ui.common.TxRows
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.domain.CalendarMonth
import com.penny.wallet.domain.CalendarMonths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

data class CalendarState(
    val month: YearMonth = YearMonth.now(),
    val calendar: CalendarMonth? = null,
    val selected: LocalDate? = null,
    val rows: List<TxRow> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val loading: Boolean = true,
)

private data class Ledger(val txs: List<Transaction>, val accounts: List<Account>, val categories: List<Category>, val goals: List<Goal>)

private data class Selection(val month: YearMonth, val day: LocalDate?)

class CalendarViewModel(private val c: AppContainer) : ViewModel() {
    private val selection = MutableStateFlow(LocalDate.now().let { Selection(YearMonth.from(it), it) })

    private val ledger = combine(c.transactions.observe(), c.accounts.observe(), c.categories.observe(), c.goals.observe(), ::Ledger)

    val state: StateFlow<CalendarState> = combine(ledger, c.settings.prefs, selection) { l, prefs, s ->
        val calendar = CalendarMonths.of(l.txs, s.month, prefs.firstDayOfWeek)
        val rows = s.day?.let { TxRows.build(CalendarMonths.dayEntries(l.txs, it), l.accounts, l.categories, l.goals) }.orEmpty()
        CalendarState(s.month, calendar, s.day, rows, LocalDate.now(), loading = false)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarState())

    fun prevMonth() = showMonth(selection.value.month.minusMonths(1))
    fun nextMonth() = showMonth(selection.value.month.plusMonths(1))

    fun selectDay(date: LocalDate) { selection.value = Selection(YearMonth.from(date), date) }

    private fun showMonth(m: YearMonth) {
        val today = LocalDate.now()
        selection.update { Selection(m, today.takeIf { YearMonth.from(it) == m }) }
    }

    // App scope: Calendar is a pushed screen, and the Undo snackbar outlives it.
    fun delete(t: Transaction, onDeleted: (Transaction) -> Unit) = c.appScope.launch {
        c.transactions.delete(t)
        withContext(Dispatchers.Main) { onDeleted(t) }
    }
    fun restore(t: Transaction) = c.appScope.launch { c.transactions.restore(t) }
}
