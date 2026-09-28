# Ornamental Fish Native v3 — Phase-3 Device QA Gate

This checklist applies to the installable `3.0.0-alpha3-qa / 302` build only.
Production signing and store submission are blocked until every P0/P1 item passes.

## 1. Installation and identity

- Install alongside the existing production app; QA package must be `com.tnfisheries.ornamentalfish.nativeqa`.
- Cold launch must reach Home without network access.
- Force stop and reopen: language, bookmarks, lesson position and quiz progress must recover.
- No crash after repeated foreground/background cycles.

## 2. Home and global navigation

- Bottom navigation must show five vector icons and labels; no H/A/L/Q/T development placeholders.
- English and Tamil labels must fit without clipping at default font scale.
- Language switch must update all visible native UI immediately.
- Home hero fish must be fully visible without body/fin cropping.
- Back sequence: species detail → Atlas; top-level destination → previously visited top-level destination → Home.
- Back from Home exits normally; it must not loop or reopen the same screen.

## 3. Atlas

Test filters: All, Indian native, Exotic, Freshwater, Brackish, Marine, Livebearer, Egg-layer, Peaceful, Active/territorial, Conservation.

- Search for Goldfish, Carassius, Cyprinidae and Tamil fish names.
- Open Goldfish, Guppy, Koi, Denison barb and Pearlspot.
- Scientific names must be italic.
- Fish images must use fit presentation; no clipped head, caudal fin or body.
- Bookmark survives reopening.
- Enter a detail page and return: previous Atlas query and selected filter must remain.
- TTS and Share must function without invoking a WebView.

## 4. Species-detail science

For each sampled species verify visible sections:

- Taxonomy and identification
- Distribution and status in India
- Housing and water requirements
- Diet, breeding and fry
- Health, quarantine and conservation

Specific checks:
- Koi = `Cyprinus carpio`; Oranda = fancy `Carassius auratus`.
- Guppy is described as a livebearer / lecithotrophic livebearing, not mammalian placental reproduction.
- Goldfish maintenance and breeding temperatures are not presented as one universal value.
- Molly salt use is not described as compulsory.
- Denison barb conservation text must not be diluted by generic aquarium wording.

## 5. Learn — English and Tamil

Inspect at minimum:
- 1.1 Introduction to Ornamental Fish Culture
- 2.1 Egg-layers and Livebearers
- 2.2 Goldfish
- 4.3 Nitrogen Cycle
- 4.7 Quarantine and Biosecurity
- Revision — Key Concepts

Pass criteria:
- No stray chapter marker such as a standalone “I”.
- Unit, lesson title, paragraphs, bold/italic terms and lists render with hierarchy.
- No learner-facing `TNAU` attribution wording.
- No legacy Tamil terms `உள்பொரி`, `அகவாரி`, `ஒரண்டா`, `ஸெப்ரா டேனியோ`, `ஃபேன்சி`.
- Tamil should read as standard Tamil Nadu textbook-register Tamil; avoid unnecessary transliteration when a clear Tamil term exists.
- English should use Indian academic English and remain concise and grammatically complete.

## 6. Practicals

Inspect all five activities, especially:
- Aquarium Working Volume
- External Morphology and Sex Identification
- Water-quality Test Kits
- Packing Practice — Dry Run

Pass criteria:
- Aim, principle/procedure, precautions and viva structure are readable.
- Packing does not prescribe a universal fixed fasting period; it must remain species/size/journey dependent.
- Male poeciliid gonopodium wording must not be generalised to all livebearing fishes.
- TAN and unionised NH3 are distinguished correctly.

## 7. Quiz

- Complete at least five questions in each language.
- Correct and incorrect selections must remain visually distinguishable.
- Explanation must match the keyed answer.
- Results page must identify missed questions and correct answers.
- Reset must erase quiz state.
- “Remind tomorrow” must request notification permission when required and schedule without requesting exact-alarm permission.

## 8. Tools

Test:
- Aquarium working volume
- Unionised NH3 from TAN
- Water-change volume
- Farm economics

Verify invalid/blank/out-of-range values do not crash.
The ammonia tool must state that it is a teaching calculation and not a replacement for laboratory measurement.

## 9. Configuration and accessibility

- Rotate portrait → landscape → portrait from Home, Atlas detail, Learn and Quiz.
- Selected screen and saveable state must recover after Activity recreation.
- Test Android font size at default and one enlarged setting.
- No clipped Tamil glyphs or overlapped buttons.
- TalkBack: bottom navigation must not read duplicate icon/label names; meaningful fish images should have a readable description.
- Three-button and gesture navigation: bottom bar must remain above system navigation.

## 10. Offline and release boundary

- Airplane mode: Home, Atlas images, Learn, Quiz and Tools must work.
- Only the external HTTPS privacy-policy action may leave the app.
- No embedded HTML/JS/CSS runtime or WebView is permitted.
- Production release remains blocked until this device-QA checklist passes and the final signed R8 build is independently inspected.
