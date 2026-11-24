package com.penny.wallet.domain.receipt

import java.time.LocalDate

/**
 * Picks the paid total out of OCR'd receipt text.
 *
 * 1. Lines are grouped into visual rows (label and amount are often separate OCR blocks on one row);
 *    the photo's tilt is removed first using the median line slope.
 * 2. Each row is classified by its label ([ReceiptKeywords]) and its amounts read ([AmountExtractor]).
 * 3. Labelled rows (total, amount due, card payment) vote for their rightmost amount, or for the next
 *    row's amount when the label stands alone. The highest weight wins; an amount seen on several
 *    labelled rows (e.g. "TOTAL 5,50" and "VISA 5,50") gets a bonus; ties go to the larger amount.
 *    OCR slips are tolerated: "TUTALE" counts as a fuzzy total, and a total row whose comma was lost
 *    ("308") counts when 3,08 appears elsewhere (otherwise it is offered but not confident).
 *    In unlabelled item rows a trailing VAT rate ("0,49 22,00") is not an amount.
 * 4. With no labelled row, the largest amount on a row that is not cash given / change / tax wins,
 *    and the result is marked not confident.
 */
object ReceiptParser {
    private const val MAX_CANDIDATES = 5
    private const val REPEAT_BONUS = 15

    internal data class Row(val text: String, val kind: RowKind, val amounts: List<Long>)

    /** Parses plain text, one receipt row per line. */
    fun parseText(text: String, today: LocalDate = LocalDate.now()): ReceiptResult = parseAll(text.lines().filter { it.isNotBlank() }, today)

    fun parse(lines: List<OcrLine>, today: LocalDate = LocalDate.now()): ReceiptResult = parseAll(groupRows(lines), today)

    /** Shop and date only matter next to an amount; without one the result stays [ReceiptResult.EMPTY]. */
    private fun parseAll(rows: List<String>, today: LocalDate): ReceiptResult {
        val r = parseRows(rows.map(::row))
        if (r.totalCents == null) return r
        return r.copy(merchant = ReceiptDetails.merchant(rows), date = ReceiptDetails.date(rows, today))
    }

    /** Typical VAT rates in Europe, in cents ("22,00" in an "%Iva" column is a rate, not a price). */
    private val VAT_RATES = setOf(400L, 500L, 550L, 600L, 700L, 800L, 900L, 1000L, 1200L, 1300L, 1400L, 1500L, 1600L,
        1700L, 1800L, 1900L, 2000L, 2100L, 2200L, 2300L, 2400L, 2500L, 2700L)

    /** A bare 3–6 digit number: a total whose decimal comma OCR lost ("3,08" read as "308"). */
    private val bareNumber = Regex("""(?<![\d.,/:-])\d{3,6}(?![\d.,/:%-]|\s?%)""")

    /** Weight of a lost-comma total nothing else on the receipt confirms: offered, never "confident". */
    private const val UNVERIFIED = 30
    private const val CONFIDENT = 45

    private fun row(text: String): Row {
        val kind = ReceiptKeywords.classify(text)
        var amounts = AmountExtractor.amounts(text)
        // Item row "0,49 22,00": price then VAT rate column. Drop the rate.
        if (kind == RowKind.PLAIN && amounts.size >= 2 && amounts.last() in VAT_RATES) amounts = amounts.dropLast(1)
        return Row(text, kind, amounts)
    }

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
        val cardVotes = mutableSetOf<Long>()
        val everywhere = rows.flatMap { it.amounts }.toSet()
        fun vote(amount: Long, weight: Int) {
            votes[amount] = maxOf(votes[amount] ?: 0, weight)
            hits[amount] = (hits[amount] ?: 0) + 1
        }
        rows.forEachIndexed { i, r ->
            if (r.kind.weight <= 0) return@forEachIndexed
            // A fuzzy label alone ("SUMMER SALE" banner) must not borrow the next item's price.
            val amount = r.amounts.lastOrNull() ?: if (r.kind != RowKind.TOTAL_FUZZY) labelOnlyAmount(rows, i) else null
            if (amount != null) {
                if (r.kind == RowKind.CARD) cardVotes += amount
                return@forEachIndexed vote(amount, r.kind.weight)
            }
            // "TOTALE 308": comma lost. Trust it fully only if 3,08 also appears elsewhere (e.g. the card line).
            if (r.kind.weight >= RowKind.TOTAL_FUZZY.weight) {
                val bare = bareNumber.findAll(r.text).lastOrNull()?.value?.toLongOrNull() ?: return@forEachIndexed
                if (bare in 1..AmountExtractor.MAX_CENTS) vote(bare, if (bare in everywhere) r.kind.weight else UNVERIFIED)
            }
        }
        val others = rows.filter { it.kind != RowKind.EXCLUDED }.flatMap { it.amounts }.distinct().sortedDescending()

        if (votes.isNotEmpty()) {
            fun score(a: Long) = votes.getValue(a) + REPEAT_BONUS * ((hits[a] ?: 1) - 1)
            val ranked = votes.keys.sortedWith(compareByDescending<Long> { score(it) }.thenByDescending { it })
            // Only a fuzzy word ("Summer Hat" ≈ summe) backs the leader, yet a card line paid another amount: trust the card.
            val leader = ranked.first()
            val best = if (votes.getValue(leader) <= RowKind.TOTAL_FUZZY.weight && leader !in cardVotes) {
                ranked.firstOrNull { it in cardVotes } ?: leader
            } else leader
            val candidates = (listOf(best) + ranked + others).distinct().take(MAX_CANDIDATES)
            return ReceiptResult(best, candidates, confident = votes.getValue(best) >= CONFIDENT)
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
