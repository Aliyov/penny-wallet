package com.penny.wallet.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.penny.wallet.data.db.entities.TxType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
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
        db.close()
    }
}
