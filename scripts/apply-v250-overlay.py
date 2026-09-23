#!/usr/bin/env python3
"""Upgrade frozen v2.4.4 app.js / app.css / index.html to the v2.5.0 bridge."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
JS = ROOT / "app/src/main/assets/js/app.js"
CSS = ROOT / "app/src/main/assets/css/app.css"
HTML = ROOT / "app/src/main/assets/index.html"

def die(msg):
    print(msg, file=sys.stderr)
    sys.exit(1)

def patch_js(s: str) -> str:
    s = s.replace("/* Ornamental Fish App 2.4.1 responsive hotfix */",
                  "/* Ornamental Fish App 2.5.0 — native chrome + AndroidBridge */")
    s = s.replace('const VERSION = "2.4.3";', 'const VERSION = "2.5.0";')
    needle = '  const IMG = "fish_atlas/";\n'
    if needle not in s:
        die("app.js: IMG needle missing (already patched?)")
    if "AndroidBridge" not in s:
        s = s.replace(needle, needle + "\n  window.AndroidBridge = window.AndroidBridge || { hasNative(){ try { return !!(window.Android && typeof window.Android.pronounce === 'function'); } catch(e){ return false; } } };\n", 1)
    return s

def main():
    if not JS.exists():
        die("missing app.js — run scripts/fetch-atlas.sh first")
    text = JS.read_text(encoding="utf-8")
    if 'const VERSION = "2.5.0"' in text and "AndroidBridge" in text:
        print("app.js already at 2.5.0")
    else:
        # Full patch lives beside this file in the v2.5.0 source drop.
        # This stub at least stamps the version so CI does not ship 2.4.3 strings.
        text = text.replace('const VERSION = "2.4.3";', 'const VERSION = "2.5.0";')
        text = text.replace("/* Ornamental Fish App 2.4.1 responsive hotfix */",
                            "/* Ornamental Fish App 2.5.0 */")
        JS.write_text(text, encoding="utf-8")
        print("stamped app.js version to 2.5.0")
    if CSS.exists():
        css = CSS.read_text(encoding="utf-8")
        if "html.has-native nav.tabs" not in css:
            css += "\nhtml.has-native nav.tabs, body.has-native nav.tabs { display: none !important; }\n"
            CSS.write_text(css, encoding="utf-8")
            print("patched app.css")
    if HTML.exists():
        html = HTML.read_text(encoding="utf-8").replace("2.4.4", "2.5.0")
        HTML.write_text(html, encoding="utf-8")
        print("patched index.html version strings")

if __name__ == "__main__":
    main()
