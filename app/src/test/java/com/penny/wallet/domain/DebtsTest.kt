package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.data.db.entities.DebtPayment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtsTest {
    private val today = d("2026-11-20")
    private fun debt(id: String, dir: DebtDirection, amount: Long, due: String? = null, remind: Boolean = false, settled: String? = null, created: Long = 0) =
        Debt(id, id, dir, amount, d("2026-10-01"), due?.let(::d), remind, settledOn = settled?.let(::d), createdAt = created)
    private fun pay(debt: String, amount: Long) = DebtPayment("p$debt$amount", debt, amount, today, createdAt = 0)

    @Test fun remainingAndSettled() {
        val s = Debts.status(debt("a", DebtDirection.I_OWE, 100_00), listOf(pay("a", 30_00), pay("a", 20_00), pay("b", 99_00)), today)
        assertEquals(50_00L, s.paid)
        assertEquals(50_00L, s.remaining)
        assertFalse(s.settled)
        assertTrue(Debts.status(debt("a", DebtDirection.I_OWE, 50_00), listOf(pay("a", 50_00)), today).settled)
    }

    @Test fun settledByHandEvenWithMoneyOpen() = assertTrue(Debts.status(debt("a", DebtDirection.OWED_TO_ME, 80_00, settled = "2026-11-01"), emptyList(), today).settled)

    @Test fun overpaidNeverGoesNegative() = assertEquals(0L, Debts.status(debt("a", DebtDirection.I_OWE, 10_00), listOf(pay("a", 15_00)), today).remaining)

    @Test fun overdueOnlyWhenOpenAndPastDue() {
        assertTrue(Debts.status(debt("a", DebtDirection.I_OWE, 10_00, due = "2026-11-19"), emptyList(), today).overdue)
        assertFalse(Debts.status(debt("a", DebtDirection.I_OWE, 10_00, due = "2026-11-20"), emptyList(), today).overdue)
        assertFalse(Debts.status(debt("a", DebtDirection.I_OWE, 10_00, due = "2026-11-19"), listOf(pay("a", 10_00)), today).overdue)
    }

    @Test fun totalsCountOnlyOpenRemaining() {
        val all = Debts.statuses(
            listOf(debt("a", DebtDirection.I_OWE, 100_00), debt("b", DebtDirection.OWED_TO_ME, 40_00), debt("c", DebtDirection.I_OWE, 5_00, settled = "2026-11-02")),
            listOf(pay("a", 25_00)), today,
        )
        assertEquals(DebtTotals(iOwe = 75_00, owedToMe = 40_00, open = 2), Debts.totals(all))
    }

    @Test fun openFirstSoonestDueFirstThenSettled() {
        val order = Debts.statuses(
            listOf(
                debt("settled", DebtDirection.I_OWE, 1, settled = "2026-11-10"),
                debt("noDue", DebtDirection.I_OWE, 1, created = 5),
                debt("late", DebtDirection.I_OWE, 1, due = "2026-12-20"),
                debt("soon", DebtDirection.OWED_TO_ME, 1, due = "2026-11-25"),
            ), emptyList(), today,
        ).map { it.debt.id }
        assertEquals(listOf("soon", "late", "noDue", "settled"), order)
    }

    @Test fun reminderOnlyOnTheDueDayForOpenDebtsWithReminderOn() {
        val all = Debts.statuses(
            listOf(
                debt("yes", DebtDirection.I_OWE, 10_00, due = "2026-11-20", remind = true),
                debt("off", DebtDirection.I_OWE, 10_00, due = "2026-11-20", remind = false),
                debt("tomorrow", DebtDirection.I_OWE, 10_00, due = "2026-11-21", remind = true),
                debt("paid", DebtDirection.I_OWE, 10_00, due = "2026-11-20", remind = true),
            ), listOf(pay("paid", 10_00)), today,
        )
        assertEquals(listOf("yes"), Debts.dueToday(all, today).map { it.debt.id })
    }
}
