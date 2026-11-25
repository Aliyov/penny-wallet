package com.penny.wallet.domain.csv

import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.time.LocalDate

enum class CsvColumn { DATE, AMOUNT, DEBIT, CREDIT, TYPE, CATEGORY, NOTE, IGNORE }

/** Which column holds what. [pennyExport]: Penny's old CSV, whose amounts were always written with two decimals. */
data class CsvMapping(
    val columns: Map<Int, CsvColumn>,
    val hasHeader: Boolean,
    val datePattern: String?,
    val pennyExport: Boolean = false,
    /** Type column holds Penny's own names (EXPENSE, INCOME, TRANSFER…). */
    val pennyTypes: Boolean = pennyExport,
    /** Penny's own export since 2.1: each row names its currency, whose decimals the amount uses. */
    val currencyColumn: Int? = null,
) {
    fun indexOf(c: CsvColumn): Int? = columns.entries.firstOrNull { it.value == c }?.key
}

/** One imported expense or income; [amountCents] is always positive, [line] is 1-based as in a text editor. */
data class CsvRow(val line: Int, val date: LocalDate, val amountCents: Long, val income: Boolean, val category: String?, val note: String?)

data class CsvRowError(val line: Int, val reason: Reason) {
    enum class Reason { DATE, AMOUNT, ZERO, NOT_EXPENSE_OR_INCOME }
}

data class CsvPreview(val rows: List<CsvRow>, val errors: List<CsvRowError>)

object CsvImport {
    private val KEYWORDS: List<Pair<CsvColumn, List<String>>> = listOf(
        CsvColumn.DATE to listOf("date", "datum", "data", "period", "time", "zeit", "buchungstag", "valuta"),
        CsvColumn.DEBIT to listOf("debit", "soll", "ausgang", "addebito", "uscite", "withdrawal"),
        CsvColumn.CREDIT to listOf("credit", "haben", "eingang", "accredito", "entrate", "deposit"),
        CsvColumn.TYPE to listOf("income/expense", "type", "typ", "tipo", "umsatztyp", "kind"),
        CsvColumn.AMOUNT to listOf("amount", "betrag", "importo", "summe", "value", "valore", "eur", "usd", "gbp", "chf"),
        CsvColumn.CATEGORY to listOf("category", "kategorie", "categoria"),
        CsvColumn.NOTE to listOf("note", "notes", "description", "memo", "payee", "notiz", "beschreibung", "verwendungszweck", "descrizione", "nota", "causale", "comment"),
    )
    private val INCOME_WORDS = listOf("income", "einnahme", "eingang", "gutschrift", "entrata", "entrate", "credit", "haben", "inc")
    private val EXPENSE_WORDS = listOf("expense", "exp", "ausgabe", "ausgang", "lastschrift", "uscita", "uscite", "spesa", "spese", "debit", "soll")
    /** Running balances look like amounts but must never be imported as one. */
    private val BALANCE_WORDS = listOf("saldo", "balance", "kontostand")

    private fun words(header: String): List<String> = header.split(Regex("[^\\p{L}/]+")).filter { it.isNotEmpty() }

