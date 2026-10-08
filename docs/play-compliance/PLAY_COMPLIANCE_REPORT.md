# PLAY_COMPLIANCE_REPORT — StudentLauncher v3.0

Audit date: **2026-10-08** · tree: local `master` @ `9c7e034` (+ this pass) · `versionCode 7` / `versionName 3.0`
Verdicts: ✅ compliant with evidence · ⚠️ issue (fixed in-repo where marked, otherwise listed as a recommendation) · ❓ cannot be verified from the repo (needs Play Console, a device, or the developer).
Severity: **BLOCKER** = rejection/suspension risk · **HIGH** = likely review friction or user harm · **LOW** = polish.

**Policy text fetched for this audit (2026-10-08, not from memory):**

| Topic | URL used | Note |
|---|---|---|
| Data safety form (incl. "collect" definition) | https://support.google.com/googleplay/android-developer/answer/10787469 | fetched OK |
| App content / privacy policy field | https://support.google.com/googleplay/android-developer/answer/9859455 | **moved** — now "Prepare your app for review"; still contains the in-listing + in-app privacy-link requirement. Discrepancy with the prompt noted here per §7 |
| User data policy | https://play.google.com/about/privacy-security/user-data/ (→ support answer/10144311) | the prompt's `answer/9859455` no longer serves this text |
| Permissions & sensitive APIs | https://support.google.com/googleplay/android-developer/answer/9888170 | fetched OK |
| Target API level | https://support.google.com/googleplay/android-developer/answer/11926878 | fetched OK |
| Families / target audience | https://support.google.com/googleplay/android-developer/answer/9893335 | fetched OK |
| Health apps & health content | https://support.google.com/googleplay/android-developer/answer/16679511 | old `answer/9231432` is 404 |
| Deceptive behavior | https://play.google.com/about/privacy-security-deception/deceptive-behavior/ (support answer/17006354) | fetched OK |
| Metadata / store listing | https://support.google.com/googleplay/android-developer/answer/9898842 | fetched OK |
| Impersonation | https://support.google.com/googleplay/android-developer/answer/9888374 | fetched OK |
| 16 KB page size | https://developer.android.com/guide/practices/page-sizes | fetched OK |

---

## 2.1 Privacy policy (GitHub Pages)

