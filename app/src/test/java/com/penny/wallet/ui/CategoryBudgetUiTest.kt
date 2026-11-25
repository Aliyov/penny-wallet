package com.penny.wallet.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import com.penny.wallet.MainActivity
import com.penny.wallet.container
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TIMEOUT = 60_000L // CI runners are much slower than a laptop

/** Set a category budget in Settings → Expense categories, see it in Expenses, then remove it. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class CategoryBudgetUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun has(t: String) = compose.onAllNodesWithText(t).fetchSemanticsNodes().isNotEmpty()
    /**
     * Clicks through the semantics action: bottom sheets are still animating in under Robolectric, so injected touches
     * can land below them. Real touches on these rows are checked on the phone.
     */
    private fun tap(t: String) {
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText(t) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText(t) and hasClickAction()).onLast().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun groceriesBudget() = runBlocking { compose.activity.container.categories.all().single { it.name == "Groceries" }.monthlyBudgetCents }

    @Test fun setShowAndRemoveACategoryBudget() {
        tap("Try with sample data")
        compose.waitUntil(TIMEOUT) { has("Bank") }

        tap("Settings")
        tap("Expense categories")
        tap("Groceries")
        tap("Not set")
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("How much you want to spend in this category", substring = true).fetchSemanticsNodes().isNotEmpty() }
        listOf("5", "0", "0").forEach(::tap)
        tap("Save") // amount sheet, on top
        compose.waitUntil(TIMEOUT) { has("${'$'}5.00 a month") || has("${'$'}500.00 a month") }
        tap("Save") // category sheet
        compose.waitUntil(TIMEOUT) { groceriesBudget() != null }
        val budget = groceriesBudget()!!

        compose.activity.onBackPressedDispatcher.let { compose.runOnUiThread { it.onBackPressed() } }
        tap("Expenses")
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText(" left", substring = true).fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText(" over", substring = true).fetchSemanticsNodes().isNotEmpty() }

        tap("Settings")
        tap("Expense categories")
        tap("Groceries")
        tap(if (budget == 500L) "${'$'}5.00 a month" else "${'$'}500.00 a month")
        tap("Remove budget")
        tap("Save")
        compose.waitUntil(TIMEOUT) { groceriesBudget() == null }
        assertEquals(null, groceriesBudget())
    }
}
