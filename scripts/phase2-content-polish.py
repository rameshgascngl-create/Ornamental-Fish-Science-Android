#!/usr/bin/env python3
from pathlib import Path
import json, re, sys

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "app/src/main/assets/data"

BOOK_TITLES = {
 "t0":("Unit I — Ornamental Fish Culture: Meaning and Scope","அலகு I — அலங்கார மீன் வளர்ப்பு: பொருளும் பரப்பும்"),
 "t1":("How to Read a Species Account","இன விவரணத்தைப் படிக்கும் முறை"),
 "i1":("1.1 Introduction to Ornamental Fish Culture","1.1 அலங்கார மீன் வளர்ப்பு — அறிமுகம்"),
 "i2":("1.2 Ornamental Fish Culture in India and Tamil Nadu","1.2 இந்தியாவிலும் தமிழ்நாட்டிலும் அலங்கார மீன் வளர்ப்பு"),
 "i3":("1.3 Indigenous and Exotic Species","1.3 தாயக மற்றும் அயல்நாட்டு இனங்கள்"),
 "i4":("1.4 Species-specific Compatibility","1.4 இனத்திற்கேற்ற இணைவாழ்வு"),
 "ii1":("2.1 Egg-layers and Livebearers","2.1 முட்டையிடும் மீன்களும் குஞ்சுகளை ஈனும் மீன்களும்"),
 "ii2":("2.2 Goldfish: Maintenance and Breeding Temperatures","2.2 தங்கமீன்: பராமரிப்பு மற்றும் இனப்பெருக்க வெப்பநிலை"),
 "ii3":("2.3 Guppy Breeding and Nursery Management","2.3 கப்பி இனப்பெருக்கமும் குஞ்சுப் பராமரிப்பும்"),
 "ii4":("2.4 Distinguishing Koi from Oranda","2.4 கோய் மற்றும் ஓராண்டாவை வேறுபடுத்துதல்"),
 "iii1":("3.1 Aquarium Plants and Feeding Principles","3.1 மீன் காட்சித் தொட்டித் தாவரங்களும் உணவளிப்பு முறைகளும்"),
 "iii2":("3.2 Live-food Culture — An Outline","3.2 உயிருணவு வளர்ப்பு — சுருக்கம்"),
 "iv1":("4.1 Water-chemistry Terms","4.1 நீர் வேதியியல் — முக்கிய சொற்கள்"),
 "iv2":("4.2 Water Requirements Are Species-specific","4.2 நீர்த் தேவைகள் இனத்திற்கேற்ப மாறுபடும்"),
 "iv3":("4.3 Nitrogen Cycle in Aquaria","4.3 மீன் காட்சித் தொட்டியில் நைட்ரஜன் சுழற்சி"),
 "iv4":("4.4 Choosing and Conditioning Source Water","4.4 நீர்மூலத் தேர்வும் நீர் தயாரிப்பும்"),
 "iv5":("4.5 Mollies: Hardness and Salinity","4.5 மொல்லி மீன்கள்: நீர்க்கடினத்தன்மையும் உவர்ப்புத்தன்மையும்"),
 "iv7":("4.7 Quarantine and Biosecurity","4.7 தனிமைப்படுத்தலும் உயிரியல் பாதுகாப்பும்"),
 "v1":("5.1 Packing Ornamental Fish for Transport","5.1 அலங்கார மீன்களைப் போக்குவரத்திற்குப் பொதியிடுதல்"),
 "in1":("Ornamental Fishes of India","இந்தியாவின் அலங்கார மீன்கள்"),
 "in2":("Threatened Endemic and Invasive Fishes","அச்சுறுத்தலுக்குள்ளான தாயகச் சிறப்பினங்களும் ஆக்கிரமிப்பு இனங்களும்"),
 "rev":("Revision — Key Concepts","மீள்பார்வை — முக்கிய கருத்துகள்"),
 "an1":("External Morphology for Identification","இனங்காண உதவும் வெளிப்புற உடலமைப்பு"),
 "reg1":("Indian Institutions and Regulatory Agencies","இந்திய நிறுவனங்களும் ஒழுங்குமுறை அமைப்புகளும்"),
 "i5":("1.5 Commercially Important Exotic Freshwater Fishes","1.5 வணிக முக்கியத்துவம் வாய்ந்த அயல்நாட்டு நன்னீர் அலங்கார மீன்கள்"),
 "i6":("1.6 Indigenous Ornamental Fishes of India","1.6 இந்தியத் தாயக அலங்கார மீன்கள்"),
 "ii6":("2.6 Livebearer Nursery Management","2.6 குஞ்சுகளை ஈனும் மீன்களின் குஞ்சுப் பராமரிப்பு"),
 "iii3":("3.3 Aquarium Plants Used in Practical Work","3.3 செய்முறைப் பயிற்சியில் பயன்படும் மீன் காட்சித் தொட்டித் தாவரங்கள்"),
 "iv8":("4.8 Aquarium Design Principles","4.8 மீன் காட்சித் தொட்டி வடிவமைப்பின் அடிப்படைகள்")
}

