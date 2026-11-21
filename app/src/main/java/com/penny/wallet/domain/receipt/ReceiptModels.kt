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
    /** The parser reads amounts with 2 decimals; currencies with other minor units (JPY 0, BHD 3) are rescaled. */
    fun inMinorUnits(fractionDigits: Int): ReceiptResult {
        if (fractionDigits == 2) return this
        fun scale(c: Long): Long = when {
            fractionDigits < 2 -> { var d = 1L; repeat(2 - fractionDigits) { d *= 10 }; (c + d / 2) / d }
            else -> { var m = 1L; repeat(fractionDigits - 2) { m *= 10 }; c * m }
        }
        return ReceiptResult(totalCents?.let(::scale), candidates.map(::scale).distinct(), confident)
    }

    companion object {
        val EMPTY = ReceiptResult(null, emptyList(), false)
    }
}
