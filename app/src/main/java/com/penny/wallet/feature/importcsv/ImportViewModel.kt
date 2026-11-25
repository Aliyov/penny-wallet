package com.penny.wallet.feature.importcsv

import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.penny.wallet.AppContainer
import com.penny.wallet.R
import com.penny.wallet.core.money.Formatters
import com.penny.wallet.core.ui.userMessage
import com.penny.wallet.data.db.entities.Account
import com.penny.wallet.data.db.entities.Category
import com.penny.wallet.data.prefs.LastImport
import com.penny.wallet.data.prefs.UserPrefs
import com.penny.wallet.domain.csv.CsvColumn
import com.penny.wallet.domain.csv.CsvDate
import com.penny.wallet.domain.csv.CsvImport
import com.penny.wallet.domain.csv.CsvMapping
import com.penny.wallet.domain.csv.CsvReader
import com.penny.wallet.domain.csv.CsvRow
import com.penny.wallet.domain.csv.CsvRowError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.time.LocalDate
import java.util.Locale

data class ImportColumn(val index: Int, val title: String?, val sample: String, val role: CsvColumn)

data class ImportPreviewRow(val line: Int, val date: LocalDate, val label: String?, val amount: String, val income: Boolean, val duplicate: Boolean)

data class ImportUi(
    val loading: Boolean = false,
    val fileName: String? = null,
    val loadError: String? = null,
    val loaded: Boolean = false,
    val columns: List<ImportColumn> = emptyList(),
    val missingDate: Boolean = false,
    val missingAmount: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val accountId: String? = null,
    val categories: List<Category> = emptyList(),
    val fallbackCategoryId: String? = null,
    val createMissing: Boolean = true,
    val skipDuplicates: Boolean = true,
    val importCount: Int = 0,
    val duplicateCount: Int = 0,
    val errorCount: Int = 0,
    val rows: List<ImportPreviewRow> = emptyList(),
    val firstErrors: List<CsvRowError> = emptyList(),
    val busy: Boolean = false,
) {
    val canImport: Boolean
        get() = loaded && !busy && !missingDate && !missingAmount && importCount > 0 && accountId != null && fallbackCategoryId != null
}

private data class ImportInput(
    val loading: Boolean = false,
    val fileName: String? = null,
    val loadError: String? = null,
    val table: List<List<String>>? = null,
    /** Text-editor line of each table row, for error messages. */
    val lines: List<Int>? = null,
    val mapping: CsvMapping? = null,
    val accountId: String? = null,
    val fallbackCategoryId: String? = null,
    val createMissing: Boolean = true,
    val skipDuplicates: Boolean = true,
    val busy: Boolean = false,
)