BOOK_BODIES = {
 "i1":(
  "<p>Ornamental fish culture is the breeding, rearing and management of fish valued for colour, body form and behaviour for aquaria, display and trade. It differs from food-fish culture because the principal value of the stock is ornamental rather than as food.</p><p><b>Learning objective:</b> Distinguish ornamental fish culture from food-fish aquaculture and identify its major activities.</p>",
  "<p>அலங்கார மீன் வளர்ப்பு என்பது நிறம், உடலமைப்பு, நடத்தை போன்ற அழகியல் பண்புகளுக்காக மீன்களை இனப்பெருக்கம் செய்து, வளர்த்து, பராமரித்து, காட்சிக்கும் வணிகத்திற்கும் பயன்படுத்தும் முறையாகும். இதன் முதன்மை நோக்கம் உணவுத் தயாரிப்பு அல்ல; அழகியல் மற்றும் காட்சிப் பயன்பாடாகும்.</p><p><b>கற்றல் நோக்கம்:</b> அலங்கார மீன் வளர்ப்பையும் உணவுமீன் வளர்ப்பையும் வேறுபடுத்தி, அதன் முக்கிய செயல்பாடுகளை விளக்குதல்.</p>"
 ),
 "ii1":(
  "<p><b>Egg-layers (oviparous fishes)</b> release eggs that develop outside the female. Many ornamental cyprinids, characins and cichlids belong to this group.</p><p><b>Livebearers</b> such as guppies, mollies, platies and swordtails give birth to free-swimming young. In poeciliids, embryos develop within the female and depend mainly on yolk reserves; this is described as lecithotrophic viviparity.</p><p><b>Key distinction:</b> do not describe guppy reproduction as mammalian-type viviparity.</p>",
  "<p><b>முட்டையிடும் மீன்கள்</b> பெண் மீனின் உடலுக்கு வெளியே வளரக்கூடிய முட்டைகளை இடுகின்றன. பல சைப்ரினிட், கராசின், சிக்லிட் அலங்கார மீன்கள் இவ்வகையைச் சேர்ந்தவை.</p><p><b>குஞ்சுகளை ஈனும் மீன்கள்</b> — கப்பி, மொல்லி, பிளாட்டி, வாள்வால் போன்றவை — நீந்தக்கூடிய குஞ்சுகளை நேரடியாக ஈனுகின்றன. பீசிலிட் மீன்களில் கருக்கள் பெண் மீனின் உடலுக்குள் வளர்ந்து, பெரும்பாலும் முட்டை மஞ்சளிலிருந்து ஊட்டம் பெறுகின்றன; இதனை <i>lecithotrophic viviparity</i> எனக் குறிப்பிடுகின்றனர்.</p><p><b>முக்கிய வேறுபாடு:</b> கப்பியின் இனப்பெருக்கத்தை பாலூட்டிகளில் காணப்படும் உயிர்க்குட்டிப் பேற்றுடன் ஒப்பிடக் கூடாது.</p>"
 ),
 "ii2":(
  "<p>Goldfish require different temperature ranges for routine maintenance and for breeding. Many fancy varieties are maintained successfully at about 16–22 °C when dissolved oxygen and filtration are adequate. Prolonged exposure to 26–28 °C increases metabolic rate and oxygen demand.</p><p>For breeding, mature fish are conditioned well and spawning is commonly stimulated by a gradual rise in temperature. A practical range around 20–24 °C is widely used. A breeding group may use one female with two males; adhesive eggs usually hatch within about 2–4 days depending on temperature.</p><p><b>Nursery:</b> newly hatched fry require very fine food such as infusoria before being shifted gradually to larger live or formulated feeds.</p>",
  "<p>தங்கமீனின் வழக்கமான பராமரிப்பிற்கும் இனப்பெருக்கத்திற்கும் தேவையான வெப்பநிலை ஒன்றாக இருக்காது. பல அலங்காரத் தங்கமீன் இரகங்கள் போதிய கரைந்த ஆக்சிஜனும் சிறந்த வடிகட்டலும் உள்ளபோது சுமார் 16–22°செ. வெப்பநிலையில் நன்கு பராமரிக்கப்படுகின்றன. 26–28°செ. போன்ற அதிக வெப்பத்தில் நீண்டகாலம் வைப்பது வளர்சிதை மாற்றத்தையும் ஆக்சிஜன் தேவையையும் அதிகரிக்கலாம்.</p><p>இனப்பெருக்கத்திற்கு முதிர்ந்த மீன்களுக்கு சத்தான உணவு வழங்கி நன்கு தயார்படுத்த வேண்டும். வெப்பநிலையை மெதுவாக உயர்த்துவது முட்டையிடலைத் தூண்ட உதவும். சுமார் 20–24°செ. நடைமுறையில் பொதுவாகப் பயன்படுத்தப்படும் வீச்சாகும். ஒரு பெண் மீனுடன் இரண்டு ஆண் மீன்களை இணைக்கலாம்; ஒட்டும் தன்மையுள்ள முட்டைகள் வெப்பநிலையைப் பொறுத்து சுமார் 2–4 நாள்களில் பொரியும்.</p><p><b>குஞ்சுப் பராமரிப்பு:</b> புதிதாகப் பொரிந்த குஞ்சுகளுக்கு முதலில் இன்ஃபியூசோரியா போன்ற மிக நுண்ணிய உணவு அளித்து, பின்னர் படிப்படியாகப் பெரிய உயிருணவு அல்லது தயாரிப்பு உணவிற்கு மாற்ற வேண்டும்.</p>"
 ),
 "ii4":(
  "<p><b>Koi</b> are ornamental forms of <i>Cyprinus carpio</i>. They possess barbels around the mouth and are primarily suited to ponds or very large systems.</p><p><b>Oranda</b> is a fancy variety of <i>Carassius auratus</i>. It has a characteristic fleshy head growth called a wen and lacks the barbels typical of koi.</p><p>Therefore koi and oranda must be treated as different ornamental forms with different identification characters and husbandry requirements.</p>",
  "<p><b>கோய்</b> என்பது <i>Cyprinus carpio</i> இனத்தின் அலங்கார வடிவமாகும். வாயைச் சுற்றி மீசை போன்ற பார்பல்கள் காணப்படும்; பெரும்பாலும் குளம் அல்லது மிகப் பெரிய வளர்ப்பு அமைப்புகளுக்கு ஏற்றது.</p><p><b>ஓராண்டா</b> என்பது <i>Carassius auratus</i> இனத்தின் அலங்காரத் தங்கமீன் இரகமாகும். தலையில் “வென்” எனப்படும் மாமிச வளர்ச்சி காணப்படும்; கோயில் காணப்படும் பார்பல்கள் இதில் இல்லை.</p><p>எனவே கோய் மற்றும் ஓராண்டா ஆகியவற்றை தனித்தனி அடையாளப் பண்புகளும் பராமரிப்புத் தேவைகளும் கொண்ட அலங்கார வடிவங்களாகக் கருத வேண்டும்.</p>"
 ),
 "iv2":(
  "<p>There is no single temperature or pH range that is ideal for every ornamental fish. Goldfish, mollies, tetras and hill-stream barbs differ substantially in their thermal and water-chemistry requirements.</p><p>Use the species account to select an appropriate maintenance range. When several species are housed together, consider the overlapping safe range rather than applying one general value to all fish.</p>",
  "<p>அனைத்து அலங்கார மீன்களுக்கும் பொருந்தக்கூடிய ஒரே வெப்பநிலை அல்லது pH வீச்சு இல்லை. தங்கமீன், மொல்லி, டெட்ரா, மலைநீரோடை பார்ப் போன்ற மீன்களின் வெப்பநிலை மற்றும் நீர் வேதியியல் தேவைகள் குறிப்பிடத்தக்க அளவில் மாறுபடும்.</p><p>ஒவ்வொரு இனத்திற்கும் அதன் இன விவரணத்தில் கொடுக்கப்பட்ட பராமரிப்பு வீச்சைப் பயன்படுத்த வேண்டும். பல இனங்களை ஒரே தொட்டியில் வைத்தால், எல்லா இனங்களுக்கும் பொருந்தக்கூடிய பாதுகாப்பான பொதுவீச்சைத் தேர்ந்தெடுக்க வேண்டும்.</p>"
 ),
 "iv3":(
  "<p>In a mature biological filter, ammonia produced from fish waste is oxidised first to nitrite and then to nitrate by communities of nitrifying microorganisms. The process is commonly introduced through the genera <i>Nitrosomonas</i> and <i>Nitrobacter</i>, but working biofilters contain a broader microbial community.</p><p>A new aquarium should be biologically matured before heavy stocking. Ammonia and nitrite must be monitored during establishment because both can become toxic.</p>",
  "<p>முதிர்ந்த உயிரியல் வடிகட்டியில் மீன் கழிவுகளிலிருந்து உருவாகும் அமோனியா முதலில் நைட்ரைட்டாகவும் பின்னர் நைட்ரேட்டாகவும் ஆக்சிகரிக்கப்படுகிறது. இந்தச் செயல்முறையை விளக்குவதற்கு <i>Nitrosomonas</i> மற்றும் <i>Nitrobacter</i> போன்ற பேரினங்கள் பொதுவாக எடுத்துக்காட்டப்படுகின்றன; நடைமுறை உயிரியல் வடிகட்டிகளில் பலவகை நுண்ணுயிர்கள் இணைந்து செயல்படுகின்றன.</p><p>புதிய மீன் காட்சித் தொட்டியில் அதிக மீன்களை விடுவதற்கு முன் உயிரியல் வடிகட்டலை நிலைநிறுத்த வேண்டும். தொடக்ககாலத்தில் அமோனியா மற்றும் நைட்ரைட் அளவுகளை கண்காணிக்க வேண்டும்; இரண்டும் அதிகரித்தால் மீன்களுக்கு நச்சுத்தன்மை ஏற்படும்.</p>"
 ),
 "iv4":(
  "<p>Source water must be tested rather than assumed to be suitable. Bore-well, tap, rain and surface waters differ in hardness, alkalinity, dissolved gases, iron and possible contaminants.</p><p>Condition the selected water before use: remove or neutralise disinfectants where present, match temperature, provide aeration when required and verify pH, hardness and nitrogenous wastes. A water source is suitable only when its measured characteristics match the needs of the species being maintained.</p>",
  "<p>ஒரு நீர்மூலம் தானாகவே ஏற்றது என்று கருதாமல் பரிசோதிக்க வேண்டும். ஆழ்குழாய் நீர், குழாய்நீர், மழைநீர், மேற்பரப்பு நீர் ஆகியவை நீர்க்கடினத்தன்மை, காரத்தன்மை, கரைந்த வாயுக்கள், இரும்புச் சத்து மற்றும் மாசுகள் போன்றவற்றில் மாறுபடலாம்.</p><p>பயன்படுத்தும் முன் நீரைத் தயார்படுத்த வேண்டும்: தேவையான இடங்களில் கிருமிநாசினி எச்சங்களை நீக்குதல் அல்லது செயலிழக்கச் செய்தல், வெப்பநிலையை ஒத்திசைத்தல், தேவைக்கேற்ப காற்றோட்டம் அளித்தல், pH, நீர்க்கடினத்தன்மை மற்றும் நைட்ரஜன் கழிவுகளைச் சரிபார்த்தல் ஆகியவை அவசியம். பராமரிக்கப்படும் இனத்தின் தேவைகளுடன் அளவிடப்பட்ட நீர்ப்பண்புகள் பொருந்தினால் மட்டுமே அந்த நீர்மூலம் ஏற்றதாகும்.</p>"
 ),
 "iv5":(
  "<p>Mollies generally perform well in clean, mineral-rich water with adequate hardness and alkalinity. Some forms tolerate brackish conditions, but routine addition of salt is not compulsory for every molly aquarium.</p><p>Use stable water chemistry, good filtration and appropriate diet as the primary management measures. Salt should be used only for a defined husbandry purpose and at a concentration appropriate to the stock and system.</p>",
  "<p>மொல்லி மீன்கள் பொதுவாகத் தூய்மையான, கனிமச்சத்து நிறைந்த, போதிய நீர்க்கடினத்தன்மையும் காரத்தன்மையும் கொண்ட நீரில் நன்றாக வளர்கின்றன. சில வடிவங்கள் உவர்நீரைத் தாங்கினாலும், எல்லா மொல்லி தொட்டிகளிலும் வழக்கமாக உப்பு சேர்ப்பது கட்டாயமல்ல.</p><p>நிலையான நீர்ப்பண்புகள், சிறந்த வடிகட்டல், பொருத்தமான உணவு ஆகியவையே முதன்மை பராமரிப்பு முறைகள். குறிப்பிட்ட பராமரிப்பு தேவைக்காக மட்டுமே, இனத்திற்கும் வளர்ப்பு அமைப்பிற்கும் ஏற்ற அளவில் உப்பைப் பயன்படுத்த வேண்டும்.</p>"
 ),
 "iv7":(
  "<p>New fish should be quarantined in a separate system before being introduced into an established aquarium. A quarantine period of about 30 days is a practical minimum for classroom and hobby systems because some parasitic and infectious problems are not obvious during a very short holding period.</p><p>Use separate nets and equipment, observe feeding and behaviour, and inspect the fish daily. Do not medicate healthy stock routinely without a diagnosis.</p>",
  "<p>புதிய மீன்களை நிலையான மீன் காட்சித் தொட்டியில் சேர்ப்பதற்கு முன் தனித்த அமைப்பில் தனிமைப்படுத்த வேண்டும். சில ஒட்டுண்ணி மற்றும் தொற்றுநோய் அறிகுறிகள் மிகக் குறுகிய காலத்தில் தெரியாமல் இருக்கலாம்; எனவே வகுப்பறை மற்றும் பொழுதுபோக்கு வளர்ப்பில் சுமார் 30 நாள் தனிமைப்படுத்தல் நடைமுறைப் பாதுகாப்பான குறைந்தபட்சமாகும்.</p><p>தனி வலை மற்றும் கருவிகளைப் பயன்படுத்தி, உணவெடுப்பு மற்றும் நடத்தையை கவனித்து, மீன்களை தினமும் பார்வையிட வேண்டும். நோயறிதல் இன்றி ஆரோக்கியமான மீன்களுக்கு வழக்கமாக மருந்தளிக்கக் கூடாது.</p>"
 ),
 "rev":(
  "<ol><li>Zebrafish (<i>Danio rerio</i>) is indigenous to the Indian subcontinent.</li><li>Koi and oranda are different ornamental forms: koi is <i>Cyprinus carpio</i>; oranda is a variety of <i>Carassius auratus</i>.</li><li>Maintenance and breeding temperatures need not be identical.</li><li>Guppies and related poeciliids are livebearers; guppy reproduction is more precisely described as lecithotrophic viviparity.</li><li>Water requirements and compatibility are species-specific.</li><li>Quarantine, biosecurity and water-quality monitoring are essential parts of responsible ornamental fish culture.</li></ol>",
  "<ol><li>வரிக்குதிரை டேனியோ (<i>Danio rerio</i>) இந்தியத் துணைக்கண்டத்தின் தாயக மீனாகும்.</li><li>கோய் மற்றும் ஓராண்டா வேறு அலங்கார வடிவங்கள்: கோய் — <i>Cyprinus carpio</i>; ஓராண்டா — <i>Carassius auratus</i> இனத்தின் ஒரு அலங்கார இரகம்.</li><li>பராமரிப்பு வெப்பநிலையும் இனப்பெருக்க வெப்பநிலையும் ஒன்றாக இருக்க வேண்டிய அவசியமில்லை.</li><li>கப்பி மற்றும் தொடர்புடைய பீசிலிட் மீன்கள் குஞ்சுகளை ஈனுபவை; கப்பியின் இனப்பெருக்கத்தை அறிவியல் துல்லியத்துடன் <i>lecithotrophic viviparity</i> எனக் குறிப்பிடலாம்.</li><li>நீர்த் தேவைகளும் இணைவாழ்வும் இனத்திற்கேற்ப மாறுபடும்.</li><li>தனிமைப்படுத்தல், உயிரியல் பாதுகாப்பு, நீர்தரக் கண்காணிப்பு ஆகியவை பொறுப்பான அலங்கார மீன் வளர்ப்பின் முக்கிய கூறுகளாகும்.</li></ol>"
 )
}

