package com.penny.wallet.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.penny.wallet.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TIMEOUT = 60_000L // CI runners are much slower than a laptop

/** With animations on, the cold-start intro plays (here via its fallback, as Robolectric shows no system splash) and then lets taps through. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class IntroUiTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun hasText(t: String) = compose.onAllNodesWithText(t).fetchSemanticsNodes().isNotEmpty()

    @Test fun introEndsAndTheAppTakesTaps() {
        val app = ApplicationProvider.getApplicationContext<TestPennyApp>()
        Settings.Global.putFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        Robolectric.buildActivity(MainActivity::class.java, Intent(app, MainActivity::class.java)).setup()
        compose.waitUntil(TIMEOUT) { hasText("Try with sample data") }
        compose.mainClock.advanceTimeBy(4_000)
        compose.onNodeWithText("Try with sample data").performScrollTo().performClick()
        compose.waitUntil(TIMEOUT) { hasText("Bank") }
    }
}
