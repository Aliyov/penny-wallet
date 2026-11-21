package com.penny.wallet.platform.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.penny.wallet.data.prefs.UserPrefs
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object Scheduler {
    private const val DAILY = "daily-reminder"
    private const val CATCH_UP = "reminder-catch-up"
    private const val BACKUP = "scheduled-backup"

    /** Delay from [now] until the next [hour]:[minute]. */
    fun delayUntil(hour: Int, minute: Int, now: LocalDateTime = LocalDateTime.now()): Duration {
        var next = now.toLocalDate().atTime(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    /**
     * Daily reminder at the user's time. [replace] = true when the time setting changed (new initial delay);
     * otherwise KEEP so app starts / boots don't shift the schedule.
     */
    fun scheduleDaily(context: Context, p: UserPrefs, replace: Boolean = false) {
        val req = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntil(p.reminderHour, p.reminderMinute).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY, if (replace) ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE else ExistingPeriodicWorkPolicy.KEEP, req,
        )
    }

    /** App start / boot: record any missed auto-payments now (no reminder notifications, those wait for the daily run). */
    fun catchUp(context: Context) {
        val req = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInputData(workDataOf(DailyReminderWorker.KEY_AUTOPAY_ONLY to true)).build()
        WorkManager.getInstance(context).enqueueUniqueWork(CATCH_UP, ExistingWorkPolicy.REPLACE, req)
    }

    /** Periodic backup when a folder is set; cancelled otherwise. */
    fun scheduleBackup(context: Context, p: UserPrefs, replace: Boolean = false) {
        val wm = WorkManager.getInstance(context)
        if (p.backupFolderUri == null) { wm.cancelUniqueWork(BACKUP); return }
        val req = PeriodicWorkRequestBuilder<WeeklyBackupWorker>(p.backupFrequency.days, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        wm.enqueueUniquePeriodicWork(BACKUP, if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP, req)
    }
}
