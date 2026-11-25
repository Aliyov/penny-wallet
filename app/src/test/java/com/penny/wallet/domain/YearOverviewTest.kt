package com.penny.wallet.domain

import com.penny.wallet.data.db.LargeData
import com.penny.wallet.data.db.entities.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class YearOverviewTest {
    private val txs = listOf(
        t(TxType.EXPENSE, 100_00, "a", date = "2026-01-15", cat = "food"),
        t(TxType.EXPENSE, 50_00, "a", date = "2026-03-02", cat = "car"),
        t(TxType.EXPENSE, 30_00, "a", date = "2026-03-20", cat = "food"),
        t(TxType.INCOME, 2000_00, "a", date = "2026-03-01"),
        t(TxType.SAVE, 300_00, "a", date = "2026-03-05", goal = "g"),
        t(TxType.UNSAVE, 100_00, "a", date = "2026-04-05", goal = "g"),
        t(TxType.TRANSFER, 999_00, "a", date = "2026-03-05", to = "b"),
        t(TxType.EXPENSE, 7_00, "a", date = "2025-12-31", cat = "food"),
    )

    @Test fun twelveMonthsWithTotals() {
        val y = YearOverviews.of(txs, 2026, 1, d("2026-11-20"))
        assertEquals(12, y.months.size)
        assertEquals(YearMonth.of(2026, 1), y.months.first().label)
        assertEquals(MonthTotals(YearMonth.of(2026, 3), 80_00, 2000_00, 300_00), y.months[2])
        assertEquals(-100_00L, y.months[3].saved)
        assertEquals(180_00L, y.spent)
        assertEquals(2000_00L, y.income)
        assertEquals(200_00L, y.saved)
        assertEquals(listOf("food" to 130_00L, "car" to 50_00L), y.topCategories.map { it.categoryId to it.spent })
        assertEquals(YearMonth.of(2026, 1), y.biggestMonth!!.label)
        assertEquals(180_00L / 11, y.averageMonthlySpent)
    }

    @Test fun monthStartDayMovesEntriesBetweenMonths() {
        // Starting on the 27th: 2 and 20 March fall in "February" (27 Feb – 26 Mar); 31 Dec and 15 Jan in "December 2025".
        val y = YearOverviews.of(txs, 2026, 27, d("2026-11-20"))
        assertEquals(80_00L, y.months[1].spent)
        assertEquals(0L, y.months[2].spent)
        assertEquals(107_00L, YearOverviews.of(txs, 2025, 27, d("2026-11-20")).months[11].spent) // 31 Dec + 15 Jan
    }

    @Test fun pastFutureAndEmptyYears() {
        assertEquals(7_00L, YearOverviews.of(txs, 2025, 1, d("2026-11-20")).averageMonthlySpent) // only December has entries
        val future = YearOverviews.of(txs, 2027, 1, d("2026-11-20"))
        assertEquals(0L, future.averageMonthlySpent)
        assertNull(future.biggestMonth)
        assertTrue(future.topCategories.isEmpty())
        assertEquals(listOf(2026, 2025), YearOverviews.years(txs, 1, d("2026-11-20")))
        assertEquals(listOf(2026), YearOverviews.years(emptyList(), 1, d("2026-11-20")))
    }

    @Test fun twentyThousandEntriesStayFast() {
        val data = LargeData()
        repeat(2) { YearOverviews.of(data.transactions, 2026, 27, d("2026-11-24")) }
        val start = System.nanoTime()
        val y = YearOverviews.of(data.transactions, 2026, 27, d("2026-11-24"))
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("year overview took $ms ms", ms < 400)
        assertEquals(y.spent, y.months.sumOf { it.spent })
    }

    @Test fun averageStartsAtTheFirstMonthWithEntries() {
        val late = listOf(t(TxType.EXPENSE, 900_00, "a", date = "2026-09-10", cat = "x"), t(TxType.EXPENSE, 1100_00, "a", date = "2026-10-10", cat = "x"))
        assertEquals(1000_00L * 2 / 3, YearOverviews.of(late, 2026, 1, d("2026-11-20")).averageMonthlySpent) // Sep, Oct, Nov
    }

    @Test fun futureEntriesDoNotInflateTheAverage() {
        val txs = listOf(t(TxType.EXPENSE, 100_00, "a", date = "2026-11-05", cat = "x"), t(TxType.EXPENSE, 1200_00, "a", date = "2026-12-05", cat = "x"))
        assertEquals(100_00L, YearOverviews.of(txs, 2026, 1, d("2026-11-20")).averageMonthlySpent)
    }
}
