package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Frequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceTest {
    @Test fun monthly_clampsToLastDayAndReanchors() {
        val r = rule(dom = 31, next = "2026-01-31")
        val feb = Recurrence.advance(r)
        assertEquals(d("2026-02-28"), feb)
        assertEquals(d("2026-03-31"), Recurrence.next(r, feb))
        assertEquals(d("2028-02-29"), Recurrence.next(r, d("2028-01-31"))) // leap year
    }

    @Test fun weeklyAndYearly() {
        assertEquals(d("2026-11-27"), Recurrence.advance(rule(Frequency.WEEKLY, dom = null, dow = 5, next = "2026-11-20")))
        val y = rule(Frequency.YEARLY, dom = 29, month = 2, next = "2028-02-29")
        assertEquals(d("2029-02-28"), Recurrence.advance(y))
    }

    @Test fun firstOnOrAfter() {
        assertEquals(d("2026-12-14"), Recurrence.firstOnOrAfter(rule(dom = 14), d("2026-11-20")))
        assertEquals(d("2026-11-20"), Recurrence.firstOnOrAfter(rule(dom = 20), d("2026-11-20")))
        assertEquals(d("2026-11-30"), Recurrence.firstOnOrAfter(rule(dom = 31), d("2026-11-20")))
        // Friday on/after Friday 20 Nov 2026
        assertEquals(d("2026-11-20"), Recurrence.firstOnOrAfter(rule(Frequency.WEEKLY, dow = 5), d("2026-11-20")))
        assertEquals(d("2027-03-01"), Recurrence.firstOnOrAfter(rule(Frequency.YEARLY, dom = 1, month = 3), d("2026-11-20")))
    }

    @Test fun states() {
        val r = rule(next = "2026-11-14", before = 1)
        assertEquals(DueState.REMIND_TODAY, Recurrence.state(r, d("2026-11-13")))
        assertEquals(DueState.DUE_TODAY, Recurrence.state(r, d("2026-11-14")))
        assertEquals(DueState.OVERDUE, Recurrence.state(r, d("2026-11-15")))
        assertEquals(DueState.SCHEDULED, Recurrence.state(r, d("2026-11-01")))
        assertEquals(DueState.PAUSED, Recurrence.state(r.copy(active = false), d("2026-11-14")))
    }

    @Test fun actions_manual() {
        val r = rule(next = "2026-11-14", before = 1)
        assertEquals(listOf(ReminderAction.Remind(d("2026-11-14"))), Recurrence.actions(r, d("2026-11-13")))
        assertEquals(listOf(ReminderAction.DueToday(d("2026-11-14"))), Recurrence.actions(r, d("2026-11-14")))
        assertTrue(Recurrence.actions(r, d("2026-11-16")).isEmpty()) // overdue: no spam
    }

    @Test fun actions_autoPayCatchesUp() {
        val r = rule(dom = 14, next = "2026-09-14", auto = true, before = 0)
        val a = Recurrence.actions(r, d("2026-11-20"))
        assertEquals(listOf("2026-09-14", "2026-10-14", "2026-11-14").map { ReminderAction.AutoPay(d(it)) }, a)
    }

    @Test fun actions_remindAfterAutoPayForNextWeekly() {
        // weekly every Friday, pays today and the next occurrence is 7 days away; remind 7 days before → also remind
        val r = rule(Frequency.WEEKLY, dom = null, dow = 5, next = "2026-11-20", auto = true, before = 7)
        assertEquals(
            listOf(ReminderAction.AutoPay(d("2026-11-20")), ReminderAction.Remind(d("2026-11-27"))),
            Recurrence.actions(r, d("2026-11-20")),
        )
    }

    @Test fun paused_noActions() {
        assertTrue(Recurrence.actions(rule(active = false, next = "2026-11-14"), d("2026-11-14")).isEmpty())
    }

    @Test fun dueInPeriod_countsWeeklyMultipleAndOverdue() {
        val weekly = rule(Frequency.WEEKLY, dom = null, dow = 5, next = "2026-11-06", amount = 500) // overdue 6 Nov
        val monthly = rule(dom = 25, next = "2026-11-25", amount = 1000)
        val paused = rule(active = false, next = "2026-11-25", amount = 99999)
        // weekly: 6, 13, 20, 27 Nov → 4 × 500
        assertEquals(2000L + 1000L, Recurrence.dueInPeriod(listOf(weekly, monthly, paused), d("2026-11-30")))
    }

    @Test fun upcoming_next7Days() {
        val today = d("2026-11-20")
        val rs = listOf(rule(next = "2026-11-28"), rule(next = "2026-11-27").copy(id = "b"), rule(next = "2026-11-10").copy(id = "c"))
        assertEquals(listOf("c", "b"), Recurrence.upcoming(rs, today).map { it.id })
    }
}
