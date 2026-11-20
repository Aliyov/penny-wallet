package com.penny.wallet.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.prefs.GoalLayout
import com.penny.wallet.data.prefs.GoalSort
import com.penny.wallet.domain.MoneyCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class GoalCard(
    val goal: Goal,
    val balance: Long,
    /** Uncapped ratio (1.2 = 120%). */
    val progress: Float,
    val remaining: Long,
    /** Cents/month to hit the deadline, null without a deadline. */
    val monthlyNeeded: Long?,
) {
    val reached: Boolean get() = balance >= goal.targetCents
    val ring: Float get() = progress.coerceIn(0f, 1f)
}

fun goalCard(g: Goal, balance: Long, today: LocalDate = LocalDate.now()) = GoalCard(
    g, balance, MoneyCalculator.goalProgress(balance, g.targetCents), (g.targetCents - balance).coerceAtLeast(0),
    MoneyCalculator.goalMonthlyNeeded(g.targetCents, balance, today, g.deadline),
)

data class SavingsState(
    val loading: Boolean = true,
    val totalSaved: Long = 0,
    val totalTarget: Long = 0,
    val goals: List<GoalCard> = emptyList(),
    val layout: GoalLayout = GoalLayout.GRID,
)

class SavingsViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<SavingsState> = combine(
        c.goals.observe(), c.accounts.observe(), c.transactions.observe(), c.settings.prefs,
    ) { goals, accounts, txs, prefs ->
        val s = MoneyCalculator.summary(accounts, goals, txs)
        val cards = goals.filterNot { it.archived }.map { goalCard(it, s.goal(it.id)) }
        val sorted = when (prefs.goalSort) {
            GoalSort.MANUAL -> cards.sortedBy { it.goal.sortOrder }
            GoalSort.PROGRESS -> cards.sortedByDescending { it.progress }
            GoalSort.DEADLINE -> cards.sortedWith(compareBy(nullsLast()) { it.goal.deadline })
        }
        SavingsState(false, s.totalSaved, cards.sumOf { it.goal.targetCents }, sorted, prefs.goalLayout)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavingsState())

    fun create(name: String, icon: String, color: Long, targetCents: Long, deadline: LocalDate?, done: (String?) -> Unit) =
        viewModelScope.launch {
            done(runCatching { require(name.isNotBlank()) { "Give it a name" }; c.goals.add(name, icon, color, targetCents, deadline) }.exceptionOrNull()?.message)
        }
}
