# Penny Wallet progress

## Phases
- [x] 1 Scaffold
- [x] 2 Domain
- [x] 3 Home + accounts
- [x] 4 Quick add
- [x] 5 Expenses + charts
- [x] 6 Savings
- [x] 7 Recurring + notifications
- [ ] 8 Backup / restore
- [ ] 9 Customization
- [ ] 10 Widgets
- [ ] 11 Polish

## Phase plans
### 1 Scaffold
- Gradle wrapper 9.9.2, AGP 9.5.2 (built-in Kotlin), Kotlin 2.4.21, KSP 2.3.14, Compose BOM 2026.11.01, version catalog.
- Theme (§7) driven by `UserPrefs` from day one (colours, type scale, corners, density, motion, glow).
- Inter (Regular/Medium/SemiBold/Bold) bundled in `res/font`.
- Room DB: 5 entities, converters, DAOs; repositories; `AppContainer` manual DI.
- Bottom nav (Home · Expenses · Savings · Settings) + floating FAB + snackbar host, empty screens.
- Seed data on first launch (guarded by `UserPrefs.seeded`).

### 2 Domain
- `MoneyCalculator`: ledger-derived balances, single-pass `summary`, goal progress / monthly needed, `unsaveSplit`, budgeting periods with month start day, category shares, history.
- `Recurrence`: clamp-to-last-day, re-anchoring `next`, `firstOnOrAfter`, due states, worker `actions` (auto-pay catch-up, no overdue spam), due-this-month, upcoming.
- `Formatters`: EU/US, symbol position, hidden cents, parsing.
- 26 JUnit tests: all passing.

### 3 Home + accounts
- `HomeViewModel` / `AccountViewModel` combine Room flows + prefs and run `MoneyCalculator`; UI gets ready-made state.
- `TxRows` builds display rows (title, icon, colour, signed amount, tone) once for every list.
- Shared components: AnimatedAmount, MoneyText (privacy mode), Numpad, PennySheet, PennyButton, IconBadge, AmountSheet, DatePickerRow, swipeable TxRowItem.
- Home: hero (metric from prefs) + glow + HUD grid, month card, account carousel + add account, upcoming strip, recent; sections ordered/hidden from prefs; backup-warning banner.
- Account detail: balance/available/in goals, Add income · Withdraw · Set balance · Transfer, rename/icon, archive, delete (→ archive offer), history grouped by day.

### 4 Quick add
- `QuickAddViewModel`: defaults (fixed or last-used account, default type), chip order (most used + last used first / manual / A–Z), save-on-chip-tap, edit mode, Make recurring, Undo.
- Sheet UI: segmented control, big amount + numpad, chips, optional row (account · date · note · recurring).

### 5 Expenses + charts
- `ExpensesViewModel`: period switcher (respects month start day), category slices + shares, last-N-months history, day groups, category filter + note search.
- Custom Canvas donut (tap to highlight), horizontal-bars alternative, tappable history bars.

### 6 Savings
- `SavingsViewModel` / `GoalViewModel`: goal cards (progress, remaining, €/month), sort per prefs, add money (≤ available), take back (≤ goal balance), delete returns money, celebration trigger at 100%.
- ProgressRing, Celebration overlay, grid/list layouts, goal sheet (name, icon, colour, required target, optional deadline).

### 7 Recurring + notifications
- `Notifications` (channel "Reminders"; remind / due today + Mark as paid / paid automatically + Undo).
- `DailyReminderWorker` (24 h periodic at reminder time, re-scheduled on change), auto-pay catch-up at app start and boot, `BootReceiver`, `MarkPaidReceiver`.
- Recurring screen: list with due states, add/edit sheet (weekly/monthly/yearly), pause, delete + Undo, Pay now + Undo. POST_NOTIFICATIONS requested on first rule.

### 8 Backup / restore
- `BackupManager`: snapshot → JSON (schemaVersion 1), SAF folder with persisted permission, same-day overwrite, keep newest N, validate + summary, restore in one Room transaction, CSV export, reset all.
- `WeeklyBackupWorker` (frequency from prefs, battery not low) shares the Back up now path.
- Backup settings screen: folder, Back up now ✓ + time, last backup, frequency, keep N, Restore (summary + double confirm), Export CSV.

