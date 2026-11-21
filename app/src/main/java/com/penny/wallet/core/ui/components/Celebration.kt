package com.penny.wallet.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.penny.wallet.core.ui.theme.ItemPalette
import com.penny.wallet.core.ui.theme.Penny
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Particle(val angle: Float, val speed: Float, val color: Color, val size: Float, val spin: Float, val round: Boolean)

private const val DURATION_MS = 1500

/** Confetti burst overlay shown when a goal reaches 100%. Skipped when animations are off. */
@Composable
fun Celebration(visible: Boolean, onFinished: () -> Unit) {
    val finished by rememberUpdatedState(onFinished)
    val enabled = Penny.motion.enabled
    val t = remember { Animatable(0f) }
    val particles = remember(visible) {
        val r = Random(System.nanoTime())
        List(90) {
            Particle(
                angle = r.nextFloat() * 2f * Math.PI.toFloat(),
                speed = 0.35f + r.nextFloat() * 0.75f,
                color = Color(ItemPalette[r.nextInt(ItemPalette.size)]),
                size = 6f + r.nextFloat() * 10f,
                spin = (r.nextFloat() - 0.5f) * 720f,
                round = r.nextBoolean(),
            )
        }
    }
    LaunchedEffect(visible, enabled) {
        if (!visible) return@LaunchedEffect
        if (!enabled) { finished(); return@LaunchedEffect }
        t.snapTo(0f)
        t.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        finished()
    }
    if (!visible || !enabled) return
    Canvas(Modifier.fillMaxSize()) {
        val p = t.value
        val origin = Offset(size.width / 2f, size.height * 0.38f)
        val reach = size.minDimension * 0.75f
        val gravity = size.height * 0.6f
        val alpha = (1f - p).coerceIn(0f, 1f)
        // ease-out burst, then gravity pulls particles down
        val burst = 1f - (1f - p) * (1f - p)
        particles.forEach { pt ->
            val d = reach * pt.speed * burst
            val pos = Offset(origin.x + cos(pt.angle) * d, origin.y + sin(pt.angle) * d + gravity * p * p)
            val col = pt.color.copy(alpha = alpha)
            if (pt.round) drawCircle(col, pt.size / 2f, pos)
            else rotate(pt.spin * p, pos) {
                drawRect(col, Offset(pos.x - pt.size / 2f, pos.y - pt.size / 4f), Size(pt.size, pt.size / 2f))
            }
        }
    }
}
