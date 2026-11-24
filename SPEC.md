# Penny Wallet — Personal Expense Tracker for Android (build spec)

> Lives in the project root. The app is built from this file phase by phase (§8), with progress logged in `PROGRESS.md`.

---

## 1. Prompt

You are building **Penny Wallet**, a native Android expense tracker for a single user (me). It is **not** a production app: no login, no backend, no cloud, no analytics, no Play Store release pipeline. All data lives **on the phone**. Prioritise a beautiful, fast, delightful UI and a correct money model over infrastructure.

**Look & feel:** Apple-like, futuristic, minimal, *not* stock Material. Pure black background, neon green as the app's signature/identity colour. Think iOS Wallet + Apple Fitness rings + a faint sci-fi HUD. Smooth spring animations, big confident numbers, lots of negative space. Disable Material You dynamic colour.

**Core features:**
1. **Multiple balances (accounts)**: e.g. "Bank", "Cash", "Revolut". Add / rename / archive / delete.
2. **Monthly balance**: any time (typically start of month) I can *add income* to an account or *set the account balance* to an exact figure (the app records the difference as an adjustment).
3. **Add / withdraw money** from any account at any time.
4. **Savings goals**: sections like "New phone", "Tablet", "Vacation", each with a **required target amount** and an optional deadline. Add / edit / delete. Move money from an account into a goal and back. Progress ring, % reached, remaining amount, and "€X/month to hit the deadline" if a deadline is set.
5. **Available to spend**: always computed live: `account balances − money allocated to goals`. Never stored.
6. **Expenses**: adding one takes **≤ 3 taps**: tap `+` → amount on a big custom numpad → tap a category chip → saved (account and date default to last used / today).
7. **Expense categories**: "Groceries", "Rent", etc. Add / edit (name, icon, colour) / delete.
8. **Recurring expenses**: e.g. "Phone credit, €10, every month on the 14th", "Netflix subscription, monthly". Notification **1 day before** (configurable) at a **user-chosen time**. Per rule: **Auto-pay** (expense is recorded automatically on the due date, with a "Paid automatically" notification + Undo) or **Manual** (due-day notification with a "Mark as paid" action).
9. **Charts**: Expenses screen shows a donut of spending share (%) per category for the selected month, a category list with amount + %, and a bar chart of the last 6 months.
10. **Backup**: automatic **weekly** backup to a JSON file in a folder I pick on the phone, plus a **"Back up now"** button and **Restore from file**. Fully local.
11. **Home-screen widgets**: available-to-spend widget and a quick-add widget (§7b).
12. **Customization everywhere**: almost every visual and behavioural choice is a setting (§7a). When in doubt, make it configurable with a good default.
13. **Currency**: EUR only (`€`, Italian/German formatting `1.234,56 €` by default, configurable).

Build it in the phases in §8. Follow the architecture, data model and rules below; ask me before deviating.

---

## 2. Tech stack

| Concern | Choice | Why |
|---|---|---|
| Language / UI | **Kotlin + Jetpack Compose** (Material 3 as a base, fully custom themed) | native, smooth animations, best notification/background support |
| Min / target SDK | minSdk 26, targetSdk 35 | covers basically every phone |
| Database | **Room** (with Flows) | local, reactive, typed |
| Settings | DataStore (Preferences) | currency, last used account/category, backup folder URI |
| Background work | **WorkManager**: `DailyReminderWorker` (daily), `WeeklyBackupWorker` (weekly) | survives reboots, no exact-alarm permission needed |
| Notifications | NotificationCompat + `POST_NOTIFICATIONS` runtime permission (Android 13+) | reminders with a "Mark as paid" action |
| Backup files | Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`, persisted URI permission) + kotlinx.serialization JSON | user-chosen folder, no storage permissions |
| Charts | Custom Compose `Canvas` donut + rings; **Vico** for the bar chart | full control over the look |
| Navigation | Navigation-Compose | simple |
| DI | Manual `AppContainer` (no Hilt) | personal app, keep it simple |
| Architecture | MVVM: Compose screen → ViewModel (StateFlow) → Repository → Room DAO | standard, testable |
| Widgets | **Jetpack Glance** (`GlanceAppWidget`) | Compose-like widgets |
| App lock | `androidx.biometric` (optional setting) | fingerprint / face to open |
| Haptics | `HapticFeedback` / `VibrationEffect` on numpad and save | premium feel |

Unit tests (JUnit) only for the money and recurrence logic (§4, §5).

---

## 3. Data model (Room entities)

**All money is `Long` cents. Never use Float/Double for money.**

```kotlin
@Entity data class Account(
  @PrimaryKey val id: String,          // UUID
  val name: String,                    // "Bank"
  val icon: String,                    // icon key
  val archived: Boolean = false,
  val sortOrder: Int,
  val createdAt: Long
)

