# Changelog

All notable changes to Calm UI are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). Play Store releases are listed on
the [store page](https://play.google.com/store/apps/details?id=com.studentlauncher).

## [Unreleased]

### Planned

- Fix the PIN removal path and require the PIN to change it
- Validate Plan input (empty titles, impossible dates)
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

[Unreleased]: https://github.com/jainam6610/calm-ui/compare/v3.2.0...HEAD
[3.2.0]: https://github.com/jainam6610/calm-ui/releases/tag/v3.2.0
