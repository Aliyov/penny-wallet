package com.penny.wallet.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/** In-memory database for Robolectric tests; queries run on the test thread. */
fun testDb(): PennyDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PennyDatabase::class.java)
    .allowMainThreadQueries().build()

val FIXED_TODAY: LocalDate = LocalDate.of(2026, 11, 24)

fun PennyDatabase.withSampleData(): PennyDatabase = apply {
    val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    runBlocking { SampleData.add(this@withSampleData, ctx::getString, today = FIXED_TODAY) }
}