@Entity data class Category(           // expense categories
  @PrimaryKey val id: String,
  val name: String,                    // "Groceries"
  val icon: String,
  val color: Long,                     // ARGB, from palette (§6)
  val archived: Boolean = false,
  val sortOrder: Int
)

@Entity data class Goal(               // savings sections
  @PrimaryKey val id: String,
  val name: String,                    // "Vacation"
  val icon: String,
  val color: Long,
  val targetCents: Long,               // REQUIRED, > 0
  val deadline: LocalDate? = null,
  val archived: Boolean = false,
  val sortOrder: Int,
  val createdAt: Long
)

enum class TxType { INCOME, WITHDRAWAL, ADJUSTMENT, EXPENSE, TRANSFER, SAVE, UNSAVE }

@Entity(indices = [Index("date"), Index("accountId"), Index("categoryId"), Index("goalId")])
data class Transaction(
  @PrimaryKey val id: String,
  val type: TxType,
  val amountCents: Long,               // positive; ADJUSTMENT may be negative
  val accountId: String,               // source (or target for INCOME / UNSAVE)
  val toAccountId: String? = null,     // TRANSFER
  val categoryId: String? = null,      // EXPENSE
  val goalId: String? = null,          // SAVE / UNSAVE
  val recurringId: String? = null,     // set when created from a recurring rule
  val date: LocalDate,
  val note: String? = null,
  val createdAt: Long
)

enum class Frequency { WEEKLY, MONTHLY, YEARLY }

@Entity data class RecurringExpense(
  @PrimaryKey val id: String,
  val name: String,                    // "Phone credit", "Netflix"
  val amountCents: Long,
  val categoryId: String,
  val accountId: String,
  val frequency: Frequency = Frequency.MONTHLY,
  val dayOfMonth: Int?,                // 1–31 for MONTHLY/YEARLY
  val month: Int?,                     // 1–12 for YEARLY
  val dayOfWeek: Int?,                 // for WEEKLY
  val nextDueDate: LocalDate,
  val remindDaysBefore: Int = 1,       // 0–7, per rule
  val autoPay: Boolean = false,        // true → record EXPENSE automatically on due date
  val active: Boolean = true
)
```

Use Room `TypeConverters` for `LocalDate` and enums.

---

## 4. Money rules (`domain/MoneyCalculator.kt`, pure Kotlin, unit-tested)

Everything is **derived from the transaction ledger**. No stored balances.

```
accountBalance(a) =
    Σ INCOME(a) + Σ ADJUSTMENT(a) + Σ TRANSFER in(a)
  − Σ EXPENSE(a) − Σ WITHDRAWAL(a) − Σ TRANSFER out(a)

goalBalance(g)        = Σ SAVE(g) − Σ UNSAVE(g)
goalProgress(g)       = goalBalance(g) / g.targetCents   (cap ring at 100%, show "Reached ✓")
goalMonthlyNeeded(g)  = (target − balance) / monthsUntil(deadline)   if deadline set

allocatedFrom(a)      = Σ SAVE from a − Σ UNSAVE to a
availableInAccount(a) = accountBalance(a) − allocatedFrom(a)

totalBalance     = Σ accountBalance (non-archived)
totalSaved       = Σ goalBalance
availableToSpend = totalBalance − totalSaved

