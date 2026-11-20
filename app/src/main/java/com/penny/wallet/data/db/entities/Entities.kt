package com.penny.wallet.data.db.entities

import androidx.room.Entity
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
    indices = [Index("date"), Index("accountId"), Index("categoryId"), Index("goalId"), Index("recurringId")],
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
