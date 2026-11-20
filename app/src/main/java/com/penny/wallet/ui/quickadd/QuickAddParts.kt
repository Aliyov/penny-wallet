package com.penny.wallet.ui.quickadd

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.prefs.QuickType
import com.penny.wallet.ui.components.DatePickerRow
import com.penny.wallet.ui.components.PennyIcons
import com.penny.wallet.ui.theme.Penny

/** Pill chip with optional icon, tinted with [tint] when selected. */
@Composable
internal fun TintChip(text: String, selected: Boolean, tint: Color, onClick: () -> Unit, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val shape = RoundedCornerShape(50)
    val bg by animateColorAsState(if (selected) tint.copy(alpha = 0.2f) else c.surface1, Penny.motion.spec(), label = "chipBg")
    val border by animateColorAsState(if (selected) tint else c.hairline, Penny.motion.spec(), label = "chipBorder")
    Row(
        modifier.height(40.dp).clip(shape).background(bg).border(0.75.dp, border, shape).clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = if (selected) tint else c.textSecondary, modifier = Modifier.size(18.dp))
        Text(text, style = Penny.type.label, color = if (selected) c.textPrimary else c.textPrimary.copy(alpha = 0.85f), maxLines = 1)
    }
}

private val chipRowPadding = PaddingValues(horizontal = 0.dp)

@Composable
internal fun CategoryChips(categories: List<Category>, selectedId: String?, onTap: (String) -> Unit) {
    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = chipRowPadding) {
        items(categories, key = { it.id }) { cat ->
            TintChip(cat.name, cat.id == selectedId, Color(cat.color), { onTap(cat.id) }, PennyIcons.get(cat.icon))
        }
    }
}

@Composable
internal fun GoalChips(goals: List<Goal>, selectedId: String?, onTap: (String) -> Unit) {
    if (goals.isEmpty()) {
        Text("Create a goal in Savings first", style = Penny.type.caption, color = Penny.colors.textSecondary)
        return
    }
    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = chipRowPadding) {
        items(goals, key = { it.id }) { g ->
            TintChip(g.name, g.id == selectedId, Color(g.color), { onTap(g.id) }, PennyIcons.get(g.icon))
        }
    }
}

@Composable
internal fun AccountChips(accounts: List<Account>, selectedId: String?, onTap: (String) -> Unit, label: String) {
    val c = Penny.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = Penny.type.label, color = c.textSecondary)
        if (accounts.isEmpty()) {
            Text("Add another account first", style = Penny.type.caption, color = c.textSecondary)
            return@Row
        }
        LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accounts, key = { it.id }) { a ->
                TintChip(a.name, a.id == selectedId, c.accent, { onTap(a.id) }, PennyIcons.get(a.icon))
            }
        }
    }
}

/** Compact optional row: account · date · note · make recurring. */
@Composable
internal fun OptionalRow(form: QuickForm, data: QuickData, edit: ((QuickForm) -> QuickForm) -> Unit) {
    val c = Penny.colors
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AccountPicker(form, data.accounts) { id -> edit { f -> f.copy(accountId = id, toAccountId = if (f.toAccountId == id) null else f.toAccountId) } }
        DatePickerRow(form.date, { d -> edit { it.copy(date = d) } })
        if (data.showNote && form.type != QuickType.SAVE) NoteChip(form.note) { n -> edit { it.copy(note = n) } }
        if (form.type == QuickType.EXPENSE && form.editing == null) {
            TintChip("Make recurring", form.recurring, c.accent, { edit { it.copy(recurring = !it.recurring) } }, Icons.Rounded.Repeat)
        }
    }
}

@Composable
private fun AccountPicker(form: QuickForm, accounts: List<Account>, onPick: (String) -> Unit) {
    val c = Penny.colors
    var open by remember { mutableStateOf(false) }
    val current = accounts.firstOrNull { it.id == form.accountId }
    val prefix = if (form.type == QuickType.TRANSFER || form.type == QuickType.SAVE) "From " else ""
    Box {
        TintChip(prefix + (current?.name ?: "Account"), false, c.accent, { if (accounts.size > 1) open = true },
            current?.let { PennyIcons.get(it.icon) } ?: Icons.Rounded.AccountBalanceWallet)
        DropdownMenu(open, { open = false }, containerColor = c.surface2) {
            accounts.forEach { a ->
                DropdownMenuItem(
                    text = { Text(a.name, style = Penny.type.body, color = if (a.id == form.accountId) c.accent else c.textPrimary) },
                    leadingIcon = { Icon(PennyIcons.get(a.icon), null, tint = c.textSecondary) },
                    onClick = { open = false; onPick(a.id) },
                )
            }
        }
    }
}

@Composable
private fun NoteChip(note: String, onChange: (String) -> Unit) {
    val c = Penny.colors
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.height(40.dp).clip(shape).background(c.surface1).border(0.75.dp, c.hairline, shape).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.EditNote, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
        BasicTextField(
            note, { onChange(it.take(120)) }, singleLine = true,
            textStyle = Penny.type.label.copy(color = c.textPrimary), cursorBrush = SolidColor(c.accent),
            modifier = Modifier.widthIn(min = 48.dp, max = 220.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (note.isEmpty()) Text("Note", style = Penny.type.label, color = c.textSecondary)
                    inner()
                }
            },
        )
    }
}
