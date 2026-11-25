# Penny Wallet progress

## Phases
- [x] 1 Scaffold
- [x] 2 Domain
- [x] 3 Home + accounts
- [x] 4 Quick add
- [x] 5 Expenses + charts
- [x] 6 Savings
- [x] 7 Recurring + notifications
- [x] 8 Backup / restore
- [x] 9 Customization
- [x] 10 Widgets
- [x] 11 Polish
- [x] 12 Save-button fix + receipt scan (1.3)
- [x] 13 Receipt scan works in release + OCR-slip tolerance (1.4)
- [x] 14 New app icon (1.5)

## Production roadmap (see docs/ARCHITECTURE.md §14)
- [x] P1 Foundations: package move, comment cleanup, strings EN/DE/IT, light/dark/system theme, currency choice, clean first run (no demo data), CI
- [x] P2 Simplify: essential settings + More options, leaner Home, plain wording, empty states (checked on phone 2026-11-24: settings, light/dark, EN/DE/IT)
- [x] P3 Features: monthly budget, "this month" insight, smarter receipt scan, category suggestions (ocr-smoke OK, budget + insight checked on phone)
- [x] P4 Support & trust: Support Penny (Play Billing tips), encrypted backups, crash report by email (phone check pending; tips testable only from a Play test track)
- [ ] P5 Play readiness: upload key + AAB, accessibility, store listing EN/DE/IT, privacy policy, Data safety (code + texts done 2026-11-24; waiting for Play Console setup, store graphics, key backup)
- [x] P5a Compete (before the closed test, done 2026-11-25): test infrastructure, language row, CSV import, category budgets, bill detection, shortcuts + Quick Settings tile, "Your data stays here", split expenses, debts, budget widget
- [x] P5b Compete (done before the closed test, 2026-11-25): all-time search, year overview, calendar, FR/ES/PT/PL/TR, monthly summary, tablet/foldable layout
- [ ] P6 Launch: closed test (12 testers / 14 days), production release

## Phase plans
### P1 Foundations
- Package move to `core/`, `feature/`, `platform/` (ARCHITECTURE §3), behaviour-neutral commit.
- Comment cleanup (§11): drop comments that restate code, banners, spec-section references.
- `ThemeMode` SYSTEM/LIGHT/DARK (default SYSTEM) with a light palette; "pure black" replaces AMOLED/dark grey; status bar icons follow the theme.
- Currency: `UserPrefs.currencyCode` (suggested from the device region), fraction digits from `java.util.Currency`, locale-aware separators; changing it never converts amounts.
- Strings: every UI string to `strings.xml` with `values-de` / `values-it`, plurals, locale-aware dates, `locales_config.xml`.
- Clean first run: no seed; empty Home card "Add your first account" + "Try with sample data" (removable).
- CI: GitHub Actions with lint, unit tests, `bundleRelease`, merged-manifest permission check, missing-translation check.

### P5a / P5b Compete (user decision 2026-11-24: features 1–10, 12, 14–17; not tags, not receipt photos)
Order (each step is one commit, and each must pass the full gate below before the next starts):
1. ✅ (2026-11-25) **Test infrastructure first:** Robolectric + Room testing + Compose UI tests on the JVM; a migration test from the exported v1 schema; backup fixtures (v1 JSON, encrypted v1) that must keep restoring; a 20 000-entry data generator with time limits on summary, search and Home state; CI runs them all.
2. ✅ (2026-11-25) **One database change for all new data (Room v1 → v2, backup schema 2):** `Category.monthlyBudget`, `Transaction.splitId`, `Transaction.importId`, new `Debt` table. A single migration, tested from v1 with real-looking data; v1 backups restore into v2; v2 → v1 restore is refused with a clear message.
3. ✅ (2026-11-25) Small items: Language row (opens the system per-app language screen on Android 13+, in-app list below), "Your data stays here" screen, app shortcuts + Quick Settings tile, budget widget.
4. ✅ (2026-11-25) Category budgets (domain `Budget` per category, Expenses bars, edit in category sheet).
5. ✅ (2026-11-25) Bill detection (`domain/BillDetector`: same payee/category and similar amount on a monthly rhythm, ≥3 occurrences, suggestion dismissible forever).
6. ✅ (2026-11-25) Split expenses (one receipt → several categories sharing a `splitId`; edit/delete/undo act on the whole split; totals unchanged).
7. ✅ (2026-11-25) Debts ("I owe" / "owed to me", partial repayments, optional reminder; not linked to account balances unless the user records the payment).
8. ✅ (2026-11-25) CSV import (parser for comma/semicolon/tab, quotes, EU/US numbers, several date formats, column mapping with preview, duplicate detection, whole import undoable via `importId`; profiles for Money Manager, Monefy, Wallet and Penny's own CSV).
Step 3 plan (2026-11-25):
- Language row in Settings (Phone, English, Deutsch, Italiano; becomes chips once P5b adds more) via `AppCompatDelegate.setApplicationLocales`, which uses the system per-app language on Android 13+ and is stored by AppCompat below that.
- "Your data stays here" screen from Settings: no internet permission, data only on this phone, backups only where the user picks, crash reports only by the user's own email, tips through Google Play.
- Static app shortcuts "Add expense" and "Scan receipt" (opens Quick add and starts the camera); Quick Settings tile "Add expense". All go through one `MainActivity` intent path.
- Budget widget (Glance): monthly budget left, spent bar, per-day amount; "Set a monthly budget" when none.
- Tests: intent routing, widget data on budget edge cases, language list; strings EN/DE/IT.
Step 4 plan (2026-11-25):
- Domain `Budget.forCategories`: one `BudgetStatus` per category with a budget, from the same rule as the monthly budget and the same period (month start day).
- Expenses: a budgeted category's row shows a bar of budget used (accent / warning when fast / danger when over) and "€X left" or "€X over" instead of its share; budgeted categories without spending yet are listed too, but stay out of the chart.
- Category sheet: "Monthly budget" row (set via the amount sheet, remove); stored in `Category.monthlyBudgetCents`, already in DB v2 and backups.
- Tests: domain edge cases (no budget, zero, exact, over, start day 27, past month, archived), Expenses state, sheet save/remove (UI test).
P5b: all-time search with amount/date filters; year overview; calendar; FR/ES/PT/PL/TR; opt-in monthly summary on the 1st; tablet/foldable layouts (list-detail, two panes ≥600dp).
P5b plan (user 2026-11-25: "lets finish product then i will give to testers" — P5b before the closed test):
1. ✅ (2026-11-25) All-time search: text over note/category/account, filters for period, amount range and kind; results by day with count and sum; tap edits.
2. ✅ (2026-11-25) Year overview: 12 months of spent/income/saved, top categories of the year, year switcher.
3. ✅ (2026-11-25) Calendar: month grid with daily spending, tap a day for its entries; respects the first day of week.
4. ✅ (2026-11-25) Monthly summary: opt-in notification on the 1st with last month's spending vs the month before and the top category.
5. ✅ (2026-11-25) Tablet/foldable: two panes at ≥600dp (list + detail) for Home/Expenses, nav rail instead of the bottom bar.
6. ✅ (2026-11-25) FR, ES, PT, PL, TR translations last, once every string is final; translation check extended to all locales.

**Test gate per step (nothing moves on with a red item):**
- Domain: JUnit for every rule incl. edge cases (month ends, leap years, start day 27, zero/three-decimal currencies, negative and huge amounts, empty data).
- Parsers (CSV, receipt): table tests with real-world samples + a fuzz test (thousands of random/garbled inputs must never crash, only return errors).
- Data: Room migration tests, backup restore of every older fixture, round trip backup → restore → identical data.
- UI (Robolectric Compose): first run, add/edit/undo expense, split, budget, debt, import with preview, backup/restore.
- Performance: 20 000 entries, Home state and search within set limits on the JVM; on the phone, the smoke app with the same data must stay smooth.
- Review: `/code-review` at high effort on each step's diff before committing; every finding fixed or explained in Decisions.
- Phone: release build installed, `ocr-smoke.sh`, the step's screens checked in light + dark and EN/DE/IT on the sample-data smoke install (never the real app's data).
- CI on GitHub green before the next step.

