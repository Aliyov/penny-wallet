package com.penny.wallet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.penny.wallet.data.db.dao.AccountDao
import com.penny.wallet.data.db.dao.CategoryDao
import com.penny.wallet.data.db.dao.DebtDao
import com.penny.wallet.data.db.dao.GoalDao
import com.penny.wallet.data.db.dao.RecurringDao
import com.penny.wallet.data.db.dao.TransactionDao
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtPayment
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.db.entities.Transaction

@Database(
    entities = [
        Account::class, Category::class, Goal::class, Transaction::class, RecurringExpense::class,
        Debt::class, DebtPayment::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PennyDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun goals(): GoalDao
    abstract fun transactions(): TransactionDao
    abstract fun recurring(): RecurringDao
    abstract fun debts(): DebtDao

    companion object {
        /** Every step from v1 on; MigrationTest runs each against the exported schemas. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        fun build(context: Context): PennyDatabase =
            Room.databaseBuilder(context, PennyDatabase::class.java, "penny.db").addMigrations(*MIGRATIONS).build()
    }
}
