package com.penny.wallet.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.penny.wallet.MainActivity
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

    @Test fun freshInstallShowsWelcomeAndSampleDataFillsHome() {
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("Add your first account").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Try with sample data").performScrollTo().assertIsDisplayed().performClick()
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("Bank").fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(TIMEOUT) { compose.onAllNodesWithText("Add your first account").fetchSemanticsNodes().isEmpty() }
    }
}
