package com.penny.wallet.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penny.wallet.ui.components.AmountSheet
import com.penny.wallet.ui.components.DatePickerRow
import com.penny.wallet.ui.components.PennyButton
import com.penny.wallet.ui.components.PennyChip
import com.penny.wallet.ui.components.PennyIcons
import com.penny.wallet.ui.components.PennySheet
import com.penny.wallet.ui.theme.Penny
import java.time.LocalDate

@Composable
internal fun AccountTopBar(name: String, archived: Boolean, onBack: () -> Unit, onRename: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit) {
    val c = Penny.colors
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = c.textPrimary) }
        Text(
            name + if (archived) " (archived)" else "", style = Penny.type.title, color = c.textPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
        )
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More", tint = c.textPrimary) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.surface2) {
                DropdownMenuItem(text = { Text("Rename / icon", color = c.textPrimary) }, onClick = { menu = false; onRename() })
                DropdownMenuItem(text = { Text(if (archived) "Unarchive" else "Archive", color = c.textPrimary) }, onClick = { menu = false; onArchive() })
                DropdownMenuItem(text = { Text("Delete", color = c.danger) }, onClick = { menu = false; onDelete() })
            }
        }
    }
}

/** Amount sheet for one of the four account actions. */
@Composable
internal fun AccountActionSheet(
    action: AccountAction, state: AccountState, onDismiss: () -> Unit,
    onConfirm: (cents: Long, date: LocalDate, note: String?, toAccountId: String?) -> Unit,
) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var note by rememberSaveable { mutableStateOf("") }
    var toId by rememberSaveable { mutableStateOf<String?>(null) }
    val c = Penny.colors
    val title = when (action) {
        AccountAction.INCOME -> "Add income"
        AccountAction.WITHDRAW -> "Withdraw"
        AccountAction.SET_BALANCE -> "Set balance"
        AccountAction.TRANSFER -> "Transfer"
    }
    if (action == AccountAction.TRANSFER && state.others.isEmpty()) {
        PennySheet(onDismiss = onDismiss, title = title) {
            Text("Add another account first to transfer money between accounts.", style = Penny.type.body, color = c.textSecondary)
            PennyButton("OK", onDismiss, Modifier.fillMaxWidth().padding(top = 16.dp))
        }
        return
    }
    val isSet = action == AccountAction.SET_BALANCE
    AmountSheet(
        title = title, onDismiss = onDismiss,
        confirmLabel = if (isSet) "Set balance" else title,
        initialCents = if (isSet) state.money.balance else 0,
        allowZero = isSet, allowNegative = isSet,
        extra = if (isSet) null else ({
            if (action == AccountAction.TRANSFER) {
                Text("To", style = Penny.type.label, color = c.textSecondary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.others.forEach { a -> PennyChip(a.name, toId == a.id, { toId = a.id }) }
                }
            }
            DatePickerRow(date, { date = it })
            NoteField(note) { note = it }
        }),
    ) { cents ->
        if (action == AccountAction.TRANSFER && toId == null) return@AmountSheet
        onConfirm(cents, date, note.trim().ifBlank { null }, toId)
    }
}

@Composable
private fun NoteField(value: String, onChange: (String) -> Unit) {
    val c = Penny.colors
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(), singleLine = true,
        placeholder = { Text("Note (optional)", color = c.textSecondary) },
        textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
    )
}

@Composable
internal fun pennyFieldColors() = Penny.colors.let { c ->
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.accent, unfocusedBorderColor = c.hairline, cursorColor = c.accent,
        focusedTextColor = c.textPrimary, unfocusedTextColor = c.textPrimary,
    )
}

@Composable
internal fun RenameSheet(name: String, icon: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var n by rememberSaveable { mutableStateOf(name) }
    var i by rememberSaveable { mutableStateOf(icon) }
    val c = Penny.colors
    PennySheet(onDismiss = onDismiss, title = "Rename account") {
        OutlinedTextField(
            n, { n = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Name") },
            textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
        )
        Text("Icon", style = Penny.type.label, color = c.textSecondary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PennyIcons.accountKeys.forEach { k ->
                val sel = k == i
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(if (sel) c.accent.copy(alpha = 0.18f) else c.surface1)
                        .border(if (sel) 1.5.dp else 0.75.dp, if (sel) c.accent else c.hairline, CircleShape)
                        .clickable { i = k },
                    contentAlignment = Alignment.Center,
                ) { Icon(PennyIcons.get(k), k, tint = if (sel) c.accent else c.textPrimary, modifier = Modifier.size(24.dp)) }
            }
        }
        PennyButton("Save", { onSave(n, i) }, Modifier.fillMaxWidth().padding(top = 20.dp), enabled = n.isNotBlank())
    }
}

@Composable
internal fun ConfirmDialog(title: String, text: String, confirm: String, danger: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val c = Penny.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface2, titleContentColor = c.textPrimary, textContentColor = c.textSecondary,
        shape = Penny.dims.cardShape,
        title = { Text(title, style = Penny.type.title) },
        text = { Text(text, style = Penny.type.body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = if (danger) c.danger else c.accent, style = Penny.type.bodyStrong) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) } },
    )
}
