package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Category
import java.math.BigInteger
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class BudgetPace { ON_TRACK, FAST, OVER }

data class BudgetStatus(
    val budget: Long,
    val spent: Long,
    /** Negative once the budget is exceeded. */
    val left: Long,
    /** What can still be spent per day, today included; 0 when nothing is left. */
    val perDay: Long,
    val daysLeft: Int,
    /** Spent share of the budget, 0..1. */
    val fraction: Float,
    val pace: BudgetPace,
)

/** The monthly budget over all expenses and the per-category budgets, measured over the user's budgeting period. */
object Budget {
    /** FAST once spending runs more than 10 % ahead of an even pace, so small swings don't warn. */
    private const val FAST_MARGIN_PERCENT = 110

    fun status(budget: Long, spent: Long, period: Period, today: LocalDate): BudgetStatus? {
        if (budget <= 0) return null
        val totalDays = ChronoUnit.DAYS.between(period.start, period.end).toInt() + 1
        val daysLeft = (ChronoUnit.DAYS.between(today, period.end).toInt() + 1).coerceIn(1, totalDays)
        val elapsed = totalDays - daysLeft + 1
        val left = budget - spent
        // spent > 110 % of an even pace, cross-multiplied; BigInteger because spent·100·days can exceed Long.
        val fast = BigInteger.valueOf(spent) * BigInteger.valueOf(100L * totalDays) >
            BigInteger.valueOf(budget) * BigInteger.valueOf(elapsed.toLong() * FAST_MARGIN_PERCENT)
        val pace = when {
            left < 0 -> BudgetPace.OVER
            fast -> BudgetPace.FAST
            else -> BudgetPace.ON_TRACK
        }
        return BudgetStatus(
            budget = budget, spent = spent, left = left,
            perDay = if (left > 0) left / daysLeft else 0,
            daysLeft = daysLeft,
            fraction = (spent.toDouble() / budget).coerceIn(0.0, 1.0).toFloat(),
            pace = pace,
        )
    }

    /**
     * Status of every category that has a budget, keyed by category id. [spentByCategory] is
     * [MoneyCalculator.spentByCategory] for the same [period], so the numbers match the Expenses chart.
     */
    fun forCategories(categories: List<Category>, spentByCategory: Map<String?, Long>, period: Period, today: LocalDate): Map<String, BudgetStatus> =
        categories.mapNotNull { cat ->
            val budget = cat.monthlyBudgetCents ?: return@mapNotNull null
            status(budget, spentByCategory[cat.id] ?: 0, period, today)?.let { cat.id to it }
        }.toMap()
}
