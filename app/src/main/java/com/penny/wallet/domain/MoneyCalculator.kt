package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class AccountMoney(val balance: Long, val allocated: Long) {
    val available: Long get() = balance - allocated
}

data class MoneySummary(
    val perAccount: Map<String, AccountMoney>,
    val perGoal: Map<String, Long>,
    val totalBalance: Long,
    val totalSaved: Long,
    /** Part of [totalSaved] added from outside the accounts (not earmarked from any balance). */
    val externalSaved: Long = 0,
) {
    val availableToSpend: Long get() = totalBalance - (totalSaved - externalSaved)
    fun account(id: String) = perAccount[id] ?: AccountMoney(0, 0)
    fun goal(id: String) = perGoal[id] ?: 0L
}

/** Inclusive date range of one budgeting "month" (respects the month start day). */
data class Period(val label: YearMonth, val start: LocalDate, val end: LocalDate) {
    operator fun contains(d: LocalDate) = !d.isBefore(start) && !d.isAfter(end)
    fun next(startDay: Int) = MoneyCalculator.period(label.plusMonths(1), startDay)
    fun prev(startDay: Int) = MoneyCalculator.period(label.minusMonths(1), startDay)
}

/**
 * Everything derives from the ledger; nothing here is stored.
 * Pure Kotlin, no Android dependencies, so it is unit-tested directly.
 */
object MoneyCalculator {

    /**
     * Pseudo account id for SAVE/UNSAVE rows whose money comes from (or goes) outside the
     * tracked accounts. It never matches a real account, so balances and availability ignore it.
     */
    const val EXTERNAL = "external"

    /** Signed effect of one transaction on account [accountId]'s balance. */
    fun effectOn(t: Transaction, accountId: String): Long {
        var e = 0L
        if (t.accountId == accountId) e += when (t.type) {
            TxType.INCOME, TxType.ADJUSTMENT -> t.amountCents
            TxType.EXPENSE, TxType.WITHDRAWAL, TxType.TRANSFER -> -t.amountCents
            TxType.SAVE, TxType.UNSAVE -> 0L // earmarks: money stays in the account
        }
        if (t.type == TxType.TRANSFER && t.toAccountId == accountId) e += t.amountCents
        return e
    }

    fun accountBalance(accountId: String, txs: List<Transaction>): Long = txs.sumOf { effectOn(it, accountId) }

    fun goalBalance(goalId: String, txs: List<Transaction>): Long = txs.sumOf {
        when {
            it.goalId != goalId -> 0L
            it.type == TxType.SAVE -> it.amountCents
            it.type == TxType.UNSAVE -> -it.amountCents
            else -> 0L
        }
    }

    /** Σ SAVE from a − Σ UNSAVE to a. */
    fun allocatedFrom(accountId: String, txs: List<Transaction>): Long = txs.sumOf {
        when {
            it.accountId != accountId -> 0L
            it.type == TxType.SAVE -> it.amountCents
            it.type == TxType.UNSAVE -> -it.amountCents
            else -> 0L
        }
    }

    fun availableInAccount(accountId: String, txs: List<Transaction>): Long =
        accountBalance(accountId, txs) - allocatedFrom(accountId, txs)

    /** Single pass over the ledger. Archived accounts are excluded from the total balance. */
    fun summary(accounts: List<Account>, goals: List<Goal>, txs: List<Transaction>): MoneySummary {
        val bal = HashMap<String, Long>()
        val alloc = HashMap<String, Long>()
        val goal = HashMap<String, Long>()
        for (t in txs) {
            when (t.type) {
                TxType.INCOME, TxType.ADJUSTMENT -> bal.merge(t.accountId, t.amountCents, Long::plus)
                TxType.EXPENSE, TxType.WITHDRAWAL -> bal.merge(t.accountId, -t.amountCents, Long::plus)
                TxType.TRANSFER -> {
                    bal.merge(t.accountId, -t.amountCents, Long::plus)
                    t.toAccountId?.let { bal.merge(it, t.amountCents, Long::plus) }
                }
                TxType.SAVE -> {
                    alloc.merge(t.accountId, t.amountCents, Long::plus)
                    t.goalId?.let { goal.merge(it, t.amountCents, Long::plus) }
                }
                TxType.UNSAVE -> {
                    alloc.merge(t.accountId, -t.amountCents, Long::plus)
                    t.goalId?.let { goal.merge(it, -t.amountCents, Long::plus) }
                }
            }
        }
        val per = accounts.associate { it.id to AccountMoney(bal[it.id] ?: 0, alloc[it.id] ?: 0) }
        return MoneySummary(
            perAccount = per,
            perGoal = goals.associate { it.id to (goal[it.id] ?: 0L) },
            totalBalance = accounts.filterNot { it.archived }.sumOf { per.getValue(it.id).balance },
            totalSaved = goals.sumOf { goal[it.id] ?: 0L },
            externalSaved = alloc[EXTERNAL] ?: 0L,
        )
    }

