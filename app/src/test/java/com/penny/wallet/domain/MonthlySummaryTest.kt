package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class MonthlySummaryTest {
    @Test fun dueOnlyOnTheFirstDayOfABudgetingMonthAndOnce() {
        assertEquals(YearMonth.of(2026, 10), MonthlySummaries.due(d("2026-11-01"), 1, null))
        assertEquals(YearMonth.of(2026, 10), MonthlySummaries.due(d("2026-11-07"), 1, YearMonth.of(2026, 9))) // caught up
        assertNull(MonthlySummaries.due(d("2026-11-08"), 1, null)) // too late, wait for next month
        assertNull(MonthlySummaries.due(d("2026-11-01"), 1, YearMonth.of(2026, 10)))
        assertEquals(YearMonth.of(2026, 10), MonthlySummaries.due(d("2026-11-01"), 1, YearMonth.of(2026, 9)))
        // Month starting on the 27th: the summary of "October" (27 Oct – 26 Nov) is due on 27 Nov.
        assertEquals(YearMonth.of(2026, 10), MonthlySummaries.due(d("2026-11-27"), 27, null))
        assertNull(MonthlySummaries.due(d("2026-11-20"), 27, null))
        // Start day 31 in February: the period starts on the 28th.
        assertEquals(YearMonth.of(2027, 1), MonthlySummaries.due(d("2027-02-28"), 31, null))
    }

    @Test fun summaryOfTheEndedMonth() {
        val txs = listOf(
            t(TxType.EXPENSE, 400_00, "a", date = "2026-09-10", cat = "food"),
            t(TxType.EXPENSE, 300_00, "a", date = "2026-10-03", cat = "food"),
            t(TxType.EXPENSE, 200_00, "a", date = "2026-10-20", cat = "car"),
            t(TxType.EXPENSE, 150_00, "a", date = "2026-10-21", cat = "food"),
            t(TxType.INCOME, 2000_00, "a", date = "2026-10-01"),
            t(TxType.SAVE, 100_00, "a", date = "2026-10-02", goal = "g"),
            t(TxType.EXPENSE, 999_00, "a", date = "2026-11-01", cat = "car"),
        )
        val s = MonthlySummaries.of(txs, YearMonth.of(2026, 10), 1)!!
        assertEquals(650_00L, s.spent)
        assertEquals(2000_00L, s.income)
        assertEquals(100_00L, s.saved)
        assertEquals(63, s.changePercent) // 650 vs 400
        assertEquals("food", s.topCategoryId)
        assertEquals(450_00L, s.topCategorySpent)
    }

    @Test fun noComparisonWithoutAPreviousMonthAndNothingForAnEmptyMonth() {
        val txs = listOf(t(TxType.EXPENSE, 10_00, "a", date = "2026-10-03", cat = "x"))
        assertNull(MonthlySummaries.of(txs, YearMonth.of(2026, 10), 1)!!.changePercent)
        assertNull(MonthlySummaries.of(txs, YearMonth.of(2026, 8), 1))
        val down = listOf(t(TxType.EXPENSE, 200_00, "a", date = "2026-09-03", cat = "x"), t(TxType.EXPENSE, 50_00, "a", date = "2026-10-03", cat = "x"))
        assertEquals(-75, MonthlySummaries.of(down, YearMonth.of(2026, 10), 1)!!.changePercent)
    }

    @Test fun onlyTransfersSendNothingAndUncategorisedIsNotTheTopCategory() {
        val moves = listOf(t(TxType.TRANSFER, 100_00, "a", date = "2026-10-03", to = "b"))
        assertNull(MonthlySummaries.of(moves, YearMonth.of(2026, 10), 1))
        val mixed = listOf(t(TxType.EXPENSE, 300_00, "a", date = "2026-10-03"), t(TxType.EXPENSE, 200_00, "a", date = "2026-10-04", cat = "food"))
        val s = MonthlySummaries.of(mixed, YearMonth.of(2026, 10), 1)!!
        assertEquals("food", s.topCategoryId)
        assertEquals(200_00L, s.topCategorySpent)
    }
}