monthSpent(m)       = Σ EXPENSE in month m
categoryShare(c, m) = spent(c, m) / monthSpent(m)
```

Rules:
- **Save** is an *earmark*: money stays in the account but is no longer available to spend. Can't save more than `availableInAccount`.
- **Set balance to X** → insert `ADJUSTMENT` with `X − accountBalance(a)`.
- **Delete a goal with money in it**: confirm, return money via `UNSAVE` to the source accounts (proportionally to `allocatedFrom`), then delete.
- **Delete a category that has expenses**: offer "Move expenses to…" or archive (hidden from pickers, still in history/charts). Never orphan data.
- **Delete an account**: only if balance is 0, otherwise offer archive.
- Overspending allowed: available can go negative, shown in red instead of green.
- Every delete shows a Snackbar with **Undo** (5 s).

---

## 5. Recurring expenses & reminders

- **Next-due calculation** (`domain/Recurrence.kt`, unit-tested): if `dayOfMonth` > days in month (e.g. 31 in February), use the last day of the month. After a payment, advance `nextDueDate` by one period.
- **`DailyReminderWorker`**: periodic, every 24 h, at the **user's reminder time** from settings (default 09:00; initial delay computed; re-scheduled whenever the setting changes). Also re-enqueued on `BOOT_COMPLETED` and app start. For each active rule:
  - `nextDueDate − remindDaysBefore == today` → notification: *"Tomorrow: Phone credit 10,00 €"*.
  - `nextDueDate == today` and **autoPay** → insert the EXPENSE (`recurringId` set), advance `nextDueDate`, notify *"Paid automatically: Netflix 20,00 €"* with **Undo** action.
  - `nextDueDate == today` and **manual** → notification *"Due today: …"* with action **Mark as paid** (BroadcastReceiver inserts the EXPENSE, advances `nextDueDate`).
  - **Idempotent**: never insert twice for the same rule + due date (check `recurringId` + `date`). If the phone was off on the due day, auto-pay catches up on the next run.
  - `nextDueDate < today` and unpaid → shows as **Overdue** in the app (no notification spam).
- In-app: Home shows an "Upcoming" strip (next 7 days). A **Recurring** screen (from Expenses or Settings) lists rules: add / edit / pause / delete, next due date, and a "Pay now" button.
- Notification channel "Reminders". Ask for `POST_NOTIFICATIONS` the first time a recurring expense is created.
- Upcoming recurring amounts in the current month are shown on Home as "€X due this month" (they don't change available until paid).

---

## 6. Backup (local only)

- **First run of backup**: user picks a folder via `ACTION_OPEN_DOCUMENT_TREE`; persist the URI permission in DataStore. (Tip shown in UI: pick a folder that your Google Drive/Files app syncs if you want an off-phone copy.)
- **File format**: `penny-wallet-backup-YYYY-MM-DD.json` containing `{ schemaVersion, exportedAt, accounts, categories, goals, transactions, recurring, settings }`.
- **`WeeklyBackupWorker`**: periodic every 7 days, constraint `requiresBatteryNotLow`. Writes a new file and **keeps the last 8**, deleting older ones.
- **Settings → Backup**: "Back up now" (same code path, shows ✓ + timestamp), "Last backup: 3 days ago", "Change folder", **Restore from file** (`ACTION_OPEN_DOCUMENT`, validates schemaVersion, shows a summary, replaces all data inside one Room transaction after double confirm).
- Also offer "Export transactions as CSV".
- If no backup for > 10 days (folder permission lost, etc.), show a small warning banner on Home.

---

## 7. Design system

**Colours (Compose theme):**
```
Background      #000000
Surface1        #0D0D0F   cards
Surface2        #16161A   sheets / raised
Hairline        #FFFFFF @ 8%   0.5–1dp borders
TextPrimary     #F5F5F7
TextSecondary   #8E8E93
Accent (Penny)  #39FF88   neon green: brand, available money, primary buttons, active tab, progress
AccentDim       #1F8F4E
Glow            Accent @ 35%, blur 24dp (drawBehind / shadow)
Danger          #FF453A
Warning         #FFD60A
```
Neon green is the app's identity: logo, FAB, primary buttons, active states, the available-to-spend number. **Sections and charts use their own colours** so they're easy to tell apart.

**Category / goal palette** (vivid on black, pick per item):
`#39FF88 green · #00D1FF cyan · #7C5CFF violet · #FF4FD8 magenta · #FF9F0A orange · #FFD60A yellow · #FF453A red · #5AC8FA sky · #BF5AF2 purple · #64D2FF ice · #A3E635 lime · #8E8E93 grey`
Always pair colour with icon + label.

