package com.penny.wallet.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.penny.wallet.AppContainer
import com.penny.wallet.ui.PlaceholderScreen

@Composable
fun PennyNavHost(nav: NavHostController, container: AppContainer, root: RootState) {
    NavHost(nav, startDestination = Routes.HOME, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
        composable(Routes.HOME) { PlaceholderScreen("Home") }
        composable(Routes.EXPENSES) { PlaceholderScreen("Expenses") }
        composable(Routes.SAVINGS) { PlaceholderScreen("Savings") }
        composable(Routes.SETTINGS) { PlaceholderScreen("Settings") }
    }
}
