# Changelog

All notable changes to Calm UI are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). Play Store releases are listed on
the [store page](https://play.google.com/store/apps/details?id=com.studentlauncher).

## [3.3.2] — 2026-10-27

### Added

- **Liquid glass is back**, held back so it cannot tint the interface. Chrome surfaces bend the
  blurred wallpaper at their edge with a hint of dispersion (Android 13+), wear a specular rim
  and an inner bevel, and cast a soft shadow. Widgets and cards take the cheaper plain backdrop
  so a board can hold twenty of them without cost.

### Fixed

- **Binary Digits** showed the four-bit groups and then repeated the ordinary time underneath.
  The clock is now bits only, matching the other minimal styles.

## [3.3.1] — 2026-10-27

### Fixed

- Long-press → **Uninstall** now opens the system uninstaller. Android requires the normal
  `REQUEST_DELETE_PACKAGES` permission for `ACTION_DELETE`; without it the system dialog opened and
  closed again in the same frame. No runtime prompt is involved.

## [3.3.0] — 2026-10-27

A correctness release: settings stop rewriting each other, the PIN and Plan guards do what they
say, and the interface drops coloured glass for one neutral panel.

### Added

- **App info** and **Uninstall** in the app long-press menu, both handed to the system through `package:` intents, so the launcher asks for no runtime permission.
- Signals page now reports whether notification access is actually granted, with **Grant notification access**, **Check again** and **Test reaction**.
- **Add a glyph matrix widget** when no glyph matrix is on Home or the widget board, because a reaction has nowhere to appear without one.
- First install starts with the real default home: one full-width, three-row, borderless clock tile (Dot style, system time, date and quote).

### Changed

- **Glass is now a plain neutral panel everywhere.** One neutral fill, a hairline rim, a subtle top sheen and a chrome-only shadow. The wallpaper-sampled tint, the luminance-following chrome and the Android 13+ edge-lensing shader are gone, so a photo wallpaper can no longer colour sheets, the dock or menus.
- The home app limit is independent of the grid column count. Changing columns no longer rewrites the limit, and the current limit stays selected even when it is not a whole row.
- The About sheet, Privacy page and Settings row read the version from the installed package instead of a hardcoded string.
- Gradle wrapper 8.14.6 and Kotlin 2.1.21 patch updates; the Compose compiler plugin follows the Kotlin version.

### Fixed

- Changing or removing the PIN now requires the current PIN. Removing a PIN that was never set is a no-op instead of a misleading toast.
- Plan and Class editors reject a blank title or a malformed date and stay open, instead of closing and silently dropping the entry.
- The notification listener re-binds itself after Samsung/One UI drops it, and only while the access grant is still there.
- Torch reactions no longer turn themselves off for the rest of the session when the camera is momentarily busy (a video call or another flashlight app). The app retries after 10 seconds and shows "Camera busy" instead of "No torch found".

### Planned

- Make Back handling predictable across sheets and internal destinations
- Larger touch targets for the smallest chrome controls

## [3.2.0] — 2026-10-26

The Liquid Glass pass, the usability fixes found during a full feature test, and the
move from a student-only launcher to Calm UI.

### Added

- Two-level material system: flat frosted **Content** for widgets and optical **Chrome** for the dock, sheets, search, handle, menus and toasts.
- Adaptive chrome tint driven by wallpaper luminance, a directional specular rim, a chrome-only soft shadow, and static edge lensing on Android 13+ with an identical-looking fallback below it.
- Search now covers every visible app, including apps already on Home or in the Dock, with an "on home" / "in dock" badge.
- Clear (×) control in the search field.
- Bottom fade in the Home widget window while more widgets are scrollable.

### Changed

- **The launcher is now Calm UI**, for anyone who wants a quieter home screen rather than students only. The application ID stays `com.studentlauncher` so existing installs keep updating; the Play listing name and the in-app About text change with this release.
- First-run onboarding starts with the distraction picker instead of a promo video, so setup is shorter and carries no third-party logos or outdated branding.
- Search moved to the top of All Apps; navigation pills and All/Study chips hide while typing so the IME cannot cover the field or results.
- The Apps handle is now a toggle: it reads "Home ↓" while All Apps is open and closes on tap or swipe-down.
- **Frosted** widget backgrounds are visibly milkier than **Glass**, so the two choices mean different things.
- The blurred wallpaper sample used by chrome is 160 px wide, up from 72 px.
- README and design audit now describe the content/chrome split.

### Fixed

- The light-theme Personalisation preview used a hardcoded near-black card and border.
- Search could not find apps that were already pinned or docked.
- The All Apps panel could collapse to zero height when the keyboard opened.
- Light-theme hairline borders were invisible on the Personalisation tiles.

## Earlier releases

Versions 2.0 through 3.1 were published on Google Play before this repository was
opened up. Their changes are summarised in the [project README](StudentLauncher/README.md)
and the [phase reports](docs/program/).

[Unreleased]: https://github.com/jainam6610/calm-ui/compare/v3.3.2...HEAD
[3.3.2]: https://github.com/jainam6610/calm-ui/releases/tag/v3.3.2
[3.3.1]: https://github.com/jainam6610/calm-ui/releases/tag/v3.3.1
[3.3.0]: https://github.com/jainam6610/calm-ui/releases/tag/v3.3.0
[3.2.0]: https://github.com/jainam6610/calm-ui/releases/tag/v3.2.0
