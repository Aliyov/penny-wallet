package com.penny.wallet.domain.csv

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Amounts as people and banks write them: "1.234,56", "1,234.56", "-12,5", "(12.50)", "€ 9,99", "12.50 EUR". */
object CsvAmount {
    private val KEEP = Regex("[^0-9.,\\-+()]")

    /** Minor units (cents, or whole yen for 0-digit currencies); null when the text isn't a number. */
    fun parse(raw: String, fractionDigits: Int): Long? {
        var s = raw.replace('−', '-').replace(KEEP, "")
        if (s.isEmpty()) return null
        var negative = false
        if (s.startsWith("(") && s.endsWith(")")) { negative = true; s = s.substring(1, s.length - 1) }
        if (s.startsWith("-")) { negative = !negative; s = s.drop(1) } else if (s.startsWith("+")) s = s.drop(1)
        if (s.endsWith("-")) { negative = !negative; s = s.dropLast(1) }
        if (s.isEmpty() || s.any { it == '-' || it == '+' || it == '(' || it == ')' }) return null
        val normalized = normalizeSeparators(s, fractionDigits) ?: return null
        val value = runCatching { BigDecimal(normalized) }.getOrNull() ?: return null
        val minor = value.setScale(fractionDigits, RoundingMode.HALF_UP).movePointRight(fractionDigits)
        val cents = runCatching { minor.longValueExact() }.getOrNull() ?: return null
        return if (negative) -cents else cents
    }

    private fun normalizeSeparators(s: String, fractionDigits: Int): String? {
        val dots = s.count { it == '.' }
        val commas = s.count { it == ',' }
        val decimal: Char? = when {
            dots > 0 && commas > 0 -> if (s.lastIndexOf('.') > s.lastIndexOf(',')) '.' else ','
            dots + commas == 0 -> null
            dots > 1 || commas > 1 -> null // only thousands separators
            else -> {
                val sep = if (dots == 1) '.' else ','
                val after = s.length - s.indexOf(sep) - 1
                // "1.234" is a thousand unless the currency has three decimals
                if (after == 3 && fractionDigits != 3) null else sep
            }
        }
        if (decimal != null && s.count { it == decimal } != 1) return null
        val thousands = if (decimal == null) s else s.substring(0, s.lastIndexOf(decimal))
        val fraction = if (decimal == null) "" else s.substring(s.lastIndexOf(decimal) + 1)
        // Grouped digits must be real thousands groups: "1.234.567", not "1.2.3"
        val groups = thousands.split('.', ',')
        if (groups.size > 1 && groups.drop(1).any { it.length != 3 }) return null
        val whole = thousands.filter { it.isDigit() }
        if (fraction.any { !it.isDigit() }) return null
        if (whole.isEmpty() && fraction.isEmpty()) return null
        return (whole.ifEmpty { "0" }) + if (fraction.isEmpty()) "" else ".$fraction"
    }
}

/** Dates in the common export formats; the format is chosen once per file so 03/04 is read the same way in every row. */
object CsvDate {
    private val PATTERNS = listOf(
        "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd", "dd.MM.yyyy", "d.M.yyyy", "dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy",
        "MM/dd/yyyy", "M/d/yyyy", "dd.MM.yy", "d.M.yy", "dd/MM/yy", "MM/dd/yy", "yyyyMMdd",
    ).map { it to DateTimeFormatter.ofPattern(it.replace("yyyy", "uuuu").replace("yy", "uu")).withResolverStyle(ResolverStyle.STRICT) }

    /** Drops a time part ("2026-11-24 13:05:00", "2026-11-24T13:05"). */
    private fun datePart(s: String): String = s.trim().substringBefore('T').substringBefore(' ')

    fun parse(raw: String, pattern: String): LocalDate? {
        val f = PATTERNS.firstOrNull { it.first == pattern }?.second ?: return null
        return runCatching { LocalDate.parse(datePart(raw), f) }.getOrNull()
    }

    /**
     * The pattern that reads the most samples, so one broken line doesn't hide the format of all the others;
     * on a tie the earlier pattern wins, so day-first beats month-first when both fit.
     */
    fun detect(samples: List<String>): String? {
        val values = samples.map(::datePart).filter { it.isNotEmpty() }
        if (values.isEmpty()) return null
        val (pattern, count) = PATTERNS.map { (p, f) -> p to values.count { v -> runCatching { LocalDate.parse(v, f) }.isSuccess } }
            .maxWith(compareBy<Pair<String, Int>> { it.second }.thenByDescending { p -> PATTERNS.indexOfFirst { it.first == p.first } })
        return pattern.takeIf { count > 0 }
    }
}
