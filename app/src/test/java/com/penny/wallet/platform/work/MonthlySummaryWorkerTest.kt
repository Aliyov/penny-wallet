package com.penny.wallet.platform.work

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import com.penny.wallet.container
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.domain.MoneyCalculator
import com.penny.wallet.ui.TestPennyApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The daily worker sends the monthly summary once, at the start of a budgeting month, and records it. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class)
class MonthlySummaryWorkerTest {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()
    private val c get() = ctx.container
    private val today = LocalDate.now()
    // Today starts a budgeting month, so the summary is due regardless of the real date.
    private val startDay = today.dayOfMonth.coerceAtMost(28)
    private val notifications get() = shadowOf(ctx.getSystemService(NotificationManager::class.java)).allNotifications

    @Before fun setUp() = runBlocking {
        shadowOf(ctx as android.app.Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val acc = c.accounts.add("Bank", "bank")
        val cat = c.categories.add("Food", "other", 0)
        val ended = MoneyCalculator.periodOf(today, startDay).label.minusMonths(1)
        val inEnded = MoneyCalculator.period(ended, startDay).start.plusDays(2)
        c.transactions.addExpense(42_00, acc.id, cat.id, inEnded, "Dinner")
        c.settings.update { UserPrefs(seeded = true, currencyCode = "EUR", monthStartDay = startDay, monthlySummary = true, lastSummaryMonth = null) }
    }

    private fun run() = runBlocking { TestListenableWorkerBuilder<DailyReminderWorker>(ctx).build().doWork() }

    @Test fun sendsOnceAndRemembersTheMonth() {
        assumeTrue("start days 29–31 aren't selectable; the domain test covers those months", today.dayOfMonth <= 28)
        run()
        assertEquals(1, notifications.size)
        val text = notifications.single().extras.getCharSequence("android.title").toString()
        assertTrue(text, text.contains("42"))
        val ended = MoneyCalculator.periodOf(today, startDay).label.minusMonths(1).toString()
        assertEquals(ended, runBlocking { c.settings.current() }.lastSummaryMonth)
        run()
        assertEquals(1, notifications.size)
    }

    @Test fun quietModeAndSwitchedOffSendNothing() {
        assumeTrue(today.dayOfMonth <= 28)
        runBlocking { c.settings.update { it.copy(quietMode = true) } }
        run()
        assertTrue(notifications.isEmpty())
        runBlocking { c.settings.update { it.copy(quietMode = false, monthlySummary = false, lastSummaryMonth = null) } }
        run()
        assertTrue(notifications.isEmpty())
    }
}
