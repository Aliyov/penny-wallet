package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class CategoryBudgetTest {
    private fun cat(id: String, budget: Long?, archived: Boolean = false) =
        Category(id, id, "other", 0, archived = archived, sortOrder = 0, monthlyBudgetCents = budget)

    private val cats = listOf(cat("food", 300_00), cat("fun", null), cat("zero", 0), cat("old", 50_00, archived = true), cat("rent", 750_00))

    private fun statuses(startDay: Int, today: String, vararg spends: Pair<String?, Long>): Map<String, BudgetStatus> {
        val txs = spends.map { (c, a) -> t(TxType.EXPENSE, a, "a", date = today, cat = c) } +
            t(TxType.INCOME, 1_000_00, "a", date = today) + t(TxType.TRANSFER, 50_00, "a", date = today, to = "b")
        val period = MoneyCalculator.periodOf(d(today), startDay)
        return Budget.forCategories(cats, MoneyCalculator.spentByCategory(txs, period), period, d(today))
    }

    @Test fun onlyCategoriesWithAPositiveBudget() {
        assertEquals(setOf("food", "old", "rent"), statuses(1, "2026-11-10").keys)
    }

    @Test fun spendingCountsOnlyItsOwnCategory() {
        val s = statuses(1, "2026-11-10", "food" to 120_00, "fun" to 999_00, null to 40_00)
        assertEquals(120_00L, s.getValue("food").spent)
        assertEquals(180_00L, s.getValue("food").left)
        assertEquals(0L, s.getValue("rent").spent)
    }

    @Test fun exactlySpentIsLeftZeroNotOver() {
        val food = statuses(1, "2026-11-10", "food" to 300_00).getValue("food")
        assertEquals(0L, food.left)
        assertTrue(food.pace != BudgetPace.OVER)
        assertEquals(1f, food.fraction)
    }

    @Test fun overBudget() {
        val food = statuses(1, "2026-11-10", "food" to 300_01).getValue("food")
        assertEquals(-1L, food.left)
        assertEquals(BudgetPace.OVER, food.pace)
        assertEquals(0L, food.perDay)
    }

    @Test fun followsTheMonthStartDay() {
        val txs = listOf(
            t(TxType.EXPENSE, 100_00, "a", date = "2026-10-26", cat = "food"), // previous period
            t(TxType.EXPENSE, 40_00, "a", date = "2026-10-27", cat = "food"),
            t(TxType.EXPENSE, 60_00, "a", date = "2026-11-26", cat = "food"),
            t(TxType.EXPENSE, 500_00, "a", date = "2026-11-27", cat = "food"), // next period
        )
        val period = MoneyCalculator.periodOf(d("2026-11-10"), 27)
        val food = Budget.forCategories(cats, MoneyCalculator.spentByCategory(txs, period), period, d("2026-11-10")).getValue("food")
        assertEquals(100_00L, food.spent)
    }

    @Test fun pastMonthIsJudgedOnTheWholeMonth() {
        val period = MoneyCalculator.period(YearMonth.of(2026, 2), 1)
        val txs = listOf(t(TxType.EXPENSE, 200_00, "a", date = "2026-02-10", cat = "food"))
        val food = Budget.forCategories(cats, MoneyCalculator.spentByCategory(txs, period), period, d("2026-11-10")).getValue("food")
        assertEquals(100_00L, food.left)
        assertEquals(BudgetPace.ON_TRACK, food.pace)
        assertFalse(food.perDay < 0)
    }

    @Test fun hugeAmountsDontOverflow() {
        val big = listOf(cat("big", Long.MAX_VALUE / 4))
        val period = MoneyCalculator.periodOf(d("2026-11-10"), 1)
        val s = Budget.forCategories(big, mapOf("big" to Long.MAX_VALUE / 8), period, d("2026-11-10")).getValue("big")
        assertTrue(s.left > 0)
        assertTrue(s.fraction in 0f..1f)
        // Half the budget spent on day 10 of 30 is well ahead of an even pace.
        assertEquals(BudgetPace.FAST, s.pace)
    }

    @Test fun hugeAmountOnTrackStaysOnTrack() {
        val budget = Long.MAX_VALUE / 4
        val period = MoneyCalculator.periodOf(d("2026-11-10"), 1)
        val s = Budget.status(budget, budget / 10, period, d("2026-11-10"))!!
        assertEquals(BudgetPace.ON_TRACK, s.pace)
        assertEquals(budget - budget / 10, s.left)
    }
}
