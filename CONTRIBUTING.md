# Contributing to Calm UI

Thanks for taking the time to help. This is a small, focused project: a launcher that stays quiet, works offline and respects the people using it. Contributions that protect those properties are the most valuable ones.

## Ways to help

- **Report a bug** with the bug report form. Include your Android version, device, app version, and the exact steps.
- **Suggest a feature** with the feature request form. Explain the real moment the change improves, not just the widget.
- **Fix something** from the issue tracker. Look for [`good first issue`](https://github.com/jainam6610/calm-ui/labels/good%20first%20issue) or [`help wanted`](https://github.com/jainam6610/calm-ui/labels/help%20wanted).
- **Improve the docs** — setup friction is a bug too.

Please read the [Code of Conduct](CODE_OF_CONDUCT.md) first. Security issues go to [SECURITY.md](SECURITY.md), never a public issue.

## Development setup

1. Install **JDK 17** and **Android SDK 36** (Android Studio latest stable is the easy path).
2. Clone and open the **inner** project:

   ```bash
   git clone https://github.com/jainam6610/calm-ui.git
   cd calm-ui/StudentLauncher
   ```

3. Build and test before you change anything:

   ```bash
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```

4. Install on a device, press Home, and choose **Calm UI** (the debug build appears as *Calm UI Debug* and uses `com.studentlauncher.debug`, so it can coexist with the Play install).

Release signing is optional for development. If you need it, copy `keystore.properties.example` to `keystore.properties` and point it at your own keystore. **Never commit a keystore or its passwords.**

## What a good change looks like

- **Keep it local.** No analytics, no crash reporting SDK, no network calls added to normal operation, no new permissions without a discussion issue first.
- **Pure logic stays pure.** Rules, policies, packing and parsing live in plain Kotlin with no Android imports and get unit tests. UI code stays thin.
- **Follow the material system.** Widgets are `Content` (flat, readable). Only floating chrome uses `Chrome` glass. Do not add a third surface style.
- **Respect motion settings.** Use `LocalMotionScale` or the existing springs so reduced-motion users get instant transitions. Never add an idle animation loop.
- **Match the surrounding code.** Kotlin style, 4-space indent, Compose naming, existing type scale and spacing. Avoid new dependencies; the app ships with none.
- **Accessibility is part of the feature.** Interactive elements need a role, a label, and a touch target of at least 48 dp where practical.

## Commits and pull requests

Use [Conventional Commits](https://www.conventionalcommits.org/):

```text
feat(widgets): resize hosted widgets with the grip
fix(search): keep results above the keyboard
docs(privacy): describe the Detoxify browser
test(detox): cover the 10-minute early exit
```

A pull request should:

1. Explain the **problem** and the **approach** in a sentence or two.
2. Link the issue (`Fixes #12`).
3. Include tests for new logic; say explicitly if something cannot be tested and why.
4. Include a screenshot or short recording for visual changes, in both light and dark themes where relevant.
5. Pass `./gradlew testDebugUnitTest lintDebug assembleDebug` locally. CI runs the same command.

Keep pull requests small. A focused fix lands faster than a rewrite, and unrelated formatting changes make review harder.

## Reporting bugs

Use the **Bug report** form and include:

- App version (Settings → About) and whether it is the Play or a self-built install
- Device model and Android version
- Steps to reproduce, expected result, actual result
- A screenshot or screen recording if the issue is visual
- `adb logcat` output around the failure if the app crashed

Do not include personal data from your device — no notification text, no task content, no photos.

## Feature requests

Describe the real moment you are trying to protect. "A widget that shows tomorrow's first meeting" is actionable; "make it better" is not. Features that add an account, a feed, ads, telemetry or a mandatory permission will be declined — that is the point of the project.

## Licence

By contributing you agree that your contribution is licensed under the [MIT Licence](LICENSE) and that you have the right to submit it.
