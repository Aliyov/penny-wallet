package com.penny.wallet.data.db

import androidx.room.withTransaction
import com.penny.wallet.R
import com.penny.wallet.core.ui.theme.ItemPalette
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Frequency
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.db.entities.Transaction
import com.penny.wallet.data.db.entities.TxType
import java.time.LocalDate
import java.util.Random
import java.util.UUID

/** Category set every new install starts with; names are localized once, then belong to the user. */
object DefaultCategories {
    private data class Def(val name: Int, val icon: String, val color: Int)

    private val defs = listOf(
        Def(R.string.category_groceries, "groceries", 0),
        Def(R.string.category_rent, "rent", 1),
        Def(R.string.category_eating_out, "restaurant", 4),
        Def(R.string.category_transport, "bus", 5),
        Def(R.string.category_subscriptions, "subscription", 2),
        Def(R.string.category_shopping, "shopping", 9),
        Def(R.string.category_health, "health", 6),
        Def(R.string.category_fun, "game", 3),
    )

    fun build(str: (Int) -> String): List<Category> = defs.mapIndexed { i, d ->
        Category(UUID.randomUUID().toString(), str(d.name), d.icon, ItemPalette[d.color], sortOrder = i)
    }
}

/**
 * Optional demo set behind "Try with sample data". Every row's id starts with [PREFIX], so
 * [remove] can take it out again without touching what the user added.
 */
object SampleData {
    const val PREFIX = "sample-"

    private fun id() = PREFIX + UUID.randomUUID().toString()

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

