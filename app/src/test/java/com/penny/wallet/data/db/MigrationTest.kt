package com.penny.wallet.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.penny.wallet.data.backup.BackupCodec
import com.penny.wallet.data.db.entities.Debt
import com.penny.wallet.data.db.entities.DebtDirection
import com.penny.wallet.data.db.entities.DebtPayment
import com.penny.wallet.data.db.entities.TxType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.LocalDate

/** Opens a database written with the v1 schema (as on users' phones) through every migration to the current version. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val name = "migration-test.db"
    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    /** Builds the database exactly as the exported schema [version] describes, without Room. */
    private fun createFromSchema(version: Int): SQLiteDatabase {
        val schema = File("schemas/${PennyDatabase::class.java.name}/$version.json")
        val db = Json.parseToJsonElement(schema.readText()).jsonObject.getValue("database").jsonObject
        val out = SQLiteDatabase.openOrCreateDatabase(ctx.getDatabasePath(name).apply { parentFile?.mkdirs() }, null)
        for (e in db.getValue("entities").jsonArray.map { it.jsonObject }) {
            val table = e.getValue("tableName").jsonPrimitive.content
            out.execSQL(e.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            e["indices"]?.jsonArray?.forEach { out.execSQL(it.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table)) }
        }
        db.getValue("setupQueries").jsonArray.forEach { out.execSQL(it.jsonPrimitive.content) }
        out.version = version
        return out
    }

    @Test fun v1DataSurvivesUpgradeToCurrent() = runBlocking {
        val day = LocalDate.of(2026, 2, 28).toEpochDay()
        createFromSchema(1).apply {
            execSQL("INSERT INTO accounts VALUES ('a1', 'Bank', 'bank', 0, 0, 1)")
            execSQL("INSERT INTO accounts VALUES ('a2', 'Cash €', 'cash', 1, 1, 2)")
            execSQL("INSERT INTO categories VALUES ('c1', 'Groceries', 'groceries', ${0xFF34C759}, 0, 0)")
            execSQL("INSERT INTO goals VALUES ('g1', 'Trip', 'plane', ${0xFF0A84FF}, 100000, $day, 0, 0, 3)")
            execSQL("INSERT INTO transactions VALUES ('t1', 'INCOME', 220000, 'a1', NULL, NULL, NULL, NULL, $day, 'Salary', 4)")
            execSQL("INSERT INTO transactions VALUES ('t2', 'EXPENSE', 1250, 'a1', NULL, 'c1', NULL, 'r1', $day, 'Café, \"quoted\"', 5)")
            execSQL("INSERT INTO transactions VALUES ('t3', 'TRANSFER', 9223372036854775, 'a1', 'a2', NULL, NULL, NULL, $day, NULL, 6)")
            execSQL("INSERT INTO transactions VALUES ('t4', 'SAVE', 30000, 'a1', NULL, NULL, 'g1', NULL, $day, NULL, 7)")
            execSQL("INSERT INTO recurring VALUES ('r1', 'Phone', 1000, 'c1', 'a1', 'MONTHLY', 31, NULL, NULL, $day, 1, 0, 1)")
            close()
        }

        val db = Room.databaseBuilder(ctx, PennyDatabase::class.java, name)
            .addMigrations(*PennyDatabase.MIGRATIONS).allowMainThreadQueries().build()

        assertEquals(listOf("a1", "a2"), db.accounts().getAll().map { it.id })
        assertEquals(true, db.accounts().getAll()[1].archived)
        val txs = db.transactions().getAll().associateBy { it.id }
        assertEquals(4, txs.size)
        assertEquals(TxType.EXPENSE, txs.getValue("t2").type)
        assertEquals("Café, \"quoted\"", txs.getValue("t2").note)
        assertEquals(9223372036854775L, txs.getValue("t3").amountCents)
        assertEquals(LocalDate.of(2026, 2, 28), txs.getValue("t1").date)
        assertEquals(31, db.recurring().getAll().single().dayOfMonth)
        assertEquals(LocalDate.of(2026, 2, 28), db.goals().getAll().single().deadline)

        assertEquals(null, db.categories().getAll().single().monthlyBudgetCents)
        assertTrue(txs.values.all { it.splitId == null && it.importId == null })
        assertTrue(db.debts().getAll().isEmpty())
        db.debts().upsert(Debt("d1", "Anna", DebtDirection.OWED_TO_ME, 5000, LocalDate.of(2026, 3, 1), createdAt = 8))
        db.debts().upsertPayment(DebtPayment("p1", "d1", 2000, LocalDate.of(2026, 3, 5), createdAt = 9))
        db.transactions().upsert(txs.getValue("t2").copy(splitId = "s1", importId = "i1"))
        db.categories().upsert(db.categories().getAll().single().copy(monthlyBudgetCents = 40000))
        assertEquals("s1", db.transactions().getAll().single { it.id == "t2" }.splitId)
        assertEquals(40000L, db.categories().getAll().single().monthlyBudgetCents)
        assertEquals(2000L, db.debts().getAllPayments().single().amountCents)
        db.close()
    }

    @Test fun v1SampleBackupMigratesUnchanged() = runBlocking {
        val b = BackupCodec.decode(File("src/test/resources/backups/v1-sample.json").readText(), null)
        fun q(v: String?) = v?.let { "'" + it.replace("'", "''") + "'" } ?: "NULL"
        fun n(v: Any?) = v?.toString() ?: "NULL"
        fun bool(v: Boolean) = if (v) 1 else 0
        createFromSchema(1).apply {
            b.accounts.forEach { execSQL("INSERT INTO accounts VALUES (${q(it.id)}, ${q(it.name)}, ${q(it.icon)}, ${bool(it.archived)}, ${it.sortOrder}, ${it.createdAt})") }
            b.categories.forEach { execSQL("INSERT INTO categories VALUES (${q(it.id)}, ${q(it.name)}, ${q(it.icon)}, ${it.color}, ${bool(it.archived)}, ${it.sortOrder})") }
            b.goals.forEach {
                execSQL("INSERT INTO goals VALUES (${q(it.id)}, ${q(it.name)}, ${q(it.icon)}, ${it.color}, ${it.targetCents}, " +
                    "${n(it.deadline?.toEpochDay())}, ${bool(it.archived)}, ${it.sortOrder}, ${it.createdAt})")
            }
            b.transactions.forEach {
                execSQL("INSERT INTO transactions VALUES (${q(it.id)}, ${q(it.type.name)}, ${it.amountCents}, ${q(it.accountId)}, " +
                    "${q(it.toAccountId)}, ${q(it.categoryId)}, ${q(it.goalId)}, ${q(it.recurringId)}, ${it.date.toEpochDay()}, ${q(it.note)}, ${it.createdAt})")
            }
            b.recurring.forEach {
                execSQL("INSERT INTO recurring VALUES (${q(it.id)}, ${q(it.name)}, ${it.amountCents}, ${q(it.categoryId)}, ${q(it.accountId)}, " +
                    "${q(it.frequency.name)}, ${n(it.dayOfMonth)}, ${n(it.month)}, ${n(it.dayOfWeek)}, ${it.nextDueDate.toEpochDay()}, " +
                    "${it.remindDaysBefore}, ${bool(it.autoPay)}, ${bool(it.active)})")
            }
            close()
        }
        val db = Room.databaseBuilder(ctx, PennyDatabase::class.java, name)
            .addMigrations(*PennyDatabase.MIGRATIONS).allowMainThreadQueries().build()
        assertEquals(b.accounts.sortedBy { it.id }, db.accounts().getAll().sortedBy { it.id })
        assertEquals(b.categories.sortedBy { it.id }, db.categories().getAll().sortedBy { it.id })
        assertEquals(b.goals.sortedBy { it.id }, db.goals().getAll().sortedBy { it.id })
        assertEquals(b.transactions.sortedBy { it.id }, db.transactions().getAll().sortedBy { it.id })
        assertEquals(b.recurring.sortedBy { it.id }, db.recurring().getAll().sortedBy { it.id })
        db.close()
    }
}
