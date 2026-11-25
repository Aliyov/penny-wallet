package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A run of manual expenses that looks like a monthly bill the user hasn't set up yet. */
data class BillSuggestion(
    /** Stable for the same note + category, so a dismissal sticks. */
    val key: String,
    val name: String,
    val categoryId: String,
    val accountId: String,
    /** The latest amount: bills change price, the newest one is what comes next. */
    val amountCents: Long,
    val dayOfMonth: Int,
    val occurrences: Int,
    val lastDate: LocalDate,
    /** The payments the suggestion was built from; adding the bill links them to it. */
    val transactionIds: List<String> = emptyList(),
)

object BillDetector {
    const val MIN_OCCURRENCES = 3
    private val GAP_DAYS = 25L..35L
    /** A bill that stopped (cancelled subscription) shouldn't be suggested any more. */
    private const val MAX_DAYS_SINCE_LAST = 45L
    private const val AMOUNT_TOLERANCE_PERCENT = 10L
    /** Small bills (a 2.99 app) may move by a few cents more than 10 %. */
    private const val MIN_AMOUNT_TOLERANCE = 100L

    private val SPACES = Regex("\\s+")

    fun normalize(note: String): String = note.trim().lowercase().replace(SPACES, " ")

    fun key(note: String, categoryId: String): String = normalize(note) + "|" + categoryId

    /**
     * Newest first. Only expenses with a note and a category count; one already tied to a bill never does.
     * [activeAccounts] / [activeCategories] (null = all) keep archived ones out of a new bill.
     */
    fun suggestions(
        txs: List<Transaction>, rules: List<RecurringExpense>, dismissed: Set<String>, today: LocalDate,
        activeAccounts: Set<String>? = null, activeCategories: Set<String>? = null,
    ): List<BillSuggestion> {
        val existing = rules.map { key(it.name, it.categoryId) }.toSet()
        return txs.asSequence()
            .filter { it.type == TxType.EXPENSE && it.recurringId == null && it.splitId == null && it.amountCents > 0 && !it.date.isAfter(today) }
            .filter { it.categoryId != null && !it.note.isNullOrBlank() }
            .filter { (activeAccounts == null || it.accountId in activeAccounts) && (activeCategories == null || it.categoryId in activeCategories) }
            .groupBy { key(it.note!!, it.categoryId!!) }
            .filterKeys { it !in existing && it !in dismissed }
            .mapNotNull { (k, group) -> fromGroup(k, group, today) }
            .sortedByDescending { it.lastDate }
    }

    private fun fromGroup(key: String, group: List<Transaction>, today: LocalDate): BillSuggestion? {
        val byDate = group.sortedWith(compareBy<Transaction> { it.date }.thenBy { it.createdAt })
        val last = byDate.last()
        if (ChronoUnit.DAYS.between(last.date, today) > MAX_DAYS_SINCE_LAST) return null
        // The newest unbroken monthly run; older history with another rhythm doesn't matter.
        val run = mutableListOf(last)
        for (i in byDate.lastIndex - 1 downTo 0) {
            if (ChronoUnit.DAYS.between(byDate[i].date, run.last().date) !in GAP_DAYS) break
            run += byDate[i]
        }
        if (run.size < MIN_OCCURRENCES) return null
        val amounts = run.map { it.amountCents }.sorted()
        val median = amounts[amounts.size / 2]
        val tolerance = maxOf(median / 100 * AMOUNT_TOLERANCE_PERCENT, MIN_AMOUNT_TOLERANCE)
        if (amounts.any { kotlin.math.abs(it - median) > tolerance }) return null
        // Most common day; on a tie the newest payment's day, which is closest to the current rhythm.
        val counts = run.groupingBy { it.date.dayOfMonth }.eachCount()
        val day = run.first { counts.getValue(it.date.dayOfMonth) == counts.values.max() }.date.dayOfMonth
        return BillSuggestion(key, last.note!!.trim(), last.categoryId!!, last.accountId, last.amountCents, day, run.size, last.date, run.map { it.id })
    }
}
