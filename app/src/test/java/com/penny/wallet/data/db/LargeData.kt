package com.penny.wallet.data.db

import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.time.LocalDate
import kotlin.random.Random

/** A heavy user: ~8 years of entries, deterministic so timings are comparable between runs. */
class LargeData(count: Int = 20_000, today: LocalDate = FIXED_TODAY, seed: Int = 1) {
    val accounts = (0 until 6).map { Account("acc$it", "Account $it", "bank", archived = it == 5, sortOrder = it, createdAt = it.toLong()) }
    val categories = (0 until 25).map { Category("cat$it", "Category $it", "other", 0xFF8E8E93, sortOrder = it) }
    val goals = (0 until 5).map { Goal("goal$it", "Goal $it", "star", 0xFF0A84FF, 500_000, today.plusMonths(it + 1L), sortOrder = it, createdAt = it.toLong()) }
    val recurring = (0 until 15).map {
        RecurringExpense("rec$it", "Bill $it", 1_000L + it * 500, "cat${it % 25}", "acc${it % 5}", Frequency.MONTHLY,
            dayOfMonth = 1 + it * 2, month = null, dayOfWeek = null, nextDueDate = today.plusDays(it.toLong() - 5),
            remindDaysBefore = 1, autoPay = it % 2 == 0, active = true)
    }
    private val notes = listOf("Coffee", "Supermarket", "Fuel", "Café Müller", "Train", "Dinner with friends", null, null)

    val transactions: List<Transaction> = Random(seed).let { rnd ->
        (0 until count).map { i ->
            val type = when (rnd.nextInt(100)) {
                in 0..79 -> TxType.EXPENSE
                in 80..89 -> TxType.INCOME
                in 90..94 -> TxType.TRANSFER
                in 95..97 -> TxType.SAVE
                else -> TxType.ADJUSTMENT
            }
            val acc = "acc${rnd.nextInt(5)}"
            Transaction(
                id = "tx$i", type = type, amountCents = if (type == TxType.INCOME) 200_000 else 100L + rnd.nextLong(20_000),
                accountId = acc,
                toAccountId = if (type == TxType.TRANSFER) "acc${(acc.last().digitToInt() + 1) % 5}" else null,
                categoryId = if (type == TxType.EXPENSE) "cat${rnd.nextInt(25)}" else null,
                goalId = if (type == TxType.SAVE) "goal${rnd.nextInt(5)}" else null,
                recurringId = if (type == TxType.EXPENSE && rnd.nextInt(20) == 0) "rec${rnd.nextInt(15)}" else null,
                date = today.minusDays(rnd.nextLong(365L * 8)), note = notes[rnd.nextInt(notes.size)], createdAt = i.toLong(),
            )
        }.sortedWith(compareByDescending<Transaction> { it.date }.thenByDescending { it.createdAt })
    }

    suspend fun insertInto(db: PennyDatabase) {
        db.accounts().upsertAll(accounts)
        db.categories().upsertAll(categories)
        db.goals().upsertAll(goals)
        db.recurring().upsertAll(recurring)
        db.transactions().upsertAll(transactions)
    }
}
