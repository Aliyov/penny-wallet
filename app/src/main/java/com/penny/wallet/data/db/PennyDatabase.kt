package com.penny.wallet.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.penny.wallet.data.db.dao.AccountDao
import com.penny.wallet.data.db.dao.CategoryDao
import com.penny.wallet.data.db.dao.GoalDao
import com.penny.wallet.data.db.dao.RecurringDao
import com.penny.wallet.data.db.dao.TransactionDao
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.db.entities.Goal
import com.penny.wallet.data.db.entities.RecurringExpense
import com.penny.wallet.data.db.entities.Transaction

@Database(
    entities = [Account::class, Category::class, Goal::class, Transaction::class, RecurringExpense::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PennyDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun goals(): GoalDao
    abstract fun transactions(): TransactionDao
    abstract fun recurring(): RecurringDao

    companion object {
        fun build(context: Context): PennyDatabase =
            Room.databaseBuilder(context, PennyDatabase::class.java, "penny.db").build()
    }
}
