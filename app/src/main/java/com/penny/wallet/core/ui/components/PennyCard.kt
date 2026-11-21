package com.penny.wallet.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.theme.Penny

/** Card: Surface1, hairline border, faint top-left gradient, corner from prefs. */
@Composable
fun PennyCard(
    modifier: Modifier = Modifier,
    tint: Color = Penny.colors.accent,
    padding: Dp = Penny.dims.cardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = Penny.dims.cardShape
    val c = Penny.colors
    Column(
        modifier
            .clip(shape)
            .background(c.surface1)
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.07f), Color.Transparent), Offset.Zero, Offset(600f, 600f)))
            .border(0.75.dp, c.hairline, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}
