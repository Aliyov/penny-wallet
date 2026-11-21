package com.penny.wallet.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.theme.Penny

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text.uppercase(), style = Penny.type.caption, color = Penny.colors.textSecondary)
        if (action != null && onAction != null) {
            Text(action, style = Penny.type.label, color = Penny.colors.accent, modifier = Modifier.padding(4.dp).then(Modifier.clickableNoRipple(onAction)))
        }
    }
}
