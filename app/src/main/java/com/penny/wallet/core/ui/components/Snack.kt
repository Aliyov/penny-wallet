package com.penny.wallet.core.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** App-wide snackbars. Every delete shows Undo for 5 s. */
class Snack(private val host: SnackbarHostState, private val scope: CoroutineScope, private val undoLabel: String = "Undo") {
    val state get() = host

    fun show(message: String) { scope.launch { host.currentSnackbarData?.dismiss(); host.showSnackbar(message) } }

    fun undo(message: String, onUndo: suspend () -> Unit) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val job = launch { delay(5_000); host.currentSnackbarData?.dismiss() }
            val r = host.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Indefinite)
            job.cancel()
            if (r == SnackbarResult.ActionPerformed) onUndo()
        }
    }
}

val LocalSnack = staticCompositionLocalOf<Snack> { error("No Snack provided") }
