package com.penny.wallet.domain.receipt

import com.penny.wallet.domain.receipt.RowKind.CARD
import com.penny.wallet.domain.receipt.RowKind.EXCLUDED
import com.penny.wallet.domain.receipt.RowKind.PLAIN
import com.penny.wallet.domain.receipt.RowKind.TOTAL
import com.penny.wallet.domain.receipt.RowKind.TOTAL_FUZZY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fuzzy "total" / card labels ([ReceiptKeywords.closeTo]) and how they rank against real labels. */
class OcrSlipRulesTest {
    private fun close(word: String, key: String) = ReceiptKeywords.closeTo(word, key)
    private fun check(kind: RowKind, vararg labels: String) = labels.forEach {
        assertEquals("\"$it\"", kind, ReceiptKeywords.classify(it))
    }

    // closeTo

    @Test fun oneLetterSlip() {
        assertTrue(close("tutale", "totale"))
        assertTrue(close("totl", "total")) // dropped letter
        assertTrue(close("tottal", "total")) // doubled letter
    }

    @Test fun digitSlip() = assertTrue(close("t0tal", "total"))

    @Test fun longKeywordAllowsTwoEdits() {
        assertTrue(close("hastercard", "mastercard"))
        assertTrue(close("hasterkard", "mastercard"))
        assertFalse(close("hasterkurt", "mastercard"))
    }

    /** Known: "summer" is one letter from "summe", so an English "Summer ..." item looks like a fuzzy total. */
    @Test fun summerIsCloseToSumme() = assertTrue(close("summer", "summe"))

    @Test fun exactMatch() {
        assertTrue(close("total", "total"))
        assertTrue(close("mastercard", "mastercard"))
    }

    @Test fun lengthDifferenceTooBig() {
        assertFalse(close("tota", "totale")) // 2 letters short, keyword < 8 letters
        assertFalse(close("totalissimo", "total"))
        assertFalse(close("mastercardxyz", "mastercard")) // 3 extra on a 10-letter keyword
    }

    @Test fun twoEditsOnShortKeywordFail() {
        assertFalse(close("totem", "total"))
        assertFalse(close("metal", "total"))
        assertFalse(close("natale", "totale"))
    }

    /** Keywords under 5 letters never match here, not even exactly: exact hits go through the regex lists. */
    @Test fun shortKeywordsNeverFuzzy() {
        assertFalse(close("vixa", "visa"))
        assertFalse(close("kart", "karte".take(4)))
        assertFalse(close("visa", "visa"))
    }

    // classify

    @Test fun fuzzyRows() = check(TOTAL_FUZZY, "TUTALE 7,00", "TOTL 7,00", "Tutal:", "SUMNE 12,00", "GESAMI 9,99")

    /** An O read as a zero is the most common OCR slip in "TOTAL". */
    @Test fun digitSlipRowIsFuzzyTotal() = check(TOTAL_FUZZY, "T0TAL 7,00", "T0TALE 3,08")

    @Test fun fuzzyCardRows() = check(CARD, "HASTERCARD 7,00", "CONTACTLES 7,00", "KONTAKTL0S", "GIROKARD 7,00")

    @Test fun nearbyItemNamesThatAreFuzzy() = check(TOTAL_FUZZY, "Summer Drink 3,50", "Tonal Shampoo 3,00")

    @Test fun nearbyItemNamesThatStayPlain() = check(PLAIN, "Totem Pole 5,00", "Metal Box 4,00", "Panettone 9,90", "Natale 2,00")

    @Test fun exclusionsBeatFuzzy() = check(
        EXCLUDED, "SUBTOTALE 5,00", "TUTALE IVA 1,10", "TUTALE CONTANTI 10,00", "TUTALE RESTO 3,90", "Sconto TUTALE 1,00",
    )

    /** Italian receipts print the discount summary as "TOTALE SCONTI", often without a minus sign. */
    @Test fun discountSummaryIsExcluded() = check(EXCLUDED, "TOTALE SCONTI 1,82", "Totale sconti -1,82")

    @Test fun exactLabelBeatsFuzzy() = check(TOTAL, "TOTAL Summer 7,00")

    // receipts

    @Test fun fuzzyItemDoesNotBeatRealTotal() = assertReceipt(
        receipt(
            """
            Summer Drink 3,50
            Cola 3,50
            TOTAL 7,00
            """,
        ),
        700, confident = true,
    )

    @Test fun fuzzyTotalAloneWins() = assertReceipt(
        receipt(
            """
            Pizza 5,00
            Cola 2,00
            TUTAL 7,00
            """,
        ),
        700, confident = true,
    )

    @Test fun itemsNearTotalWordsDoNotTakeOver() = assertReceipt(
        receipt(
            """
            Tonal Shampoo 3,00
            Totem Pole 2,40
            Metal Box 4,00
            TOTAL 9,40
            """,
        ),
        940, confident = true,
    )

    @Test fun subtotalIsNotATotal() = assertReceipt(receipt("SUBTOTALE 5,00\nIVA 1,10\nTOTALE 6,10"), 610, confident = true)

    @Test fun negativeDiscountSummaryNeverWins() = assertReceipt(
        receipt(
            """
            Pane 2,00
            Latte 5,92
            Totale Sconti -1,82
            TOTALE 6,10
            """,
        ),
        610, confident = true,
    )

    @Test fun negativeDiscountSummaryBeforeCardLine() = assertReceipt(
        receipt("Pane 2,00\nLatte 5,92\nTotale Sconti -1,82\nCARTA 6,10"), 610, confident = true,
    )

    /** "TOTALE SCONTI 1,82" without a minus: weight 90 would beat the card line. */
    @Test fun positiveDiscountSummaryDoesNotBeatCard() = assertReceipt(
        receipt("Pane 2,00\nLatte 5,92\nTOTALE SCONTI 1,82\nCARTA 6,10"), 610, confident = true,
    )

    /** A fuzzy item (70) must not outrank the only real label, a card line (45). */
    @Test fun fuzzyItemDoesNotBeatCardLine() = assertReceipt(
        receipt(
            """
            Summer Hat 12,00
            Socks 3,00
            VISA 15,00
            """,
        ),
        1500, confident = true,
    )

    /** A "SUMMER SALE" banner is label-only, so it would grab the next item's price. */
    @Test fun fuzzyBannerDoesNotGrabNextItem() = assertReceipt(
        receipt(
            """
            SUMMER SALE
            Cola 2,00
            Chips 1,50
            VISA 3,50
            """,
        ),
        350, confident = true,
    )

    @Test fun fuzzyCardConfirmsTotal() = assertReceipt(receipt("Pizza 5,00\nTUTALE 5,00\nHASTERCARD 5,00"), 500, confident = true)
}
