package com.penny.wallet.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import com.penny.wallet.MainActivity
import com.penny.wallet.container
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TIMEOUT = 30_000L

/** Search from Expenses: typing finds entries from every month, not just the current one. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class SearchUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun has(t: String) = compose.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun tap(t: String) {
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText(t) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText(t) and hasClickAction()).onLast().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun searchFindsEntriesFromEveryMonth() {
        tap("Try with sample data")
        compose.waitUntil(TIMEOUT) { has("Bank") }
        val expected = runBlocking { compose.activity.container.transactions.all() }.count { it.note.orEmpty().contains("Supermarket", ignoreCase = true) }
        assertTrue("sample data has supermarket entries", expected > 0)

        tap("Expenses")
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasContentDescription("Search") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasContentDescription("Search") and hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(TIMEOUT) { has("Type a word") }
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("supermarket")
        val summary = if (expected == 1) "1 result" else "$expected results"
        compose.waitUntil(TIMEOUT) { has(summary) }
        assertTrue(has("Supermarket"))
    }
}
