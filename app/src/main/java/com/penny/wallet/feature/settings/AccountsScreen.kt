package com.penny.wallet.feature.settings

import androidx.compose.ui.platform.LocalResources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.data.repo.AccountDeleteResult
import com.penny.wallet.feature.home.ConfirmDialog
import com.penny.wallet.feature.home.pennyFieldColors
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyIcons
import com.penny.wallet.core.ui.components.PennySheet
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.components.rememberHaptic
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Accounts can only be deleted when empty; otherwise they are archived. */
@Composable
fun AccountsScreen(onBack: () -> Unit, onOpenAccount: (String) -> Unit) {
    val vm = pennyViewModel { ManageViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val haptic = rememberHaptic()
    val c = Penny.colors
    val res = LocalResources.current
    var adding by remember { mutableStateOf(false) }
    var mustArchive by remember { mutableStateOf<AccountItem?>(null) }

    val active = remember(state.accounts) { state.accounts.filter { !it.account.archived } }
    val archived = remember(state.accounts) { state.accounts.filter { it.account.archived } }
    var order by remember { mutableStateOf(active) }
    LaunchedEffect(active) { order = active }

    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val fi = order.indexOfFirst { it.account.id == from.key }
        val ti = order.indexOfFirst { it.account.id == to.key }
        if (fi >= 0 && ti >= 0) {
            order = order.toMutableList().apply { add(ti, removeAt(fi)) }
            haptic(HapticFeedbackType.TextHandleMove)
        }
    }
    val commitOrder = { vm.reorderAccounts(order.map { it.account.id } + archived.map { it.account.id }) }

    val archive = { item: AccountItem, on: Boolean ->
        vm.setAccountArchived(item, on) { err -> snack.show(err ?: res.getString(if (on) R.string.manage_archived_msg else R.string.manage_restored_msg, item.account.name)) }
    }
    val delete = { item: AccountItem ->
        vm.deleteAccount(item.account.id) { r ->
            when (r) {
                is AccountDeleteResult.Deleted -> snack.undo(res.getString(R.string.manage_deleted_msg, r.account.name)) { vm.undoAccountDelete(r.account) }
                AccountDeleteResult.MustArchive -> mustArchive = item
            }
        }
    }

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        SettingsTopBar(stringResource(R.string.settings_accounts), onBack, onAdd = { adding = true })
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = Penny.dims.gutter).navigationBarsPadding()) {
            if (order.isEmpty()) item("empty") {
                Text(stringResource(R.string.manage_accounts_empty), style = Penny.type.body, color = c.textSecondary, modifier = Modifier.padding(vertical = 24.dp))
            }
            items(order, key = { it.account.id }) { item ->
                ReorderableItem(reorder, key = item.account.id) { dragging ->
                    ManageRow(
                        icon = item.account.icon, color = c.accent, title = item.account.name,
                        subtitle = transactions(item.txCount), dragging = dragging,
                        onClick = { onOpenAccount(item.account.id) },
                        handle = Modifier.draggableHandle(onDragStarted = { haptic(HapticFeedbackType.LongPress) }, onDragStopped = { commitOrder() }),
                        trailing = {
                            Balance(item)
                            RowMenu(archived = false, onArchive = { archive(item, true) }, onDelete = { delete(item) })
                        },
                    )
                }
            }
            if (archived.isNotEmpty()) {
                item("archivedLabel") { SectionLabel(stringResource(R.string.manage_archived), Modifier.padding(top = 16.dp)) }
                items(archived, key = { "a-" + it.account.id }) { item ->
                    ManageRow(
                        icon = item.account.icon, color = c.textSecondary, title = item.account.name,
                        subtitle = transactions(item.txCount), dimmed = true,
                        onClick = { onOpenAccount(item.account.id) },
                        trailing = {
                            UnarchiveButton { archive(item, false) }
                            RowMenu(archived = true, onArchive = { archive(item, false) }, onDelete = { delete(item) })
                        },
                    )
                }
            }
            item("bottom") { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (adding) AddAccountSheet(onDismiss = { adding = false }) { n, i -> vm.addAccount(n, i); adding = false }
    mustArchive?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.manage_account_cant_delete_title, item.account.name),
            text = stringResource(R.string.manage_account_cant_delete_body),
            confirm = stringResource(R.string.action_archive), danger = false, onDismiss = { mustArchive = null },
        ) { mustArchive = null; archive(item, true) }
    }
}

@Composable
private fun Balance(item: AccountItem) {
    val c = Penny.colors
    val b = item.money.balance
    MoneyText(b, Penny.type.bodyStrong, if (b < 0) c.danger else c.textPrimary, Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun RowMenu(archived: Boolean, onArchive: () -> Unit, onDelete: () -> Unit) {
    val c = Penny.colors
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.manage_more), tint = c.textSecondary) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.surface2) {
            DropdownMenuItem(text = { Text(stringResource(if (archived) R.string.manage_unarchive else R.string.action_archive), color = c.textPrimary) }, onClick = { open = false; onArchive() })
            DropdownMenuItem(text = { Text(stringResource(R.string.action_delete), color = c.danger) }, onClick = { open = false; onDelete() })
        }
    }
}

@Composable
private fun AddAccountSheet(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf(PennyIcons.accountKeys.first()) }
    PennySheet(onDismiss = onDismiss, title = stringResource(R.string.manage_new_account)) {
        OutlinedTextField(
            name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.label_name)) },
            textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
        )
        FieldLabel(stringResource(R.string.label_icon))
        IconPicker(PennyIcons.accountKeys, icon, Penny.colors.accent) { icon = it }
        PennyButton(stringResource(R.string.manage_add_account), { onSave(name.trim(), icon) }, Modifier.fillMaxWidth().padding(top = 20.dp), enabled = name.isNotBlank())
    }
}

@Composable
private fun transactions(n: Int) = pluralStringResource(R.plurals.manage_transactions, n, n)
