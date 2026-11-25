package com.penny.wallet.ui

import android.content.Intent
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.penny.wallet.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

private const val TIMEOUT = 60_000L // CI runners are much slower than a laptop

/** App shortcuts, the Quick Settings tile and the widget all reach Quick add through MainActivity's intent actions. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class LaunchActionsUiTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun hasText(t: String) = compose.onAllNodesWithText(t).fetchSemanticsNodes().isNotEmpty()

    private fun startWithSampleData(): ActivityController<MainActivity> {
        val controller = Robolectric.buildActivity(MainActivity::class.java, Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)).setup()
        compose.waitUntil(TIMEOUT) { hasText("Try with sample data") }
        compose.onNodeWithText("Try with sample data").performScrollTo().performClick()
        compose.waitUntil(TIMEOUT) { hasText("Bank") }
        return controller
    }

    /** The camera starts after a database read on a background thread, which waitForIdle doesn't track. */
    private fun awaitStarted(controller: ActivityController<MainActivity>): Intent? {
        val end = System.currentTimeMillis() + TIMEOUT
        while (System.currentTimeMillis() < end) {
            compose.waitForIdle()
            shadowOf(controller.get()).nextStartedActivityForResult?.let { return it.intent }
            Thread.sleep(50)
        }
        return null
    }

    private fun send(controller: ActivityController<MainActivity>, action: String) {
        controller.newIntent(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).setAction(action))
    }

    @Test fun addExpenseOpensQuickAddWithoutCamera() {
        val controller = startWithSampleData()
        send(controller, MainActivity.ACTION_QUICK_ADD)
        compose.waitUntil(TIMEOUT) { hasText("Scan receipt") }
        compose.waitForIdle()
        assertNull(shadowOf(controller.get()).nextStartedActivityForResult)
    }

    @Test fun scanReceiptOpensQuickAddAndStartsTheCamera() {
        val controller = startWithSampleData()
        send(controller, MainActivity.ACTION_SCAN_RECEIPT)
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, awaitStarted(controller)?.action)
        compose.waitUntil(TIMEOUT) { hasText("Scan receipt") }
    }

    @Test fun aRequestIsHandledOnceAndTapsStillWorkAfterRecreation() {
        val controller = startWithSampleData()
        send(controller, MainActivity.ACTION_SCAN_RECEIPT)
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, awaitStarted(controller)?.action)
        controller.recreate()
        compose.waitUntil(TIMEOUT) { hasText("Bank") }
        compose.waitForIdle()
        assertNull("scan replayed after recreation", shadowOf(controller.get()).nextStartedActivityForResult)
        send(controller, MainActivity.ACTION_SCAN_RECEIPT)
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, awaitStarted(controller)?.action)
    }

    @Test fun actionsAreIgnoredBeforeTheFirstAccount() {
        val controller = Robolectric.buildActivity(MainActivity::class.java, Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)).setup()
        compose.waitUntil(TIMEOUT) { hasText("Add your first account") }
        send(controller, MainActivity.ACTION_SCAN_RECEIPT)
        compose.waitForIdle()
        assertEquals(false, hasText("Scan receipt"))
        assertNull(shadowOf(controller.get()).nextStartedActivityForResult)
    }
}