### P5 Play readiness
- targetSdk 36 (Play requirement), version 2.0 (code 7).
- Upload key `~/penny-wallet-keys/upload.jks`, passwords in `~/.gradle/gradle.properties`; bundle signed only with `-Ppenny.signUpload=true`.
- Accessibility: labelled month arrows and colour swatches, 48dp targets on small icons, selectable roles on pickers, button role on text links.
- `docs/PRIVACY.md` (EN/DE/IT), `docs/store/listing.md` (EN/DE/IT), `docs/store/play-console.md` (Data safety, content rating, products, release steps).
- Store screenshots from a separate sample-data install (smoke package), never real data.

### P4 Support & trust
- Encrypted backups: optional password (Backup screen), AES-GCM with PBKDF2 key, `format` marker in a JSON envelope so restore asks for the password; plain backups still restore.
- Support Penny: Play Billing consumables `tip_tea` (€2, "Buy me a tea") and `tip_cappuccino` (€4) behind `TipRepository`; Settings → Support Penny.
- Crash report by email to aliyov.code@proton.me: stack trace saved on crash, Home offers to send it via the mail app on next start.

### P3 Features
- `domain/Budget`: one monthly budget over all expenses (`UserPrefs.monthlyBudget`, minor units, 0 = none); left, per day, pace (on track / faster than planned / over).
- `domain/Insights`: spending so far vs the same number of days last period, plus the biggest category; shown in the Home month card.
- Settings → Your money → Monthly budget (amount sheet, Remove budget).
- Receipt scan reads the shop name (header rows) and date; Accept fills note, recent date and the category last used for that shop.
- `domain/CategorySuggest`: learned shop → category; time-of-day habit puts the likely category first (Most used order only).
- Unit tests for all four; EN/DE/IT strings.

### P2 Simplify
- Settings main screen: "Your money" (Accounts, Categories, Recurring bills) + essentials inline (Theme, Accent, Currency, Month starts on, Reminder time, Backup, App lock) + one "More options" row. Support Penny comes in P4.
- "Personalise" becomes "More options": everything else (look details, Home layout, Quick add, Charts, Savings, reminder/backup details, number format), no duplicates of the essentials. "Erase all data" moves to the bottom of More options.
- "Reset all options" keeps the currency (bug: it fell back to EUR).
- Leaner Home for new installs: HUD grid off by default; upcoming only when there are bills (already); no other section removed.
- Shared `EmptyState` (one sentence + one button) on every list: Home recent, account history, expenses, goal history, savings, categories, accounts, recurring.
- Plain wording pass over EN strings, then DE/IT to match.
- Tests for the prefs reset; verifier + translation check.

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

### 9 Customization
- `UserPrefs` (all §7a options) already drives theme/motion/formatting/Home/Quick add/charts/savings since phase 1.
- Settings hub, Categories (reorder, edit, archive, delete → move/archive), Accounts (reorder, archive, delete → archive offer), Personalise (every §7a option with per-row reset + Reset all), Reset all data (double confirm).

### 10 Widgets
- Glance `BalanceWidget` (responsive 2×1 → 4×2: available, spent this month, next recurring) and `QuickAddWidget` (1×1 / 2×1, deep link to Quick add).
- `WidgetUpdater`: debounced `updateAll` on every repository write + widget-relevant settings; privacy mode respected.

### 11 Polish
- Splash screen (core-splashscreen) held until settings load, so the theme never flashes defaults.
- Biometric / device-credential app lock with lock-after delay (ProcessLifecycleOwner); never locks out a phone without any lock method.

