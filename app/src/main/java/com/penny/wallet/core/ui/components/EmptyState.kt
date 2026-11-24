package com.penny.wallet.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.theme.Penny

/** Every empty list shows one sentence and, when there is something to do, one button (ARCHITECTURE §9). */
@Composable
fun EmptyState(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    action: String? = null,
    kind: ButtonKind = ButtonKind.SECONDARY,
    onAction: (() -> Unit)? = null,
) {
    val c = Penny.colors
    Column(modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (title != null) Text(title, style = Penny.type.title, color = c.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
        Text(text, style = Penny.type.body, color = c.textSecondary, textAlign = TextAlign.Center)
        if (action != null && onAction != null) PennyButton(action, onAction, Modifier.padding(top = 16.dp), kind = kind)
    }
}
