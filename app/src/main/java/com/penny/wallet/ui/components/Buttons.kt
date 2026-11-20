package com.penny.wallet.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.penny.wallet.ui.theme.Penny
import com.penny.wallet.ui.theme.glow

enum class ButtonKind { PRIMARY, SECONDARY, DANGER }

/** Pill button: PRIMARY = accent fill + glow, SECONDARY = surface + hairline, DANGER = red text. */
@Composable
fun PennyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: ButtonKind = ButtonKind.PRIMARY, enabled: Boolean = true) {
    val c = Penny.colors
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, Penny.motion.spec(), label = "btn")
    val haptic = rememberHaptic()
    val shape = RoundedCornerShape(50)
    val (bg, fg) = when (kind) {
        ButtonKind.PRIMARY -> c.accent to c.onAccent
        ButtonKind.SECONDARY -> c.surface1 to c.textPrimary
        ButtonKind.DANGER -> c.surface1 to c.danger
    }
    Box(
        modifier.height(52.dp).scale(scale).alpha(if (enabled) 1f else 0.4f)
            .then(if (kind == ButtonKind.PRIMARY && enabled) Modifier.glow(c.accent, Penny.motion.glowAlpha * 0.6f, 16.dp) else Modifier)
            .clip(shape).background(bg)
            .then(if (kind != ButtonKind.PRIMARY) Modifier.border(0.75.dp, c.hairline, shape) else Modifier)
            .clickable(src, null, enabled = enabled) { haptic(HapticFeedbackType.LongPress); onClick() }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Penny.type.bodyStrong, color = fg) }
}

/** Round tinted icon badge used in lists. */
@Composable
fun IconBadge(icon: String, color: Color, modifier: Modifier = Modifier, size: Int = 40) {
    Box(modifier.size(size.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Icon(PennyIcons.get(icon), null, tint = color, modifier = Modifier.size((size * 0.55f).dp))
    }
}
