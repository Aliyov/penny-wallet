package com.penny.wallet.domain.receipt

/**
 * Picks the paid total out of OCR'd receipt text.
 *
 * 1. Lines are grouped into visual rows (label and amount are often separate OCR blocks on one row);
 *    the photo's tilt is removed first using the median line slope.
 * 2. Each row is classified by its label ([ReceiptKeywords]) and its amounts read ([AmountExtractor]).
 * 3. Labelled rows (total, amount due, card payment) vote for their rightmost amount, or for the next
 *    row's amount when the label stands alone. The highest weight wins; an amount seen on several
 *    labelled rows (e.g. "TOTAL 5,50" and "VISA 5,50") gets a bonus; ties go to the larger amount.
 * 4. With no labelled row, the largest amount on a row that is not cash given / change / tax wins,
 *    and the result is marked not confident.
 */
object ReceiptParser {
    private const val MAX_CANDIDATES = 5
    private const val REPEAT_BONUS = 15

    internal data class Row(val text: String, val kind: RowKind, val amounts: List<Long>)

    /** Parses plain text, one receipt row per line. */
    fun parseText(text: String): ReceiptResult = parseRows(text.lines().filter { it.isNotBlank() }.map(::row))

    fun parse(lines: List<OcrLine>): ReceiptResult = parseRows(groupRows(lines).map(::row))

    private fun row(text: String) = Row(text, ReceiptKeywords.classify(text), AmountExtractor.amounts(text))

    /** Clusters OCR lines into rows by their deskewed vertical centre; each row's text reads left to right. */
    internal fun groupRows(lines: List<OcrLine>): List<String> {
        val usable = lines.filter { it.text.isNotBlank() && it.bottom > it.top }
        if (usable.isEmpty()) return emptyList()
        val slope = usable.map { it.slope }.sorted()[usable.size / 2].takeIf { it.isFinite() } ?: 0f
        val heights = usable.map { it.bottom - it.top }.sorted()
        val tolerance = heights[heights.size / 2] * 0.55f
        data class Placed(val line: OcrLine, val y: Float, val x: Float)
        val placed = usable.map { l ->
            val cx = (l.left + l.right) / 2f
            val cy = (l.top + l.bottom) / 2f
            Placed(l, cy - slope * cx, l.left)
        }.sortedBy { it.y }
        val rows = mutableListOf<MutableList<Placed>>()
        for (p in placed) {
            val current = rows.lastOrNull()
            if (current != null && kotlin.math.abs(p.y - current.map { it.y }.average().toFloat()) <= tolerance) current += p
            else rows += mutableListOf(p)
        }
        return rows.map { r -> r.sortedBy { it.x }.joinToString(" ") { it.line.text.trim() } }
    }

    internal fun parseRows(rows: List<Row>): ReceiptResult {
        val votes = mutableMapOf<Long, Int>() // amount → best weight
        val hits = mutableMapOf<Long, Int>() // amount → number of labelled rows
        rows.forEachIndexed { i, r ->
            if (r.kind.weight <= 0) return@forEachIndexed
            val amount = r.amounts.lastOrNull() ?: labelOnlyAmount(rows, i) ?: return@forEachIndexed
            votes[amount] = maxOf(votes[amount] ?: 0, r.kind.weight)
            hits[amount] = (hits[amount] ?: 0) + 1
        }
        val others = rows.filter { it.kind != RowKind.EXCLUDED }.flatMap { it.amounts }.distinct().sortedDescending()

        if (votes.isNotEmpty()) {
            fun score(a: Long) = votes.getValue(a) + REPEAT_BONUS * ((hits[a] ?: 1) - 1)
            val ranked = votes.keys.sortedWith(compareByDescending<Long> { score(it) }.thenByDescending { it })
            val candidates = (ranked + others).distinct().take(MAX_CANDIDATES)
            return ReceiptResult(ranked.first(), candidates, confident = true)
        }
        if (others.isEmpty()) return ReceiptResult.EMPTY
        return ReceiptResult(others.first(), others.take(MAX_CANDIDATES), confident = false)
    }

    /** A label alone on its row ("TOTAL" / "5,50" split over two rows): take the next row's amount if it is unlabelled. */
    private fun labelOnlyAmount(rows: List<Row>, i: Int): Long? {
        val next = rows.getOrNull(i + 1) ?: return null
        if (next.kind != RowKind.PLAIN) return null
        return next.amounts.lastOrNull()
    }
}