    fun guessMapping(table: List<List<String>>): CsvMapping {
        val header = table.firstOrNull().orEmpty().map { it.trim().lowercase() }
        // Penny's own export. Files from before 2.1 ("amount_eur") always used two decimals, whatever the currency.
        if (header.take(3) == listOf("date", "type", "amount_eur")) {
            return CsvMapping(
                mapOf(0 to CsvColumn.DATE, 1 to CsvColumn.TYPE, 2 to CsvColumn.AMOUNT, 5 to CsvColumn.CATEGORY, 7 to CsvColumn.NOTE),
                hasHeader = true, datePattern = "yyyy-MM-dd", pennyExport = true,
            )
        }
        if (header.take(4) == listOf("date", "type", "amount", "currency")) {
            return CsvMapping(
                mapOf(0 to CsvColumn.DATE, 1 to CsvColumn.TYPE, 2 to CsvColumn.AMOUNT, 6 to CsvColumn.CATEGORY, 8 to CsvColumn.NOTE),
                hasHeader = true, datePattern = "yyyy-MM-dd", pennyTypes = true, currencyColumn = 3,
            )
        }
        val byHeader = mutableMapOf<Int, CsvColumn>()
        val headerWords = header.map(::words)
        for ((column, keywords) in KEYWORDS) {
            // Keyword order is priority ("betrag" beats "eur"); whole words only, so "Wertstellung" is no amount.
            // Within one keyword the exact header wins ("amount" over "converted amount"), then the earlier column.
            val idx = keywords.firstNotNullOfOrNull { kw ->
                val free = header.indices.filter { i -> i !in byHeader && (column != CsvColumn.AMOUNT || headerWords[i].none { it in BALANCE_WORDS }) }
                free.firstOrNull { header[it] == kw } ?: free.firstOrNull { i ->
                    kw in headerWords[i] || (kw.contains('/') && header[i].contains(kw)) ||
                        // German compounds: "Buchungsdatum", "Transaktionsdatum"
                        (column == CsvColumn.DATE && kw == "datum" && headerWords[i].any { it.endsWith("datum") })
                }
            }
            if (idx != null) byHeader[idx] = column
        }
        val data = table.drop(1)
        val hasHeader = byHeader.containsValue(CsvColumn.DATE) && CsvDate.detect(header.take(1)) == null
        val mapping = if (hasHeader) byHeader else guessFromData(table)
        val rows = if (hasHeader) data else table
        val dateIdx = mapping.entries.firstOrNull { it.value == CsvColumn.DATE }?.key
        return CsvMapping(mapping, hasHeader, dateIdx?.let { i -> CsvDate.detect(rows.mapNotNull { it.getOrNull(i) }) })
    }

    /** No header: the first column where every value is a date, then the first where every value is a number. */
    private fun guessFromData(table: List<List<String>>): Map<Int, CsvColumn> {
        val width = table.maxOfOrNull { it.size } ?: 0
        fun col(i: Int) = table.mapNotNull { it.getOrNull(i) }.filter { it.isNotBlank() }
        val date = (0 until width).firstOrNull { CsvDate.detect(col(it)) != null }
        val amount = (0 until width).firstOrNull { i -> i != date && col(i).isNotEmpty() && col(i).all { CsvAmount.parse(it, 2) != null } }
        val note = (0 until width).firstOrNull { it != date && it != amount }
        return buildMap {
            date?.let { put(it, CsvColumn.DATE) }
            amount?.let { put(it, CsvColumn.AMOUNT) }
            note?.let { put(it, CsvColumn.NOTE) }
        }
    }

