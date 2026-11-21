package com.penny.wallet.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.theme.Penny

/** iOS-style segmented control with a sliding accent-tinted pill indicator. */
@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val haptic = rememberHaptic()
    val shape = RoundedCornerShape(50)
    BoxWithConstraints(
        modifier.fillMaxWidth().height(40.dp).clip(shape).background(c.surface1).border(0.75.dp, c.hairline, shape).padding(3.dp),
    ) {
        val count = options.size.coerceAtLeast(1)
        val segW = maxWidth / count
        val x by animateDpAsState(segW * selectedIndex.coerceIn(0, count - 1), Penny.motion.spec(), label = "seg")
        Box(
            Modifier.offset(x = x).width(segW).fillMaxHeight().clip(shape)
                .background(c.accent.copy(alpha = 0.18f)).border(0.75.dp, c.accent.copy(alpha = 0.6f), shape),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                val color by animateColorAsState(if (selected) c.accent else c.textSecondary, Penny.motion.spec(), label = "segText")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(shape).clickableNoRipple {
                        if (!selected) { haptic(HapticFeedbackType.TextHandleMove); onSelect(i) }
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = if (selected) Penny.type.bodyStrong else Penny.type.label, color = color, maxLines = 1, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
