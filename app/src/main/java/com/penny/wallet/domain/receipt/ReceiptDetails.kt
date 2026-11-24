package com.penny.wallet.domain.receipt

import java.time.DateTimeException
import java.time.LocalDate

/** Shop name and purchase date from the receipt rows (top to bottom). */
internal object ReceiptDetails {
    private const val HEADER_ROWS = 4
    private const val MAX_NAME = 32

    /** Words that start a non-name header row: document titles, contact lines, legal ids. */
    private val notAName = Regex(
        """\b(www|http|https|tel|telefon|phone|fax|email|e mail|iva|p iva|vat|ust|ust id|steuer|st nr|uid|cif|nif|""" +
            """receipt|bon|kassenbon|beleg|rechnung|quittung|scontrino|fattura|documento|ricevuta|commerciale|""" +
            """welcome|willkommen|benvenuti|grazie|arrivederci|danke|dank|vielen|einkauf|thank|thanks|merci)\b""",
    )
    private val street = Regex("""\b(str|strasse|straße|via|viale|piazza|corso|platz|weg|allee|road|rd|street|st|avenue|ave)\b""")

    fun merchant(rows: List<String>): String? {
        for (raw in rows.take(HEADER_ROWS)) {
            val text = raw.trim().trim('*', '-', '=', '#', '.', ' ')
            val letters = text.count { it.isLetter() }
            val digits = text.count { it.isDigit() }
            if (letters < 3 || digits > 2 || letters * 10 < text.count { !it.isWhitespace() } * 6) continue
            val n = ReceiptKeywords.normalize(text)
            if (notAName.containsMatchIn(n) || street.containsMatchIn(n)) continue
            if (ReceiptKeywords.classify(text) != RowKind.PLAIN) continue
            return tidy(text).take(MAX_NAME).trim()
        }
        return null
    }

    /** "LIDL" stays, "ESSELUNGA SPA" becomes "Esselunga Spa"; mixed case is kept as printed. */
    private fun tidy(s: String): String {
        val collapsed = s.replace(Regex("""\s+"""), " ")
        if (collapsed.any { it.isLowerCase() }) return collapsed
        return collapsed.split(' ').joinToString(" ") { w ->
            if (w.length <= 4) w else w.lowercase().replaceFirstChar { it.titlecase() }
        }
    }

    private val dayFirst = Regex("""(?<!\d)(\d{1,2})[./-](\d{1,2})[./-](\d{4}|\d{2})(?!\d)""")
    private val isoDate = Regex("""(?<!\d)(\d{4})-(\d{2})-(\d{2})(?!\d)""")

    /**
     * First plausible date on the receipt: day-first (EU), unless the second number can only be a day (US order).
     * [today] bounds the search: receipts are never from the future and rarely older than a year.
     */
    fun date(rows: List<String>, today: LocalDate): LocalDate? {
        for (row in rows) {
            isoDate.findAll(row).forEach { m ->
                val (y, mo, d) = m.destructured
                valid(y.toInt(), mo.toInt(), d.toInt(), today)?.let { return it }
            }
            dayFirst.findAll(row).forEach { m ->
                val (a, b, yRaw) = m.destructured
                val y = if (yRaw.length == 2) 2000 + yRaw.toInt() else yRaw.toInt()
                val first = a.toInt()
                val second = b.toInt()
                val (day, month) = if (second > 12 && first <= 12) second to first else first to second
                valid(y, month, day, today)?.let { return it }
            }
        }
        return null
    }

    private fun valid(y: Int, m: Int, d: Int, today: LocalDate): LocalDate? = try {
        LocalDate.of(y, m, d).takeIf { !it.isAfter(today) && !it.isBefore(today.minusYears(1)) }
    } catch (_: DateTimeException) {
        null
    }
}