## Environment
- JDK 25 system OpenJDK is a **JRE only (no javac)**. The build uses a **JDK 21 toolchain auto-downloaded by Gradle** (foojay resolver in `settings.gradle.kts`), so no sudo is needed. Alternative: `sudo dnf install java-25-openjdk-devel`.
-  Android SDK at `~/Android/Sdk` (platforms 35/36/37 installed via sdkmanager, no sudo needed), adb at `~/Android/Sdk/platform-tools/adb` (not on PATH).

## Decisions
- **compileSdk 37.1** (`release(37) { minorApiLevel = 1 }`; Compose 1.13 requires it), targetSdk 35, minSdk 26.
- **LocalDate stored as epoch day** (Long) in Room so month ranges are integer comparisons. Enums stored by name.
- Entity class is named `Transaction` (table `transactions`); Room's `@Transaction` annotation isn't used (we use `db.withTransaction {}`), so no clash.
- Entities are `@Serializable` and double as backup DTOs; dates serialize as ISO strings.
- **UserPrefs is stored as one JSON string in DataStore**: new settings need no migration, missing keys fall back to defaults, and backups include settings trivially.
- `MainActivity` is an `AppCompatActivity` (needed later for `BiometricPrompt`).
- **Account delete**: allowed only if the account has no transactions at all (so balance is 0 and nothing is orphaned, including transfers whose other side lives in another account). Otherwise the UI offers Archive.
- **Goal delete**: money goes back via `MoneyCalculator.unsaveSplit` (each account's net contribution; proportional with largest-remainder rounding if contributions are mixed). The goal's SAVE/UNSAVE history is deleted with it, since those rows are earmarks only and don't affect account balances; Undo restores goal + history.
- **Recurring pay is idempotent by expected due date**: `pay(ruleId, expectedDue)` is a no-op if the rule has already advanced (so a double tap on "Mark as paid" can't pay next month), plus the `(recurringId, date)` check from the spec.
- **Month start day**: a period is labelled by the month it starts in (start day 27 → "November" = 27 Nov – 26 Dec). The day is clamped to the month's length.
- `monthsUntil(deadline)` rounds partial months up, minimum 1; "€X/month" rounds up to the cent.
- Hidden cents round to the nearest euro (12,60 € → 13 €).
- Bottom nav "blur": a real backdrop blur isn't possible in Compose without a third-party library, so the bar is a translucent (82%) raised surface on API 31+ and solid below.

- **Archive an account** is blocked while it has money earmarked in goals (it'd make available-to-spend wrong); the user is told to take it back first.
- Account undo-after-delete calls the repo directly (the screen's ViewModel is already gone).
- **Make recurring** (quick add) creates a MONTHLY rule on the expense's day of month, links the expense to it, and sets nextDueDate to the following month (this occurrence is already paid).
- Editing a transaction keeps its type; withdrawals/adjustments/unsaves edit amount, date and note only.

- **Charts are all custom Compose** (donut on Canvas, history bars as Compose boxes); Vico dropped. Reason: tapping a history bar switches the month, the look matches the theme exactly, and it's one dependency fewer.
- Recurring "Pay now": early → dated today; due/overdue → dated on the due date (keeps it in the right month).
- Resuming a paused rule whose date has passed jumps to the next occurrence from today (no backlog).
- Notifications respect privacy mode (amounts shown as ••••) and quiet mode (none at all; auto-pay still records).
- App start runs an auto-pay-only catch-up; reminders only come from the daily run at the user's time.

- Restoring a backup also restores settings, except device-specific ones (backup folder, last backup time, permission flag).
- Reset all data clears the ledger only; settings are kept and the seed isn't re-run.
- Recurring sheet: first account preselected; category preselected only if exactly one exists.

## Notes
- Icon registry `ui/components/PennyIcons.kt`: 102 icons; keys are persisted, so never rename. Stand-ins: piggy=Toll, revolut=Payment, tax=ReceiptLong.
