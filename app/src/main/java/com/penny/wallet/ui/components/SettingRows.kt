package com.penny.wallet.ui.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.penny.wallet.ui.theme.Penny

/** Title (+ optional subtitle) with a small "Reset" action shown when [onReset] != null. */
@Composable
private fun RowTitle(title: String, subtitle: String?, onReset: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = Penny.colors
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = Penny.type.bodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f, fill = false))
            if (onReset != null) {
                Text(
                    "Reset", style = Penny.type.caption, color = c.accent,
                    modifier = Modifier.padding(start = 8.dp).clip(Penny.dims.smallShape).clickable(onClick = onReset).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        if (subtitle != null) Text(subtitle, style = Penny.type.caption, color = c.textSecondary)
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    onReset: (() -> Unit)? = null,
) {
    val c = Penny.colors
    val haptic = rememberHaptic()
    val toggle: (Boolean) -> Unit = { haptic(HapticFeedbackType.TextHandleMove); onChange(it) }
    Row(
        modifier.fillMaxWidth().clip(Penny.dims.smallShape).clickable(enabled = enabled) { toggle(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowTitle(title, subtitle, onReset, Modifier.weight(1f).padding(end = 12.dp))
        Switch(
            checked, toggle, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent, checkedTrackColor = c.accent,
                uncheckedThumbColor = c.textSecondary, uncheckedTrackColor = c.surface1, uncheckedBorderColor = c.hairline,
            ),
        )
    }
}

/** Segmented control for ≤ 4 options, wrapping chips otherwise. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onReset: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RowTitle(title, subtitle, onReset)
        if (options.size <= 4) {
            SegmentedControl(options.map { it.second }, options.indexOfFirst { it.first == selected }.coerceAtLeast(0), { onSelect(options[it].first) })
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (v, label) -> PennyChip(label, v == selected, { onSelect(v) }) }
            }
        }
    }
}

@Composable
fun StepperRow(
    title: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    format: (Int) -> String = { it.toString() },
    subtitle: String? = null,
    onReset: (() -> Unit)? = null,
) {
    val c = Penny.colors
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RowTitle(title, subtitle, onReset, Modifier.weight(1f).padding(end = 12.dp))
        StepButton(Icons.Rounded.Remove, "Decrease", value > range.first) { onChange((value - 1).coerceIn(range)) }
        Text(
            format(value), style = Penny.type.bodyStrong, color = c.textPrimary, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 56.dp).padding(horizontal = 6.dp),
        )
        StepButton(Icons.Rounded.Add, "Increase", value < range.last) { onChange((value + 1).coerceIn(range)) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Penny.colors
    val haptic = rememberHaptic()
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(c.surface2).border(0.75.dp, c.hairline, CircleShape)
            .clickable(enabled = enabled) { haptic(HapticFeedbackType.TextHandleMove); onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = if (enabled) c.accent else c.textSecondary.copy(alpha = 0.4f), modifier = Modifier.size(18.dp)) }
}

@Composable
fun NavRow(
    title: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onReset: (() -> Unit)? = null,
) {
    val c = Penny.colors
    Row(
        modifier.fillMaxWidth().clip(Penny.dims.smallShape).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowTitle(title, subtitle, onReset, Modifier.weight(1f).padding(end = 12.dp))
        if (value != null) Text(value, style = Penny.type.body, color = c.textSecondary)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textSecondary)
    }
}
