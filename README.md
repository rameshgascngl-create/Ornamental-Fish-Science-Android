# Ornamental Fish Science — Android v2.5.0

Offline educational app for ornamental fish science
(`com.tnfisheries.ornamentalfish`).

This tree answers Google Play’s *“Your website is converted into an app”*
minimum-functionality rejection. v2.4.4 was a WebView-only Activity with an
HTML tab bar. v2.5.0 adds native Android chrome and device features.

## What changed in 2.5.0
- Native `Toolbar` + `BottomNavigationView` around the existing offline WebView
- System Back asks `window.__appBack()` first (species account → Atlas, not exit)
- TextToSpeech pronunciation, system share sheet, revision notification, bookmark export
- In-page HTML tab bar hides when native chrome is present
- Version unified to **2.5.0** / versionCode **250** (was 2.4.4 / 244)

Academic payload (species, book, quiz, labs, atlas images, `boot.js`) is
unchanged from the frozen v2.4.3 hashes in `release-evidence/`.

## Open in Android Studio
1. Clone this repository.
2. Restore atlas images (not stored in git; they are frozen in the v2.4.4 source zip):

       bash scripts/fetch-atlas.sh

3. Open the folder in Android Studio (this is a Gradle project).
4. Build a **release** AAB/APK with the same keystore used for v2.4.4
   (`alias ornamentalfish`).
5. Device QA: bottom nav both ways, Back on a species page, Pronounce, Share,
   quiz reminder, bookmark export.

## GitHub Actions signed build
Secrets already used for v2.4.4:
- `OFISH_KEYSTORE_BASE64`
- `OFISH_KEYSTORE_PASSWORD`
- `OFISH_KEY_ALIAS` = `ornamentalfish`
- `OFISH_KEY_PASSWORD`

Run workflow **Ornamental Fish v2.5.0 signed release**. Download the artifact
`OrnamentalFish-v2.5.0-PLAY-FIX.apk`.

Expected signing certificate SHA-256:
`59F5D6ECBFED41958CA04DA3E23E5CE9ADA98814D5023B053336C39F0D21F51B`

## Play Console review notes
> Not a website wrapper. Content is offline in the APK. v2.5.0 adds a native
> Toolbar and BottomNavigationView, system Back that leaves a species account
> without exiting, TextToSpeech, the system share sheet, a revision
> notification, and bookmark export via the Storage Access Framework. https
> links open in the browser.

No `INTERNET` permission. Privacy policy:
https://rameshgascngl-create.github.io/Zoology-and-Life-Sciences-Digital-Learning-Resources/ornamental-fish-science/privacy-policy-v2.html