LAB_TITLES = {
 "vol":("Aquarium Working Volume","மீன் காட்சித் தொட்டியின் பயன்பாட்டு நீரளவு"),
 "id":("External Morphology and Sex Identification","வெளிப்புற உடலமைப்பும் பாலின அறிதலும்"),
 "chem":("Reading Water-quality Test Kits","நீர்தரச் சோதனைக் கருவிகளை வாசித்தல்"),
 "pack":("Packing Practice — Dry Run","பொதியிடும் பயிற்சி — மீன் இல்லா முன்னோட்டம்"),
 "eco":("Farm Economics Worksheet","பண்ணைப் பொருளாதாரப் பயிற்சித்தாள்")
}

LAB_BODIES = {
 "vol":(
  "<p><b>Aim:</b> Estimate the working water volume of a rectangular aquarium.</p><p><b>Principle:</b> V (L) = internal length × internal width × water height (cm) ÷ 1000. Allow separately for substrate and freeboard.</p><p><b>Materials:</b> Measuring tape and calculator.</p><p><b>Procedure:</b> Measure the internal length, internal width and actual water height rather than the outside glass dimensions.</p><p><b>Result:</b> Verify the value using the Volume calculator under Tools.</p><p><b>Precautions:</b> Cylindrical and bow-front aquaria require different formulae. Avoid simplistic inch-per-gallon stocking rules.</p><p><b>Viva:</b> Why is actual water height used instead of total tank height?</p>",
  "<p><b>நோக்கம்:</b> செவ்வக மீன் காட்சித் தொட்டியின் பயன்பாட்டு நீரளவை மதிப்பிடுதல்.</p><p><b>கோட்பாடு:</b> நீரளவு (லிட்டர்) = உள் நீளம் × உள் அகலம் × நீர் உயரம் (செ.மீ.) ÷ 1000. அடிமண் மற்றும் மேற்புற காலியிடத்திற்கான அளவைத் தனியாகக் கணக்கில் கொள்ள வேண்டும்.</p><p><b>பொருட்கள்:</b> அளவுநாடா மற்றும் கணிப்பான்.</p><p><b>முறை:</b> தொட்டியின் வெளிப்புறக் கண்ணாடி அளவுகளை அல்லாமல், உள் நீளம், உள் அகலம் மற்றும் உண்மையான நீர் உயரத்தை அளக்கவும்.</p><p><b>முடிவு:</b> கருவிகள் பகுதியில் உள்ள நீரளவுக் கணிப்பியைப் பயன்படுத்தி மதிப்பைச் சரிபார்க்கவும்.</p><p><b>முன்னெச்சரிக்கை:</b> உருளை மற்றும் வளைந்த முன்புறம் கொண்ட தொட்டிகளுக்கு வேறு கணக்கீட்டு முறைகள் தேவை. இன்ச்–கேலன் போன்ற எளிமைப்படுத்தப்பட்ட மீன் அடர்த்தி விதிகளைப் பயன்படுத்த வேண்டாம்.</p><p><b>வாய்மொழி வினா:</b> தொட்டியின் மொத்த உயரத்திற்கு பதிலாக உண்மையான நீர் உயரத்தை ஏன் பயன்படுத்த வேண்டும்?</p>"
 ),
 "id":(
  "<p><b>Aim:</b> Identify important external characters used in ornamental-fish recognition and sex determination.</p><p><b>Principle:</b> Koi possess barbels around the mouth, whereas oranda is a goldfish variety with a characteristic wen. Male poeciliids such as guppies, mollies, platies and swordtails possess a gonopodium.</p><p><b>Precautions:</b> Minimise handling and repeated netting. Illustrations are learning aids and should not replace examination of a healthy specimen or a reliable diagnostic photograph.</p><p><b>Viva:</b> Which external characters distinguish koi from oranda?</p>",
  "<p><b>நோக்கம்:</b> அலங்கார மீன்களை இனங்காணவும் பாலினத்தை வேறுபடுத்தவும் உதவும் முக்கிய வெளிப்புறப் பண்புகளை அறிதல்.</p><p><b>கோட்பாடு:</b> கோய் மீனின் வாயைச் சுற்றி பார்பல்கள் காணப்படும்; ஓராண்டா தங்கமீனின் அலங்கார இரகமாகும், அதன் தலையில் வென் எனப்படும் மாமிச வளர்ச்சி காணப்படும். கப்பி, மொல்லி, பிளாட்டி, வாள்வால் போன்ற பீசிலிட் ஆண் மீன்களில் கோனோபோடியம் காணப்படும்.</p><p><b>முன்னெச்சரிக்கை:</b> மீன்களைத் தேவையில்லாமல் கையாளுதல் மற்றும் மீண்டும் மீண்டும் வலைபோடுதல் தவிர்க்கப்பட வேண்டும். இங்கே தரப்பட்டுள்ள படங்கள் கற்றலுக்கான உதவிப்படங்களாகும்; ஆரோக்கியமான மாதிரி அல்லது நம்பகமான அடையாளப் புகைப்படத்தைப் பார்வையிடுவதற்குப் பதிலாக பயன்படுத்தக் கூடாது.</p><p><b>வாய்மொழி வினா:</b> கோய் மற்றும் ஓராண்டாவை வேறுபடுத்த உதவும் வெளிப்புறப் பண்புகள் யாவை?</p>"
 ),
 "chem":(
  "<p><b>Aim:</b> Record pH, GH, KH, total ammonia nitrogen (TAN), nitrite, nitrate and temperature using appropriate test kits.</p><p><b>Principle:</b> Colour-comparison kits should be read under suitable white/daylight illumination. TAN is not identical to unionised ammonia (NH₃); the NH₃ fraction rises as pH and temperature increase.</p><p><b>Precautions:</b> Check expiry dates and follow the kit manufacturer's timing and sample-volume instructions. Do not determine medication dosage from illustrative colour examples.</p><p><b>Viva:</b> Why can the same TAN value become more hazardous at a higher pH?</p>",
  "<p><b>நோக்கம்:</b> பொருத்தமான சோதனைக் கருவிகளைப் பயன்படுத்தி pH, GH, KH, மொத்த அமோனியா நைட்ரஜன் (TAN), நைட்ரைட், நைட்ரேட் மற்றும் வெப்பநிலையைப் பதிவு செய்தல்.</p><p><b>கோட்பாடு:</b> நிற ஒப்பீட்டுச் சோதனைகளை ஏற்ற வெள்ளை அல்லது பகலொளியில் வாசிக்க வேண்டும். TAN என்பது அயனியாகாத அமோனியா (NH₃) அளவுக்கு சமமல்ல; pH மற்றும் வெப்பநிலை உயரும்போது NH₃ பகுதி அதிகரிக்கும்.</p><p><b>முன்னெச்சரிக்கை:</b> கருவியின் காலாவதி தேதியைச் சரிபார்த்து, தயாரிப்பாளர் குறிப்பிட்ட நேரம் மற்றும் மாதிரி அளவைப் பின்பற்ற வேண்டும். உதாரண நிறங்களை வைத்து மருந்தளவை நிர்ணயிக்கக் கூடாது.</p><p><b>வாய்மொழி வினா:</b> ஒரே TAN மதிப்பில் pH உயரும்போது நச்சுத்தன்மை ஏன் அதிகரிக்கலாம்?</p>"
 ),
 "pack":(
  "<p><b>Aim:</b> Practise the sequence of ornamental-fish packing without using live animals.</p><p><b>Procedure:</b> On paper, grade fish by species and size; plan an appropriate pre-transport fasting period according to species, size and journey duration; estimate water volume and oxygen headspace; plan double-bagging; and prepare labels with species name and number of fish.</p><p><b>Precautions:</b> Actual live-fish packing must follow species-specific stocking density, temperature and journey-time requirements under competent supervision.</p>",
  "<p><b>நோக்கம்:</b> உயிருள்ள மீன்களைப் பயன்படுத்தாமல் அலங்கார மீன் பொதியிடும் செயல்முறையின் வரிசையைப் பயிற்சி செய்தல்.</p><p><b>முறை:</b> தாளில் இனமும் அளவும் அடிப்படையாக மீன்களை வகைப்படுத்தவும்; இனம், மீன் அளவு, பயண நேரம் ஆகியவற்றுக்கு ஏற்ற முன் உணவு நிறுத்தும் காலத்தைத் திட்டமிடவும்; பையில் தேவையான நீரளவும் ஆக்சிஜன் இடவசதியும் கணக்கிடவும்; இரட்டைப் பை முறையைத் திட்டமிடவும்; இனப் பெயரும் மீன் எண்ணிக்கையும் கொண்ட அடையாளச் சீட்டைத் தயாரிக்கவும்.</p><p><b>முன்னெச்சரிக்கை:</b> உயிருள்ள மீன்களை உண்மையில் பொதியிடும்போது இனத்திற்கேற்ற மீன் அடர்த்தி, வெப்பநிலை, பயண நேரம் ஆகியவற்றைப் பின்பற்றி திறமையான மேற்பார்வையில் செயல்பட வேண்டும்.</p>"
 )
}

