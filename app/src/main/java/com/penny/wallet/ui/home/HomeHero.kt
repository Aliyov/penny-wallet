package com.penny.wallet.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.prefs.HeroMetric
import com.penny.wallet.ui.components.AnimatedAmount
import com.penny.wallet.ui.components.money
import com.penny.wallet.ui.theme.Penny
import kotlin.math.hypot

/** Hero number with radial accent glow and optional HUD grid (SPEC §7, §7a Home). */
@Composable
fun HomeHero(state: HomeState, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val prefs = Penny.prefs
    val s = state.summary
    val (label, cents) = when (prefs.heroMetric) {
        HeroMetric.AVAILABLE -> "Available to spend" to s.availableToSpend
        HeroMetric.TOTAL -> "Total balance" to s.totalBalance
        HeroMetric.SPENT_MONTH -> "Spent this month" to state.spentThisMonth
    }
    val color = when {
        prefs.heroMetric == HeroMetric.AVAILABLE && s.availableToSpend < 0 -> c.danger
        prefs.heroMetric == HeroMetric.AVAILABLE -> c.accent
        else -> c.textPrimary
    }
    Box(modifier.fillMaxWidth()) {
        HeroBackdrop(Modifier.matchParentSize(), glowColor = color, grid = prefs.hudGrid)
        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp).animateContentSize(sizeSpec())) {
            Text("Penny", style = Penny.type.bodyStrong, color = c.accent)
            Spacer(Modifier.height(28.dp))
            Text(label, style = Penny.type.caption, color = c.textSecondary)
            Spacer(Modifier.height(4.dp))
            AnimatedAmount(cents, Penny.type.hero, color)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${money(s.totalBalance)} total", style = Penny.type.label, color = c.textSecondary)
                Text("·", style = Penny.type.label, color = c.textSecondary)
                Text("${money(s.totalSaved)} saved", style = Penny.type.label, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun HeroBackdrop(modifier: Modifier, glowColor: Color, grid: Boolean) {
    val glowAlpha = Penny.motion.glowAlpha
    val line = Penny.colors.hairline
    Canvas(modifier) {
        val center = Offset(size.width * 0.3f, size.height * 0.6f)
        val radius = hypot(size.width, size.height) * 0.6f
        if (glowAlpha > 0f) {
            drawCircle(
                Brush.radialGradient(listOf(glowColor.copy(alpha = 0.35f * glowAlpha), Color.Transparent), center, radius),
                radius, center,
            )
        }
        if (grid) {
            val step = 24.dp.toPx()
            val stroke = 0.5.dp.toPx()
            // Each line fades out radially from the centre by drawing it with a radial brush.
            val fade = Brush.radialGradient(listOf(line, line.copy(alpha = 0f)), center, radius * 0.9f)
            var x = (center.x % step)
            while (x < size.width) { drawLine(fade, Offset(x, 0f), Offset(x, size.height), stroke); x += step }
            var y = (center.y % step)
            while (y < size.height) { drawLine(fade, Offset(0f, y), Offset(size.width, y), stroke); y += step }
        }
    }
}
