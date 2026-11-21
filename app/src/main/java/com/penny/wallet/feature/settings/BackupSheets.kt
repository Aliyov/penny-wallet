package com.penny.wallet.feature.settings

import com.penny.wallet.core.util.DateText
import com.penny.wallet.core.util.dateText
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.penny.wallet.data.backup.BackupSummary
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.ui.components.ButtonKind
import com.penny.wallet.core.ui.components.PennyButton
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.PennySheet
import com.penny.wallet.core.ui.theme.Penny
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Step 1 of restore: what's in the file. "Replace all data" leads to the second confirmation. */
@Composable
internal fun RestoreSummarySheet(sum: BackupSummary, onDismiss: () -> Unit, onReplace: () -> Unit) {
    val c = Penny.colors
    val dates = dateText()
    PennySheet(onDismiss = onDismiss, title = "Restore backup") {
        PennyCard(Modifier.fillMaxWidth()) {
            SummaryLine("Exported", isoToDate(sum.exportedAt, dates))
            SummaryLine("Accounts", sum.accounts.toString())
            SummaryLine("Categories", sum.categories.toString())
            SummaryLine("Goals", sum.goals.toString())
            SummaryLine("Transactions", sum.transactions.toString())
            SummaryLine("Recurring", sum.recurring.toString())
            if (sum.firstDate != null && sum.lastDate != null) {
                SummaryLine("Date range", "${isoToDate(sum.firstDate, dates)} – ${isoToDate(sum.lastDate, dates)}")
            }
        }
        if (sum.danglingRefs > 0) {
            Text(
                "${sum.danglingRefs} entries reference missing accounts, categories or goals. They will be restored anyway.",
                style = Penny.type.caption, color = c.warning, modifier = Modifier.padding(top = 12.dp),
            )
        }
        Text(
            "Restoring replaces all data currently on this phone.",
            style = Penny.type.caption, color = c.textSecondary, modifier = Modifier.padding(top = 12.dp),
        )
        PennyButton("Replace all data", onReplace, Modifier.fillMaxWidth().padding(top = 16.dp), kind = ButtonKind.DANGER)
        PennyButton("Cancel", onDismiss, Modifier.fillMaxWidth().padding(top = 8.dp), kind = ButtonKind.SECONDARY)
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    val c = Penny.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = Penny.type.label, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = Penny.type.bodyStrong, color = c.textPrimary)
    }
}

/** Accepts an ISO instant or ISO date; falls back to the raw text. */
private fun isoToDate(s: String, dates: DateText): String =
    runCatching { dates.fullDate(Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate()) }
        .recoverCatching { dates.fullDate(LocalDate.parse(s.take(10))) }
        .getOrDefault(s)