def clean_en(s):
 for a,b in [
  ("Current Science / welfare practice:","Scientific and welfare note:"),
  ("Current Science:","Scientific note:"),
  ("Curriculum:","Key point:"),
  ("TNAU-style ",""),("TNAU farm texts also quote 20–30","some breeding protocols use a wider warm-water range"),
  ("TNAU Agritech; ",""),("; TNAU Agritech",""),("TNAU Agritech",""),
  ("in this app","in this resource"),("This app","This resource"),("the app","the resource"),
  ("v1 printed ",""),("critical defect in v1","incorrect identification")
 ]: s=s.replace(a,b)
 return re.sub(r" {2,}"," ",s)

def clean_ta(s):
 for a,b in [
  ("TNAU பெயர்கள்: அலங்கார மீன் வளர்ப்பு / வண்ண மீன் வளர்ப்பு.",""),
  ("TNAU பாணிக் ",""),("TNAU ",""),
  ("பாடத்திட்டம்:","முக்கிய குறிப்பு:"),("நவீன அறிவியல்:","அறிவியல் குறிப்பு:"),
  ("அகவாரித்","மீன் காட்சித் தொட்டித்"),("அகவாரி","மீன் காட்சித் தொட்டி"),
  ("உள்பொரி முட்டையிடுவன","குஞ்சுகளை ஈனும் மீன்கள்"),("உள்பொரி","குஞ்சுகளை ஈனும்"),
  ("ஒரண்டா","ஓராண்டா"),("ஸெப்ரா டேனியோ","வரிக்குதிரை டேனியோ"),
  ("ஃபேன்சி தங்கமீன்","அலங்காரத் தங்கமீன்"),("ஃபேன்சி","அலங்கார இரகம்"),
  ("கர்ப்பப்புள்ளி","கருவுற்ற புள்ளி"),("கர்ப்பப் புள்ளி","கருவுற்ற புள்ளி"),
  ("தத்துவம்:","கோட்பாடு:"),("வாய்மொழி:","வாய்மொழி வினா:"),
  ("வேலை செய்யும் கொள்ளளவு","பயன்பாட்டு நீரளவு"),("மென்நீர்","மென்மையான நீர்"),
  ("முதல் பதிப்பு","முந்தைய விளக்கம்"),
  ("அகவாகல்கல்ச்சர்","நீர்வாழ் உயிரின வளர்ப்பு")
 ]: s=s.replace(a,b)
 return re.sub(r" {2,}"," ",s)