    /** [lines]: the text-editor line of each table row, from [CsvReader.readWithLines]. */
    fun preview(table: List<List<String>>, m: CsvMapping, fractionDigits: Int, lines: List<Int>? = null): CsvPreview {
        val digits = if (m.pennyExport) 2 else fractionDigits
        val rows = mutableListOf<CsvRow>()
        val errors = mutableListOf<CsvRowError>()
        val start = if (m.hasHeader) 1 else 0
        for (i in start until table.size) {
            val r = table[i]
            val line = lines?.getOrNull(i) ?: (i + 1)
            fun cell(c: CsvColumn) = m.indexOf(c)?.let { r.getOrNull(it) }?.trim().orEmpty()
            val date = m.datePattern?.let { CsvDate.parse(cell(CsvColumn.DATE), it) }
            if (date == null) { errors += CsvRowError(line, CsvRowError.Reason.DATE); continue }
            val typeText = cell(CsvColumn.TYPE).lowercase()
            val typeIncome: Boolean? = when {
                typeText.isEmpty() -> null
                m.pennyTypes -> when (typeText.uppercase()) { TxType.INCOME.name -> true; TxType.EXPENSE.name -> false; else -> null }
                INCOME_WORDS.any { typeText.startsWith(it) } -> true
                EXPENSE_WORDS.any { typeText.startsWith(it) } -> false
                else -> null
            }
            // Penny's own other types (transfers, savings) are reported; a bank's own codes (CARD_PAYMENT) fall back to the sign.
            if (m.pennyTypes && typeText.isNotEmpty() && typeIncome == null) { errors += CsvRowError(line, CsvRowError.Reason.NOT_EXPENSE_OR_INCOME); continue }
            val signed: Long? = if (m.indexOf(CsvColumn.DEBIT) != null || m.indexOf(CsvColumn.CREDIT) != null) {
                val debit = cell(CsvColumn.DEBIT).takeIf { it.isNotEmpty() }?.let { CsvAmount.parse(it, digits) }
                val credit = cell(CsvColumn.CREDIT).takeIf { it.isNotEmpty() }?.let { CsvAmount.parse(it, digits) }
                when {
                    debit != null && debit != 0L -> -kotlin.math.abs(debit)
                    credit != null && credit != 0L -> kotlin.math.abs(credit)
                    debit != null || credit != null -> 0L
                    else -> null
                }
            } else {
                val fileDigits = m.currencyColumn?.let { r.getOrNull(it) }?.let(::currencyDigits)
                if (fileDigits == null || fileDigits == digits) CsvAmount.parse(cell(CsvColumn.AMOUNT), digits)
                else CsvAmount.parse(cell(CsvColumn.AMOUNT), fileDigits)?.let { rescale(it, fileDigits, digits) }
            }
            if (signed == null) { errors += CsvRowError(line, CsvRowError.Reason.AMOUNT); continue }
            if (signed == 0L || signed == Long.MIN_VALUE) { errors += CsvRowError(line, CsvRowError.Reason.ZERO); continue }
            val income = typeIncome ?: (signed > 0)
            rows += CsvRow(line, date, kotlin.math.abs(signed), income, cell(CsvColumn.CATEGORY).ifEmpty { null }, cell(CsvColumn.NOTE).ifEmpty { null })
        }
        return CsvPreview(rows, errors)
    }

    /**
     * Lines that look already recorded in the account: same date, kind and amount, and the same note when both have one.
     * Each existing entry accounts for one line only, so two equal coffees in the file against one in the app leave one.
     */
    fun duplicates(rows: List<CsvRow>, existing: List<Transaction>, accountId: String): Set<Int> {
        val inAccount = existing.filter { it.accountId == accountId && (it.type == TxType.EXPENSE || it.type == TxType.INCOME) }
            .groupBy { Triple(it.date, it.type == TxType.INCOME, it.amountCents) }
            .mapValues { it.value.toMutableList() }
        fun sameNote(t: Transaction, r: CsvRow) = t.note.isNullOrBlank() || r.note.isNullOrBlank() || t.note.trim().equals(r.note.trim(), ignoreCase = true)
        val dup = mutableSetOf<Int>()
        // Exact note matches first, so a noted entry isn't used up by a line without a note.
        for (exactFirst in listOf(true, false)) for (r in rows) {
            if (r.line in dup) continue
            val pool = inAccount[Triple(r.date, r.income, r.amountCents)] ?: continue
            val hit = pool.firstOrNull { t -> if (exactFirst) !t.note.isNullOrBlank() && !r.note.isNullOrBlank() && sameNote(t, r) else sameNote(t, r) } ?: continue
            pool.remove(hit)
            dup += r.line
        }
        return dup
    }

    private fun currencyDigits(code: String): Int? =
        runCatching { java.util.Currency.getInstance(code.trim().uppercase()).defaultFractionDigits }.getOrNull()?.takeIf { it >= 0 }

    private fun rescale(minor: Long, from: Int, to: Int): Long? = runCatching {
        java.math.BigDecimal.valueOf(minor).movePointLeft(from).setScale(to, java.math.RoundingMode.HALF_UP).movePointRight(to).longValueExact()
    }.getOrNull()
}
