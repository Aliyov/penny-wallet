package com.penny.wallet.feature.settings

import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.penny.wallet.R
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.components.PennyIcons
import com.penny.wallet.core.ui.theme.ItemPalette
import com.penny.wallet.core.ui.theme.Penny

/** Back arrow + title, optional "+" action. */
@Composable
internal fun SettingsTopBar(title: String, onBack: () -> Unit, onAdd: (() -> Unit)? = null) {
    val c = Penny.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = c.textPrimary) }
        Text(title, style = Penny.type.title, color = c.textPrimary, modifier = Modifier.weight(1f))
        if (onAdd != null) IconButton(onClick = onAdd) { Icon(Icons.Rounded.Add, stringResource(R.string.action_add), tint = c.accent) }
    }
}

/** One row inside a grouped settings card: icon tile, title/subtitle, chevron. */
@Composable
internal fun SettingsRow(
    icon: ImageVector, title: String, onClick: () -> Unit,
    subtitle: String? = null, tint: Color = Penny.colors.accent, danger: Boolean = false, chevron: Boolean = true,
) {
    val c = Penny.colors
    val color = if (danger) c.danger else tint
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Penny.dims.cardPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(Penny.dims.smallShape).background(color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Penny.type.bodyStrong, color = if (danger) c.danger else c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = Penny.type.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (chevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.textSecondary)
    }
}

@Composable
internal fun RowDivider() = HorizontalDivider(Modifier.padding(start = 64.dp), thickness = 0.5.dp, color = Penny.colors.hairline)

/** Grid of round icon tiles. */
@Composable
internal fun IconPicker(keys: List<String>, selected: String, tint: Color, onPick: (String) -> Unit) {
    val c = Penny.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.forEach { k ->
            val sel = k == selected
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(if (sel) tint.copy(alpha = 0.18f) else c.surface1)
                    .border(if (sel) 1.5.dp else 0.75.dp, if (sel) tint else c.hairline, CircleShape)
                    .selectable(sel, role = Role.RadioButton) { onPick(k) },
                contentAlignment = Alignment.Center,
            ) { Icon(PennyIcons.get(k), k, tint = if (sel) tint else c.textPrimary, modifier = Modifier.size(22.dp)) }
        }
    }
}

@Composable
internal fun ColorSwatches(selected: Long, onPick: (Long) -> Unit) {
    val c = Penny.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ItemPalette.forEach { argb ->
            val sel = argb == selected
            val colourDesc = stringResource(R.string.a11y_colour, ItemPalette.indexOf(argb) + 1)
            Box(
                Modifier.minimumInteractiveComponentSize().size(36.dp).clip(CircleShape).background(Color(argb))
                    .border(if (sel) 2.dp else 0.dp, if (sel) c.textPrimary else Color.Transparent, CircleShape)
                    .selectable(sel, role = Role.RadioButton) { onPick(argb) }.semantics { contentDescription = colourDesc },
                contentAlignment = Alignment.Center,
            ) { if (sel) Icon(Icons.Rounded.Check, null, tint = c.background, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
internal fun FieldLabel(text: String) =
    Text(text, style = Penny.type.label, color = Penny.colors.textSecondary, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

@Composable
internal fun BottomGap() = Spacer(Modifier.height(120.dp))
