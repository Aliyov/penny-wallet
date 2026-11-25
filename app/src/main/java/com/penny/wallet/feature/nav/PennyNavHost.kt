package com.penny.wallet.feature.nav

import com.penny.wallet.feature.support.PrivacyScreen
import com.penny.wallet.feature.debts.DebtsScreen
import com.penny.wallet.feature.support.SupportScreen
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.penny.wallet.AppContainer
import com.penny.wallet.feature.home.AccountScreen
import com.penny.wallet.feature.expenses.ExpensesScreen
import com.penny.wallet.feature.home.HomeScreen
import com.penny.wallet.feature.search.SearchScreen
import com.penny.wallet.feature.year.YearScreen
import com.penny.wallet.feature.recurring.RecurringScreen
import com.penny.wallet.feature.savings.GoalScreen
import com.penny.wallet.feature.savings.SavingsScreen
import com.penny.wallet.feature.settings.AccountsScreen
import com.penny.wallet.feature.settings.BackupScreen
import com.penny.wallet.feature.importcsv.ImportScreen
import com.penny.wallet.feature.settings.CategoriesScreen
import com.penny.wallet.feature.settings.PersonaliseScreen
import com.penny.wallet.feature.settings.SettingsScreen

private val push: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { slideInHorizontally { it / 3 } + fadeIn() }
private val pop: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { slideOutHorizontally { it / 3 } + fadeOut() }

@Composable
fun PennyNavHost(nav: NavHostController, container: AppContainer, root: RootState) {
    val editTx: (String) -> Unit = { root.openEdit(it) }
    val openSearch: () -> Unit = { nav.navigate(Routes.SEARCH) }
    val scope = rememberCoroutineScope()
    val addExpense: () -> Unit = { scope.launch { if (container.accounts.all().isNotEmpty()) root.openNew() } }
    NavHost(nav, startDestination = Routes.HOME, enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenAccount = { nav.navigate(Routes.account(it)) },
                onOpenRecurring = { nav.navigate(Routes.RECURRING) },
                onEditTransaction = editTx,
                onOpenBackup = { nav.navigate(Routes.BACKUP) },
                onAddExpense = addExpense,
                onOpenSearch = openSearch,
            )
        }
        composable(Routes.EXPENSES) { ExpensesScreen(
                onOpenRecurring = { nav.navigate(Routes.RECURRING) }, onEditTransaction = editTx, onAddExpense = addExpense, onOpenSearch = openSearch,
                onOpenYear = { nav.navigate(Routes.YEAR) }, monthRequest = root.expensesMonthRequest, onMonthRequestHandled = { root.expensesMonthRequest = null },
            )
        }
        composable(Routes.SAVINGS) { SavingsScreen(onOpenGoal = { nav.navigate(Routes.goal(it)) }) }
        composable(Routes.SETTINGS) { SettingsScreen(onOpen = { nav.navigate(it) }) }
        composable(
            Routes.ACCOUNT,
            enterTransition = push, popExitTransition = pop,
        ) { e ->
            AccountScreen(e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }, onEditTransaction = editTx)
        }
        composable(Routes.RECURRING, enterTransition = push, popExitTransition = pop) {
            RecurringScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.GOAL, enterTransition = push, popExitTransition = pop) { e ->
            GoalScreen(e.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() })
        }
        composable(Routes.BACKUP, enterTransition = push, popExitTransition = pop) { BackupScreen(onBack = { nav.popBackStack() }, onOpenImport = { nav.navigate(Routes.IMPORT) }) }
        composable(Routes.IMPORT, enterTransition = push, popExitTransition = pop) { ImportScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.CATEGORIES, enterTransition = push, popExitTransition = pop) { CategoriesScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.ACCOUNTS, enterTransition = push, popExitTransition = pop) {
            AccountsScreen(onBack = { nav.popBackStack() }, onOpenAccount = { nav.navigate(Routes.account(it)) })
        }
        composable(Routes.SUPPORT, enterTransition = push, popExitTransition = pop) { SupportScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PRIVACY, enterTransition = push, popExitTransition = pop) { PrivacyScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SEARCH, enterTransition = push, popExitTransition = pop) { SearchScreen(onBack = { nav.popBackStack() }, onEditTransaction = editTx) }
        composable(Routes.YEAR, enterTransition = push, popExitTransition = pop) {
            // Year is only opened from Expenses: going back lands there. Switching tabs would restore the saved
            // Expenses stack, which still holds this screen.
            // Only while Year is on top: a second tap during the exit animation would pop Expenses too.
            val onTop = { nav.currentDestination?.route == Routes.YEAR }
            YearScreen(
                onBack = { if (onTop()) nav.popBackStack() },
                onOpenMonth = { m -> if (onTop()) { root.expensesMonthRequest = m; nav.popBackStack() } },
            )
        }
        composable(Routes.DEBTS, enterTransition = push, popExitTransition = pop) { DebtsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PERSONALISE, enterTransition = push, popExitTransition = pop) { PersonaliseScreen(onBack = { nav.popBackStack() }) }
    }
}
