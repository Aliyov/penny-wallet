package com.penny.wallet.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtPayment
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.db.entities.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder, createdAt") fun observeAll(): Flow<List<Account>>
    @Query("SELECT * FROM accounts ORDER BY sortOrder, createdAt") suspend fun getAll(): List<Account>
    @Query("SELECT * FROM accounts WHERE id = :id") suspend fun get(id: String): Account?
    @Upsert suspend fun upsert(a: Account)
    @Upsert suspend fun upsertAll(a: List<Account>)
    @Delete suspend fun delete(a: Account)
    @Query("DELETE FROM accounts") suspend fun clear()
    @Query("DELETE FROM accounts WHERE id LIKE 'sample-%'") suspend fun deleteSample()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name") fun observeAll(): Flow<List<Category>>
    @Query("SELECT * FROM categories ORDER BY sortOrder, name") suspend fun getAll(): List<Category>
    @Query("SELECT * FROM categories WHERE id = :id") suspend fun get(id: String): Category?
    @Upsert suspend fun upsert(c: Category)
    @Upsert suspend fun upsertAll(c: List<Category>)
    @Delete suspend fun delete(c: Category)
    @Query("DELETE FROM categories") suspend fun clear()
    @Query(
        "DELETE FROM categories WHERE id LIKE 'sample-%' AND id NOT IN (SELECT categoryId FROM transactions WHERE categoryId IS NOT NULL) " +
            "AND id NOT IN (SELECT categoryId FROM recurring)"
    )
    suspend fun deleteUnusedSample()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY sortOrder, createdAt") fun observeAll(): Flow<List<Goal>>
    @Query("SELECT * FROM goals ORDER BY sortOrder, createdAt") suspend fun getAll(): List<Goal>
    @Query("SELECT * FROM goals WHERE id = :id") suspend fun get(id: String): Goal?
    @Upsert suspend fun upsert(g: Goal)
    @Upsert suspend fun upsertAll(g: List<Goal>)
    @Delete suspend fun delete(g: Goal)
    @Query("DELETE FROM goals") suspend fun clear()
    @Query("DELETE FROM goals WHERE id LIKE 'sample-%'") suspend fun deleteSample()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC") fun observeAll(): Flow<List<Transaction>>
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC") suspend fun getAll(): List<Transaction>
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun get(id: String): Transaction?
    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId ORDER BY date DESC, createdAt DESC")
    fun observeForAccount(accountId: String): Flow<List<Transaction>>
    @Query("SELECT * FROM transactions WHERE accountId = :accountId OR toAccountId = :accountId")
    suspend fun getForAccount(accountId: String): List<Transaction>
    @Query("SELECT * FROM transactions WHERE goalId = :goalId") suspend fun getForGoal(goalId: String): List<Transaction>
    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId") suspend fun countForCategory(categoryId: String): Int
    @Query("SELECT COUNT(*) FROM transactions WHERE recurringId = :recurringId AND date = :date")
    suspend fun countForRecurring(recurringId: String, date: LocalDate): Int
    @Query("UPDATE transactions SET categoryId = :to WHERE categoryId = :from") suspend fun moveCategory(from: String, to: String)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(t: Transaction)
    @Upsert suspend fun upsert(t: Transaction)
    @Upsert suspend fun upsertAll(t: List<Transaction>)
    @Delete suspend fun delete(t: Transaction)
    @Query("DELETE FROM transactions WHERE id IN (:ids)") suspend fun deleteIds(ids: List<String>)
    @Query("SELECT * FROM transactions WHERE id IN (:ids)") suspend fun getIds(ids: List<String>): List<Transaction>
    @Query("DELETE FROM transactions WHERE importId = :importId") suspend fun deleteImport(importId: String): Int
    @Query("SELECT * FROM transactions WHERE splitId = :splitId ORDER BY createdAt") suspend fun getSplit(splitId: String): List<Transaction>
    @Query("DELETE FROM transactions WHERE splitId = :splitId") suspend fun deleteSplit(splitId: String)
    @Query("UPDATE transactions SET recurringId = :recurringId WHERE id IN (:ids) AND recurringId IS NULL")
    suspend fun linkToRecurring(ids: List<String>, recurringId: String)
    @Query("DELETE FROM transactions") suspend fun clear()
    @Query(
        "DELETE FROM transactions WHERE id LIKE 'sample-%' OR accountId LIKE 'sample-%' OR toAccountId LIKE 'sample-%' " +
            "OR goalId LIKE 'sample-%' OR recurringId LIKE 'sample-%'"
    )
    suspend fun deleteSample()
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY nextDueDate, name") fun observeAll(): Flow<List<RecurringExpense>>
    @Query("SELECT * FROM recurring ORDER BY nextDueDate, name") suspend fun getAll(): List<RecurringExpense>
    @Query("SELECT * FROM recurring WHERE id = :id") suspend fun get(id: String): RecurringExpense?
    @Upsert suspend fun upsert(r: RecurringExpense)
    @Upsert suspend fun upsertAll(r: List<RecurringExpense>)
    @Delete suspend fun delete(r: RecurringExpense)
    @Query("DELETE FROM recurring") suspend fun clear()
    @Query("DELETE FROM recurring WHERE id LIKE 'sample-%' OR accountId LIKE 'sample-%'") suspend fun deleteSample()
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY settledOn IS NOT NULL, date DESC, createdAt DESC") fun observeAll(): Flow<List<Debt>>
    @Query("SELECT * FROM debts ORDER BY date DESC, createdAt DESC") suspend fun getAll(): List<Debt>
    @Query("SELECT * FROM debts WHERE id = :id") suspend fun get(id: String): Debt?
    @Upsert suspend fun upsert(d: Debt)
    @Upsert suspend fun upsertAll(d: List<Debt>)
    @Query("DELETE FROM debts WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM debts") suspend fun clear()

    @Query("SELECT * FROM debt_payments ORDER BY date, createdAt") fun observePayments(): Flow<List<DebtPayment>>
    @Query("SELECT * FROM debt_payments ORDER BY date, createdAt") suspend fun getAllPayments(): List<DebtPayment>
    @Upsert suspend fun upsertPayment(p: DebtPayment)
    @Upsert suspend fun upsertPayments(p: List<DebtPayment>)
    @Query("DELETE FROM debt_payments WHERE id = :id") suspend fun deletePayment(id: String)
    @Query("DELETE FROM debt_payments WHERE debtId = :debtId") suspend fun deletePaymentsOf(debtId: String)
    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId") suspend fun paymentsOf(debtId: String): List<DebtPayment>
    @Query("SELECT * FROM debt_payments WHERE id = :id") suspend fun getPayment(id: String): DebtPayment?
    @Query("DELETE FROM debt_payments") suspend fun clearPayments()
    @Query("DELETE FROM debts WHERE id LIKE 'sample-%'") suspend fun deleteSample()
}
