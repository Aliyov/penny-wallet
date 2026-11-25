package com.penny.wallet.core.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.theme.Penny

/** Start-side navigation for wide screens; same items and selection logic as [PennyBottomBar]. */
@Composable
fun PennyNavRail(
    items: List<NavItem>, current: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = Penny.colors
    val haptic = rememberHaptic()
    val colors = NavigationRailItemDefaults.colors(
        selectedIconColor = c.textPrimary, selectedTextColor = c.textPrimary,
        unselectedIconColor = c.textSecondary, unselectedTextColor = c.textSecondary,
        indicatorColor = c.accent.copy(alpha = 0.18f),
    )
    NavigationRail(modifier, containerColor = c.surface1, contentColor = c.textPrimary, header = header?.let { h ->
        { h(); Spacer(Modifier.height(12.dp)) }
    }) {
        Spacer(Modifier.padding(top = 4.dp))
        items.forEach { item ->
            NavigationRailItem(
                selected = current == item.route,
                onClick = { haptic(HapticFeedbackType.TextHandleMove); onSelect(item.route) },
                icon = { Icon(item.icon, null, modifier = Modifier.size(24.dp)) },
                label = { Text(item.label, style = Penny.type.caption) },
                colors = colors,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }
}
