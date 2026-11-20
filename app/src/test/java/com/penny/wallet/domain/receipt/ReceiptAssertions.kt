package com.penny.wallet.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Checks the result and the invariants every [ReceiptResult] must keep. */
internal fun assertReceipt(result: ReceiptResult, totalCents: Long?, confident: Boolean) {
    assertEquals("totalCents", totalCents, result.totalCents)
    assertEquals("confident", confident, result.confident)
    assertCandidates(result)
}

internal fun assertCandidates(result: ReceiptResult) {
    val c = result.candidates
    if (result.totalCents != null) assertEquals("candidates must start with the total: $c", result.totalCents, c.firstOrNull())
    else assertTrue("no total means no candidates: $c", c.isEmpty())
    assertEquals("candidates must be distinct: $c", c.distinct(), c)
    assertTrue("at most 5 candidates: $c", c.size <= 5)
    assertTrue("candidates must be positive: $c", c.all { it > 0 })
}

/** Parses a receipt written as an indented multi-line string. */
internal fun receipt(text: String): ReceiptResult = ReceiptParser.parseText(text.trimIndent())
