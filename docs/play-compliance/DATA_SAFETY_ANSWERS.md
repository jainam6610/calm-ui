# DATA_SAFETY_ANSWERS — Play Console → App content → Data safety

Prepared 2026-10-08 from source evidence (see `FACT_SHEET.md`). This is a **Console action sheet** — the answers must be entered by the developer in Play Console.

## Definition relied on

> "**'Collect' means transmitting data from your app off a user's device.**"
>
> "Not in scope for data collection — **On-device access/processing: User data accessed by your app that is only processed locally on the user's device and not sent off device does not need to be disclosed.**"

Source: *Provide information for Google Play's Data safety section*, https://support.google.com/googleplay/android-developer/answer/10787469 (fetched 2026-10-08).

Why it applies here: the app declares `INTERNET` only for the optional browser inside the ported Calm screen (FACT_SHEET §1.3). Requests go directly from the device to the site the user chose; the developer runs no server, and there is no analytics, crash reporting or ad SDK. Everything the app itself creates stays on the device, so the answer remains **No**.

## Section-by-section answers

### 1. Data collection and security

| Question | Answer |
|---|---|
| **Does your app collect or share any of the required user data types?** | **No** |

Consequence: per the form flow ("If you selected Yes, confirm the following…"), the *encryption in transit* and *deletion request* questions are only asked when the answer is Yes. They are therefore **not reached**. If the form nevertheless shows them, use:

| Question | Answer if shown | Why |
|---|---|---|
| Is all collected data encrypted in transit? | Not applicable — the developer receives no data | browser traffic goes directly to the chosen site (FACT_SHEET §1.3) |
| Do you provide a way for users to request deletion of their data? | Users delete everything themselves: Settings → Apps → Student Launcher → Storage → Clear storage, or uninstall | privacy.html "Retention and deletion" |

Do **not** tick a claim the app cannot honour (e.g. an in-app deletion-request form) — none exists.

### 2. Data types

Select **no data types**. Nothing is collected or shared.

Processed on device only, therefore not declared (each with its source):

| Data type that *looks* relevant | Why it is not declared |
|---|---|
| Installed apps | read via `<queries>` `MAIN/LAUNCHER` for the home screen/app list; never transmitted (FACT_SHEET §1.5, §1.3) |
| Photos | obtained from the system photo picker, re-encoded into app-private storage; never transmitted (§1.4) |
| App interactions / "other actions" (focus-session minutes) | self-tracked in local prefs (`LauncherViewModel.kt:307-333`); never transmitted |
| Other user-generated content (tasks, events, expenses, GPA, habits, water, flashcards) | local `SharedPreferences` only (§1.4); never transmitted |
| Other info (PIN) | salted PBKDF2 **hash** in local prefs (`PinLock.kt:85-88`); never transmitted |
| Notification metadata (optional notification access) | package, category, post time, group-summary flag and importance only; held in memory by `SignalStore`, not written to disk and never transmitted (FACT_SHEET §1.7) |
| Device or other IDs / crash logs / diagnostics | none collected (no analytics SDK, no `Log` calls: `rg 'Log\.|println' app/src` → 0 matches) |
| Location, contacts, calendar, SMS, audio, health data (device health APIs) | no permission, no API usage (§1.1, §1.4) |

### 3. Security practices

| Question | Answer |
|---|---|
| Is user data encrypted in transit? | Only reachable if Q1 = Yes → N/A |
| Do you provide a way for users to request deletion? | Only reachable if Q1 = Yes → see table above |
| **Independent security review (MASA)?** | **No** (no assessment performed) |
| **Committed to follow the Play Families Policy (badge)?** | Depends on the target-audience selection — see report §2.6. Recommended selection excludes children under 13 → **leave the badge off**. If the developer instead targets children, opt in only after re-checking the Families policy. |

## Consistency table (answer ↔ policy sentence ↔ source evidence)

| # | Data safety answer | Privacy policy sentence (`privacy.html`) that supports it | Source evidence | Match |
|---|---|---|---|---|
| 1 | Collects/shares no user data | "no data is sent to the developer… no accounts, no ads and no analytics… We do not collect, transmit, sell or share personal data" | `INTERNET` exists only for the user-driven Calm browser (§1.3); no analytics/ad/crash SDK | ✅ |
| 2 | (not declared) installed apps | "The app reads the list of installed apps… only to show your home screen and app list. This list stays on your device" | `<queries>` MAIN/LAUNCHER (§1.5) | ✅ |
| 3 | (not declared) photos | "Wallpaper photos are chosen with Android's system photo picker… only stores a copy in its private storage" | `MainActivity.kt:43-47`, `Wallpapers.kt:64-94` | ✅ |
| 4 | (not declared) plan/task/expense/GPA/habit/water/flashcard data | "Your tasks, events, widget content (including expenses, grades, habits and hydration logs), flashcards and settings stay on this device" | §1.4 rows | ✅ |
| 5 | (not declared) focus/session minutes | "usage minutes are counted by the launcher itself for apps you open from it… never sent anywhere" | `LauncherViewModel.kt:307-333` | ✅ |
| 6 | (not declared) PIN | "If you set a PIN, only a salted hash of it is stored… never the PIN itself" | `PinLock.kt:27,85-102` | ✅ |
| 7 | No ads declaration (App content → Ads: **No**) | "no ads" | no ad SDK in deps (§1.8) | ✅ |
| 8 | Security review: No | policy page makes no security-review claim | — | ✅ |
| 9 | (not declared) notification metadata | "Notification access (optional, special access) … reads metadata only — the app package, category, post time, group-summary flag and importance" | `SignalsListenerService.kt:24-42`, `NotificationSignals.kt:3-37` | ✅ |

Any mismatch would be ⚠️; none found after the `privacy.html` rewrite in this pass.