    /** [scale] converts the cent amounts below into the user's currency (e.g. ×100 for yen-like magnitudes). */
    suspend fun add(db: PennyDatabase, str: (Int) -> String, scale: Long = 1, today: LocalDate = LocalDate.now()) {
        val now = System.currentTimeMillis()
        val rnd = Random(42)
        val existingCats = db.categories().getAll()
        val newCats = mutableListOf<Category>()
        fun cat(icon: String, name: Int, colorIdx: Int): Category =
            existingCats.firstOrNull { it.icon == icon && !it.archived }
                ?: newCats.firstOrNull { it.icon == icon }
                ?: Category(id(), str(name), icon, ItemPalette[colorIdx], sortOrder = existingCats.size + newCats.size)
                    .also { newCats += it }

        val bank = Account(id(), str(R.string.sample_bank), "bank", sortOrder = 0, createdAt = now)
        val cash = Account(id(), str(R.string.sample_cash), "cash", sortOrder = 1, createdAt = now + 1)

        val groceries = cat("groceries", R.string.category_groceries, 0)
        val rent = cat("rent", R.string.category_rent, 1)
        val restaurants = cat("restaurant", R.string.category_eating_out, 4)
        val transport = cat("bus", R.string.category_transport, 5)
        val subs = cat("subscription", R.string.category_subscriptions, 2)
        val fun_ = cat("game", R.string.category_fun, 3)

        val phoneGoal = Goal(
            id(), str(R.string.sample_goal_phone), "phone", ItemPalette[7], targetCents = 900_00 * scale,
            deadline = today.plusMonths(6), sortOrder = 0, createdAt = now,
        )
        val vacationGoal = Goal(
            id(), str(R.string.sample_goal_vacation), "beach", ItemPalette[8], targetCents = 2_000_00 * scale,
            deadline = null, sortOrder = 1, createdAt = now + 1,
        )

        val phoneCredit = RecurringExpense(
            id(), str(R.string.sample_phone_credit), 10_00 * scale, subs.id, bank.id, Frequency.MONTHLY,
            dayOfMonth = 14, nextDueDate = nextMonthlyOn(14, today), autoPay = false,
        )
        val streaming = RecurringExpense(
            id(), str(R.string.sample_streaming), 13_00 * scale, subs.id, bank.id, Frequency.MONTHLY,
            dayOfMonth = 22, nextDueDate = nextMonthlyOn(22, today), autoPay = true,
        )

        data class Spend(val cat: Category, val acc: Account, val min: Long, val max: Long, val notes: List<Int>)
        val spends = listOf(
            Spend(groceries, bank, 15_00, 85_00, listOf(R.string.sample_note_supermarket, R.string.sample_note_weekly_shop)),
            Spend(groceries, cash, 3_00, 18_00, listOf(R.string.sample_note_bakery, R.string.sample_note_market)),
            Spend(restaurants, cash, 8_00, 35_00, listOf(R.string.sample_note_lunch, R.string.sample_note_coffee)),
            Spend(restaurants, bank, 20_00, 60_00, listOf(R.string.sample_note_dinner)),
            Spend(transport, bank, 2_00, 40_00, listOf(R.string.sample_note_fuel, R.string.sample_note_train)),
            Spend(fun_, bank, 10_00, 45_00, listOf(R.string.sample_note_cinema, R.string.sample_note_concert)),
        )

        val drafts = mutableListOf<Draft>()
        val firstMonth = today.withDayOfMonth(1).minusMonths(2)
        drafts += Draft(TxType.INCOME, 300_00, cash.id, firstMonth, note = str(R.string.sample_note_starting_cash))

        for (m in 0..2) {
            val monthStart = firstMonth.plusMonths(m.toLong())
            val lastDay = if (m == 2) today.dayOfMonth else monthStart.lengthOfMonth()
            fun dayOf(d: Int): LocalDate? = monthStart.withDayOfMonth(minOf(d, monthStart.lengthOfMonth()))
                .takeIf { !it.isAfter(today) }

            dayOf(1)?.let { drafts += Draft(TxType.INCOME, 2_200_00, bank.id, it, note = str(R.string.sample_note_salary)) }
            dayOf(2)?.let { drafts += Draft(TxType.EXPENSE, 750_00, bank.id, it, category = rent.id, note = rent.name) }
            dayOf(14)?.let {
                drafts += Draft(TxType.EXPENSE, 10_00, bank.id, it, category = subs.id, recurring = phoneCredit.id, note = phoneCredit.name)
            }
            dayOf(22)?.let {
                drafts += Draft(TxType.EXPENSE, 13_00, bank.id, it, category = subs.id, recurring = streaming.id, note = streaming.name)
            }
            repeat(9) {
                val s = spends[rnd.nextInt(spends.size)]
                val amount = s.min + (rnd.nextDouble() * (s.max - s.min)).toLong()
                val day = 1 + rnd.nextInt(lastDay)
                drafts += Draft(
                    TxType.EXPENSE, amount, s.acc.id, monthStart.withDayOfMonth(day),
                    category = s.cat.id, note = str(s.notes[rnd.nextInt(s.notes.size)]),
                )
            }
        }

        drafts += Draft(TxType.TRANSFER, 100_00, bank.id, firstMonth.plusDays(9), to = cash.id, note = str(R.string.sample_note_atm))
        drafts += Draft(TxType.SAVE, 300_00, bank.id, firstMonth.plusDays(3), goal = phoneGoal.id)
        drafts += Draft(TxType.SAVE, 450_00, bank.id, firstMonth.plusMonths(1).plusDays(3).coerceAtMost(today), goal = vacationGoal.id)

        val sorted = drafts.filter { !it.date.isAfter(today) }.sortedBy { it.date }
        val base = now - sorted.size * 1000L
        val transactions = sorted.mapIndexed { i, d ->
            Transaction(
                id = id(), type = d.type, amountCents = d.amount * scale, accountId = d.account,
                toAccountId = d.to, categoryId = d.category, goalId = d.goal, recurringId = d.recurring,
                date = d.date, note = d.note, createdAt = base + i * 1000L,
            )
        }

        db.withTransaction {
            db.accounts().upsertAll(listOf(bank, cash))
            db.categories().upsertAll(newCats)
            db.goals().upsertAll(listOf(phoneGoal, vacationGoal))
            db.recurring().upsertAll(listOf(phoneCredit, streaming))
            db.transactions().upsertAll(transactions)
        }
    }

    /** Also removes what the user entered on sample accounts or goals, since those disappear. */
    suspend fun remove(db: PennyDatabase) = db.withTransaction {
        db.transactions().deleteSample()
        db.recurring().deleteSample()
        db.goals().deleteSample()
        db.accounts().deleteSample()
        db.categories().deleteUnusedSample()
    }
}
