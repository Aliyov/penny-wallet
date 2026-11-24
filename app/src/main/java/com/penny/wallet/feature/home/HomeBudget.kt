package com.penny.wallet.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.money
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.domain.BudgetPace
import com.penny.wallet.domain.BudgetStatus
import com.penny.wallet.domain.Insights
import com.penny.wallet.domain.MonthInsight
import kotlin.math.abs

@Composable
internal fun BudgetBar(b: BudgetStatus) {
    val c = Penny.colors
    val color = when (b.pace) {
        BudgetPace.ON_TRACK -> c.accent
        BudgetPace.FAST -> c.warning
        BudgetPace.OVER -> c.danger
    }
    val shape = RoundedCornerShape(50)
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(shape).background(c.hairline)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(b.fraction, 0f..1f) },
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(b.fraction).clip(shape).background(color))
        }
        val text = when (b.pace) {
            BudgetPace.OVER -> stringResource(R.string.budget_over, money(-b.left))
            BudgetPace.FAST -> stringResource(R.string.budget_left_fast, money(b.left), money(b.perDay))
            BudgetPace.ON_TRACK -> stringResource(R.string.budget_left, money(b.left), money(b.perDay))
        }
        Text(text, style = Penny.type.label, color = if (b.pace == BudgetPace.ON_TRACK) c.textSecondary else color, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
internal fun InsightLine(i: MonthInsight, topName: String?) {
    val c = Penny.colors
    val change = i.changePercent?.let { p ->
        when {
            abs(p) < Insights.SAME_PERCENT -> stringResource(R.string.insight_same)
            p < 0 -> stringResource(R.string.insight_less, -p)
            else -> stringResource(R.string.insight_more, p)
        }
    }
    val top = topName?.takeIf { i.topCategoryCents > 0 }?.let { stringResource(R.string.insight_top, it, money(i.topCategoryCents)) }
    val line = listOfNotNull(change, top).joinToString(" · ")
    if (line.isNotEmpty()) Text(line, style = Penny.type.label, color = c.textSecondary, modifier = Modifier.padding(top = 8.dp))
}
