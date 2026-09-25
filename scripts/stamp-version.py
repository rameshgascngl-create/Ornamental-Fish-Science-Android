#!/usr/bin/env python3
"""Force user-facing strings to 2.6.0 after the v2.4.4 fetch + v2.5 overlay."""
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
subs = [
    ('2.5.0', '2.6.0'),
    ('2.4.4', '2.6.0'),
]
for rel in [
    'app/src/main/assets/index.html',
    'app/src/main/assets/js/app.js',
    'app/src/main/assets/js/boot.js',
]:
    p = ROOT / rel
    if not p.exists():
        continue
    text = p.read_text(encoding='utf-8')
    orig = text
    for a, b in subs:
        text = text.replace(a, b)
    if 'JSON.parse(localStorage.getItem("of_marks")' in text and 'function readJson' not in text:
        text = text.replace(
            'marks: JSON.parse(localStorage.getItem("of_marks") || "[]"),\n    answered: JSON.parse(localStorage.getItem("of_answered") || "{}"),',
            'marks: readJson("of_marks", []),
    answered: readJson("of_answered", {}),' )
        helper = '''  function readJson(key, fallback) {
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return fallback;
      return JSON.parse(raw);
    } catch (e) { return fallback; }
  }
'''
        text = text.replace('  function save() {', helper + '  function save() {', 1)
    if text != orig:
        p.write_text(text, encoding='utf-8')
        print('stamped', rel)