class ImportViewModel(private val c: AppContainer) : ViewModel() {
    private val input = MutableStateFlow(ImportInput())
    @Volatile private var rowsToImport: List<CsvRow> = emptyList()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ImportUi> = combine(input, c.settings.prefs, c.accounts.observe(), c.categories.observe()) { i, p, a, cats ->
        Sources(i, p, a.filter { !it.archived }, cats.filter { !it.archived })
    }.mapLatest { compute(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ImportUi())

    private data class Sources(val i: ImportInput, val p: UserPrefs, val accounts: List<Account>, val categories: List<Category>)

    private suspend fun compute(s: Sources): ImportUi {
        val i = s.i
        val accountId = i.accountId?.takeIf { id -> s.accounts.any { it.id == id } }
            ?: s.p.lastAccountId?.takeIf { id -> s.accounts.any { it.id == id } }
            ?: s.accounts.firstOrNull()?.id
        val fallbackId = i.fallbackCategoryId?.takeIf { id -> s.categories.any { it.id == id } }
            ?: s.categories.firstOrNull { it.name.trim().lowercase() in OTHER_NAMES }?.id
            ?: s.categories.firstOrNull()?.id
        val base = ImportUi(
            loading = i.loading, fileName = i.fileName, loadError = i.loadError, accounts = s.accounts, accountId = accountId,
            categories = s.categories, fallbackCategoryId = fallbackId, createMissing = i.createMissing,
            skipDuplicates = i.skipDuplicates, busy = i.busy,
        )
        val table = i.table
        val m = i.mapping
        if (table == null || m == null) {
            rowsToImport = emptyList()
            return base
        }
        val preview = CsvImport.preview(table, m, Formatters.fractionDigits(s.p.currencyCode.ifEmpty { Formatters.DEFAULT_CURRENCY }), i.lines)
        val dupes = if (accountId == null) emptySet() else CsvImport.duplicates(preview.rows, c.transactions.all(), accountId)
        val toImport = if (i.skipDuplicates) preview.rows.filter { it.line !in dupes } else preview.rows
        rowsToImport = toImport
        val fmt = Formatters.of(s.p, Locale.getDefault())
        val catNames = s.categories.associateBy({ it.id }, { it.name })
        val hasAmount = m.indexOf(CsvColumn.AMOUNT) != null || m.indexOf(CsvColumn.DEBIT) != null || m.indexOf(CsvColumn.CREDIT) != null
        return base.copy(
            loaded = true,
            columns = columnsOf(table, m),
            missingDate = m.indexOf(CsvColumn.DATE) == null || m.datePattern == null,
            missingAmount = !hasAmount,
            importCount = toImport.size,
            duplicateCount = if (i.skipDuplicates) dupes.size else 0,
            errorCount = preview.errors.size,
            rows = preview.rows.take(PREVIEW_ROWS).map { r ->
                ImportPreviewRow(
                    r.line, r.date, r.note ?: r.category ?: if (r.income) null else fallbackId?.let(catNames::get),
                    Formatters.money(if (r.income) r.amountCents else -r.amountCents, fmt, signed = true),
                    r.income, r.line in dupes,
                )
            },
            firstErrors = preview.errors.take(5),
        )
    }

    private fun columnsOf(table: List<List<String>>, m: CsvMapping): List<ImportColumn> {
        val width = table.maxOfOrNull { it.size } ?: 0
        val header = if (m.hasHeader) table.firstOrNull() else null
        val firstData = table.getOrNull(if (m.hasHeader) 1 else 0).orEmpty()
        return (0 until width).map { idx ->
            ImportColumn(idx, header?.getOrNull(idx)?.trim()?.ifEmpty { null }, firstData.getOrNull(idx)?.trim().orEmpty(), m.columns[idx] ?: CsvColumn.IGNORE)
        }
    }

    fun load(uri: Uri) = viewModelScope.launch {
        input.update { it.copy(loading = true, loadError = null) }
        val result = withContext(Dispatchers.IO) { runCatching { readTable(uri) } }
        result.fold(
            onSuccess = { (name, read) ->
                val (table, lines) = read
                if (table.isEmpty()) {
                    input.update { it.copy(loading = false, loadError = c.context.getString(R.string.import_error_empty), fileName = null, table = null, lines = null, mapping = null) }
                } else {
                    input.update { it.copy(loading = false, fileName = name, table = table, lines = lines, mapping = CsvImport.guessMapping(table)) }
                }
            },
            onFailure = { e ->
                val msg = if (e is FileTooBigException) c.context.getString(R.string.import_error_too_big) else c.context.getString(R.string.import_error_read)
                if (e !is FileTooBigException) android.util.Log.w("Penny", "CSV import read failed", e)
                // Never leave the previous file importable behind an error.
                input.update { it.copy(loading = false, loadError = msg, fileName = null, table = null, lines = null, mapping = null) }
            },
        )
    }

    private fun readTable(uri: Uri): Pair<String?, Pair<List<List<String>>, List<Int>>> {
        val resolver = c.context.contentResolver
        val name = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cur ->
                if (cur.moveToFirst()) cur.getString(0) else null
            }
        }.getOrNull()
        val bytes = resolver.openInputStream(uri)?.use { s ->
            val out = java.io.ByteArrayOutputStream()
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = s.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                if (out.size() > MAX_BYTES) throw FileTooBigException()
            }
            out.toByteArray()
        } ?: throw java.io.IOException("No stream")
        var text = String(bytes, Charsets.UTF_8)
        // German bank exports are often windows-1252; invalid UTF-8 shows up as replacement characters.
        if ('\uFFFD' in text) text = String(bytes, Charset.forName("windows-1252"))
        return name to CsvReader.readWithLines(text.removePrefix("\uFEFF"))
    }

    fun setColumn(index: Int, role: CsvColumn) = input.update { i ->
        val m = i.mapping ?: return@update i
        val table = i.table ?: return@update i
        val exclusive = role != CsvColumn.IGNORE
        val conflicting = when (role) {
            CsvColumn.AMOUNT -> setOf(CsvColumn.AMOUNT, CsvColumn.DEBIT, CsvColumn.CREDIT)
            CsvColumn.DEBIT, CsvColumn.CREDIT -> setOf(role, CsvColumn.AMOUNT)
            else -> if (exclusive) setOf(role) else emptySet()
        }
        val cols = m.columns.filter { (k, v) -> k != index && v !in conflicting }.toMutableMap()
        if (role != CsvColumn.IGNORE) cols[index] = role
        val dateIdx = cols.entries.firstOrNull { it.value == CsvColumn.DATE }?.key
        val pattern = when {
            dateIdx == null -> null
            role == CsvColumn.DATE -> {
                val rows = if (m.hasHeader) table.drop(1) else table
                CsvDate.detect(rows.mapNotNull { it.getOrNull(index) }.filter { it.isNotBlank() })
            }
            else -> m.datePattern
        }
        i.copy(mapping = m.copy(columns = cols, datePattern = pattern))
    }

    fun setAccount(id: String) = input.update { it.copy(accountId = id) }
    fun setFallbackCategory(id: String) = input.update { it.copy(fallbackCategoryId = id) }
    fun setCreateMissing(on: Boolean) = input.update { it.copy(createMissing = on) }
    fun setSkipDuplicates(on: Boolean) = input.update { it.copy(skipDuplicates = on) }

    fun import(onDone: (LastImport) -> Unit, onError: (String) -> Unit) {
        val s = state.value
        val accountId = s.accountId ?: return
        val fallbackId = s.fallbackCategoryId ?: return
        val rows = rowsToImport
        if (!s.canImport || rows.isEmpty()) return
        input.update { it.copy(busy = true) }
        // App scope: leaving the screen mid-import must not cut off recording it as undoable.
        c.appScope.launch {
            val result = runCatching { c.imports.import(rows, accountId, fallbackId, s.createMissing) }
            withContext(Dispatchers.Main) {
                // After the screen is gone its callbacks would navigate the wrong screen; the import is still undoable in Settings.
                if (cleared) return@withContext
                result.onSuccess { onDone(it) }
                    .onFailure { e -> input.update { it.copy(busy = false) }; onError(c.userMessage(e)) }
            }
        }
    }

    private var cleared = false

    override fun onCleared() { cleared = true }

    fun undo(last: LastImport) {
        c.appScope.launch { runCatching { c.imports.undo(last) }.onFailure { android.util.Log.w("Penny", "Undo import failed", it) } }
    }

    private class FileTooBigException : Exception()

    companion object {
        private const val MAX_BYTES = 5 * 1024 * 1024
        private const val PREVIEW_ROWS = 30
        private val OTHER_NAMES = setOf("other", "others", "sonstiges", "altro", "altri")
    }
}
