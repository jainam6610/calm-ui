# Google Play checklist for Student Launcher

Developed by thats.jainam.

## Requirements this project already meets
- targetSdk 36 / compileSdk 36. Google Play requires new apps and updates to target Android 16 (API 36) from August 31, 2026.
- Release builds use R8 shrinking and resource shrinking, and build an Android App Bundle (AAB).
- No AccessibilityService, usage-access, overlay (SYSTEM_ALERT_WINDOW), exact-alarm or QUERY_ALL_PACKAGES permission.
- The only permission is POST_NOTIFICATIONS, requested when the user turns on focus reminders.
- allowBackup is off and data-extraction rules exclude all app data, so nothing is copied to
  cloud backup or device-to-device transfer.
- Predictive back is enabled and the UI is edge-to-edge (both are required behaviour on API 36).

## Build the release bundle
1. Create an upload key (once):
   `keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
2. Copy `keystore.properties.example` to `keystore.properties` and fill it in. Never commit it.
3. Run `./gradlew bundleRelease`.
4. Upload `app/build/outputs/bundle/release/app-release.aab` in Play Console and enrol in Play App Signing.
5. Before promoting to production, run `./gradlew lintRelease` and test on Android 8, 13 and 16 devices.
   New personal developer accounts may need a closed test with a minimum number of testers first. Check the current rule in Play Console.

## Store listing
- App name: Student Launcher
- Category: Personalization
- Short description (80 characters max):
  A calm, distraction-free launcher for students with widgets and focus tools.
- Full description:

  Student Launcher is a minimal home screen built for studying.

  Home
  - Your favourite apps in a clean list or grid, with an A to Z scrubber for everything else.
  - Liquid glass design, smooth spring animations and dot-matrix details.
  - Light and dark themes, your own wallpaper with blur, dim and zoom controls.
  - Monochrome themed icons, or your original icons, in the shape you like.

  Widgets you can add, move, resize and remove
  - Clock (dot, digital, words or analog), date, week, battery
  - Next up, exam countdown, study timer, tasks
  - Quick settings shortcuts, 3D dot sphere and a 25 by 25 glyph matrix display
  - Regular Android widgets too

  Focus tools
  - Study mode hides the apps you mark as distracting.
  - A short pause before a distracting app opens.
  - Optional reminders every 5, 10 or 15 minutes while you are in a distracting app.
    Reminders are plain notifications. The app only counts the time of apps you open from
    it — no accessibility, no usage access, no overlay and no special access.

  Private by design: no accounts, no ads, no tracking. Everything stays on your phone.

  Developed by thats.jainam.

## Data safety form
- Does your app collect or share any of the required user data types? **No.** Google's definition:
  "'Collect' means transmitting data from your app off a user's device" and "user data accessed by
  your app that is only processed locally … and not sent off device does not need to be disclosed"
  (https://support.google.com/googleplay/android-developer/answer/10787469). The app has no
  INTERNET permission and no network code, so the answer stops there.
- Encryption in transit / deletion-request questions are only asked when the first answer is Yes.
  If they do appear, answer: not applicable (no data leaves the device) and "users delete it
  themselves: clear app storage or uninstall; nothing is stored on our servers".
- Independent security review: No. Committed to Play Families policy: only if the app is declared
  to target children (see Content rating and audience).
- Permissions to explain if asked: POST_NOTIFICATIONS, only for the optional focus reminders.

## Content rating and audience
- Answer the IARC questionnaire honestly: no violence, no user-generated content, no purchases, no ads,
  no location, no communication features, no personal information prompts.
- Target audience (developer's final call): recommended **13-15, 16-18, 19-21, 22+** — i.e. students who
  are teenagers and older, children under 12 excluded. That keeps the app outside the Google Play Families
  policy (which applies "if one of the target audiences for your app is children"). The app would already
  meet most of it (no ads, no third-party SDKs, nothing collected), so targeting under-12s is possible too —
  but then every Families rule, the listing imagery and the Data safety "Committed to follow the Play
  Families Policy" badge apply. Source: https://support.google.com/googleplay/android-developer/answer/9893335
- Health apps declaration: complete it if Play shows it on App content — the health policy covers apps with
  wellbeing/self-tracking features and says "All developers must complete the Health apps declaration form".
  Honest answers: no medical claims, not a medical device, no health data read from sensors or Health Connect.
  Source: https://support.google.com/googleplay/android-developer/answer/16679511

## Policy notes
- Say clearly in the listing that this is a home screen replacement.
- Do not describe it as parental control or as an app blocker. It hides apps inside the launcher and sends reminders, it does not block other launchers or apps.
- Reminders are inexact alarms and can arrive a little late when the phone is in battery saver.
- Privacy policy URL (enter in Play Console → App content → Privacy policy):
  https://jainam662010-boop.github.io/StudentLauncher/privacy.html
  Source of truth is the repo-root `privacy.html` (GitHub Pages serves the root of `master`;
  `gh api repos/jainam662010-boop/StudentLauncher/pages` → `source: {branch: master, path: "/"}`).
  After editing it, commit + push and verify with
  `curl.exe -sIL https://jainam662010-boop.github.io/StudentLauncher/privacy.html` (expect 200).

## Store graphics included
- `store/icon-512.png` (512 x 512 app icon)
- `store/feature-graphic-1024x500.png` (feature graphic)
- Screenshots: take at least 4 from a real device or emulator, portrait, showing home, the A to Z panel, widgets and the wallpaper editor.
