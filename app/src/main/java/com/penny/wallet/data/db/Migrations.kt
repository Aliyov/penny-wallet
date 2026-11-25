package com.penny.wallet.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** SQL copied from app/schemas/…/2.json; Room validates the result against the entities on open. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `categories` ADD COLUMN `monthlyBudgetCents` INTEGER")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `splitId` TEXT")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `importId` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_splitId` ON `transactions` (`splitId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_importId` ON `transactions` (`importId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `debts` (`id` TEXT NOT NULL, `person` TEXT NOT NULL, `direction` TEXT NOT NULL, " +
                "`amountCents` INTEGER NOT NULL, `date` INTEGER NOT NULL, `dueDate` INTEGER, `remind` INTEGER NOT NULL, " +
                "`note` TEXT, `settledOn` INTEGER, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `debt_payments` (`id` TEXT NOT NULL, `debtId` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, " +
                "`date` INTEGER NOT NULL, `note` TEXT, `transactionId` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`debtId`) REFERENCES `debts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_debtId` ON `debt_payments` (`debtId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_transactionId` ON `debt_payments` (`transactionId`)")
    }
}
