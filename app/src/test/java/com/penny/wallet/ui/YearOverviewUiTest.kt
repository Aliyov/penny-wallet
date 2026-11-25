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
import com.penny.wallet.core.ui.moneyText
import com.penny.wallet.domain.YearOverviews
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

private const val TIMEOUT = 60_000L // CI runners are much slower than a laptop

/** Expenses → Year overview shows this year's spending as the domain computes it. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class YearOverviewUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun has(t: String) = compose.onAllNodesWithText(t, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
    private fun tap(t: String) {
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText(t) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText(t) and hasClickAction()).onLast().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun yearOverviewShowsTheYearsSpending() {
        tap("Try with sample data")
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("Bank").fetchSemanticsNodes().isNotEmpty() }
        val c = compose.activity.container
        compose.waitUntil(TIMEOUT) { runBlocking { c.transactions.all() }.size > 10 }
        val prefs = runBlocking { c.settings.current() }
        val year = YearOverviews.years(runBlocking { c.transactions.all() }, prefs.monthStartDay, LocalDate.now()).first()
        val spent = YearOverviews.of(runBlocking { c.transactions.all() }, year, prefs.monthStartDay, LocalDate.now()).spent
        assertTrue(spent > 0)

        tap("Expenses")
        tap("Year overview")
        Thread.sleep(1500) // let the push transition finish (Robolectric threw from a half-measured list otherwise)
        compose.waitForIdle()
        compose.waitUntil(TIMEOUT) { has("Top categories") }
        assertTrue(has(year.toString()))
        val expected = c.moneyText(spent)
        compose.waitUntil(TIMEOUT) { has(expected) }
    }
}
