package com.penny.wallet.feature.importcsv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.penny.wallet.R
import com.penny.wallet.core.ui.components.PennyCard
import com.penny.wallet.core.ui.components.PennyChip
import com.penny.wallet.core.ui.theme.Penny
import com.penny.wallet.core.util.dateText
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.domain.csv.CsvColumn
import com.penny.wallet.domain.csv.CsvRowError

private val ROLE_ORDER = listOf(
    CsvColumn.DATE, CsvColumn.AMOUNT, CsvColumn.DEBIT, CsvColumn.CREDIT, CsvColumn.TYPE, CsvColumn.CATEGORY, CsvColumn.NOTE, CsvColumn.IGNORE,
)

@Composable
private fun roleName(r: CsvColumn): String = stringResource(
    when (r) {
        CsvColumn.DATE -> R.string.import_role_date
        CsvColumn.AMOUNT -> R.string.import_role_amount
        CsvColumn.DEBIT -> R.string.import_role_debit
        CsvColumn.CREDIT -> R.string.import_role_credit
        CsvColumn.TYPE -> R.string.import_role_type
        CsvColumn.CATEGORY -> R.string.import_role_category
        CsvColumn.NOTE -> R.string.import_role_note
        CsvColumn.IGNORE -> R.string.import_role_ignore
    },
)

@Composable
internal fun SummaryCard(s: ImportUi) {
    val c = Penny.colors
    PennyCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            s.fileName?.let { Text(it, style = Penny.type.bodyStrong, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            val parts = buildList {
                add(pluralStringResource(R.plurals.import_count_import, s.importCount, s.importCount))
                if (s.duplicateCount > 0) add(pluralStringResource(R.plurals.import_count_duplicates, s.duplicateCount, s.duplicateCount))
                if (s.errorCount > 0) add(pluralStringResource(R.plurals.import_count_errors, s.errorCount, s.errorCount))
            }
            Text(parts.joinToString(" · "), style = Penny.type.label, color = c.textSecondary)
        }
    }
}

@Composable
internal fun Warnings(s: ImportUi) {
    val c = Penny.colors
    val messages = buildList {
        if (s.columns.none { it.role == CsvColumn.DATE }) add(R.string.import_missing_date)
        else if (s.missingDate) add(R.string.import_bad_date)
        if (s.missingAmount) add(R.string.import_missing_amount)
        if (s.accountId == null) add(R.string.import_no_account)
        if (s.fallbackCategoryId == null) add(R.string.import_no_category)
    }
    messages.forEach { Text(stringResource(it), style = Penny.type.label, color = c.warning) }
}

@Composable
internal fun ColumnRoleRow(col: ImportColumn, onPick: (CsvColumn) -> Unit) {
    val c = Penny.colors
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(col.title ?: stringResource(R.string.import_column_n, col.index + 1), style = Penny.type.body, color = c.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (col.sample.isNotEmpty()) {
                Text(col.sample, style = Penny.type.caption, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box {
            PennyChip(roleName(col.role), col.role != CsvColumn.IGNORE, { open = true })
            DropdownMenu(open, { open = false }, containerColor = c.surface2) {
                ROLE_ORDER.forEach { r ->
                    DropdownMenuItem(
                        text = { Text(roleName(r), style = Penny.type.body, color = if (r == col.role) c.accent else c.textPrimary) },
                        onClick = { open = false; onPick(r) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun FallbackCategoryRow(categories: List<Category>, selectedId: String?, onPick: (String) -> Unit) {
    val c = Penny.colors
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(stringResource(R.string.import_fallback_title), style = Penny.type.body, color = c.textPrimary)
            Text(stringResource(R.string.import_fallback_body), style = Penny.type.caption, color = c.textSecondary)
        }
        Box {
            val name = categories.firstOrNull { it.id == selectedId }?.name ?: stringResource(R.string.import_role_category)
            PennyChip(name, true, { if (categories.isNotEmpty()) open = true })
            DropdownMenu(open, { open = false }, containerColor = c.surface2) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name, style = Penny.type.body, color = if (cat.id == selectedId) c.accent else c.textPrimary) },
                        onClick = { open = false; onPick(cat.id) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun PreviewList(rows: List<ImportPreviewRow>) {
    val c = Penny.colors
    val dates = dateText()
    PennyCard(Modifier.fillMaxWidth(), padding = 12.dp) {
        Column {
            rows.forEachIndexed { i, r ->
                if (i > 0) HorizontalDivider(color = c.hairline, thickness = 0.5.dp)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp).alpha(if (r.duplicate) 0.5f else 1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(r.label ?: dates.day(r.date), style = Penny.type.body, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val caption = if (r.duplicate) stringResource(R.string.import_duplicate_caption, dates.day(r.date)) else dates.day(r.date)
                        if (r.label != null || r.duplicate) Text(caption, style = Penny.type.caption, color = c.textSecondary)
                    }
                    Text(r.amount, style = Penny.type.bodyStrong, color = if (r.income) c.accent else c.textPrimary)
                }
            }
        }
    }
}

@Composable
internal fun ErrorList(s: ImportUi) {
    if (s.firstErrors.isEmpty()) return
    val c = Penny.colors
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        s.firstErrors.forEach { e ->
            val reason = stringResource(
                when (e.reason) {
                    CsvRowError.Reason.DATE -> R.string.import_reason_date
                    CsvRowError.Reason.AMOUNT -> R.string.import_reason_amount
                    CsvRowError.Reason.ZERO -> R.string.import_reason_zero
                    CsvRowError.Reason.NOT_EXPENSE_OR_INCOME -> R.string.import_reason_type
                },
            )
            Text(stringResource(R.string.import_error_line, e.line, reason), style = Penny.type.caption, color = c.textSecondary)
        }
        val more = s.errorCount - s.firstErrors.size
        if (more > 0) Text(pluralStringResource(R.plurals.import_more_errors, more, more), style = Penny.type.caption, color = c.textSecondary)
    }
}
