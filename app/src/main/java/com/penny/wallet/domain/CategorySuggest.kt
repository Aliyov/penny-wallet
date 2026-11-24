package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.min

/** Category guesses learned from the user's own past expenses. Nothing leaves the phone. */
object CategorySuggest {
    private const val LOOKBACK_DAYS = 90L
    private const val MIN_HISTORY = 8
    private const val MIN_VOTES = 3
    private const val MIN_SHARE = 0.4

    /** Category used most for past expenses whose note names the same shop, e.g. "Lidl" for "LIDL Berlin". */
    fun forMerchant(merchant: String, txs: List<Transaction>): String? {
        val key = normalize(merchant)
        if (key.length < 3) return null
        return txs.asSequence()
            .filter { it.type == TxType.EXPENSE && it.categoryId != null }
            .filter { t ->
                val n = t.note?.let(::normalize) ?: return@filter false
                n.length >= 3 && (n == key || n.startsWith("$key ") || key.startsWith("$n "))
            }
            .groupBy { it.categoryId!! }
            .maxWithOrNull(compareBy<Map.Entry<String, List<Transaction>>> { it.value.size }.thenBy { e -> e.value.maxOf { it.createdAt } })
            ?.key
    }

    /**
     * Category the user tends to pick at this time of day (weekday vs weekend), or null when the habit isn't clear.
     * Uses when expenses were entered ([Transaction.createdAt]), since the booking date has no time.
     */
    fun forTime(txs: List<Transaction>, now: LocalDateTime, zone: ZoneId): String? {
        val since = now.minusDays(LOOKBACK_DAYS).atZone(zone).toInstant().toEpochMilli()
        val recent = txs.filter { it.type == TxType.EXPENSE && it.categoryId != null && it.createdAt >= since }
        if (recent.size < MIN_HISTORY) return null
        val weekend = now.dayOfWeek.isWeekend()
        val scores = mutableMapOf<String, Int>()
        val votes = mutableMapOf<String, Int>()
        for (t in recent) {
            val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(t.createdAt), zone)
            val hours = abs(at.hour - now.hour).let { min(it, 24 - it) }
            var w = when { hours <= 1 -> 2; hours <= 2 -> 1; else -> 0 }
            if (w == 0) continue
            if (at.dayOfWeek.isWeekend() == weekend) w += 1
            scores[t.categoryId!!] = (scores[t.categoryId] ?: 0) + w
            votes[t.categoryId] = (votes[t.categoryId] ?: 0) + 1
        }
        val best = scores.maxByOrNull { it.value } ?: return null
        val total = scores.values.sum()
        return best.key.takeIf { (votes[it] ?: 0) >= MIN_VOTES && best.value >= total * MIN_SHARE }
    }

    internal fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    private fun DayOfWeek.isWeekend() = this == DayOfWeek.SATURDAY || this == DayOfWeek.SUNDAY
}
