package com.penny.wallet.platform.crash

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

/**
 * Crash reports without an SDK: the stack trace is written to a private file, and on the next start
 * Home offers to send it through the user's mail app, where they see exactly what is sent.
 * Only app/OS version, phone model and the stack trace go in; never accounts, amounts or notes.
 */
object CrashReporter {
    const val SUPPORT_EMAIL = "aliyov.code@proton.me"
    private const val MAX_CHARS = 6_000

    private fun file(context: Context) = File(context.filesDir, "last-crash.txt")

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching { file(app).writeText(format(e, appVersion(app), Instant.now().toString())) }
            previous?.uncaughtException(thread, e)
        }
    }

    fun pending(context: Context): String? = file(context).takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }

    fun clear(context: Context) {
        file(context).delete()
    }

    /** Opens the mail app with the report filled in. Returns false when there is no mail app. */
    fun send(context: Context, report: String): Boolean {
        val subject = "Penny Wallet crash report"
        val uri = Uri.parse("mailto:$SUPPORT_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(report)}")
        val intent = Intent(Intent.ACTION_SENDTO, uri)
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, report)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /** Exception messages are left out: they can quote user input. Class names and frames are enough to fix a crash. */
    internal fun format(e: Throwable, version: String, at: String, device: String = deviceLine()): String {
        val sw = StringWriter()
        PrintWriter(sw).use { pw ->
            pw.println("App: Penny Wallet $version")
            pw.println("Device: $device")
            pw.println("Time: $at")
            pw.println()
            var t: Throwable? = e
            var depth = 0
            while (t != null && depth < 5) {
                pw.println((if (depth == 0) "" else "Caused by: ") + t.javaClass.name)
                t.stackTrace.take(25).forEach { pw.println("    at $it") }
                t = t.cause
                depth++
            }
        }
        return sw.toString().take(MAX_CHARS)
    }

    private fun deviceLine() = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    private fun appVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
}
