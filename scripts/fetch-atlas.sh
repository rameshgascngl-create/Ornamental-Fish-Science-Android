#!/usr/bin/env bash
# Copies frozen atlas JPEGs + launcher icon from the public v2.4.4 source zip.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
URL='https://raw.githubusercontent.com/rameshgascngl-create/Zoology-and-Life-Sciences-Digital-Learning-Resources/main/ornamental-fish-science/OrnamentalFish-v2.4.4-NEW-STORE-SOURCE.zip'
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
echo "Downloading v2.4.4 source zip…"
curl -fsSL -o "$TMP/src.zip" "$URL"
unzip -qo "$TMP/src.zip" -d "$TMP/src"
mkdir -p "$ROOT/app/src/main/assets/fish_atlas" "$ROOT/app/src/main/res/drawable"
cp -a "$TMP/src/app/src/main/assets/fish_atlas/." "$ROOT/app/src/main/assets/fish_atlas/"
cp "$TMP/src/app/src/main/res/drawable/app_icon.png" "$ROOT/app/src/main/res/drawable/app_icon.png"
echo "Atlas images and app_icon.png restored."
