package com.penny.wallet.platform.widget

import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.penny.wallet.core.ui.theme.pennyColors

/** Widgets follow the system theme (day/night), tinted with the user's accent. */
internal class WidgetColors(accentArgb: Long, pureBlack: Boolean) {
    constructor(d: WidgetData) : this(d.accentArgb, d.pureBlack)

    private val day = pennyColors(accentArgb, dark = false, pureBlack = false)
    private val night = pennyColors(accentArgb, dark = true, pureBlack = pureBlack)

    val background: ColorProvider = ColorProvider(day.surface1, night.surface1)
    val primary: ColorProvider = ColorProvider(day.textPrimary, night.textPrimary)
    val secondary: ColorProvider = ColorProvider(day.textSecondary, night.textSecondary)
    val accent: ColorProvider = ColorProvider(day.accent, night.accent)
    val onAccent: ColorProvider = ColorProvider(day.onAccent, night.onAccent)
    val danger: ColorProvider = ColorProvider(day.danger, night.danger)
    val warning: ColorProvider = ColorProvider(day.warning, night.warning)
    val track: ColorProvider = ColorProvider(day.surface2, night.surface2)
}
