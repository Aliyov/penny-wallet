# Penny Wallet progress

## Phases
- [x] 1 Scaffold
- [x] 2 Domain
- [ ] 3 Home + accounts
- [ ] 4 Quick add
- [ ] 5 Expenses + charts
- [ ] 6 Savings
- [ ] 7 Recurring + notifications
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

## Notes
- Icon registry `ui/components/PennyIcons.kt`: 102 icons; keys are persisted, so never rename. Stand-ins: piggy=Toll, revolut=Payment, tax=ReceiptLong.
