package com.penny.wallet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitTest {
    @Test fun firstPartTakesTheRest() {
        val p = Split.balanced(50_00, listOf(SplitPart("food", 0), SplitPart("home", 12_50), SplitPart("kids", 7_25)))
        assertEquals(listOf(30_25L, 12_50L, 7_25L), p.map { it.amountCents })
        assertEquals(50_00L, p.sumOf { it.amountCents })
    }

    @Test fun startKeepsTheCategoryAndAddsAnEmptyPart() {
        val p = Split.start(20_00, "food")
        assertEquals(listOf(SplitPart("food", 20_00), SplitPart(null, 0)), p)
        assertFalse(Split.isComplete(20_00, p))
    }

    @Test fun completeOnlyWhenEveryPartIsFilled() {
        assertTrue(Split.isComplete(20_00, listOf(SplitPart("a", 15_00), SplitPart("b", 5_00))))
        assertFalse(Split.isComplete(20_00, listOf(SplitPart("a", 20_00))))
        assertFalse(Split.isComplete(20_00, listOf(SplitPart("a", 15_00), SplitPart(null, 5_00))))
        assertFalse(Split.isComplete(20_00, listOf(SplitPart("a", 20_00), SplitPart("b", 0))))
        assertFalse(Split.isComplete(20_00, listOf(SplitPart("a", 25_00), SplitPart("b", -5_00))))
        assertFalse(Split.isComplete(20_00, listOf(SplitPart("a", 15_00), SplitPart("b", 4_00))))
        assertFalse(Split.isComplete(0, listOf(SplitPart("a", 0), SplitPart("b", 0))))
    }

    @Test fun othersLargerThanTotalLeaveANegativeRest() {
        val p = Split.balanced(10_00, listOf(SplitPart("a", 0), SplitPart("b", 12_00)))
        assertEquals(-2_00L, p.first().amountCents)
        assertFalse(Split.isComplete(10_00, p))
    }

    @Test fun zeroDecimalAndThreeDecimalCurrenciesAddUpExactly() {
        assertEquals(1_000L, Split.balanced(1_000, listOf(SplitPart("a", 0), SplitPart("b", 333), SplitPart("c", 333))).sumOf { it.amountCents })
        assertEquals(12_345L, Split.balanced(12_345, listOf(SplitPart("a", 0), SplitPart("b", 1))).sumOf { it.amountCents })
    }
}
