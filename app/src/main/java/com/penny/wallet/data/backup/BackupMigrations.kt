package com.penny.wallet.data.backup

/** Upgrades a decoded older backup step by step to [Backup.CURRENT_SCHEMA]. */
object BackupMigrations {
    fun upgrade(b: Backup): Backup {
        var cur = b
        // 1 → 2: categories, transactions and debts gained fields that default to "none".
        if (cur.schemaVersion == 1) cur = cur.copy(schemaVersion = 2)
        if (cur.schemaVersion != Backup.CURRENT_SCHEMA) throw BackupProblem.NotABackup()
        return cur
    }
}
