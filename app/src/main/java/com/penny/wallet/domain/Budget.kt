package com.penny.wallet.domain

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

/** One monthly spending budget over all expenses, measured over the user's budgeting period. */
object Budget {
    /** FAST once spending runs more than 10 % ahead of an even pace, so small swings don't warn. */
    private const val FAST_MARGIN_PERCENT = 110

    fun status(budget: Long, spent: Long, period: Period, today: LocalDate): BudgetStatus? {
        if (budget <= 0) return null
        val totalDays = ChronoUnit.DAYS.between(period.start, period.end).toInt() + 1
        val daysLeft = (ChronoUnit.DAYS.between(today, period.end).toInt() + 1).coerceIn(1, totalDays)
        val elapsed = totalDays - daysLeft + 1
        val left = budget - spent
        val evenPace = budget * elapsed / totalDays
        val pace = when {
            left < 0 -> BudgetPace.OVER
            spent * 100 > evenPace * FAST_MARGIN_PERCENT -> BudgetPace.FAST
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
}
