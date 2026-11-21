package com.penny.wallet.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.theme.Penny
import kotlin.math.cos
import kotlin.math.sin

/** Apple Fitness style progress ring (SPEC §7): dim track, rounded arc from 12 o'clock, glowing tip. */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val motion = Penny.motion
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), motion.spec(), label = "ring")
    val glow = motion.glowAlpha
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val radius = size.minDimension / 2f - w / 2f - w * 0.3f
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arc = Size(radius * 2, radius * 2)
            drawArc(color.copy(alpha = 0.18f), 0f, 360f, false, topLeft, arc, style = Stroke(w))
            val p = animated.coerceIn(0f, 1f)
            if (p <= 0.001f) return@Canvas
            val sweep = p * 360f
            drawArc(color, -90f, sweep, false, topLeft, arc, style = Stroke(w, cap = StrokeCap.Round))
            if (glow > 0f) {
                val rad = Math.toRadians((sweep - 90f).toDouble())
                val tip = Offset(center.x + radius * cos(rad).toFloat(), center.y + radius * sin(rad).toFloat())
                drawCircle(
                    Brush.radialGradient(listOf(color.copy(alpha = glow), Color.Transparent), tip, w * 1.6f),
                    radius = w * 1.6f, center = tip,
                )
                drawCircle(Color.White.copy(alpha = glow * 0.8f), radius = w * 0.18f, center = tip)
            }
        }
        content()
    }
}
