package com.penny.wallet.feature.settings

import com.penny.wallet.core.util.DateText
import com.penny.wallet.core.util.dateText
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.penny.wallet.R
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
    PennySheet(onDismiss = onDismiss, title = stringResource(R.string.backup_restore_title)) {
        PennyCard(Modifier.fillMaxWidth()) {
            SummaryLine(stringResource(R.string.backup_sum_exported), isoToDate(sum.exportedAt, dates))
            SummaryLine(stringResource(R.string.settings_accounts), sum.accounts.toString())
            SummaryLine(stringResource(R.string.backup_sum_categories), sum.categories.toString())
            SummaryLine(stringResource(R.string.backup_sum_goals), sum.goals.toString())
            SummaryLine(stringResource(R.string.backup_sum_transactions), sum.transactions.toString())
            SummaryLine(stringResource(R.string.backup_sum_recurring), sum.recurring.toString())
            if (sum.firstDate != null && sum.lastDate != null) {
                SummaryLine(stringResource(R.string.backup_sum_date_range), stringResource(R.string.backup_date_range_value, isoToDate(sum.firstDate, dates), isoToDate(sum.lastDate, dates)))
            }
        }
        if (sum.danglingRefs > 0) {
            Text(
                pluralStringResource(R.plurals.backup_dangling, sum.danglingRefs, sum.danglingRefs),
                style = Penny.type.caption, color = c.warning, modifier = Modifier.padding(top = 12.dp),
            )
        }
        Text(
            stringResource(R.string.backup_restore_note),
            style = Penny.type.caption, color = c.textSecondary, modifier = Modifier.padding(top = 12.dp),
        )
        PennyButton(stringResource(R.string.backup_replace_all), onReplace, Modifier.fillMaxWidth().padding(top = 16.dp), kind = ButtonKind.DANGER)
        PennyButton(stringResource(R.string.action_cancel), onDismiss, Modifier.fillMaxWidth().padding(top = 8.dp), kind = ButtonKind.SECONDARY)
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
