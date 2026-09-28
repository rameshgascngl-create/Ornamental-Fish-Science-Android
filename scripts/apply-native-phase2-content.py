#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "app" / "src" / "main" / "assets" / "data"

def load(name):
    return json.loads((DATA / name).read_text(encoding="utf-8"))

def save(name, value):
    (DATA / name).write_text(
        json.dumps(value, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

def walk_replace(value, pairs):
    if isinstance(value, dict):
        return {k: walk_replace(v, pairs) for k, v in value.items()}
    if isinstance(value, list):
        return [walk_replace(v, pairs) for v in value]
    if isinstance(value, str):
        for old, new in pairs:
            value = value.replace(old, new)
        return value
    return value

species = load("species.json")
book = load("book.json")
quiz = load("quiz.json")
labs = load("labs.json")

# Tamil register: clear Tamil Nadu textbook-style terminology and natural usage.
tamil_replacements = [
    ("அகவாகல்கல்ச்சர்", "நீருயிரி வளர்ப்பு"),
    ("அகவாரி", "மீன் தொட்டி"),
    ("உள்பொரி குஞ்சு", "குஞ்சுகளை ஈனும் மீன்களின் குஞ்சு"),
    ("உள்பொரி மீன்கள்", "குஞ்சுகளை ஈனும் மீன்கள்"),
    ("உள்பொரியும்", "குஞ்சுகளை ஈனும் மீன்களும்"),
    ("உள்பொரி", "குஞ்சுகளை ஈனும் மீன்"),
    ("வெளிநாட்டு", "அந்நிய"),
    ("அருகிய", "அழியும் அபாயமுள்ள"),
    ("அத்துமீறிய", "ஆக்கிரமிப்பு அந்நிய"),
    ("பாய்மீன் மொல்லி", "செயில்ஃபின் மொல்லி"),
    ("தள உறுப்பு", "லேபிரிந்த் உறுப்பு"),
    ("ஸெப்ரா டேனியோ", "ஜீப்ரா டேனியோ"),
    ("ஸெப்ரா", "ஜீப்ரா"),
    ("கத்திவால்", "வாள்வால் மீன்"),
    ("கோர்மி", "கௌராமி"),
    ("கிளௌன்", "கிளவுன்"),
    ("மாய அட்டவணை", "ஒரே பொதுவான அட்டவணை"),
]
species = walk_replace(species, tamil_replacements)
book = walk_replace(book, tamil_replacements)
quiz = walk_replace(quiz, tamil_replacements)
labs = walk_replace(labs, tamil_replacements)

species_by_id = {item["id"]: item for item in species}

species_updates = {
    "goldfish": {
        "en": "Goldfish (common and fantail forms)",
        "temp_breed_c": "Spawning is often induced by fresh water and a modest temperature rise within the breeding range, commonly around 20–24 °C.",
        "breed": "Adhesive egg-scatterer. A practical breeding group may use 1 female with 2 males. Females may release several thousand eggs; hatching is commonly about 2–4 days depending on temperature.",
        "ta_temp_breed_c": "புதுநீர் சேர்த்தலும் இனப்பெருக்க வரம்பிற்குள் மிதமான வெப்பநிலை உயர்வும் முட்டையிடலைத் தூண்டலாம்; பொதுவாக 20–24°செ. ஏற்ற வரம்பாகும்.",
        "ta_breed": "ஒட்டும் முட்டைகளைச் சிதறவிடும் மீன். நடைமுறை இனப்பெருக்கத்தில் 1 பெண் : 2 ஆண் குழு பயன்படுத்தலாம். பெண் பல ஆயிரம் முட்டைகள் இடலாம்; வெப்பநிலையைப் பொறுத்து பொதுவாக 2–4 நாளில் பொரியும்.",
    },
    "koi": {"ta": "கோய் (அலங்காரக் கார்ப்)"},
    "molly": {"ta": "மொல்லி மீன்"},
    "sailfin": {
        "ta": "செயில்ஃபின் மொல்லி",
        "ta_sex": "ஆணில் பெரிய செயில்ஃபின் முதுகுத் துடுப்பு தெளிவாகக் காணப்படும்.",
    },
    "swordtail": {"ta": "வாள்வால் மீன்"},
    "betta": {"ta": "சியாமீஸ் சண்டை மீன் (பீட்டா)"},
    "gourami": {"ta": "மூன்று புள்ளி கௌராமி"},
    "zebra": {"ta": "ஜீப்ரா டேனியோ"},
    "clownfish": {"ta": "ஒசெல்லாரிஸ் கிளவுன் மீன் (கடல் எடுத்துக்காட்டு)"},
}
for sid, values in species_updates.items():
    species_by_id[sid].update(values)

for item in species:
    for key, value in list(item.items()):
        if not isinstance(value, str):
            continue
        value = value.replace("Curriculum farm pair ", "Practical breeding group ")
        value = value.replace("(curriculum ratio)", "")
        value = value.replace(
            "(curriculum: குஞ்சுகளை ஈனும் மீன் முட்டையிடுவன / ovo-viviparous).",
            "(lecithotrophic viviparity; embryos are nourished mainly by yolk).",
        )
        value = value.replace("TNAU Agritech; ", "").replace("TNAU Agritech", "")
        value = value.replace("TNAU farm texts also quote 20–30", "")
        value = value.replace("TNAU", "")
        item[key] = re.sub(r"\s{2,}", " ", value).strip()

book_by_id = {item["id"]: item for item in book}

title_updates = {
    "t0": ("Unit I — Introduction and scope of ornamental fish culture", "அலகு I — அலங்கார மீன் வளர்ப்பு: அறிமுகமும் பரப்பும்"),
    "i1": ("1.1 Introduction to ornamental fish culture", "1.1 அலங்கார மீன் வளர்ப்பு — அறிமுகம்"),
    "i2": ("1.2 Ornamental fish culture in India and Tamil Nadu", "1.2 இந்தியாவிலும் தமிழ்நாட்டிலும் அலங்கார மீன் வளர்ப்பு"),
    "i3": ("1.3 Indigenous and exotic species", "1.3 தாயக இனங்களும் அந்நிய இனங்களும்"),
    "i4": ("1.4 Species compatibility", "1.4 மீன்களின் இணக்கத்தன்மை"),
    "ii1": ("2.1 Egg-layers and livebearers", "2.1 முட்டையிடும் மீன்களும் குஞ்சுகளை ஈனும் மீன்களும்"),
    "ii2": ("2.2 Goldfish: maintenance and breeding temperature", "2.2 தங்கமீன்: பராமரிப்பு மற்றும் இனப்பெருக்க வெப்பநிலை"),
    "ii3": ("2.3 Guppy breeding and nursery management", "2.3 கப்பி இனப்பெருக்கமும் குஞ்சுப் பராமரிப்பும்"),
    "iv3": ("4.3 Nitrogen cycle in the aquarium", "4.3 மீன் தொட்டியின் நைட்ரஜன் சுழற்சி"),
    "iv4": ("4.4 Testing source water", "4.4 நீராதாரத்தைப் பரிசோதித்தல்"),
    "iv5": ("4.5 Do mollies require salt?", "4.5 மொல்லி மீன்களுக்கு உப்பு தேவையா?"),
    "iv6": ("4.6 Disease recognition and first response", "4.6 நோய் அறிகுறிகளும் முதல் நடவடிக்கையும்"),
    "iv7": ("4.7 Quarantine and biosecurity", "4.7 தனிமைப்படுத்தலும் உயிரியல் பாதுகாப்பும்"),
    "v1": ("5.1 Conditioning and packing fish for transport", "5.1 போக்குவரத்துக்கு முன் தயார்படுத்தலும் பொதியிடலும்"),
    "v2": ("5.2 Basic farm economics", "5.2 அடிப்படை பண்ணைப் பொருளாதாரம்"),
    "in2": ("Threatened endemic and invasive species", "அழியும் அபாயமுள்ள தனித்தாயக இனங்களும் ஆக்கிரமிப்பு அந்நிய இனங்களும்"),
    "rev": ("Key concepts for revision", "மீள்பார்வைக்கான முக்கியக் கருத்துகள்"),
    "an1": ("External anatomy used in identification", "இனங்காண உதவும் வெளிப்புற உடலமைப்பு"),
    "reg1": ("Indian fisheries and conservation institutions", "இந்திய மீன்வள மற்றும் பாதுகாப்பு நிறுவனங்கள்"),
    "i5": ("1.5 Commercially important exotic freshwater fishes", "1.5 வணிக முக்கியத்துவம் வாய்ந்த அந்நிய நன்னீர் அலங்கார மீன்கள்"),
    "iii3": ("3.3 Aquarium plants used in practical work", "3.3 செய்முறைப் பயிற்சியில் பயன்படும் மீன் தொட்டித் தாவரங்கள்"),
    "iv8": ("4.8 Aquarium design principles", "4.8 மீன் தொட்டி வடிவமைப்பின் அடிப்படைகள்"),
}
for bid, (en_title, ta_title) in title_updates.items():
    book_by_id[bid]["en_t"] = en_title
    book_by_id[bid]["ta_t"] = ta_title

rewrites = {
    "i1": (
        "<p>Ornamental fish culture is the breeding, rearing, maintenance and marketing of fishes valued for their colour, body form and behaviour. It differs from food-fish farming because the principal aim is display, recreation, education or trade rather than food production.</p><p><b>Learning objective:</b> Distinguish ornamental fish culture from food-fish aquaculture and identify the main activities involved in an ornamental-fish unit.</p>",
        "<p>அலங்கார மீன் வளர்ப்பு என்பது நிறம், உடல் வடிவம், நடத்தை ஆகியவற்றால் காட்சிக்குப் பயன்படும் மீன்களை இனப்பெருக்கம் செய்து, வளர்த்து, பராமரித்து, சந்தைப்படுத்தும் செயல்முறையாகும். இது உணவுக்காக மீன் உற்பத்தி செய்யும் உணவுமீன் வளர்ப்பிலிருந்து வேறுபடும்.</p><p><b>கற்றல் நோக்கம்:</b> அலங்கார மீன் வளர்ப்பையும் உணவுமீன் வளர்ப்பையும் வேறுபடுத்தி, அலங்கார மீன் பண்ணையின் முக்கிய செயல்பாடுகளை அடையாளம் காணுதல்.</p>",
    ),
    "ii1": (
        "<p><b>Egg-layers (oviparous fishes)</b> release eggs into the environment. Goldfish, koi, danios, barbs and angelfish are common ornamental examples.</p><p><b>Livebearers</b> such as guppy, molly, platy and swordtail retain developing embryos within the female and give birth to free-swimming young. In guppies and related poeciliids, embryo nutrition is mainly derived from yolk; this is described as lecithotrophic viviparity.</p><p>Breeding method, brood care and first feed must therefore be selected according to the reproductive type of the species.</p>",
        "<p><b>முட்டையிடும் மீன்கள்</b> முட்டைகளை வெளிப்புற சூழலில் இடுகின்றன. தங்கமீன், கோய், டேனியோ, பார்ப், ஏஞ்சல் மீன் ஆகியவை இதற்கான பொதுவான அலங்கார மீன் எடுத்துக்காட்டுகள்.</p><p><b>குஞ்சுகளை ஈனும் மீன்கள்</b> கருவை பெண் மீனின் உடலுக்குள் வளர்த்து, சுதந்திரமாக நீந்தும் குஞ்சுகளை ஈனுகின்றன. கப்பி, மொல்லி, பிளாட்டி, வாள்வால் மீன் ஆகியவை முக்கிய எடுத்துக்காட்டுகள். கப்பி போன்ற Poeciliidae குடும்ப மீன்களில் கரு முதன்மையாக முட்டை மஞ்சளிலிருந்து ஊட்டம் பெறுகிறது.</p><p>எனவே இனப்பெருக்க முறை, குஞ்சுப் பாதுகாப்பு, முதல் உணவு ஆகியவை இனத்தின் இனப்பெருக்க வகைக்கு ஏற்ப தேர்வு செய்யப்பட வேண்டும்.</p>",
    ),
    "ii2": (
        "<p>Goldfish require different temperature management for long-term maintenance and for breeding. Many fancy goldfish are maintained successfully at about 16–22 °C with good filtration and high dissolved oxygen. Prolonged exposure to 26–28 °C increases metabolic rate and oxygen demand.</p><p>Breeding is often encouraged by fresh water, suitable spawning material and a modest temperature rise within the breeding range, commonly around 20–24 °C. A practical breeding group may use one female with two males. Eggs are adhesive and the adults should be removed after spawning because they may eat the eggs.</p><p>Hatching time varies with temperature and is commonly about 2–4 days. Newly hatched fry require appropriately small first feeds.</p>",
        "<p>தங்கமீன்களின் நீண்டகால பராமரிப்பு வெப்பநிலையும் இனப்பெருக்க வெப்பநிலையும் ஒன்றல்ல. பல அலங்காரத் தங்கமீன்கள் சிறந்த வடிகட்டலும் போதிய கரைந்த ஆக்சிஜனும் உள்ள நிலையில் சுமார் 16–22°செ. வெப்பநிலையில் நன்கு வளர்கின்றன. 26–28°செ. போன்ற அதிக வெப்பநிலை நீண்டகாலம் நீடித்தால் வளர்சிதை மாற்றமும் ஆக்சிஜன் தேவையும் அதிகரிக்கும்.</p><p>புதுநீர் சேர்த்தல், ஏற்ற முட்டையிடும் தளம், இனப்பெருக்க வரம்பிற்குள் மிதமான வெப்பநிலை உயர்வு ஆகியவை முட்டையிடலைத் தூண்டலாம்; பொதுவாக 20–24°செ. ஏற்ற வரம்பாகும். நடைமுறையில் 1 பெண் : 2 ஆண் குழு பயன்படுத்தலாம். முட்டைகள் ஒட்டும் தன்மை உடையவை. முட்டையிட்ட பின் பெற்றோர் மீன்களை அகற்ற வேண்டும்.</p><p>வெப்பநிலையைப் பொறுத்து பொதுவாக 2–4 நாளில் முட்டைகள் பொரியும். புதிதாகப் பொரித்த குஞ்சுகளுக்கு மிகச் சிறிய அளவிலான முதல் உணவு வழங்க வேண்டும்.</p>",
    ),
    "iv3": (
        "<p>In a mature biological filter, toxic ammonia released from fish waste is oxidised first to nitrite and then to nitrate. Ammonia-oxidising archaea and bacteria, nitrite-oxidising bacteria such as <i>Nitrospira</i>, and complete ammonia-oxidising (comammox) <i>Nitrospira</i> may contribute to this process.</p><p>A new aquarium must be cycled before heavy stocking. During cycling, ammonia and nitrite are monitored until the biological filter can process the expected waste load. Nitrate is controlled mainly by water changes, plant uptake and sensible stocking.</p>",
        "<p>மீன்களின் கழிவிலிருந்து உருவாகும் நச்சுத்தன்மை வாய்ந்த அமோனியா, முதிர்ந்த உயிரியல் வடிகட்டியில் முதலில் நைட்ரைட்டாகவும் பின்னர் நைட்ரேட்டாகவும் ஆக்சிகரிக்கப்படுகிறது. அமோனியாவை ஆக்சிகரிக்கும் ஆர்க்கியா மற்றும் பாக்டீரியா, <i>Nitrospira</i> போன்ற நைட்ரைட்டை ஆக்சிகரிக்கும் பாக்டீரியா, முழுமையான அமோனியா ஆக்சிகரிப்பைச் செய்யும் comammox <i>Nitrospira</i> ஆகியவை இச்செயலில் பங்கெடுக்கலாம்.</p><p>புதிய மீன் தொட்டியில் அதிக மீன்களை விடுவதற்கு முன் உயிரியல் வடிகட்டி முதிர்ச்சியடையும் வரை சுழற்சி ஏற்படுத்த வேண்டும். அமோனியா மற்றும் நைட்ரைட் அளவுகள் பாதுகாப்பான நிலைக்கு வரும்வரை கண்காணிக்க வேண்டும். நைட்ரேட்டை நீர் மாற்றம், தாவரங்களின் ஊட்டச்சத்து உறிஞ்சல், ஏற்ற மீன் அடர்த்தி ஆகியவற்றால் கட்டுப்படுத்தலாம்.</p>",
    ),
    "iv4": (
        "<p>Source water must be tested before it is used in an aquarium or breeding unit. Bore-well water may be suitable, but it can also contain high hardness, iron, low dissolved oxygen or other dissolved substances. Municipal water may contain chlorine or chloramine.</p><p>Check the relevant parameters, condition the water when necessary, and match the water chemistry to the species being kept. No source should be considered suitable merely because it is clear or comes from a particular type of well.</p>",
        "<p>மீன் தொட்டி அல்லது இனப்பெருக்க அலகில் பயன்படுத்துவதற்கு முன் நீராதாரத்தைப் பரிசோதிக்க வேண்டும். ஆழ்குழாய் கிணற்று நீர் சில இடங்களில் ஏற்றதாக இருக்கலாம்; ஆனால் அதிக கடினத்தன்மை, இரும்பு, குறைந்த கரைந்த ஆக்சிஜன் அல்லது பிற கரைந்த பொருட்கள் இருக்கலாம். நகராட்சி நீரில் குளோரின் அல்லது குளோரமின் இருக்கலாம்.</p><p>தேவையான நீர்தர அளவுகளைப் பரிசோதித்து, தேவையெனில் நீரைச் சீரமைத்து, வளர்க்கப்படும் இனத்திற்கேற்ற நீர் வேதியியலைப் பராமரிக்க வேண்டும். நீர் தெளிவாக இருப்பதையோ அதன் மூலத்தையோ மட்டும் வைத்து ஏற்றது என்று முடிவு செய்யக்கூடாது.</p>",
    ),
    "iv5": (
        "<p>Mollies do not require routine addition of sodium chloride in every aquarium. Many commonly cultured mollies thrive in hard, alkaline freshwater when water quality is stable.</p><p>Some sailfin and related populations originate from brackish habitats and can tolerate moderate salinity. Salt use, if required for a defined husbandry or clinical purpose, must therefore be based on the species, strain and water conditions rather than used as a universal rule.</p>",
        "<p>எல்லா மொல்லி மீன் தொட்டிகளிலும் வழக்கமாக உப்பு சேர்க்க வேண்டிய அவசியமில்லை. பல வளர்ப்பு மொல்லிகள் நீர்தரம் நிலையாக உள்ள கடினத்தன்மையும் காரத்தன்மையும் கொண்ட நன்னீரில் நன்கு வாழ்கின்றன.</p><p>செயில்ஃபின் மொல்லி மற்றும் தொடர்புடைய சில இயற்கைக் கூட்டங்கள் உவர்நீர் வாழிடங்களில் தோன்றியதால் மிதமான உவர்த்தன்மையைத் தாங்கக்கூடும். எனவே உப்பு பயன்படுத்த வேண்டுமெனில், அது இனத்தின் தேவைகள், வளர்ப்பு இரகம் மற்றும் நீர்தர நிலை ஆகியவற்றின் அடிப்படையில் மட்டுமே தீர்மானிக்கப்பட வேண்டும்.</p>",
    ),
    "iv7": (
        "<p>New fish should be quarantined in a separate tank with separate nets and equipment before they are introduced to an established collection. A practical minimum is about 30 days, and a longer period may be required when disease signs, treatment or cool-water pathogens are involved.</p><p>Observe appetite, respiration, swimming, skin and fins; test water quality; and avoid sharing water or equipment between quarantine and display tanks. Quarantine reduces, but does not eliminate, the risk of introducing pathogens.</p>",
        "<p>புதிய மீன்களை ஏற்கனவே உள்ள மீன் கூட்டத்துடன் சேர்ப்பதற்கு முன் தனித் தொட்டியில் தனி வலை மற்றும் கருவிகளுடன் தனிமைப்படுத்த வேண்டும். நடைமுறையில் சுமார் 30 நாட்கள் குறைந்தபட்சமாகப் பயன்படுத்தலாம்; நோய் அறிகுறிகள், சிகிச்சை அல்லது குளிர்நீரில் நீளும் நோய்க்கிருமி வாழ்க்கைச் சுழற்சி இருந்தால் அதிக காலம் தேவைப்படலாம்.</p><p>உணவு உட்கொள்ளல், சுவாசம், நீந்தும் முறை, தோல், துடுப்புகள் ஆகியவற்றைக் கவனித்து நீர்தரத்தைப் பரிசோதிக்க வேண்டும். தனிமைத் தொட்டியின் நீர் அல்லது கருவிகளை காட்சித் தொட்டியுடன் பகிரக்கூடாது. தனிமைப்படுத்தல் நோய்க்கிருமி நுழைவு அபாயத்தை குறைக்கிறது; முழுமையாக நீக்காது.</p>",
    ),
    "v1": (
        "<p>Before transport, feed is usually withheld for 24–48 hours, depending on species and condition, to reduce faecal waste and ammonia accumulation in the bag.</p><p>Fish are packed in clean water with an adequate oxygen-filled gas space, commonly in strong double polythene bags placed inside insulated boxes. The safe number of fish per bag depends on body mass, species, temperature and journey duration; a fixed water-to-oxygen ratio must not be treated as universal.</p><p>Keep packages shaded and stable. Avoid excessive heat and sudden temperature change at arrival. Incompatible species should not be mixed in the same transport bag.</p>",
        "<p>போக்குவரத்துக்கு முன் இனமும் உடல்நிலையும் பொருத்து பொதுவாக 24–48 மணி நேரம் உணவு நிறுத்தப்படுகிறது. இதனால் பையில் மலக்கழிவும் அமோனியா சேர்க்கையும் குறைகிறது.</p><p>சுத்தமான நீருடன் போதிய ஆக்சிஜன் நிரப்பிய வாயு இடம் உள்ள வலுவான இரட்டைப் பாலிதீன் பைகளில் மீன்களைப் பொதியலாம்; பைகள் வெப்பத்தைத் தடுக்கக் கூடிய பெட்டிகளில் வைக்கப்பட வேண்டும். ஒரு பையில் பாதுகாப்பாக வைக்கக்கூடிய மீன்களின் எண்ணிக்கை மீன் உடல் நிறை, இனம், வெப்பநிலை, பயண நேரம் ஆகியவற்றைப் பொறுத்தது. ஒரே நீர்–ஆக்சிஜன் விகிதத்தை எல்லா சூழலுக்கும் பொருந்தும் விதியாகக் கருதக்கூடாது.</p><p>பொதிகளை நிழலிலும் நிலையான நிலையில் வைத்திருக்க வேண்டும். அதிக வெப்பத்தையும், வந்தவுடன் ஏற்படும் திடீர் வெப்பநிலை மாற்றத்தையும் தவிர்க்க வேண்டும். இணக்கமற்ற இனங்களை ஒரே போக்குவரத்துப் பையில் சேர்க்கக்கூடாது.</p>",
    ),
    "rev": (
        "<ol><li>Zebrafish (<i>Danio rerio</i>) is indigenous to the Indian subcontinent.</li><li>Koi is an ornamental form of <i>Cyprinus carpio</i>; Oranda is a variety of <i>Carassius auratus</i>.</li><li>Maintenance and breeding temperatures must be considered separately for goldfish.</li><li>Guppy, molly, platy and swordtail are livebearers; guppy development is lecithotrophic viviparity.</li><li>Quarantine new stock for about 30 days with separate equipment.</li><li>Biological filtration involves diverse ammonia- and nitrite-oxidising microorganisms.</li><li>Water requirements are species-specific; routine salt addition for every molly is not necessary.</li><li>Stocking density depends on filtration, oxygen demand, behaviour, body mass and waste production.</li></ol>",
        "<ol><li>ஜீப்ரா டேனியோ (<i>Danio rerio</i>) இந்தியத் துணைக்கண்டத் தாயக இனம்.</li><li>கோய் என்பது <i>Cyprinus carpio</i> இனத்தின் அலங்கார வடிவம்; ஒரண்டா என்பது <i>Carassius auratus</i> இனத்தின் அலங்கார இரகம்.</li><li>தங்கமீன்களின் பராமரிப்பு வெப்பநிலையும் இனப்பெருக்க வெப்பநிலையும் தனித்தனியாகக் கருதப்பட வேண்டும்.</li><li>கப்பி, மொல்லி, பிளாட்டி, வாள்வால் மீன் ஆகியவை குஞ்சுகளை ஈனும் மீன்கள்.</li><li>புதிய மீன்களை தனி கருவிகளுடன் சுமார் 30 நாட்கள் தனிமைப்படுத்துவது பாதுகாப்பான நடைமுறை.</li><li>உயிரியல் வடிகட்டலில் அமோனியா மற்றும் நைட்ரைட்டை ஆக்சிகரிக்கும் பலவகை நுண்ணுயிரிகள் பங்கெடுக்கின்றன.</li><li>நீர்தரத் தேவைகள் இனத்திற்கேற்ப மாறும்; எல்லா மொல்லிகளுக்கும் வழக்கமாக உப்பு சேர்க்கத் தேவையில்லை.</li><li>மீன் அடர்த்தி வடிகட்டல் திறன், ஆக்சிஜன் தேவை, நடத்தை, உடல் நிறை, கழிவு உற்பத்தி ஆகியவற்றைப் பொறுத்தது.</li></ol>",
    ),
    "reg1": (
        "<p>Important Indian organisations relevant to ornamental fisheries include ICAR-NBFGR for fish genetic resources, ICAR-CIFA for freshwater aquaculture, ICAR-CIBA for brackish-water aquaculture, NFDB and the Department of Fisheries for development programmes, and MPEDA/EIC for export and health-certification requirements.</p><p>For conservation and trade, consult the current IUCN Red List, CITES appendices, the Wildlife (Protection) Act and applicable state fisheries rules. Requirements can change; use the current official circular or database when making regulatory decisions.</p>",
        "<p>அலங்கார மீன்வளத்துடன் தொடர்புடைய முக்கிய இந்திய நிறுவனங்களில் மீன் மரபியல் வளங்களுக்கு ICAR-NBFGR, நன்னீர் நீருயிரி வளர்ப்புக்கு ICAR-CIFA, உவர்நீர் நீருயிரி வளர்ப்புக்கு ICAR-CIBA, வளர்ச்சித் திட்டங்களுக்கு NFDB மற்றும் மீன்வளத் துறை, ஏற்றுமதி மற்றும் சுகாதாரச் சான்றுகளுக்கு MPEDA/EIC ஆகியவை அடங்கும்.</p><p>பாதுகாப்பு மற்றும் வணிகத் தேவைகளுக்கு நடப்பு IUCN சிவப்புப் பட்டியல், CITES இணைப்புகள், வனவிலங்கு (பாதுகாப்பு) சட்டம் மற்றும் பொருந்தும் மாநில மீன்வள விதிகளைப் பார்க்க வேண்டும். விதிகள் மாறக்கூடியவை; ஒழுங்குமுறை முடிவுகளுக்கு நடப்பு அதிகாரப்பூர்வ சுற்றறிக்கை அல்லது தரவுத்தளத்தைப் பயன்படுத்த வேண்டும்.</p>",
    ),
}
for bid, (en_html, ta_html) in rewrites.items():
    book_by_id[bid]["en"] = en_html
    book_by_id[bid]["ta"] = ta_html

neutral_pairs = [
    ("<b>Curriculum:</b> ", ""),
    ("<b>Current Science / welfare practice:</b> ", ""),
    ("<b>Current Science:</b> ", ""),
    ("<b>பாடத்திட்டம்:</b> ", ""),
    ("<b>நவீன நலப் பயிற்சி:</b> ", ""),
    ("<b>நவீன அறிவியல்:</b> ", ""),
    ("Curriculum: ", ""),
    ("Current Science: ", ""),
    ("Current Science / welfare practice: ", ""),
    ("பாடத்திட்டம்: ", ""),
    ("நவீன அறிவியல்: ", ""),
    ("TNAU-style notes often ", ""),
    ("TNAU Agritech", ""),
    ("TNAU", ""),
]
book = walk_replace(book, neutral_pairs)
book_by_id = {item["id"]: item for item in book}

# Remove citation-note prose from learner-facing lessons; provenance remains in release evidence.
for bid in ("i2", "v3", "in2"):
    item = book_by_id[bid]
    item["en"] = re.sub(r"<p>Refs?:.*?</p>", "", item["en"], flags=re.I | re.S)
    item["ta"] = re.sub(r"<p>ஆதாரம்:.*?</p>", "", item["ta"], flags=re.S)

book_by_id["iv1"]["en"] = re.sub(r"<p>Ref:.*?</p>", "", book_by_id["iv1"]["en"], flags=re.I | re.S)
book_by_id["iv1"]["ta"] = re.sub(r"<p>ஆதாரம்:.*?</p>", "", book_by_id["iv1"]["ta"], flags=re.S)

book_by_id["t1"]["en"] = book_by_id["t1"]["en"].replace(
    "Drug names and doses are not printed as recipes.",
    "Drug treatment and dosage should follow qualified aquatic-veterinary or fisheries advice.",
)
book_by_id["t1"]["ta"] = book_by_id["t1"]["ta"].replace(
    "மருந்துப் பெயரும் அளவும் பொதுவான மருந்தளவு வழிமுறையாக அச்சிடப்படவில்லை.",
    "மருந்துச் சிகிச்சையும் அளவும் தகுதிவாய்ந்த நீரியல் கால்நடை மருத்துவர் அல்லது மீன்வள நிபுணர் ஆலோசனையைப் பின்பற்ற வேண்டும்.",
)
book_by_id["ii3"]["en"] = book_by_id["ii3"]["en"].replace(
    "Curriculum first feeds: infusoria then larger live food.",
    "Suitable first feeds include infusoria followed by larger live foods as the fry grow.",
)
book_by_id["ii3"]["ta"] = book_by_id["ii3"]["ta"].replace(
    "பாடத்திட்ட முதல் உணவு: இன்ஃபியூசோரியா பின்னர் பெரிய உயிருணவு.",
    "முதல் உணவாக இன்ஃபியூசோரியா வழங்கி, குஞ்சுகள் வளரும்போது பெரிய உயிருணவுகளுக்கு மாற்றலாம்.",
)

book_by_id["t0"]["ta"] = book_by_id["t0"]["ta"].replace("குஞ்சுப் பண்ணை", "குஞ்சு பொரிப்பகம்")
book_by_id["t1"]["ta"] = book_by_id["t1"]["ta"].replace("நீர் எண்கள்", "நீர்தர அளவுகள்").replace("வளர்ப்பு வீச்சு", "பராமரிப்பு வரம்பு")
book_by_id["i4"]["ta"] = book_by_id["i4"]["ta"].replace("இணை வாழ்தல்", "இணக்கத்தன்மை")
book_by_id["an1"]["ta"] = book_by_id["an1"]["ta"].replace("இரு சோடி மீசை", "இரண்டு ஜோடி மீசை")
book_by_id["fil1"]["ta"] = book_by_id["fil1"]["ta"].replace("நார்ப்பஞ்சு", "வடிகட்டி நார்").replace("ஹெட்டரோட்ரோப்களுக்கு", "பிற கரிம ஊட்ட நுண்ணுயிர்களுக்கு")
book_by_id["wel1"]["ta"] = book_by_id["wel1"]["ta"].replace("உயிரியையோ", "மீன்களையோ").replace("இருப்பு என்பது", "மீன் அடர்த்தி")
book_by_id["iii4"]["ta"] = book_by_id["iii4"]["ta"].replace("நுண்ணுயிர் கலவை", "இன்ஃபியூசோரியா நுண்ணுயிர் கலவை").replace("நீர்க்கட்டிகள்", "முட்டை உறைகள்")
book_by_id["iv8"]["ta"] = book_by_id["iv8"]["ta"].replace("இடைமூடி", "மேல் மூடி")

# Quiz: remove source-attribution framing and use direct scientific questions.
quiz[0].update(
    q="Which statement correctly distinguishes koi and Oranda?",
    qt="கோய் மற்றும் ஒரண்டாவைச் சரியாக வேறுபடுத்துவது எது?",
)
quiz[1].update(
    q="Danio rerio is",
    qt="Danio rerio என்பது",
    o=["An exotic livebearer", "An indigenous egg-layer of the Indian subcontinent", "A marine fish listed in CITES Appendix I", "A brackish-water puffer"],
    ot=["அந்நிய குஞ்சுகளை ஈனும் மீன்", "இந்தியத் துணைக்கண்டத் தாயக முட்டையிடும் மீன்", "CITES இணைப்பு I-ல் உள்ள கடல் மீன்", "உவர்நீர் பஃபர் மீன்"],
    et="ஜீப்ரா டேனியோ இந்தியத் துணைக்கண்டத் தாயக மீன்.",
)
quiz[2].update(
    q="Which description best fits guppy reproduction?",
    qt="கப்பி இனப்பெருக்கத்தைச் சரியாக விளக்கும் கூற்று எது?",
    o=["Placental reproduction like mammals", "Lecithotrophic viviparity; free-swimming young are born", "Mouth brooding only", "Marine nest spawning"],
    ot=["பாலூட்டிகளைப் போன்ற நஞ்சுக்கொடி இனப்பெருக்கம்", "முட்டை மஞ்சள் ஊட்டத்துடன் கரு வளர்ந்து, நீந்தும் குஞ்சுகள் ஈனப்படுகின்றன", "வாயில் முட்டை அடைகாப்பு மட்டும்", "கடல் நீரில் கூடுகட்டி முட்டையிடுதல்"],
    e="Guppy embryos are nourished mainly by yolk and the female gives birth to free-swimming young.",
    et="கப்பியில் கரு முதன்மையாக முட்டை மஞ்சளிலிருந்து ஊட்டம் பெற்று, சுதந்திரமாக நீந்தும் குஞ்சுகள் ஈனப்படுகின்றன.",
)
quiz[3].update(
    q="A practical goldfish breeding group may use",
    qt="தங்கமீன் இனப்பெருக்கத்திற்கான நடைமுறை குழு விகிதம் எது?",
    e="One female with two males is a commonly used practical breeding group; it is not a fixed biological rule.",
    et="1 பெண் : 2 ஆண் என்பது நடைமுறையில் பயன்படுத்தப்படும் இனப்பெருக்கக் குழு விகிதம்; இது கட்டாய உயிரியல் விதி அல்ல.",
)
quiz[4].update(
    q="A practical quarantine period for new ornamental fish is",
    qt="புதிய அலங்கார மீன்களுக்கு ஏற்ற நடைமுறை தனிமைப்படுத்தல் காலம் எது?",
    e="About 30 days with separate equipment is a practical minimum; a longer period may be required when disease is suspected.",
    et="தனி கருவிகளுடன் சுமார் 30 நாட்கள் தனிமைப்படுத்துவது நடைமுறை குறைந்தபட்சம்; நோய் சந்தேகம் இருந்தால் அதிக காலம் தேவைப்படலாம்.",
)
quiz[5].update(
    q="A mature biological filter is best described as",
    qt="முதிர்ந்த உயிரியல் வடிகட்டியைச் சரியாக விளக்கும் கூற்று எது?",
    e="Biological filters contain diverse ammonia- and nitrite-oxidising microorganisms, including Nitrospira and comammox organisms.",
    et="உயிரியல் வடிகட்டியில் அமோனியா மற்றும் நைட்ரைட்டை ஆக்சிகரிக்கும் பலவகை நுண்ணுயிரிகள், Nitrospira மற்றும் comammox உயிரிகள் உட்பட, காணப்படுகின்றன.",
)
quiz[7].update(
    q="Routine sodium chloride addition for every molly aquarium is",
    qt="எல்லா மொல்லி மீன் தொட்டிகளிலும் வழக்கமாக உப்பு சேர்ப்பது",
    e="It is not mandatory. Stable water quality, appropriate hardness and alkalinity are more important; salinity needs depend on the strain and husbandry purpose.",
    et="இது கட்டாயமல்ல. நிலையான நீர்தரம், ஏற்ற கடினத்தன்மை மற்றும் காரத்தன்மை முக்கியமானவை; உவர்த்தன்மைத் தேவை வளர்ப்பு இரகத்தையும் நோக்கத்தையும் பொறுத்தது.",
)
quiz[8]["ot"][1] = "பண்ணையில் இனப்பெருக்கம் செய்யப்பட்ட மீன்களை மட்டும் பயன்படுத்துதல்"
quiz[8]["e"] = "Use documented captive-bred stock and avoid collection of threatened endemic populations from the wild."
quiz[8]["et"] = "ஆவணப்படுத்தப்பட்ட பண்ணை இனப்பெருக்க மீன்களைப் பயன்படுத்தி, அழியும் அபாயமுள்ள தனித்தாயக மீன்களை காட்டிலிருந்து சேகரிக்க வேண்டாம்."
quiz[14]["e"] = "About 30 days or more with separate equipment is a safer quarantine plan."
quiz[14]["et"] = "தனி கருவிகளுடன் சுமார் 30 நாட்கள் அல்லது அதற்கு மேல் தனிமைப்படுத்துவது பாதுகாப்பான நடைமுறை."
quiz[15]["e"] = "Use documented captive-bred stock; avoid wild collection of threatened endemic populations."
quiz[15]["et"] = "ஆவணப்படுத்தப்பட்ட பண்ணை இனப்பெருக்க மீன்களைப் பயன்படுத்த வேண்டும்; அழியும் அபாயமுள்ள தனித்தாயக இனங்களை காட்டிலிருந்து சேகரிக்க வேண்டாம்."

quiz = walk_replace(
    quiz,
    [
        ("Curriculum: ", ""),
        ("Curriculum ", ""),
        ("பாடநூல் சில இடங்களில் 2–3 வாரத்தை அச்சிடலாம். ", ""),
        ("TNAU ", ""),
    ],
)

labs = walk_replace(
    labs,
    [
        ("இன்ச்–கேலன்", "இன்ச்–கேலன் அடிப்படையிலான"),
        ("உள்பொரி", "குஞ்சுகளை ஈனும் மீன்"),
        ("அகவாரி", "மீன் தொட்டி"),
    ],
)

for dataset in (species, book, quiz, labs):
    for item in dataset:
        for key, value in list(item.items()):
            if isinstance(value, str):
                item[key] = value.replace("TNAU Agritech; ", "").replace("TNAU Agritech", "").replace("TNAU", "")

save("species.json", species)
save("book.json", book)
save("quiz.json", quiz)
save("labs.json", labs)

print("Phase-2 editorial overlay applied: Indian English + Tamil Nadu textbook-style Tamil.")