    /** Uncapped ratio; the ring caps at 1. */
    fun goalProgress(balance: Long, target: Long): Float =
        if (target <= 0) 0f else balance.toFloat() / target.toFloat()

    /** Whole months left until [deadline], rounding a partial month up; at least 1. */
    fun monthsUntil(today: LocalDate, deadline: LocalDate): Long {
        if (!deadline.isAfter(today)) return 1
        var m = ChronoUnit.MONTHS.between(today, deadline)
        if (today.plusMonths(m).isBefore(deadline)) m++
        return maxOf(1, m)
    }

    /** Cents per month needed to reach the target by the deadline (rounded up), or null without deadline. */
    fun goalMonthlyNeeded(target: Long, balance: Long, today: LocalDate, deadline: LocalDate?): Long? {
        deadline ?: return null
        val remaining = target - balance
        if (remaining <= 0) return 0
        val months = monthsUntil(today, deadline)
        return (remaining + months - 1) / months
    }

    /** Balance-setting adjustment: X − current. */
    fun adjustmentFor(targetBalance: Long, currentBalance: Long): Long = targetBalance - currentBalance

    /**
     * How to return a goal's money to its source accounts when deleting it.
     * Uses each account's net contribution to this goal; if those don't add up cleanly
     * (money taken back to a different account), splits proportionally to positive
     * contributions with largest-remainder rounding so cents always sum exactly.
     */
    fun unsaveSplit(goalId: String, txs: List<Transaction>): Map<String, Long> {
        val net = LinkedHashMap<String, Long>()
        for (t in txs) if (t.goalId == goalId) when (t.type) {
            TxType.SAVE -> net.merge(t.accountId, t.amountCents, Long::plus)
            TxType.UNSAVE -> net.merge(t.accountId, -t.amountCents, Long::plus)
            else -> {}
        }
        val total = net.values.sum()
        if (total <= 0) return emptyMap()
        if (net.values.all { it >= 0 }) return net.filterValues { it > 0 }
        val pos = net.filterValues { it > 0 }
        val weight = pos.values.sum()
        val base = pos.mapValues { (_, v) -> v * total / weight }.toMutableMap()
        var left = total - base.values.sum()
        val order = pos.entries.sortedByDescending { (it.value * total) % weight }.map { it.key }
        var i = 0
        while (left > 0) { base.merge(order[i % order.size], 1L, Long::plus); left--; i++ }
        return base.filterValues { it > 0 }
    }

    /** The budgeting month labelled [label], starting on [startDay] (clamped to month length). */
    fun period(label: YearMonth, startDay: Int): Period {
        val s = startDay.coerceIn(1, 31)
        val start = label.atDay(minOf(s, label.lengthOfMonth()))
        val next = label.plusMonths(1)
        val end = next.atDay(minOf(s, next.lengthOfMonth())).minusDays(1)
        return Period(label, start, end)
    }

    /** The period that contains [date]. Periods are labelled by the month in which they start. */
    fun periodOf(date: LocalDate, startDay: Int): Period {
        val ym = YearMonth.from(date)
        val p = period(ym, startDay)
        return if (date.isBefore(p.start)) period(ym.minusMonths(1), startDay) else p
    }

    fun monthSpent(txs: List<Transaction>, p: Period): Long =
        txs.sumOf { if (it.type == TxType.EXPENSE && it.date in p) it.amountCents else 0L }

    fun spentByCategory(txs: List<Transaction>, p: Period): Map<String?, Long> =
        txs.asSequence().filter { it.type == TxType.EXPENSE && it.date in p }
            .groupBy { it.categoryId }.mapValues { (_, v) -> v.sumOf { it.amountCents } }

    /** 0..1 share of a category in a month's spending. */
    fun categoryShare(spent: Long, monthTotal: Long): Float =
        if (monthTotal <= 0) 0f else spent.toFloat() / monthTotal.toFloat()

    /** Spending in each of the last [count] periods ending with [current], oldest first. */
    fun spentHistory(txs: List<Transaction>, current: Period, count: Int, startDay: Int): List<Pair<Period, Long>> =
        (count - 1 downTo 0).map { back ->
            val p = period(current.label.minusMonths(back.toLong()), startDay)
            p to monthSpent(txs, p)
        }
}
