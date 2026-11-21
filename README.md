<div align="center">

# Penny Wallet

**A personal Android expense tracker. Offline, private, fast.**

Pure black background, neon-green accent. Big numbers, progress rings and spring animations.
All of your data stays on your phone.

![Android](https://img.shields.io/badge/Android-8.0%2B-39FF88?style=flat-square&logo=android&logoColor=black)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-39FF88?style=flat-square&logo=kotlin&logoColor=black)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-39FF88?style=flat-square&logo=jetpackcompose&logoColor=black)
![Offline](https://img.shields.io/badge/network-none-0D0D0F?style=flat-square)

</div>

---

## Why

Most budget apps want an account, a subscription or your bank login. Penny Wallet asks for none of them.
There's no backend, no analytics and **no internet permission at all**. It answers one question:
*how much can I actually spend right now?*

```
available to spend = what's in your accounts − what you've set aside for goals
```

That number is calculated live from your transaction history. It is never stored, so it can't drift out of sync.

## Features

| | |
|---|---|
| **Accounts** | Bank, Cash, Revolut… add income, withdraw, transfer, or *set the balance* to an exact figure (the difference is recorded as an adjustment). |
| **Three-tap expenses** | `+` → amount on a big custom numpad → tap a category. Saved. The account and date default to last used / today. |
| **Savings goals** | A required target and an optional deadline. Apple Fitness-style rings, "€X/month to hit the deadline" and a celebration at 100%. Saved money is *earmarked*: it stays in the account but no longer counts as available. |
| **Recurring expenses** | Weekly, monthly or yearly. A reminder N days before, at your chosen time. **Auto-pay** records the expense on the due date and lets you undo it from the notification. **Manual** rules get a *Mark as paid* action. |
| **Charts** | A spending donut per category (tap a slice), per-category breakdown, and a history of recent months (tap a bar to jump to that month). |
| **Backup** | Automatic JSON backups to a folder you pick (daily / weekly / monthly), keeping the last N files. *Back up now*, restore with a summary and double confirmation, and CSV export. Pick a folder your Drive/Files app syncs for an off-phone copy. |
| **Widgets** | A resizable *Available to spend* widget and a one-tap *Quick add* widget. |
| **App lock** | Fingerprint, face or PIN, with an optional "lock after" delay. |
| **Undo everywhere** | Every delete shows a 5-second Undo. |

### Customise almost everything

Light, dark or system theme · accent colour (presets or any hex) · pure black dark mode · glow strength · HUD grid · corner style · text size · density ·
animations full/reduced/off · haptics · your currency (suggested from your region) · show cents · number format follows your language or `1.234,56` / `1,234.56` · privacy mode (amounts hidden until tapped) ·
Home section order and visibility · what the hero number shows · quick-add defaults and chip order ·
**month start day** (payday on the 27th → your "month" runs 27th–26th) · goal layout and sort · reminder time · quiet mode · backup frequency.

All of these apply live, and each has a reset.

## Install

**From a release build** (recommended; the debug build is noticeably slower):

```bash
./gradlew assembleRelease
# → app/build/outputs/apk/release/penny-wallet-release.apk
```

Copy the APK to your phone and open it, or install over USB with `./gradlew installRelease`.

> The release build is signed with your local debug key, so it installs over a debug build without losing data.
> The app starts empty with a few default categories. *Try with sample data* on the first screen fills in a demo set you can remove again with one tap.

### Requirements

- Android 8.0+ (minSdk 26)
- JDK 17+ for Gradle (a full JDK 21 toolchain is downloaded automatically if your system Java has no `javac`)
- Android SDK with platform `android-37.1`

## Tech stack

| Concern | Choice |
|---|---|
| UI | Kotlin, Jetpack Compose (Material 3 base, fully custom themed; Material You dynamic colour off) |
| Architecture | MVVM: Composable → ViewModel (`StateFlow`) → Repository → Room DAO |
| Storage | Room (Flows) for data · DataStore for settings |
| Background | WorkManager (daily reminders, scheduled backups), boot receiver |
| Backup | Storage Access Framework + kotlinx.serialization JSON |
| Widgets | Jetpack Glance |
| Charts | Custom Compose `Canvas` (donut, rings, bars) |
| DI | A small hand-written `AppContainer`, no Hilt |
| Build | Gradle 9.9, AGP 9.5, Kotlin 2.4, KSP, version catalog, R8 + baseline profiles |

### How money is handled

- **All money is `Long` minor units** (cents, or whole yen for zero-decimal currencies). No floats anywhere. Formatting happens only in the UI layer.
- **Nothing derived is stored.** Account balances, goal balances and "available" are computed from the transaction ledger by
  [`MoneyCalculator`](app/src/main/java/com/penny/wallet/domain/MoneyCalculator.kt), a pure Kotlin object with unit tests.
- **Recurrence is unit-tested too** ([`Recurrence`](app/src/main/java/com/penny/wallet/domain/Recurrence.kt)): a rule on the 31st
  goes Jan 31 → Feb 28 → Mar 31. Auto-pay catches up if the phone was off, and payments are idempotent, so a double tap on
  *Mark as paid* can't pay next month as well.
- **Deletes never orphan data.** A category with expenses must be moved or archived. An account with history is archived.
  Deleting a goal returns its money to the accounts it came from.

```
app/src/main/java/com/penny/wallet/
  core/      money (Formatters) · ui (theme, components) · util (dates)
  domain/    MoneyCalculator · Recurrence · receipt parsing   ← pure, unit-tested
  data/      db (entities, DAOs, sample data) · repo · backup · prefs · ocr
  feature/   home · add · expenses · savings · recurring · settings · lock · nav
  platform/  notify · work (reminders, backups, boot) · widget
```

## Development

```bash
./gradlew assembleDebug testDebugUnitTest   # build + unit tests
./gradlew installDebug                      # install on a connected device
```

- [`SPEC.md`](SPEC.md): the full product and architecture spec
- [`PROGRESS.md`](PROGRESS.md): build log, every design decision and the reasoning behind it

## Privacy

Penny Wallet requests **no internet permission**. It has no accounts, no analytics and no crash reporting.
The only data that leaves the app is the backup or CSV file you write to a folder you choose.

---

<div align="center">
<sub>Free, offline, in English, German and Italian.</sub>
</div>
