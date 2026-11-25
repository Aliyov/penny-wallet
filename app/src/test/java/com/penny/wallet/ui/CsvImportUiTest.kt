package com.penny.wallet.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.penny.wallet.MainActivity
import com.penny.wallet.container
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

private const val TIMEOUT = 30_000L

/** Settings → Backup → Import from CSV with a Monefy-style file, then undo the whole import. */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestPennyApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class CsvImportUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun has(t: String) = compose.onAllNodesWithText(t, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun tap(t: String) {
        compose.waitUntil(TIMEOUT) { compose.onAllNodes(hasText(t) and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        val n = compose.onAllNodes(hasText(t) and hasClickAction()).onLast()
        runCatching { n.performScrollTo() }
        n.performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun imported() = runBlocking { compose.activity.container.transactions.all() }.filter { it.importId != null }

    @Test fun importAFileAndUndoIt() {
        val file = File(compose.activity.cacheDir, "monefy.csv").apply {
            writeText(
                "date,account,category,amount,currency,converted amount,currency,description\n" +
                    "24/11/2026,Cash,Groceries,-12.30,EUR,-12.30,EUR,Lunch\n" +
                    "25/11/2026,Cash,Hobbies,-40.00,EUR,-40.00,EUR,Paint\n" +
                    "26/11/2026,Cash,Salary,1500,EUR,1500,EUR,Pay\n" +
                    "not a date,Cash,X,1,EUR,1,EUR,broken\n",
            )
        }
        tap("Try with sample data")
        compose.waitUntil(TIMEOUT) { has("Bank") }
        tap("Settings")
        tap("Backup")
        tap("Import from CSV")
        tap("Choose CSV file")

        val shadow = shadowOf(compose.activity)
        val pick = shadow.nextStartedActivityForResult
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, pick.intent.action)
        shadow.receiveResult(pick.intent, Activity.RESULT_OK, Intent().setData(Uri.fromFile(file)))
        compose.waitUntil(TIMEOUT) { has("Import 3 entries") }
        assertTrue(has("Line 5"))
        tap("Import 3 entries")

        compose.waitUntil(TIMEOUT) { imported().size == 3 }
        val rows = imported()
        assertEquals(setOf(12_30L, 40_00L, 1500_00L), rows.map { it.amountCents }.toSet())
        assertEquals(1, rows.map { it.importId }.distinct().size)
        val last = runBlocking { compose.activity.container.settings.current() }.lastImport!!
        assertEquals(3, last.count)
        assertTrue(runBlocking { compose.activity.container.categories.all() }.any { it.name == "Hobbies" })

        runBlocking { compose.activity.container.imports.undo(last) }
        assertTrue(imported().isEmpty())
        assertTrue(runBlocking { compose.activity.container.categories.all() }.none { it.name == "Hobbies" })
    }
}
