package com.penny.wallet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.penny.wallet.ui.theme.Penny

/**
 * Cash-register style entry: digits shift in from the right ("1","2","5" → 1,25 €). Holds no state;
 * the caller keeps a digit string and gets new strings back. Max 9 digits (9.999.999,99 €).
 */
object NumpadLogic {
    const val MAX_DIGITS = 9
    fun press(digits: String, key: Char): String = when (key) {
        '<' -> digits.dropLast(1)
        'C' -> ""
        else -> if (digits.length >= MAX_DIGITS || (digits.isEmpty() && key == '0')) digits
                else if (key == 'D') (if (digits.isEmpty()) "" else (digits + "00").take(MAX_DIGITS)) else digits + key
    }
    fun fromCents(c: Long): String = if (c <= 0) "" else c.toString()
}

@Composable
fun Numpad(digits: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, keyHeight: Int = 60) {
    val haptic = rememberHaptic()
    val rows = listOf("123", "456", "789", "D0<")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    NumKey(k, Modifier.weight(1f).height(keyHeight.dp), onLongPress = if (k == '<') ({ onChange("") }) else null) {
                        haptic(HapticFeedbackType.TextHandleMove)
                        onChange(NumpadLogic.press(digits, k))
                    }
                }
            }
        }
    }
}

@Composable
private fun NumKey(k: Char, modifier: Modifier, onLongPress: (() -> Unit)?, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, Penny.motion.spec(), label = "key")
    val c = Penny.colors
    Box(
        modifier.scale(scale).clip(Penny.dims.smallShape)
            .background(if (pressed) c.surface2.copy(alpha = 1f) else c.surface1)
            .clickable(src, null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (k) {
            '<' -> Icon(Icons.AutoMirrored.Rounded.Backspace, "Delete", tint = c.textSecondary, modifier = Modifier.size(24.dp))
            'D' -> Text("00", style = Penny.type.title, color = c.textPrimary)
            else -> Text(k.toString(), style = Penny.type.title, color = c.textPrimary)
        }
    }
}
