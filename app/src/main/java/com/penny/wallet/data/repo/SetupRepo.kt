package com.penny.wallet.data.repo

import android.content.Context
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.data.db.DefaultCategories
import com.penny.wallet.data.db.PennyDatabase
import com.penny.wallet.data.db.SampleData
import com.penny.wallet.data.prefs.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class SetupRepo(
    private val context: Context,
    private val db: PennyDatabase,
    private val settings: SettingsStore,
    private val listener: ChangeListener,
) {
    val hasSampleData: Flow<Boolean> =
        db.accounts().observeAll().map { list -> list.any { it.id.startsWith(SampleData.PREFIX) } }

    /** First launch: default categories and a currency from the device region. No demo data. */
    suspend fun ensureInitialised(locale: Locale = Locale.getDefault()) {
        val p = settings.current()
        if (!p.seeded) {
            val upgradedInstall = db.accounts().getAll().isNotEmpty()
            if (db.categories().getAll().isEmpty()) db.categories().upsertAll(DefaultCategories.build(context::getString))
            settings.update {
                it.copy(seeded = true, currencyCode = it.currencyCode.ifEmpty {
                    if (upgradedInstall) Formatters.DEFAULT_CURRENCY else Formatters.suggestedCurrency(locale)
                })
            }
            listener.onDataChanged()
        } else if (p.currencyCode.isEmpty()) {
            // Installs from before currency choice were euro-only.
            settings.update { it.copy(currencyCode = Formatters.DEFAULT_CURRENCY) }
        }
    }

    suspend fun addSampleData() {
        val digits = Formatters.fractionDigits(settings.current().currencyCode)
        // Sample amounts are in cents; read as yen they are still plausible, three-decimal currencies need ×10.
        val scale = if (digits == 3) 10L else 1L
        SampleData.add(db, context::getString, scale)
        listener.onDataChanged()
    }

    suspend fun removeSampleData() {
        SampleData.remove(db)
        listener.onDataChanged()
    }
}
