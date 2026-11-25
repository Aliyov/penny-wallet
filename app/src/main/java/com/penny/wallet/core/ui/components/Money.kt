package com.penny.wallet.core.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.core.util.currentLocale

/** Privacy mode: amounts show •••• until the user taps one (reveals all until the app restarts / prefs change). */
class PrivacyState { var revealed by mutableStateOf(false) }
val LocalPrivacy = staticCompositionLocalOf { PrivacyState() }

/** Formats cents with the user's number style. Respects privacy mode unless [ignorePrivacy]. */
@Composable
fun money(cents: Long, signed: Boolean = false, ignorePrivacy: Boolean = false): String {
    val p = Penny.prefs
    if (p.hideAmounts && !ignorePrivacy && !LocalPrivacy.current.revealed) return Formatters.HIDDEN
    return Formatters.money(cents, Formatters.of(p, currentLocale()), signed)
}

/**
 * Shrinks instead of clipping when a long amount or currency prefix ("US$", "R$") doesn't fit (down to half size).
 * [fitCents]: the value to size for, so a counting-up animation keeps one size instead of jumping as digits appear.
 */
@Composable
fun MoneyText(cents: Long, style: TextStyle, color: Color, modifier: Modifier = Modifier, signed: Boolean = false, fitCents: Long = cents) {
    val privacy = LocalPrivacy.current
    val hidden = Penny.prefs.hideAmounts && !privacy.revealed
    val resolved = style.copy(color = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } })
    val text = money(cents, signed)
    val fitText = money(fitCents, signed)
    val clickable = if (hidden) modifier.clickableNoRipple { privacy.revealed = true } else modifier
    if (!style.fontSize.isSp) {
        BasicText(text, clickable, resolved, maxLines = 1, softWrap = false)
        return
    }
    BoxWithConstraints(clickable) {
        val measurer = rememberTextMeasurer()
        val maxWidth = constraints.maxWidth
        val size = remember(fitText, resolved, maxWidth) { fittingSize(fitText, resolved, maxWidth, measurer) }
        // Keep the full-size line height when shrunk, so rows and cards below don't move.
        val shown = if (size == resolved.fontSize) resolved
        else resolved.copy(fontSize = size, lineHeight = if (resolved.lineHeight.isSp) resolved.lineHeight else resolved.fontSize * 1.2f)
        BasicText(text, style = shown, maxLines = 1, softWrap = false)
    }
}

private fun fittingSize(text: String, style: TextStyle, maxWidth: Int, measurer: androidx.compose.ui.text.TextMeasurer): TextUnit {
    if (maxWidth == Constraints.Infinity) return style.fontSize
    val full = style.fontSize.value
    var size = full
    while (size > full / 2) {
        val width = measurer.measure(text, style.copy(fontSize = size.sp), maxLines = 1, softWrap = false).size.width
        if (width <= maxWidth) break
        size -= 1f
    }
    return size.coerceAtLeast(full / 2).sp
}
