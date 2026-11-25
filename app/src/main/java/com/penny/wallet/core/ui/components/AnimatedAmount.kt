package com.penny.wallet.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.penny.wallet.data.prefs.AnimationLevel
import com.penny.wallet.core.ui.theme.Penny
import kotlin.math.roundToLong

/** Big number that counts to its new value. Instant when animations are off. */
@Composable
fun AnimatedAmount(cents: Long, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val level = Penny.motion.level
    val anim = remember { Animatable(cents.toFloat()) }
    LaunchedEffect(cents, level) {
        when (level) {
            AnimationLevel.OFF -> anim.snapTo(cents.toFloat())
            else -> anim.animateTo(cents.toFloat(), tween(if (level == AnimationLevel.FULL) 700 else 300, easing = FastOutSlowInEasing))
        }
    }
    // Float precision is fine for display of the in-between frames; the final frame shows the exact Long.
    val shown = if (anim.isRunning) anim.value.roundToLong() else cents
    MoneyText(shown, style, color, modifier, fitCents = cents)
}
