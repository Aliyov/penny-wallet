package com.penny.wallet.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToString
import com.penny.wallet.MainActivity
import com.penny.wallet.container
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** First composition under Robolectric takes several seconds on a busy machine. */
private const val TIMEOUT = 30_000L

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = TestPennyApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
class FirstRunUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    /** On timeout, prints the screen and the database so a CI failure can be read from the report. */
    private fun waitFor(what: String, condition: () -> Boolean) {
        try {
            compose.waitUntil(TIMEOUT, condition)
        } catch (e: Throwable) {
            val accounts = runBlocking { compose.activity.container.db.accounts().getAll().map { it.name } }
            println("Timed out waiting for $what. Accounts in db: $accounts")
            println(compose.onRoot(useUnmergedTree = true).printToString())
            throw e
        }
    }

    private fun hasText(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test fun freshInstallShowsWelcomeAndSampleDataFillsHome() {
        waitFor("welcome card") { hasText("Add your first account") }
        compose.waitForIdle()
        compose.onNodeWithText("Try with sample data").performScrollTo().assertIsDisplayed().performClick()
        waitFor("sample account") { hasText("Bank") }
        waitFor("welcome card to go") { !hasText("Add your first account") }
    }
}
