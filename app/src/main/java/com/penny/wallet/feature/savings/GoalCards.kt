package com.penny.wallet.feature.savings

import com.penny.wallet.R
import androidx.compose.ui.res.stringResource
import com.penny.wallet.core.util.currentLocale
import com.penny.wallet.core.util.dateText
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.PennyIcons
import com.penny.wallet.core.ui.components.ProgressRing
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.Penny

@Composable
internal fun GoalGridCard(card: GoalCard, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Penny.colors
    val color = Color(card.goal.color)
    PennyCard(modifier.fillMaxWidth(), tint = color, onClick = onClick) {
        GoalRing(card, color, Modifier.size(84.dp).align(Alignment.CenterHorizontally))
        Text(
            card.goal.name, style = Penny.type.bodyStrong, color = c.textPrimary, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp),
        )
        GoalDetails(card, color)
    }
}

@Composable
internal fun GoalListRow(card: GoalCard, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Penny.colors
    val color = Color(card.goal.color)
    PennyCard(modifier.fillMaxWidth(), tint = color, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GoalRing(card, color, Modifier.size(64.dp), stroke = 8)
            Column(Modifier.weight(1f)) {
                Text(card.goal.name, style = Penny.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                GoalDetails(card, color)
            }
        }
    }
}

@Composable
private fun GoalRing(card: GoalCard, color: Color, modifier: Modifier, stroke: Int = 9) {
    ProgressRing(card.ring, color, modifier, stroke = stroke.dp) {
        Icon(PennyIcons.get(card.goal.icon), null, tint = color, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun GoalDetails(card: GoalCard, color: Color) {
    val c = Penny.colors
    Text(stringResource(R.string.savings_saved_of_target, money(card.balance), money(card.goal.targetCents)), style = Penny.type.label, color = c.textSecondary, maxLines = 1)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(Formatters.percent(card.progress, currentLocale()), style = Penny.type.bodyStrong, color = color)
        if (card.reached) Text(stringResource(R.string.savings_reached), style = Penny.type.label, color = c.accent)
    }
    card.goal.deadline?.let { d ->
        Text(dateText().fullDate(d), style = Penny.type.caption, color = c.textSecondary, maxLines = 1)
    }
    card.monthlyNeeded?.takeIf { it > 0 }?.let { m ->
        Text(stringResource(R.string.savings_per_month, money(m)), style = Penny.type.caption, color = c.textPrimary, maxLines = 1)
    }
}

@Composable
internal fun NewGoalCard(grid: Boolean, onClick: () -> Unit) {
    val c = Penny.colors
    val shape = Penny.dims.cardShape
    Box(
        Modifier.fillMaxWidth().heightIn(min = if (grid) 180.dp else 72.dp).clip(shape)
            .border(1.dp, c.hairline, shape).clickable(onClick = onClick).padding(Penny.dims.cardPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Add, null, tint = c.accent, modifier = Modifier.size(22.dp))
            Text(stringResource(R.string.savings_new_goal), style = Penny.type.bodyStrong, color = c.accent)
        }
    }
}
