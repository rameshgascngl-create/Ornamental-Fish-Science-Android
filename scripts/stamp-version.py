#!/usr/bin/env python3
"""Stamp user-facing 2.6.0 after integrity check. Do not run before sha256sum."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
READ_JSON = """  function readJson(key, fallback) {
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return fallback;
      return JSON.parse(raw);
    } catch (e) { return fallback; }
  }
"""

OLD_PARSE = (
    'marks: JSON.parse(localStorage.getItem("of_marks") || "[]"),\n'
    '    answered: JSON.parse(localStorage.getItem("of_answered") || "{}"),'
)
NEW_PARSE = 'marks: readJson("of_marks", []),
    answered: readJson("of_answered", {}),' 


def stamp(path: Path) -> None:
    if not path.exists():
        return
    text = path.read_text(encoding="utf-8")
    orig = text
    text = text.replace("2.5.0", "2.6.0").replace("2.4.4", "2.6.0")
    if path.name == "app.js" and "function readJson" not in text and OLD_PARSE in text:
        text = text.replace(OLD_PARSE, NEW_PARSE, 1)
        text = text.replace("  function save() {", READ_JSON + "  function save() {", 1)
    if text != orig:
        path.write_text(text, encoding="utf-8")
        print("stamped", path.relative_to(ROOT))


def main() -> None:
    assets = ROOT / "app/src/main/assets"
    stamp(assets / "index.html")
    stamp(assets / "js/app.js")
    # boot.js version label only; do not rewrite the frozen species snapshot.
    boot = assets / "js/boot.js"
    if boot.exists():
        text = boot.read_text(encoding="utf-8")
        # leave embedded payload hashes alone; only the banner string if present
        print("boot.js left at frozen academic snapshot")


if __name__ == "__main__":
    main()
