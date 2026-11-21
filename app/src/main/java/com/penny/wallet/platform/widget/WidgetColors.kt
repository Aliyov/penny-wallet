package com.penny.wallet.platform.widget

import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.penny.wallet.core.ui.theme.pennyColors

/** Widgets follow the system theme (day/night), tinted with the user's accent. */
internal class WidgetColors(d: WidgetData) {
    private val day = pennyColors(d.accentArgb, dark = false, pureBlack = false)
    private val night = pennyColors(d.accentArgb, dark = true, pureBlack = d.pureBlack)

    val background: ColorProvider = ColorProvider(day.surface1, night.surface1)
    val primary: ColorProvider = ColorProvider(day.textPrimary, night.textPrimary)
    val secondary: ColorProvider = ColorProvider(day.textSecondary, night.textSecondary)
    val accent: ColorProvider = ColorProvider(day.accent, night.accent)
    val onAccent: ColorProvider = ColorProvider(day.onAccent, night.onAccent)
    val danger: ColorProvider = ColorProvider(day.danger, night.danger)
}
