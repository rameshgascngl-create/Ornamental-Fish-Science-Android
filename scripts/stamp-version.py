#!/usr/bin/env python3
"""Stamp user-facing 2.6.1 after integrity check. Do not run before sha256sum."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION = "2.6.1"
READ_JSON = (
    "  function readJson(key, fallback) {\n"
    "    try {\n"
    "      const raw = localStorage.getItem(key);\n"
    "      if (!raw) return fallback;\n"
    "      return JSON.parse(raw);\n"
    "    } catch (e) { return fallback; }\n"
    "  }\n"
)
OLD_PARSE = (
    "marks: JSON.parse(localStorage.getItem(\"of_marks\") || \"[]\"),\n"
    "    answered: JSON.parse(localStorage.getItem(\"of_answered\") || \"{}\"),"
)
NEW_PARSE = (
    "marks: readJson(\"of_marks\", []),\n"
    "    answered: readJson(\"of_answered\", {}),"
)
PRIVACY_NOTE = (
    "<p>If you choose a revision reminder, the app may request notification "
    "permission and store the reminder time on this device only. Nothing is sent "
    "to a server. You can refuse the permission; the lesson content still works.</p>"
)


def stamp(path: Path) -> None:
    if not path.exists():
        return
    text = path.read_text(encoding="utf-8")
    orig = text
    for old in ("2.6.0", "2.5.0", "2.4.4"):
        text = text.replace(old, VERSION)
    if path.name == "app.js" and "function readJson" not in text and OLD_PARSE in text:
        text = text.replace(OLD_PARSE, NEW_PARSE, 1)
        text = text.replace("  function save() {", READ_JSON + "  function save() {", 1)
    if path.name == "privacy-policy.html" and "revision reminder" not in text:
        if "</body>" in text:
            text = text.replace("</body>", PRIVACY_NOTE + "\n</body>", 1)
    if text != orig:
        path.write_text(text, encoding="utf-8")
        print("stamped", path.relative_to(ROOT))


def main() -> None:
    assets = ROOT / "app/src/main/assets"
    stamp(assets / "index.html")
    stamp(assets / "js/app.js")
    stamp(assets / "privacy-policy.html")
    print("boot.js left at frozen academic snapshot")


if __name__ == "__main__":
    main()
