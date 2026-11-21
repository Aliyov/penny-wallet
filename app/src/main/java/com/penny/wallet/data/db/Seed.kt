package com.penny.wallet.data.db

import androidx.room.withTransaction
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import com.penny.wallet.core.ui.theme.ItemPalette
import java.time.LocalDate
import java.util.Random
import java.util.UUID

/** Demo data: 2 accounts, 6 categories, 2 goals, ~3 months of transactions, 2 recurring rules. */
object Seed {

    private fun id() = UUID.randomUUID().toString()

    /** Next date with [day] (clamped to month length) on or after [from]. */
    fun nextMonthlyOn(day: Int, from: LocalDate): LocalDate {
        val thisMonth = from.withDayOfMonth(minOf(day, from.lengthOfMonth()))
        if (!thisMonth.isBefore(from)) return thisMonth
        val next = from.plusMonths(1).withDayOfMonth(1)
        return next.withDayOfMonth(minOf(day, next.lengthOfMonth()))
    }

    private data class Draft(
        val type: TxType,
        val amount: Long,
        val account: String,
        val date: LocalDate,
        val to: String? = null,
        val category: String? = null,
        val goal: String? = null,
        val recurring: String? = null,
        val note: String? = null,
    )

    suspend fun run(db: PennyDatabase, today: LocalDate = LocalDate.now()) {
        val now = System.currentTimeMillis()
        val rnd = Random(42)

        val bank = Account(id(), "Bank", "bank", sortOrder = 0, createdAt = now)
        val cash = Account(id(), "Cash", "cash", sortOrder = 1, createdAt = now + 1)

        fun cat(name: String, icon: String, colorIdx: Int, order: Int) =
            Category(id(), name, icon, ItemPalette[colorIdx], sortOrder = order)
        val groceries = cat("Groceries", "groceries", 0, 0)
        val rent = cat("Rent", "rent", 1, 1)
        val restaurants = cat("Restaurants", "restaurant", 4, 2)
        val transport = cat("Transport", "bus", 5, 3)
        val subs = cat("Subscriptions", "subscription", 2, 4)
        val fun_ = cat("Fun", "game", 3, 5)

        val phoneGoal = Goal(
            id(), "New phone", "phone", ItemPalette[7], targetCents = 900_00,
            deadline = today.plusMonths(6), sortOrder = 0, createdAt = now,
        )
        val vacationGoal = Goal(
            id(), "Vacation", "beach", ItemPalette[8], targetCents = 2_000_00,
            deadline = null, sortOrder = 1, createdAt = now + 1,
        )

        val phoneCredit = RecurringExpense(
            id(), "Phone credit", 10_00, subs.id, bank.id, Frequency.MONTHLY,
            dayOfMonth = 14, nextDueDate = nextMonthlyOn(14, today), autoPay = false,
        )
        val netflix = RecurringExpense(
            id(), "Netflix", 20_00, subs.id, bank.id, Frequency.MONTHLY,
            dayOfMonth = 22, nextDueDate = nextMonthlyOn(22, today), autoPay = true,
        )

        // Small expenses: (category, account, min cents, max cents, notes)
        data class Spend(val cat: Category, val acc: Account, val min: Long, val max: Long, val notes: List<String>)
        val spends = listOf(
            Spend(groceries, bank, 15_00, 85_00, listOf("Supermarket", "Weekly shop", "Market")),
            Spend(groceries, cash, 3_00, 18_00, listOf("Bakery", "Corner shop")),
            Spend(restaurants, cash, 8_00, 35_00, listOf("Lunch", "Coffee & cake", "Kebab")),
            Spend(restaurants, bank, 20_00, 60_00, listOf("Dinner out", "Sushi")),
            Spend(transport, bank, 2_00, 40_00, listOf("Metro", "Fuel", "Train ticket")),
            Spend(fun_, bank, 10_00, 45_00, listOf("Cinema", "Concert", "Board game")),
        )

        val drafts = mutableListOf<Draft>()
        val firstMonth = today.withDayOfMonth(1).minusMonths(2)
        drafts += Draft(TxType.INCOME, 300_00, cash.id, firstMonth, note = "Starting cash")

        for (m in 0..2) {
            val monthStart = firstMonth.plusMonths(m.toLong())
            val isCurrent = m == 2
            val lastDay = if (isCurrent) today.dayOfMonth else monthStart.lengthOfMonth()
            fun dayOf(d: Int): LocalDate? = monthStart.withDayOfMonth(minOf(d, monthStart.lengthOfMonth()))
                .takeIf { !it.isAfter(today) }

            dayOf(1)?.let { drafts += Draft(TxType.INCOME, 2_200_00, bank.id, it, note = "Salary") }
            dayOf(2)?.let { drafts += Draft(TxType.EXPENSE, 750_00, bank.id, it, category = rent.id, note = "Rent") }
            dayOf(14)?.let {
                drafts += Draft(TxType.EXPENSE, 10_00, bank.id, it, category = subs.id, recurring = phoneCredit.id, note = "Phone credit")
            }
            dayOf(22)?.let {
                drafts += Draft(TxType.EXPENSE, 20_00, bank.id, it, category = subs.id, recurring = netflix.id, note = "Netflix")
            }
            repeat(9) {
                val s = spends[rnd.nextInt(spends.size)]
                val amount = s.min + (rnd.nextDouble() * (s.max - s.min)).toLong()
                val day = 1 + rnd.nextInt(lastDay)
                drafts += Draft(
                    TxType.EXPENSE, amount, s.acc.id, monthStart.withDayOfMonth(day),
                    category = s.cat.id, note = s.notes[rnd.nextInt(s.notes.size)],
                )
            }
        }

        // One transfer and goal savings in the first (complete) month.
        drafts += Draft(TxType.TRANSFER, 100_00, bank.id, firstMonth.plusDays(9), to = cash.id, note = "ATM")
        drafts += Draft(TxType.SAVE, 300_00, bank.id, firstMonth.plusDays(3), goal = phoneGoal.id)
        drafts += Draft(TxType.SAVE, 450_00, bank.id, firstMonth.plusMonths(1).plusDays(3).coerceAtMost(today), goal = vacationGoal.id)

        val sorted = drafts.filter { !it.date.isAfter(today) }.sortedBy { it.date }
        val base = now - sorted.size * 1000L
        val transactions = sorted.mapIndexed { i, d ->
            Transaction(
                id = id(), type = d.type, amountCents = d.amount, accountId = d.account,
                toAccountId = d.to, categoryId = d.category, goalId = d.goal, recurringId = d.recurring,
                date = d.date, note = d.note, createdAt = base + i * 1000L,
            )
        }

        db.withTransaction {
            db.accounts().upsertAll(listOf(bank, cash))
            db.categories().upsertAll(listOf(groceries, rent, restaurants, transport, subs, fun_))
            db.goals().upsertAll(listOf(phoneGoal, vacationGoal))
            db.recurring().upsertAll(listOf(phoneCredit, netflix))
            db.transactions().upsertAll(transactions)
        }
    }
}
