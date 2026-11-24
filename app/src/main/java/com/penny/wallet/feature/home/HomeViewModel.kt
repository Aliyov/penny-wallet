package com.penny.wallet.feature.home

import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.domain.AccountMoney
import com.penny.wallet.domain.Budget
import com.penny.wallet.domain.BudgetStatus
import com.penny.wallet.domain.Insights
import com.penny.wallet.domain.MonthInsight
import com.penny.wallet.domain.DueState
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.domain.MoneySummary
import com.penny.wallet.domain.Period
import com.penny.wallet.domain.Recurrence
import com.penny.wallet.core.ui.common.TxRow
import com.penny.wallet.core.ui.common.TxRows
import com.penny.wallet.core.ui.common.UpcomingItem
import com.penny.wallet.core.ui.common.upcomingItems
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class AccountCard(val account: Account, val money: AccountMoney)

data class HomeState(
    val loading: Boolean = true,
    val summary: MoneySummary = MoneySummary(emptyMap(), emptyMap(), 0, 0),
    val period: Period? = null,
    val spentThisMonth: Long = 0,
    val daysLeft: Int = 0,
    val dueThisMonth: Long = 0,
    val accounts: List<AccountCard> = emptyList(),
    val upcoming: List<UpcomingItem> = emptyList(),
    val overdueCount: Int = 0,
    val recent: List<TxRow> = emptyList(),
    val backupWarning: Boolean = false,
    val firstRun: Boolean = false,
    val hasSampleData: Boolean = false,
    val currencyCode: String = "",
    val budget: BudgetStatus? = null,
    val insight: MonthInsight? = null,
    val topCategoryName: String? = null,
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<HomeState> = combine(
        c.accounts.observe(), c.categories.observe(), c.goals.observe(), c.transactions.observe(),
        combine(c.recurring.observe(), c.settings.prefs, c.setup.hasSampleData, ::Triple),
    ) { accounts, cats, goals, txs, (rules, prefs, hasSample) ->
        buildHomeState(accounts, cats, goals, txs, rules, prefs, hasSample, LocalDate.now(), System.currentTimeMillis())
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun delete(t: Transaction, onDeleted: (Transaction) -> Unit) = viewModelScope.launch {
        c.transactions.delete(t); onDeleted(t)
    }

    fun addAccount(name: String, icon: String, openingBalance: Long) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val a = c.accounts.add(name, icon)
        if (openingBalance != 0L) c.transactions.setBalance(a.id, openingBalance, LocalDate.now())
    }

    fun setCurrency(code: String) = viewModelScope.launch { c.settings.update { it.copy(currencyCode = code) } }

    fun addSampleData() = viewModelScope.launch { c.setup.addSampleData() }

    fun removeSampleData() = viewModelScope.launch { c.setup.removeSampleData() }

    fun restore(t: Transaction) = viewModelScope.launch { c.transactions.restore(t) }
}

/** Pure so the 20 000-entry performance test can time it. */
internal fun buildHomeState(
    accounts: List<Account>, cats: List<Category>, goals: List<Goal>, txs: List<Transaction>,
    rules: List<RecurringExpense>, prefs: UserPrefs, hasSample: Boolean, today: LocalDate, nowMillis: Long,
): HomeState {
    val summary = MoneyCalculator.summary(accounts, goals, txs)
    val period = MoneyCalculator.periodOf(today, prefs.monthStartDay)
    val upcoming = upcomingItems(rules, cats, today)
    val lastBackup = prefs.lastBackupAt
    val spent = MoneyCalculator.monthSpent(txs, period)
    val insight = Insights.month(txs, period, prefs.monthStartDay, today)
    return HomeState(
        loading = false,
        summary = summary,
        period = period,
        spentThisMonth = spent,
        daysLeft = ChronoUnit.DAYS.between(today, period.end).toInt() + 1,
        dueThisMonth = Recurrence.dueInPeriod(rules, period.end),
        accounts = accounts.filterNot { it.archived }.map { AccountCard(it, summary.account(it.id)) },
        upcoming = upcoming,
        overdueCount = upcoming.count { it.state == DueState.OVERDUE },
        recent = TxRows.build(txs.take(prefs.recentCount.coerceIn(3, 10)), accounts, cats, goals),
        backupWarning = txs.isNotEmpty() && !hasSample &&
            (lastBackup == null || nowMillis - lastBackup > 10L * 86_400_000L),
        firstRun = accounts.isEmpty(),
        hasSampleData = hasSample,
        currencyCode = prefs.currencyCode,
        budget = Budget.status(prefs.monthlyBudget, spent, period, today),
        insight = insight,
        topCategoryName = insight?.topCategoryId?.let { id -> cats.firstOrNull { it.id == id }?.name },
    )
}
