package com.penny.wallet.platform

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.penny.wallet.MainActivity

/** Quick Settings tile "Add expense": opens Quick add like the widget and the app shortcut. */
class QuickAddTileService : TileService() {
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_QUICK_ADD)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Android 14 rejects the Intent overload; older versions lack the PendingIntent one.
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
