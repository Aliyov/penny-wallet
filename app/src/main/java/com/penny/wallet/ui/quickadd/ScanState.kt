package com.penny.wallet.ui.quickadd

import com.penny.wallet.domain.receipt.ReceiptResult

/** Receipt scan flow shown over the quick add sheet. */
sealed interface ScanState {
    data object Idle : ScanState
    data object Reading : ScanState
    data class Found(val result: ReceiptResult) : ScanState
    data object NothingFound : ScanState
    data class Failed(val message: String) : ScanState
}
