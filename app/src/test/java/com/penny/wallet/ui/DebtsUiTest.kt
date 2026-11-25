package com.penny.wallet.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
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
import com.penny.wallet.data.db.entities.DebtDirection
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TIMEOUT = 30_000L

/** Settings → Debts: add a debt, pay it back in full, and it shows as settled. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class DebtsUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun has(t: String) = compose.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()

    /** Semantics clicks: injected touches can land below bottom sheets that are still animating in. */
    private fun tap(t: String, last: Boolean = true) {
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText(t) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        val n = compose.onAllNodes(hasText(t) and hasClickAction())
        (if (last) n.onLast() else n.onFirst()).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun debts() = runBlocking { compose.activity.container.debts.all() }

    @Test fun addAndSettleADebt() {
        tap("Try with sample data")
        compose.waitUntil(TIMEOUT) { has("Bank") }
        tap("Settings")
        tap("Debts")
        tap("Add debt")
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Anna")
        tap("Owes you")
        tap("Amount")
        listOf("4", "0", "00").forEach { tap(it) }
        tap("Save") // amount sheet
        Thread.sleep(1500) // let the amount sheet finish closing so its Save isn't found again
        compose.waitForIdle()
        tap("Save") // debt sheet
        compose.waitUntil(TIMEOUT) { debts().isNotEmpty() }
        val d = debts().single()
        assertEquals("Anna", d.person)
        assertEquals(DebtDirection.OWED_TO_ME, d.direction)
        assertEquals(40_00L, d.amountCents)

        tap("Anna")
        tap("Record payment")
        Thread.sleep(1500) // let the payment sheet finish opening over the detail sheet
        compose.waitForIdle()
        tap("Save") // the payment sheet starts at what is still open
        compose.waitUntil(TIMEOUT) { debts().single().settledOn != null }
        assertNotNull(debts().single().settledOn)
        compose.waitUntil(TIMEOUT) { has("Settled") }
    }
}
