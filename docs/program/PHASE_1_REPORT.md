# Phase 1 — widget spans and hosted sizing

Date: 2026-10-22  
Baseline: `9e4963a` (Phase 0 report)  
Phase 1 head: `8a6b974`  
Measured commit: `8a6b974`

## Decisions applied

| Decision | Choice |
|---|---|
| Unpaired legacy `half` | Preserve rendered width: an unpaired half becomes `colSpan = 4`; two consecutive halves become `2 + 2`. |
| Span height | Frame heights, including frame padding: `104 / 144 / 204 / 264 dp`. Compact: `84 / 114 / 164 / 224 dp`. |
| Grid order | Flow packing by `colSpan`; order is position. A lone half leaves a deliberate half-width hole. |

## Completed items

| Item | Commit | Result |
|---|---|---|
| Span model and migration | `32e81b3` | `WidgetItem` now stores `colSpan` / `rowSpan` (1–4). Legacy JSON resolves explicit spans first, then preserves rendered width for old halves. Old hosted `size` maps to rows 1 / 2 / 4. |
| Flow grid | `32e81b3` | Home and Widget Board pack rows by span and reserve trailing columns. A lone half no longer expands to full width. Board vertical-neighbour and drag logic use the same packing. |
| Four height levels | `8a6b974` | Resize and Settings expose Small / Medium / Tall / Extra tall. Frame and content tables are explicit; content caps remain 80 / 120 / 180 / 240 dp. |
| Hosted widget sizing | `8a6b974` | A hosted widget's starting row span comes from the provider minimum. Composed size is reported with `updateAppWidgetOptions` (`MIN/MAX_WIDTH/HEIGHT`; `OPTION_APPWIDGET_SIZES` on API 31+). |
| Hosted controls | `8a6b974` | Shared accent resize handle for framed and hosted widgets; hosted settings target is a 48 dp hit area with a 28 dp visual. |
| Strings | `8a6b974` | `widget_settings` added to `res/values/strings.xml`. |

## Gate evidence

Run in a detached clean worktree at `8a6b974` so unrelated uncommitted Detox work could not affect the result:

```text
.\gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease
BUILD SUCCESSFUL in 4m 4s
```

| Measurement | Phase 0 `eacd089` | Phase 1 `8a6b974` |
|---|---|---|
| Unit tests | 128 passed | **140 passed, 0 failed, 0 skipped** |
| Lint | 0 errors / 66 warnings | **0 errors / 63 warnings** |
| AAB size | 4,183,522 bytes (3.99 MiB) | **4,185,110 bytes (3.99 MiB)** |
| AAB SHA-256 | `7D779C561EC20C396EEF47B1494833CF04DA2D3067D9B8E01EE83203295AD01D` | `A08BB249BB446052D9F1FF421CFFA3B00AD0459699F4AD356A77C2C251608332` |

Phase 1 adds **12 unit tests** (7 migration/packing, 5 frame/height). AAB delta from Phase 0: **+1,588 bytes (+0.038 %)**. Delta from the original baseline `fcec642`: **+60,977 bytes (+1.48 %)**.

The 63 measured warnings are lower than Phase 0's 66 because the clean Phase 1 worktree excludes later uncommitted Detox edits; Phase 1 itself adds no new warning category.

## Invariants

- No `INTERNET`, ads, analytics or new permissions. `AndroidManifest.xml` is unchanged from Phase 0.
- Existing `home_widgets_v2` / `board_widgets_v2` JSON remains readable. New JSON writes `colSpan` / `rowSpan` plus one-release legacy `half` / `height` fields.
- New user-facing text is in `strings.xml`.
- Touch targets remain at least 48 dp; the hosted settings and resize controls use 48 dp hit areas without enlarging the visual chrome.
- Reduced-motion behaviour and the widget undo window are unchanged.

## Functional smoke test

Emulator: `Pixel_5`, Android 14 (API 34), debug APK from the clean worktree installed with `adb install -r`, app data cleared.

Verified by `uiautomator dump` and touch input:

- Onboarding completed and landed on Home; the default Clock / Next up / Study timer widgets rendered with the new frame heights.
- Long-pressing the Clock selected it and exposed a 48 dp **Resize widget** target.
- Dragging the corner down grew the Clock from Medium to the maximum height; the row below moved down by the expected 120 dp.
- Settings showed **Small / Medium / Tall / Extra tall**; choosing Small returned the Clock to the shortest frame.
- Dragging the corner left made the Clock half-width; Next up moved into the same row and Study timer stayed on the next row, leaving the expected hole at the right.
- The Widget Board packed the same widgets by span: Clock full-width, Next up + Study timer side by side.
- Home spans survived a force-stop and relaunch; the Board save path uses the same JSON writer.
- An Android **Analog** clock was added through **Browse Android widgets**, granted host access, configured, and appeared in a hosted frame.
- The hosted widget's 48 dp **Widget settings** target opened its sheet with the same four height choices; Medium and a corner resize changed its frame height.
- No `FATAL EXCEPTION` for `com.studentlauncher` appeared in logcat.

Runtime budgets are **not claimed**: the emulator is not a phone-valid performance target, so no cold-start or frame-time number is reported.

## Observed follow-ups (not changed in Phase 1)

- Hosted providers may ignore reported sizes if they do not declare resizable options.
- Widget spans are packed by row; a tall widget does not allow a later short widget to fill space beside it in the next row. This keeps the order stable and avoids overlap.
- Notification Tier 1 shipping remains blocked on Play/privacy reconciliation and Console verification.
- Phase 3 still needs `antigravity-settings-iconpacks-addendum.md`.

Phase 1 is complete pending approval. Phase 2 has not started.
