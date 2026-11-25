package com.penny.wallet.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import com.penny.wallet.MainActivity
import com.penny.wallet.container
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

private const val TIMEOUT = 30_000L

@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class BillSuggestionUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun has(t: String) = compose.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun click(t: String) {
        compose.onAllNodes(hasText(t) and hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    /** Three monthly "Netflix" payments entered by hand, like a user would before setting up the bill. */
    private fun seed() = runBlocking {
        val c = compose.activity.container
        val acc = c.accounts.add("Bank", "bank")
        val cat = c.categories.all().first()
        val today = LocalDate.now()
        listOf(60L, 30L, 0L).forEach { days -> c.transactions.addExpense(12_99, acc.id, cat.id, today.minusDays(days), "Netflix") }
        cat.id
    }

    @Test fun addAsBillCreatesItAndHidesTheCard() {
        val catId = seed()
        compose.waitUntil(TIMEOUT) { has("Looks like a bill") }
        click("Add as bill")
        compose.waitUntil(TIMEOUT) { runBlocking { compose.activity.container.recurring.all() }.isNotEmpty() }
        val bill = runBlocking { compose.activity.container.recurring.all() }.single()
        assertEquals("Netflix", bill.name)
        assertEquals(catId, bill.categoryId)
        assertEquals(true, bill.dayOfMonth in 1..31)
        assertEquals(true, bill.nextDueDate.isAfter(LocalDate.now()))
        compose.waitUntil(TIMEOUT) { !has("Looks like a bill") }
    }

    @Test fun notABillHidesItForGood() {
        seed()
        compose.waitUntil(TIMEOUT) { has("Looks like a bill") }
        click("Not a bill")
        compose.waitUntil(TIMEOUT) { !has("Looks like a bill") }
        assertEquals(1, runBlocking { compose.activity.container.settings.current() }.dismissedBills.size)
        assertEquals(emptyList<Any>(), runBlocking { compose.activity.container.recurring.all() })
    }
}
