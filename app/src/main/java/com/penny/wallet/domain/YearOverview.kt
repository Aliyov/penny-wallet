package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.time.LocalDate
import java.time.YearMonth

data class MonthTotals(val label: YearMonth, val spent: Long, val income: Long, val saved: Long)

data class YearCategory(val categoryId: String?, val spent: Long, val share: Float)

data class YearOverview(
    val year: Int,
    /** Always 12, January first; months are the user's budgeting periods, labelled by the month they start in. */
    val months: List<MonthTotals>,
    val spent: Long,
    val income: Long,
    /** Put into goals minus taken out. */
    val saved: Long,
    /** Biggest first, at most [YearOverviews.TOP]. */
    val topCategories: List<YearCategory>,
    /** Over the started months since the first one with entries, so a year begun mid-way isn't averaged down; 0 if none. */
    val averageMonthlySpent: Long,
    val biggestMonth: MonthTotals?,
)

object YearOverviews {
    const val TOP = 5

    fun of(txs: List<Transaction>, year: Int, startDay: Int, today: LocalDate): YearOverview {
        val buckets = (1..12).associate { YearMonth.of(year, it) to LongArray(3) }
        val byCategory = HashMap<String?, Long>()
        for (t in txs) {
            val label = MoneyCalculator.periodOf(t.date, startDay).label
            if (label.year != year) continue
            val b = buckets.getValue(label)
            when (t.type) {
                TxType.EXPENSE -> { b[0] += t.amountCents; byCategory.merge(t.categoryId, t.amountCents, Long::plus) }
                TxType.INCOME -> b[1] += t.amountCents
                TxType.SAVE -> b[2] += t.amountCents
                TxType.UNSAVE -> b[2] -= t.amountCents
                else -> Unit
            }
        }
        val months = buckets.map { (label, b) -> MonthTotals(label, b[0], b[1], b[2]) }
        val spent = months.sumOf { it.spent }
        val current = MoneyCalculator.periodOf(today, startDay).label
        val lastStarted = when {
            year < current.year -> 12
            year == current.year -> current.monthValue
            else -> 0
        }
        val firstActive = months.indexOfFirst { it.spent != 0L || it.income != 0L || it.saved != 0L } + 1
        val started = if (firstActive == 0 || firstActive > lastStarted) 0 else lastStarted - firstActive + 1
        return YearOverview(
            year = year, months = months, spent = spent, income = months.sumOf { it.income }, saved = months.sumOf { it.saved },
            topCategories = byCategory.entries.sortedByDescending { it.value }.take(TOP)
                .map { YearCategory(it.key, it.value, MoneyCalculator.categoryShare(it.value, spent)) },
            // Same months on both sides: future-dated entries must not inflate the average.
            averageMonthlySpent = if (started == 0) 0 else months.subList(firstActive - 1, lastStarted).sumOf { it.spent } / started,
            biggestMonth = months.filter { it.spent > 0 }.maxByOrNull { it.spent },
        )
    }

    /** Years that have entries, newest first, always including the current one. */
    fun years(txs: List<Transaction>, startDay: Int, today: LocalDate): List<Int> =
        (txs.map { MoneyCalculator.periodOf(it.date, startDay).label.year } + MoneyCalculator.periodOf(today, startDay).label.year)
            .distinct().sortedDescending()
}
