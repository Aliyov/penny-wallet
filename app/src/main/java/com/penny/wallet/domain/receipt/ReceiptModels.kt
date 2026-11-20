package com.penny.wallet.domain.receipt

/**
 * One recognised text line in image pixel coordinates (y grows downwards).
 * [slope] is the line's tilt as dy/dx, measured from its top edge (0 for a straight photo).
 */
data class OcrLine(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val slope: Float = 0f,
)

/**
 * Outcome of reading a receipt.
 * [totalCents] is the best guess (null when no amount was found at all),
 * [candidates] the distinct amounts worth offering, best first (contains [totalCents] when non-null),
 * [confident] is true when the total came from a "total" style label, false for the largest-amount fallback.
 */
data class ReceiptResult(
    val totalCents: Long?,
    val candidates: List<Long>,
    val confident: Boolean,
) {
    companion object {
        val EMPTY = ReceiptResult(null, emptyList(), false)
    }
}
