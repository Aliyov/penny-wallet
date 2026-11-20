package com.penny.wallet.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Palette for categories / goals (SPEC §7). Vivid on black. */
val ItemPalette: List<Long> = listOf(
    0xFF39FF88, 0xFF00D1FF, 0xFF7C5CFF, 0xFFFF4FD8, 0xFFFF9F0A, 0xFFFFD60A,
    0xFFFF453A, 0xFF5AC8FA, 0xFFBF5AF2, 0xFF64D2FF, 0xFFA3E635, 0xFF8E8E93,
)

/** Accent presets offered in Personalise. */
val AccentPresets: List<Pair<String, Long>> = listOf(
    "Neon green" to 0xFF39FF88, "Cyan" to 0xFF00D1FF, "Violet" to 0xFF7C5CFF,
    "Magenta" to 0xFFFF4FD8, "Orange" to 0xFFFF9F0A, "White" to 0xFFF5F5F7,
)

@Immutable
data class PennyColors(
    val background: Color,
    val surface1: Color,
    val surface2: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentDim: Color,
    val onAccent: Color,
    val danger: Color,
    val warning: Color,
) {
    val glow: Color get() = accent.copy(alpha = 0.35f)
}

fun pennyColors(accentArgb: Long, darkGrey: Boolean): PennyColors {
    val accent = Color(accentArgb)
    return PennyColors(
        background = if (darkGrey) Color(0xFF111113) else Color(0xFF000000),
        surface1 = if (darkGrey) Color(0xFF1A1A1E) else Color(0xFF0D0D0F),
        surface2 = if (darkGrey) Color(0xFF232328) else Color(0xFF16161A),
        hairline = Color.White.copy(alpha = 0.08f),
        textPrimary = Color(0xFFF5F5F7),
        textSecondary = Color(0xFF8E8E93),
        accent = accent,
        accentDim = lerpToBlack(accent, 0.45f),
        onAccent = Color.Black,
        danger = Color(0xFFFF453A),
        warning = Color(0xFFFFD60A),
    )
}

private fun lerpToBlack(c: Color, f: Float) = Color(c.red * (1 - f), c.green * (1 - f), c.blue * (1 - f), c.alpha)
