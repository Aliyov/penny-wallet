package com.penny.wallet.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserEdgeTest {
    private fun empty(text: String) = assertEquals("\"$text\"", ReceiptResult.EMPTY, ReceiptParser.parseText(text))

    @Test fun emptyText() = empty("")
    @Test fun whitespaceOnly() = empty("   \n\n\t \n")
    @Test fun garbage() = empty("@@@ ### ~~~\nlorem ipsum dolor\n|||| ////")
    @Test fun onlyADate() = empty("20.11.2026")
    @Test fun onlyDateAndTime() = empty("Datum 20.11.26  14:32\nBon 4711  Kasse 2")
    @Test fun labelWithoutAmount() = empty("TOTAL")
    @Test fun labelFollowedByText() = empty("SUMME\nVielen Dank für Ihren Einkauf")
    @Test fun onlyPercentagesAndWeights() = empty("MwSt 19,00%\n0,456 kg\n7%")
    @Test fun onlyNegativeAmounts() = empty("Rabatt -1,20\nPfand 0,25-")

    @Test fun onlyCashAndChangeGiveNothing() = empty("Bar 10,00\nRückgeld 4,50")

    @Test fun singleAmount() = assertReceipt(ReceiptParser.parseText("4,20"), 420, confident = false)

    @Test fun windowsLineEndings() = assertReceipt(
        ReceiptParser.parseText("Kaffee 2,50\r\nKuchen 3,00\r\nSUMME 5,50\r\nBar 10,00\r\n"), 550, confident = true,
    )

    @Test fun totalLabelBeatsLargerUnlabelledAmount() = assertReceipt(
        receipt(
            """
            Hotel Garni
            Zimmer 2 Nächte      180,00
            Anzahlung             100,00-
            Zu zahlen              80,00
            """,
        ),
        8000, confident = true,
    )

    @Test fun strongBeatsPlainTotal() = assertReceipt(
        receipt(
            """
            Total               40,00
            Gutschein           10,00
            Zu zahlen           30,00
            """,
        ),
        3000, confident = true,
    )

    @Test fun repeatedTotalAndCardAgree() = assertReceipt(
        receipt(
            """
            Pizza                9,50
            Cola                 3,00
            Total               12,50
            Mastercard          12,50
            """,
        ),
        1250, confident = true,
    )

    @Test fun candidatesCappedAtFive() {
        val text = (1..12).joinToString("\n") { "Artikel $it   ${it},${10 + it}" } + "\nSUMME 99,99"
        val r = ReceiptParser.parseText(text)
        assertReceipt(r, 9999, confident = true)
        assertEquals(5, r.candidates.size)
    }

    @Test fun candidatesListLargestOthersAfterTotal() {
        val r = receipt(
            """
            Flat White      3,20
            Croissant       2,75
            Total           5,95
            Cash           10,00
            Change          4,05
            """,
        )
        assertReceipt(r, 595, confident = true)
        assertTrue("cash/change must not be offered: ${r.candidates}", 1000L !in r.candidates && 405L !in r.candidates)
        assertEquals(listOf(595L, 320L, 275L), r.candidates)
    }

    @Test fun fallbackCandidatesSortedDescending() {
        val r = ReceiptParser.parseText("1,20\n3,40\n2,10")
        assertReceipt(r, 340, confident = false)
        assertEquals(listOf(340L, 210L, 120L), r.candidates)
    }

    @Test fun ocrSlipsOnTheTotalLine() = assertReceipt(
        ReceiptParser.parseText("Brot 2,49\nKäse 3,O1\nSUMME EUR 5, 5O"), 550, confident = true,
    )
}
