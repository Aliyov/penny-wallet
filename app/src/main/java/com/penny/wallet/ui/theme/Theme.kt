package com.penny.wallet.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.prefs.AnimationLevel
import com.penny.wallet.data.prefs.BackgroundStyle
import com.penny.wallet.data.prefs.Density
import com.penny.wallet.data.prefs.GlowLevel
import com.penny.wallet.data.prefs.UserPrefs

@Immutable
data class PennyDims(
    val corner: Dp,
    val cornerSmall: Dp,
    val gutter: Dp,
    val gap: Dp,
    val cardPadding: Dp,
) {
    val cardShape get() = RoundedCornerShape(corner)
    val smallShape get() = RoundedCornerShape(cornerSmall)
}

@Immutable
data class PennyMotion(val level: AnimationLevel, val haptics: Boolean, val glow: GlowLevel, val hudGrid: Boolean) {
    val enabled get() = level != AnimationLevel.OFF
    /** Default spring for the app; snaps when animations are off. */
    fun <T> spec(stiffness: Float = Spring.StiffnessMediumLow): AnimationSpec<T> = when (level) {
        AnimationLevel.FULL -> spring(Spring.DampingRatioMediumBouncy, stiffness)
        AnimationLevel.REDUCED -> spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)
        AnimationLevel.OFF -> snap()
    }
    val glowAlpha get() = when (glow) { GlowLevel.OFF -> 0f; GlowLevel.SUBTLE -> 0.35f; GlowLevel.STRONG -> 0.6f }
}

val LocalPennyColors = staticCompositionLocalOf { pennyColors(0xFF39FF88, false) }
val LocalPennyType = staticCompositionLocalOf { pennyType(1f) }
val LocalPennyDims = staticCompositionLocalOf { PennyDims(28.dp, 16.dp, 20.dp, 12.dp, 20.dp) }
val LocalPennyMotion = staticCompositionLocalOf { PennyMotion(AnimationLevel.FULL, true, GlowLevel.SUBTLE, true) }
val LocalUserPrefs = staticCompositionLocalOf { UserPrefs() }

object Penny {
    val colors: PennyColors @Composable @ReadOnlyComposable get() = LocalPennyColors.current
    val type: PennyType @Composable @ReadOnlyComposable get() = LocalPennyType.current
    val dims: PennyDims @Composable @ReadOnlyComposable get() = LocalPennyDims.current
    val motion: PennyMotion @Composable @ReadOnlyComposable get() = LocalPennyMotion.current
    val prefs: UserPrefs @Composable @ReadOnlyComposable get() = LocalUserPrefs.current
}

@Composable
fun PennyTheme(prefs: UserPrefs, content: @Composable () -> Unit) {
    val colors = remember(prefs.accentArgb, prefs.background) {
        pennyColors(prefs.accentArgb, prefs.background == BackgroundStyle.DARK_GREY)
    }
    val type = remember(prefs.textSize) { pennyType(prefs.textSize.scale) }
    val compact = prefs.density == Density.COMPACT
    val dims = remember(prefs.corners, compact) {
        val c = prefs.corners.dp.dp
        PennyDims(
            corner = c, cornerSmall = (prefs.corners.dp * 0.6f).dp,
            gutter = if (compact) 16.dp else 20.dp, gap = if (compact) 8.dp else 12.dp,
            cardPadding = if (compact) 14.dp else 20.dp,
        )
    }
    val motion = remember(prefs.animations, prefs.haptics, prefs.glow, prefs.hudGrid) {
        PennyMotion(prefs.animations, prefs.haptics, prefs.glow, prefs.hudGrid)
    }
    // Material You dynamic colour is intentionally NOT used.
    val scheme = darkColorScheme(
        primary = colors.accent, onPrimary = colors.onAccent, secondary = colors.accent,
        background = colors.background, onBackground = colors.textPrimary,
        surface = colors.surface1, onSurface = colors.textPrimary,
        surfaceVariant = colors.surface2, onSurfaceVariant = colors.textSecondary,
        surfaceContainer = colors.surface2, surfaceContainerHigh = colors.surface2,
        surfaceContainerLow = colors.surface1, surfaceContainerHighest = colors.surface2,
        error = colors.danger, outline = colors.hairline, outlineVariant = colors.hairline,
    )
    CompositionLocalProvider(
        LocalPennyColors provides colors, LocalPennyType provides type, LocalPennyDims provides dims,
        LocalPennyMotion provides motion, LocalUserPrefs provides prefs,
    ) {
        MaterialTheme(colorScheme = scheme, typography = materialTypography(type), content = content)
    }
}
