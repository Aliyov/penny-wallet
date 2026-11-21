package com.penny.wallet.feature.savings

import com.penny.wallet.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.container
import com.penny.wallet.feature.home.ConfirmDialog
import com.penny.wallet.core.ui.common.TxRowItem
import com.penny.wallet.core.ui.components.Celebration
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny
import kotlinx.coroutines.launch

internal enum class GoalDialog { EDIT, DELETE, ADD, TAKE_BACK }

@Composable
fun GoalScreen(id: String, onBack: () -> Unit) {
    val vm = pennyViewModel(key = "goal-$id") { GoalViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val container = LocalContext.current.container
    var left by remember { mutableStateOf(false) }
    val leave = { if (!left) { left = true; onBack() } }
    var dialog by rememberSaveable { mutableStateOf<GoalDialog?>(null) }
    var celebrate by remember { mutableStateOf(false) }

    LaunchedEffect(state.missing) { if (state.missing) leave() }
    val card = state.card ?: return
    val c = Penny.colors
    val addedText = stringResource(R.string.savings_added_to, card.goal.name)
    val returnedText = stringResource(R.string.savings_money_returned)
    val goalDeletedText = stringResource(R.string.savings_goal_deleted)

    Box(Modifier.fillMaxSize().background(c.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            GoalTopBar(card.goal.name, onBack = leave, onEdit = { dialog = GoalDialog.EDIT }, onDelete = { dialog = GoalDialog.DELETE })
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = Penny.dims.gutter).navigationBarsPadding()) {
                item("hero") { GoalHero(card) }
                item("actions") {
                    GoalActions(
                        onAdd = { dialog = GoalDialog.ADD },
                        onTakeBack = { dialog = GoalDialog.TAKE_BACK },
                        canTakeBack = card.balance > 0,
                    )
                }
                item("historyLabel") { SectionLabel(stringResource(R.string.savings_history), Modifier.padding(top = 16.dp)) }
                if (state.rows.isEmpty()) item("empty") {
                    Text(stringResource(R.string.savings_history_empty), style = Penny.type.body, color = c.textSecondary, modifier = Modifier.padding(vertical = 24.dp))
                }
                items(state.rows, key = { it.tx.id }) { row -> TxRowItem(row, Modifier.animateItem()) }
                item("bottom") { Spacer(Modifier.height(32.dp)) }
            }
        }
        Celebration(celebrate) { celebrate = false }
    }

    when (dialog) {
        GoalDialog.ADD -> GoalMoneySheet(
            title = stringResource(R.string.savings_add_money), confirm = stringResource(R.string.savings_add_to_goal), accounts = state.accounts, outside = state.outside, initialAccount = state.suggestedFrom,
            showAvailable = true, onDismiss = { dialog = null },
        ) { cents, accountId ->
            dialog = null
            vm.addMoney(cents, accountId) { err, reached ->
                if (err != null) snack.show(err) else if (reached) celebrate = true else snack.show(addedText)
            }
        }
        GoalDialog.TAKE_BACK -> GoalMoneySheet(
            title = stringResource(R.string.savings_take_back), confirm = stringResource(R.string.savings_take_back), accounts = state.accounts, outside = state.outside, initialAccount = state.suggestedBack,
            showAvailable = false, onDismiss = { dialog = null },
        ) { cents, accountId ->
            dialog = null
            vm.takeBack(cents, accountId) { err -> snack.show(err ?: returnedText) }
        }
        GoalDialog.EDIT -> GoalSheet(card.goal, onDismiss = { dialog = null }) { name, icon, color, target, deadline, done ->
            vm.update(card.goal.copy(name = name, icon = icon, color = color, targetCents = target, deadline = deadline)) { err ->
                done(err)
                if (err == null) dialog = null
            }
        }
        GoalDialog.DELETE -> ConfirmDialog(
            title = stringResource(R.string.savings_delete_title, card.goal.name), text = stringResource(R.string.savings_delete_body), confirm = stringResource(R.string.action_delete), danger = true,
            onDismiss = { dialog = null },
        ) {
            dialog = null
            vm.delete { d ->
                leave()
                // The VM is gone after navigating back, so undo goes through the container's app scope.
                if (d != null) snack.undo(goalDeletedText) { container.appScope.launch { container.goals.undoDelete(d) } }
            }
        }
        null -> Unit
    }
}
