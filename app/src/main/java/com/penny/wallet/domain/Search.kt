package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.text.Normalizer
import java.time.LocalDate

enum class SearchKind { ALL, EXPENSES, INCOME }

data class SearchFilter(
    val query: String = "",
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    /** Bounds on the amount as shown (always positive), in minor units. */
    val minCents: Long? = null,
    val maxCents: Long? = null,
    val kind: SearchKind = SearchKind.ALL,
) {
    val isEmpty: Boolean get() = query.isBlank() && from == null && to == null && minCents == null && maxCents == null && kind == SearchKind.ALL
}

data class SearchResult(val rows: List<Transaction>, val spent: Long, val received: Long)

/** All-time search over every entry, not just one month. */
object Search {
    private val MARKS = Regex("\\p{Mn}+")

    // Letters that NFD doesn't split into base letter + accent.
    private val LETTERS = mapOf('ß' to "ss", 'æ' to "ae", 'œ' to "oe", 'ø' to "o", 'ł' to "l", 'đ' to "d", 'ı' to "i")

    /** Lower case without accents, so "cafe" finds "Café", "strasse" finds "Straße", "lodz" finds "Łódź". */
    fun fold(s: String): String {
        val stripped = MARKS.replace(Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD), "")
        return if (stripped.none { it in LETTERS }) stripped else buildString { stripped.forEach { ch -> append(LETTERS[ch] ?: ch) } }
    }

    /** [typeLabels]: shown titles of rows without a note ("Transfer"), so they can be found by that word too. */
    fun run(
        txs: List<Transaction>, accounts: List<Account>, categories: List<Category>, goals: List<Goal>, f: SearchFilter,
        typeLabels: Map<TxType, String> = emptyMap(),
    ): SearchResult {
        val types = typeLabels.mapValues { fold(it.value) }
        val words = fold(f.query).split(Regex("\\s+")).filter { it.isNotEmpty() }
        val acc = accounts.associate { it.id to fold(it.name) }
        val cat = categories.associate { it.id to fold(it.name) }
        val goal = goals.associate { it.id to fold(it.name) }
        val noteCache = HashMap<String, String>()
        val rows = txs.filter { t ->
            val amount = kotlin.math.abs(t.amountCents)
            when (f.kind) {
                SearchKind.ALL -> true
                SearchKind.EXPENSES -> t.type == TxType.EXPENSE
                SearchKind.INCOME -> t.type == TxType.INCOME
            } &&
                (f.from == null || !t.date.isBefore(f.from)) && (f.to == null || !t.date.isAfter(f.to)) &&
                (f.minCents == null || amount >= f.minCents) && (f.maxCents == null || amount <= f.maxCents) &&
                (words.isEmpty() || run {
                    val haystack = listOfNotNull(
                        t.note?.let { n -> noteCache.getOrPut(n) { fold(n) } },
                        t.categoryId?.let(cat::get), acc[t.accountId], t.toAccountId?.let(acc::get), t.goalId?.let(goal::get), types[t.type],
                    )
                    words.all { w -> haystack.any { it.contains(w) } }
                })
        }.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt })
        return SearchResult(
            rows,
            spent = rows.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents },
            received = rows.filter { it.type == TxType.INCOME }.sumOf { it.amountCents },
        )
    }
}
