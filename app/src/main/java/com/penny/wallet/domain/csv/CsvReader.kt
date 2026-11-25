package com.penny.wallet.domain.csv

/** RFC-4180 style reader that also accepts semicolon and tab files (EU spreadsheets, bank exports). */
object CsvReader {
    private val DELIMITERS = charArrayOf(',', ';', '\t')

    fun read(text: String): List<List<String>> = readWithLines(text).first

    /** Rows plus the line each row starts on in a text editor (blank lines and multi-line quotes make them differ). */
    fun readWithLines(text: String): Pair<List<List<String>>, List<Int>> {
        val body = text.removePrefix("\uFEFF")
        val starts = mutableListOf<Int>()
        var line = 1
        var rowStart = 1
        val delimiter = detectDelimiter(body)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < body.length) {
            val ch = body[i]
            when {
                quoted && ch == '"' && body.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                ch == '"' && (quoted || field.isBlank()) -> { if (!quoted) field.setLength(0); quoted = !quoted }
                quoted -> { field.append(ch); if (ch == '\n') line++ }
                ch == delimiter -> { row += field.toString().trim(); field.setLength(0) }
                ch == '\r' || ch == '\n' -> {
                    if (ch == '\r' && body.getOrNull(i + 1) == '\n') i++
                    row += field.toString().trim(); field.setLength(0)
                    if (row.any { it.isNotEmpty() }) { rows += row; starts += rowStart }
                    row = mutableListOf()
                    line++
                    rowStart = line
                }
                else -> field.append(ch)
            }
            i++
        }
        row += field.toString().trim()
        if (row.any { it.isNotEmpty() }) { rows += row; starts += rowStart }
        return rows to starts
    }

    /** The delimiter that splits the header line into the most columns, ignoring quoted text. */
    fun detectDelimiter(text: String): Char {
        val firstLine = buildString {
            var quoted = false
            for (ch in text) {
                if (ch == '"') quoted = !quoted
                if (!quoted && (ch == '\n' || ch == '\r')) break
                if (!quoted) append(ch)
            }
        }
        return DELIMITERS.maxBy { d -> firstLine.count { it == d } }.takeIf { d -> firstLine.count { it == d } > 0 } ?: ','
    }
}
