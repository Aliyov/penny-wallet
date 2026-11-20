package com.penny.wallet.ui.recurring

import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.repo.Payment
import com.penny.wallet.domain.DueState
import com.penny.wallet.domain.Recurrence
import com.penny.wallet.notify.Notifications
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class RuleRow(val rule: RecurringExpense, val state: DueState, val category: Category?, val account: Account?)

data class RecurringState(
    val rows: List<RuleRow> = emptyList(),
    val monthlyTotal: Long = 0,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val defaultRemindDays: Int = 1,
)

/** What the edit sheet produces. The schedule anchors are validated here, not in the UI. */
data class RuleDraft(
    val id: String? = null,
    val name: String = "",
    val amountCents: Long = 0,
    val categoryId: String? = null,
    val accountId: String? = null,
    val frequency: Frequency = Frequency.MONTHLY,
    val dayOfMonth: Int = LocalDate.now().dayOfMonth,
    val month: Int = LocalDate.now().monthValue,
    val dayOfWeek: Int = LocalDate.now().dayOfWeek.value,
    val remindDaysBefore: Int = 1,
    val autoPay: Boolean = false,
    val active: Boolean = true,
)

fun RecurringExpense.toDraft() = RuleDraft(id, name, amountCents, categoryId, accountId, frequency,
    dayOfMonth ?: nextDueDate.dayOfMonth, month ?: nextDueDate.monthValue, dayOfWeek ?: nextDueDate.dayOfWeek.value,
    remindDaysBefore, autoPay, active)

class RecurringViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<RecurringState> = combine(
        c.recurring.observe(), c.categories.observe(), c.accounts.observe(), c.settings.prefs,
    ) { rules, cats, accounts, prefs ->
        val today = LocalDate.now()
        val cat = cats.associateBy { it.id }
        val acc = accounts.associateBy { it.id }
        val rows = rules.map { RuleRow(it, Recurrence.state(it, today), cat[it.categoryId], acc[it.accountId]) }
            .sortedWith(compareBy<RuleRow> { it.state == DueState.PAUSED }.thenBy { it.rule.nextDueDate })
        // Normalised monthly cost of active rules (weekly ×52/12, yearly /12), for the header.
        val monthly = rules.filter { it.active }.sumOf {
            when (it.frequency) { Frequency.MONTHLY -> it.amountCents; Frequency.WEEKLY -> it.amountCents * 52 / 12; Frequency.YEARLY -> it.amountCents / 12 }
        }
        RecurringState(rows, monthly, cats.filterNot { it.archived }, accounts.filterNot { it.archived }, prefs.defaultRemindDaysBefore)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringState())

    /**
     * Saves a draft. For a new rule — or when the schedule changed — nextDueDate becomes the first occurrence on/after today.
     * Returns (error, askNotificationPermission).
     */
    fun save(d: RuleDraft, done: (String?, Boolean) -> Unit) = viewModelScope.launch {
        val err = when {
            d.name.isBlank() -> "Give it a name"
            d.amountCents <= 0 -> "Enter an amount"
            d.categoryId == null -> "Pick a category"
            d.accountId == null -> "Pick an account"
            else -> null
        }
        if (err != null) return@launch done(err, false)
        val old = d.id?.let { c.recurring.get(it) }
        val rule = RecurringExpense(
            id = d.id ?: UUID.randomUUID().toString(), name = d.name.trim(), amountCents = d.amountCents,
            categoryId = d.categoryId!!, accountId = d.accountId!!, frequency = d.frequency,
            dayOfMonth = if (d.frequency == Frequency.WEEKLY) null else d.dayOfMonth.coerceIn(1, 31),
            month = if (d.frequency == Frequency.YEARLY) d.month.coerceIn(1, 12) else null,
            dayOfWeek = if (d.frequency == Frequency.WEEKLY) d.dayOfWeek.coerceIn(1, 7) else null,
            nextDueDate = LocalDate.now(), remindDaysBefore = d.remindDaysBefore.coerceIn(0, 7), autoPay = d.autoPay, active = d.active,
        )
        val scheduleSame = old != null && old.frequency == rule.frequency && old.dayOfMonth == rule.dayOfMonth &&
            old.month == rule.month && old.dayOfWeek == rule.dayOfWeek
        val next = if (scheduleSame) old!!.nextDueDate else Recurrence.firstOnOrAfter(rule, LocalDate.now())
        c.recurring.save(rule.copy(nextDueDate = next))
        var ask = false
        if (old == null) {
            val p = c.settings.current()
            if (!p.notificationPermissionAsked) { c.settings.update { it.copy(notificationPermissionAsked = true) }; ask = true }
        }
        done(null, ask)
    }

    fun setActive(r: RecurringExpense, active: Boolean) = viewModelScope.launch {
        // Resuming a long-paused rule shouldn't produce a backlog: jump to the next occurrence from today.
        val next = if (active && r.nextDueDate.isBefore(LocalDate.now())) Recurrence.firstOnOrAfter(r, LocalDate.now()) else r.nextDueDate
        c.recurring.save(r.copy(active = active, nextDueDate = next))
    }

    fun delete(r: RecurringExpense, done: (RecurringExpense) -> Unit) = viewModelScope.launch {
        c.recurring.delete(r); Notifications.cancel(c.context, r.id); done(r)
    }
    fun restore(r: RecurringExpense) = viewModelScope.launch { c.recurring.restore(r) }

    /** "Pay now": pays the current occurrence dated today if it's early, or on its due date otherwise. */
    fun payNow(r: RecurringExpense, done: (Payment?) -> Unit) = viewModelScope.launch {
        val today = LocalDate.now()
        val p = c.recurring.pay(r.id, expectedDue = r.nextDueDate, paidOn = if (r.nextDueDate.isAfter(today)) today else r.nextDueDate)
        Notifications.cancel(c.context, r.id)
        done(p)
    }
    fun undoPay(p: Payment) = viewModelScope.launch { c.recurring.undoPay(p) }
}
