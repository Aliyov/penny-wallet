package com.penny.wallet

import android.app.Application
import com.penny.wallet.data.db.Seed
import kotlinx.coroutines.launch

class PennyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appScope.launch {
            val s = container.settings
            if (!s.current().seeded) {
                if (container.accounts.all().isEmpty()) Seed.run(container.db)
                s.update { it.copy(seeded = true) }
            }
        }
    }
}

val android.content.Context.container: AppContainer get() = (applicationContext as PennyApp).container
