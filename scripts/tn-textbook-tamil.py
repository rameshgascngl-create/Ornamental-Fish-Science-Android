#!/usr/bin/env python3
"""Rewrite user-facing Tamil toward TN school/college textbook register."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"

UI = [
    ("விவரணத்துக்கு மீனைத் தொடவும். பாடங்கள் Learn-இல்.",
     "இன விவரத்தைக் காண மீனைத் தொடவும். பாடங்கள் பாடம் பகுதியில் உள்ளன."),
    ("இனங்களை ஆயுங்கள்",
     "இனங்களை அறிக"),
    ("தேடும் விவரணம்",
     "தேடக்கூடிய இன விவரங்கள்"),
    ("அகவாரி அறிவியல்",
     "மீன்வளர்ப்பு அறிவியல்"),
    ("நீர் மற்றும் பண்ணைக் கணக்கிகள்",
     "நீர் மற்றும் பண்ணைக் கணக்கீடுகள்"),
    ("புத்தக்குறி",
     "புத்தகக்குறி"),
    ("உச்சரி",
     "உச்சரிக்க"),
    ("நலமும் தனிமையும்",
     "நலமும் தனிமைப்படுத்தலும்"),
    ("வளர்ப்பு வெப்பம்",
     "வளர்ப்பு வெப்பநிலை"),
    ("இனப்பெருக்க வெப்பம்",
     "இனப்பெருக்க வெப்பநிலை"),
    ("குறைந்த அமைப்பு",
     "குறைந்தபட்ச தொட்டி"),
    ("இணை வாழ்தல்",
     "இணக்கத்தன்மை"),
]

NAMES = {
    "oranda": "ஒராண்டா (தங்கமீனின் அலங்கார வகை)",
    "koi": "கோய் (அலங்காரக் கெண்டை)",
    "guppy": "குப்பி",
    "molly": "சாதாரண மொல்லி",
    "swordtail": "வாள்வால்",
    "betta": "சண்டை மீன் / பீட்டா",
    "gourami": "மூன்று புள்ளி கೌராமி",
    "angelfish": "ஏஞ்சல் மீன்",
    "tigerbarb": "புலிப்பார்ப்",
    "zebra": "வரிக்குதிரை டேனியோ",
    "rosybarb": "இளஞ்சிரும்ப் பார்ப்",
    "pearlspot": "கரிமீன் / முத்துக்கண்ணி",
    "rasbora": "ஹார்லிக்கின் ரஸ்போரா",
    "clownfish": "கிளೌன் மீன் (கடல் எடுத்துக்காட்டு)",
}


def patch_js(path: Path) -> None:
    if not path.exists():
        return
    text = path.read_text(encoding="utf-8")
    orig = text
    for a, b in UI:
        text = text.replace(a, b)
    # filter chips that are single words — only inside t() Tamil slot leftovers
    text = text.replace('t("Livebearer","உள்பொரி")', 't("Livebearer","உயிருடன் ஈனும்")')
    text = text.replace('t("Egg-layer","முட்டையிடுபவை")', 't("Egg-layer","முட்டையிடும் இனங்கள்")')
    text = text.replace('t("Peaceful","அமைதி")', 't("Peaceful","அமைதியான")')
    text = text.replace('t("Active / territorial","சுறுசுறுப்பு")', 't("Active / territorial","சுறுசுறுப்பான / ஆள்புலம் காக்கும்")')
    text = text.replace('t("Indian native","தாயக")', 't("Indian native","இந்தியத் தாயகம்")')
    text = text.replace('t("Exotic","வெளிநாட்டு")', 't("Exotic","அயல்நாட்டு")')
    text = text.replace('t("Brackish","உவர்")', 't("Brackish","உவர்நீர்")')
    text = text.replace('t("Quarantine","தனிமை")', 't("Quarantine","தனிமைப்படுத்தல்")')
    if text != orig:
        path.write_text(text, encoding="utf-8")
        print("patched", path)


def patch_species(path: Path) -> None:
    if not path.exists():
        return
    data = json.loads(path.read_text(encoding="utf-8"))
    n = 0
    for s in data:
        sid = s.get("id")
        if sid in NAMES and s.get("ta") != NAMES[sid]:
            s["ta"] = NAMES[sid]
            n += 1
        for key, val in list(s.items()):
            if isinstance(val, str) and "இரகம்" in val:
                s[key] = val.replace("இரகம்", "வகை")
                n += 1
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("species tamil patches", n)


def main() -> None:
    patch_js(ASSETS / "js/app.js")
    patch_species(ASSETS / "data/species.json")


if __name__ == "__main__":
    main()
