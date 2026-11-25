package com.penny.wallet.platform.widget

import com.penny.wallet.R
import com.penny.wallet.domain.Budget
import com.penny.wallet.domain.BudgetPace
import com.penny.wallet.domain.MoneyCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetWidgetDataTest {
    private val names = mapOf(
        R.string.widget_budget_none to "none", R.string.widget_budget_title to "title", R.string.widget_budget_set to "set", R.string.widget_budget_left to "left",
        R.string.widget_budget_over to "over", R.string.widget_budget_per_day to "%s/day", R.string.widget_budget_of to "%s of %s",
    )
    private val text: (Int, Array<out Any>) -> String = { id, args -> names.getValue(id).format(*args) }
    private val money: (Long) -> String = { "€$it" }
    private val period = MoneyCalculator.period(YearMonth.of(2026, 2), 1)

    private fun data(budget: Long, spent: Long, today: LocalDate = LocalDate.of(2026, 2, 15), m: (Long) -> String = money, hide: Boolean = false) =
        budgetWidgetData(Budget.status(budget, spent, period, today), m, text, 0xFF00FF00, pureBlack = false, hideAmounts = hide)

    @Test fun noBudget() {
        val d = data(0, 5000)
        assertEquals("none", d.title)
        assertNull(d.amount)
        assertEquals("set", d.detail)
        assertNull(d.pace)
        assertNull(d.fraction)
    }

    @Test fun onTrack() {
        val d = data(28_000, 10_000)
        assertEquals("left", d.title)
        assertEquals("€18000", d.amount)
        assertEquals("€1285/day", d.detail) // 18 000 over 14 days left, today included
        assertEquals(BudgetPace.ON_TRACK, d.pace)
    }

    @Test fun spendingFast() = assertEquals(BudgetPace.FAST, data(28_000, 20_000).pace)

    @Test fun exactlySpentIsNotOver() {
        val d = data(28_000, 28_000)
        assertEquals("left", d.title)
        assertEquals("€0", d.amount)
        assertEquals("€0/day", d.detail)
        assertEquals(1f, d.fraction)
    }

    @Test fun overShowsPositiveOverage() {
        val d = data(28_000, 30_050)
        assertEquals("over", d.title)
        assertEquals("€2050", d.amount)
        assertEquals("€30050 of €28000", d.detail)
        assertEquals(BudgetPace.OVER, d.pace)
        assertEquals(1f, d.fraction)
    }

    @Test fun lastDayOfMonth() = assertEquals("€8000/day", data(28_000, 20_000, LocalDate.of(2026, 2, 28)).detail)

    @Test fun privacyModeHidesAmountsBarColourAndOverage() {
        val d = data(28_000, 30_050, m = { "•••" }, hide = true)
        assertEquals("title", d.title)
        assertEquals("•••", d.amount)
        assertNull(d.detail)
        assertNull(d.fraction)
        assertNull(d.pace)
    }
}
