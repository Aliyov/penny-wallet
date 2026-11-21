package com.penny.wallet.core.money

import com.penny.wallet.data.prefs.NumberFormatStyle
import com.penny.wallet.data.prefs.SymbolPosition
import com.penny.wallet.data.prefs.UserPrefs
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

/** The only place money becomes text. Amounts are minor units of the user's single currency. */
object Formatters {
    const val HIDDEN = "••••"
    const val DEFAULT_CURRENCY = "EUR"

    data class MoneyFormat(
        val currencyCode: String = DEFAULT_CURRENCY,
        val showCents: Boolean = true,
        val style: NumberFormatStyle = NumberFormatStyle.AUTO,
        val symbolPosition: SymbolPosition = SymbolPosition.AUTO,
        val locale: Locale = Locale.ROOT,
    )

    fun of(p: UserPrefs, locale: Locale = Locale.getDefault()) = MoneyFormat(
        currencyCode = p.currencyCode.ifEmpty { DEFAULT_CURRENCY },
        showCents = p.showCents, style = p.numberFormat, symbolPosition = p.symbolPosition, locale = locale,
    )

    fun currency(code: String): Currency =
        runCatching { Currency.getInstance(code) }.getOrElse { Currency.getInstance(DEFAULT_CURRENCY) }

    /** EUR 2, JPY 0, BHD 3; pseudo-currencies without minor units count as 2. */
    fun fractionDigits(code: String): Int = currency(code).defaultFractionDigits.let { if (it < 0) 2 else it }

    /** The currency of the device region, or EUR when the region has none. */
    fun suggestedCurrency(locale: Locale): String =
        runCatching { Currency.getInstance(locale).currencyCode }.getOrNull() ?: DEFAULT_CURRENCY

    fun symbol(code: String, locale: Locale): String = currency(code).getSymbol(locale)

    fun money(amount: Long, f: MoneyFormat = MoneyFormat(), signed: Boolean = false, forceCents: Boolean = false): String {
        val digits = fractionDigits(f.currencyCode)
        val unit = pow10(digits)
        val a = abs(amount)
        val (group, dec) = separators(f)
        val showFraction = digits > 0 && (f.showCents || forceCents)
        val major = if (showFraction) a / unit else (a + unit / 2) / unit
        val whole = major.toString().reversed().chunked(3).joinToString(group.toString()).reversed()
        val num = if (showFraction) "$whole$dec${(a % unit).toString().padStart(digits, '0')}" else whole
        val sym = symbol(f.currencyCode, f.locale)
        val body = if (symbolBefore(f)) sym + (if (sym.last().isLetter()) " " else "") + num else "$num $sym"
        val sign = when { amount < 0 -> "−"; signed && amount > 0 -> "+"; else -> "" }
        return sign + body
    }

    /** Digits typed on the numpad are minor units ("1234" = 12,34 € or 1234 ¥). */
    fun digitsToCents(digits: String): Long = digits.toLongOrNull() ?: 0L

    /**
     * Parses user text like "1.234,56", "1234.5", "€ 12" into minor units; null if invalid. The last separator
     * is the decimal one when at most [fractionDigits] digits follow it, so both EU and US input work.
     */
    fun parseCents(text: String, fractionDigits: Int = 2): Long? {
        val t = text.trim().replace('−', '-').filter { it.isDigit() || it == ',' || it == '.' || it == '-' }
        if (t.isEmpty() || t.none(Char::isDigit)) return null
        val neg = t.startsWith("-")
        val body = t.removePrefix("-")
        if ('-' in body) return null
        val lastSep = maxOf(body.lastIndexOf(','), body.lastIndexOf('.'))
        val fracLen = body.length - lastSep - 1
        val (intPart, frac) = if (lastSep >= 0 && fractionDigits > 0 && fracLen in 1..fractionDigits) {
            body.substring(0, lastSep) to body.substring(lastSep + 1)
        } else body to ""
        val intDigits = intPart.filter(Char::isDigit).ifEmpty { "0" }
        if (intDigits.length > 15) return null
        val minor = intDigits.toLong() * pow10(fractionDigits) + frac.padEnd(fractionDigits, '0').ifEmpty { "0" }.toLong()
        return if (neg) -minor else minor
    }

    fun percent(share: Float, locale: Locale = Locale.ROOT): String {
        val pf = NumberFormat.getPercentInstance(locale)
        val p = share * 100
        return if (p in 0.01f..0.99f) "<" + pf.format(0.01) else pf.format(Math.round(p) / 100.0)
    }

    private fun separators(f: MoneyFormat): Pair<Char, Char> = when (f.style) {
        NumberFormatStyle.EU -> '.' to ','
        NumberFormatStyle.US -> ',' to '.'
        NumberFormatStyle.AUTO -> DecimalFormatSymbols.getInstance(f.locale).let { it.groupingSeparator to it.decimalSeparator }
    }

    private fun symbolBefore(f: MoneyFormat): Boolean = when (f.symbolPosition) {
        SymbolPosition.BEFORE -> true
        SymbolPosition.AFTER -> false
        SymbolPosition.AUTO -> (NumberFormat.getCurrencyInstance(f.locale) as? DecimalFormat)
            ?.toPattern()?.trimStart()?.startsWith('¤') ?: false
    }

    private fun pow10(n: Int): Long { var r = 1L; repeat(n) { r *= 10 }; return r }
}