### 12 Save-button fix + receipt scan (1.3)
- Bug: with "Save on chip tap" on (default) the expense Save button was hidden, so picking the category before typing the amount left no way to save. The Save button is now always shown; a chip tap still saves instantly when an amount is already typed.
- Receipt scan: "Scan receipt" chip (expense, new entries) → Take photo (system camera app via FileProvider, no CAMERA permission) or Choose from gallery (photo picker).
- `data/ocr/ReceiptReader`: ML Kit Text Recognition (bundled Latin model, offline), image decoded upright and capped at 3000 px; temp photo deleted after reading.
- `domain/receipt` (pure Kotlin, unit-tested): `AmountExtractor` (all number formats, ignores dates/%/weights/negatives, fixes O→0), `ReceiptKeywords` (multilingual total / card / subtotal / tax / cash / change / tip / discount vocabulary), `ReceiptParser` (deskewed row grouping, labelled-row voting, largest-amount fallback).
- 130 parser unit tests (157 total): amount formats, multilingual receipts, tilted/split OCR geometry, edge cases. Fixed from them: US suggested-tip tables ("18% = … (Total …)"), "5, 5O" OCR slips, "-£0.75" discounts, "Saving"/"Uw voordeel" lines, "2,99 €/kg" unit prices.
- Release APK limited to arm64-v8a + armeabi-v7a (ML Kit native libs): 47 MB → 24 MB. versionCode 4 / 1.3.
- Not smoke-tested on a device: no phone connected and the local emulator segfaults at boot. (This let the 1.3 R8 bug through: see phase 13.)
- Confirm dialog: "Add €X to expense?", editable amount, other amounts as chips, Decline · Retake · Accept. Accept fills the amount; the user still picks the category.

### 13 Receipt scan works in release + OCR-slip tolerance (1.4)
- Root cause of "can't read the receipt" in 1.3: R8 broke ML Kit (NPE in `InputImage.fromBitmap`, release only). Fixed with keep rules in `proguard-rules.pro`; scan errors are now logged (tag PennyReceipt).
- Found with an on-phone probe on a real Carrefour receipt: the condensed total line was read as "TUTALE OUPLKSO" / "308".
- Parser: fuzzy "total" words (`TOTAL_FUZZY`, edit distance 1, 2 for 8+ letters, digit slips like T0TAL); a lost-comma total ("308") counts when the same amount appears elsewhere, otherwise it is offered but not confident; the trailing VAT rate is dropped in item rows ("0,49 22,00"); a fuzzy-only leader loses to a card line; plural discount words ("Sconti").
- Tests: real phone capture as `RealReceiptRegressionTest` + 54 rule tests (211 total).
- `smoke` build type (release + R8 + probe, separate app id `com.penny.wallet.smoke`) and `scripts/ocr-smoke.sh`: runs the real OCR on the connected phone against `Test_Recipe/*` and checks `Test_Recipe/expected.txt`. **Run it before every release that touches OCR.** Receipt photos are git-ignored (public repo).

### 14 New app icon (1.5)
- Source: `App_Icon.png` (1254² with its own rounded square on white). The adaptive foreground (`mipmap-*/ic_launcher_foreground.png`) is the wallet artwork on a navy gradient fitted from the image (least-squares, residual ≈2/255), so circle/squircle masks never show the white corners. Wallet ≈50 dp of 108 dp, inside the safe zone.
- Themed icon (Android 13+): hand-built vector `drawable/ic_launcher_monochrome.xml` (wallet, coins, chart bars), because a silhouette derived from the painting was ragged.
- The splash screen uses the same foreground. The old vector icon drawables were removed.

## Environment
- JDK 25 system OpenJDK is a **JRE only (no javac)**. The build uses a **JDK 21 toolchain auto-downloaded by Gradle** (foojay resolver in `settings.gradle.kts`), so no sudo is needed. Alternative: `sudo dnf install java-25-openjdk-devel`.
-  Android SDK at `~/Android/Sdk` (platforms 35/36/37 installed via sdkmanager, no sudo needed), adb at `~/Android/Sdk/platform-tools/adb` (not on PATH).

## Decisions
- **Logo v2 + opening animation (user, 2026-11-25):** new `app_icon.jpg` (wallet mark + "Penny Wallet" wordmark) replaces `app_icon.png`. Launcher icon uses only the wallet mark (the wordmark is unreadable at icon size) on a pure black background, fitted inside the 66dp safe circle; monochrome layer has the bars cut out; store icon regenerated. The user picked option A of two previewed animations: the system splash shows the empty wallet, then a Compose overlay (`feature/intro/SplashIntro.kt`, layers cut from the source in `drawable-nodpi/splash_intro_*`) lets the notes rise, the bars grow, the wordmark fade in, and fades into the app (1.5 s). Android draws the splash icon 288dp wide on every version (12+: 192dp view × 1.5), so the overlay takes only the icon's centre from the exit listener and uses 288dp; a mismatch showed as a jump on the phone. Plays only on a plain cold start (not for widget/shortcut launches or recreation), not with the in-app animations set below Full or the system's "Remove animations"; the app-lock prompt waits until it ends; a 1.5 s fallback starts it if the system never calls the exit listener. Checked on the phone by screen recording.
- **New app icon (user, 2026-11-25):** from the user's `app_icon.png` (artwork on a rounded near-black square on white). Used as-is it would sit as a square inside the launcher's mask ("sticker"), so it is split the adaptive-icon way: the artwork alone, cut out of its #07090F background, on a transparent foreground at 40dp of 108dp (same proportion as in the source, inside the 66dp safe circle); a full-bleed #07090F background, so the launcher shape is the icon's edge; a monochrome layer from the same artwork for Android 13+ themed icons (replaces the old vector silhouette). The splash uses the same background colour and the frameless artwork (120dp of 288dp), so opening the app looks like the icon filling the screen. `docs/store/icon-512.png` is the full-bleed Play Store icon. Checked on the phone.
- **CI flake (2026-11-25):** UI tests waited for "Bank" with a case-insensitive substring match, which the welcome card's "your bank account" satisfied before sample data existed; on slow CI runners the next step then timed out. Exact match now, and 60 s timeouts.
- **P5b step 6 languages FR, ES, PT, PL, TR (2026-11-25):**
  - Translated by five parallel translator agents from the EN source with DE/IT as reference and a shared glossary; informal address everywhere (tu/tú/você/ty/sen) like DE "du". Portuguese is Brazilian wording in the generic `values-pt`; Spanish is neutral (e.g. "Mostrar decimales"); Polish has real one/few/many/other forms and gender-neutral debt labels; Turkish avoids suffixes on placeholders.
  - `check-translations.py` now covers all seven languages, takes a locale argument, checks each language's CLDR plural forms (Polish few/many, "many" for it/fr/es/pt) and flags unescaped apostrophes. Italian gained missing "many" forms.
  - Strings that receive a relative day ("Today", "Yesterday") use the "Label: %1$s" form in every language, so nothing reads "due on Today" / "dal Ieri" (fixed in DE/IT too). French/Portuguese "one" covers 0: plurals that receive the number show it; "days left" (number drawn separately, never 0) is marked `tools:ignore="ImpliedQuantity"`.
  - User report (2026-11-25): in DE/IT/ES/PT/PL/TR the bottom bar icons were misaligned — tabs sized to their label and long ones ("Einstellungen", "Impostazioni") pushed neighbours and could wrap. Now equal-width slots, one-line labels that shrink slightly (to 75 %) instead of wrapping; checked on the phone in all eight languages.
  - Review fixes: `debts_due` in DE also uses "Fällig: %1$s"; amounts size once for the final value (`fitCents`), so the count-up animation no longer jumps in size, and keep their full line height when shrunk; non-sp font sizes and unspecified colours fall back safely; French uses a narrow no-break space before : ; ? ! so punctuation never wraps alone; the apostrophe check counts backslashes, understands quoted parts and also checks EN. Left: paired amounts (e.g. "You owe" / "Owed to you") shrink independently.
  - Phone (2026-11-25): Home and Expenses in all five languages fit; Portuguese "US$3.082,88" was clipped in the hero, so `MoneyText` now auto-shrinks (TextAutoSize, down to half size) instead of clipping — no change where amounts already fit.
