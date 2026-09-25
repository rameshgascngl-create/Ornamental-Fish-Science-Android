# Ornamental Fish Science — Android v2.6.1

Offline educational app (`com.tnfisheries.ornamentalfish`).

v2.6.1 keeps the native chrome Play asked for (toolbar, bottom tabs, Back,
TTS, share, reminder, bookmark export) and **drops AppCompat / Material**.
Those libraries were pulling `ProfileInstallReceiver` (DUMP guard) and
`DebugProbesKt.bin` into the 2.5.x Play APK.

## What 2.6.1 changes
- Framework `Activity` + `Toolbar` + five-tab bar (no AndroidX)
- Same `window.Android` bridge and `window.__appBack()` Back handling
- `versionName` **2.6.1** / `versionCode` **261**
- Release minify on; packaging excludes `DebugProbesKt.bin`
- Academic payload still restored by `scripts/fetch-atlas.sh`

## Open in Android Studio
1. Clone this repository (branch `v2.6.1-structured` or `main` after merge).
2. Restore atlas images:

       bash scripts/fetch-atlas.sh

3. Open the folder in Android Studio and sync Gradle.
4. Sign the release build with the existing `ornamentalfish` keystore.

## GitHub Actions
Workflow **Ornamental Fish v2.6.1 signed release** uses the same secrets as 2.5.x:
- `OFISH_KEYSTORE_BASE64`
- `OFISH_KEYSTORE_PASSWORD`
- `OFISH_KEY_ALIAS` = `ornamentalfish`
- `OFISH_KEY_PASSWORD`

Expected signing certificate SHA-256:
`59F5D6ECBFED41958CA04DA3E23E5CE9ADA98814D5023B053336C39F0D21F51B`

## Play notes
No `INTERNET`. No `DUMP`. Privacy policy:
https://rameshgascngl-create.github.io/Zoology-and-Life-Sciences-Digital-Learning-Resources/ornamental-fish-science/privacy-policy-v2.html
