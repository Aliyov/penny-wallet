package com.penny.wallet.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class UserPrefsTest {
    private val custom = UserPrefs(
        themeMode = ThemeMode.LIGHT, accentArgb = 0xFF123456, hudGrid = true, textSize = TextSize.L,
        currencyCode = "CHF", monthStartDay = 27, reminderHour = 20, quietMode = true,
        backupFrequency = BackupFrequency.DAILY, backupFolderUri = "content://folder", backupKeep = 3, lastBackupAt = 42L,
        biometricLock = true, lockAfter = LockAfter.FIVE_MIN,
        lastAccountId = "a1", lastCategoryId = "c1", notificationPermissionAsked = true, seeded = true,
    )

    @Test fun resetRestoresOptionDefaults() {
        val r = custom.withDefaultOptions()
        val d = UserPrefs()
        assertEquals(d.themeMode, r.themeMode)
        assertEquals(d.accentArgb, r.accentArgb)
        assertEquals(d.hudGrid, r.hudGrid)
        assertEquals(d.textSize, r.textSize)
        assertEquals(d.monthStartDay, r.monthStartDay)
        assertEquals(d.reminderHour, r.reminderHour)
        assertEquals(d.quietMode, r.quietMode)
    }

    @Test fun resetKeepsCurrencyBackupLockAndState() {
        val r = custom.withDefaultOptions()
        assertEquals("CHF", r.currencyCode)
        assertEquals(BackupFrequency.DAILY, r.backupFrequency)
        assertEquals("content://folder", r.backupFolderUri)
        assertEquals(3, r.backupKeep)
        assertEquals(42L, r.lastBackupAt)
        assertEquals(true, r.biometricLock)
        assertEquals(LockAfter.FIVE_MIN, r.lockAfter)
        assertEquals("a1", r.lastAccountId)
        assertEquals("c1", r.lastCategoryId)
        assertEquals(true, r.notificationPermissionAsked)
        assertEquals(true, r.seeded)
    }

    @Test fun newInstallsHaveNoHudGrid() = assertEquals(false, UserPrefs().hudGrid)
}
