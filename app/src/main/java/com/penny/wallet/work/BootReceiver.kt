package com.penny.wallet.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.penny.wallet.container
import kotlinx.coroutines.launch

/** Re-enqueues the daily reminder and the backup after reboot (SPEC §5), and catches up auto-pay. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val c = context.container
        val pending = goAsync()
        c.appScope.launch {
            try {
                val p = c.settings.current()
                Scheduler.scheduleDaily(context, p)
                Scheduler.scheduleBackup(context, p)
                Scheduler.catchUp(context)
            } finally { pending.finish() }
        }
    }
}
