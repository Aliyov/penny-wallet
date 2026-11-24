package com.penny.wallet.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.PennySheet
import com.penny.wallet.core.ui.components.SwitchRow
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.data.backup.BackupCrypto
import com.penny.wallet.feature.home.ConfirmDialog
import com.penny.wallet.feature.home.pennyFieldColors

@Composable
internal fun PasswordCard(on: Boolean, onSet: (CharArray) -> Unit, onRemove: () -> Unit) {
    var setting by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    PennyCard(Modifier.fillMaxWidth()) {
        SwitchRow(
            stringResource(R.string.backup_password), on, { v -> if (v) setting = true else removing = true },
            subtitle = stringResource(if (on) R.string.backup_password_on else R.string.backup_password_off),
        )
    }
    if (setting) SetPasswordSheet(onDismiss = { setting = false }) { pw -> setting = false; onSet(pw) }
    if (removing) {
        ConfirmDialog(
            stringResource(R.string.backup_password_remove_title), stringResource(R.string.backup_password_remove_body),
            stringResource(R.string.backup_password_remove), danger = true, onDismiss = { removing = false },
        ) { removing = false; onRemove() }
    }
}

@Composable
private fun SetPasswordSheet(onDismiss: () -> Unit, onSave: (CharArray) -> Unit) {
    val c = Penny.colors
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val longEnough = first.length >= BackupCrypto.MIN_PASSWORD
    val same = first == second
    PennySheet(onDismiss = onDismiss, title = stringResource(R.string.backup_password_set_title)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.backup_password_warning), style = Penny.type.body, color = c.warning)
            PasswordField(first, { first = it }, stringResource(R.string.backup_password_new),
                error = if (first.isNotEmpty() && !longEnough) stringResource(R.string.backup_password_too_short, BackupCrypto.MIN_PASSWORD) else null)
            PasswordField(second, { second = it }, stringResource(R.string.backup_password_repeat),
                error = if (second.isNotEmpty() && !same) stringResource(R.string.backup_password_mismatch) else null)
            PennyButton(stringResource(R.string.backup_password_turn_on), { onSave(first.toCharArray()) }, Modifier.fillMaxWidth(), enabled = longEnough && same)
        }
    }
}

@Composable
internal fun RestorePasswordDialog(error: String?, busy: Boolean, onDismiss: () -> Unit, onSubmit: (CharArray) -> Unit) {
    val c = Penny.colors
    var pw by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface2, titleContentColor = c.textPrimary, textContentColor = c.textSecondary,
        shape = Penny.dims.cardShape,
        title = { Text(stringResource(R.string.backup_password_enter_title), style = Penny.type.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.backup_password_enter_body), style = Penny.type.body)
                PasswordField(pw, { pw = it }, stringResource(R.string.backup_password), error = error)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(pw.toCharArray()) }, enabled = pw.isNotEmpty() && !busy) {
                Text(stringResource(R.string.backup_password_open), color = c.accent, style = Penny.type.bodyStrong)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = c.textSecondary) } },
    )
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String, error: String? = null) {
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(), singleLine = true,
        label = { Text(label) }, isError = error != null,
        supportingText = error?.let { { Text(it, color = Penny.colors.danger) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        textStyle = Penny.type.body, shape = Penny.dims.smallShape, colors = pennyFieldColors(),
    )
}
