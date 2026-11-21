#!/usr/bin/env bash
# On-phone receipt OCR smoke test with the real release (R8) code path.
# Installs a separate app (com.penny.wallet.smoke) so the real app's data is never touched, runs ML Kit +
# the parser on every photo in Test_Recipe/, compares with Test_Recipe/expected.txt, then uninstalls it.
#   expected.txt lines: "<file name> <total in cents>", e.g. "recipe.jpg 308". Lines starting with # are ignored.
#   Usage: scripts/ocr-smoke.sh [-v]   (-v also prints every OCR line and row)
set -euo pipefail
cd "$(dirname "$0")/.."
ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
PKG=com.penny.wallet.smoke
RECEIVER="$PKG/com.penny.wallet.debug.OcrProbeReceiver"
DIR="/sdcard/Android/data/$PKG/files/probe"
VERBOSE=false; [[ "${1:-}" == "-v" ]] && VERBOSE=true

[[ $("$ADB" devices | grep -cw device) -ge 1 ]] || { echo "No phone connected (adb devices)"; exit 2; }
shopt -s nullglob nocaseglob
photos=(Test_Recipe/*.jpg Test_Recipe/*.jpeg Test_Recipe/*.png Test_Recipe/*.webp)
[[ ${#photos[@]} -gt 0 ]] || { echo "No photos in Test_Recipe/"; exit 2; }

echo "Building smoke APK…"
./gradlew -q assembleSmoke
"$ADB" install -r app/build/outputs/apk/smoke/penny-wallet-smoke.apk >/dev/null
trap '"$ADB" uninstall "$PKG" >/dev/null 2>&1 || true' EXIT

"$ADB" shell mkdir -p "$DIR"
for p in "${photos[@]}"; do "$ADB" push "$p" "$DIR/" >/dev/null; done
# A freshly installed app is "stopped" and MIUI/HyperOS also block broadcasts to apps never opened:
# open the smoke app once (its own seeded data only), then send the broadcast.
"$ADB" shell am start -W -n "$PKG/com.penny.wallet.MainActivity" >/dev/null
"$ADB" logcat -c
# The first broadcast can be dropped while the app is still starting: resend until the probe says START.
started=false
for _ in $(seq 1 6); do
  "$ADB" shell am broadcast -n "$RECEIVER" --ez verbose "$VERBOSE" >/dev/null
  for _ in $(seq 1 5); do
    # (capture first: with pipefail, `logcat | grep -q` fails when grep closes the pipe early)
    out=$("$ADB" logcat -d -s 'OcrProbe:*'); grep -q "START" <<<"$out" && { started=true; break 2; }
    sleep 1
  done
done
$started || { echo "Probe never started (is the phone unlocked?)"; exit 1; }
for _ in $(seq 1 180); do
  out=$("$ADB" logcat -d -s 'OcrProbe:*'); grep -q "DONE" <<<"$out" && break
  sleep 1
done
log=$("$ADB" logcat -d -s 'OcrProbe:*' | sed 's/^.*OcrProbe: //')
grep -q "^DONE" <<<"$log" || { echo "Probe did not finish (timeout). Log:"; echo "$log" | tail -20; exit 1; }
$VERBOSE && grep -E "^(LINE|ROW) " <<<"$log"

fail=0; checked=0
while read -r name cents; do
  [[ -z "$name" || "$name" == \#* ]] && continue
  line=$(grep -E "^RESULT file=$name " <<<"$log" || true)
  if [[ -z "$line" ]]; then echo "MISSING  $name (no photo or no result)"; fail=1; continue; fi
  got=$(sed -E 's/.*total=([^ ]+).*/\1/' <<<"$line")
  checked=$((checked + 1))
  if [[ "$got" == "$cents" ]]; then echo "OK       $line"; else echo "WRONG    expected=$cents  $line"; fail=1; fi
done < <(cat Test_Recipe/expected.txt 2>/dev/null || true)
# Photos without an expectation: show what was read.
grep -E "^RESULT " <<<"$log" | while read -r l; do
  n=$(sed -E 's/RESULT file=([^ ]+).*/\1/' <<<"$l"); grep -q "^$n " Test_Recipe/expected.txt 2>/dev/null || echo "UNCHECKED $l"
done
echo "Checked $checked photo(s)."
exit $fail
