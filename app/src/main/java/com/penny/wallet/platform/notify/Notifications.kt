package com.penny.wallet.platform.notify

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

/** Channel "Reminders" + the three notification kinds from SPEC §5. */
object Notifications {
    const val CHANNEL = "reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Upcoming and due recurring expenses"
            })
        }
    }

    fun canPost(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Stable id per rule so a newer notification replaces the older one. */
    fun idFor(ruleId: String) = ruleId.hashCode()

    private fun amount(r: RecurringExpense, p: UserPrefs) =
        if (p.hideAmounts) Formatters.HIDDEN else Formatters.money(r.amountCents, Formatters.of(p))

    fun remind(context: Context, r: RecurringExpense, due: LocalDate, today: LocalDate, p: UserPrefs) {
        val days = ChronoUnit.DAYS.between(today, due)
        val whenText = if (days == 1L) "Tomorrow" else "In $days days"
        post(context, r, base(context, p, "$whenText: ${r.name} ${amount(r, p)}", "Due ${Formatters.day(due, today)}"))
    }

    fun dueToday(context: Context, r: RecurringExpense, due: LocalDate, p: UserPrefs) {
        val b = base(context, p, "Due today: ${r.name} ${amount(r, p)}", "Tap Mark as paid once it's done")
            .addAction(0, "Mark as paid", receiver(context, MarkPaidReceiver.ACTION_PAY, r.id, due, null))
        post(context, r, b)
    }

    fun autoPaid(context: Context, r: RecurringExpense, due: LocalDate, txId: String, p: UserPrefs) {
        val b = base(context, p, "Paid automatically: ${r.name} ${amount(r, p)}", "Recorded for ${Formatters.day(due)}")
            .addAction(0, "Undo", receiver(context, MarkPaidReceiver.ACTION_UNDO, r.id, due, txId))
        post(context, r, b)
    }

    fun cancel(context: Context, ruleId: String) = NotificationManagerCompat.from(context).cancel(idFor(ruleId))

    /** Tinted with the user's accent from settings. */
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
