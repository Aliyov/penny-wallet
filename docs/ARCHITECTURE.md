# Penny Wallet: Architecture & Engineering Guide

The single reference for how Penny Wallet is built. Product details live in `SPEC.md`, progress and decisions in `PROGRESS.md`. When this guide and older code disagree, the guide wins and the code gets fixed when it is next touched.

---

## 1. Product principles

Penny Wallet is a **free, offline, private** expense tracker for Google Play. Every change is checked against these rules:

1. **Understood in 10 seconds.** A first-time user can add an expense without reading anything.
2. **One screen, one job.** Each screen has one primary action. Secondary actions live in an overflow or a sheet.
3. **Ask late, not early.** No setup wizard and no permission requests up front. A feature appears when it becomes relevant (e.g. "Scan receipt" only while adding an expense).
4. **Smart defaults, few settings.** If most people would pick X, X is the default. Rarely changed options sit behind "More options".
5. **Nothing surprising.** No demo data, no nagging, no badges, no pop-ups asking for ratings or tips.
6. **Private by construction.** No `INTERNET` permission, no accounts, no analytics, no ads. Data leaves the phone only when the user exports or backs it up.

Explicitly out of scope: bank connections, cloud sync, multi-user, multiple currencies at once, investments, shared expenses.

---

## 2. Platform & stack

