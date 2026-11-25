package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class CalendarDay(val date: LocalDate, val inMonth: Boolean, val spent: Long, val income: Long, val count: Int)

data class CalendarMonth(
    val month: YearMonth,
    /** Whole weeks (rows of 7) covering the month, starting on the user's first day of week. */
    val weeks: List<List<CalendarDay>>,
    val spent: Long,
    val income: Long,
    /** The highest daily spending in the month, to scale the day shading. */
    val maxDaySpent: Long,
)

/** A calendar month of entries; days outside the month are shown greyed and carry no amounts. */
object CalendarMonths {
    /** [firstDayOfWeek]: 1 = Monday … 7 = Sunday (ISO). */
    fun of(txs: List<Transaction>, month: YearMonth, firstDayOfWeek: Int): CalendarMonth {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        val spent = HashMap<LocalDate, Long>()
        val income = HashMap<LocalDate, Long>()
        val count = HashMap<LocalDate, Int>()
        for (t in txs) {
            if (t.date.isBefore(first) || t.date.isAfter(last)) continue
            when (t.type) {
                TxType.EXPENSE -> spent.merge(t.date, t.amountCents, Long::plus)
                TxType.INCOME -> income.merge(t.date, t.amountCents, Long::plus)
                else -> Unit
            }
            count.merge(t.date, 1, Int::plus)
        }
        val dow = DayOfWeek.of(firstDayOfWeek.coerceIn(1, 7))
        val start = first.with(TemporalAdjusters.previousOrSame(dow))
        val end = last.with(TemporalAdjusters.nextOrSame(dow.minus(1)))
        val days = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.map { d ->
            val inMonth = YearMonth.from(d) == month
            CalendarDay(d, inMonth, if (inMonth) spent[d] ?: 0 else 0, if (inMonth) income[d] ?: 0 else 0, if (inMonth) count[d] ?: 0 else 0)
        }.toList()
        return CalendarMonth(month, days.chunked(7), spent.values.sum(), income.values.sum(), spent.values.maxOrNull() ?: 0)
    }

    /** Entries of one day, newest first. */
    fun dayEntries(txs: List<Transaction>, date: LocalDate): List<Transaction> =
        txs.filter { it.date == date }.sortedByDescending { it.createdAt }
}
