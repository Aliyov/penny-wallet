package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.TxType.EXPENSE
import com.penny.wallet.data.db.entities.TxType.INCOME
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class BudgetInsightsTest {
    private val nov = MoneyCalculator.period(YearMonth.of(2026, 11), 1)

    @Test fun noBudgetNoStatus() = assertNull(Budget.status(0, 5_000, nov, d("2026-11-10")))

    @Test fun onTrackSplitsWhatIsLeftOverRemainingDays() {
        val s = Budget.status(30_000, 10_000, nov, d("2026-11-11"))!!
        assertEquals(20_000L, s.left)
        assertEquals(20, s.daysLeft)
        assertEquals(1_000L, s.perDay)
        assertEquals(BudgetPace.ON_TRACK, s.pace)
    }

    @Test fun fastWhenWellAheadOfEvenPace() {
        // day 5 of 30: even pace is 5 000; 6 000 is 20 % ahead
        assertEquals(BudgetPace.FAST, Budget.status(30_000, 6_000, nov, d("2026-11-05"))!!.pace)
        assertEquals(BudgetPace.ON_TRACK, Budget.status(30_000, 5_400, nov, d("2026-11-05"))!!.pace)
    }

    @Test fun overBudget() {
        val s = Budget.status(30_000, 31_000, nov, d("2026-11-20"))!!
        assertEquals(BudgetPace.OVER, s.pace)
        assertEquals(-1_000L, s.left)
        assertEquals(0L, s.perDay)
        assertEquals(1f, s.fraction)
    }

    @Test fun lastDayCountsAsOneDayLeft() = assertEquals(1, Budget.status(30_000, 0, nov, d("2026-11-30"))!!.daysLeft)

    @Test fun customStartDayPeriod() {
        val p = MoneyCalculator.periodOf(d("2026-11-30"), 27) // 27 Nov – 26 Dec
        val s = Budget.status(31_000, 0, p, d("2026-11-30"))!!
        assertEquals(27, s.daysLeft)
    }

    @Test fun insightComparesSameNumberOfDays() {
        val txs = listOf(
            t(EXPENSE, 10_000, "a", "2026-10-03", cat = "food"),
            t(EXPENSE, 90_000, "a", "2026-10-20", cat = "rent"), // after the same point last month: ignored
            t(EXPENSE, 8_000, "a", "2026-11-02", cat = "food"),
            t(EXPENSE, 4_000, "a", "2026-11-09", cat = "fun"),
            t(INCOME, 50_000, "a", "2026-11-01"),
        )
        val i = Insights.month(txs, nov, 1, d("2026-11-10"))!!
        assertEquals(20, i.changePercent)
        assertEquals("food", i.topCategoryId)
        assertEquals(8_000L, i.topCategoryCents)
    }

    @Test fun insightWithoutLastMonthHasNoChange() {
        val i = Insights.month(listOf(t(EXPENSE, 500, "a", "2026-11-02", cat = "food")), nov, 1, d("2026-11-10"))!!
        assertNull(i.changePercent)
    }

    @Test fun insightLessSpending() {
        val txs = listOf(t(EXPENSE, 10_000, "a", "2026-10-05"), t(EXPENSE, 7_500, "a", "2026-11-05"))
        assertEquals(-25, Insights.month(txs, nov, 1, d("2026-11-06"))!!.changePercent)
    }

    @Test fun nothingToSay() = assertNull(Insights.month(emptyList(), nov, 1, d("2026-11-10")))

    @Test fun shortPreviousMonthIsCappedAtItsEnd() {
        val mar = MoneyCalculator.period(YearMonth.of(2026, 3), 1)
        val txs = listOf(t(EXPENSE, 1_000, "a", "2026-02-28"), t(EXPENSE, 1_000, "a", "2026-03-31"))
        assertEquals(0, Insights.month(txs, mar, 1, d("2026-03-31"))!!.changePercent)
    }
}
