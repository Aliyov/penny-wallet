package com.penny.wallet.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

val ItemPalette: List<Long> = listOf(
    0xFF39FF88, 0xFF00D1FF, 0xFF7C5CFF, 0xFFFF4FD8, 0xFFFF9F0A, 0xFFFFD60A,
    0xFFFF453A, 0xFF5AC8FA, 0xFFBF5AF2, 0xFF64D2FF, 0xFFA3E635, 0xFF8E8E93,
)

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

fun pennyColors(accentArgb: Long, dark: Boolean, pureBlack: Boolean): PennyColors =
    if (dark) darkColors(Color(accentArgb), pureBlack) else lightColors(Color(accentArgb))

private fun darkColors(accent: Color, pureBlack: Boolean) = PennyColors(
    background = if (pureBlack) Color(0xFF000000) else Color(0xFF111113),
    surface1 = if (pureBlack) Color(0xFF0D0D0F) else Color(0xFF1A1A1E),
    surface2 = if (pureBlack) Color(0xFF16161A) else Color(0xFF232328),
    hairline = Color.White.copy(alpha = 0.08f),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFF9A9AA0),
    accent = accent,
    accentDim = lerp(accent, Color.Black, 0.45f),
    onAccent = onColor(accent),
    danger = Color(0xFFFF453A),
    warning = Color(0xFFFFD60A),
)

// Neon accents are unreadable on white, so the light theme darkens the accent until text in it passes WCAG AA.
private fun lightColors(accent: Color): PennyColors {
    val background = Color(0xFFF4F4F6)
    val readable = darkenForContrast(accent, background, 4.5f)
    return PennyColors(
        background = background,
        surface1 = Color(0xFFFFFFFF),
        surface2 = Color(0xFFEAEAEE),
        hairline = Color.Black.copy(alpha = 0.10f),
        textPrimary = Color(0xFF111113),
        textSecondary = Color(0xFF5E5E66),
        accent = readable,
        accentDim = lerp(readable, Color.White, 0.75f),
        onAccent = onColor(readable),
        danger = Color(0xFFC9261C),
        warning = Color(0xFF9A5B00),
    )
}

fun onColor(bg: Color): Color =
    if (contrast(Color.Black, bg) >= contrast(Color.White, bg)) Color.Black else Color.White

fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

private fun darkenForContrast(c: Color, bg: Color, target: Float): Color {
    var out = c
    var f = 0f
    while (contrast(out, bg) < target && f < 1f) {
        f += 0.05f
        out = lerp(c, Color.Black, f)
    }
    return out
}
