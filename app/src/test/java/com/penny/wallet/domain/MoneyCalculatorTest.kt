package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.TxType.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class MoneyCalculatorTest {
    private val bank = Account("bank", "Bank", "bank", sortOrder = 0, createdAt = 0)
    private val cash = Account("cash", "Cash", "cash", sortOrder = 1, createdAt = 0)
    private val phone = Goal("g1", "Phone", "phone", 0, 90_000, sortOrder = 0, createdAt = 0)

    private val ledger = listOf(
        t(INCOME, 200_000, "bank"),
        t(EXPENSE, 12_345, "bank", cat = "food"),
        t(WITHDRAWAL, 5_000, "bank"),
        t(TRANSFER, 10_000, "bank", to = "cash"),
        t(ADJUSTMENT, -655, "cash"),
        t(SAVE, 30_000, "bank", goal = "g1"),
        t(UNSAVE, 5_000, "bank", goal = "g1"),
    )

    @Test fun accountBalance_followsFormula() {
        assertEquals(200_000 - 12_345 - 5_000 - 10_000, MoneyCalculator.accountBalance("bank", ledger))
        assertEquals(10_000L - 655, MoneyCalculator.accountBalance("cash", ledger))
    }

    @Test fun saveIsEarmarkOnly() {
        assertEquals(25_000, MoneyCalculator.goalBalance("g1", ledger))
        assertEquals(25_000, MoneyCalculator.allocatedFrom("bank", ledger))
        assertEquals(172_655L - 25_000, MoneyCalculator.availableInAccount("bank", ledger))
    }

    @Test fun summary_matchesIndividualFunctions() {
        val s = MoneyCalculator.summary(listOf(bank, cash), listOf(phone), ledger)
        assertEquals(172_655, s.account("bank").balance)
        assertEquals(147_655, s.account("bank").available)
        assertEquals(172_655L + 9_345, s.totalBalance)
        assertEquals(25_000, s.totalSaved)
        assertEquals(172_655L + 9_345 - 25_000, s.availableToSpend)
    }

    @Test fun summary_excludesArchivedFromTotal() {
        val s = MoneyCalculator.summary(listOf(bank, cash.copy(archived = true)), listOf(phone), ledger)
        assertEquals(172_655, s.totalBalance)
    }

    @Test fun overspendingGoesNegative() {
        val s = MoneyCalculator.summary(listOf(bank), emptyList(), listOf(t(INCOME, 100, "bank"), t(EXPENSE, 250, "bank")))
        assertEquals(-150, s.availableToSpend)
    }

    @Test fun adjustment_isDifference() {
        assertEquals(-2_345, MoneyCalculator.adjustmentFor(10_000, 12_345))
        assertEquals(5_000, MoneyCalculator.adjustmentFor(5_000, 0))
    }

    @Test fun goalProgressAndMonthly() {
        assertEquals(0.5f, MoneyCalculator.goalProgress(45_000, 90_000), 0.0001f)
        assertEquals(1.2f, MoneyCalculator.goalProgress(108_000, 90_000), 0.0001f)
        assertNull(MoneyCalculator.goalMonthlyNeeded(90_000, 0, d("2026-11-20"), null))
        // 20 Nov → 20 May = exactly 6 months
        assertEquals(15_000L, MoneyCalculator.goalMonthlyNeeded(90_000, 0, d("2026-11-20"), d("2027-05-20")))
        // 20 Nov → 21 May rounds up to 7 months; 90000/7 = 12857.14 → 12858
        assertEquals(12_858L, MoneyCalculator.goalMonthlyNeeded(90_000, 0, d("2026-11-20"), d("2027-05-21")))
        assertEquals(0L, MoneyCalculator.goalMonthlyNeeded(90_000, 95_000, d("2026-11-20"), d("2027-05-21")))
        // deadline passed → everything now
        assertEquals(40_000L, MoneyCalculator.goalMonthlyNeeded(90_000, 50_000, d("2026-11-20"), d("2026-11-01")))
    }

    @Test fun unsaveSplit_usesNetContributions() {
        val txs = listOf(t(SAVE, 300, "bank", goal = "g"), t(SAVE, 100, "cash", goal = "g"), t(UNSAVE, 50, "bank", goal = "g"))
        assertEquals(mapOf("bank" to 250L, "cash" to 100L), MoneyCalculator.unsaveSplit("g", txs))
    }

    @Test fun unsaveSplit_mixedContributionsSumExactly() {
        // 100 saved from bank, 100 from cash, 101 taken back to a third account → 99 left in goal
        val txs = listOf(t(SAVE, 100, "bank", goal = "g"), t(SAVE, 100, "cash", goal = "g"), t(UNSAVE, 101, "rev", goal = "g"))
        val split = MoneyCalculator.unsaveSplit("g", txs)
        assertEquals(99L, split.values.sum())
        assertEquals(setOf("bank", "cash"), split.keys)
    }

    @Test fun unsaveSplit_emptyGoal() {
        assertEquals(emptyMap<String, Long>(), MoneyCalculator.unsaveSplit("g", listOf(t(SAVE, 10, "bank", goal = "g"), t(UNSAVE, 10, "bank", goal = "g"))))
    }

    @Test fun periods_calendarMonth() {
        val p = MoneyCalculator.period(YearMonth.of(2026, 2), 1)
        assertEquals(d("2026-02-01"), p.start); assertEquals(d("2026-02-28"), p.end)
    }

    @Test fun periods_customStartDay() {
        val p = MoneyCalculator.period(YearMonth.of(2026, 11), 27)
        assertEquals(d("2026-11-27"), p.start); assertEquals(d("2026-12-26"), p.end)
        assertEquals(YearMonth.of(2026, 10), MoneyCalculator.periodOf(d("2026-11-20"), 27).label)
        assertEquals(YearMonth.of(2026, 11), MoneyCalculator.periodOf(d("2026-11-27"), 27).label)
        // start day 31 clamps: Feb period starts Feb 28, ends Mar 30
        val feb = MoneyCalculator.period(YearMonth.of(2026, 2), 31)
        assertEquals(d("2026-02-28"), feb.start); assertEquals(d("2026-03-30"), feb.end)
    }

    @Test fun monthSpentAndShares() {
        val txs = listOf(
            t(EXPENSE, 300, "bank", "2026-11-01", cat = "a"), t(EXPENSE, 100, "bank", "2026-11-30", cat = "b"),
            t(EXPENSE, 999, "bank", "2026-10-31", cat = "a"), t(INCOME, 5000, "bank", "2026-11-05"),
        )
        val p = MoneyCalculator.period(YearMonth.of(2026, 11), 1)
        assertEquals(400, MoneyCalculator.monthSpent(txs, p))
        assertEquals(mapOf("a" to 300L, "b" to 100L), MoneyCalculator.spentByCategory(txs, p))
        assertEquals(0.75f, MoneyCalculator.categoryShare(300, 400), 0.0001f)
        assertEquals(0f, MoneyCalculator.categoryShare(0, 0), 0f)
        val hist = MoneyCalculator.spentHistory(txs, p, 3, 1)
        assertEquals(listOf(0L, 999L, 400L), hist.map { it.second })
    }
}
