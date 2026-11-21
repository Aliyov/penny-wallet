package com.penny.wallet.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.feature.home.pennyFieldColors
import com.penny.wallet.core.ui.components.ButtonKind
import com.penny.wallet.core.ui.components.IconBadge
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyIcons
import com.penny.wallet.core.ui.components.PennySheet
import com.penny.wallet.core.ui.theme.ItemPalette
import com.penny.wallet.core.ui.theme.Penny

/** Add (initial == null) or edit a category: name, icon, colour, archive, delete. */
@Composable
internal fun CategoryEditSheet(
    initial: Category?, onDismiss: () -> Unit, onSave: (name: String, icon: String, color: Long) -> Unit,
    onToggleArchive: () -> Unit = {}, onDelete: () -> Unit = {},
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: "other") }
    var color by rememberSaveable { mutableLongStateOf(initial?.color ?: ItemPalette.first()) }
    PennySheet(onDismiss = onDismiss, title = stringResource(if (initial == null) R.string.manage_new_category else R.string.manage_edit_category)) {
        Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon, Color(color), size = 48)
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    name, { name = it }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.label_name)) },
                    textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
                )
            }
            FieldLabel(stringResource(R.string.label_colour))
            ColorSwatches(color) { color = it }
            FieldLabel(stringResource(R.string.label_icon))
            IconPicker(PennyIcons.all.map { it.first }, icon, Color(color)) { icon = it }
            PennyButton(stringResource(R.string.action_save), { onSave(name.trim(), icon, color) }, Modifier.fillMaxWidth().padding(top = 20.dp), enabled = name.isNotBlank())
            if (initial != null) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PennyButton(stringResource(if (initial.archived) R.string.manage_unarchive else R.string.action_archive), onToggleArchive, Modifier.weight(1f), kind = ButtonKind.SECONDARY)
                    PennyButton(stringResource(R.string.action_delete), onDelete, Modifier.weight(1f), kind = ButtonKind.DANGER)
                }
            }
        }
    }
}

/** The category has expenses: move them elsewhere, or archive instead. */
@Composable
internal fun CategoryInUseDialog(cat: Category, count: Int, onMove: () -> Unit, onArchive: () -> Unit, onDismiss: () -> Unit) {
    val c = Penny.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface2, titleContentColor = c.textPrimary, textContentColor = c.textSecondary,
        shape = Penny.dims.cardShape,
        title = { Text(stringResource(R.string.manage_category_delete_title, cat.name), style = Penny.type.title) },
        text = {
            Text(pluralStringResource(R.plurals.manage_category_in_use_body, count, count), style = Penny.type.body)
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onMove) { Text(stringResource(R.string.manage_move_expenses), color = c.danger, style = Penny.type.bodyStrong) }
                TextButton(onClick = onArchive) { Text(stringResource(R.string.manage_archive_instead), color = c.accent, style = Penny.type.bodyStrong) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = c.textSecondary) }
            }
        },
    )
}

/** Pick where the expenses of [from] go. */
@Composable
internal fun MoveTargetSheet(from: Category, options: List<Category>, onDismiss: () -> Unit, onPick: (Category) -> Unit) {
    val c = Penny.colors
    PennySheet(onDismiss = onDismiss, title = stringResource(R.string.manage_move_title, from.name)) {
        if (options.isEmpty()) {
            Text(stringResource(R.string.manage_move_none), style = Penny.type.body, color = c.textSecondary)
        }
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
            options.forEach { cat ->
                Row(
                    Modifier.fillMaxWidth().clickable { onPick(cat) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(cat.icon, Color(cat.color), size = 36)
                    Spacer(Modifier.width(12.dp))
                    Text(cat.name, style = Penny.type.bodyStrong, color = c.textPrimary)
                }
            }
        }
    }
}