**Typography:** Bundle **Inter** (or SF Pro Display if available locally) via `res/font`. Tabular numbers (`fontFeatureSettings = "tnum"`). Hero balance 56–64sp, SemiBold, letterSpacing −0.02em. Section labels 13sp, TextSecondary.

**Components & motion:**
- Cards: 28dp corner radius, Surface1, hairline border, faint top-left gradient.
- Bottom nav: custom, translucent black with blur (`Modifier.blur` / RenderEffect on API 31+, solid fallback below), green active indicator dot.
- All forms in **ModalBottomSheet**s (iOS-style, drag to dismiss), not new screens.
- FAB: neon green circle, black `+`, soft glow, spring scale 0.92 on press.
- Animated numbers (roll/count) on balance changes; `animateContentSize`, `spring()` everywhere.
- Progress rings (Apple Fitness style) for goals.
- Faint radial green glow / subtle grid behind the hero number on Home for the futuristic touch.
- Edge-to-edge, dark status/nav bars, haptic tick on numpad keys and on save.
- App icon: black background, neon green "P" glyph/coin. Adaptive icon + monochrome layer.

---

## 7a. Customization (Settings essentials + More options)

All stored in DataStore (`SettingsStore`), exposed as one `StateFlow<UserPrefs>` and applied live (no restart). Every option has a sensible default and a "Reset to default".

**Appearance**
- Accent colour: Neon green (default) + presets (cyan, violet, magenta, orange, white) + custom hex picker.
- Background: AMOLED black (default) / dark grey.
- Glow intensity: off / subtle / strong. Futuristic HUD grid on Home: on/off.
- Corner style: round (28dp) / softer (20dp) / sharp (12dp).
- Text size: S / M / L; density: comfortable / compact.
- Animations: full / reduced / off. Haptics: on/off.
- Number style: show cents on/off, `1.234,56 €` vs `€1,234.56`, symbol before/after.
- Hide amounts (privacy mode: shows `••••` until tapped).

**Home**
- Reorder and show/hide Home sections (hero, month strip, accounts, upcoming, recent) via drag list.
- Hero shows: available to spend (default) / total balance / spent this month.
- Number of recent transactions (3–10).

**Quick add**
- Default type (expense/income), default account (last used / fixed), category chip order (most used / manual / alphabetical), save-on-chip-tap on/off, show note field on/off.

**Expenses & charts**
- Default chart: donut / horizontal bars; months shown in history (3/6/12).
- First day of week, **month start day** (e.g. payday the 27th → "month" = 27th–26th).

**Savings**
- Goal card layout: grid / list; sort by progress / deadline / manual.

**Reminders**
- **Reminder time** (time picker), default days-before for new rules, overdue badge on/off, quiet mode (no notifications at all).

**Backup**
- Frequency: daily / weekly (default) / monthly, folder, number of files kept (default 8).

**Security**
- Biometric app lock on/off, lock after (immediately / 1 min / 5 min).

**Data**
- Categories, accounts, goals: name, icon (≥ 60 icon set), colour, order, archive.

## 7b. Widgets (Glance)

- **Balance widget** (2×1, resizable to 4×2): "Available" amount in accent colour, small "spent this month"; larger size adds next upcoming recurring expense. Tap → opens app.
- **Quick add widget** (1×1 / 2×1): green `+` → opens the Quick add sheet directly (deep link).
- Styled to match the app (black, accent from settings, rounded corners). Update on every DB write (`updateAll` from repositories) and after workers run. Respects privacy mode (hide amounts).

## 8. Screens & build phases

**Bottom nav: Home · Expenses · Savings · Settings**, with a floating **+** button.

