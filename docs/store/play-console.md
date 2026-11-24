# Play Console answers

## Data safety

- **Does your app collect or share any of the required user data types?** No.
  - The app has no internet permission; nothing leaves the phone through the app.
  - Receipt photos are processed on the device (ML Kit bundled model) and never transmitted.
  - Backups/CSV go only to a location the user picks (user-initiated export, not collection).
  - Crash reports are sent only by the user from their own email app, after seeing the text. This is user-initiated sharing through another app, not collection by Penny Wallet. If the review asks for a stricter reading, declare **App info and performance → Crash logs**: collected, optional, not shared, purpose *App functionality*, not processed ephemerally.
  - Tips: payment is handled by Google Play; the app receives no payment data.
- **Is all data encrypted in transit?** Not applicable (no data transmitted).
- **Do you provide a way for users to request deletion?** Yes: Settings → More options → Erase all data, or uninstall.

## App content

- **Privacy policy:** https://github.com/Aliyov/penny-wallet/blob/main/docs/PRIVACY.md
- **Ads:** No ads.
- **App access:** All functionality available without login.
- **Content rating (IARC):** Utility/productivity, no violence, no user interaction, no purchases of digital goods beyond tips (in-app purchases: yes), no location sharing → expected rating PEGI 3 / Everyone.
- **Target audience:** 18+ (finance app; not designed for children).
- **News app:** No. **Government app:** No. **Financial features:** declare "None of the above" (no loans, banking, crypto or payments between users); it is a personal budgeting tool.
- **Health apps:** No.

## In-app products (Monetize → Products → In-app products)

| Product ID | Name (EN) | Price |
|---|---|---|
| `tip_tea` | Buy me a tea | €2.00 |
| `tip_cappuccino` | Cappuccino | €4.00 |

Both are one-time products. The app consumes them so they can be bought again; nothing is unlocked.

## Release

- Build the bundle: `./gradlew bundleRelease -Ppenny.signUpload=true` → `app/build/outputs/bundle/release/penny-wallet-release.aab`
- Upload key: `~/penny-wallet-keys/upload.jks`, passwords in `~/.gradle/gradle.properties` (`PENNY_UPLOAD_*`). Keep a copy of both outside this computer (password manager). Play App Signing holds the app signing key; a lost upload key can be reset through Play support.
- Upload certificate SHA-256: `07:06:86:4D:E5:E5:A5:66:BD:2A:7C:04:5B:CC:20:B2:7C:96:25:CB:B5:DB:EE:5A:00:AE:5B:84:74:01:6F:AC`
- New personal developer accounts need a **closed test with at least 12 testers for 14 days** before production access (P6).
