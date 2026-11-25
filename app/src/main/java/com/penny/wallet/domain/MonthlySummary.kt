package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.time.LocalDate
import java.time.YearMonth

data class MonthlySummary(
    /** The budgeting month that just ended. */
    val month: YearMonth,
    val spent: Long,
    val income: Long,
    val saved: Long,
    /** Change against the month before, in whole percent; null when that month had no spending to compare with. */
    val changePercent: Int?,
    val topCategoryId: String?,
    val topCategorySpent: Long,
)

object MonthlySummaries {
    /** Days after a month starts that a missed summary (phone off, worker skipped) is still sent. */
    const val CATCH_UP_DAYS = 6L

    fun due(today: LocalDate, startDay: Int, lastSent: YearMonth?): YearMonth? {
        val current = MoneyCalculator.periodOf(today, startDay)
        if (today.isAfter(current.start.plusDays(CATCH_UP_DAYS))) return null
        val ended = current.label.minusMonths(1)
        return ended.takeIf { lastSent == null || lastSent.isBefore(ended) }
    }

    /** Null when nothing was spent, earned or saved, so a month of only transfers sends no "you spent 0". */
    fun of(txs: List<Transaction>, month: YearMonth, startDay: Int): MonthlySummary? {
        val p = MoneyCalculator.period(month, startDay)
        val before = MoneyCalculator.period(month.minusMonths(1), startDay)
        val inMonth = txs.filter { it.date in p }
        val spent = inMonth.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents }
        val income = inMonth.filter { it.type == TxType.INCOME }.sumOf { it.amountCents }
        val saved = inMonth.sumOf { when (it.type) { TxType.SAVE -> it.amountCents; TxType.UNSAVE -> -it.amountCents; else -> 0L } }
        if (spent == 0L && income == 0L && saved == 0L) return null
        val prev = MoneyCalculator.monthSpent(txs, before)
        // Uncategorised spending isn't a category to name; the top named one is more useful.
        val top = MoneyCalculator.spentByCategory(txs, p).filterKeys { it != null }.maxByOrNull { it.value }
        return MonthlySummary(
            month = month, spent = spent, income = income, saved = saved,
            changePercent = if (prev > 0) Math.round((spent - prev) * 100.0 / prev).toInt() else null,
            topCategoryId = top?.key, topCategorySpent = top?.value ?: 0,
        )
    }
}
