package com.penny.wallet.data.backup

/**
 * Plain RFC-4180 CSV; amounts as decimals with a dot so spreadsheets parse them in any locale setting, with as many
 * decimals as the currency has (yen none, dinar three).
 */
object Csv {
    private fun esc(s: String?): String {
        val v = s.orEmpty()
        return if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }

    fun decimal(minor: Long, fractionDigits: Int): String =
        java.math.BigDecimal.valueOf(minor).movePointLeft(fractionDigits).setScale(fractionDigits).toPlainString()

    fun transactions(b: Backup, outsideLabel: String = "Outside"): String {
        val acc = b.accounts.associate { it.id to it.name } + (com.penny.wallet.domain.MoneyCalculator.EXTERNAL to outsideLabel)
        val cat = b.categories.associate { it.id to it.name }
        val goal = b.goals.associate { it.id to it.name }
        val currency = b.settings?.currencyCode?.ifEmpty { null } ?: com.penny.wallet.core.money.Formatters.DEFAULT_CURRENCY
        val digits = com.penny.wallet.core.money.Formatters.fractionDigits(currency)
        val sb = StringBuilder("date,type,amount,currency,account,to_account,category,goal,note\r\n")
        b.transactions.sortedWith(compareBy({ it.date }, { it.createdAt })).forEach { t ->
            sb.append(listOf(t.date.toString(), t.type.name, decimal(t.amountCents, digits), currency, acc[t.accountId], t.toAccountId?.let(acc::get),
                t.categoryId?.let(cat::get), t.goalId?.let(goal::get), t.note).joinToString(",") { esc(it) }).append("\r\n")
        }
        return sb.toString()
    }
}
