package com.penny.wallet.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.penny.wallet.domain.Formatters
import com.penny.wallet.ui.theme.Penny

/** Privacy mode: amounts show •••• until the user taps one (reveals all until the app restarts / prefs change). */
class PrivacyState { var revealed by mutableStateOf(false) }
val LocalPrivacy = staticCompositionLocalOf { PrivacyState() }

/** Formats cents with the user's number style. Respects privacy mode unless [ignorePrivacy]. */
@Composable
fun money(cents: Long, signed: Boolean = false, ignorePrivacy: Boolean = false): String {
    val p = Penny.prefs
    if (p.hideAmounts && !ignorePrivacy && !LocalPrivacy.current.revealed) return Formatters.HIDDEN
    return Formatters.money(cents, Formatters.of(p), signed)
}

@Composable
fun MoneyText(cents: Long, style: TextStyle, color: Color, modifier: Modifier = Modifier, signed: Boolean = false) {
    val privacy = LocalPrivacy.current
    val hidden = Penny.prefs.hideAmounts && !privacy.revealed
    Text(money(cents, signed), style = style, color = color, maxLines = 1,
        modifier = if (hidden) modifier.clickableNoRipple { privacy.revealed = true } else modifier)
}
