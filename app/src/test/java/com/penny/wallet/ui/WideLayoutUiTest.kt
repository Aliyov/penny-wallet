package com.penny.wallet.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.penny.wallet.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TIMEOUT = 30_000L

/** Tablet in landscape: navigation rail instead of the bottom bar, and two panes on Home and Expenses. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w900dp-h700dp-land-xhdpi")
class WideLayoutUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun has(t: String) = compose.onAllNodesWithText(t).fetchSemanticsNodes().isNotEmpty()
    private fun tap(t: String) {
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText(t) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText(t) and hasClickAction()).onLast().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun railAndTwoPanesOnWideScreens() {
        tap("Try with sample data")
        compose.waitUntil(TIMEOUT) { has("Bank") && has("RECENT") }

        listOf("Home", "Expenses", "Savings", "Settings").forEach {
            compose.onAllNodes(hasText(it) and hasClickAction()).onFirst().assertIsDisplayed()
        }
        compose.onAllNodesWithText("Bank").onFirst().assertIsDisplayed()
        compose.onNodeWithText("RECENT").assertIsDisplayed()

        tap("Expenses")
        compose.waitUntil(TIMEOUT) { has("CATEGORIES") && has("TRANSACTIONS") }
        compose.onNodeWithText("CATEGORIES").assertIsDisplayed()
        compose.onNodeWithText("TRANSACTIONS").assertIsDisplayed()
        assertFalse("sample month has entries", has("No expenses this month."))
    }
}
