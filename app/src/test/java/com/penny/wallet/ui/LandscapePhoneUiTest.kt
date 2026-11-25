package com.penny.wallet.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performSemanticsAction
import com.penny.wallet.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TIMEOUT = 60_000L // CI runners are much slower than a laptop

/** A phone on its side is wide but short: it keeps the bottom bar, and every tab stays reachable. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w800dp-h360dp-land-xhdpi")
class LandscapePhoneUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun tap(t: String) {
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText(t) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText(t) and hasClickAction()).onLast().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun shortWideScreensKeepTheBottomBar() {
        tap("Try with sample data")
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("Bank").fetchSemanticsNodes().isNotEmpty() }
        // The + button sits in the bottom bar (with its glow), and Settings is displayed, not clipped off a rail.
        compose.onAllNodes(hasContentDescription("Add") and hasClickAction()).onFirst().assertIsDisplayed()
        val settings = compose.onAllNodes(hasText("Settings") and hasClickAction()).onFirst()
        settings.assertIsDisplayed()
        val tab = settings.fetchSemanticsNode().boundsInRoot
        val root = compose.onAllNodes(androidx.compose.ui.test.isRoot()).onFirst().fetchSemanticsNode().boundsInRoot
        assertTrue("Settings tab in the bottom bar, not a side rail", tab.top > root.height * 0.6f && tab.left > root.width * 0.5f)
        tap("Settings")
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("YOUR MONEY", ignoreCase = true).fetchSemanticsNodes().isNotEmpty() }
    }
}