| Item | Verdict | Evidence | Policy section cited | Severity |
|---|---|---|---|---|
| Public HTTPS URL, 200, no login/geofence/redirect loop | ✅ | `curl.exe -sIL https://jainam662010-boop.github.io/StudentLauncher/privacy.html` → `HTTP/1.1 200 OK`, `Content-Type: text/html`, single hop (no redirects), GitHub Pages. **Final URL = the URL above (no trailing redirect)** | User Data policy: "active URL, applies to your app, specifically covers user privacy" | — |
| Not a PDF/editable doc, not blocked by `noindex`/robots | ✅ | served file is HTML (`Content-Type: text/html`); fetched body contains no `noindex`/`robots` meta; repo file `privacy.html` has neither | User Data policy (same clause) | — |
| Names the app + developer + a working contact email | ✅ (⚠️ before fix) | was ⚠️: no email anywhere (GitHub link only, old `privacy.html:71`). Fixed: root `privacy.html` now names *Student Launcher*, package `com.studentlauncher`, developer *thats.jainam*, email **ruplaikarnawat01@gmail.com** + Instagram @thats.jainam.dev | User Data policy: privacy policy must comprehensively disclose how user data is handled; App content: policy "applies to your app" | — |
| …and the email matches Play Console developer contact | ❓ | Console-only | App content → developer contact | LOW (Console) |
| Discloses every data family in §1.4 (PIN, tasks/events, widget data incl. expenses/GPA/habits/water/flashcards, photos, usage minutes, installed apps, settings) | ✅ (⚠️ before fix) | was ⚠️: old policy listed only "settings, widget layout, tasks, pinned apps, marked apps, wallpaper" — missing PIN, plan events, expense/GPA/habit/water/flashcard content, usage stats. Fixed: `privacy.html` "What the app stores on your device" + "How the usage numbers are produced"; source = `FACT_SHEET.md` §1.4 | User Data policy: disclose "how your app accesses, collects, uses, and shares user data, not limited by the Data safety section" | — |
| States data stays on device and is **not** transmitted + what clearing/uninstall does | ✅ | `privacy.html` "Short version", "Retention and deletion"; source proof: no `INTERNET` permission (merged manifest) + zero network code (`FACT_SHEET.md` §1.3) | User Data policy (accuracy) | — |
| Retention & deletion covered, honestly including absence of in-app deletion | ✅ policy / ⚠️ product | `privacy.html` states Clear storage / uninstall are the complete deletion path and says there is no in-app "delete my data" button. **Recommendation** (not implemented — behaviour change): a "Reset all data" button in Settings (~10 lines calling `prefs.edit().clear()` + deleting `filesDir/wallpaper*.jpg`), because Play's deletion question is about giving users control | Data safety help: deletion-request mechanism (only asked when data is collected) | LOW |
| States the permission actually requested + why | ✅ | `privacy.html` "Permissions we request" ↔ `AndroidManifest.xml:5` ↔ `MainActivity.kt:57-61` | Permissions & APIs: in-context incremental requests | — |
| Children/minors consistent with target-audience decision | ✅ | `privacy.html` "Children and young users": no name/age/email/contacts/location, no ads, no IAP, no UGC sharing, no analytics — true under either Console selection (verified: no such collection anywhere, `FACT_SHEET.md` §1.4) | Families policy (9893335) | — |
| Effective date bumped + how changes are announced | ✅ (⚠️ before fix) | old: "September 20, 2026"; now **October 8, 2026** + a "Changes to this policy" section (release notes in the Play listing + in app) | User Data: keep policy & Data safety up to date | — |
| Same URL in Play Console **and** inside the app | ✅ in-app / ❓ Console | in-app: `ui/FirstRun.kt:354-355` (Settings → Privacy & About → Privacy Policy) and `ui/WidgetSheets.kt:317-318` (About → Privacy Policy), both opening exactly `https://jainam662010-boop.github.io/StudentLauncher/privacy.html` | App content: "You must link to a privacy policy on your app's store listing page and within your app" | Console part: MEDIUM |
| Only one copy of the policy in the repo | ✅ (⚠️ before fix) | was ⚠️: three copies (root `privacy.html`, `StudentLauncher/docs/privacy.html` identical, `StudentLauncher/docs/PRIVACY_POLICY.md` markdown). Fixed: both `docs/` copies deleted (`git rm`), `PLAY_STORE.md` reference corrected to the repo root file | User Data: single authoritative policy | — |
| GitHub Pages serves the intended branch/folder and latest commit | ⚠️ | Pages config verified: `gh api repos/jainam662010-boop/StudentLauncher/pages` → `{"status":"built","build_type":"legacy","source":{"branch":"master","path":"/"},"https_enforced":true}` — correct. **But** local `master` is **9 commits ahead of `origin/master`**, and the live page still serves the old text (`Content-Length: 4027` = old file; `Last-Modified: Fri, 25 Sep 2026`). New text goes live only after push (§4) | User Data: policy must be available | MEDIUM (resolved by push) |
| Mobile-readable, `<title>` names the app, plain language | ✅ | `privacy.html` has `<meta name="viewport">`, `<title>Privacy Policy – Student Launcher</title>`, a small-screen media query, short plain sentences, no external font/script/tracker | User Data: readable, app-specific | — |

## 2.2 Data safety form (Console) — drafted in `DATA_SAFETY_ANSWERS.md`

| Item | Verdict | Evidence | Policy section cited | Severity |
|---|---|---|---|---|
| "Collects or shares any required user data types?" → **No** | ✅ | quote used: *"'Collect' means transmitting data from your app off a user's device"* + *"On-device access/processing … does not need to be disclosed"* (answer/10787469); app has no `INTERNET` permission and no network code | Data safety help, "Data collection" | — |
| On-device-only data types correctly *not* declared, each mapped to source | ✅ | consistency table in `DATA_SAFETY_ANSWERS.md` (8 rows) | Data safety help | — |
| Security practices answers | ✅ | encryption-in-transit / deletion questions unreachable when Q1=No; independent security review = No; Families badge conditional on §2.6 | Data safety help, "Other app and data disclosures" | — |
| Consistency: answer ↔ policy sentence ↔ source, no mismatch | ✅ | 8/8 rows match after the `privacy.html` rewrite | User Data: Data safety "must be consistent with … the app's privacy policy" | — |
| Ads declaration (App content → Ads) = **No** | ✅ (Console entry) | no ad SDK in `app/build.gradle.kts:56-68`; dex shows no ad packages; `onboarding_ad.mp4` is a self-made promo, not an ad network placement (but see 2.4) | App content → Ads | Console |

