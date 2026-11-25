package com.penny.wallet.domain

import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.data.db.entities.DebtPayment
import java.time.LocalDate

data class DebtStatus(
    val debt: Debt,
    val paid: Long,
    /** Never below 0, even if more was paid back than owed. */
    val remaining: Long,
    val settled: Boolean,
    val overdue: Boolean,
)

data class DebtTotals(val iOwe: Long, val owedToMe: Long, val open: Int)

/** Personal debts ("I owe Anna", "Marco owes me"): kept apart from account balances unless a payment is recorded there too. */
object Debts {
    fun status(debt: Debt, payments: List<DebtPayment>, today: LocalDate): DebtStatus {
        val paid = payments.filter { it.debtId == debt.id }.sumOf { it.amountCents }
        val remaining = (debt.amountCents - paid).coerceAtLeast(0)
        val settled = debt.settledOn != null || remaining == 0L
        return DebtStatus(debt, paid, remaining, settled, overdue = !settled && debt.dueDate?.isBefore(today) == true)
    }

    /** Open first (soonest due, then newest), settled last (most recently settled first). */
    fun statuses(debts: List<Debt>, payments: List<DebtPayment>, today: LocalDate): List<DebtStatus> {
        val byDebt = payments.groupBy { it.debtId }
        return debts.map { status(it, byDebt[it.id].orEmpty(), today) }.sortedWith(
            compareBy<DebtStatus> { it.settled }
                .thenBy { if (it.settled) 0 else it.debt.dueDate?.toEpochDay() ?: Long.MAX_VALUE }
                .thenByDescending { it.debt.settledOn ?: it.debt.date }
                .thenByDescending { it.debt.createdAt }
        )
    }

    fun totals(statuses: List<DebtStatus>): DebtTotals {
        val open = statuses.filterNot { it.settled }
        return DebtTotals(
            iOwe = open.filter { it.debt.direction == DebtDirection.I_OWE }.sumOf { it.remaining },
            owedToMe = open.filter { it.debt.direction == DebtDirection.OWED_TO_ME }.sumOf { it.remaining },
            open = open.size,
        )
    }

    /** Debts whose reminder fires today: open, reminder on, due today. */
    fun dueToday(statuses: List<DebtStatus>, today: LocalDate): List<DebtStatus> =
        statuses.filter { !it.settled && it.debt.remind && it.debt.dueDate == today }
}
