#!/usr/bin/env python3
from pathlib import Path
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "app/src/main/assets/data"
ATLAS = ROOT / "app/src/main/assets/fish_atlas"

EXPECTED = {
    "species.json": 22,
    "book.json": 37,
    "quiz.json": 21,
    "labs.json": 5,
}

BANNED = [
    "TNAU",
    "உள்பொரி",
    "அகவாரி",
    "ஒரண்டா",
    "ஸெப்ரா டேனியோ",
    "ஃபேன்சி",
    "v1 printed",
    "critical defect in v1",
    "குஞ்சுகளை ஈனும்யும்",
]

SPECIES_REQUIRED = [
    "id","en","ta","sci","family","order","native","status_in",
    "size_cm","life_yr","temper","tank_min_l","group","temp_hold_c",
    "temp_breed_c","ph","gh_kh","salinity","diet","breed","sex","fry",
    "compat","disease","quarantine","iucn","img","origin","habitat",
    "breed_type","temper_class",
    "ta_native","ta_status_in","ta_size_cm","ta_life_yr","ta_temper",
    "ta_tank_min_l","ta_group","ta_temp_hold_c","ta_temp_breed_c",
    "ta_ph","ta_gh_kh","ta_salinity","ta_diet","ta_breed","ta_sex",
    "ta_fry","ta_compat","ta_disease","ta_quarantine","ta_iucn",
]

SAFE_HTML_TAGS = {"p","b","i","ol","ul","li","div","br"}

def die(message):
    print("CONTENT VALIDATION FAIL:", message, file=sys.stderr)
    sys.exit(1)

def read(name):
    path = DATA / name
    if not path.exists():
        die(f"missing {name}")
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        die(f"{name} is not valid UTF-8 JSON: {exc}")

def require_unique_ids(rows, label):
    ids = [str(x.get("id","")).strip() for x in rows]
    if any(not x for x in ids):
        die(f"{label}: blank id")
    if len(set(ids)) != len(ids):
        die(f"{label}: duplicate id")

def contains_tamil(text):
    return any("\u0B80" <= ch <= "\u0BFF" for ch in text)

def html_tags(text):
    return {
        m.group(1).lower()
        for m in re.finditer(r"<\/?\s*([a-zA-Z0-9]+)", text)
    }

species = read("species.json")
book = read("book.json")
quiz = read("quiz.json")
labs = read("labs.json")

for name, rows in [
    ("species.json", species),
    ("book.json", book),
    ("quiz.json", quiz),
    ("labs.json", labs),
]:
    if len(rows) != EXPECTED[name]:
        die(f"{name}: expected {EXPECTED[name]} records, found {len(rows)}")

require_unique_ids(species, "species")
require_unique_ids(book, "book")
require_unique_ids(labs, "labs")

for s in species:
    sid = s["id"]
    for key in SPECIES_REQUIRED:
        if key not in s or not str(s[key]).strip():
            die(f"species {sid}: missing/blank {key}")

    if not re.match(r"^[A-Z][a-zA-Z-]+\s+[a-z][a-zA-Z-]+", s["sci"].strip()):
        die(f"species {sid}: scientific name is not binomial-like: {s['sci']}")

    tamil_language_fields = [
        k for k in SPECIES_REQUIRED
        if k.startswith("ta_") and k not in {"ta_ph"}
    ] + ["ta"]
    for key in tamil_language_fields:
        value = str(s.get(key,"")).strip()
        if len(value) > 4 and not contains_tamil(value):
            die(f"species {sid}: Tamil field {key} lacks Tamil text")

    image = ATLAS / s["img"]
    if not image.is_file() or image.stat().st_size == 0:
        die(f"species {sid}: missing image {s['img']}")

    if s["origin"] not in {"indigenous","exotic"}:
        die(f"species {sid}: invalid origin {s['origin']}")
    if s["habitat"] not in {"freshwater","brackish","marine"}:
        die(f"species {sid}: invalid habitat {s['habitat']}")
    if s["breed_type"] not in {"egg-layer","livebearer"}:
        die(f"species {sid}: invalid breed_type {s['breed_type']}")

for page in book:
    pid = page["id"]
    for key in ("ch","en_t","ta_t","en","ta"):
        if key not in page or not str(page[key]).strip():
            die(f"book {pid}: missing/blank {key}")
    if not contains_tamil(page["ta_t"]) or not contains_tamil(page["ta"]):
        die(f"book {pid}: Tamil parity missing")
    tags = html_tags(page["en"]) | html_tags(page["ta"])
    unsupported = tags - SAFE_HTML_TAGS
    if unsupported:
        die(f"book {pid}: unsupported native-rich-text tags {sorted(unsupported)}")
    lower = (page["en"] + page["ta"]).lower()
    if "<script" in lower or "javascript:" in lower or "<iframe" in lower:
        die(f"book {pid}: executable/embedded web markup not allowed")

for i, q in enumerate(quiz):
    for key in ("q","qt","o","ot","a","e","et","type"):
        if key not in q:
            die(f"quiz {i}: missing {key}")
    if not str(q["q"]).strip() or not str(q["qt"]).strip():
        die(f"quiz {i}: blank question")
    if not contains_tamil(str(q["qt"])) or not contains_tamil(str(q["et"])):
        die(f"quiz {i}: Tamil semantic parity missing")
    if not isinstance(q["o"], list) or not isinstance(q["ot"], list):
        die(f"quiz {i}: options must be lists")
    if len(q["o"]) < 2 or len(q["o"]) != len(q["ot"]):
        die(f"quiz {i}: English/Tamil option count mismatch")
    if not isinstance(q["a"], int) or not 0 <= q["a"] < len(q["o"]):
        die(f"quiz {i}: invalid answer index {q['a']}")
    if any(not str(x).strip() for x in q["o"] + q["ot"]):
        die(f"quiz {i}: blank option")

for lab in labs:
    lid = lab["id"]
    for key in ("en_t","ta_t","en","ta"):
        if key not in lab or not str(lab[key]).strip():
            die(f"lab {lid}: missing/blank {key}")
    if not contains_tamil(lab["ta_t"]) or not contains_tamil(lab["ta"]):
        die(f"lab {lid}: Tamil parity missing")
    tags = html_tags(lab["en"]) | html_tags(lab["ta"])
    unsupported = tags - SAFE_HTML_TAGS
    if unsupported:
        die(f"lab {lid}: unsupported native-rich-text tags {sorted(unsupported)}")

combined = "\n".join(
    (DATA / name).read_text(encoding="utf-8")
    for name in EXPECTED
)
for term in BANNED:
    if term.lower() in combined.lower():
        die(f"banned learner-facing wording remains: {term}")

print(
    "NATIVE CONTENT VALIDATION: PASS — "
    f"{len(species)} species, {len(book)} lessons, "
    f"{len(quiz)} quiz questions, {len(labs)} practicals"
)
