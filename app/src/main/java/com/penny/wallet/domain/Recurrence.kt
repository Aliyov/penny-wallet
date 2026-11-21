package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.data.db.entities.RecurringExpense
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class DueState { PAUSED, SCHEDULED, REMIND_TODAY, DUE_TODAY, OVERDUE }

/** What the daily worker should do for a rule today. */
sealed interface ReminderAction {
    data class Remind(val due: LocalDate) : ReminderAction
    data class AutoPay(val due: LocalDate) : ReminderAction
    data class DueToday(val due: LocalDate) : ReminderAction
}

object Recurrence {

    fun clampDay(ym: YearMonth, day: Int): LocalDate = ym.atDay(day.coerceIn(1, ym.lengthOfMonth()))

    /** First occurrence of the rule's schedule on or after [from]. Uses the rule's anchors, not nextDueDate. */
    fun firstOnOrAfter(r: RecurringExpense, from: LocalDate): LocalDate = when (r.frequency) {
        Frequency.WEEKLY -> from.with(TemporalAdjusters.nextOrSame(DayOfWeek.of((r.dayOfWeek ?: from.dayOfWeek.value).coerceIn(1, 7))))
        Frequency.MONTHLY -> {
            val day = r.dayOfMonth ?: from.dayOfMonth
            val thisMonth = clampDay(YearMonth.from(from), day)
            if (!thisMonth.isBefore(from)) thisMonth else clampDay(YearMonth.from(from).plusMonths(1), day)
        }
        Frequency.YEARLY -> {
            val month = (r.month ?: from.monthValue).coerceIn(1, 12)
            val day = r.dayOfMonth ?: from.dayOfMonth
            val thisYear = clampDay(YearMonth.of(from.year, month), day)
            if (!thisYear.isBefore(from)) thisYear else clampDay(YearMonth.of(from.year + 1, month), day)
        }
    }

    /**
     * The occurrence after [date], by one period. Month/year rules re-anchor on dayOfMonth, so a rule on the 31st
     * goes Jan 31 → Feb 28 → Mar 31 (never drifts to the 28th).
     */
    fun next(r: RecurringExpense, date: LocalDate): LocalDate = when (r.frequency) {
        Frequency.WEEKLY -> date.plusWeeks(1)
        Frequency.MONTHLY -> clampDay(YearMonth.from(date).plusMonths(1), r.dayOfMonth ?: date.dayOfMonth)
        Frequency.YEARLY -> clampDay(YearMonth.from(date).plusYears(1), r.dayOfMonth ?: date.dayOfMonth)
    }

    /** nextDueDate advanced by one period (after a payment). */
    fun advance(r: RecurringExpense): LocalDate = next(r, r.nextDueDate)

    fun reminderDate(r: RecurringExpense): LocalDate = r.nextDueDate.minusDays(r.remindDaysBefore.coerceIn(0, 7).toLong())

    fun state(r: RecurringExpense, today: LocalDate): DueState = when {
        !r.active -> DueState.PAUSED
        r.nextDueDate.isBefore(today) -> DueState.OVERDUE
        r.nextDueDate == today -> DueState.DUE_TODAY
        r.remindDaysBefore > 0 && reminderDate(r) == today -> DueState.REMIND_TODAY
        else -> DueState.SCHEDULED
    }

    /**
     * Worker decisions for one rule. Auto-pay catches up every missed occurrence up to today
     * (phone off on the due day). Manual overdue rules produce nothing (no notification spam).
     */
    fun actions(r: RecurringExpense, today: LocalDate): List<ReminderAction> {
        if (!r.active) return emptyList()
        val out = mutableListOf<ReminderAction>()
        var due = r.nextDueDate
        if (r.autoPay) {
            var guard = 0
            while (!due.isAfter(today) && guard++ < 400) {
                out += ReminderAction.AutoPay(due)
                due = next(r, due)
            }
        } else if (due == today) {
            out += ReminderAction.DueToday(due)
            due = next(r, due)
        }
        if (r.remindDaysBefore > 0 && due.minusDays(r.remindDaysBefore.toLong()) == today) out += ReminderAction.Remind(due)
        return out
    }

    /** Unpaid occurrences of the rule from nextDueDate through [until] inclusive. */
    fun occurrencesUntil(r: RecurringExpense, until: LocalDate, max: Int = 60): List<LocalDate> {
        if (!r.active) return emptyList()
        val out = mutableListOf<LocalDate>()
        var d = r.nextDueDate
        while (!d.isAfter(until) && out.size < max) { out += d; d = next(r, d) }
        return out
    }

    /** "€X due this month": unpaid occurrences (incl. overdue) up to the end of the period. */
    fun dueInPeriod(rules: List<RecurringExpense>, periodEnd: LocalDate): Long =
        rules.sumOf { r -> r.amountCents * occurrencesUntil(r, periodEnd).size }

    /** Rules with an occurrence in the next [days] days (incl. overdue), sorted by date. */
    fun upcoming(rules: List<RecurringExpense>, today: LocalDate, days: Long = 7): List<RecurringExpense> =
        rules.filter { it.active && !it.nextDueDate.isAfter(today.plusDays(days)) }.sortedBy { it.nextDueDate }
}