- **P5b step 5 tablet / foldable (2026-11-25):**
  - Width classes from `screenWidthDp` (updates on rotation, unfolding, multi-window): <600 compact (unchanged phone UI), 600–839 medium, ≥840 expanded. Medium/expanded: a navigation rail with the + action replaces the bottom bar on the four tab screens; every destination is centred at a readable width (680dp, two panes 1200dp) from the nav host, so screen files stay untouched; sheets max 640dp wide.
  - Expanded: two panes for Home (banners/hero/month/accounts | upcoming/recent, keeping the user's section order) and Expenses (chart/categories/history | search + day list); item builders are shared with the single-column path, so phones build the same list as before.
  - Review fixes: the rail needs ≥480dp height (a phone on its side is wide but short; the rail clipped Settings), so landscape phones keep the bottom bar (`LandscapePhoneUiTest`); the rail stays on pushed screens so content doesn't jump 80dp; one saved list state per screen shared by both layouts keeps the scroll across rotation; Home falls back to one column when the right side would be empty or during first run; no FAB glow in the 80dp rail; end-side navigation bar and cutout insets on the content; removed a sheet width equal to M3's default; plural check requires "many" for it/fr/es/pt (Android MissingQuantity) and the Italian files got it.
  - UI built by the implementer agent from a written spec; `WideLayoutUiTest` at 900×700dp. Phone (2026-11-25): simulated 900dp with `wm density 128` (reset afterwards): rail and both two-pane screens correct; normal density unchanged.
- **P5b step 4 monthly summary (2026-11-25):**
  - Opt-in switch in Settings ("Monthly summary"), off by default; switching it on asks for the notification permission on Android 13+ and marks the month that already ended as done, so the first summary comes at the next month start.
  - `domain/MonthlySummaries`: for the budgeting month that ended — spent, change against the month before in whole percent (none without a previous month), top category, income, saved; nothing for a month without entries. Due from the first day of a budgeting month for 7 days (also from the catch-up run at app start), so a phone that was off on day one still gets it; recorded in `UserPrefs.lastSummaryMonth` and sent once. Quiet mode records it without notifying; privacy mode hides the amounts and the percentage.
  - Review fixes: the month is claimed atomically inside one settings write before posting (daily and catch-up runs can overlap); a restore keeps this phone's `lastSummaryMonth` (like the backup folder); a month with only transfers sends nothing; uncategorised spending is never the named top category; the change moved from the title into a BigTextStyle body so it isn't cut off; lock screen shows only "Monthly summary ready"; a denied permission switches it back off with a message; worker tests use Assume on the 29th–31st; notification strings live with the other notification strings.
  - Tested with a Robolectric worker test (androidx.work testing) that checks the posted notification, once-only, quiet mode and the switch.
- **P5b step 3 calendar (2026-11-25):**
  - `domain/CalendarMonth`: calendar months (not budgeting periods: a calendar shows dates), whole weeks starting on the user's first day of week, per-day spent/income/count; days outside the month greyed without amounts. Screen from a calendar icon in the Expenses header: day cells shaded by spending relative to the month's busiest day, compact amount or a dot, today outlined, tap a day for its entries (edit, swipe-delete with Undo in the app scope).
  - Found by the UI test: the day cell's `clearAndSetSemantics` dropped the click action, so TalkBack users couldn't open a day; role and click are now set inside it. Cells are ~45dp wide on a 360dp screen (seven columns) but 52dp tall.
  - Review fixes: back from Calendar and Search only pops while that screen is on top; the Expenses header became title + search on one line and labelled chips (Bills, Calendar, Year overview) in a scrolling row below, because three icons squeezed the title into broken lines (DE at 360dp); days with only income or moves get a dot; the day text colour is the one with more contrast against the blended cell; grid with 6dp sides and 2dp gaps so cells reach 48dp on 360dp; the UI test matches the full date and asserts the note. Left: "today" doesn't refresh across midnight while the screen stays alive; a day that switched to a dot stays a dot after the cell widens.
  - UI built by the implementer agent from a written spec. Phone (2026-11-25): November with rent day brightest, tap on the 5th listed "Weekly shop −$15.57".
- **P5b step 2 year overview (2026-11-25):**
  - `domain/YearOverview`: 12 budgeting months (labelled by the month they start in, so the month start day moves entries between months and even years), spent / income / saved (into goals minus out), top 5 categories, biggest month. The monthly average runs from the first month with entries to the current one, so a year begun in September isn't averaged over January–August (found on the phone: $282 instead of $1,036).
  - Screen from a chart icon in the Expenses header; year arrows step through years that have data; tapping a bar or a month opens Expenses on that month. Found on the phone: switching to the Expenses tab restored its saved stack, which still held the year screen, so the tap did nothing; the year screen now goes back instead (it is only opened from Expenses) after handing over the month.
  - Review fixes: back and month taps act only while Year is on top (a double tap popped Expenses too); the average uses the same months on both sides, so future-dated entries don't inflate it; "Month by month" is hidden when no month has spending or income; top-category rows aren't announced as buttons; opening a month from Year clears an earlier category filter or search in Expenses.
  - UI built by the implementer agent from a written spec.
- **P5b step 1 all-time search (2026-11-25):**
  - `domain/Search`: every typed word must appear in the note, category, account (also the target of a transfer) or goal, ignoring case and accents; filters for period, amount range (on the shown, positive amount) and kind. 20 000 entries well under 400 ms.
  - Search screen from a search icon in the Expenses header and next to the "Penny" title on Home; the month search in Expenses stays. Results by day, newest first, capped at 300 with a hint to narrow down; an empty filter shows a hint instead of every entry. "This month" uses the budgeting period, the other periods are calendar based. If the user hides the Home hero section, Search is still in Expenses.
  - Review fixes: delete/undo from Search run in the app scope (Search is a pushed screen; Undo outlived it); "Last 3 months" ends at the end of this month like the other presets; day headers show what was spent that day like Expenses; focus is requested from inside the lazy header item; folding also maps ß/æ/œ/ø/ł/đ/ı; Min or Max alone can be reset (0 = any); rows without a note are found by their kind ("Transfer"). Left: query and filters aren't restored after process death.
  - UI built by the implementer agent from a written spec. Phone (2026-11-25): "rent" found the three rent payments across Sep–Nov with "3 results · spent $2,250.00".
- **P5a step 8 CSV import (2026-11-25):**
  - Columns are recognised by header keywords in EN/DE/IT (date/Datum/data, amount/Betrag/importo, debit/credit/Soll/Haben, category, note/Verwendungszweck/payee…) instead of fixed per-app profiles; this covers Monefy, Wallet, Money Manager and most bank exports, and every column can be reassigned on the preview. Without a header the date and amount columns are guessed from the data. Penny's own export is recognised exactly.
  - Amounts: comma/dot decimals, thousands groups (must be groups of 3), signs before/after, parentheses, currency symbols; minor units per currency (0/2/3 decimals). Dates: one pattern per file, the one that reads the most rows (one broken line doesn't hide the format), day-first on a tie. 5 MB limit; windows-1252 fallback for older bank files.
  - Rows become expenses or income only (transfers and other Penny types are reported, not imported). Duplicates = same account, date, kind, amount and note (or either without a note), skipped by default.
  - `ImportRepo` writes everything in one Room transaction with one `importId`; undo removes exactly those rows and the categories the import created, unless they were used since. The last import stays undoable from Settings → Backup ("Undo last import", with confirm).
  - Export fix: the transaction CSV wrote every amount divided by 100 under "amount_eur", wrong for yen or dinar. It now writes the currency's own decimals plus a currency column; old exports still import (two decimals).
  - Review fixes: unknown type values (Revolut CARD_PAYMENT, N26, DKB Eingang/Ausgang) fall back to the amount's sign instead of failing every row; header keywords match whole words in priority order ("Betrag" over "EUR", "Wertstellung" no longer an amount, balance/Saldo columns never), German "-datum" compounds count as dates; each existing entry marks at most one line as duplicate; active categories win over archived ones with the same name; Penny's new export reads each row's currency decimals; error lines are text-editor lines; the import runs in the app scope (undo is always recorded) and its callbacks are skipped once the screen is gone; a failed second file clears the first; Reset all data clears "Undo last import".
  - UI built by the implementer agent from a written spec. Phone (2026-11-25): German bank file (semicolons, dd.MM.yy, "1.850,00") picked through the system file picker, columns recognised, 3 rows imported, "Undo last import" removed them.
- **P5a step 7 debts (2026-11-25):**
  - Debts are personal records (Settings → Debts), separate from balances. A repayment can optionally be recorded in an account in the same DB transaction: money received → INCOME, money paid back → WITHDRAWAL (not an expense, so it never counts as spending or against budgets). Deleting the repayment deletes that transaction too.
  - A debt is settled when the last repayment covers it (or by hand: forgiven, paid outside the app); paying more than is open and editing the amount below what was paid are refused. Delete takes repayments and recorded transactions with it and Undo restores all of them.
  - Reminder: optional, needs a due date, fires once on the due day from the daily worker (same channel and quiet mode as bills).
  - Review fixes: a debt settled by hand stays settled on edits and payment removals (only a "paid in full" settle reopens); removing a repayment shows Undo and restores its account transaction; "Reopen" is hidden on fully repaid debts; Undo runs in the app scope so leaving the screen can't lose it; the direction can't flip once repayments exist; reminders are cancelled on settle/delete; the reminder text says where Debts is; DAO queries per debt instead of loading whole tables. Left for later: reminder catch-up for a missed due day, and "overdue" refreshing at midnight while the screen stays open.
  - UI built by the implementer agent from a written spec; the date dialog was extracted into `PennyDatePickerDialog` for reuse. Phone (2026-11-25): added "Anna owes you $40", recorded a full repayment into Bank → settled, Bank +$40, Home shows "Anna paid back".
- **P5a step 6 split expenses (2026-11-25):**
  - A split is N ordinary EXPENSE rows sharing `splitId`, account, date and note, so every total, budget, chart, bill detection (which skips splits) and backup works unchanged. `addSplit`/`replaceSplit` write all parts in one Room transaction.
  - Quick add: the numpad sets the receipt total; the first part always shows the rest (`Split.balanced`), so parts can't drift from the total. Save explains the first missing thing (category, amount, parts over the total). "Make recurring" is hidden while splitting.
  - Delete of any part deletes the whole split; the repository keeps the deleted parts for the following Undo, so the six existing delete/undo call sites stay unchanged. Editing any part opens the whole split.
  - Review fixes: a split only applies while the type is Expense (switching to Income no longer saved split expenses); editing a split keeps part ids and creation times by position; Undo after the app was restarted brings the row back as an ordinary expense instead of an uneditable one-part split; the deleted-split cache keeps only the last four; a rest of exactly 0 gets its own message.
  - Rows show "… · Split" so parts of one receipt are recognisable in Home, Expenses and account lists.
  - Phone (2026-11-25): split $50 into Groceries/Eating out with real taps, edit opened both parts, delete removed both; no crash on a double tap of the amount sheet's Save (a Robolectric-only layout exception when a disposing sheet's button was clicked again).
- **P5a step 5 bill detection (2026-11-25):**
  - `BillDetector` only looks at expenses with a note and a category (no guessing from amounts alone), grouped by normalised note + category. A bill = the newest unbroken run of ≥3 payments 25–35 days apart, amounts within 10 % of the median (at least 1.00 leeway for small bills), last payment ≤45 days ago. Payments tied to a bill, split parts, income and future dates never count.
  - One suggestion at a time on Home (newest first). "Add as bill" creates a monthly, hand-paid bill on the most common day with the latest amount and the user's default reminder; first due after today and at least 20 days after the last payment. "Not a bill" stores the key in `UserPrefs.dismissedBills` (travels in backups, never asked again). Sample data never triggers suggestions.
  - Review fixes: "Add as bill" is idempotent (double tap) and links the source payments to the new bill in one DB transaction, so renaming the bill never brings the suggestion back; the first due date is the next one after the last payment (+20 days), so a payment due today or already missed shows as due/overdue instead of being skipped; archived accounts/categories never end up in a suggestion; on a tie the newest payment's day wins; the count uses `<plurals>`; "Added to Bills" shows only after the save.
- **P5a step 4 category budgets (2026-11-25):**
  - `Budget.forCategories` reuses `Budget.status`, so a category budget follows the same period, pace and "per day" rules as the monthly budget. Archived categories keep their budget but are only listed when they have spending in the period.
  - Found while testing huge amounts: the "spending fast" check multiplied Longs (`spent * 100`, `budget * elapsed`) and overflowed above ~9·10^16 cents, flipping on-track to fast. Now compared exactly with BigInteger.
  - Budgeted categories without spending show as "€0 · €X left" rows under the chart but are not drawn in the chart, so the donut/bars keep meaning "where the money went".
  - Budgets show only for the current period (review): amounts have no history, so a past month would be judged against today's budget, even before the category existed.
  - In the category sheet the budget is part of the draft like name and colour and is saved with Save (the Settings monthly budget row applies at once because it has no surrounding form).
  - Phone (2026-11-25): the budget row sat below the long icon grid and was easy to miss; it now comes right after the name. Set $300 on Groceries with real taps → Expenses shows the bar and "$121.68 left".
  - Robolectric UI tests click through semantics actions inside bottom sheets: injected touches landed below sheets that were still animating in. Real touches are checked on the phone.
- **P5a step 3 small items (2026-11-25):**
  - Language: an in-app row on Android 13+ only. `AppCompatDelegate.setApplicationLocales` writes the system per-app setting there, which applies to the whole process (notifications, widgets, ViewModel texts). On 8–12 AppCompat would translate only the activity and leave the rest in the phone's language, so the row is hidden there (review finding). Languages are named in their own words; a test keeps `AppLanguage.tags`, `localeFilters` and the `values-xx` folders identical; widgets refresh after a change.
  - Shortcuts are static (`res/xml/shortcuts.xml`); resource files get no `applicationId` placeholder, so the smoke build has its own copy (test: identical apart from the package). "Scan receipt" opens Quick add as an expense and starts the camera; all entry points (widget, shortcuts, tile) go through `MainActivity` actions and are ignored until the first account exists, like the widget before.
  - Quick Settings tile uses the PendingIntent overload on Android 14+ (the Intent one throws there).
  - Budget widget shows the overall monthly budget (left, bar, per day; overage when over) from the same `Budget.status` as Home; category budgets join in step 4. Privacy mode hides every amount.
  - Launch requests are consumed once and held in `MainActivity` (review finding): with App lock on, a composition-scoped "handled" counter was lost while locked and replayed the scan after every unlock, and after a recreation the next tap was ignored. A scan always starts a fresh expense even if Quick add was open.
  - Budget widget in privacy mode shows only "Monthly budget •••" (no bar, colour or overage); the small size shows just amount + bar; the no-budget text no longer promises that a tap opens Settings.
  - Quick add "Scan receipt" is an accent-tinted button with a camera icon instead of a neutral chip (user request), so it reads as an action next to the category/account chips.
  - "Your data stays here" lists only verifiable facts (no INTERNET permission, allowBackup=false, backups to a user-picked folder, on-device OCR with the scan photo deleted, crash reports by the user's email, tips via Play).
- **P5a step 2 database v2 (2026-11-25):**
  - One migration v1 → v2 (`data/db/Migrations.kt`, SQL copied from the exported `2.json`): `categories.monthlyBudgetCents` (null = no budget), `transactions.splitId` + `importId` (indexed), new tables `debts` and `debt_payments`. Repayments got their own table in the same migration so partial payments never need a second schema change; a payment may point at a transaction when the user also recorded the money moving, otherwise debts never touch balances.
  - Backup schema 2 adds `debts` and `debtPayments` (default empty, so v1 files decode unchanged); `BackupMigrations.upgrade` lifts older files step by step. Released v1 apps already refuse a v2 file as "too new" with the existing message.
  - Frozen fixtures `v2-sample*.json` cover every new field.
  - Phone (Xiaomi, 2026-11-25): smoke app with the previous release + sample data, then the v2 build installed over it: migration ran, every amount identical, no errors in logcat; `ocr-smoke.sh` OK; Home checked light EN, dark DE, dark IT. Found and fixed an older bug: on a cold start Home drew items for the empty loading state, so it opened scrolled past the top banner and to the end of the accounts row; Home now draws nothing until the first state is loaded.
  - Review fixes: `debt_payments` has foreign keys (debt deleted → its payments go; transaction deleted → payment kept, link cleared); restore drops or unlinks broken references and counts them as dangling; the restore sheet shows the debt count so replacing data never silently removes debts; the summary shows the file's own schema version. `MigrationTest` migrates both hand-written edge rows and the complete v1 sample backup and requires identical rows afterwards.
- **P5a step 1 test infrastructure (2026-11-25):**
  - `BackupCodec` holds the Android-free part of reading a backup (decrypt, version check before full decode, JSON) so every older fixture is tested on the JVM. Fixtures in `app/src/test/resources/backups/` are frozen: regenerate only for a new schema version (`-Ppenny.writeFixtures=true`), never overwrite an old one.
  - Home and Expenses state building moved into pure `buildHomeState` / `buildExpensesState` so the 20 000-entry test times exactly what the ViewModels run. Limits (400 ms per state, 150 ms summary) catch O(n²) mistakes, not small regressions.
  - `PennyDatabase.MIGRATIONS` lists every step; `MigrationTest` writes v1 rows with raw SQL from the exported schema and opens them with the current database.
  - Test JVM heap 1.5 GB and Gradle 3 GB: the earlier 4 GB + 3 GB setting ran the 16 GB laptop out of memory.
  - Robolectric tests run on a plain `Application`; only UI tests use `TestPennyApp` (initialises WorkManager, resets the process-wide DataStore). Review findings fixed: vacuous search perf test, fixture overwrite guard, single JSON parse on restore, narrower read exception handling, unused test deps removed.
- **P5a/P5b scope (2026-11-24):** user chose features 1–10, 12, 14–17 (no tags, no receipt photos). Debts are personal records ("I owe X"), not shared-expense splitting, so §1's out-of-scope list still holds. Languages: FR, ES, PT, PL, TR (Azerbaijani/Russian offered, not chosen yet). All new data goes into one Room migration (v1 → v2) and backup schema 2 so there is only one upgrade path to test.
- **P5 (2026-11-24):**
  - Local release builds stay debug-signed so they install over the existing sideloaded app without data loss; only the Play bundle uses the upload key (opt-in flag). Moving from the sideloaded app to the Play version means backup → install from Play → restore (different signature).
  - targetSdk 36: the app is already edge-to-edge and has no custom back handling; the test phone runs Android 15, so Android 16-only behaviour is unverified on a device.
  - Privacy policy is hosted as `docs/PRIVACY.md` in the public repository (GitHub URL), no separate site.
  - Data safety: "no data collected"; crash reports are user-sent email (fallback declaration noted in play-console.md).
- **P4 (2026-11-24):**
  - Backup password is stored on the phone (Android Keystore-wrapped, separate private prefs, never in `UserPrefs` because prefs travel in backups) so scheduled backups stay encrypted. If it can't be unwrapped, the backup fails with a message instead of silently writing a plain file.
  - Encrypted files keep the `.json` name so pruning and the restore picker work unchanged. KDF iterations from a file are bounded (10k–2M).
  - Restoring a protected backup on a phone without a password keeps protection on with that password. Turning protection on or off never touches older files.
  - Play Billing 8.0.0 adds only `com.android.vending.BILLING` to the merged manifest, no network permission (checked), so the §10 decision stands. Billing connects only while the Support screen is open; purchases are consumed (tips can repeat) and leftovers are consumed on the next visit. Prices come from Play, names from our strings.
  - Crash reports leave out exception messages (they can quote user input): only class names, stack frames, app/OS version and phone model. The user sees the email before sending. Support address chosen by the user: aliyov.code@proton.me.
- **P3 (2026-11-24):**
  - Budget lives in `UserPrefs` (default 0), so no Room migration and old backups restore unchanged. "Reset all options" keeps it. Changing currency does not convert it, like every other amount.
  - "Faster than planned" only shows once spending is more than 10 % ahead of an even daily pace, to avoid nagging. Colour is never the only signal: the text says it too.
  - Insight compares the same number of days (capped at the shorter month's end); "about the same" below 5 %.
  - Receipt dates: day-first unless only month-first is possible; ignored if in the future or older than a year, and the form only takes dates from the last 60 days. Shop name: first of the top 4 rows that is mostly letters and not a title, contact, tax id or street line.
  - Category learning only reads the user's own notes and categories on the phone. A learned category is preselected but never auto-saved.
  - Time habit uses `createdAt` (entry time) from the last 90 days; needs 8+ expenses, 3+ votes and a 40 % share, else falls back to last used.
- **P2 (2026-11-24):**
  - Settings shows the essentials inline (Theme, Accent, Currency, Month starts on, Reminder time, App lock) plus Backup and More options. "Personalise" became "More options"; its duplicate Backup frequency/files-kept card was removed (the Backup screen has both). "Erase all data" sits at the bottom of More options.
  - "Reset all options" (`UserPrefs.withDefaultOptions`) now keeps currency, backup schedule and app lock as well as state, so a reset never changes how amounts read, stops backups or unlocks the app.
  - App lock switch is disabled with a hint when the phone has no screen lock; reminder time follows the phone's 12/24 h setting.
  - HUD grid default is off. Prefs are stored with `encodeDefaults`, so existing installs keep their current value. No Home section was removed.
  - Plain wording: "Recurring" is now "Bills & subscriptions" / "Bills", "Outside" is "Elsewhere", "Unarchive" is "Restore", "Reset all data" is "Erase all data". The CSV column value "Outside" is unchanged so files stay readable by other tools.
  - `EmptyState` (core/ui/components) on every list; Home and Expenses offer "Add expense" (it only opens when an account exists).
- **P1 (2026-11-21):**
  - Currency: `UserPrefs.currencyCode` (ISO 4217). New installs get the region's currency; installs from before P1 and old backups without the field become EUR. `numberFormat` and the new `symbolPosition` default to AUTO (from the locale); explicit EU/US and before/after stay as overrides. `symbolBefore` is ignored and kept for one release.
  - Zero/three-decimal currencies: amounts stay minor units; the numpad types minor units; receipt results (parser works in 2 decimals) are rescaled with `ReceiptResult.inMinorUnits`.
  - Theme: `themeMode` SYSTEM/LIGHT/DARK (default SYSTEM). The old `background` enum now means "pure black" (AMOLED) vs normal dark; new installs default to normal dark. The light palette darkens the accent until it reaches 4.5:1 on the background, and `onAccent` is picked by contrast. Widgets use day/night colour providers.
  - First run: no demo data. 8 default categories are created in the current language. Sample data rows all have ids starting with `sample-`; removing it also removes anything entered on sample accounts/goals (the dialog says so). The FAB is hidden until the first account exists; the welcome card is the only action. No backup warning before there's real data.
  - Strings live in per-area files (`strings_common/home/money/settings/personalise/platform.xml`), checked by `scripts/check-translations.py`. Per-app language via `generateLocaleConfig`. Italian plurals carry a `many` form (lint).
  - Notes created by the app ("Balance set", "Returned from X") are stored in the language active at the time. CSV column headers stay English so other tools can read them.
  - Repository input errors are typed (`InvalidInputException`, `InsufficientAvailableException`) and mapped by `AppContainer.userMessage`; ViewModel money text uses `moneyText` (user currency + locale).
  - Monthly rules read "Every month on day 14" (ordinals differ per language).
  - CI: `.github/workflows/ci.yml` runs the translation check, lint, unit tests, `bundleRelease` + `assembleRelease` and `scripts/check-manifest.sh`. It runs once pushed.
  - Open for P5: targetSdk is 35; Play will require a newer target by launch.
- **Production direction (2026-11-21):** free app, personal Play developer account, languages EN/DE/IT, tips via Play Billing consumables only, confirmed by the user; no external tip links in any build, GitHub APKs included. Architecture rules in `docs/ARCHITECTURE.md`.
- Savings "Outside (not from balance)": SAVE/UNSAVE rows use the pseudo account id `external` (MoneyCalculator.EXTERNAL). Adds to the goal without reducing any account or Available to spend. No schema change, so old backups still restore.
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

- App lock uses BIOMETRIC_WEAK or DEVICE_CREDENTIAL (PIN fallback). The lock screen waits for settings to load, so it can't be bypassed at cold start.
- Quick add widget deep link: `MainActivity.ACTION_QUICK_ADD`; repeated taps are handled via a request counter (singleTask + onNewIntent).

- Notifications are tinted with the user's accent colour.

- **Receipt OCR = ML Kit bundled model** (free, on-device, no network). INTERNET / ACCESS_NETWORK_STATE that ML Kit's logging library adds are stripped with `tools:node="remove"`, so the app stays offline. Only Latin-script words are recognised (digits work everywhere); Cyrillic/Greek receipts fall back to the largest amount. Receipt photos are never stored, so the data model and backups are unchanged.
- **Release testing of OCR happens on the real phone** (`scripts/ocr-smoke.sh`), because unit tests can't catch R8/ML Kit problems and the emulator doesn't boot here. The smoke app is uninstalled afterwards; on MIUI it must be opened once before it accepts broadcasts (the script does this).
- No §7a setting for the scanner (not in the spec); the chip just sits first in the optional row.

## Performance pass (after first phone test: "kinda laggy")
- **Release build**: R8 minify + resource shrinking (APK 26.6 MB → 4.5 MB), versionCode 3 / 1.2. Signed with the local debug key, so `penny-wallet-release.apk` installs over the debug app and keeps all data (signatures verified identical).
- `androidx.profileinstaller`: ships Compose/AndroidX baseline profiles (`assets/dexopt/baseline.prof`) so a sideloaded APK gets ahead-of-time compiled hot paths.
- All 10 ViewModels: ledger maths / row building moved off the main thread (`flowOn(Dispatchers.Default)`).
- Quick add data flow is `WhileSubscribed` (was Eagerly: recomputed the ledger on every write even with the sheet closed).
- `Modifier.glow`: Paint + BlurMaskFilter cached via `drawWithCache` (was rebuilt every frame on the FAB/buttons).
- Home hero backdrop: brushes + grid path cached, drawn in its own graphics layer.
- R8 rules keep serializers and enum names (persisted in Room, backups and settings JSON).

## How to use / what's configurable
**Install:** connect the phone with USB debugging on, then `./gradlew installRelease` (or copy `app/build/outputs/apk/release/penny-wallet-release.apk`, the fast optimised build; the debug APK is only for development). A new install starts empty with 8 default categories; "Try with sample data" on Home adds a removable demo set.

**Daily use**
- `+` → type the amount on the numpad → tap a category chip: saved (3 taps), or tap **Save**.
- **Scan receipt** (first chip above the numpad, expenses): Take photo / Choose from gallery → "Add €X to expense?" → edit the number or tap another amount found, then Accept (fills the amount), Retake or Decline. Pick the category to save. Works offline; photos aren't kept. Switch to Income / Save / Transfer at the top; account, date, note and "Make recurring" sit above the numpad.
- Home: available to spend (green; red when negative), month spending, accounts (tap for income / withdraw / set balance / transfer), upcoming recurring items, recent entries (swipe left to delete, tap to edit). Every delete has a 5-second Undo.
- Expenses: switch months, tap donut slices, tap a category to filter, tap a history bar to jump to that month, search notes. Recurring rules are reached from here (or Settings).
- Savings: goals with rings; Add money (only up to what's available in that account) / Take back. Deleting a goal returns its money.
- Recurring: Auto-pay records the expense on the due day ("Paid automatically" + Undo); Manual sends "Due today" with Mark as paid. Reminders arrive N days before, at your reminder time.
- Backup: Settings → Backup → Pick folder (choose one your Drive/Files app syncs for an off-phone copy). Runs automatically and keeps the last N files; Restore shows a summary and asks twice; CSV export.
- Widgets: long-press the home screen → Widgets → "Penny balance" (resizable) and "Penny quick add".

**Configurable (Settings → Personalise)**: accent colour (presets + hex), AMOLED / dark grey, glow, HUD grid, corners, text size, density, animations, haptics, cents, number format, € position, privacy mode; Home section order/visibility, hero metric, recent count; quick add defaults (type, account, chip order, save on chip tap, note field); default chart, history months, first day of week, month start day (payday); goal layout and sort; reminder time, default days before, overdue badge, quiet mode; backup frequency and files kept; biometric lock and lock delay. Every option has a reset, plus "Reset all".

## Notes
- Icon registry `core/ui/components/PennyIcons.kt`: 102 icons; keys are persisted, so never rename. Stand-ins: piggy=Toll, revolut=Payment, tax=ReceiptLong.
