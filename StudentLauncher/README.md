# Student Launcher 3.0

A calm, distraction-free Android launcher for students. Kotlin + Jetpack Compose, no third-party libraries.
Developed by thats.jainam.

## Build
1. Open this folder in Android Studio (latest stable) and let Gradle sync. If Studio offers newer AGP, Kotlin or Compose versions, accept them.
   Pinned versions: Gradle 8.11.1, AGP 8.9.1, Kotlin 2.1.20, Compose BOM 2025.04.01, compileSdk/targetSdk 36, minSdk 26.
2. Run on a device or emulator, press Home and choose Student Launcher as the default home app.
3. Play Store: see docs/PLAY_STORE.md (signing, AAB, listing text, data safety, privacy policy, store graphics in store/).

Verified green on this tree: `./gradlew testDebugUnitTest lintDebug assembleDebug` (0 lint errors).

## Quiet Motion redesign
- First-run onboarding is a five-step, skippable flow that ends by teaching the Apps handle, A-Z rail, widget resize, and Widget Board gestures. It only requests notification permission when the student explicitly chooses focus reminders; the flow can be restarted in Settings.
- **Plan** is a first-party internal app, reached from Home. Tasks store title, course, ISO due date, priority, and completion locally in launcher preferences. No account or network is involved.
- Home uses a single widget language (12dp corners, neutral outlines, restrained red signal state). Widget placement uses Compose layout animation and the home canvas uses spring/fade-through motion; motion intensity and visual density are configurable in Settings.
- Settings is an ordinary internal launcher destination rather than a persistent menu bar. Existing wallpaper, app, widget, and focus controls remain available.

## What is new in 2.0
- Wallpaper editor: blur, dim, and zoom + drag for photos (Style > Adjust wallpaper).
- Widget system: add, reorder, edit and remove. Long-press any widget to edit it.
  Clock (dot, digital, words, analog), date, week, battery, next up, exam countdown, study timer,
  tasks, quick settings, 3D sphere, glyph matrix, plus regular Android widgets.
  Each widget can use a Glass, Solid or None (no translucent cover) background, full or half width, and its own text colour.
- Glyph matrix: a live 25 x 25 dot display (489 dots) that follows your phone. It shows the clock,
  switches to a charging ring (with a bolt while plugged in) while plugged in, and plays a short
  burst when a signal arrives — then settles and costs nothing (no idle frame loop). It does not
  drive the physical Glyph Matrix on Nothing Phone (3), which needs Nothing's own SDK.
- Icon packs: Original, Mono, Hairline, Bold, Dot, Bubble and Tile. Every pack is derived from the
  app's own themed (monochrome) layer when it ships one; apps without one keep their original icon.
- Optional reactions, off by default: a finished focus session plays the Done reaction on the matrix,
  and — only if you opt in — the camera light can flash too, bounded in code (3 flashes per second,
  3 seconds per session, 12 per hour, a 10 second cool-down, and skipped in battery saver, Do Not
  Disturb, reduced motion, when the device is warm, or below 15% battery while unplugged).
- Call signals (Android 12+), off by default: the launcher notices an incoming call from the audio
  mode only. A single accelerometer reading then picks face up (matrix pulse, never the torch) or face
  down (torch, if enabled, and nothing on the matrix). No caller name, number, SMS, contact or call
  log is ever read.
- Notification signals, off by default: with notification access granted, the launcher counts
  notifications from their **metadata only** — package, category, post time, group-summary and
  importance flags. It never reads a title, body, sender, icon or extras, keeps counts in memory
  only, and ignores its own notifications and background noise.
- Side rail dock (vertical glass pill) as an alternative to the bottom dock.
- Focus reminders every 5, 10 or 15 minutes while you are in a distracting app you opened from the launcher.
  Uses an inexact alarm and a notification. No accessibility, usage-access or overlay permission.
- Smoother motion: home/panel transition, blur depth and scrubber wave run in the draw phase, so they do not recompose.
- Play Store readiness: target API 36, R8 + resource shrinking, signing config, docs, icon and feature graphic.

## Map
- ui/Glass.kt: liquid glass modifier
- ui/Home.kt: layers, transitions, overlays
- ui/Widgets.kt, ui/WidgetSheets.kt, ui/HostedWidgets.kt: widgets, add/edit sheets, Android widget hosting
- ui/GlyphMatrix.kt: 25 x 25 mask (489 cells) and the event-driven reactions
- ui/IconPacks.kt: the seven icon-pack renderers
- ui/Personalization.kt: the Personalization hub and its Icons/Theme/Wallpaper/Home/Clock pages
- reactions/TorchPolicy.kt, reactions/TorchController.kt: opt-in torch reactions and their safety rails
- signals/SignalsTier0.kt: Tier 0 call signals (audio mode + one accelerometer reading)
- ui/WallpaperEditor.kt, ui/Wallpapers.kt: wallpaper generation and editing
- ui/AppViews.kt: app rows, scrubber, panel, docks
- ui/Menus.kt, ui/Overlays.kt: menu bar, dropdowns, sheets
- FocusReminders.kt: reminder alarms and notification
- LauncherViewModel.kt: state and persistence