def main():
 species=json.load(open(DATA/"species.json",encoding="utf-8"))
 for s in species:
  for k,v in list(s.items()):
   if not isinstance(v,str): continue
   s[k]=clean_ta(v) if (k.startswith("ta_") or k in {"ta","alt_ta"}) else clean_en(v)
  s["refs"]=s.get("refs","").replace("Additional institutional pages: ; ","Additional institutional sources: ").replace("Additional institutional pages: ","Additional institutional sources: ")
  if s["id"]=="rosybarb": s["status_in"]="Indigenous ornamental fish of the Indian subcontinent"
  if s["id"]=="goldfish":
   s["temp_breed_c"]="Spawning is commonly stimulated near 20–24 °C after conditioning and a gradual temperature rise."
   s["breed"]="Adhesive egg scatterer. A practical breeding group may use 1 female : 2 males. Fecundity varies widely; eggs commonly hatch in about 2–4 days depending on temperature."
   s["ta_temp_breed_c"]="நன்கு தயார்படுத்தப்பட்ட முதிர்ந்த மீன்களில் வெப்பநிலையை மெதுவாக உயர்த்தி, சுமார் 20–24°செ. அருகில் முட்டையிடலைத் தூண்டலாம்."
   s["ta_breed"]="ஒட்டும் தன்மையுள்ள முட்டைகளைச் சிதறவிடும். நடைமுறையில் 1 பெண் : 2 ஆண் என்ற இனப்பெருக்கக் குழுவைப் பயன்படுத்தலாம். முட்டைகளின் எண்ணிக்கை பரவலாக மாறுபடும்; வெப்பநிலையைப் பொறுத்து பொதுவாக 2–4 நாள்களில் பொரியும்."
  if s["id"]=="guppy":
   s["breed"]="Lecithotrophic livebearer. Females give birth to free-swimming young; brood size varies widely and sperm can be stored for later broods."
   s["ta_breed"]="குஞ்சுகளை ஈனும் மீன். பெண் மீன் நீந்தக்கூடிய குஞ்சுகளை நேரடியாக ஈனும்; குஞ்சு எண்ணிக்கை மாறுபடும். பெண் மீன் விந்தணுக்களைச் சேமித்து பின்னர் மேலும் குஞ்சுகளை ஈனலாம்."
   s["ta_status_in"]="இந்தியாவில் பரவலாக வளர்க்கப்படும் அயல்நாட்டு அலங்கார மீன்; சிறு அளவிலான பண்ணைகளில் முக்கியமானது."
   s["ta_temp_breed_c"]="குஞ்சுகளை ஈனும் இனம்; தனியான வெப்பத் தூண்டல் பொதுவாகத் தேவையில்லை."
  live={
   "guppy":("அமைதியான பிற குஞ்சுகளை ஈனும் மீன்களுடன்.","குஞ்சுகளை ஈனும் அலங்கார மீன்."),
   "molly":("பிற அமைதியான குஞ்சுகளை ஈனும் மீன்களுடன்; காரத்தன்மை பொருந்தும் இனங்களுடன்.","குஞ்சுகளை ஈனும் அலங்கார மொல்லி மீன்."),
   "sailfin":("போதிய இடமுள்ள அமைதியான இனங்களுடன்.","குஞ்சுகளை ஈனும் பாய்மீன் மொல்லி."),
   "platy":("பிற அமைதியான குஞ்சுகளை ஈனும் மீன்களுடன்.","குஞ்சுகளை ஈனும் பிளாட்டி."),
   "swordtail":("பிற அமைதியான குஞ்சுகளை ஈனும் மீன்களுடன்; ஆண் மீன்களுக்கு போதிய இடம் தேவை.","குஞ்சுகளை ஈனும் வாள்வால் மீன்.")
  }
  if s["id"] in live:
   s["ta_compat"], s["alt_ta"] = live[s["id"]]
   if s["id"]!="guppy":
    s["ta_status_in"]=s["ta_status_in"].replace("குஞ்சுகளை ஈனும்","அலங்கார மீன்").replace("அலங்கார மீன் மீன்","அலங்கார மீன்")
    s["ta_temp_breed_c"]="குஞ்சுகளை ஈனும் இனம்; தனியான முட்டையிடும் வெப்பத் தூண்டல் பொதுவாகத் தேவையில்லை."
    s["ta_breed"]="குஞ்சுகளை ஈனும் மீன்; பெண் மீன் நீந்தக்கூடிய குஞ்சுகளை ஈனும்."
 json.dump(species,open(DATA/"species.json","w",encoding="utf-8"),ensure_ascii=False,separators=(",",":"))

 book=json.load(open(DATA/"book.json",encoding="utf-8"))
 for x in book:
  x["en"]=clean_en(x["en"]); x["ta"]=clean_ta(x["ta"])
  x["en_t"]=clean_en(x["en_t"]); x["ta_t"]=clean_ta(x["ta_t"])
  if x["id"] in BOOK_TITLES: x["en_t"],x["ta_t"]=BOOK_TITLES[x["id"]]
  if x["id"] in BOOK_BODIES: x["en"],x["ta"]=BOOK_BODIES[x["id"]]
  if x["id"]=="in1":
   x["en"]=x["en"].replace(", TNAU","").replace("TNAU.","")
   x["ta"]=x["ta"].replace(", TNAU","").replace("TNAU.","")
  if x["id"]=="in2": x["ta"]=x["ta"].replace("குஞ்சுகளை ஈனும்யும்","குஞ்சுகளை ஈனும் அயல்நாட்டு மீன்களும்")
 json.dump(book,open(DATA/"book.json","w",encoding="utf-8"),ensure_ascii=False,separators=(",",":"))

 quiz=json.load(open(DATA/"quiz.json",encoding="utf-8"))
 for x in quiz:
  x["q"]=clean_en(x["q"]); x["qt"]=clean_ta(x["qt"]); x["e"]=clean_en(x["e"]); x["et"]=clean_ta(x["et"])
  x["o"]=[clean_en(v) for v in x["o"]]; x["ot"]=[clean_ta(v) for v in x["ot"]]
 quiz[0].update({"q":"Which statement correctly distinguishes Koi from Oranda?","qt":"கோய் மற்றும் ஓராண்டாவைச் சரியாக வேறுபடுத்திக் காட்டும் கூற்று எது?","o":["They are the same species","Koi is Cyprinus carpio; Oranda is a variety of Carassius auratus","Both are marine wrasses","Both are indigenous loaches"],"ot":["இரண்டும் ஒரே இனம்","கோய் — Cyprinus carpio; ஓராண்டா — Carassius auratus இனத்தின் ஓர் அலங்கார இரகம்","இரண்டும் கடல் வாழ் ராஸ் மீன்கள்","இரண்டும் இந்தியத் தாயக லோச் மீன்கள்"],"e":"Koi is an ornamental form of Cyprinus carpio, whereas Oranda is a fancy goldfish variety of Carassius auratus.","et":"கோய் என்பது Cyprinus carpio இனத்தின் அலங்கார வடிவம்; ஓராண்டா என்பது Carassius auratus இனத்தைச் சேர்ந்த அலங்காரத் தங்கமீன் இரகம்."})
 quiz[1].update({"qt":"Danio rerio இந்தியாவில் எவ்வகை மீன்?","ot":["அயல்நாட்டு குஞ்சுகளை ஈனும் மீன்","இந்தியத் துணைக்கண்டத் தாயக முட்டையிடும் மீன்","CITES இணைப்பு I கடல் மீன்","உவர்நீர்ப் பஃபர் மீன்"],"et":"வரிக்குதிரை டேனியோ இந்தியத் துணைக்கண்டத்தின் தாயக இனமாகும்."})
 quiz[2].update({"o":["Mammal-like placental viviparity","Lecithotrophic livebearing","Mouth brooding only","Marine nest building"],"ot":["பாலூட்டிகளைப் போன்ற நஞ்சுக்கொடி வழி உயிர்க்குட்டிப் பேறு","முட்டை மஞ்சளின் ஊட்டத்தில் வளர்ந்து குஞ்சுகளை ஈனுதல்","வாயில் மட்டும் முட்டைகளை அடைகாத்தல்","கடலில் கூடு அமைத்தல்"],"e":"Guppies are livebearers; embryos depend mainly on yolk reserves rather than a mammalian-type placenta.","et":"கப்பி குஞ்சுகளை ஈனும் மீன்; கருக்கள் பெரும்பாலும் முட்டை மஞ்சளிலிருந்து ஊட்டம் பெறுகின்றன."})
 quiz[3].update({"q":"A practical goldfish breeding group may use","qt":"தங்கமீன் இனப்பெருக்கக் குழுவில் பொதுவாகப் பயன்படுத்தக்கூடிய விகிதம் எது?","e":"One mature female with two males is a commonly used practical breeding group; spawning success also depends on conditioning and water quality.","et":"ஒரு முதிர்ந்த பெண் மீனுடன் இரண்டு ஆண் மீன்களை இணைப்பது நடைமுறையில் பொதுவாகப் பயன்படுத்தப்படும் விகிதமாகும்; வெற்றி மீன்களின் தயார்நிலை மற்றும் நீர்தரத்தையும் சார்ந்தது."})
 quiz[7].update({"e":"Routine salt addition is not compulsory for every molly tank; stable hardness, alkalinity and water quality are more important.","et":"எல்லா மொல்லி தொட்டிகளிலும் வழக்கமாக உப்பு சேர்ப்பது கட்டாயமல்ல; நிலையான நீர்க்கடினத்தன்மை, காரத்தன்மை மற்றும் நீர்தரம் முக்கியமானவை."})
 quiz[14].update({"e":"About 30 days or longer is a practical quarantine period for many classroom and hobby systems; very short holds can miss developing infections.","et":"வகுப்பறை மற்றும் பொழுதுபோக்கு வளர்ப்பில் சுமார் 30 நாள் அல்லது அதற்கு மேற்பட்ட தனிமைப்படுத்தல் நடைமுறைப் பாதுகாப்பானது; மிகக் குறுகிய காலத்தில் சில தொற்றுகள் தெரியாமல் போகலாம்."})
 quiz[18]["qt"]="அலங்காரத் தங்கமீனையும் நியான் டெட்ராவையும் 28°செ. வெப்பத்தில் 12 மணி நேரம் ஒரே பயணப் பையில் வைப்பதில் முக்கியக் குறை என்ன?"
 json.dump(quiz,open(DATA/"quiz.json","w",encoding="utf-8"),ensure_ascii=False,separators=(",",":"))

 labs=json.load(open(DATA/"labs.json",encoding="utf-8"))
 for x in labs:
  x["en"]=clean_en(x["en"]); x["ta"]=clean_ta(x["ta"])
  if x["id"] in LAB_TITLES: x["en_t"],x["ta_t"]=LAB_TITLES[x["id"]]
  if x["id"] in LAB_BODIES: x["en"],x["ta"]=LAB_BODIES[x["id"]]
  x["ta"]=x["ta"].replace("இவ்வளவுரையின் வரைபடங்கள்","இங்கே உள்ள வரைபடங்கள்").replace("இவ்வளவுரையின் நிற அட்டவணை","இங்கே உள்ள நிற அட்டவணை")
 json.dump(labs,open(DATA/"labs.json","w",encoding="utf-8"),ensure_ascii=False,separators=(",",":"))

 combined="\n".join((DATA/f).read_text(encoding="utf-8") for f in ("species.json","book.json","quiz.json","labs.json"))
 banned=["TNAU","உள்பொரி","அகவாரி","ஒரண்டா","ஸெப்ரா டேனியோ","ஃபேன்சி","v1 printed","critical defect in v1","குஞ்சுகளை ஈனும்யும்"]
 bad=[x for x in banned if x.lower() in combined.lower()]
 if bad:
  raise SystemExit("Phase-2 terminology gate failed: "+", ".join(bad))
 print("Phase-2 content polish: PASS")

if __name__=="__main__":
 main()