**Home**
- Hero: *Available to spend* (big, green) · under it "€X total · €Y saved".
- Month strip: spent this month, days left.
- Account cards carousel (balance + available). Tap → account detail: history, Add income, Withdraw, Set balance, Transfer.
- Upcoming recurring (next 7 days) + overdue badge.
- Recent transactions (last 5); swipe to delete, tap to edit.

**Quick add (+)**: bottom sheet
- Segmented control: **Expense** (default) · Income · Save · Transfer.
- Huge amount + custom numpad (no system keyboard).
- Category chips (last used first): tapping one saves immediately.
- Optional row: account · date · note · "Make recurring" toggle.

**Expenses**
- Month switcher (‹ November 2026 ›).
- Donut (custom Canvas) with total in the centre; tap slice to highlight.
- Category list: icon, name, amount, %, thin bar.
- Last 6 months bar chart (Vico).
- Transaction list grouped by day, filter by category, search notes.
- Entry to **Recurring** screen.

**Savings**
- Total saved hero.
- 2-column grid of goal cards: ring, saved / target, %, deadline, "€X/month".
- Goal detail: Add money, Take back, Edit, Delete. Celebration animation at 100%.
- "+ New goal" card (name, icon, colour, **target**, optional deadline).

**Settings**
- Your money: Accounts, Expense categories (add, edit, drag to reorder, archive, delete with reassign), Bills & subscriptions.
- Essentials inline: Theme, Accent, Currency, Month starts on, Reminder time, App lock.
- Backup (§6).
- More options (§7a): everything else, plus Erase all data (double confirm) at the bottom.

### Phases
1. **Scaffold**: Compose project, theme (§7), fonts, bottom nav + empty screens, Room DB + DAOs, seed data on first launch (2 accounts, 6 categories, 2 goals, ~40 transactions over 3 months, 2 recurring rules).
2. **Domain**: `MoneyCalculator` + `Recurrence` + JUnit tests.
3. **Home + accounts**: hero, cards, income / withdraw / set balance / transfer.
4. **Quick add sheet** with numpad + haptics.
5. **Expenses screen** + charts.
6. **Savings** + goals with targets.
7. **Recurring + notifications** (WorkManager, boot receiver, auto-pay, Mark-as-paid, reminder time).
8. **Backup / restore** (SAF, scheduled worker, Back up now).
9. **Customization** (§7a): SettingsStore + Personalise screens, applied live across the app.
10. **Widgets** (§7b).
11. **Polish**: animations, empty states, app icon, splash screen, biometric lock.

---

## 9. Project structure

```
app/src/main/java/com/penny/wallet/
  PennyApp.kt                // Application, AppContainer, schedules workers
  MainActivity.kt
  data/
    db/          PennyDatabase.kt, entities/, dao/, Converters.kt, Seed.kt
    repo/        AccountRepo, TransactionRepo, GoalRepo, CategoryRepo, RecurringRepo
    backup/      BackupManager.kt (serialize, write via SAF, prune, restore), BackupModels.kt
    prefs/       SettingsStore.kt (DataStore)
  domain/        MoneyCalculator.kt, Recurrence.kt, Formatters.kt
  widget/       BalanceWidget.kt, QuickAddWidget.kt, WidgetUpdater.kt
  work/          DailyReminderWorker.kt, WeeklyBackupWorker.kt, BootReceiver.kt, MarkPaidReceiver.kt
  notify/        Notifications.kt (channels, builders)
  ui/
    theme/       Color.kt, Type.kt, Theme.kt, Glow.kt
    components/  Card, Numpad, Chip, Ring, Donut, AnimatedAmount, SegmentedControl, PennySheet
    home/  expenses/  savings/  settings/  quickadd/  recurring/
app/src/test/   MoneyCalculatorTest.kt, RecurrenceTest.kt
```

Data flow: **UI event → ViewModel → Repository writes a Transaction → Room Flow emits → ViewModel combines + runs MoneyCalculator → StateFlow → UI**. Composables never compute balances themselves.

---

## 10. Out of scope
Login, cloud sync/servers, bank connections, multi-user, other currencies, Wear OS, Play Store publishing.
