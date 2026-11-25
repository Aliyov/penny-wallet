package com.penny.wallet.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performSemanticsAction
import com.penny.wallet.MainActivity
import com.penny.wallet.container
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

private const val TIMEOUT = 30_000L

/** Expenses → Calendar: today's cell is selected and lists today's entries. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class CalendarUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun has(t: String) = compose.onAllNodesWithText(t, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
    private fun tap(t: String) {
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText(t) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText(t) and hasClickAction()).onLast().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun calendarOpensOnThisMonthWithItsDays() {
        tap("Try with sample data")
        val c = compose.activity.container
        compose.waitUntil(TIMEOUT) { has("Bank") }
        compose.waitUntil(TIMEOUT) { runBlocking { c.transactions.all() }.size > 10 }
        val withEntries = runBlocking { c.transactions.all() }.filter { it.date.month == LocalDate.now().month && it.date.year == LocalDate.now().year }
        assertTrue(withEntries.isNotEmpty())

        tap("Expenses")
        tap("Calendar")
        Thread.sleep(1500) // push transition
        compose.waitForIdle()
        val day = withEntries.first { it.note != null }.date
        val name = day.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US)
        val cell = hasContentDescription("$name ${day.dayOfMonth}:", substring = true) and hasClickAction() // "November 3: spent …"
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(cell).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(cell).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        val note = withEntries.firstOrNull { it.date == day && it.note != null }?.note
        assertTrue("sample entry with a note", note != null)
        compose.waitUntil(TIMEOUT) { has(note!!) }
    }
}
