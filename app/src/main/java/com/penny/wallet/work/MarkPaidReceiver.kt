package com.penny.wallet.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.penny.wallet.container
import com.penny.wallet.notify.Notifications
import kotlinx.coroutines.launch
import java.time.LocalDate

/** "Mark as paid" and "Undo" actions from notifications. */
class MarkPaidReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ruleId = intent.getStringExtra(EXTRA_RULE) ?: return
        val due = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DUE, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE } ?: return)
        val c = context.container
        val pending = goAsync()
        c.appScope.launch {
            try {
                when (intent.action) {
                    ACTION_PAY -> c.recurring.pay(ruleId, expectedDue = due) // no-op on double tap
                    ACTION_UNDO -> intent.getStringExtra(EXTRA_TX)?.let { c.recurring.undoPay(it, due) }
                }
                Notifications.cancel(context, ruleId)
            } finally { pending.finish() }
        }
    }

    companion object {
        const val ACTION_PAY = "com.penny.wallet.PAY"
        const val ACTION_UNDO = "com.penny.wallet.UNDO_PAY"
        const val EXTRA_RULE = "rule"
        const val EXTRA_DUE = "due"
        const val EXTRA_TX = "tx"
    }
}
