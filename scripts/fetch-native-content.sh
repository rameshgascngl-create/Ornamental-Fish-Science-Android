#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
URL='https://raw.githubusercontent.com/rameshgascngl-create/Zoology-and-Life-Sciences-Digital-Learning-Resources/main/ornamental-fish-science/OrnamentalFish-v2.4.4-NEW-STORE-SOURCE.zip'
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

curl -fsSL -o "$TMP/src.zip" "$URL"
unzip -qo "$TMP/src.zip" -d "$TMP/src"
SRC="$TMP/src/app/src/main"

(cd "$SRC" && sha256sum -c "$ROOT/release-evidence/ACADEMIC_PAYLOAD_SHA256_v2.4.3.txt")

rm -rf "$ROOT/app/src/main/assets/data" "$ROOT/app/src/main/assets/fish_atlas"
mkdir -p "$ROOT/app/src/main/assets/data" "$ROOT/app/src/main/assets/fish_atlas" "$ROOT/app/src/main/res/drawable"
cp -a "$SRC/assets/data/." "$ROOT/app/src/main/assets/data/"
cp -a "$SRC/assets/fish_atlas/." "$ROOT/app/src/main/assets/fish_atlas/"
cp "$SRC/res/drawable/app_icon.png" "$ROOT/app/src/main/res/drawable/app_icon.png"

python3 "$ROOT/scripts/phase2-content-polish.py"

rm -f "$ROOT/app/src/main/assets/index.html" "$ROOT/app/src/main/assets/privacy-policy.html"
rm -rf "$ROOT/app/src/main/assets/js" "$ROOT/app/src/main/assets/css"

echo "Native Phase-2 payload prepared: verified source + bilingual content polish + atlas images only."
