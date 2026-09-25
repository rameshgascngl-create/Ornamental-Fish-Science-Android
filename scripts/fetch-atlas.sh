#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
URL='https://raw.githubusercontent.com/rameshgascngl-create/Zoology-and-Life-Sciences-Digital-Learning-Resources/main/ornamental-fish-science/OrnamentalFish-v2.4.4-NEW-STORE-SOURCE.zip'
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
echo "Downloading v2.4.4 source zip…"
curl -fsSL -o "$TMP/src.zip" "$URL"
unzip -qo "$TMP/src.zip" -d "$TMP/src"
SRC="$TMP/src/app/src/main"
mkdir -p "$ROOT/app/src/main/assets/fish_atlas" \
         "$ROOT/app/src/main/assets/data" \
         "$ROOT/app/src/main/assets/js" \
         "$ROOT/app/src/main/assets/css" \
         "$ROOT/app/src/main/res/drawable"
cp -a "$SRC/assets/fish_atlas/." "$ROOT/app/src/main/assets/fish_atlas/"
cp -a "$SRC/assets/data/." "$ROOT/app/src/main/assets/data/"
cp "$SRC/assets/js/boot.js" "$ROOT/app/src/main/assets/js/boot.js"
cp "$SRC/assets/js/app.js" "$ROOT/app/src/main/assets/js/app.js"
cp "$SRC/assets/css/app.css" "$ROOT/app/src/main/assets/css/app.css"
cp "$SRC/assets/privacy-policy.html" "$ROOT/app/src/main/assets/privacy-policy.html"
cp "$SRC/res/drawable/app_icon.png" "$ROOT/app/src/main/res/drawable/app_icon.png"
if [ ! -f "$ROOT/app/src/main/assets/index.html" ]; then
  cp "$SRC/assets/index.html" "$ROOT/app/src/main/assets/index.html"
fi
if [ -f "$ROOT/scripts/apply-v250-overlay.py" ]; then
  python3 "$ROOT/scripts/apply-v250-overlay.py"
fi
echo "Academic payload restored; integrity must be verified before v2.6.1 stamping."
