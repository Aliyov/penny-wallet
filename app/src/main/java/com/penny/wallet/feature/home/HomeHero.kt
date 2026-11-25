package com.penny.wallet.feature.home

import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import com.penny.wallet.data.prefs.HeroMetric
import com.penny.wallet.core.ui.components.AnimatedAmount
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.Penny
import kotlin.math.hypot

/** Hero number with radial accent glow and optional HUD grid. */
@Composable
fun HomeHero(state: HomeState, onOpenSearch: () -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val prefs = Penny.prefs
    val s = state.summary
    val (label, cents) = when (prefs.heroMetric) {
        HeroMetric.AVAILABLE -> stringResource(R.string.home_available_to_spend) to s.availableToSpend
        HeroMetric.TOTAL -> stringResource(R.string.home_total_balance) to s.totalBalance
        HeroMetric.SPENT_MONTH -> stringResource(R.string.home_spent_this_month) to state.spentThisMonth
    }
    val color = when {
        prefs.heroMetric == HeroMetric.AVAILABLE && s.availableToSpend < 0 -> c.danger
        prefs.heroMetric == HeroMetric.AVAILABLE -> c.accent
        else -> c.textPrimary
    }
    Box(modifier.fillMaxWidth()) {
        HeroBackdrop(Modifier.matchParentSize(), glowColor = color, grid = prefs.hudGrid)
        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp).animateContentSize(sizeSpec())) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_brand), style = Penny.type.bodyStrong, color = c.accent, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSearch) { Icon(Icons.Rounded.Search, stringResource(R.string.search_open), tint = c.textPrimary) }
            }
            Spacer(Modifier.height(16.dp))
            Text(label, style = Penny.type.caption, color = c.textSecondary)
            Spacer(Modifier.height(4.dp))
            AnimatedAmount(cents, Penny.type.hero, color)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_hero_total, money(s.totalBalance)), style = Penny.type.label, color = c.textSecondary)
                Text("·", style = Penny.type.label, color = c.textSecondary)
                Text(stringResource(R.string.home_hero_saved, money(s.totalSaved)), style = Penny.type.label, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun HeroBackdrop(modifier: Modifier, glowColor: Color, grid: Boolean) {
    val glowAlpha = Penny.motion.glowAlpha
    val line = Penny.colors.hairline
    // Brushes and the grid path are built once per size (drawWithCache); graphicsLayer gives the backdrop its own
    // layer so the counting hero number above it doesn't force a redraw.
    Spacer(
        modifier.graphicsLayer().drawWithCache {
            val center = Offset(size.width * 0.3f, size.height * 0.6f)
            val radius = hypot(size.width, size.height) * 0.6f
            val glowBrush = Brush.radialGradient(listOf(glowColor.copy(alpha = 0.35f * glowAlpha), Color.Transparent), center, radius)
            val fade = Brush.radialGradient(listOf(line, line.copy(alpha = 0f)), center, radius * 0.9f)
            val step = 24.dp.toPx()
            val gridPath = Path().apply {
                var x = center.x % step
                while (x < size.width) { moveTo(x, 0f); lineTo(x, size.height); x += step }
                var y = center.y % step
                while (y < size.height) { moveTo(0f, y); lineTo(size.width, y); y += step }
            }
            val stroke = Stroke(0.5.dp.toPx())
            onDrawBehind {
                if (glowAlpha > 0f) drawCircle(glowBrush, radius, center)
                if (grid) drawPath(gridPath, fade, style = stroke)
            }
        },
    )
}
