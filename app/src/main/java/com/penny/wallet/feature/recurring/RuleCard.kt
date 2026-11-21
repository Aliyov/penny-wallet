package com.penny.wallet.feature.recurring

import com.penny.wallet.core.util.dateText
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.domain.DueState
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.ui.components.IconBadge
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.theme.Penny
import java.time.DayOfWeek
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RuleCard(
    row: RuleRow, modifier: Modifier = Modifier,
    onEdit: () -> Unit, onPayNow: () -> Unit, onToggleActive: () -> Unit, onDelete: () -> Unit,
) {
    val c = Penny.colors
    val r = row.rule
    var menu by remember { mutableStateOf(false) }
    val paused = row.state == DueState.PAUSED
    val shape = Penny.dims.cardShape
    Row(
        modifier.fillMaxWidth().clip(shape).background(c.surface1).border(0.75.dp, c.hairline, shape)
            .combinedClickable(onClick = onEdit, onLongClick = { menu = true })
            .padding(Penny.dims.cardPadding),
        verticalAlignment = Alignment.Top,
    ) {
        IconBadge(row.category?.icon ?: "category", row.category?.let { Color(it.color) } ?: c.textSecondary, Modifier.alpha(if (paused) 0.5f else 1f))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.name, style = Penny.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                MoneyText(r.amountCents, Penny.type.bodyStrong, if (paused) c.textSecondary else c.textPrimary)
            }
            Text(frequencyText(r), style = Penny.type.caption, color = c.textSecondary)
            row.account?.let { Text(it.name, style = Penny.type.caption, color = c.textSecondary) }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (paused) "Paused" else "Next: ${dateText().day(r.nextDueDate)}",
                    style = Penny.type.label, color = c.textPrimary, modifier = Modifier.weight(1f, fill = false),
                )
                when (row.state) {
                    DueState.OVERDUE -> Pill("Overdue", c.danger)
                    DueState.DUE_TODAY -> Pill("Due today", c.warning)
                    DueState.PAUSED -> Pill("Paused", c.textSecondary)
                    else -> Unit
                }
                if (r.autoPay && !paused) Pill("Auto-pay", c.accent)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Box {
                Icon(
                    Icons.Rounded.MoreVert, "More", tint = c.textSecondary,
                    modifier = Modifier.padding(start = 4.dp).clip(RoundedCornerShape(50)).clickable { menu = true }.padding(4.dp).size(20.dp),
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.surface2) {
                    DropdownMenuItem(text = { Text("Edit", color = c.textPrimary) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(if (r.active) "Pause" else "Resume", color = c.textPrimary) }, onClick = { menu = false; onToggleActive() })
                    DropdownMenuItem(text = { Text("Delete", color = c.danger) }, onClick = { menu = false; onDelete() })
                }
            }
            if (!paused) SmallButton("Pay now", Modifier.padding(top = 8.dp), onPayNow)
        }
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Text(
        text, style = Penny.type.caption, color = color, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun SmallButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    val c = Penny.colors
    val shape = RoundedCornerShape(50)
    Text(
        text, style = Penny.type.label, color = c.accent, maxLines = 1,
        modifier = modifier.clip(shape).background(c.accent.copy(alpha = 0.12f)).border(0.75.dp, c.accent.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

internal fun ordinal(n: Int): String = n.toString() + when {
    n % 100 in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}

internal fun weekdayName(iso: Int, style: TextStyle = TextStyle.FULL): String =
    DayOfWeek.of(iso.coerceIn(1, 7)).getDisplayName(style, Locale.ENGLISH)

internal fun monthName(m: Int, style: TextStyle = TextStyle.SHORT): String =
    Month.of(m.coerceIn(1, 12)).getDisplayName(style, Locale.ENGLISH)

/** "Every month on the 14th" / "Every Friday" / "Every year on 3 Mar". */
internal fun frequencyText(r: RecurringExpense): String = when (r.frequency) {
    Frequency.WEEKLY -> "Every ${weekdayName(r.dayOfWeek ?: r.nextDueDate.dayOfWeek.value)}"
    Frequency.MONTHLY -> "Every month on the ${ordinal(r.dayOfMonth ?: r.nextDueDate.dayOfMonth)}"
    Frequency.YEARLY -> "Every year on ${r.dayOfMonth ?: r.nextDueDate.dayOfMonth} ${monthName(r.month ?: r.nextDueDate.monthValue)}"
}
