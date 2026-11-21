package com.penny.wallet.platform.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.DpSize
import com.penny.wallet.MainActivity

class QuickAddWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(48.dp, 48.dp), DpSize(110.dp, 48.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = loadWidgetData(context)
        val open = Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_QUICK_ADD)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        provideContent {
            val wide = LocalSize.current.width >= 110.dp
            val wc = WidgetColors(d)
            Box(
                GlanceModifier.fillMaxSize().background(wc.background).cornerRadius(22.dp).clickable(actionStartActivity(open)),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(GlanceModifier.size(36.dp).background(wc.accent).cornerRadius(18.dp), contentAlignment = Alignment.Center) {
                        Text("+", style = TextStyle(color = wc.onAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    }
                    if (wide) {
                        Spacer(GlanceModifier.width(10.dp))
                        Text(d.labels.addExpense, style = TextStyle(color = wc.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}
