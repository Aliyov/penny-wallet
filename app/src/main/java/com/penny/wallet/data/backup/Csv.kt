package com.penny.wallet.data.backup

/** Plain RFC-4180 CSV; amounts as decimal euros with a dot so spreadsheets parse them in any locale setting. */
object Csv {
    private fun esc(s: String?): String {
        val v = s.orEmpty()
        return if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }

    fun euros(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val a = kotlin.math.abs(cents)
        return "$sign${a / 100}.${(a % 100).toString().padStart(2, '0')}"
    }

    fun transactions(b: Backup): String {
        val acc = b.accounts.associate { it.id to it.name }
        val cat = b.categories.associate { it.id to it.name }
        val goal = b.goals.associate { it.id to it.name }
        val sb = StringBuilder("date,type,amount_eur,account,to_account,category,goal,note\r\n")
        b.transactions.sortedWith(compareBy({ it.date }, { it.createdAt })).forEach { t ->
            sb.append(listOf(t.date.toString(), t.type.name, euros(t.amountCents), acc[t.accountId], t.toAccountId?.let(acc::get),
                t.categoryId?.let(cat::get), t.goalId?.let(goal::get), t.note).joinToString(",") { esc(it) }).append("\r\n")
        }
        return sb.toString()
    }
}
