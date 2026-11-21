package com.penny.wallet.core.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.theme.Penny

data class NavItem(val route: String, val label: String, val icon: ImageVector)

/**
 * Floating translucent pill. Content behind isn't blurred via RenderEffect (that would blur the bar itself);
 * instead the pill uses a high-alpha surface on API 31+ with a hairline, and a solid surface below.
 */
@Composable
fun PennyBottomBar(items: List<NavItem>, current: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Penny.colors
    val haptic = rememberHaptic()
    val bg = if (Build.VERSION.SDK_INT >= 31) c.surface2.copy(alpha = 0.82f) else c.surface2
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth()
            .clip(shape).background(bg).border(0.75.dp, c.hairline, shape).height(64.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            if (i == items.size / 2) Spacer(Modifier.width(64.dp)) // room for the FAB
            val selected = current == item.route
            val tint by animateColorAsState(if (selected) c.textPrimary else c.textSecondary, label = "tint")
            val dot by animateFloatAsState(if (selected) 1f else 0f, Penny.motion.spec(), label = "dot")
            Column(
                Modifier.padding(horizontal = 6.dp).clickableNoRipple { haptic(HapticFeedbackType.TextHandleMove); onSelect(item.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(item.icon, item.label, tint = tint, modifier = Modifier.size(24.dp))
                Text(item.label, style = Penny.type.caption, color = tint)
                Box(Modifier.padding(top = 3.dp).size(4.dp).alpha(dot).clip(CircleShape).background(c.accent))
            }
        }
    }
}
