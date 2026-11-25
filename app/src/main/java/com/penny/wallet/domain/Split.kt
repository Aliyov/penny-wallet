package com.penny.wallet.domain

/** One part of a split expense; [categoryId] may be unset while the user is still filling it in. */
data class SplitPart(val categoryId: String?, val amountCents: Long)

/** The parts always add up to the receipt total: the first part takes whatever the others leave. */
object Split {
    /** [parts] with the first one's amount set to [total] minus the others; that rest may be 0 or negative. */
    fun balanced(total: Long, parts: List<SplitPart>): List<SplitPart> {
        if (parts.isEmpty()) return parts
        val others = parts.drop(1).sumOf { it.amountCents }
        return listOf(parts.first().copy(amountCents = total - others)) + parts.drop(1)
    }

    /** Ready to save: at least two parts, every part with a category and a positive amount, adding up to [total]. */
    fun isComplete(total: Long, parts: List<SplitPart>): Boolean =
        total > 0 && parts.size >= 2 && parts.all { it.categoryId != null && it.amountCents > 0 } && parts.sumOf { it.amountCents } == total

    /** Starting point when the user taps Split: the current category with everything, plus an empty second part. */
    fun start(total: Long, categoryId: String?): List<SplitPart> = balanced(total, listOf(SplitPart(categoryId, total), SplitPart(null, 0)))
}
