package com.penny.wallet.core.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.components.IconBadge
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.theme.Penny

/** One ledger row. Swipe left to delete (caller shows Undo), tap to edit. */
@Composable
fun TxRowItem(row: TxRow, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, onDelete: (() -> Unit)? = null) {
    if (onDelete == null) { TxRowContent(row, modifier, onClick); return }
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(Penny.colors.danger.copy(alpha = 0.18f)).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Rounded.Delete, "Delete", tint = Penny.colors.danger)
            }
        },
        modifier = modifier,
    ) { TxRowContent(row, Modifier.background(Penny.colors.background), onClick) }
}

@Composable
private fun TxRowContent(row: TxRow, modifier: Modifier, onClick: (() -> Unit)?) {
    val c = Penny.colors
    val tint = row.color?.let { Color(it) } ?: when (row.tone) {
        TxRow.Tone.POSITIVE -> c.accent
        else -> c.textSecondary
    }
    Row(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(row.icon, tint)
        Column(Modifier.weight(1f)) {
            Text(row.title, style = Penny.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(row.subtitle, style = Penny.type.label, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MoneyText(
            row.amount, Penny.type.amount,
            when (row.tone) { TxRow.Tone.POSITIVE -> c.accent; TxRow.Tone.NEGATIVE -> c.textPrimary; TxRow.Tone.NEUTRAL -> c.textSecondary },
            signed = row.tone == TxRow.Tone.POSITIVE,
        )
        Box(Modifier.width(0.dp))
    }
}
