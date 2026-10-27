# Foreground service declaration — Play Console

App: **Calm UI** (formerly Student Launcher; `com.studentlauncher`), versionCode 13, versionName 3.3, targetSdk 36
Prepared: 2026-10-25
Source of truth: `StudentLauncher/app/src/main/AndroidManifest.xml:8-9,121-128`,
`StudentLauncher/app/src/main/java/com/studentlauncher/detox/DetoxGuardService.kt`

Use this file for **Play Console → App content → Foreground service permissions**.
The declaration is reviewed under the Device and Network Abuse policy (Foreground services).

---

## 1. What to select in the console

| Console control | Answer |
|---|---|
| Foreground service type | **Special use** (`FOREGROUND_SERVICE_SPECIAL_USE`) |
| Use case (pre-set list) | Choose **Other / My use case is not listed**, then paste the manual use-case text below |
| Manifest subtype already declared | `Countdown for the app's own Detoxify focus session` |
| Video | Required. Use the shot list in §5 |

Manual use-case text:

> Focus-session countdown for the app's own launcher. A user starts a timed Detoxify
> session and the service keeps that user-chosen deadline accurate while they use
> other apps.

---

## 2. Console field — description of the app functionality

Paste this (494 characters):

> Detoxify is an optional focus mode the user starts from the launcher and unlocks with
> a PIN. For 1 hour to 30 days, the home screen becomes a countdown showing only the apps
> the user allowed. This service keeps that deadline accurate while the user is in other
> apps: it holds the end time, posts an ongoing low-importance countdown notification,
> refreshes the saved deadline at most hourly, and ends the session at zero. It starts
> only from the visible launcher and stops when the session ends.

Longer version, only if the form allows more space:

> Detoxify is an optional focus mode inside Calm UI. The user picks a length
> (1 hour, 1 day, 1 week or 30 days), picks the apps that stay available, and confirms
> with their PIN. The launcher then hands the screen to the ported Calm home, which shows
> the remaining time in place of the clock.
>
> The service exists for the time the user is *not* on that screen. Android pauses the
> launcher's UI timers as soon as another app is in front, so a visible-only countdown
> would stop and the session could outlive the deadline the user chose. `DetoxGuardService`
> owns the deadline instead. It stores the remaining time on the monotonic clock (so
> changing the system clock cannot extend a session), wakes at most once an hour to
> refresh the saved deadline, and wakes at the deadline to end the session, clear the
> saved deadline, post a "Detoxify complete" notification and stop itself.
>
> The ongoing notification uses the system chronometer (`setUsesChronometer` +
> `setChronometerCountDown`), so the time left ticks in the shade without waking the app
> every second. The notification is low importance, silent, non-dismissible while the
> session runs, and opens the launcher when tapped.
>
> The service starts only from `LauncherViewModel.startDetox()` (and when the launcher
> resumes with an active session), never from a boot receiver, alarm, job or other
> background trigger. It stops in `finish()` and when the user ends the session in the
> launcher. It is not a general-purpose background daemon and there is no data collection:
> all state is in app-private preferences and the notification is local.

---

## 3. Console field — user impact if the task is deferred or interrupted

Paste this (464 characters):

> If the service is deferred or interrupted, a session can outlive the period the user
> chose: the launcher keeps returning to the quiet screen after the countdown should have
> ended. The ongoing countdown can also disappear while the session is still enforced, so
> the user loses the visible time left and the reason their home screen is restricted.
> The deadline is the feature the user opted into, so it must stay reliable for up to
> 30 days while they use other apps.

Why this is the real impact (for a reviewer reply, not necessarily the form):

- The session is a commitment device: the user set a PIN specifically so they cannot
  casually undo it. A late deadline is the failure mode the feature exists to prevent.
- The notification is also the user's status display. If it is delayed, the user cannot
  tell whether the session is still running, how long is left, or why apps are hidden.
- The app deliberately does **not** use exact alarms, accessibility, usage access,
  overlays or a device admin. The foreground service is the least-privilege way to keep
  one user-started timer honest.

---

## 4. Why `specialUse` and not something else

Put this in the reviewer note / "additional information" box if one is available, and
use it if Google asks follow-up questions.

**No defined foreground service type applies.** The work is not media playback,
media processing, media projection, location, camera, microphone, phone call, connected
device, data sync, health, remote messaging or a system exemption.

**`shortService` cannot do it.** Android caps `shortService` at roughly three minutes;
Detoxify sessions are 1 hour to 30 days (`DetoxPolicy.kt:14-25`).

**Without a foreground service, the deadline is not enforceable:**

| Alternative | Why it is not sufficient |
|---|---|
| UI timer only (`Handler` / `viewModelScope`) | Paused when the activity is not visible. The user is in another app for most of the session — exactly when the deadline must still hold (`DetoxGuardService.kt:22-30`). |
| `WorkManager` | Minimum periodic interval is 15 minutes; work is inexact and can be deferred by Doze / battery saver. It cannot maintain a live ongoing countdown. |
| `AlarmManager` exact alarm | Needs `SCHEDULE_EXACT_ALARM` (special access) or `USE_EXACT_ALARM` (limited to alarm/calendar core use); Play restricts exact-alarm use, and this app deliberately requests neither. An alarm also only fires once — it does not keep session state and an ongoing session notification current for a month. |
| Plain notification, no service | The process can be killed; nothing ends the session at the deadline, and the user is left with a stale or missing countdown. |

**The service is narrowly scoped:**

- Started only by a visible activity (`LauncherViewModel.kt:649`, resumed session at
  `LauncherViewModel.kt:168-172`), satisfying Android 12+ background-start rules.
