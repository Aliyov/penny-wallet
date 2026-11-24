package com.penny.wallet

import com.penny.wallet.platform.crash.CrashReporter
import android.app.Application
import com.penny.wallet.platform.notify.Notifications
import com.penny.wallet.feature.lock.AppLock
import com.penny.wallet.platform.widget.WidgetUpdater
import com.penny.wallet.platform.work.Scheduler
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PennyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        container = AppContainer(this)
        WidgetUpdater(this, container).start()
        AppLock.install { container.settings.prefs.value }
        container.appScope.launch {
            val s = container.settings
            container.setup.ensureInitialised()
            Notifications.ensureChannel(this@PennyApp)
            val p = s.current()
            Scheduler.scheduleDaily(this@PennyApp, p)
            Scheduler.scheduleBackup(this@PennyApp, p)
            Scheduler.catchUp(this@PennyApp)
            watchScheduleSettings()
        }
    }
}

private fun PennyApp.watchScheduleSettings() {
    val s = container.settings
    container.appScope.launch {
        s.prefs.map { it.reminderHour to it.reminderMinute }.distinctUntilChanged().drop(1)
            .collect { Scheduler.scheduleDaily(this@watchScheduleSettings, s.current(), replace = true) }
    }
    container.appScope.launch {
        s.prefs.map { Triple(it.backupFolderUri, it.backupFrequency, it.seeded) }.distinctUntilChanged().drop(1)
            .collect { Scheduler.scheduleBackup(this@watchScheduleSettings, s.current(), replace = true) }
    }
}

val android.content.Context.container: AppContainer get() = (applicationContext as PennyApp).container
