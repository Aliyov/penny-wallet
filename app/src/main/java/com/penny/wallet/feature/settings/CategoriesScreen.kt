package com.penny.wallet.feature.settings

import androidx.compose.ui.platform.LocalResources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.SectionLabel
import com.penny.wallet.core.ui.components.rememberHaptic
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private sealed interface CatSheet {
    data object New : CatSheet
    data class Edit(val cat: Category) : CatSheet
    data class InUse(val cat: Category, val count: Int) : CatSheet
    data class Move(val cat: Category) : CatSheet
}

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val vm = pennyViewModel { ManageViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = LocalSnack.current
    val haptic = rememberHaptic()
    val c = Penny.colors
    val res = LocalResources.current
    var sheet by remember { mutableStateOf<CatSheet?>(null) }

    val active = remember(state.categories) { state.categories.filter { !it.category.archived } }
    val archived = remember(state.categories) { state.categories.filter { it.category.archived } }
    var order by remember { mutableStateOf(active) }
    LaunchedEffect(active) { order = active }

    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        val fi = order.indexOfFirst { it.category.id == from.key }
        val ti = order.indexOfFirst { it.category.id == to.key }
        if (fi >= 0 && ti >= 0) {
            order = order.toMutableList().apply { add(ti, removeAt(fi)) }
            haptic(HapticFeedbackType.TextHandleMove)
        }
    }
    val commitOrder = { vm.reorderCategories(order.map { it.category.id } + archived.map { it.category.id }) }

    fun delete(cat: Category) {
        vm.deleteCategory(cat) { r ->
            sheet = when (r) {
                is CategoryDelete.Deleted -> { snack.undo(res.getString(R.string.manage_deleted_msg, r.category.name)) { vm.undoCategoryDelete(r.category) }; null }
                is CategoryDelete.InUse -> CatSheet.InUse(r.category, r.count)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding()) {
        SettingsTopBar(stringResource(R.string.settings_categories), onBack, onAdd = { sheet = CatSheet.New })
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = Penny.dims.gutter).navigationBarsPadding()) {
            item("hint") {
                Text(stringResource(R.string.manage_categories_hint), style = Penny.type.caption, color = c.textSecondary,
                    modifier = Modifier.padding(bottom = 8.dp))
            }
            if (order.isEmpty()) item("empty") {
                Text(stringResource(R.string.manage_categories_empty), style = Penny.type.body, color = c.textSecondary, modifier = Modifier.padding(vertical = 24.dp))
            }
            items(order, key = { it.category.id }) { item ->
                ReorderableItem(reorder, key = item.category.id) { dragging ->
                    ManageRow(
                        icon = item.category.icon, color = Color(item.category.color), title = item.category.name,
                        subtitle = expenses(item.expenseCount), dragging = dragging,
                        onClick = { sheet = CatSheet.Edit(item.category) },
                        handle = Modifier.draggableHandle(onDragStarted = { haptic(HapticFeedbackType.LongPress) }, onDragStopped = { commitOrder() }),
                    )
                }
            }
            if (archived.isNotEmpty()) {
                item("archivedLabel") { SectionLabel(stringResource(R.string.manage_archived), Modifier.padding(top = 16.dp)) }
                items(archived, key = { "a-" + it.category.id }) { item ->
                    ManageRow(
                        icon = item.category.icon, color = Color(item.category.color), title = item.category.name,
                        subtitle = expenses(item.expenseCount), dimmed = true,
                        onClick = { sheet = CatSheet.Edit(item.category) },
                        trailing = { UnarchiveButton { vm.setCategoryArchived(item.category.id, false) } },
                    )
                }
            }
            item("bottom") { Spacer(Modifier.height(32.dp)) }
        }
    }

    when (val s = sheet) {
        CatSheet.New -> CategoryEditSheet(null, onDismiss = { sheet = null }, onSave = { n, i, col -> vm.addCategory(n, i, col); sheet = null })
        is CatSheet.Edit -> CategoryEditSheet(
            s.cat, onDismiss = { sheet = null },
            onSave = { n, i, col -> vm.saveCategory(s.cat.copy(name = n, icon = i, color = col)); sheet = null },
            onToggleArchive = {
                vm.setCategoryArchived(s.cat.id, !s.cat.archived); sheet = null
                snack.show(res.getString(if (s.cat.archived) R.string.manage_restored_msg else R.string.manage_archived_msg, s.cat.name))
            },
            onDelete = { delete(s.cat) },
        )
        is CatSheet.InUse -> CategoryInUseDialog(
            s.cat, s.count,
            onMove = { sheet = CatSheet.Move(s.cat) },
            onArchive = { vm.setCategoryArchived(s.cat.id, true); sheet = null; snack.show(res.getString(R.string.manage_archived_msg, s.cat.name)) },
            onDismiss = { sheet = null },
        )
        is CatSheet.Move -> MoveTargetSheet(
            s.cat, state.categories.map { it.category }.filter { it.id != s.cat.id && !it.archived },
            onDismiss = { sheet = null },
        ) { target ->
            sheet = null
            vm.moveAndDelete(s.cat, target.id) { m ->
                if (m != null) snack.undo(res.getString(R.string.manage_moved_deleted_msg, target.name, s.cat.name)) { vm.undoMove(m) }
            }
        }
        null -> Unit
    }
}

@Composable
private fun expenses(n: Int) = pluralStringResource(R.plurals.manage_expenses, n, n)
