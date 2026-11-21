package com.penny.wallet.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.penny.wallet.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private const val TNUM = "tnum"

@Immutable
data class PennyType(
    val hero: TextStyle,
    val amountLarge: TextStyle,
    val amount: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

fun pennyType(scale: Float): PennyType {
    fun s(size: Float, w: FontWeight, ls: Float = 0f, tnum: Boolean = false) = TextStyle(
        fontFamily = Inter, fontWeight = w, fontSize = (size * scale).sp,
        letterSpacing = ls.em, fontFeatureSettings = if (tnum) TNUM else null,
    )
    return PennyType(
        hero = s(58f, FontWeight.SemiBold, -0.02f, true),
        amountLarge = s(34f, FontWeight.SemiBold, -0.02f, true),
        amount = s(17f, FontWeight.SemiBold, -0.01f, true),
        title = s(22f, FontWeight.SemiBold, -0.01f),
        body = s(15f, FontWeight.Normal),
        bodyStrong = s(15f, FontWeight.Medium),
        label = s(13f, FontWeight.Medium),
        caption = s(11f, FontWeight.Medium, 0.02f),
    )
}

fun materialTypography(t: PennyType) = Typography(
    bodyLarge = t.body, bodyMedium = t.body, bodySmall = t.label,
    titleLarge = t.title, titleMedium = t.bodyStrong, titleSmall = t.label,
    labelLarge = t.bodyStrong, labelMedium = t.label, labelSmall = t.caption,
    headlineLarge = t.amountLarge, headlineMedium = t.title, headlineSmall = t.title,
)