| Area | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 (themed by Penny's own tokens) |
| Build | Gradle wrapper, Kotlin DSL, version catalog `gradle/libs.versions.toml`, AGP built-in Kotlin, KSP |
| Storage | Room (SQLite) for the ledger, DataStore for preferences |
| Background | WorkManager (reminders, auto-pay, backups) |
| Widgets | Glance |
| OCR | ML Kit Text Recognition, bundled Latin model (offline) |
| Tips | Google Play Billing (see §10) |
| SDK levels | minSdk 26, targetSdk = Play's current requirement, compileSdk = latest stable |
| Distribution | Android App Bundle (`.aab`) through Google Play, signed with an upload key, Play App Signing on |

Only stable releases of libraries. Every new dependency must keep the merged manifest free of `INTERNET` / `ACCESS_NETWORK_STATE`, or have those removed with `tools:node="remove"` and still work offline.

---

## 3. Code layout

One Gradle module (`:app`). A single module keeps builds fast and the project easy to navigate at this size; the package boundaries below do the job modules would.

```
com.penny.wallet
├── App.kt, MainActivity.kt, AppContainer.kt    entry points + manual DI
├── core/
│   ├── money/        Money formatting, currency, parsing (pure Kotlin)
│   ├── ui/           theme (tokens, light/dark), design-system components
│   └── util/         small shared helpers (dates, flows)
├── domain/           pure Kotlin business logic, no Android imports
│   ├── MoneyCalculator, Recurrence, Budget, Insights
│   └── receipt/      receipt text parsing
├── data/
│   ├── db/           Room database, entities, DAOs, migrations
│   ├── prefs/        UserPrefs + SettingsStore (DataStore)
│   ├── repo/         repositories (the only way the app touches data)
│   ├── backup/       backup / restore / CSV, versioned format
│   ├── ocr/          ML Kit wrapper
│   └── billing/      Play Billing wrapper (tips)
├── feature/          one package per user-facing feature
│   ├── home/  add/  expenses/  savings/  recurring/
│   ├── settings/  onboarding/  support/  lock/
│   └── <feature>/    <Name>Screen.kt, <Name>ViewModel.kt, <Name>UiState.kt, components/
└── platform/         Android integration: notify/, work/, widget/, shortcuts/
```

Current → target (moved once, in a behaviour-neutral commit at the start of the production work):

| Today | Target |
|---|---|
| `ui/theme`, `ui/components`, `ui/common` | `core/ui` |
| `domain/Formatters.kt` | `core/money` |
| `ui/home`, `ui/account` | `feature/home` (account detail is part of Home's flow) |
| `ui/quickadd` | `feature/add` |
| `ui/expenses`, `ui/savings`, `ui/recurring`, `ui/settings`, `ui/lock` | `feature/…` with the same names |
| `ui/nav` | `feature/nav` |
| `notify`, `work`, `widget` | `platform/…` |

---

## 4. Layers & rules

```
Composable  →  ViewModel  →  Repository  →  Room / DataStore / ML Kit / Billing
                   ↓
                domain (pure functions)
```

- **Composables** render a `UiState` and send user intents to the ViewModel. They never compute money, never call repositories, never hold business state. Screen-level composables take `state` + lambdas so they can be previewed and tested without a ViewModel.
- **ViewModels** combine repository flows into one `StateFlow<XUiState>` (`stateIn(WhileSubscribed(5_000))`, heavy work on `Dispatchers.Default`). One-off results (saved, error, undo) are returned through callbacks or a `Channel`, never stored as state that replays.
- **Repositories** own reads/writes and transactions. They notify widgets/backups through `ChangeListener`. Nothing else touches DAOs.
- **Domain** is pure Kotlin: deterministic, no clock or locale reads inside (pass `today`, `Locale` in), fully unit-tested.
- **DI** stays manual (`AppContainer`, created once in `App`). ViewModels get it through the existing `pennyViewModel { }` factory. No DI framework: the graph is small and explicit wiring is easier to follow.
- **Errors**: repositories throw typed exceptions for user-correctable problems (e.g. `InsufficientAvailableException`), ViewModels map them to localized messages. Unexpected exceptions are logged and surfaced as a generic message, never swallowed silently.

---

## 5. Money & currency

- Amounts are `Long` **minor units** everywhere (cents for EUR). No `Double` for money, ever.
- The app has **one currency**, chosen by the user (`UserPrefs.currencyCode`, ISO 4217), suggested on first run from the device region. Fraction digits come from `java.util.Currency` (EUR 2, JPY 0).
- Changing the currency later only changes formatting; it never converts stored amounts (the dialog says so).
- Formatting and parsing live only in `core/money`, use the user's locale for separators and the currency's symbol/position, and are covered by tests for EN/DE/IT.
- Balance, available-to-spend, budgets, goal progress and recurrence are computed only in `domain/` (`MoneyCalculator`, `Budget`, `Recurrence`).

---

## 6. Data & persistence

**Room**
- Schemas are exported to `app/schemas/` and committed. Every schema change bumps the version and ships a migration (auto-migration when possible, manual otherwise). Destructive migration is never enabled.
- Every migration has a test that opens the previous schema with real data and checks it after migration (JVM tests with the bundled SQLite driver).
- Dates are stored as epoch days, enums by name (renaming an enum constant is a data-breaking change).

**Preferences**
- `UserPrefs` is one serializable data class stored as JSON in DataStore. New fields must have defaults, so old installs and old backups load unchanged. Removed options are left as ignored fields for one release, then dropped.

**Backups**
- JSON with `schemaVersion`. The app can restore **every** backup version it ever wrote: older versions are upgraded step by step in `data/backup/BackupMigrations.kt`. A test restores a fixture file of each version.
- Optional password encryption (AES-GCM, key from PBKDF2), indicated in the file header so restore knows to ask for the password.

---

## 7. Theming

- `ThemeMode = SYSTEM | LIGHT | DARK` (default SYSTEM). Dark has an optional "pure black" variant.
- Colours exist only as tokens in `core/ui/theme` (`PennyColors`): one light and one dark palette, plus the user's accent. No hex colours in feature code.
- Both palettes meet WCAG AA contrast for text. Every screen is previewed in light and dark.
- Status/navigation bar icons follow the active theme. The splash screen and widgets follow the system theme.

---

## 8. Localization (English, German, Italian)

- Every user-visible string lives in `res/values/strings.xml`, with `values-de` and `values-it` translations. No literal UI text in Kotlin, including notification text and widget labels.
- Use `<plurals>` for counts and placeholders (`%1$s`) for sentences. Never build sentences by concatenation.
- Dates, months and weekdays come from `DateTimeFormatter` with the current locale, numbers from `core/money`.
- Per-app language support (`locales_config.xml`), so users can pick the app language in Android settings.
- Receipt-scanning keywords are independent of the UI language: every receipt language is always recognised.
- Text must survive German word length: no fixed-width text containers, test at 200% font size.

---

## 9. UX rules (how simplicity is enforced)

- **First run:** empty Home with one card: "Add your first account" (name + current balance + currency preselected). No demo data. A small "Try with sample data" link fills a removable demo set.
- **Settings:** visible by default: Theme, Accent, Currency, Month starts on, Reminders, Backup, App lock, Support Penny. Everything else is under **More options**.
- **Home:** balance, this month, budget (only if set), recent. Upcoming bills appear only when there are some.
- **Empty states:** one sentence + one button on every list.
- **Wording:** plain everyday words, short sentences, no jargon ("Money set aside", not "earmarked").
- **Destructive actions:** confirm once, offer Undo where possible.
- **Accessibility:** content descriptions on icons, 48dp touch targets, TalkBack order checked on key screens, no information shown by colour alone.

---

## 10. Support Penny (tips)

- The app is completely free. Settings → **Support Penny** shows a short thank-you text and three one-off tips (small / medium / large coffee).
- **Decided:** tips use **Google Play Billing** only (consumable in-app products), behind a `TipRepository` interface in `data/billing`. One tap plus fingerprint for the user, local currency and VAT handled by Google, no policy risk.
- **No external tip links in any build** (no Buy Me a Coffee, Ko-fi or similar), including the APKs published on GitHub.
- A tip unlocks nothing: no badges, no features. A thank-you message is shown after purchase.
- No prompts or reminders to tip anywhere else in the app.
- If Play Billing is unavailable (e.g. an APK installed from GitHub or a phone without the Play Store), the screen explains that tips need the Play Store version and nothing else changes.
- The merged manifest is checked to confirm Billing adds no network permission. If it does, the decision is revisited before release.

---

## 11. Code style

- Kotlin official style. Files stay under ~300 lines: split into components instead of growing.
- Names explain intent. Prefer small, well-named functions over comments.
- **Comments are rare.** Write one only to explain *why* something non-obvious is done (a platform quirk, a policy reason, a money rule). Never restate what the code does, never add section banners, changelog notes or commented-out code. KDoc only on domain functions whose contract is not obvious from the signature.
- No `TODO` in `main`. Open work goes into `PROGRESS.md`.
- No author, tool or generator attributions in code, comments, commit messages, docs or store texts.
- Commit messages: imperative summary line, optional short body explaining why.

---

## 12. Testing & quality gates

| Layer | What is tested | Where |
|---|---|---|
| Domain | money rules, budget, recurrence, insights, receipt parsing | `src/test`, JUnit |
| Formatting | currency and locale formats for EN/DE/IT | `src/test` |
| Data | Room migrations, backup upgrade + restore for every version | `src/test` (bundled SQLite driver) |
| UI | key flows: first run, add expense, scan confirm, budget, backup/restore | Compose UI tests on JVM (Robolectric) |
| Parsers | CSV import and receipt parsing: real samples plus fuzzing (random input never crashes) | `src/test` |
| Performance | 20 000-entry data set: Home state, search and summaries within time limits | `src/test` + smoke app on the phone |
| Release build | R8 + ML Kit + Billing actually work | `scripts/ocr-smoke.sh` and an install-and-launch check on a real phone |

**Continuous integration** (GitHub Actions, on every push and pull request): lint, unit + UI tests, `bundleRelease`, a merged-manifest check (fails on `INTERNET` / `ACCESS_NETWORK_STATE`), a string check (no missing DE/IT translations).

**Definition of done for a phase:** CI green, release build installed and smoke-tested on the phone, light and dark checked, EN/DE/IT checked, `PROGRESS.md` updated, committed.

---

## 13. Release & Google Play

- **Signing:** a dedicated upload keystore stored outside the repository. Its passwords live in `~/.gradle/gradle.properties`, never in git. Play App Signing holds the app signing key. The debug key is used only for local builds.
- **Versioning:** `versionCode` strictly increases. `versionName` is `MAJOR.MINOR.PATCH`.
- **Artifacts:** `bundleRelease` for Play. A universal APK stays available for GitHub releases.
- **Store listing:** title "Penny Wallet: Expense Tracker", short and full description, and screenshots in light and dark, all in EN/DE/IT.
- **Policy:** privacy policy hosted on GitHub Pages ("no data collected"). Data safety form: no data collected or shared. Content rating questionnaire. Target audience 18+ (finance).
- **Personal developer account:** closed testing with at least 12 testers for 14 continuous days before production access is granted.
- **Sideloaded users:** builds from GitHub are signed with a different key than Play builds. To switch: back up in the app → uninstall → install from Play → restore.
- **Release checklist:** version bumped → CI green → `ocr-smoke.sh` passes on the phone → `bundleRelease` → upload to the testing track → release notes in EN/DE/IT.

---

## 14. Production roadmap

Tracked with checkboxes in `PROGRESS.md`:

1. **P1 Foundations:** package move (§3), comment cleanup (§11), strings extracted + DE/IT (§8), theme modes (§7), currency choice (§5), clean first run without demo data (§9), CI (§12).
2. **P2 Simplify:** settings split into essentials + More options, leaner Home, plain wording, empty states.
3. **P3 Features:** monthly budget, "this month" insight card, smarter receipt scan (shop name, date, learned category), habit-based category suggestions.
4. **P4 Support & trust:** Support Penny (Play Billing), encrypted backups, crash report by email (no SDK).
5. **P5 Play readiness:** upload key + AAB, accessibility pass, store listing EN/DE/IT, privacy policy, Data safety.
6. **P5a/P5b Compete:** language row, CSV import, category budgets, bill detection, shortcuts + Quick Settings tile, "Your data stays here", split expenses, debts, budget widget; then all-time search, year overview, calendar, FR/ES/PT/PL/TR, monthly summary, tablet layouts.
7. **P6 Launch:** closed test (12 testers / 14 days), fixes, production release.
8. **Later:** tags/trips, receipt photos attached to expenses.