- One ongoing, low-importance, silent notification (`DetoxGuardService.kt:131-149`).
- Uses the system chronometer, so it does not wake every second.
- Wakes at most once an hour and once at the deadline (`DetoxGuardService.kt:83-88`).
- Holds no wake lock, requests no additional runtime permission, performs no network I/O
  and sends no data off the device (`DetoxGuardService.kt:29-30`).
- Stops at zero (`DetoxGuardService.kt:90-104`) or when the user ends the session
  (`LauncherViewModel.kt:732`).

**User-perceptible.** The user starts it with a PIN; the notification appears immediately;
Android 13+ also lists it in the Task Manager / Active apps. The user can end the session
from the app (PIN + 10-minute cooling-off) or stop the app from system settings.

**Core functionality.** Detoxify is a headline feature, not a background convenience: it
has its own home-screen entry (`ui/AppViews.kt:374`), its own settings group
(`ui/FirstRun.kt:97`) and its own start/summary sheets (`ui/Detox.kt`). Without the service,
the countdown — the whole feature — is unreliable whenever the user opens another app.

---

## 5. Demo video — required shot list (60–120 seconds)

Play requires a video showing the user steps that trigger the feature. Record on a
physical device running Android 14+ (the FGS type is only enforced there), with a fresh
install of the build you are submitting.

### 5.1 Fresh-install pre-flight (do this before recording)

1. Uninstall any previous copy, then install the signed release build (`versionCode 11`).
   Do not record over old app data — the notification channel and PIN state must be fresh.
2. Make it the default Home app: **Settings → Apps → Default apps → Home app → Student
   Launcher**. If it is not the default launcher, pressing Home leaves the session screen
   and the demo looks wrong.
3. Grant notifications *before* starting the session: open Calm UI →
   **Settings → Pauses & reminders → Allow reminders → Allow**. On Android 13+ this is the
   only place the app requests it, and without the grant the FGS notification will not
   appear in the shade.
4. Turn off Do Not Disturb and battery saver. Set a long screen timeout. Record at 1080p
   with the status bar visible.
5. Show the build once: **Settings → Version** should read **Calm UI 3.3 (13)**.

### 5.2 Exact taps

1. Home → tap **Detoxify** → choose **1 hour** → **start** → set a PIN (for example 1234).
2. The Calm screen opens with the countdown in place of the clock. Pull down the shade and
   show the ongoing **Detoxify — 59:xx left** notification.
3. Tap **tools ↑** at the bottom of the Calm home → tap **calculator**. That opens another
   app while the session runs. Pull down the shade again: the same notification is still
   there, with less time left. (Using Calculator avoids the Home-button ambiguity and proves
   the service, not the visible launcher, is keeping the countdown.)
4. Optional: open the Android 13+ **Active apps / Task Manager** panel to show Student
   Launcher is running a foreground service.
5. Tap the notification → it opens the launcher and returns to Calm.
6. End the session to show it stops: **tools ↑ → leave dumb mode → start the wait**.
   The 10-minute cooling-off is real; record it, then jump-cut with a caption
   *"10-minute cooling-off, trimmed"*. After the wait: **leave dumb mode → PIN →** type
   `i choose to leave` **→** enter a reason of 10+ characters **→ leave**. Show the ongoing
   countdown notification disappear and the **Detoxify ended** notification appear.
7. Keep the video unlisted on YouTube/Drive and paste the link in the declaration.

The minimum acceptable clip is steps 1–3 (about 45–60 seconds): it proves the countdown
survives while another app is in front, which is the whole reason the service exists. The
early-exit footage in step 6 is what proves it stops; include it if you can, but do not fake
the countdown or the wait.

---

## 6. Before you submit — two real gaps

### 6.1 Notifications are not requested when Detoxify starts

`POST_NOTIFICATIONS` is only requested from **Settings → Pauses & reminders → Allow
reminders** (`ui/FirstRun.kt:165` → `MainActivity.kt:57-61`). On a fresh Android 13+
install, a reviewer can start Detoxify without ever granting notifications. The service
still runs, but its notification is not in the shade, which weakens the "user-perceptible"
story and makes the demo video impossible.

Recommended small fix before submitting: when `startDetox()` runs and
`POST_NOTIFICATIONS` is not granted on API 33+, show an in-context explanation and request
it before `DetoxGuardService.start(ctx)`. Do not block the session if the user declines;
just explain that the countdown will only be visible in the Task Manager.

### 6.2 The compliance docs still say there is no service

These lines are stale and contradict the manifest, the privacy policy and this
declaration. Update them before uploading:

- `docs/play-compliance/FACT_SHEET.md:20` — "0 services"
- `docs/play-compliance/FACT_SHEET.md:92` — "No WorkManager, no foreground services, no
  boot receiver."
- `docs/play-compliance/PLAY_COMPLIANCE_REPORT.md:58` — permission dump lists only
  `POST_NOTIFICATIONS`; it should also list `FOREGROUND_SERVICE` and
  `FOREGROUND_SERVICE_SPECIAL_USE`.

### 6.3 Honest risk

`specialUse` is reviewed subjectively. The strongest parts of this case are: user-initiated,
PIN-gated, visible ongoing notification, no data collection, minimal wakeups, and no other
FGS type or exact-alarm permission that fits. The weak part is that Detoxify is a feature
of a launcher rather than the app's entire purpose, so describe it as a **core feature**
(which it is) and never as an app blocker or parental control: the app only changes its own
home screen and never blocks apps opened outside it.

---

## 7. Exact manifest facts (for the reviewer note)

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<service
    android:name=".detox.DetoxGuardService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Countdown for the app's own Detoxify focus session" />
</service>
```

Keep the console use case consistent with the `android:value` above. The platform reads the
manifest value as a hint; the console declaration is what the reviewer reads, so they should
describe the same feature in the same words.