## 2.3 Permissions and sensitive APIs

| Item | Verdict | Evidence | Policy section cited | Severity |
|---|---|---|---|---|
| Declared permissions == what the policy + Data safety say | ✅ | `aapt2 dump permissions app-release.apk` → `POST_NOTIFICATIONS` + `com.studentlauncher.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (androidx-internal, not user-facing); matches `privacy.html` and `DATA_SAFETY_ANSWERS.md` | Permissions & APIs (9888170) | — |
| `QUERY_ALL_PACKAGES` absent (launcher exception not needed) | ✅ | absent from source + merged manifest; app list comes from `<queries>` `MAIN/LAUNCHER` (`AndroidManifest.xml:7-11`). Policy: "You may not use QUERY_ALL_PACKAGES if your app can operate with a more targeted scoped package visibility declaration" → no declaration form needed | Package (App) Visibility Permission, 9888170 | — |
| `PACKAGE_USAGE_STATS` absent | ✅ | `rg 'UsageStatsManager\|PACKAGE_USAGE_STATS' app/src` → 0; not in manifest. Usage minutes are self-tracked (`LauncherViewModel.kt:307-333`) | Permissions & APIs / User data | — |
| Accessibility service absent | ✅ | no `<service>`, no `AccessibilityService` in source or merged manifest | Accessibility API section | — |
| `SYSTEM_ALERT_WINDOW` absent | ✅ | not in manifest (merged) | Permissions & APIs | — |
| Exact alarms absent | ✅ | no `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM`; reminders use `setAndAllowWhileIdle` (inexact) `FocusReminders.kt:39` | Exact Alarm Permission section | — |
| `READ_MEDIA_*` absent (photo picker used) | ✅ | manifest has none; `ActivityResultContracts.PickVisualMedia` (`MainActivity.kt:43-47`) | Photo & Video Permissions: "may only request READ_MEDIA_* if system pickers … are not sufficient" — we don't request them | — |
| `SET_WALLPAPER`, `BIND_APPWIDGET` absent | ✅ | manifest; widget binding via system dialog (`MainActivity.kt:90-102`) | Permissions & APIs | — |
| `USE_FULL_SCREEN_INTENT`, `REQUEST_INSTALL_PACKAGES`, `MANAGE_EXTERNAL_STORAGE`, `READ_CONTACTS`, location, SMS/call-log | ✅ absent | `aapt2 dump permissions` complete list above | Permissions & APIs | — |
| Runtime request is in-context; denial degrades, doesn't break | ✅ | requested only from Settings → Focus → reminder interval / "Allow reminders" (`ui/FirstRun.kt:313-318` → `MainActivity.kt:57-61`); denial → toast only (`MainActivity.kt:49-51`); no auto-request on launch | Permissions: "Request … incrementally … in context"; "Respect users' decisions if they decline" | — |
| No hidden permissions merged from libraries | ✅ | release merged manifest dump (§1.1) — only the two rows | Permissions & APIs | — |
| Backup rules deliberate and policy-consistent | ✅ (⚠️ before fix) | was ⚠️ (lint `DataExtractionRules`: `allowBackup` deprecated on 12+, add `dataExtractionRules`). Fixed: `res/xml/data_extraction_rules.xml` excludes `sharedpref` + `file` from cloud backup **and** device transfer, wired at `AndroidManifest.xml:32-33`; lint warning gone; `privacy.html` "Security" matches | Android 12 backup rules; User Data accuracy | — |

## 2.4 Launcher / core functionality

| Item | Verdict | Evidence | Policy section cited | Severity |
|---|---|---|---|---|
| HOME handling standard; user is never trapped | ✅ | `AndroidManifest.xml:48-53` standard `MAIN/HOME/DEFAULT/LAUNCHER`; `launchMode="singleTask"`; `LauncherViewModel.back()` (`:240-258`) only closes the top overlay and does nothing on the home layer (no interception of Home/Recents); default-home switch offered via `RoleManager.ROLE_HOME` / default-apps settings (`ui/Onboarding.kt:232-239`) | Deceptive Behavior: no dark patterns; functionality must work as described | — |
| PIN / study / mindful "doors" restrict only this launcher's surfaces | ✅ | PIN gate is `requirePin { … }` on **in-app** actions only (`LauncherViewModel.kt:608`); copy says "hides", "watch", "delay grows" (`FirstRun.kt:285-291`, `Onboarding.kt:161`); listing explicitly says it is *not* an app blocker (`PLAY_STORE.md` Policy notes: "It hides apps inside the launcher and sends reminders, it does not block other launchers or apps") | Deceptive Behavior: "Apps must provide an accurate disclosure, description and images/video of their functionality" | — |
| Forgot-PIN cooling-off has a real exit; user can always change default launcher | ✅ | 24 h cool-down (`PinPolicy.kt:14`), "Cancel reset" always available (`ui/PinSheet.kt:157`), PIN sheet dismissible by Back/close (`LauncherViewModel.kt:250`, `PinSheet.kt:116,160`); default-home change is a **system** setting, reachable regardless of the PIN | Deceptive Behavior (no entrapment) | — |
| No interference with other apps / system UI / settings outside launcher scope | ✅ | no `SYSTEM_ALERT_WINDOW`, no accessibility, no settings write; only `startActivity` for launch intents (`LauncherViewModel.kt:307-316`) and widget bind/config flows (`MainActivity.kt:90-121`) | Device and Network Abuse / Malware (Elevated Privilege Abuse) | — |
| No ads / ad SDKs / affiliate or tracking links | ✅ | deps = AndroidX+Compose+JUnit (`app/build.gradle.kts:56-68`); dex has no ad network packages; the only outbound links are the privacy URL opened in the user's browser (`FirstRun.kt:355`, `WidgetSheets.kt:318`) | Ads policy; App content → Ads = No | — |
| `onboarding_ad.mp4` verdict | ⚠️ **replace** | 10 frames extracted (`ffmpeg -vf fps=1`): a simulated home screen carrying a **Google "G" logo** and a **YouTube logo** (frames 1 and 4), a visible **AI-generator sparkle watermark** (bottom right of most frames), and pseudo-UI text that does not exist in this build ("Speck-time tracker", "Creatre lavager", "Total Expeniser", "Toalsday"). Only the last frame ("Student Launcher / by thats.jainam.", frame 10) is accurate. **Not implemented here** — removing the onboarding video step is a product/behaviour change. Smallest change: delete `STEP_VIDEO` handling in `ui/Onboarding.kt:134-155` (or re-record a real screen capture and overwrite `res/raw/onboarding_ad.mp4`) | Impersonation: "Be careful not to use … in-app elements that could mislead users about your app's relationship to someone else or another app"; Metadata: "Provide screenshots and videos that reflect your app's actual experience"; IP: brand logos without permission | **HIGH** |

## 2.5 Health and wellbeing claims

| Item | Verdict | Evidence | Policy section cited | Severity |
|---|---|---|---|---|
| No medical / therapeutic / diagnostic claims in app, README, store text or policy | ✅ | `rg -i 'medical\|therap\|diagnos\|cure\|anxiety\|depression\|clinically\|ADHD\|mental health' app/src docs README` → only `SecureRandom` false positives. Copy is behavioural: "watch", "daily limit", "pause", "focus" | Health apps policy scope test | — |
| App scope under the Health policy | ⚠️ (Console) | fetched text: *"If your app offers health-related features or information as part of its functionality … it must comply"* and *"All developers must complete the Health apps declaration form on the App content page"*. This app ships mindfuness/breathe/focus/usage-budget/hydration/habit features → **assume in scope, complete the declaration** (exact answers in Console actions). If Play does *not* show the declaration, no action needed | Health apps and health content (16679511) | **HIGH** (Console) |
| Neutral wellbeing disclaimer present | ✅ (added) | `privacy.html` "Wellbeing features": self-help/productivity only, not medical/therapeutic/diagnostic, not medical advice. **Recommendation:** mirror one line at the end of the full store description | Health apps policy (disclaimer recommended when wellbeing language appears) | LOW |
| Optional exact question for the developer if unsure | ❓ | *"Does Play Console → App content show a 'Health apps' declaration for com.studentlauncher?"* — only the Console screen answers this | 16679511 | — |

## 2.6 Target audience, minors, Families policy

| Item | Verdict | Evidence | Policy section cited | Severity |
|---|---|---|---|---|
| What each target-age selection triggers | ✅ (documented) | fetched: *"you must indicate the target audience … by selecting from the list of age groups"*; *"If one of the target audiences for your app is children, you must comply with the following requirements"*; *"apps designed for … choose the age group that best represents that school level"*; and imagery/terminology that targets children can change Google's assessment of your declaration. Families badge in Data safety is opt-in from the Security-practices section | Families policy (9893335) + Data safety help | — |
| Recommended selection + consequences | ✅ written (`PLAY_STORE.md` → Content rating and audience) | **Recommended: 13-15, 16-18, 19-21, 22+** (exact Console labels to be confirmed on screen ❓) — students who are teens and up, children under 13 excluded → Families policy not triggered. Consequences spelled out: the app already has **no ads, no third-party SDKs and nothing collected**, so even the children-included path is low-effort, but it brings the full Families rule set, child-appropriate listing imagery, and the optional "Committed to play Families Policy" badge. Final call is the developer's | 9893335 | Console MEDIUM |
| No age / name / contact / location collected from anyone | ✅ | no input of any of these in source (`rg 'age\|birth\|email\|contact'` finds none); manifest has no location/contacts/calendar permissions (§2.3); no form, no account | User Data; Families data rules (irrelevant because nothing is collected) | — |
| Listing/app text does not appeal to children inconsistently with the declared audience | ✅ | description is functional/neutral ("for students", "built for studying"), no cartoon mascots, no "for kids" wording, no game-like rewards tied to children's content (`PLAY_STORE.md:30-53`, `ui/Onboarding.kt`) | Families: "if you choose to include imagery and terminology … that could be considered targeting children" | — |

## 2.7 Store listing and metadata

| Item | Verdict | Evidence | Policy section cited | Severity |
|---|---|---|---|---|
| No unverifiable superlatives / keyword stuffing / price text / emoji spam | ✅ | `PLAY_STORE.md:23-53` full text reviewed line by line: no "best", "No. 1", "guarantee", "100%", no competitor app name in the title, no emoji | Metadata (9898842): "We don't allow apps with misleading, improperly formatted, non-descriptive, irrelevant, excessive… metadata" | — |
| Privacy/performance claims it cannot prove | ✅ | "no accounts, no ads, no tracking. Everything stays on your phone" — provable (no INTERNET permission, no SDK) | Metadata: "accurately describe its functionality" | — |
| Competitor/brand names in the description | ✅ (⚠️ before fix) | was ⚠️: "Niagara-style list" and "Nothing-inspired dot-matrix details" (`PLAY_STORE.md:33-34`). Fixed → "a clean list or grid" / "and dot-matrix details". (In-app settings text still says "Niagara letter pages" — `FirstRun.kt:337`; a descriptive comparison, not an affiliation claim → acceptable, no action) | Metadata: "Don't use a celebrity's name or a brand's logo without permission"; Impersonation | LOW (fixed) |
| Every feature named in the listing exists in the build | ✅ | verified individually: list/grid + A–Z scrubber, glass/spring motion, light/dark, wallpaper blur/dim/zoom, **mono or original icons** (`AppRepository.kt:38-46`, `AppViews.kt:113-118`), **icon shapes** (`FirstRun.kt:179-180`), widgets clock/date/week/battery/next/countdown/timer/tasks/quick/sphere/glyph (**25×25** `WidgetModels.kt:52`) + "Regular Android widgets" (`WidgetHostManager.kt`), study-mode hide (`FirstRun.kt:285`), pause (`FirstRun.kt:305-309`), 5/10/15-min reminders (`FirstRun.kt:311-316`). No numeric widget count is claimed (catalog = 28 entries / 29 type constants) | Metadata: accurate description | — |
| Screenshots in repo match shipped UI, no third-party marks/personal data | ❓ | **none in repo** — only `store/icon-512.png` and `store/feature-graphic-1024x500.png`; `PLAY_STORE.md` tells the developer to capture ≥4 from a real device. Must be done before submission | Metadata: "Provide screenshots and videos that reflect your app's actual experience" | MEDIUM (Console) |
| Contact details real | ✅ | email `ruplaikarnawat01@gmail.com` + Instagram @thats.jainam.dev (developer-provided, 2026-10-08) in `privacy.html`; GitHub repo `jainam662010-boop/StudentLauncher` exists | Metadata / User Data | — |
| Feature graphic & icon provenance | ❓ | both added in `e2686f6 "v2.0 - Initial release"`, no licence recorded → see `ASSETS.md` | IP policy | MEDIUM |

## 2.8 Technical requirements

| Item | Verdict | Evidence | Policy section cited | Severity |
|---|---|---|---|---|
| `targetSdk` meets current requirement | ✅ | `targetSdk = 36` (`app/build.gradle.kts:21`); fetched requirement: "Starting August 31, 2026: New apps and app updates must target Android 16 (API level 36) or higher" | Target API level requirements (11926878) | — |
| AAB uploaded + signed; Play App Signing | ✅ build / ❓ Console | `bundleRelease` output exists (`app/build/outputs/bundle/release/app-release.aab`, 3,250,724 B); `signingConfigs.release` loaded from `keystore.properties` (`app/build.gradle.kts:26-44`). Play App Signing enrolment is a Console fact | Release requirements | Console |
| Keystore / secrets never in git | ✅ | `git ls-files` → only `keystore.properties.example`; `git log --all --diff-filter=A -- '*.jks' '*.keystore' keystore.properties` → empty; both `.gitignore` files exclude `keystore.properties`, `*.jks`, `*.keystore`, `local.properties` | Security / release hygiene | — |
| `versionCode` > live value | ❓ | repo has **7** (`app/build.gradle.kts:22`); live value only in Play Console | Version codes | Console |
| 64-bit + 16 KB alignment | ✅ | `arm64-v8a` + `x86_64` present; `zipalign -c -P 16 -v 4 app-release.apk` → `Verification successful`; requirement text fetched from developer.android.com/guide/practices/page-sizes | 64-bit & 16 KB page-size compatibility requirements | — |
| Lint + tests clean | ✅ | `./gradlew testDebugUnitTest lintDebug assembleDebug` → exit 0; `0 errors, 53 warnings` (all `UseKtx`/`DefaultLocale`/`GradleDependency`/`UnusedAttribute` style — none functional). Re-run green **after** the manifest + backup-rules change | Release quality | — |
| No logging of PINs/personal data | ✅ | `rg 'Log\.|println' app/src/main` → 0 matches | User Data | — |
| No test/debug flags, hard-coded dev URLs, debuggable release | ✅ | `rg 'BuildConfig\|DEBUG' app/src` → 0; no dev URLs (only `example.com` used as an intent-resolution probe, `AppRepository.kt:67`); `android:debuggable` absent from the release merged manifest | Malware / debug hygiene | — |
| Backup behaviour decided deliberately + policy matches | ✅ (⚠️ before fix) | `allowBackup="false"` + new `data_extraction_rules.xml` (no cloud backup, no device transfer) ↔ `privacy.html` "Security" paragraph | User Data accuracy | — |

## 2.9 Intellectual property and licenses

| Item | Verdict | Evidence | Policy section cited | Severity |
|---|---|---|---|---|
| Poppins (SIL OFL) license shipped/credited as the license requires | ⚠️ | license text exists in repo (`StudentLauncher/app/OFL-POPPINS.txt`, tracked) but **not in the APK and not credited in-app**; OFL requires the license to accompany the font software. Smallest fix: add "Poppins © The Poppins Project Authors — SIL Open Font License 1.1" + link to `OFL-POPPINS.txt` in the About sheet, or ship the text under `res/raw/`. Not implemented (adds in-app UI) | IP / license compliance (repo-level; Play does not block, but it is a licence obligation) | LOW |
| Bundled wallpapers/icons/sounds/video documented; unlicensed removed | ⚠️ | documented in `ASSETS.md`: wallpapers are procedural (first-party), launcher icon is vector (first-party), fonts = OFL (OK), **store PNGs unknown ❓**, **`onboarding_ad.mp4` unlicensed/unknown + shows Google/YouTube marks ⚠️** (see 2.4) | IP policy | HIGH for the video, MEDIUM for store PNGs |
| No other apps' logos/names beyond what `PackageManager` provides | ⚠️ (see 2.4) | normal use of app labels/icons from `PackageManager` is fine; the only third-party marks found are **inside the onboarding video** (Google G, YouTube) | Impersonation (9888374) | HIGH |
| AndroidX/Compose open-source notices | ✅ recommendation only | no Play requirement; customary is an OSS-licenses screen or a repo file listing Apache-2.0 components. Not implemented (would add UI — deliberately not over-engineered) | — | LOW |

## 2.10 Content rating and app access (Console answers prepared)

| Item | Verdict | Evidence | Policy section cited | Severity |
|---|---|---|---|---|
| IARC questionnaire answers drafted | ✅ (draft) | based on verified facts: no violence/weapons/crude humour/sexual content/user-generated sharing/chat/location/payments/subscriptions/ads/gambling/personal-info prompts; single-user app, offline. Exact wording below | Content ratings | Console |
| App access declaration | ✅ (draft) | no login, no account, no special access — all functionality available to a reviewer without credentials | App content → App access | Console |
| Play App Signing / live versionCode / live policy URL state | ❓ | Console-only screens | Release requirements | Console |

---

## Ranked list of all ⚠️ and ❓

1. **⚠️ HIGH — `res/raw/onboarding_ad.mp4`**: AI-generated promo with Google/YouTube logos, an AI watermark and fake UI text. Replace with a real screen recording or delete the video onboarding step (`ui/Onboarding.kt:134-155`). *Impersonation / Metadata accuracy.*
2. **⚠️ HIGH (Console) — Health apps declaration**: fetched policy says all developers with health-related features must complete it; complete it with the honest answers below. *Health apps policy.*
3. **⚠️ MEDIUM — live privacy policy is still the old text** until `master` is pushed (local is 9 commits ahead; live `Content-Length: 4027`, `Last-Modified: 25 Sep 2026`). Push steps in §4.
4. **❓ MEDIUM — Play Console state** not verifiable from the repo: privacy-URL field, Data safety form, target-audience selection, content rating, Ads declaration, App access, Play App Signing, live `versionCode` vs 7, developer-contact email matching `ruplaikarnawat01@gmail.com`.
5. **❓ MEDIUM — store graphics** (`store/icon-512.png`, `store/feature-graphic-1024x500.png`) provenance unknown; confirm self-made or replace. **Screenshots** must still be captured from the shipped UI.
6. **⚠️ LOW — Poppins OFL** not credited in-app / not shipped with the font files.
7. **⚠️ LOW — no in-app "delete all data" button**; policy is honest about it (Clear storage / uninstall). Add only if the developer wants it.
8. **⚠️ LOW — wellbeing disclaimer** exists in the policy but not in the store description; mirror one line there.
9. **❓ LOW — exact Console labels** for the recommended age groups (13-15, 16-18, 19-21, 22+) must be read off the screen.

**Everything else in §2 is ✅ with evidence. No BLOCKER remains in the repo.**

## Console actions (do these in Play Console — nothing below is done by this pass)

1. **Privacy policy** (App content → Privacy policy), enter exactly:
   `https://jainam662010-boop.github.io/StudentLauncher/privacy.html`
   (do **not** add a `.html` variant, query string or `http://`). Also keep it as the listing's public privacy URL.
