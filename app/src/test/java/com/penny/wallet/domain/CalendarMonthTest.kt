package com.penny.wallet.domain

import com.penny.wallet.data.db.LargeData
import com.penny.wallet.data.db.entities.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.YearMonth

class CalendarMonthTest {
    private val txs = listOf(
        t(TxType.EXPENSE, 10_00, "a", date = "2026-11-03", cat = "c"),
        t(TxType.EXPENSE, 5_00, "a", date = "2026-11-03", cat = "c"),
        t(TxType.INCOME, 2000_00, "a", date = "2026-11-01"),
        t(TxType.TRANSFER, 50_00, "a", date = "2026-11-03", to = "b"),
        t(TxType.EXPENSE, 99_00, "a", date = "2026-10-31", cat = "c"),
        t(TxType.EXPENSE, 40_00, "a", date = "2026-11-30", cat = "c"),
    )

    @Test fun weeksStartOnTheChosenDayAndCoverTheMonth() {
        val monday = CalendarMonths.of(txs, YearMonth.of(2026, 11), 1)
        assertTrue(monday.weeks.all { it.size == 7 })
        assertTrue(monday.weeks.all { it.first().date.dayOfWeek == DayOfWeek.MONDAY })
        assertEquals(30, monday.weeks.flatten().count { it.inMonth })
        val sunday = CalendarMonths.of(txs, YearMonth.of(2026, 11), 7)
        assertEquals(DayOfWeek.SUNDAY, sunday.weeks.first().first().date.dayOfWeek)
        assertEquals(30, sunday.weeks.flatten().count { it.inMonth })
    }

    @Test fun dailyTotalsOnlyForTheMonth() {
        val m = CalendarMonths.of(txs, YearMonth.of(2026, 11), 1)
        val day3 = m.weeks.flatten().single { it.date == d("2026-11-03") }
        assertEquals(15_00L, day3.spent)
        assertEquals(3, day3.count)
        val oct31 = m.weeks.flatten().single { it.date == d("2026-10-31") }
        assertEquals(false, oct31.inMonth)
        assertEquals(0L, oct31.spent)
        assertEquals(55_00L, m.spent)
        assertEquals(2000_00L, m.income)
        assertEquals(40_00L, m.maxDaySpent)
    }

    @Test fun februaryInALeapYearAndAMonthStartingOnTheFirstDayOfWeek() {
        val feb = CalendarMonths.of(emptyList(), YearMonth.of(2028, 2), 1)
        assertEquals(29, feb.weeks.flatten().count { it.inMonth })
        val june2026 = CalendarMonths.of(emptyList(), YearMonth.of(2026, 6), 1) // 1 June 2026 is a Monday
        assertEquals(d("2026-06-01"), june2026.weeks.first().first().date)
        assertEquals(5, june2026.weeks.size)
        assertEquals(0L, june2026.maxDaySpent)
    }

    @Test fun dayEntriesNewestFirst() {
        val e = CalendarMonths.dayEntries(txs, d("2026-11-03"))
        assertEquals(3, e.size)
        assertTrue(e.zipWithNext().all { (a, b) -> a.createdAt >= b.createdAt })
    }

    @Test fun twentyThousandEntriesStayFast() {
        val data = LargeData()
        repeat(2) { CalendarMonths.of(data.transactions, YearMonth.of(2026, 11), 1) }
        val start = System.nanoTime()
        CalendarMonths.of(data.transactions, YearMonth.of(2026, 11), 1)
        assertTrue((System.nanoTime() - start) / 1_000_000 < 200)
    }
}
