package com.penny.wallet.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.prefs.AnimationLevel
import com.penny.wallet.data.prefs.BackgroundStyle
import com.penny.wallet.data.prefs.CornerStyle
import com.penny.wallet.data.prefs.Density
import com.penny.wallet.data.prefs.GlowLevel
import com.penny.wallet.data.prefs.NumberFormatStyle
import com.penny.wallet.data.prefs.TextSize
import com.penny.wallet.domain.Formatters
import com.penny.wallet.ui.account.pennyFieldColors
import com.penny.wallet.ui.components.ChoiceRow
import com.penny.wallet.ui.components.PennyButton
import com.penny.wallet.ui.components.PennySheet
import com.penny.wallet.ui.components.SwitchRow
import com.penny.wallet.ui.theme.AccentPresets
import com.penny.wallet.ui.theme.Penny

internal fun LazyListScope.appearanceSection(x: PrefsCtx) {
    val p = x.p
    val d = x.d
    item("preview") { PreviewCard(x) }
    settingsCard("appearance", "Appearance") {
        AccentPicker(x)
        ChoiceRow(
            "Background", listOf(BackgroundStyle.AMOLED to "AMOLED black", BackgroundStyle.DARK_GREY to "Dark grey"), p.background,
            { v -> x.set { it.copy(background = v) } }, onReset = x.reset(p.background, d.background) { it.copy(background = d.background) },
        )
        ChoiceRow(
            "Glow", listOf(GlowLevel.OFF to "Off", GlowLevel.SUBTLE to "Subtle", GlowLevel.STRONG to "Strong"), p.glow,
            { v -> x.set { it.copy(glow = v) } }, onReset = x.reset(p.glow, d.glow) { it.copy(glow = d.glow) },
        )
        SwitchRow(
            "HUD grid on Home", p.hudGrid, { v -> x.set { it.copy(hudGrid = v) } }, subtitle = "Faint futuristic grid behind the hero number",
            onReset = x.reset(p.hudGrid, d.hudGrid) { it.copy(hudGrid = d.hudGrid) },
        )
        ChoiceRow(
            "Corners", listOf(CornerStyle.ROUND to "Round", CornerStyle.SOFTER to "Softer", CornerStyle.SHARP to "Sharp"), p.corners,
            { v -> x.set { it.copy(corners = v) } }, onReset = x.reset(p.corners, d.corners) { it.copy(corners = d.corners) },
        )
        ChoiceRow(
            "Text size", listOf(TextSize.S to "S", TextSize.M to "M", TextSize.L to "L"), p.textSize,
            { v -> x.set { it.copy(textSize = v) } }, onReset = x.reset(p.textSize, d.textSize) { it.copy(textSize = d.textSize) },
        )
        ChoiceRow(
            "Density", listOf(Density.COMFORTABLE to "Comfortable", Density.COMPACT to "Compact"), p.density,
            { v -> x.set { it.copy(density = v) } }, onReset = x.reset(p.density, d.density) { it.copy(density = d.density) },
        )
        ChoiceRow(
            "Animations", listOf(AnimationLevel.FULL to "Full", AnimationLevel.REDUCED to "Reduced", AnimationLevel.OFF to "Off"), p.animations,
            { v -> x.set { it.copy(animations = v) } }, onReset = x.reset(p.animations, d.animations) { it.copy(animations = d.animations) },
        )
        SwitchRow(
            "Haptics", p.haptics, { v -> x.set { it.copy(haptics = v) } },
            onReset = x.reset(p.haptics, d.haptics) { it.copy(haptics = d.haptics) },
        )
        SwitchRow(
            "Show cents", p.showCents, { v -> x.set { it.copy(showCents = v) } },
            onReset = x.reset(p.showCents, d.showCents) { it.copy(showCents = d.showCents) },
        )
        ChoiceRow(
            "Number format", listOf(NumberFormatStyle.EU to "1.234,56", NumberFormatStyle.US to "1,234.56"), p.numberFormat,
            { v -> x.set { it.copy(numberFormat = v) } }, onReset = x.reset(p.numberFormat, d.numberFormat) { it.copy(numberFormat = d.numberFormat) },
        )
        ChoiceRow(
            "Currency symbol", listOf(true to "Before (€12)", false to "After (12 €)"), p.symbolBefore,
            { v -> x.set { it.copy(symbolBefore = v) } }, onReset = x.reset(p.symbolBefore, d.symbolBefore) { it.copy(symbolBefore = d.symbolBefore) },
        )
        SwitchRow(
            "Hide amounts", p.hideAmounts, { v -> x.set { it.copy(hideAmounts = v) } }, subtitle = "Privacy mode: shows •••• until tapped",
            onReset = x.reset(p.hideAmounts, d.hideAmounts) { it.copy(hideAmounts = d.hideAmounts) },
        )
    }
}

