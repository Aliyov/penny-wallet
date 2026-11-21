package com.penny.wallet.core.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Soft neon glow behind a shape. alpha 0 = off.
 * The Paint + BlurMaskFilter are built once per size/colour (drawWithCache), not on every frame.
 */
fun Modifier.glow(color: Color, alpha: Float, radius: Dp = 24.dp, corner: Dp = 999.dp): Modifier =
    if (alpha <= 0f) this else drawWithCache {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.copy(alpha = alpha).toArgb()
            maskFilter = BlurMaskFilter(radius.toPx(), BlurMaskFilter.Blur.NORMAL)
        }
        val r = corner.toPx().coerceAtMost(size.minDimension / 2)
        onDrawBehind {
            drawIntoCanvas { it.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint) }
        }
    }
