package com.penny.wallet.feature.recurring

import androidx.compose.material3.minimumInteractiveComponentSize
import com.penny.wallet.core.util.currentLocale
import com.penny.wallet.R
import androidx.compose.ui.res.stringResource
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
import java.time.LocalDate

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
                    if (paused) stringResource(R.string.recurring_paused) else stringResource(R.string.recurring_next, dateText().day(r.nextDueDate)),
                    style = Penny.type.label, color = c.textPrimary, modifier = Modifier.weight(1f, fill = false),
                )
                when (row.state) {
                    DueState.OVERDUE -> Pill(stringResource(R.string.recurring_overdue), c.danger)
                    DueState.DUE_TODAY -> Pill(stringResource(R.string.recurring_due_today), c.warning)
                    DueState.PAUSED -> Pill(stringResource(R.string.recurring_paused), c.textSecondary)
                    else -> Unit
                }
                if (r.autoPay && !paused) Pill(stringResource(R.string.recurring_auto_pay), c.accent)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Box {
                Icon(
                    Icons.Rounded.MoreVert, stringResource(R.string.recurring_more), tint = c.textSecondary,
                    modifier = Modifier.padding(start = 4.dp).minimumInteractiveComponentSize().clip(RoundedCornerShape(50)).clickable { menu = true }.padding(4.dp).size(20.dp),
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.surface2) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_edit), color = c.textPrimary) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(if (r.active) R.string.recurring_pause else R.string.recurring_resume), color = c.textPrimary) }, onClick = { menu = false; onToggleActive() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_delete), color = c.danger) }, onClick = { menu = false; onDelete() })
                }
            }
            if (!paused) SmallButton(stringResource(R.string.recurring_pay_now), Modifier.padding(top = 8.dp), onPayNow)
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

private fun anchorDate(month: Int, day: Int): LocalDate {
    val m = Month.of(month.coerceIn(1, 12))
    // Leap year so 29 Feb is a valid date.
    return LocalDate.of(2000, m, day.coerceIn(1, m.maxLength()))
}

@Composable
internal fun frequencyText(r: RecurringExpense): String {
    val day = r.dayOfMonth ?: r.nextDueDate.dayOfMonth
    return when (r.frequency) {
        Frequency.WEEKLY -> stringResource(
            R.string.recurring_every_weekday,
            DayOfWeek.of((r.dayOfWeek ?: r.nextDueDate.dayOfWeek.value).coerceIn(1, 7)).getDisplayName(TextStyle.FULL, currentLocale()),
        )
        Frequency.MONTHLY -> stringResource(R.string.recurring_every_month_on, day)
        Frequency.YEARLY -> stringResource(R.string.recurring_every_year_on, dateText().shortDate(anchorDate(r.month ?: r.nextDueDate.monthValue, day)))
    }
}
