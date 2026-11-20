package com.penny.wallet.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.penny.wallet.MainActivity

/** "Available" in accent + spent this month; the large size adds the next recurring expense (SPEC §7b). */
class BalanceWidget : GlanceAppWidget() {
    companion object {
        private val SMALL = DpSize(110.dp, 40.dp)
        private val LARGE = DpSize(250.dp, 110.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val d = loadWidgetData(context)
        val open = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        provideContent { Content(d, open) }
    }

    @androidx.compose.runtime.Composable
    private fun Content(d: WidgetData, open: Intent) {
        val large = LocalSize.current.height >= LARGE.height
        val accent = if (d.negative) Color(0xFFFF453A) else Color(d.accentArgb)
        val secondary = ColorProvider(Color(0xFF8E8E93))
        Column(
            GlanceModifier.fillMaxSize().background(widgetBackground(d)).cornerRadius(22.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp).clickable(actionStartActivity(open)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Available", style = TextStyle(color = secondary, fontSize = 11.sp))
            Text(d.available, maxLines = 1, style = TextStyle(color = ColorProvider(accent), fontSize = if (large) 30.sp else 20.sp, fontWeight = FontWeight.Bold))
            Text("Spent this month ${d.spentMonth}", maxLines = 1, style = TextStyle(color = secondary, fontSize = 11.sp))
            if (large && d.nextName != null) {
                Spacer(GlanceModifier.height(8.dp))
                Text("Next: ${d.nextName} · ${d.nextWhen}", maxLines = 1,
                    style = TextStyle(color = ColorProvider(Color(0xFFF5F5F7)), fontSize = 12.sp, fontWeight = FontWeight.Medium))
                Text(d.nextAmount.orEmpty(), maxLines = 1, style = TextStyle(color = secondary, fontSize = 12.sp))
            }
        }
    }
}

internal fun widgetBackground(d: WidgetData) = if (d.darkGrey) Color(0xFF1A1A1E) else Color(0xFF0D0D0F)

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}
