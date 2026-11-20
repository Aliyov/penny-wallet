package com.penny.wallet.domain

import com.penny.wallet.data.prefs.NumberFormatStyle
import com.penny.wallet.data.prefs.UserPrefs
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/** The only place money becomes text. EUR only. */
object Formatters {
    const val HIDDEN = "••••"

    data class MoneyFormat(val showCents: Boolean = true, val style: NumberFormatStyle = NumberFormatStyle.EU, val symbolBefore: Boolean = false)

    fun of(p: UserPrefs) = MoneyFormat(p.showCents, p.numberFormat, p.symbolBefore)

    fun money(amount: Long, f: MoneyFormat = MoneyFormat(), signed: Boolean = false, forceCents: Boolean = false): String {
        val neg = amount < 0
        val a = abs(amount)
        val (group, dec) = if (f.style == NumberFormatStyle.EU) '.' to ',' else ',' to '.'
        val cents = f.showCents || forceCents
        val euros = if (cents) a / 100 else (a + 50) / 100 // round when cents are hidden
        val whole = euros.toString().reversed().chunked(3).joinToString(group.toString()).reversed()
        val num = if (cents) "$whole$dec${(a % 100).toString().padStart(2, '0')}" else whole
        val body = if (f.symbolBefore) "€$num" else "$num €"
        val sign = when { neg && a > 0 -> "−"; signed && !neg && a > 0 -> "+"; else -> "" }
        return sign + body
    }

    /** Digits typed on the numpad ("1234" = 12,34 €) → cents. */
    fun digitsToCents(digits: String): Long = digits.toLongOrNull() ?: 0L

    /** Parses user text like "1.234,56", "1234.5", "12" into cents; null if invalid. */
    fun parseCents(text: String): Long? {
        val t = text.trim().replace("€", "").replace(" ", "").replace("−", "-")
        if (t.isEmpty()) return null
        val lastSep = maxOf(t.lastIndexOf(','), t.lastIndexOf('.'))
        val (intPart, frac) = if (lastSep >= 0 && t.length - lastSep - 1 in 1..2) {
            t.substring(0, lastSep) to t.substring(lastSep + 1)
        } else t to ""
        val i = intPart.replace(".", "").replace(",", "")
        val neg = i.startsWith("-")
        val digits = i.removePrefix("-").ifEmpty { "0" }
        if (!digits.all(Char::isDigit) || !frac.all(Char::isDigit)) return null
        val cents = digits.toLong() * 100 + frac.padEnd(2, '0').toLong()
        return if (neg) -cents else cents
    }

    private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    private val shortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val fullFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    fun day(d: LocalDate, today: LocalDate = LocalDate.now()): String = when (d) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> if (d.year == today.year) dayFmt.format(d) else fullFmt.format(d)
    }

    fun shortDate(d: LocalDate): String = shortFmt.format(d)
    fun fullDate(d: LocalDate): String = fullFmt.format(d)

    fun month(ym: YearMonth): String = "${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${ym.year}"
    fun monthShort(ym: YearMonth): String = ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

    fun percent(share: Float): String = "${(share * 100).let { if (it in 0.01f..0.99f) "<1" else Math.round(it).toString() }}%"

    fun relativeDays(fromMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val days = (nowMillis - fromMillis) / 86_400_000L
        return when { days <= 0 -> "today"; days == 1L -> "yesterday"; else -> "$days days ago" }
    }
}
