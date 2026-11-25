package com.penny.wallet.feature.add

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import com.penny.wallet.core.util.dateText
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.util.currentLocale
import com.penny.wallet.domain.receipt.ReceiptResult
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.Penny

/** Starts a receipt scan from the camera or the gallery; results go to [QuickAddViewModel.readReceipt]. */
class ReceiptLaunchers(val camera: () -> Unit, val gallery: () -> Unit)

@Composable
fun rememberReceiptLaunchers(vm: QuickAddViewModel): ReceiptLaunchers {
    var pending by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pending
        pending = null
        if (ok && uri != null) vm.readReceipt(uri)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.readReceipt(uri)
    }
    val noCamera = stringResource(R.string.add_scan_no_camera)
    val noGallery = stringResource(R.string.add_scan_no_gallery)
    return remember(vm, camera, gallery, noCamera, noGallery) {
        ReceiptLaunchers(
            camera = {
                try {
                    val uri = vm.newReceiptPhotoUri()
                    pending = uri
                    camera.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    vm.scanFailed(noCamera)
                }
            },
            gallery = {
                try {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } catch (_: ActivityNotFoundException) {
                    vm.scanFailed(noGallery)
                }
            },
        )
    }
}

/** "Scan receipt" chip with a camera / gallery menu. */
@Composable
internal fun ReceiptChip(launchers: ReceiptLaunchers) {
    val c = Penny.colors
    var open by remember { mutableStateOf(false) }
    Box {
        ScanButton { open = true }
        DropdownMenu(open, { open = false }, containerColor = c.surface2) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_take_photo), style = Penny.type.body, color = c.textPrimary) },
                leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null, tint = c.textSecondary) },
                onClick = { open = false; launchers.camera() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_choose_gallery), style = Penny.type.body, color = c.textPrimary) },
                leadingIcon = { Icon(Icons.Rounded.PhotoLibrary, null, tint = c.textSecondary) },
                onClick = { open = false; launchers.gallery() },
            )
        }
    }
}

/** Tinted with the accent so it reads as an action, not as one more option among the neutral chips. */
@Composable
private fun ScanButton(onClick: () -> Unit) {
    val c = Penny.colors
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.height(40.dp).clip(shape).background(c.accent.copy(alpha = 0.16f)).border(1.dp, c.accent, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.PhotoCamera, null, tint = c.accent, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.add_scan_receipt), style = Penny.type.label, fontWeight = FontWeight.SemiBold, color = c.accent, maxLines = 1)
    }
}

/** Reading spinner, then "Add €5.50 to expense?" with an editable amount; Decline · Retake · Accept. */
@Composable
fun ReceiptScanDialog(vm: QuickAddViewModel, launchers: ReceiptLaunchers) {
    val state by vm.scan.collectAsStateWithLifecycle()
    when (val s = state) {
        ScanState.Idle -> Unit
        ScanState.Reading -> ScanAlert(stringResource(R.string.add_scan_reading), onDismiss = vm::closeScan, body = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(color = Penny.colors.accent, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                Text(stringResource(R.string.add_scan_looking), style = Penny.type.body)
            }
        }) { DialogButton(stringResource(R.string.action_cancel), vm::closeScan) }
        ScanState.NothingFound -> ScanAlert(stringResource(R.string.add_scan_nothing_title), onDismiss = vm::closeScan, body = {
            Text(stringResource(R.string.add_scan_nothing_body), style = Penny.type.body)
        }) { RetryButtons(vm, launchers) }
        is ScanState.Failed -> ScanAlert(stringResource(R.string.add_scan_failed_title), onDismiss = vm::closeScan, body = {
            Text(s.message, style = Penny.type.body)
        }) { RetryButtons(vm, launchers) }
        is ScanState.Found -> ConfirmAmount(s.result, vm, launchers)
    }
}

@Composable
private fun ConfirmAmount(result: ReceiptResult, vm: QuickAddViewModel, launchers: ReceiptLaunchers) {
    val c = Penny.colors
    val prefs = Penny.prefs
    val locale = currentLocale()
    val digits = Formatters.fractionDigits(prefs.currencyCode)
    fun editable(cents: Long) = Formatters.money(cents, Formatters.of(prefs, locale), forceCents = true)
        .filter { it.isDigit() || it == ',' || it == '.' }
    var text by remember(result) { mutableStateOf(editable(result.totalCents ?: 0L)) }
    val cents = Formatters.parseCents(text, digits)?.takeIf { it in 1..MAX_CENTS }
    ScanAlert(
        title = if (cents != null) stringResource(R.string.add_scan_confirm_amount, money(cents, ignorePrivacy = true))
        else stringResource(R.string.add_scan_confirm),
        onDismiss = vm::closeScan,
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val details = listOfNotNull(result.merchant, result.date?.let { dateText().day(it) }).joinToString(" · ")
                if (details.isNotEmpty()) Text(details, style = Penny.type.bodyStrong, color = c.textPrimary)
                if (!result.confident) {
                    Text(stringResource(R.string.add_scan_not_confident), style = Penny.type.caption, color = c.textSecondary)
                }
                OutlinedTextField(
                    text, { text = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }.take(12) },
                    singleLine = true, label = { Text(stringResource(R.string.label_amount)) }, isError = cents == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = Penny.type.title.copy(color = c.textPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = c.accent, unfocusedBorderColor = c.hairline, cursorColor = c.accent,
                        focusedLabelColor = c.accent, unfocusedLabelColor = c.textSecondary,
                        errorBorderColor = c.danger, errorLabelColor = c.danger,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                val others = result.candidates
                if (others.size > 1) {
                    Text(stringResource(R.string.add_scan_other_amounts), style = Penny.type.caption, color = c.textSecondary)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        others.forEach { a ->
                            TintChip(money(a, ignorePrivacy = true), a == cents, c.accent, { text = editable(a) })
                        }
                    }
                }
            }
        },
    ) {
        DialogButton(stringResource(R.string.add_scan_decline), vm::closeScan, muted = true)
        DialogButton(stringResource(R.string.add_scan_retake), launchers.camera, muted = true)
        DialogButton(stringResource(R.string.add_scan_accept), { cents?.let { vm.acceptReceipt(it, result) } }, enabled = cents != null)
    }
}

@Composable
private fun RetryButtons(vm: QuickAddViewModel, launchers: ReceiptLaunchers) {
    DialogButton(stringResource(R.string.action_close), vm::closeScan, muted = true)
    DialogButton(stringResource(R.string.add_scan_gallery), launchers.gallery, muted = true)
    DialogButton(stringResource(R.string.add_scan_retake), launchers.camera)
}

@Composable
private fun ScanAlert(title: String, onDismiss: () -> Unit, body: @Composable () -> Unit, buttons: @Composable () -> Unit) {
    val c = Penny.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface2, titleContentColor = c.textPrimary, textContentColor = c.textSecondary,
        shape = Penny.dims.cardShape,
        title = { Text(title, style = Penny.type.title) },
        text = body,
        confirmButton = { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { buttons() } },
    )
}

@Composable
private fun DialogButton(label: String, onClick: () -> Unit, muted: Boolean = false, enabled: Boolean = true) {
    val c = Penny.colors
    TextButton(onClick = onClick, enabled = enabled) {
        Text(label, style = Penny.type.bodyStrong, color = when {
            !enabled -> c.textSecondary.copy(alpha = 0.5f)
            muted -> c.textSecondary
            else -> c.accent
        })
    }
}

private const val MAX_CENTS = 10_000_000L
