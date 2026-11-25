package com.penny.wallet.platform.work

import com.penny.wallet.domain.MonthlySummaries
import com.penny.wallet.domain.Debts
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.penny.wallet.container
import com.penny.wallet.domain.Recurrence
import com.penny.wallet.domain.ReminderAction
import com.penny.wallet.platform.notify.Notifications
import java.time.LocalDate

/** Reminders, auto-pay (with catch-up) and due-today notifications, once a day at the user's time. */
class DailyReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val prefs = c.settings.current()
        val autoPayOnly = inputData.getBoolean(KEY_AUTOPAY_ONLY, false)
        val today = LocalDate.now()
        val notify = !prefs.quietMode
        for (rule in c.recurring.all()) {
            for (action in Recurrence.actions(rule, today)) when (action) {
                is ReminderAction.AutoPay -> {
                    // idempotent: no-op if this occurrence was already paid
                    val payment = c.recurring.pay(rule.id, expectedDue = action.due) ?: continue
                    if (notify) Notifications.autoPaid(applicationContext, rule, action.due, payment.transaction.id, prefs)
                }
                is ReminderAction.DueToday -> if (notify && !autoPayOnly) Notifications.dueToday(applicationContext, rule, action.due, prefs)
                is ReminderAction.Remind -> if (notify && !autoPayOnly) Notifications.remind(applicationContext, rule, action.due, today, prefs)
            }
        }
        // Also from the catch-up run at app start, so a summary missed on day one still arrives.
        if (prefs.monthlySummary) sendMonthlySummary(c, prefs, today, notify)
        if (notify && !autoPayOnly) {
            Debts.dueToday(Debts.statuses(c.debts.all(), c.debts.payments(), today), today)
                .forEach { Notifications.debtDue(applicationContext, it, prefs) }
        }
        return Result.success()
    }

    /**
     * The month is claimed inside one settings write before anything is posted: the daily run and the catch-up run can
     * overlap, and only the one that claims it notifies. Claimed even when quiet or empty, so it never comes late.
     */
    private suspend fun sendMonthlySummary(c: com.penny.wallet.AppContainer, prefs: com.penny.wallet.data.prefs.UserPrefs, today: LocalDate, notify: Boolean) {
        var claimed: java.time.YearMonth? = null
        c.settings.update { p ->
            val lastSent = p.lastSummaryMonth?.let { runCatching { java.time.YearMonth.parse(it) }.getOrNull() }
            val month = MonthlySummaries.due(today, p.monthStartDay, lastSent)?.takeIf { p.monthlySummary }
            claimed = month
            if (month == null) p else p.copy(lastSummaryMonth = month.toString())
        }
        val month = claimed ?: return
        val summary = MonthlySummaries.of(c.transactions.all(), month, prefs.monthStartDay) ?: return
        if (!notify) return
        val top = summary.topCategoryId?.let { id -> c.categories.all().firstOrNull { it.id == id }?.name }
        Notifications.monthlySummary(applicationContext, summary, top, prefs)
    }

    companion object { const val KEY_AUTOPAY_ONLY = "autopay_only" }
}
