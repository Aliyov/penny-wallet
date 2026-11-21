package com.penny.wallet.core.util

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.core.os.ConfigurationCompat
import com.penny.wallet.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Locale-aware date text; relative words ("Today") come from string resources. */
class DateText(private val context: Context, val locale: Locale) {
    private fun pattern(skeleton: String) = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    private val dayFmt by lazy { pattern("EEEdMMM") }
    private val shortFmt by lazy { pattern("dMMM") }
    private val fullFmt by lazy { pattern("dMMMyyyy") }
    private val monthFmt by lazy { pattern("MMMMyyyy") }

    fun day(d: LocalDate, today: LocalDate = LocalDate.now()): String = when (d) {
        today -> context.getString(R.string.date_today)
        today.minusDays(1) -> context.getString(R.string.date_yesterday)
        today.plusDays(1) -> context.getString(R.string.date_tomorrow)
        else -> if (d.year == today.year) dayFmt.format(d) else fullFmt.format(d)
    }

    fun shortDate(d: LocalDate): String = shortFmt.format(d)
    fun fullDate(d: LocalDate): String = fullFmt.format(d)
    fun month(ym: YearMonth): String = monthFmt.format(ym.atDay(1)).replaceFirstChar { it.titlecase(locale) }
    fun monthShort(ym: YearMonth): String = ym.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
    fun weekday(dayOfWeek: java.time.DayOfWeek): String = dayOfWeek.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }

    fun relativeDays(fromMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val days = ((nowMillis - fromMillis) / 86_400_000L).toInt()
        return when {
            days <= 0 -> context.getString(R.string.date_today_lower)
            days == 1 -> context.getString(R.string.date_yesterday_lower)
            else -> context.resources.getQuantityString(R.plurals.date_days_ago, days, days)
        }
    }

    companion object {
        fun of(context: Context): DateText =
            DateText(context, ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault())
    }
}

@Composable
@ReadOnlyComposable
fun dateText(): DateText {
    LocalConfiguration.current
    return DateText.of(LocalContext.current)
}

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalLocale.current.platformLocale
