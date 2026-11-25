package com.penny.wallet.data.prefs

import kotlinx.serialization.Serializable

/** New fields need defaults so old installs and old backups still load. */
@Serializable
data class UserPrefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentArgb: Long = 0xFF39FF88,
    val background: BackgroundStyle = BackgroundStyle.DARK_GREY,
    val glow: GlowLevel = GlowLevel.SUBTLE,
    val hudGrid: Boolean = false,
    val corners: CornerStyle = CornerStyle.ROUND,
    val textSize: TextSize = TextSize.M,
    val density: Density = Density.COMFORTABLE,
    val animations: AnimationLevel = AnimationLevel.FULL,
    val haptics: Boolean = true,
    val showCents: Boolean = true,
    val currencyCode: String = "",
    val numberFormat: NumberFormatStyle = NumberFormatStyle.AUTO,
    val symbolPosition: SymbolPosition = SymbolPosition.AUTO,
    @Deprecated("Replaced by symbolPosition; kept one release so old settings still decode.")
    val symbolBefore: Boolean = false,
    val hideAmounts: Boolean = false,
    val homeSections: List<HomeSection> = HomeSection.entries.toList(),
    val hiddenHomeSections: Set<HomeSection> = emptySet(),
    val heroMetric: HeroMetric = HeroMetric.AVAILABLE,
    val recentCount: Int = 5,
    val quickAddDefaultType: QuickType = QuickType.EXPENSE,
    val fixedAccountId: String? = null,
    val chipOrder: ChipOrder = ChipOrder.MOST_USED,
    val saveOnChipTap: Boolean = true,
    val showNoteField: Boolean = true,
    val lastAccountId: String? = null,
    val lastCategoryId: String? = null,
    val defaultChart: ChartStyle = ChartStyle.DONUT,
    val historyMonths: Int = 6,
    val firstDayOfWeek: Int = 1,
    val monthStartDay: Int = 1,
    /** Monthly spending budget in minor units; 0 = none. */
    val monthlyBudget: Long = 0,
    val goalLayout: GoalLayout = GoalLayout.GRID,
    val goalSort: GoalSort = GoalSort.MANUAL,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val defaultRemindDaysBefore: Int = 1,
    /** [com.penny.wallet.domain.BillDetector.key]s the user answered "Not a bill" to. */
    val dismissedBills: Set<String> = emptySet(),
    /** The most recent CSV import, so it can still be undone after the snackbar is gone. */
    val lastImport: LastImport? = null,
    val overdueBadge: Boolean = true,
    val quietMode: Boolean = false,
    val notificationPermissionAsked: Boolean = false,
    val backupFrequency: BackupFrequency = BackupFrequency.WEEKLY,
    val backupFolderUri: String? = null,
    val backupKeep: Int = 8,
    val lastBackupAt: Long? = null,
    val biometricLock: Boolean = false,
    val lockAfter: LockAfter = LockAfter.IMMEDIATELY,
    val seeded: Boolean = false,
)

/**
 * "Reset all options": look and behaviour go back to defaults. Kept: state (backup folder, last used, permission asked,
 * seeded), the currency (amounts are stored without one), the budget (an amount, not an option), backup schedule and app lock (resetting must not silently
 * stop backups or unlock the app).
 */
fun UserPrefs.withDefaultOptions(): UserPrefs = UserPrefs().copy(
    currencyCode = currencyCode, monthlyBudget = monthlyBudget,
    backupFolderUri = backupFolderUri, lastBackupAt = lastBackupAt, backupFrequency = backupFrequency, backupKeep = backupKeep,
    biometricLock = biometricLock, lockAfter = lockAfter,
    lastAccountId = lastAccountId, lastCategoryId = lastCategoryId,
    notificationPermissionAsked = notificationPermissionAsked, seeded = seeded,
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Dark theme variant: AMOLED is pure black. */
enum class BackgroundStyle { AMOLED, DARK_GREY }
enum class GlowLevel { OFF, SUBTLE, STRONG }
enum class CornerStyle(val dp: Int) { ROUND(28), SOFTER(20), SHARP(12) }
enum class TextSize(val scale: Float) { S(0.9f), M(1f), L(1.12f) }
enum class Density { COMFORTABLE, COMPACT }
enum class AnimationLevel { FULL, REDUCED, OFF }
enum class NumberFormatStyle { AUTO, EU, US }
enum class SymbolPosition { AUTO, BEFORE, AFTER }
enum class HomeSection { HERO, MONTH, ACCOUNTS, UPCOMING, RECENT }
enum class HeroMetric { AVAILABLE, TOTAL, SPENT_MONTH }
enum class QuickType { EXPENSE, INCOME, SAVE, TRANSFER }
enum class ChipOrder { MOST_USED, MANUAL, ALPHABETICAL }
enum class ChartStyle { DONUT, BARS }
enum class GoalLayout { GRID, LIST }
enum class GoalSort { PROGRESS, DEADLINE, MANUAL }
enum class BackupFrequency(val days: Long) { DAILY(1), WEEKLY(7), MONTHLY(30) }
enum class LockAfter(val millis: Long) { IMMEDIATELY(0), ONE_MIN(60_000), FIVE_MIN(300_000) }

@Serializable
data class LastImport(val id: String, val count: Int, val createdCategoryIds: List<String> = emptyList(), val at: Long = 0)
