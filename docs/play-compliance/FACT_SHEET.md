# FACT_SHEET — StudentLauncher v3.0 ground truth

Audit date: **2026-10-08** (local `master` = `9c7e034`, 9 commits ahead of `origin/master`).
Every row cites `file:line` or the exact command + its output. Nothing here is from memory.

## §1.1 Manifest (source + merged)

Source: `StudentLauncher/app/src/main/AndroidManifest.xml`
Merged (release): `StudentLauncher/app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml` (produced by `./gradlew :app:processReleaseMainManifest`, exit 0)

| Fact | Evidence |
|---|---|
| Permissions (source) | `AndroidManifest.xml:5` → `POST_NOTIFICATIONS` only |
| Permissions (release merged) | `aapt2 dump permissions app-release.apk` → `POST_NOTIFICATIONS` + `com.studentlauncher.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (auto-added by androidx.core for its own non-exported receiver; normal, not a user-facing permission) |
| `QUERY_ALL_PACKAGES` | **absent** from source and merged manifest |
| `<queries>` entries | `AndroidManifest.xml:7-29` → `MAIN/LAUNCHER` (app list), `DIAL`, `SENDTO smsto`, `VIEW https`, `IMAGE_CAPTURE`, `settings.SETTINGS` |
| Exported components | `AndroidManifest.xml:40-54` `MainActivity` `exported="true"` with `MAIN` + `HOME` + `DEFAULT` + `LAUNCHER`; `AndroidManifest.xml:56-58` `FocusReminderReceiver` `exported="false"` |
| Launch mode | `AndroidManifest.xml:43` `singleTask`, `stateNotNeeded="true"` (line 44) |
| Receivers/services/providers | 1 manifest receiver (`FocusReminderReceiver`); 0 services; 0 providers. A *dynamic* receiver for `ACTION_PACKAGE_ADDED/REMOVED/REPLACED` is registered at runtime: `MainActivity.kt:34-40` |
| Backup | `AndroidManifest.xml:32` `android:allowBackup="false"`; no `dataExtractionRules`, no `fullBackupContent` (lint `DataExtractionRules` warning, see §2.8) |
| Cleartext / network security | no `usesCleartextTraffic`, no `networkSecurityConfig` attributes anywhere in manifest |
| Target/compile SDK | `app/build.gradle.kts:16,21` → compileSdk 36, targetSdk 36; `minSdk 26` (line 20); `versionCode 7`, `versionName "3.0"` (lines 22-23) |

## §1.2 Release artifact

Built: `./gradlew :app:processReleaseMainManifest :app:assembleDebug` → exit 0; `./gradlew testDebugUnitTest lintDebug assembleDebug` → exit 0.
Release APK present from an earlier signed build: `StudentLauncher/app/build/outputs/apk/release/app-release.apk` (1,503,559 B) and AAB `app/build/outputs/bundle/release/app-release.aab` (3,250,724 B).

| Fact | Evidence |
|---|---|
| APK permissions | `aapt2 dump permissions app-release.apk` (build-tools 37.0.0) → the two rows above |
| Dex packages (`apkanalyzer dex packages --defined-only`) | `com.studentlauncher` + `androidx.*` / `android.compose.*` / `android.*` framework stubs + R8 short obfuscated names (`p0`, `t1`, …). **No third-party SDK packages** (no okhttp/retrofit/firebase/gms/mopub/appsflyer) |
| Native libs | `unzip`-equivalent listing → `lib/{arm64-v8a,armeabi-v7a,x86,x86_64}/libandroidx.graphics.path.so` (from `androidx.graphics`, transitively via Compose) |
| 64-bit | `arm64-v8a` + `x86_64` present → 64-bit requirement met |
| 16 KB page size | `zipalign -c -P 16 -v 4 app-release.apk` → `Verification successful`. Requirement: "all apps targeting Android 15 (API level 35) and higher must support 16 KB memory page sizes on 64-bit devices on Google Play … starting February 1, 2027 [updates without support can't ship]" — https://developer.android.com/guide/practices/page-sizes (fetched 2026-10-08) |
| Debuggable | `android:debuggable` absent from release merged manifest (not present in the `<application>` element) |
| Test/debug flags | `rg 'BuildConfig\|DEBUG' app/src/main` → no matches |

## §1.3 Network / data egress

Command: `rg -n 'https?://|HttpURLConnection|OkHttp|WebView|Socket|DownloadManager|ACTION_VIEW|mailto:|URL\(' app/src -g '*.kt'` → exactly 4 hits:

| Location | What it does |
|---|---|
| `data/AppRepository.kt:67` | `resolve(Intent(ACTION_VIEW, Uri.parse("https://example.com")))` — intent-resolution probe to find the browser for the default dock; **no network I/O** |
| `ui/FirstRun.kt:355` | opens the privacy policy URL in the user's browser |
| `ui/WidgetSheets.kt:318` | opens the privacy policy URL in the user's browser |

No `HttpURLConnection`, no OkHttp, no `WebView`, no `Socket`, no `DownloadManager`, no SDK init, no `mailto:`.
**Verdict: the app never transmits data off the device.** Opening a URL in the user's browser is a user-initiated browser navigation, not app transmission. Supporting evidence: no INTERNET permission declared or merged (§1.1), so the process cannot open sockets at all.

## §1.4 Data the app creates or stores

All app state lives in **one** `SharedPreferences` file `student_launcher` (`LauncherViewModel.kt:39`, `Context.MODE_PRIVATE`) plus two kinds of files in `filesDir`. No database, no cache dir usage (`rg 'cacheDir|openFileOutput'` → no matches).

| Data family | Keys / file | What it holds | Identifies a person? | Leaves device? | Protection |
|---|---|---|---|---|---|
| **PIN** | `pin_hash`, `pin_salt`, `pin_fails`, `pin_lock_until`, `pin_reset_started`, `pin_protect_study` (`security/PinLock.kt:94-99`) | **salted PBKDF2-HMAC-SHA256 hash only** — 16-byte `SecureRandom` salt, 20 000 iterations, 256-bit key (`PinLock.kt:27,85-88,100-102`). The PIN itself is never stored | No (credential material, not identity) | No | App-private prefs; hash verified with `MessageDigest.isEqual` (`PinLock.kt:50`) |
| Focus/session usage stats | `today_date`, `today_usage`, `session_pkg`, `session_start`, `pause_date`, `pause_count_today` (`LauncherViewModel.kt:369-391,319-333,341-358`) | minutes per flagged app, self-tracked from **the moment this launcher starts the app** (`startApp` → `beginSession`, `LauncherViewModel.kt:307-316`) until `endSession` (`MainActivity.kt:71-77`) | No | No | app-private prefs |
| `UsageStatsManager` | **absent** — `rg 'UsageStatsManager\|PACKAGE_USAGE_STATS' app/src` → 0 matches; `PACKAGE_USAGE_STATS` not in manifest (§1.1) | — | — | — | — |
| Plan tasks/events | `plan_tasks`, `plan_events` (`LauncherViewModel.kt:142-143`) | task title, course, ISO due date, priority, completion; events | **Yes** — user-authored personal schedule | No | app-private prefs |
| Widget configs + widget data | `home_widgets_v2` / `widgets_v2` / `board_widgets_v2` (`LauncherViewModel.kt:97-102`) written through `vm.setCfg` (`StudentWidgets.kt:112,148`) | widget layout **and** personal widget content: expense log + budget (`StudentWidgets.kt:116-148`), GPA rows (`StudentWidgets.kt:402-414`), habits, water log, flashcards, assignments, reminders, planner rows, mindful day history (`MindfulStore.kt:52-63`) | **Yes** — grades, spending, health-adjacent log | No | app-private prefs |
| Mindful settings/limits | `mindful_*` settings JSON, per-app limits (`MindfulStore.kt:11-47`) | distraction budgets, schedules | No | No | app-private prefs |
| Wallpaper choices | `wallpaper`, `wpBlur/Dim/Zoom/PanX/PanY`(+`_slot`) (`LauncherViewModel.kt:49-55,512-529`) | preference + edit values | No | No | app-private prefs |
| **Photo copies** | `filesDir/wallpaper.jpg`, `wallpaper1..n.jpg` (`ui/Wallpapers.kt:64-65,94`) | JPEG re-encode of user-picked photos (system photo picker, `MainActivity.kt:43-47`) | **Yes** — user's personal images | No | app-private files; original URIs are not retained (photos are decoded and re-written as JPEG) |
| Lists | `pinned`, `flagged`, `study`, home order (`LauncherViewModel.kt:622-700`) | package names of chosen apps | Indirectly (installed apps) | No | app-private prefs |
| Backups of this data | **none** — `allowBackup="false"` (`AndroidManifest.xml:32`) → no cloud backup, no device-to-device transfer | — | — | — | — |

Deletion available today: **Clear storage / uninstall only** (no in-app "delete all data" button — see report recommendation).

## §1.5 Installed-app visibility

- Enumeration: `<queries>` with `MAIN`+`LAUNCHER` intent (`AndroidManifest.xml:7-11`) + `PackageManager.getLaunchIntentForPackage` / launcher-intent queries (`data/AppRepository.kt`).
- `QUERY_ALL_PACKAGES`: **not declared** (§1.1). The launcher exception in the Package (App) Visibility permission policy ("Apps that have a core purpose to launch, search, or interoperate with other apps… may obtain scope-appropriate visibility") is therefore satisfied by the *limited* path — policy explicitly prefers it: "You may not use QUERY_ALL_PACKAGES if your app can operate with a more targeted scoped package visibility declaration."
- No declaration form needed for app visibility.

## §1.6 Android widget hosting

`data/WidgetHostManager.kt:9-21` — plain `AppWidgetHost(ctx, 1024)` + `AppWidgetManager`.
Binding: `MainActivity.kt:90-102` → `allocateAppWidgetId()` → `bindAppWidgetIdIfAllowed(...)`; if not allowed, fires `AppWidgetManager.ACTION_APPWIDGET_BIND` → **system bind dialog** (lines 96-100), result handled in `onActivityResult` (line 118).
No `BIND_APPWIDGET`, no `INSTALL_SHORTCUT`, no privileged permission requested (§1.1).

## §1.7 Notifications and alarms

- `POST_NOTIFICATIONS` requested **in context**: only from Settings → Focus → reminder interval / "Allow reminders" (`ui/FirstRun.kt:313-318` → `MainActivity.kt:57-61`). Not requested at first launch.
- Denial path: toast "Allow notifications so reminders can appear" (`MainActivity.kt:49-51`); everything else in the app keeps working — reminder scheduling itself does not depend on the grant (`FocusReminders.kt:32-40`), only the visible notification does.
- Alarms: `AlarmManager.setAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP, …)` (`FocusReminders.kt:39`) — **inexact by design**, comment at line 38. No `USE_EXACT_ALARM`, no `SCHEDULE_EXACT_ALARM`, no `USE_FULL_SCREEN_INTENT` in manifest.
- No `WorkManager`, no foreground services, no boot receiver.

## §1.8 Third-party assets

| Asset | License / source | Evidence |
|---|---|---|
| Poppins fonts ×3 (`res/font/poppins_{light,regular,medium}.ttf`) | SIL OFL 1.1 text present in repo: `StudentLauncher/app/OFL-POPPINS.txt` (tracked, `git ls-files`). **Not packaged in the APK and not credited inside the app** | `git ls-files`; `res/font/` listing |
| `res/raw/onboarding_ad.mp4` (2.4 MB, 10 s, 1280×720 h264 + AAC) | Frame-inspected `ffmpeg -vf fps=1` → 10 frames. AI-generated promo: shows a **Google "G" search-widget logo** and a **YouTube logo** on the simulated home screen (frames 4 and 1), a visible **AI-generator sparkle watermark** (bottom-right of most frames), and garbled pseudo-UI text that is not this app's UI ("Speck-time tracker", "Creatre lavager", "Snwte to taacks", "Total Expeniser", "Toalsday"). Final frame: "Student Launcher / by thats.jainam." (frame 10) | frames extracted to temp dir; verdict + policy cite in report §2.4 |
| Preset wallpapers (`dune`, `mist`, `meadow`, `plain`) | Procedurally drawn in code (color blobs), no bitmap assets | `ui/Wallpapers.kt:34,41` |
| App icon / launcher drawable | Vector XML in repo | `res/drawable/ic_launcher_foreground.xml`, `res/mipmap-anydpi-v26/ic_launcher.xml` |
| Store graphics `store/icon-512.png`, `store/feature-graphic-1024x500.png` | Tracked since `e2686f6 "v2.0 - Initial release"`; **origin/license unknown** → ❓ | `git log --diff-filter=A -- StudentLauncher/store` |
| `app/build.gradle.kts` dependencies | AndroidX/Compose/JUnit only — no third-party SDK | `app/build.gradle.kts:56-68` |

## §1.9 Store-facing text and in-app claims

| Source | Claims found (verbatim) |
|---|---|
| `docs/PLAY_STORE.md:23-51` | name "Student Launcher"; short desc "A calm, distraction-free launcher for students with widgets and focus tools."; full description incl. "Study mode **hides** the apps you mark as distracting", "A short pause before a distracting app opens", "The app never watches what you do and needs no special access", "Private by design: no accounts, no ads, no tracking. Everything stays on your phone." |
| `docs/PLAY_STORE.md:53-58` | Data safety draft answers (see report — one needs rewording) |
| `docs/PLAY_STORE.md:60-68` | IARC note, target-audience note "choose 13 and over…", policy URL |
| `README.md:1-42` | "Student Launcher 3.0"; "no third-party libraries"; honest notes on the 489-cell glyph matrix, the seven icon packs, the optional torch reactions and the Tier 0 call signals ("No accessibility, usage-access or overlay permission"; the only permission is POST_NOTIFICATIONS) |
| In-app About (`ui/WidgetSheets.kt:309-313`) | "Developed by thats.jainam", "A calm launcher for students. Everything stays on your phone: no accounts, no ads, no tracking." |
| In-app Settings (`ui/FirstRun.kt:347-358`) | "Plan, widget, and launcher preferences stay on this device. No account or analytics required." + Privacy Policy button + About button |
| Onboarding (`ui/Onboarding.kt:138,161,191,204`) | "A calmer home screen, built for studying." / "Pick the apps you want Mindful Mode to **watch**" / "Set a daily limit" / "With a PIN set, opening Focus settings … needs it too" |
| Focus wording (`ui/FirstRun.kt:285-291`) | "Study mode — **Hide** apps marked distracting", "Escalating pause — Delay grows each time you open a distracting app", "Session tracking — Track time in distracting apps" |
| Widget catalog (`data/WidgetModels.kt:11-70`) | 29 type constants, **28 catalog entries** (incl. "Monitor", "Quotes & facts", "Photos") — no numeric "20+ widgets" claim exists anywhere in store text |

No medical/therapeutic/diagnostic wording anywhere: `rg -i 'medical|therap|diagnos|cure|anxiety|depression|clinically|ADHD|mental health' app/src docs README` → 0 relevant matches.

## §1.10 Secrets / keystore hygiene

- `git ls-files | Select-String 'jks|keystore|properties'` → only `gradle.properties`, `gradle-wrapper.properties`, `keystore.properties.example`. **No keystore or secrets tracked.**
- `git log --all --diff-filter=A -- '*.jks' '*.keystore' keystore.properties` → empty (never committed).
- `.gitignore` (root + `StudentLauncher/`) both ignore `keystore.properties`, `*.jks`, `*.keystore`, `local.properties`.
- Local `upload.jks` + `keystore.properties` exist on disk only (unsigned/untracked).
