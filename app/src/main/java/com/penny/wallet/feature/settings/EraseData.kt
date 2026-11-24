package com.penny.wallet.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.feature.home.ConfirmDialog
import com.penny.wallet.feature.home.pennyFieldColors

/** Erase-everything row: confirm once, then type the confirmation word. */
@Composable
internal fun EraseAllData() {
    val vm = pennyViewModel { ManageViewModel(it) }
    val snack = LocalSnack.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    val clearedMsg = stringResource(R.string.settings_reset_done)

    PennyCard(Modifier.fillMaxWidth(), padding = 0.dp) {
        Column(Modifier.padding(vertical = 4.dp)) {
            SettingsRow(
                Icons.Rounded.DeleteForever, stringResource(R.string.settings_reset), { step = 1 },
                stringResource(R.string.settings_reset_sub), danger = true, chevron = false,
            )
        }
    }

    when (step) {
        1 -> ConfirmDialog(
            title = stringResource(R.string.settings_reset_title), text = stringResource(R.string.settings_reset_body),
            confirm = stringResource(R.string.settings_reset_continue), danger = true, onDismiss = { step = 0 },
        ) { step = 2 }
        2 -> TypeResetDialog(onDismiss = { step = 0 }) {
            step = 0
            vm.resetAll { snack.show(clearedMsg) }
        }
    }
}

@Composable
private fun TypeResetDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val c = Penny.colors
    var typed by remember { mutableStateOf("") }
    val word = stringResource(R.string.settings_reset_word)
    val ok = typed.trim().equals(word, ignoreCase = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface2, titleContentColor = c.textPrimary, textContentColor = c.textSecondary,
        shape = Penny.dims.cardShape,
        title = { Text(stringResource(R.string.settings_reset_sure_title), style = Penny.type.title) },
        text = {
            Column {
                Text(stringResource(R.string.settings_reset_sure_body, word), style = Penny.type.body)
                OutlinedTextField(
                    typed, { typed = it }, Modifier.fillMaxWidth().padding(top = 12.dp), singleLine = true,
                    placeholder = { Text(word, color = c.textSecondary) },
                    textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = ok) {
                Text(stringResource(R.string.settings_reset_confirm), color = if (ok) c.danger else c.textSecondary, style = Penny.type.bodyStrong)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = c.textSecondary) } },
    )
}
