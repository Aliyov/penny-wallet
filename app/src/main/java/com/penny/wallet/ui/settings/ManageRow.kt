package com.penny.wallet.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penny.wallet.ui.components.IconBadge
import com.penny.wallet.ui.components.PennyCard

import com.penny.wallet.ui.theme.Penny

/** List row for categories / accounts: badge, title, subtitle, trailing slot, optional drag handle. */
@Composable
internal fun ManageRow(
    icon: String, color: Color, title: String, subtitle: String, onClick: () -> Unit,
    dragging: Boolean = false, dimmed: Boolean = false,
    handle: Modifier? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Penny.colors
    val elevation by animateDpAsState(if (dragging) 12.dp else 0.dp, Penny.motion.spec(), label = "drag")
    PennyCard(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).shadow(elevation, Penny.dims.cardShape).alpha(if (dimmed) 0.5f else 1f),
        tint = color, padding = 12.dp, onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = Penny.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = Penny.type.caption, color = c.textSecondary, maxLines = 1)
            }
            trailing?.invoke()
            if (handle != null) {
                Box(handle.padding(start = 4.dp).size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.DragHandle, "Drag to reorder", tint = if (dragging) color else c.textSecondary)
                }
            }
        }
    }
}

@Composable
internal fun UnarchiveButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text("Unarchive", style = Penny.type.label, color = Penny.colors.accent) }
}
