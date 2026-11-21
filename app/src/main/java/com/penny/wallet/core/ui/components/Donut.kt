package com.penny.wallet.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.theme.Penny
import kotlin.math.atan2
import kotlin.math.hypot

/** One donut segment. [share] is 0..1 (pre-computed by the ViewModel). */
data class DonutSlice(val share: Float, val color: Color)

/**
 * Reusable Canvas donut: rounded caps, small gaps, animated sweep on appear / data change.
 * Tap a slice to select it (others dim); tapping it again or outside the ring clears.
 */
@Composable
fun Donut(
    slices: List<DonutSlice>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    thickness: Dp = 22.dp,
    gapDegrees: Float = 2.5f,
    centerContent: @Composable BoxScope.() -> Unit = {},
) {
    val motion = Penny.motion
    val track = Penny.colors.hairline
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        if (!motion.enabled) { progress.snapTo(1f); return@LaunchedEffect }
        progress.snapTo(0f)
        progress.animateTo(1f, motion.spec(Spring.StiffnessVeryLow))
    }
    val alphas = slices.indices.map { i ->
        animateFloatAsState(if (selected == null || selected == i) 1f else 0.25f, motion.spec(), label = "slice$i").value
    }
    // Cumulative start angles (0° = 12 o'clock, clockwise).
    val starts = remember(slices) { slices.runningFold(0f) { acc, s -> acc + s.share * 360f } }

    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(slices, selected) {
                detectTapGestures { p ->
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val stroke = thickness.toPx()
                    val radius = minOf(cx, cy) - stroke / 2f
                    val dist = hypot(p.x - cx, p.y - cy)
                    if (slices.isEmpty() || dist < radius - stroke || dist > radius + stroke) { onSelect(null); return@detectTapGestures }
                    val deg = ((Math.toDegrees(atan2((p.y - cy).toDouble(), (p.x - cx).toDouble())).toFloat() + 90f) + 360f) % 360f
                    val idx = (slices.indices).firstOrNull { deg >= starts[it] && deg < starts[it + 1] } ?: slices.lastIndex
                    onSelect(if (idx == selected) null else idx)
                }
            },
        ) {
            val stroke = thickness.toPx()
            val radius = size.minDimension / 2f - stroke / 2f
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            if (slices.isEmpty()) return@Canvas
            val p = progress.value.coerceIn(0f, 1f)
            val single = slices.count { it.share > 0f } <= 1
            val capDeg = if (single) 0f else Math.toDegrees((stroke / 2f / radius).toDouble()).toFloat()
            slices.forEachIndexed { i, s ->
                if (s.share <= 0f) return@forEachIndexed
                val full = s.share * 360f * p
                val start = starts[i] * p - 90f
                if (single) {
                    drawArc(s.color.copy(alpha = alphas[i]), start, full, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                } else {
                    val sweep = (full - gapDegrees - 2 * capDeg).coerceAtLeast(0.01f)
                    val st = start + (full - sweep) / 2f
                    drawArc(s.color.copy(alpha = alphas[i]), st, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
        }
        centerContent()
    }
}
