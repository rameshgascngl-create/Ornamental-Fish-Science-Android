# Ornamental Fish Science 2.6.1 — Play release pack

## Identity

| Field | Value |
|---|---|
| Name | Ornamental Fish Science |
| Package | `com.tnfisheries.ornamentalfish` |
| versionName | **2.6.1** |
| versionCode | **261** |
| minSdk | 24 |
| targetSdk | 36 |
| Upload key SHA-256 | `59F5D6ECBFED41958CA04DA3E23E5CE9ADA98814D5023B053336C39F0D21F51B` |
| Play artifact | signed **AAB** |
| Sideload / alt stores | signed **APK** |

Do not change the package ID. Use Play App Signing; this repo only holds the upload key in Actions secrets.

## Permissions and Data Safety

| Permission | Why | Data Safety |
|---|---|---|
| `POST_NOTIFICATIONS` | Optional revision reminder after a quiz | Not collected. Notification is local. User is prompted only when they tap Remind me. |
| `RECEIVE_BOOT_COMPLETED` | Restore that one reminder after reboot | Not collected. |
| Internet / camera / mic / location / contacts / phone / SMS / storage | **Not requested** | No data shared off device. |

Collected data: **none**. Local-only: language, bookmarks, quiz progress, reading page, reminder title/time in SharedPreferences / WebView localStorage.

Ads: no. Analytics: no. SDKs beyond Android framework: none.

Target audience: higher education / adult learners. Recommended Play audience **13+** or supervised classroom. Not Designed for Families unless you complete that programme separately.

## Privacy policy

Public URL (HTTPS): https://rameshgascngl-create.github.io/Zoology-and-Life-Sciences-Digital-Learning-Resources/ornamental-fish-science/privacy-policy-v2.html

Must mention: package ID, local storage keys, optional notifications, no account, no ads.

## Third-party assets

Fish atlas images ship with per-file `img_note` / `img_kind` (`photograph` vs `ai`) in `species.json`. Goldfish photograph is Wikimedia CC BY 2.0 (George Chernilevsky). Other photographs require the note on the species page. AI plates must not be described as diagnostic photos. Course text is original classroom material for Tamil Nadu fisheries education.

## What this pack does **not** claim

No physical device or emulator QA was run in the automated session. Lint/unit-test/zipalign/apksigner results come from GitHub Actions when that workflow finishes.
