#!/usr/bin/env bash
# The app must stay offline: fail if any merged release manifest asks for network access.
set -euo pipefail
cd "$(dirname "$0")/.."
found=$(find app/build/intermediates -path '*release*' -name AndroidManifest.xml -path '*merged_manifest*' 2>/dev/null || true)
[ -n "$found" ] || { echo "No merged release manifest found; run a release build first." >&2; exit 1; }
status=0
for m in $found; do
  if grep -E 'android.permission.(INTERNET|ACCESS_NETWORK_STATE)' "$m"; then
    echo "Network permission in $m" >&2; status=1
  fi
done
[ $status -eq 0 ] && echo "No network permissions in the merged manifest."
exit $status