@Composable
private fun PreviewCard(x: PrefsCtx) {
    val c = Penny.colors
    val shape = RoundedCornerShape(x.p.corners.dp.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(shape).background(c.surface1).border(0.75.dp, c.hairline, shape).padding(Penny.dims.cardPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Preview", style = Penny.type.caption, color = c.textSecondary)
        Text(Formatters.money(123456, Formatters.of(x.p)), style = Penny.type.amountLarge, color = c.accent)
        Text("Available to spend", style = Penny.type.label, color = c.textSecondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccentPicker(x: PrefsCtx) {
    val c = Penny.colors
    var editing by rememberSaveable { mutableStateOf(false) }
    val isPreset = AccentPresets.any { it.second == x.p.accentArgb }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val reset = x.reset(x.p.accentArgb, x.d.accentArgb) { it.copy(accentArgb = x.d.accentArgb) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Accent colour", style = Penny.type.bodyStrong, color = c.textPrimary)
            if (reset != null) {
                Text("Reset", style = Penny.type.caption, color = c.accent, modifier = Modifier.padding(start = 8.dp).clip(Penny.dims.smallShape).clickable(onClick = reset).padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentPresets.forEach { (name, argb) ->
                Swatch(Color(argb), name, x.p.accentArgb == argb) { x.set { it.copy(accentArgb = argb) } }
            }
            Swatch(if (isPreset) c.surface2 else Color(x.p.accentArgb), "Custom", !isPreset, custom = true) { editing = true }
        }
    }
    if (editing) HexSheet(x.p.accentArgb, onDismiss = { editing = false }) { argb -> x.set { it.copy(accentArgb = argb) }; editing = false }
}

@Composable
private fun Swatch(color: Color, desc: String, selected: Boolean, custom: Boolean = false, onClick: () -> Unit) {
    val c = Penny.colors
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(color)
            .border(if (selected) 2.dp else 0.75.dp, if (selected) c.textPrimary else c.hairline, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            custom -> Icon(Icons.Rounded.Edit, desc, tint = if (selected) Color.Black else c.textSecondary, modifier = Modifier.size(18.dp))
            selected -> Icon(Icons.Rounded.Check, desc, tint = Color.Black, modifier = Modifier.size(18.dp))
        }
    }
}

/** Parses "#RRGGBB", "RRGGBB" or "rgb(r, g, b)" / "r,g,b" into an opaque ARGB Long, or null. */
internal fun parseHex(text: String): Long? {
    val t = text.trim().lowercase()
    if (',' in t) {
        val parts = t.removePrefix("rgb").trim('(', ')', ' ').split(',').map { it.trim().toIntOrNull() }
        if (parts.size != 3 || parts.any { it == null || it !in 0..255 }) return null
        val (r, g, b) = parts.map { it!!.toLong() }
        return 0xFF000000L or (r shl 16) or (g shl 8) or b
    }
    val s = t.removePrefix("#")
    if (s.length != 6 || s.any { it.lowercaseChar() !in "0123456789abcdef" }) return null
    return 0xFF000000L or s.toLong(16)
}

@Composable
private fun HexSheet(current: Long, onDismiss: () -> Unit, onApply: (Long) -> Unit) {
    val c = Penny.colors
    var text by rememberSaveable { mutableStateOf("#%06X".format(current and 0xFFFFFF)) }
    val parsed = parseHex(text)
    PennySheet(onDismiss = onDismiss, title = "Custom accent") {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).size(48.dp).clip(Penny.dims.smallShape).background(parsed?.let { Color(it) } ?: c.surface1).border(0.75.dp, c.hairline, Penny.dims.smallShape))
            OutlinedTextField(
                text, { text = it.take(20) }, Modifier.fillMaxWidth(), singleLine = true, colors = pennyFieldColors(),
                label = { Text("#RRGGBB or rgb(r, g, b)") }, isError = parsed == null,
                supportingText = { if (parsed == null) Text("Use 6 hex digits, e.g. #39FF88", color = c.danger) },
            )
            PennyButton("Use colour", { parsed?.let(onApply) }, Modifier.fillMaxWidth(), enabled = parsed != null)
        }
    }
}
