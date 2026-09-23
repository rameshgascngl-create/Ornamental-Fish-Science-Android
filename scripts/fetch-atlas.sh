#!/usr/bin/env bash
# Restores frozen academic payload (atlas JPEGs, JSON, boot.js) and launcher
# icon from the public v2.4.4 source zip. Does not overwrite v2.5.0 app.js.
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
         "$ROOT/app/src/main/res/drawable"
cp -a "$SRC/assets/fish_atlas/." "$ROOT/app/src/main/assets/fish_atlas/"
cp -a "$SRC/assets/data/." "$ROOT/app/src/main/assets/data/"
cp "$SRC/assets/js/boot.js" "$ROOT/app/src/main/assets/js/boot.js"
cp "$SRC/res/drawable/app_icon.png" "$ROOT/app/src/main/res/drawable/app_icon.png"
echo "Atlas, data JSON, boot.js and app_icon.png restored."
