package com.penny.wallet.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.penny.wallet.core.ui.theme.Penny

/** iOS-style sheet for every form: Surface2, drag handle, drag to dismiss. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PennySheet(
    onDismiss: () -> Unit,
    title: String? = null,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Penny.colors
    val r = Penny.dims.corner
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
        containerColor = c.surface2,
        contentColor = c.textPrimary,
        scrimColor = c.background.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = r + 4.dp, topEnd = r + 4.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).width(36.dp).size(36.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(c.textSecondary.copy(alpha = 0.4f)))
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Penny.dims.gutter).navigationBarsPadding().padding(bottom = 12.dp)) {
            if (title != null) Text(title, style = Penny.type.title, color = c.textPrimary, modifier = Modifier.padding(bottom = 16.dp).align(Alignment.Start))
            content()
        }
    }
}
