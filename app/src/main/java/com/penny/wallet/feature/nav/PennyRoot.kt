package com.penny.wallet.feature.nav

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.res.stringResource
import com.penny.wallet.AppContainer
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.NavItem
import com.penny.wallet.core.ui.components.PennyBottomBar
import com.penny.wallet.core.ui.components.PennyFab
import com.penny.wallet.core.ui.components.Snack
import com.penny.wallet.core.ui.theme.Penny

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
    const val SUPPORT = "support"
    const val PRIVACY = "privacy"
    const val IMPORT = "import"
    const val DEBTS = "debts"
    fun account(id: String) = "account/$id"
    fun goal(id: String) = "goal/$id"
}

@Composable
private fun rememberTabs(): List<NavItem> {
    val home = stringResource(R.string.nav_home)
    val expenses = stringResource(R.string.nav_expenses)
    val savings = stringResource(R.string.nav_savings)
    val settings = stringResource(R.string.nav_settings)
    return remember(home, expenses, savings, settings) {
        listOf(
            NavItem(Routes.HOME, home, Icons.Rounded.Home),
            NavItem(Routes.EXPENSES, expenses, Icons.Rounded.DonutLarge),
            NavItem(Routes.SAVINGS, savings, Icons.Rounded.Savings),
            NavItem(Routes.SETTINGS, settings, Icons.Rounded.Tune),
        )
    }
}

/** Quick add opened from outside the app; [scan] starts the camera right away. Identity matters: every tap is new. */
class LaunchRequest(val scan: Boolean)

class RootState {
    var quickAddOpen by mutableStateOf(false)
    var scanOnOpen by mutableStateOf(false)
    var editTxId by mutableStateOf<String?>(null)
    fun openNew() { editTxId = null; quickAddOpen = true }
    fun openEdit(id: String) { editTxId = id; quickAddOpen = true }
    fun close() { quickAddOpen = false; editTxId = null; scanOnOpen = false }
}

@Composable
fun PennyRoot(container: AppContainer, request: LaunchRequest? = null, onLaunchHandled: () -> Unit = {}) {
    val nav = rememberNavController()
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.action_undo)
    val snack = remember(undoLabel) { Snack(host, scope, undoLabel) }
    val root = remember { RootState() }
    LaunchedEffect(request) {
        if (request == null) return@LaunchedEffect
        if (container.accounts.all().isNotEmpty()) {
            root.openNew()
            root.scanOnOpen = request.scan
        }
        onLaunchHandled()
    }

    val tabs = rememberTabs()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in tabs.map { it.route }
    val hasAccounts by remember { container.accounts.observe().map { it.isNotEmpty() } }.collectAsStateWithLifecycle(true)

    CompositionLocalProvider(LocalSnack provides snack) {
        Box(Modifier.fillMaxSize().background(Penny.colors.background)) {
            PennyNavHost(nav, container, root)
            if (showBar) {
                PennyBottomBar(tabs, route, onSelect = { nav.switchTab(it) }, modifier = Modifier.align(Alignment.BottomCenter))
                // Before the first account exists, Home's welcome card is the only action.
                if (hasAccounts) PennyFab(
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