2. **Data safety** (App content → Data safety): answer **No** to "Does your app collect or share any of the required user data types?", leave Data types empty, security practices: independent security review **No**, Families badge **No** (unless you choose a children-including target audience). Full sheet: `docs/play-compliance/DATA_SAFETY_ANSWERS.md`.
3. **Ads** (App content → Ads): **No**.
4. **Target audience and content**: select **13-15, 16-18, 19-21, 22+** (read the exact labels on screen). Rationale in `PLAY_STORE.md`. If you instead include children, complete Families compliance and consider the Families badge.
5. **Health apps declaration** (App content → Health apps, if shown): "Does your app offer health-related features?" **Yes** (mindfulness/focus/self-tracking); "Is it a medical device / does it claim to diagnose, treat or cure?" **No**; "Does it access health data (sensors, Health Connect)?" **No**; "Does it give medical advice?" **No**.
6. **Permissions declaration**: none required — `POST_NOTIFICATIONS` is a normal runtime permission and `QUERY_ALL_PACKAGES` is not requested.
7. **Content rating (IARC)**: no violence, no sexual content, no crude humour, no user-generated content, no sharing/communication between users, no location, no purchases/in-app purchases/subscriptions, no ads, no personal information prompts, no gambling, no unrestricted internet (the app has no internet access).
8. **App access**: "No login or special access is required — all features are available on first launch."
9. **Play App Signing**: confirm enrolment, and confirm the upload AAB is `app/build/outputs/bundle/release/app-release.aab`.
10. **Version code**: confirm live versionCode < **7** before uploading.
11. **Developer contact email**: set to `ruplaikarnawat01@gmail.com` so it matches the privacy policy.
12. **Store listing**: paste the short/full descriptions from `PLAY_STORE.md`; capture 4+ screenshots from a real device; confirm the icon/feature graphic are self-made.

