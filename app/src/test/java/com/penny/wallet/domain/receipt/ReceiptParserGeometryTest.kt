package com.penny.wallet.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

/** OCR blocks with boxes: rows have to be rebuilt from geometry before the labels can be read. */
class ReceiptParserGeometryTest {
    private val h = 40f // text height in px
    private val gap = 60f // row pitch in px

    /** A block whose top edge sits on a line through (0, [rowTop]) tilted by [slope]. */
    private fun block(text: String, left: Float, right: Float, rowTop: Float, slope: Float = 0f): OcrLine {
        val top = rowTop + slope * left
        return OcrLine(text, left, top, right, top + h, slope)
    }

    /** Two-column receipt: label block on the left, amount block on the right, one row each. */
    private fun twoColumn(rows: List<Pair<String, String?>>, slope: Float = 0f): List<OcrLine> =
        rows.flatMapIndexed { i, (label, amount) ->
            val y = 100f + i * gap
            listOfNotNull(
                block(label, 40f, 40f + label.length * 18f, y, slope),
                amount?.let { block(it, 820f, 820f + it.length * 18f, y, slope) },
            )
        }

    private val cafe = listOf(
        "Café Lindner" to null,
        "Cappuccino" to "3,20",
        "Croissant" to "2,30",
        "Apfelschorle 0,4l" to "3,10",
        "SUMME EUR" to "8,60",
        "Gegeben BAR" to "20,00",
        "Rückgeld" to "11,40",
        "MwSt 19%" to "1,37",
    )

    @Test fun labelAndAmountAreSeparateBlocksOnOneRow() {
        val lines = listOf(block("TOTAL", 40f, 160f, 500f), block("13,65", 820f, 920f, 503f))
        assertEquals(listOf("TOTAL 13,65"), ReceiptParser.groupRows(lines))
        assertReceipt(ReceiptParser.parse(lines), 1365, confident = true)
    }

    @Test fun straightTwoColumnReceipt() {
        val lines = twoColumn(cafe)
        val rows = ReceiptParser.groupRows(lines)
        assertEquals(cafe.size, rows.size)
        assertEquals("SUMME EUR 8,60", rows[4])
        assertEquals("Gegeben BAR 20,00", rows[5])
        assertReceipt(ReceiptParser.parse(lines), 860, confident = true)
    }

    @Test fun tiltedPhotoDownwards() {
        // 0.1 slope: the right column sits ~80px (more than a row) lower than its label.
        val lines = twoColumn(cafe, slope = 0.1f)
        val rows = ReceiptParser.groupRows(lines)
        assertEquals(cafe.size, rows.size)
        assertEquals("SUMME EUR 8,60", rows[4])
        assertReceipt(ReceiptParser.parse(lines), 860, confident = true)
    }

    @Test fun tiltedPhotoUpwards() {
        val lines = twoColumn(cafe, slope = -0.08f).map { it.copy(top = it.top + 200f, bottom = it.bottom + 200f) }
        val rows = ReceiptParser.groupRows(lines)
        assertEquals("Rückgeld 11,40", rows[6])
        assertReceipt(ReceiptParser.parse(lines), 860, confident = true)
    }

    @Test fun slightTiltWithoutSlopeInfo() {
        // Real photos are rarely perfectly straight; a 0.02 tilt that OCR did not report still reads one row.
        val lines = twoColumn(cafe, slope = 0.02f).map { it.copy(slope = 0f) }
        assertEquals("SUMME EUR 8,60", ReceiptParser.groupRows(lines)[4])
        assertReceipt(ReceiptParser.parse(lines), 860, confident = true)
    }

