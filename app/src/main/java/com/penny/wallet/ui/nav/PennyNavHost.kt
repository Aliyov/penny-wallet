package com.penny.wallet.ui.nav

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.penny.wallet.AppContainer
import com.penny.wallet.ui.PlaceholderScreen
import com.penny.wallet.ui.account.AccountScreen
import com.penny.wallet.ui.home.HomeScreen

@Composable
fun PennyNavHost(nav: NavHostController, container: AppContainer, root: RootState) {
    val editTx: (String) -> Unit = { root.openEdit(it) }
    NavHost(nav, startDestination = Routes.HOME, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenAccount = { nav.navigate(Routes.account(it)) },
                onOpenRecurring = { nav.navigate(Routes.RECURRING) },
                onEditTransaction = editTx,
                onOpenBackup = { nav.navigate(Routes.BACKUP) },
            )
        }
        composable(Routes.EXPENSES) { PlaceholderScreen("Expenses") }
        composable(Routes.SAVINGS) { PlaceholderScreen("Savings") }
        composable(Routes.SETTINGS) { PlaceholderScreen("Settings") }
        composable(
            Routes.ACCOUNT,
            enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
            popExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() },
        ) { e ->
            AccountScreen(e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }, onEditTransaction = editTx)
        }
        composable(Routes.RECURRING) { PlaceholderScreen("Recurring") }
        composable(Routes.BACKUP) { PlaceholderScreen("Backup") }
    }
}
