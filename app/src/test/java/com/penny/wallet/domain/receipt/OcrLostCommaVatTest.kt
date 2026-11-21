package com.penny.wallet.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lost decimal comma on total rows ("TOTALE 308"), the VAT-rate column and the confident flag. */
class OcrLostCommaVatTest {
    /** Items 0,49 + 2,00 then [totalRow] last: a bare number that is not a price must leave the fallback (200). */
    private fun ignoredBare(vararg totalRows: String) {
        val wrong = totalRows.mapNotNull { row ->
            val r = receipt("Pane 0,49\nLatte 2,00\n$row")
            if (r.totalCents == 200L && !r.confident) null else "\"$row\" → ${r.totalCents} confident=${r.confident}"
        }
        assertTrue(wrong.joinToString("; "), wrong.isEmpty())
    }

    // lost comma

    @Test fun lostCommaConfirmedByCard() = assertReceipt(receipt("Pane 1,08\nLatte 2,00\nTOTALE 308\nVISA 3,08"), 308, confident = true)

    @Test fun lostCommaUnconfirmed() {
        val r = receipt("Pane 0,49\nLatte 2,00\nTOTALE 308")
        assertReceipt(r, 308, confident = false)
        assertTrue(r.candidates.containsAll(listOf(200L, 49L)))
    }

    @Test fun lostCommaFourDigits() = assertReceipt(receipt("Vino 12,34\nTOTALE 1234\nMASTERCARD 12,34"), 1234, confident = true)

    @Test fun lostCommaOnFuzzyRow() = assertReceipt(receipt("Pane 3,08\nTUTALE 308\nVISA 3,08"), 308, confident = true)

    @Test fun lostCommaDoesNotBeatRealTotal() = assertReceipt(
        receipt("Pane 2,00\nLatte 4,10\nTOTALE PUNTI 1250\nTOTALE 6,10"), 610, confident = true,
    )

    @Test fun datesOnTotalRowIgnored() = ignoredBare("TOTALE 20/11/2026", "TOTALE 20.11.2026", "TOTALE 20-11-2026")

    /** ISO dates: "2026" is followed by "-", which the bare-number lookahead does not exclude. */
    @Test fun isoDateOnTotalRowIgnored() = ignoredBare("TOTALE 2026-11-20")

    @Test fun timesOnTotalRowIgnored() = ignoredBare("TOTALE 12:30", "TOTALE 12:30:45", "TOTALE ORE 9:05")

    @Test fun phoneNumbersOnTotalRowIgnored() = ignoredBare("TOTALE TEL 011/6699819", "TOTALE 0116699819", "TOTALE +390116699819")

    @Test fun percentagesOnTotalRowIgnored() = ignoredBare("TOTALE 100%", "TOTALE 150%")

    /** OCR often puts a space before "%". */
    @Test fun spacedPercentageOnTotalRowIgnored() = ignoredBare("TOTALE 100 %")

    @Test fun bareNumberOnPlainRowNeverUsed() =
        assertReceipt(receipt("Scontrino 4711\nArticolo 308\nCola 2,00\nPane 1,00"), 200, confident = false)

    @Test fun bareNumberOnCardRowNeverUsed() = assertReceipt(receipt("Cola 2,00\nVISA 308"), 200, confident = false)

    @Test fun bareNumberOnAmountRowNeverUsed() = assertReceipt(receipt("Cola 2,00\nBetrag 308"), 200, confident = false)

    // VAT column

    @Test fun vatRateDroppedFromItemRow() {
        val r = receipt("Pane 0,49 22,00")
        assertReceipt(r, 49, confident = false)
        assertFalse(2200L in r.candidates)
    }

    /** Known trade-off: a line total that equals a VAT rate (10,00) is dropped as if it were the rate. */
    @Test fun lineTotalEqualToRateIsDropped() = assertReceipt(receipt("Pizza 2 x 5,00 10,00"), 500, confident = false)

    @Test fun singleAmountEqualToRateIsKept() = assertReceipt(receipt("Vino 22,00"), 2200, confident = false)

    @Test fun nonRateSecondAmountIsKept() = assertReceipt(receipt("Pane 0,49 3,11"), 311, confident = false)

    /** Labelled rows are never touched: the last amount (here 22,00) is what the total row votes for. */
    @Test fun totalRowKeepsTrailingRate() = assertReceipt(receipt("Pane 3,08\nTOTALE 3,08 22,00"), 2200, confident = true)

    @Test fun italianFallbackPicksPriceNotRate() {
        val r = receipt(
            """
            SUPERMERCATO ROSSI
            DESCRIZIONE PREZZO %IVA
            PANE 1,20 4,00
            LATTE 1,50 10,00
            VINO ROSSO 8,90 22,00
            GRAZIE E ARRIVEDERCI
            """,
        )
        assertReceipt(r, 890, confident = false)
        assertEquals(listOf(890L, 150L, 120L), r.candidates)
    }

    // confident flag

    @Test fun cardOnlyIsConfident() = assertReceipt(receipt("Cola 2,00\nChips 1,50\nVISA 3,50"), 350, confident = true)

    @Test fun unverifiedOnlyIsNotConfident() = assertReceipt(receipt("Cola 2,00\nTOTALE 350"), 350, confident = false)

    @Test fun fallbackIsNotConfident() = assertReceipt(receipt("Cola 2,00\nChips 1,50"), 200, confident = false)
}
