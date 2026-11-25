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
import com.penny.wallet.data.db.entities.Transaction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TIMEOUT = 60_000L // CI runners are much slower than a laptop

/** Split one receipt in Quick add, then check that delete and undo act on every part. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class SplitExpenseUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun has(t: String) = compose.onAllNodesWithText(t).fetchSemanticsNodes().isNotEmpty()

    /** Semantics clicks: injected touches can land below bottom sheets that are still animating in. */
    private fun tap(t: String, last: Boolean = true) {
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText(t) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        val nodes = compose.onAllNodes(hasText(t) and hasClickAction())
        (if (last) nodes.onLast() else nodes.onFirst()).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun splits(): List<Transaction> = runBlocking { compose.activity.container.transactions.all() }.filter { it.splitId != null }

    @Test fun splitSaveDeleteAndUndo() {
        tap("Try with sample data")
        compose.waitUntil(TIMEOUT) { has("Bank") }
        val before = runBlocking { compose.activity.container.transactions.all() }.size

        compose.onAllNodes(hasContentDescription("Add") and hasClickAction()).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(TIMEOUT) { has("Split") }
        listOf("5", "0", "00").forEach { tap(it) } // 50.00 total
        tap("Split") // no category picked yet: tapping a category chip would save at once (save-on-tap)
        compose.waitUntil(TIMEOUT) { has("Rest") }
        tap("Pick category", last = false)
        tap("Groceries")
        tap("Pick category")
        tap("Eating out")
        tap("Set amount")
        listOf("1", "2", "5", "0").forEach { tap(it) } // 12.50 for eating out
        tap("Save") // amount sheet
        compose.waitUntil(TIMEOUT) { has("\$37.50") } // rest for groceries
        // Let the amount sheet finish closing; otherwise its own "Save" is still in the tree and is the one found.
        Thread.sleep(1500)
        compose.waitForIdle()
        tap("Save") // quick add

        compose.waitUntil(TIMEOUT) { splits().size == 2 }
        val parts = splits()
        assertEquals(listOf(12_50L, 37_50L), parts.map { it.amountCents }.sorted())
        assertEquals(1, parts.map { it.splitId }.distinct().size)
        assertEquals(before + 2, runBlocking { compose.activity.container.transactions.all() }.size)

        runBlocking { compose.activity.container.transactions.delete(parts.last()) }
        compose.waitUntil(TIMEOUT) { splits().isEmpty() }
        runBlocking { compose.activity.container.transactions.restore(parts.last()) }
        compose.waitUntil(TIMEOUT) { splits().size == 2 }
        assertTrue(splits().map { it.id }.toSet() == parts.map { it.id }.toSet())
    }
}