    @Test fun labelOnOneRowAmountOnTheNext() {
        val lines = listOf(
            block("Espresso", 40f, 200f, 100f), block("2,10", 820f, 900f, 100f),
            block("Tramezzino", 40f, 240f, 160f), block("4,50", 820f, 900f, 160f),
            block("TOTALE COMPLESSIVO", 40f, 400f, 220f),
            block("6,60", 820f, 900f, 280f),
            block("Contanti", 40f, 200f, 340f), block("10,00", 820f, 920f, 340f),
            block("Resto", 40f, 140f, 400f), block("3,40", 820f, 900f, 400f),
        )
        assertEquals("TOTALE COMPLESSIVO", ReceiptParser.groupRows(lines)[2])
        assertReceipt(ReceiptParser.parse(lines), 660, confident = true)
    }

    @Test fun shuffledLineOrderGivesTheSameRows() {
        val lines = twoColumn(cafe, slope = 0.05f)
        val expected = ReceiptParser.groupRows(lines)
        repeat(5) { seed ->
            val shuffled = lines.shuffled(Random(seed))
            assertEquals(expected, ReceiptParser.groupRows(shuffled))
            assertReceipt(ReceiptParser.parse(shuffled), 860, confident = true)
        }
    }

    @Test fun rowTextReadsLeftToRight() {
        val lines = listOf(block("EUR 5,50", 600f, 760f, 100f), block("Betrag", 40f, 160f, 102f), block("VISA", 300f, 380f, 98f))
        assertEquals(listOf("Betrag VISA EUR 5,50"), ReceiptParser.groupRows(lines))
    }

    @Test fun middleColumnOfThreeStaysOnRow() {
        val lines = listOf(
            block("2 x", 40f, 100f, 100f), block("Brötchen", 140f, 300f, 100f), block("0,90", 820f, 900f, 101f),
            block("Summe", 40f, 160f, 160f), block("0,90", 820f, 900f, 161f),
        )
        assertEquals(listOf("2 x Brötchen 0,90", "Summe 0,90"), ReceiptParser.groupRows(lines))
    }

    @Test fun zeroHeightLineIsIgnored() {
        val flat = OcrLine("TOTAL 99,99", 40f, 300f, 400f, 300f)
        val lines = listOf(flat, block("Summe", 40f, 160f, 100f), block("4,20", 820f, 900f, 100f))
        assertEquals(listOf("Summe 4,20"), ReceiptParser.groupRows(lines))
        assertReceipt(ReceiptParser.parse(lines), 420, confident = true)
    }

    @Test fun onlyZeroHeightLines() {
        val lines = listOf(OcrLine("TOTAL 5,50", 0f, 100f, 200f, 100f))
        assertEquals(emptyList<String>(), ReceiptParser.groupRows(lines))
        assertEquals(ReceiptResult.EMPTY, ReceiptParser.parse(lines))
    }

    @Test fun invertedBoxIsIgnored() {
        val lines = listOf(OcrLine("TOTAL 5,50", 0f, 140f, 200f, 100f))
        assertEquals(ReceiptResult.EMPTY, ReceiptParser.parse(lines))
    }

    @Test fun blankLinesIgnored() {
        val lines = listOf(block("   ", 40f, 160f, 100f), block("", 300f, 400f, 100f))
        assertEquals(emptyList<String>(), ReceiptParser.groupRows(lines))
        assertEquals(ReceiptResult.EMPTY, ReceiptParser.parse(lines))
    }

    @Test fun noLines() {
        assertEquals(emptyList<String>(), ReceiptParser.groupRows(emptyList()))
        assertEquals(ReceiptResult.EMPTY, ReceiptParser.parse(emptyList()))
    }

    @Test fun nanSlopeFallsBackToStraight() {
        val lines = twoColumn(cafe).map { it.copy(slope = Float.NaN) }
        assertReceipt(ReceiptParser.parse(lines), 860, confident = true)
    }

    @Test fun tallerHeaderDoesNotSwallowRows() {
        val header = OcrLine("PENNY MARKT", 40f, 0f, 600f, 90f)
        val lines = listOf(header) + twoColumn(cafe.drop(1))
        assertReceipt(ReceiptParser.parse(lines), 860, confident = true)
    }
}
