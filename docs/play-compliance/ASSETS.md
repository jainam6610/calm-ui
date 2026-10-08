# ASSETS — bundled third-party assets, source and license

Audit date: 2026-10-08. Evidence commands listed per row.

## Fonts

| Asset | Files | License | Evidence | Action |
|---|---|---|---|---|
| Poppins | `StudentLauncher/app/src/main/res/font/poppins_light.ttf`, `poppins_regular.ttf`, `poppins_medium.ttf` | **SIL Open Font License 1.1** — license text shipped in repo at `StudentLauncher/app/OFL-POPPINS.txt` (tracked: `git ls-files`) | file listing | ⚠️ OFL requires the license to accompany the font *software*. It is in the repo but **not in the APK and not credited in-app**. Recommendation: add a one-line "Poppins — SIL OFL 1.1" credit with a link to `OFL-POPPINS.txt`/OFL text in the About sheet, or ship the OFL text under `res/raw/` + an open-source-licenses screen (§2.9). Not implemented here (in-app UI addition). |

## Video

| Asset | Files | Source | Evidence | Verdict |
|---|---|---|---|---|
| Onboarding promo | `StudentLauncher/app/src/main/res/raw/onboarding_ad.mp4` (2,473,405 B; 10.005 s; 1280×720 h264 + AAC) | Unknown/AI-generated; no license or attribution in repo | `ffprobe` + `ffmpeg -vf fps=1` frame extraction (10 frames) | ⚠️ **replace** (see report §2.4): frames show a Google "G" logo and a YouTube logo on a simulated home screen, an AI-generator watermark, and pseudo-UI text that does not match this app's real UI. Storing it is fine legally-unproven; using it in-app risks (a) third-party marks under Impersonation/IP and (b) inaccurate functionality depiction under Deceptive Behavior/Metadata. |

## Images / store graphics

| Asset | Files | Source | License | Action |
|---|---|---|---|---|
| App icon (512) | `StudentLauncher/store/icon-512.png` | added in `e2686f6 "v2.0 - Initial release"`; no provenance in repo | **unknown** ❓ | Developer must confirm it was self-made (then record it here) or replace it |
| Feature graphic | `StudentLauncher/store/feature-graphic-1024x500.png` | same commit | **unknown** ❓ | same |
| Launcher icon in app | `res/drawable/ic_launcher_foreground.xml`, `res/mipmap-anydpi-v26/*` | authored in repo (vector XML) | first-party | none |
| Preset wallpapers | none as files — drawn procedurally (`ui/Wallpapers.kt:34,41`: `dune`, `mist`, `meadow`, `plain`) | first-party code | first-party | none |
| User photos | copied from the system photo picker into `filesDir` (`ui/Wallpapers.kt:64-94`) | user's own | n/a | disclosed in privacy policy |

## Code dependencies

`app/build.gradle.kts:56-68` — AndroidX (core-ktx, activity-compose, lifecycle), Compose BOM 2025.04.01, JUnit (test only). No third-party SDK, no ad/analytics/tracking library. Confirmed against the release APK dex: `apkanalyzer dex packages --defined-only` lists only `com.studentlauncher`, `androidx.*`, framework stubs, and R8 short names.

**AndroidX/Compose notices:** customary (not a Play blocking requirement) is an open-source-licenses screen (`OSS licenses` menu) or a file in the repo listing Apache-2.0 components. Recommendation only — not implemented (would add UI).
