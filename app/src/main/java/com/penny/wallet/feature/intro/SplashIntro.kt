package com.penny.wallet.feature.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.penny.wallet.R

// Geometry of the layers in the source artwork's pixels (app_icon.jpg). The system splash draws the empty wallet
// (splash_icon) centred on the mark's centre, with the mark's farthest pixel at 368 px of the 1152 px icon.
private const val CX = 186.5f
private const val CY = 181f
private const val RMAX = 237.95f
private const val ICON_SPAN = 368f / 1152f
private const val BAR_BASE = 321f
private val BARS = listOf(44f to 87f, 96f to 137f, 150f to 192f)
private const val NOTES_DROP = 125f
private const val BODY_TOP = 126f
private const val TEXT_X = -185f
private const val TEXT_Y = 395f
private const val LIFT = 75f

private const val TOTAL = 1510f
private val Ease = CubicBezierEasing(.2f, .8f, .2f, 1f)
private val Spring = CubicBezierEasing(.34f, 1.56f, .64f, 1f)
private val EaseIn = CubicBezierEasing(.42f, 0f, 1f, 1f)

private fun seg(t: Float, delay: Float, duration: Float, easing: Easing) =
    easing.transform(((t - delay) / duration).coerceIn(0f, 1f))

/**
 * Cold-start animation. Its first frame is the system splash (black, empty wallet), so it takes over without a jump:
 * the notes rise, the bars grow, the name fades in, then everything fades into the app.
 * [iconBounds] is the system splash's icon view in window pixels; null means centred on screen.
 */
@Composable
fun SplashIntro(iconBounds: Rect?, play: Boolean, onDone: () -> Unit) {
    val notes = ImageBitmap.imageResource(R.drawable.splash_intro_notes)
    val body = ImageBitmap.imageResource(R.drawable.splash_intro_body)
    val bars = ImageBitmap.imageResource(R.drawable.splash_intro_bars)
    val penny = ImageBitmap.imageResource(R.drawable.splash_intro_text_penny)
    val wallet = ImageBitmap.imageResource(R.drawable.splash_intro_text_wallet)
    val time = remember { Animatable(0f) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(play) {
        if (!play) return@LaunchedEffect
        time.animateTo(TOTAL, tween(TOTAL.toInt(), easing = LinearEasing))
        onDone()
    }
    Canvas(
        Modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            .graphicsLayer {
                val exit = seg(time.value, 1250f, 260f, EaseIn)
                alpha = 1f - exit
                scaleX = 1f + 0.06f * exit
                scaleY = scaleX
            }
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }
            .clearAndSetSemantics {},
    ) {
        val t = time.value
        drawRect(Color.Black)
        // The splash icon is always drawn 288 dp wide (Android 12+ scales its 192 dp view by 1.5); only the centre varies.
        val icon = Rect(iconBounds?.translate(-origin)?.center ?: center, 144.dp.toPx())
        val u = icon.width * ICON_SPAN / RMAX
        val lift = LIFT * u * seg(t, 525f, 525f, Ease)
        val mx = icon.center.x - CX * u
        val my = icon.center.y - CY * u - lift

        // Clipped at the wallet's top edge so the notes never show through the clasp's ring while they rise.
        clipRect(bottom = my + BODY_TOP * u) { layer(notes, mx, my + NOTES_DROP * u * (1f - seg(t, 150f, 450f, Spring)), u) }
        layer(body, mx, my, u)
        BARS.forEachIndexed { i, (x0, x1) ->
            val grow = seg(t, 380f + i * 90f, 320f, Spring)
            if (grow > 0f) clipRect(mx + x0 * u, 0f, mx + x1 * u, size.height) {
                scale(1f, grow, Offset(0f, my + BAR_BASE * u)) { layer(bars, mx, my, u) }
            }
        }
        val tx = mx + TEXT_X * u
        val ty = my + TEXT_Y * u
        seg(t, 780f, 380f, Ease).let { layer(penny, tx, ty + 30f * u * (1f - it), u, it) }
        seg(t, 860f, 380f, Ease).let { layer(wallet, tx, ty + 30f * u * (1f - it), u, it) }
    }
}

private fun DrawScope.layer(image: ImageBitmap, x: Float, y: Float, u: Float, alpha: Float = 1f) {
    if (alpha <= 0f) return
    translate(x, y) { scale(u, Offset.Zero) { drawImage(image, alpha = alpha) } }
}
