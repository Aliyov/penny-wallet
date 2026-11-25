package com.penny.wallet.feature.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.AmountSheet
import com.penny.wallet.core.ui.components.PennyIcons
import com.penny.wallet.core.ui.components.clickableNoRipple
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.domain.SplitPart

/** "Split" chip next to "Scan receipt": turns one expense into several categories. */
@Composable
internal fun SplitChip(on: Boolean, onClick: () -> Unit) {
    TintChip(stringResource(R.string.add_split), on, Penny.colors.accent, onClick, Icons.Rounded.CallSplit)
}

/**
 * One row per part: category and amount. The numpad sets the receipt total; the first part shows the rest, so the parts
 * always add up. Other parts' amounts are typed in an amount sheet.
 */
@Composable
internal fun SplitEditor(
    parts: List<SplitPart>, categories: List<Category>,
    onCategory: (Int, String) -> Unit, onAmount: (Int, Long) -> Unit, onAdd: () -> Unit, onRemove: (Int) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        parts.forEachIndexed { i, p ->
            PartRow(i, p, categories, onCategory = { onCategory(i, it) }, onEditAmount = { editing = i }, onRemove = if (i > 0 && parts.size > 2) ({ onRemove(i) }) else null)
        }
        TextButton(onClick = onAdd) {
            Icon(Icons.Rounded.Add, null, tint = Penny.colors.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_split_add_part), style = Penny.type.label, color = Penny.colors.accent)
        }
    }
    editing?.let { i ->
        val part = parts.getOrNull(i)
        if (part == null || i == 0) { editing = null; return@let }
        AmountSheet(
            stringResource(R.string.add_split_part_amount), onDismiss = { editing = null }, confirmLabel = stringResource(R.string.action_save),
            initialCents = part.amountCents,
        ) { cents -> onAmount(i, cents); editing = null }
    }
}

@Composable
private fun PartRow(
    index: Int, part: SplitPart, categories: List<Category>,
    onCategory: (String) -> Unit, onEditAmount: () -> Unit, onRemove: (() -> Unit)?,
) {
    val c = Penny.colors
    val cat = categories.firstOrNull { it.id == part.categoryId }
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) {
            TintChip(
                cat?.name ?: stringResource(R.string.add_split_pick_category), cat != null, cat?.let { Color(it.color) } ?: c.warning,
                { open = true }, cat?.let { PennyIcons.get(it.icon) } ?: Icons.Rounded.Category,
            )
            DropdownMenu(open, { open = false }, containerColor = c.surface2) {
                categories.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name, style = Penny.type.body, color = if (option.id == part.categoryId) c.accent else c.textPrimary) },
                        leadingIcon = { Icon(PennyIcons.get(option.icon), null, tint = Color(option.color)) },
                        onClick = { open = false; onCategory(option.id) },
                    )
                }
            }
        }
        if (index == 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.add_split_rest), style = Penny.type.caption, color = c.textSecondary)
                Text(money(part.amountCents), style = Penny.type.bodyStrong, color = if (part.amountCents > 0) c.textPrimary else c.danger, textAlign = TextAlign.End)
            }
        } else {
            Text(
                if (part.amountCents > 0) money(part.amountCents) else stringResource(R.string.add_split_set_amount),
                style = Penny.type.bodyStrong, color = if (part.amountCents > 0) c.textPrimary else c.accent,
                modifier = Modifier.clickableNoRipple(onEditAmount),
            )
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Close, stringResource(R.string.add_split_remove_part), tint = c.textSecondary, modifier = Modifier.size(18.dp))
            }
        } else {
            Spacer(Modifier.width(32.dp))
        }
    }
}
