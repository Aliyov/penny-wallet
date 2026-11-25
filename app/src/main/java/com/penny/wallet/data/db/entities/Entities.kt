package com.penny.wallet.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** All money is Long cents. Never Float/Double. Entities double as backup DTOs (kotlinx.serialization). */

@Serializable
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val archived: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long,
)

@Serializable
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: Long,
    val archived: Boolean = false,
    val sortOrder: Int,
    /** null = no budget for this category. */
    val monthlyBudgetCents: Long? = null,
)

@Serializable
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val color: Long,
    val targetCents: Long,
    @Serializable(with = LocalDateSerializer::class) val deadline: LocalDate? = null,
    val archived: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long,
)

enum class TxType { INCOME, WITHDRAWAL, ADJUSTMENT, EXPENSE, TRANSFER, SAVE, UNSAVE }

@Serializable
@Entity(
    tableName = "transactions",
    indices = [
        Index("date"), Index("accountId"), Index("categoryId"), Index("goalId"), Index("recurringId"),
        Index("splitId"), Index("importId"),
    ],
)
data class Transaction(
    @PrimaryKey val id: String,
    val type: TxType,
    /** Positive; ADJUSTMENT may be negative. */
    val amountCents: Long,
    /** Source account (or target for INCOME / UNSAVE). */
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val goalId: String? = null,
    val recurringId: String? = null,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val note: String? = null,
    val createdAt: Long,
    /** Shared by the parts of one receipt split across categories; edit/delete act on all of them. */
    val splitId: String? = null,
    /** Set on rows from one CSV import so the whole import can be undone. */
    val importId: String? = null,
)

enum class Frequency { WEEKLY, MONTHLY, YEARLY }

@Serializable
@Entity(tableName = "recurring")
data class RecurringExpense(
    @PrimaryKey val id: String,
    val name: String,
    val amountCents: Long,
    val categoryId: String,
    val accountId: String,
    val frequency: Frequency = Frequency.MONTHLY,
    /** 1–31 for MONTHLY/YEARLY. */
    val dayOfMonth: Int? = null,
    /** 1–12 for YEARLY. */
    val month: Int? = null,
    /** 1 (Mon) – 7 (Sun), ISO, for WEEKLY. */
    val dayOfWeek: Int? = null,
    @Serializable(with = LocalDateSerializer::class) val nextDueDate: LocalDate,
    val remindDaysBefore: Int = 1,
    val autoPay: Boolean = false,
    val active: Boolean = true,
)

enum class DebtDirection { I_OWE, OWED_TO_ME }

/** A personal record, not linked to account balances unless a payment is recorded as a transaction too. */
@Serializable
@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey val id: String,
    val person: String,
    val direction: DebtDirection,
    val amountCents: Long,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val dueDate: LocalDate? = null,
    val remind: Boolean = false,
    val note: String? = null,
    @Serializable(with = LocalDateSerializer::class) val settledOn: LocalDate? = null,
    val createdAt: Long,
)

@Serializable
@Entity(
    tableName = "debt_payments",
    indices = [Index("debtId"), Index("transactionId")],
    foreignKeys = [
        ForeignKey(entity = Debt::class, parentColumns = ["id"], childColumns = ["debtId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Transaction::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.SET_NULL),
    ],
)
data class DebtPayment(
    @PrimaryKey val id: String,
    val debtId: String,
    val amountCents: Long,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val note: String? = null,
    /** The matching account transaction, when the user also recorded the money moving. */
    val transactionId: String? = null,
    val createdAt: Long,
)
