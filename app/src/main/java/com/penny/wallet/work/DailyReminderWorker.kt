package com.penny.wallet.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.penny.wallet.container
import com.penny.wallet.domain.Recurrence
import com.penny.wallet.domain.ReminderAction
import com.penny.wallet.notify.Notifications
import java.time.LocalDate

/** SPEC §5: reminders, auto-pay (with catch-up) and due-today notifications, once a day at the user's time. */
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
        return Result.success()
    }

    companion object { const val KEY_AUTOPAY_ONLY = "autopay_only" }
}