## §4 — Verifying the live policy page

- The live URL currently serves the **old** policy (`Last-Modified: Fri, 25 Sep 2026`, `Content-Length: 4027`). It will **not** show the new text until you push (this pass did not push).
- Push + refresh:
  ```
  git push origin master
  ```
  (GitHub Pages is `legacy`, source `master` / `/`, status `built` → Pages rebuilds automatically within ~1 min; re-run a build from the repo's Actions/Pages tab if it doesn't.)
- Post-push checks (run on your machine):
  ```
  curl.exe -sIL https://jainam662010-boop.github.io/StudentLauncher/privacy.html     # expect: HTTP/1.1 200 OK
  curl.exe -s https://jainam662010-boop.github.io/StudentLauncher/privacy.html | Select-String "October 8, 2026"   # expect: the match
  ```
- **Exact string to paste into Play Console → App content → Privacy policy:**
  `https://jainam662010-boop.github.io/StudentLauncher/privacy.html`

## §6 — Git

- Commit 1: `docs: play compliance fact sheet and report` (docs/play-compliance only)
- Commit 2: `fix: privacy policy and play-policy findings` (privacy.html, manifest + backup rules, docs deletions, store copy, README)
- Nothing tagged, **nothing pushed**. Push with:
  ```
  git push origin master
  ```

## Readiness statement

Verified from source and build outputs: the app declares exactly one user-facing permission, has no network stack at all (so the "no data leaves the device" claim in both the policy and the Data safety form is provable), stores everything in app-private preferences with a salted PBKDF2 PIN hash, backs nothing up to the cloud (now also explicitly excluded by `dataExtractionRules`), targets the required API 36, ships a 64-bit, 16 KB-aligned artifact, logs nothing, contains no ad/analytics SDK, and its store copy now describes only features that exist; the privacy policy was rewritten so that every sentence about data is backed by a `file:line` citation, dated October 8, 2026, and is reachable at the same single URL that the app itself links to. What remains is **not** verifiable from a repository: the Play Console declarations (privacy-URL field, Data safety, target audience, content rating, Health-apps declaration, Ads, App access, Play App Signing, live versionCode, developer-contact email), the replacement of the onboarding video, capture of real screenshots, confirmation of the store graphics' provenance, and the push that makes the new policy live — each is listed above with exact wording or steps. Whether the app is ultimately approved is Google's decision, and Google's own guidance states that Google Play reviews apps across all policy requirements while the developer alone is responsible for the accuracy of the declarations; this report can show the repo is consistent and evidence-backed, not guarantee an outcome.
