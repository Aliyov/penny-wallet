package com.penny.wallet.ui.savings

import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.data.repo.DeletedGoal
import com.penny.wallet.data.repo.InsufficientAvailableException
import com.penny.wallet.domain.Formatters
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.ui.common.TxRow
import com.penny.wallet.ui.common.TxRows
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AccountChoice(val account: Account, val available: Long, /** this goal's net money from the account */ val inGoal: Long)

data class GoalState(
    val card: GoalCard? = null,
    val accounts: List<AccountChoice> = emptyList(),
    /** Suggested account for Add money (most available) and Take back (largest contributor). */
    val suggestedFrom: String? = null,
    val suggestedBack: String? = null,
    val rows: List<TxRow> = emptyList(),
    val missing: Boolean = false,
)

class GoalViewModel(private val c: AppContainer, private val id: String) : ViewModel() {
    val state: StateFlow<GoalState> = combine(
        c.goals.observe(), c.accounts.observe(), c.transactions.observe(), c.categories.observe(),
    ) { goals, accounts, txs, cats ->
        val g = goals.firstOrNull { it.id == id } ?: return@combine GoalState(missing = true)
        val s = MoneyCalculator.summary(accounts, goals, txs)
        val mine = txs.filter { it.goalId == id }
        val inGoal = HashMap<String, Long>()
        mine.forEach { t -> inGoal.merge(t.accountId, if (t.type == TxType.SAVE) t.amountCents else -t.amountCents, Long::plus) }
        val choices = accounts.filterNot { it.archived }.map { AccountChoice(it, s.account(it.id).available, inGoal[it.id] ?: 0) }
        GoalState(
            card = goalCard(g, s.goal(id)),
            accounts = choices,
            suggestedFrom = choices.maxByOrNull { it.available }?.account?.id,
            suggestedBack = choices.maxByOrNull { it.inGoal }?.takeIf { it.inGoal > 0 }?.account?.id ?: choices.firstOrNull()?.account?.id,
            rows = TxRows.build(mine, accounts, cats, goals),
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalState())

    /** Earmark money. Fails with a readable message if more than the account's available. */
    fun addMoney(amount: Long, accountId: String, done: (String?, Boolean) -> Unit) = viewModelScope.launch {
        val before = state.value.card?.reached ?: false
        val err = runCatching { c.transactions.save(amount, accountId, id, LocalDate.now()) }.exceptionOrNull()?.let(::message)
        // second flag: this deposit made the goal reach 100% → celebrate
        val nowReached = err == null && (state.value.card?.let { it.balance + amount >= it.goal.targetCents } ?: false)
        done(err, !before && nowReached)
    }

    fun takeBack(amount: Long, accountId: String, done: (String?) -> Unit) = viewModelScope.launch {
        done(runCatching { c.transactions.unsave(amount, accountId, id, LocalDate.now()) }.exceptionOrNull()?.let(::message))
    }

    fun update(g: Goal, done: (String?) -> Unit) = viewModelScope.launch {
        done(runCatching { require(g.name.isNotBlank()) { "Give it a name" }; require(g.targetCents > 0) { "Target must be above zero" }; c.goals.save(g) }
            .exceptionOrNull()?.message)
    }

    /** Returns money to the source accounts and deletes; the caller offers Undo via the repo (VM may be gone). */
    fun delete(done: (DeletedGoal?) -> Unit) = viewModelScope.launch { done(c.goals.delete(id)) }

    private fun message(e: Throwable) =
        if (e is InsufficientAvailableException) "Only ${Formatters.money(e.availableCents)} available" else e.message ?: "Something went wrong"
}
