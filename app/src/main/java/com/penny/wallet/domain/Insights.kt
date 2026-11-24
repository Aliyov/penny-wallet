package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class MonthInsight(
    /** Change against the same point of the previous period in percent, or null without spending to compare to. */
    val changePercent: Int?,
    val topCategoryId: String?,
    val topCategoryCents: Long,
)

object Insights {
    /** Below this change the months count as "about the same". */
    const val SAME_PERCENT = 5

    /**
     * Spending so far this period against the same number of days last period, plus the biggest category.
     * Null when there is nothing to say yet.
     */
    fun month(txs: List<Transaction>, period: Period, startDay: Int, today: LocalDate): MonthInsight? {
        val upTo = minOf(today, period.end)
        val dayIndex = ChronoUnit.DAYS.between(period.start, upTo)
        val prev = period.prev(startDay)
        val prevUpTo = minOf(prev.start.plusDays(dayIndex), prev.end)
        var now = 0L
        var before = 0L
        val byCategory = mutableMapOf<String, Long>()
        for (t in txs) {
            if (t.type != TxType.EXPENSE) continue
            if (!t.date.isBefore(period.start) && !t.date.isAfter(upTo)) {
                now += t.amountCents
                t.categoryId?.let { byCategory[it] = (byCategory[it] ?: 0L) + t.amountCents }
            } else if (!t.date.isBefore(prev.start) && !t.date.isAfter(prevUpTo)) {
                before += t.amountCents
            }
        }
        val change = if (before > 0) ((now - before) * 100 / before).toInt() else null
        val top = byCategory.maxByOrNull { it.value }
        if (now == 0L && change == null) return null
        return MonthInsight(change, top?.key, top?.value ?: 0L)
    }
}
