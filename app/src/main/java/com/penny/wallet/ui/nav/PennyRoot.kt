package com.penny.wallet.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.penny.wallet.AppContainer
import com.penny.wallet.ui.components.LocalSnack
import com.penny.wallet.ui.components.NavItem
import com.penny.wallet.ui.components.PennyBottomBar
import com.penny.wallet.ui.components.PennyFab
import com.penny.wallet.ui.components.Snack
import com.penny.wallet.ui.theme.Penny

object Routes {
    const val HOME = "home"
    const val EXPENSES = "expenses"
    const val SAVINGS = "savings"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account/{id}"
    const val GOAL = "goal/{id}"
    const val RECURRING = "recurring"
    const val CATEGORIES = "categories"
    const val ACCOUNTS = "accounts"
    const val PERSONALISE = "personalise"
    const val BACKUP = "backup"
    fun account(id: String) = "account/$id"
    fun goal(id: String) = "goal/$id"
}

private val tabs = listOf(
    NavItem(Routes.HOME, "Home", Icons.Rounded.Home),
    NavItem(Routes.EXPENSES, "Expenses", Icons.Rounded.DonutLarge),
    NavItem(Routes.SAVINGS, "Savings", Icons.Rounded.Savings),
    NavItem(Routes.SETTINGS, "Settings", Icons.Rounded.Tune),
)

/** Global UI state: quick add sheet requests (FAB, widget deep link). */
class RootState {
    var quickAddOpen by mutableStateOf(false)
    /** Transaction being edited in the quick add sheet, or null for a new entry. */
    var editTxId by mutableStateOf<String?>(null)
    fun openNew() { editTxId = null; quickAddOpen = true }
    fun openEdit(id: String) { editTxId = id; quickAddOpen = true }
    fun close() { quickAddOpen = false; editTxId = null }
}

@Composable
fun PennyRoot(container: AppContainer, startWithQuickAdd: Boolean = false) {
    val nav = rememberNavController()
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snack = remember { Snack(host, scope) }
    val root = remember { RootState() }
    var consumedStart by rememberSaveable { mutableStateOf(false) }
    if (startWithQuickAdd && !consumedStart) { consumedStart = true; root.openNew() }

    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in tabs.map { it.route }

    CompositionLocalProvider(LocalSnack provides snack) {
        Box(Modifier.fillMaxSize().background(Penny.colors.background)) {
            PennyNavHost(nav, container, root)
            if (showBar) {
                PennyBottomBar(tabs, route, onSelect = { nav.switchTab(it) }, modifier = Modifier.align(Alignment.BottomCenter))
                PennyFab(
                    onClick = { root.openNew() },
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp),
                )
            }
            SnackbarHost(host, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (showBar) 92.dp else 16.dp)) {
                Snackbar(it, containerColor = Penny.colors.surface2, contentColor = Penny.colors.textPrimary,
                    actionColor = Penny.colors.accent, shape = Penny.dims.smallShape)
            }
            QuickAddHost(root)
        }
    }
}

fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(Routes.HOME) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
