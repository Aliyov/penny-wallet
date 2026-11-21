package com.penny.wallet.platform.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.penny.wallet.container
import com.penny.wallet.data.backup.BackupResult

/** Scheduled backup (daily/weekly/monthly per settings, battery not low). Same code path as "Back up now". */
class WeeklyBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (val r = applicationContext.container.backup.backupNow()) {
        is BackupResult.Ok -> Result.success()
        // Missing folder/permission: retrying won't help; Home shows the >10 days banner instead.
        is BackupResult.Failed -> if (runAttemptCount < 2) Result.retry() else Result.failure()
    }
}
