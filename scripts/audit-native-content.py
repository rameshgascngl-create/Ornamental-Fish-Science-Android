#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "app" / "src" / "main" / "assets" / "data"

BANNED = (
    "TNAU",
    "Curriculum",
    "Current Science",
    "பாடத்திட்டம்",
    "நவீன அறிவியல்",
    "அகவாரி",
    "உள்பொரி",
    "அகவாகல்கல்ச்சர்",
    "பாய்மீன்",
    "அருகிய",
)

TAMIL = re.compile(r"[\u0B80-\u0BFF]")

def load(name):
    return json.loads((DATA / name).read_text(encoding="utf-8"))

def iter_strings(value, path="$"):
    if isinstance(value, dict):
        for key, child in value.items():
            yield from iter_strings(child, path + "." + key)
    elif isinstance(value, list):
        for index, child in enumerate(value):
            yield from iter_strings(child, path + f"[{index}]")
    elif isinstance(value, str):
        yield path, value

def fail(message):
    print("CONTENT AUDIT FAIL:", message)
    sys.exit(1)

datasets = {
    "species": load("species.json"),
    "book": load("book.json"),
    "quiz": load("quiz.json"),
    "labs": load("labs.json"),
}

for dataset_name, dataset in datasets.items():
    for path, text in iter_strings(dataset):
        for banned in BANNED:
            if banned.lower() in text.lower():
                fail(f"{dataset_name}{path}: banned learner-facing wording {banned!r}")

# English-labelled learner fields must not contain Tamil-script prose.
for i, item in enumerate(datasets["book"]):
    for key in ("en_t", "en"):
        if TAMIL.search(item.get(key, "")):
            fail(f"book[{i}].{key}: Tamil script leaked into English mode")

for i, item in enumerate(datasets["species"]):
    for key in (
        "en", "native", "status_in", "size_cm", "life_yr", "temper",
        "tank_min_l", "group", "temp_hold_c", "temp_breed_c", "ph",
        "gh_kh", "salinity", "diet", "breed", "sex", "fry", "compat",
        "disease", "quarantine", "iucn", "alt_en"
    ):
        if TAMIL.search(item.get(key, "")):
            fail(f"species[{i}].{key}: Tamil script leaked into English mode")

for i, item in enumerate(datasets["quiz"]):
    for key in ("q", "e"):
        if TAMIL.search(item.get(key, "")):
            fail(f"quiz[{i}].{key}: Tamil script leaked into English mode")
    for j, option in enumerate(item.get("o", [])):
        if TAMIL.search(option):
            fail(f"quiz[{i}].o[{j}]: Tamil script leaked into English mode")

for i, item in enumerate(datasets["labs"]):
    for key in ("en_t", "en"):
        if TAMIL.search(item.get(key, "")):
            fail(f"labs[{i}].{key}: Tamil script leaked into English mode")

expected = {"species": 22, "book": 37, "quiz": 21, "labs": 5}
for name, count in expected.items():
    if len(datasets[name]) != count:
        fail(f"{name}: expected {count} records, found {len(datasets[name])}")

print("CONTENT AUDIT PASS: bilingual separation, terminology gate and record counts verified.")
