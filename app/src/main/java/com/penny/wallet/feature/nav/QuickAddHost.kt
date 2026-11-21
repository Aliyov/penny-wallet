package com.penny.wallet.feature.nav

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.LocalSnack
import com.penny.wallet.core.ui.pennyViewModel
import com.penny.wallet.feature.add.QuickAddSheet
import com.penny.wallet.feature.add.QuickAddViewModel
import com.penny.wallet.feature.add.rememberReceiptLaunchers

/** Hosts the Quick add sheet above every screen (FAB, edit taps, widget deep link). */
@Composable
fun QuickAddHost(root: RootState) {
    val vm = pennyViewModel { QuickAddViewModel(it) }
    val snack = LocalSnack.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    // Registered here (always composed) so a camera result is delivered even if the activity was recreated.
    val receipt = rememberReceiptLaunchers(vm)

    LaunchedEffect(root.quickAddOpen, root.editTxId) {
        if (root.quickAddOpen) root.editTxId?.let { vm.startEdit(it) } ?: vm.startNew()
    }
    if (!root.quickAddOpen) return
    val deletedText = stringResource(R.string.deleted)

    QuickAddSheet(
        vm = vm,
        receipt = receipt,
        onDismiss = { vm.closeScan(); root.close() },
        onResult = { r ->
            vm.closeScan()
            root.close()
            val undo = r.undo
            if (undo != null) snack.undo(r.message) { vm.undoNew(undo) } else snack.show(r.message)
            if (r.askNotificationPermission && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onDeleted = { t ->
            root.close()
            snack.undo(deletedText) { vm.restore(t) }
        },
    )
}
