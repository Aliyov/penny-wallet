package com.penny.wallet.platform.notify

import com.penny.wallet.core.util.DateText
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.penny.wallet.MainActivity
import com.penny.wallet.R
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.platform.work.MarkPaidReceiver
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object Notifications {
    const val CHANNEL = "reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notif_channel_desc)
            })
        }
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Stable id per rule so a newer notification replaces the older one. */
    fun idFor(ruleId: String) = ruleId.hashCode()

    private fun amount(context: Context, r: RecurringExpense, p: UserPrefs) =
        if (p.hideAmounts) Formatters.HIDDEN else Formatters.money(r.amountCents, Formatters.of(p, DateText.of(context).locale))

    fun remind(context: Context, r: RecurringExpense, due: LocalDate, today: LocalDate, p: UserPrefs) {
        val days = ChronoUnit.DAYS.between(today, due)
        val title = context.resources.getQuantityString(R.plurals.notif_due_in_days, days.toInt(), days.toInt(), r.name, amount(context, r, p))
        post(context, r, base(context, p, title, context.getString(R.string.notif_due_on, DateText.of(context).day(due, today))))
    }

    fun dueToday(context: Context, r: RecurringExpense, due: LocalDate, p: UserPrefs) {
        val b = base(context, p, context.getString(R.string.notif_due_today, r.name, amount(context, r, p)), context.getString(R.string.notif_due_today_hint))
            .addAction(0, context.getString(R.string.notif_action_mark_paid), receiver(context, MarkPaidReceiver.ACTION_PAY, r.id, due, null))
        post(context, r, b)
    }

    fun autoPaid(context: Context, r: RecurringExpense, due: LocalDate, txId: String, p: UserPrefs) {
        val b = base(context, p, context.getString(R.string.notif_auto_paid, r.name, amount(context, r, p)), context.getString(R.string.notif_recorded_for, DateText.of(context).day(due)))
            .addAction(0, context.getString(R.string.action_undo), receiver(context, MarkPaidReceiver.ACTION_UNDO, r.id, due, txId))
        post(context, r, b)
    }

    fun cancel(context: Context, ruleId: String) = NotificationManagerCompat.from(context).cancel(idFor(ruleId))

    private fun base(context: Context, p: UserPrefs, title: String, text: String) =
        NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_penny)
            .setColor(p.accentArgb.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE),
            )

    private fun receiver(context: Context, action: String, ruleId: String, due: LocalDate, txId: String?): PendingIntent {
        val i = Intent(context, MarkPaidReceiver::class.java).setAction(action)
            .putExtra(MarkPaidReceiver.EXTRA_RULE, ruleId)
            .putExtra(MarkPaidReceiver.EXTRA_DUE, due.toEpochDay())
            .putExtra(MarkPaidReceiver.EXTRA_TX, txId)
        val code = (action + ruleId + due).hashCode()
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun post(context: Context, r: RecurringExpense, b: NotificationCompat.Builder) {
        if (!canPost(context)) return
        ensureChannel(context)
        @Suppress("MissingPermission") // checked in canPost
        NotificationManagerCompat.from(context).notify(idFor(r.id), b.build())
    }
}
