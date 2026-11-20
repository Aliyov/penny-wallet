package com.penny.wallet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
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
import com.penny.wallet.ui.theme.glow

/** Neon circle, black +, soft glow, spring scale 0.92 on press (SPEC §7). */
@Composable
fun PennyFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, Penny.motion.spec(), label = "fab")
    val haptic = rememberHaptic()
    val c = Penny.colors
    Box(
        modifier.size(64.dp).scale(scale).glow(c.accent, Penny.motion.glowAlpha, 22.dp)
            .clip(CircleShape).background(c.accent)
            .clickable(src, null) { haptic(HapticFeedbackType.LongPress); onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Add, "Add", tint = c.onAccent, modifier = Modifier.size(32.dp)) }
}
