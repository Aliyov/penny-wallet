package com.penny.wallet.feature.home

import com.penny.wallet.core.util.dateText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.penny.wallet.domain.DueState
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.ui.common.UpcomingItem
import com.penny.wallet.core.ui.components.IconBadge
import com.penny.wallet.core.ui.components.MoneyText
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.clickableNoRipple
import com.penny.wallet.core.ui.theme.Penny

@Composable
fun UpcomingStrip(items: List<UpcomingItem>, overdueCount: Int, onOpenRecurring: () -> Unit) {
    val gutter = Penny.dims.gutter
    val showBadge = Penny.prefs.overdueBadge
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = gutter).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("UPCOMING", style = Penny.type.caption, color = Penny.colors.textSecondary)
            if (showBadge && overdueCount > 0) {
                Spacer(Modifier.width(8.dp))
                OverdueBadge("$overdueCount overdue")
            }
            Spacer(Modifier.weight(1f))
            Text("All", style = Penny.type.label, color = Penny.colors.accent, modifier = Modifier.clickableNoRipple(onOpenRecurring).padding(4.dp))
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(Penny.dims.gap),
        ) {
            items(items, key = { it.rule.id }) { UpcomingChip(it, showBadge, Modifier.animateItem()) }
        }
    }
}

@Composable
private fun UpcomingChip(item: UpcomingItem, showBadge: Boolean, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val tint = item.color?.let { Color(it) } ?: c.accent
    val overdue = item.state == DueState.OVERDUE
    PennyCard(modifier.width(170.dp), tint = tint, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(item.icon, tint, size = 32)
            Spacer(Modifier.weight(1f))
            if (overdue && showBadge) OverdueBadge("Overdue")
        }
        Spacer(Modifier.height(10.dp))
        Text(item.rule.name, style = Penny.type.bodyStrong, color = c.textPrimary, maxLines = 1)
        Text(
            dateText().day(item.rule.nextDueDate), style = Penny.type.caption,
            color = if (overdue) c.danger else c.textSecondary, maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        MoneyText(item.rule.amountCents, Penny.type.amount, c.textPrimary)
    }
}

@Composable
private fun OverdueBadge(text: String) {
    val c = Penny.colors
    Text(
        text, style = Penny.type.caption, color = c.danger,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(c.danger.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}
