package com.penny.wallet.ui.nav

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.penny.wallet.ui.components.LocalSnack
import com.penny.wallet.ui.pennyViewModel
import com.penny.wallet.ui.quickadd.QuickAddSheet
import com.penny.wallet.ui.quickadd.QuickAddViewModel

/** Hosts the Quick add sheet above every screen (FAB, edit taps, widget deep link). */
@Composable
fun QuickAddHost(root: RootState) {
    val vm = pennyViewModel { QuickAddViewModel(it) }
    val snack = LocalSnack.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(root.quickAddOpen, root.editTxId) {
        if (root.quickAddOpen) root.editTxId?.let { vm.startEdit(it) } ?: vm.startNew()
    }
    if (!root.quickAddOpen) return

    QuickAddSheet(
        vm = vm,
        onDismiss = { root.close() },
        onResult = { r ->
            root.close()
            val undo = r.undo
            if (undo != null) snack.undo(r.message) { vm.undoNew(undo) } else snack.show(r.message)
            if (r.askNotificationPermission && Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onDeleted = { t ->
            root.close()
            snack.undo("Deleted") { vm.restore(t) }
        },
    )
}
