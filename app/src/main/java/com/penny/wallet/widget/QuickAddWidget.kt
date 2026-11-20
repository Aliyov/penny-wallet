package com.penny.wallet.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
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
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.DpSize
import com.penny.wallet.MainActivity

/** Green "+" that opens the Quick add sheet directly (deep link). 1×1, or 2×1 with a label. */
class QuickAddWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(48.dp, 48.dp), DpSize(110.dp, 48.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = loadWidgetData(context)
        val open = Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_QUICK_ADD)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        provideContent {
            val wide = LocalSize.current.width >= 110.dp
            val accent = Color(d.accentArgb)
            Box(
                GlanceModifier.fillMaxSize().background(widgetBackground(d)).cornerRadius(22.dp).clickable(actionStartActivity(open)),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(GlanceModifier.size(36.dp).background(accent).cornerRadius(18.dp), contentAlignment = Alignment.Center) {
                        Text("+", style = TextStyle(color = ColorProvider(Color.Black), fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    }
                    if (wide) {
                        Spacer(GlanceModifier.width(10.dp))
                        Text("Add expense", style = TextStyle(color = ColorProvider(Color(0xFFF5F5F7)), fontSize = 14.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}
