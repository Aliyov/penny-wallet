package com.penny.wallet.data.prefs

import kotlinx.serialization.Serializable

/** Every user-facing visual/behaviour choice (SPEC §7a). Defaults = the spec's defaults. */
@Serializable
data class UserPrefs(
    // Appearance
    val accentArgb: Long = 0xFF39FF88,
    val background: BackgroundStyle = BackgroundStyle.AMOLED,
    val glow: GlowLevel = GlowLevel.SUBTLE,
    val hudGrid: Boolean = true,
    val corners: CornerStyle = CornerStyle.ROUND,
    val textSize: TextSize = TextSize.M,
    val density: Density = Density.COMFORTABLE,
    val animations: AnimationLevel = AnimationLevel.FULL,
    val haptics: Boolean = true,
    val showCents: Boolean = true,
    val numberFormat: NumberFormatStyle = NumberFormatStyle.EU,
    val symbolBefore: Boolean = false,
    val hideAmounts: Boolean = false,
    // Home
    val homeSections: List<HomeSection> = HomeSection.entries.toList(),
    val hiddenHomeSections: Set<HomeSection> = emptySet(),
    val heroMetric: HeroMetric = HeroMetric.AVAILABLE,
    val recentCount: Int = 5,
    // Quick add
    val quickAddDefaultType: QuickType = QuickType.EXPENSE,
    val fixedAccountId: String? = null,
    val chipOrder: ChipOrder = ChipOrder.MOST_USED,
    val saveOnChipTap: Boolean = true,
    val showNoteField: Boolean = true,
    val lastAccountId: String? = null,
    val lastCategoryId: String? = null,
    // Expenses & charts
    val defaultChart: ChartStyle = ChartStyle.DONUT,
    val historyMonths: Int = 6,
    val firstDayOfWeek: Int = 1,
    val monthStartDay: Int = 1,
    // Savings
    val goalLayout: GoalLayout = GoalLayout.GRID,
    val goalSort: GoalSort = GoalSort.MANUAL,
    // Reminders
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val defaultRemindDaysBefore: Int = 1,
    val overdueBadge: Boolean = true,
    val quietMode: Boolean = false,
    val notificationPermissionAsked: Boolean = false,
    // Backup
    val backupFrequency: BackupFrequency = BackupFrequency.WEEKLY,
    val backupFolderUri: String? = null,
    val backupKeep: Int = 8,
    val lastBackupAt: Long? = null,
    // Security
    val biometricLock: Boolean = false,
    val lockAfter: LockAfter = LockAfter.IMMEDIATELY,
    // Internal
    val seeded: Boolean = false,
)

enum class BackgroundStyle { AMOLED, DARK_GREY }
enum class GlowLevel { OFF, SUBTLE, STRONG }
enum class CornerStyle(val dp: Int) { ROUND(28), SOFTER(20), SHARP(12) }
enum class TextSize(val scale: Float) { S(0.9f), M(1f), L(1.12f) }
enum class Density { COMFORTABLE, COMPACT }
enum class AnimationLevel { FULL, REDUCED, OFF }
enum class NumberFormatStyle { EU, US }
enum class HomeSection { HERO, MONTH, ACCOUNTS, UPCOMING, RECENT }
enum class HeroMetric { AVAILABLE, TOTAL, SPENT_MONTH }
enum class QuickType { EXPENSE, INCOME, SAVE, TRANSFER }
enum class ChipOrder { MOST_USED, MANUAL, ALPHABETICAL }
enum class ChartStyle { DONUT, BARS }
enum class GoalLayout { GRID, LIST }
enum class GoalSort { PROGRESS, DEADLINE, MANUAL }
enum class BackupFrequency(val days: Long) { DAILY(1), WEEKLY(7), MONTHLY(30) }
enum class LockAfter(val millis: Long) { IMMEDIATELY(0), ONE_MIN(60_000), FIVE_MIN(300_000) }
