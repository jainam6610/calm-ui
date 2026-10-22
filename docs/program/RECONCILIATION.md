# Phase R — Reconciliation report

Date: 2026-10-22  
Mode: Plan / read-only. No source file was edited. Build outputs were regenerated only to establish a baseline.  
Repository: `C:\Users\dell\Downloads\StudentLauncher-v2`  
Baseline commit: `fcec642` — `feat(signals): live ambient matrix state and Tier 1 notification metadata signals`

> Staged here because Plan mode forbids writing into the repository. On approval, the build agent can move this file to `docs/program/RECONCILIATION.md` unchanged.

---

## 1. Git state

| Item | Result |
|---|---|
| Branch | `master`, tracking `origin/master` |
| Ahead/behind | **ahead 9**, behind 0 |
| Working tree | **clean** (`git status --porcelain` empty) |
| Stashes | none |
| Tags | none |
| Remote head | `e06fc18` |
| Local commits not pushed | 9 |

Recent local commits:

```
fcec642 feat(signals): live ambient matrix state and Tier 1 notification metadata signals
1db0aab docs: record the signals policy gate and refresh the 3.x feature copy
ed44073 perf(wallpaper): sample photos to screen size and batch icon state writes
885d99b feat(signals): Tier 0 call signals from the audio mode, no caller identity
6223a79 feat(torch): opt-in torch reactions with hard, hard code-enforced safety rails
c4fa154 feat(matrix): reactive 25x25 glyph matrix, event-driven and static when idle
a7f1937 feat(icons): seven built-in icon packs derived from the themed glyph
f31c075 feat(settings): Personalization hub with sub-pages and one global clock style
fb7e12d chore: baseline v3.0 working tree before design phases
```

No push or tag was performed.

---

## 2. Baseline build and measurements

Environment: Windows 11, Gradle 8.11.1, Gradle JVM 17.0.19, project `compileSdk`/`targetSdk` 36, `minSdk` 26, `versionCode` 7, `versionName` 3.0.

| Gate | Result |
|---|---|
| `.\gradlew testDebugUnitTest --rerun-tasks` | **BUILD SUCCESSFUL** |
| Unit tests | **98 tests, 0 failures, 0 errors** across 17 classes |
| `.\gradlew lintDebug` | **0 errors, 63 warnings** |
| `.\gradlew assembleDebug` | **BUILD SUCCESSFUL** |
| `.\gradlew bundleRelease` | **BUILD SUCCESSFUL** (R8 + signing) |

Lint warning mix: `UseKtx` 41, `AutoboxingStateCreation` 22, `DefaultLocale` 10, `GradleDependency` 5, `UseOfNonLambdaOffsetOverload` 2, plus one each of `InlinedApi`, `DataExtractionRules`, `UnusedAttribute`, `ObsoleteSdkInt`, `StaticFieldLeak`.

Artifacts at HEAD:

| Artifact | Size |
|---|---|
| `app-release.aab` | **4,124,133 bytes (3.93 MiB)** |
| `app-debug.apk` | 11.07 MB |
| `app-release.apk` | 1.95 MB — **stale** (13-10-2026); AAB is the authoritative baseline |

AAB SHA-256: `9E3994C09F1A464E5BC3F27FF7D7D8B1B6DEA3A57F314C4000B55FF14ACCEAD3`

Merged release manifest:

- `POST_NOTIFICATIONS` only.
- No `INTERNET`.
- One AndroidX-injected `com.studentlauncher.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.
- `SignalsListenerService` bound with `BIND_NOTIFICATION_LISTENER_SERVICE`.
- `allowBackup="false"`.

**Correction to the prompt:** the existing suite is **98 tests**, not 36. All 98 pass. Budget/acceptance language that assumes 36 needs updating.

### Runtime numbers — ❓

No usable device was attached:

- `adb devices -l` shows `R5CT119SE7T` as **unauthorized**.
- A `Pixel_5` AVD exists but was not booted.

Therefore meminfo, gfxinfo, startup, frame health and touch behaviour remain ❓. Exact commands:

```powershell
adb devices -l
adb install -r StudentLauncher\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -W -n com.studentlauncher/.MainActivity
adb shell dumpsys meminfo com.studentlauncher
adb shell dumpsys gfxinfo com.studentlauncher framestats
```

No runtime number in this report is invented.

---

## 3. Scorecard audit

Legend: ✅ verified · ❌ claim wrong or materially incomplete · ❓ needs a device/Console/manual check.

| # | Claim | Verdict | Evidence / correction |
|---|---|---|---|
| 1 | Span model absent | ✅ | `data/WidgetModels.kt` still stores only `half: Boolean` and `height: Int (1..3)`. No `colSpan`/`rowSpan`. |
| 2 | A half before a full leaves a hole | ❌ | `buildRows` puts a lone half in its own row (`ui/Widgets.kt:220-229`), and every row child uses `Modifier.weight(1f)` (`:275`). A lone half therefore **renders full width**, not half and not a hole. Width silently depends on the neighbour. This is worse for predictability and changes the migration design. |
| 3 | Hosted widgets ignore `resizeMode`/min sizes; no `updateAppWidgetOptions` | ✅ | `ui/HostedWidgets.kt:29-34` reads only `item.size`; `data/WidgetHostManager.kt` never reports size. Repo-wide grep found **zero** `updateAppWidgetOptions`, `updateAppWidgetSize`, `resizeMode`, `minResizeWidth/Height`, `targetCellWidth/Height`. Additionally, hosted widgets bypass `WidgetFrame` (`ui/Widgets.kt:528-534`), so there is **no corner-resize path at all**; only the edit sheet changes `size`. |
| 4 | No undo on remove | ✅ | `LauncherViewModel.removeWidget` deletes immediately (`LauncherViewModel.kt:760-768`). No `undo` identifier exists in `ui/` or the ViewModel. Hosted `appWidgetId` is released immediately. |
| 5 | No swipe-up gesture to All Apps | ✅ | No vertical-drag gesture exists in `ui/` (grep: no `detectVerticalDragGestures`/`verticalDrag`). Home is a `HorizontalPager` (`Home.kt:283`). All Apps opens only from the A–Z scrubber (`LauncherViewModel.kt:509-526`; index 27 opens Settings) or the panel's own pill. The search field lives **inside** the panel (`AppViews.kt:401-403`), so it cannot open the panel. |
| 6 | `compactDensity` applies only to the Board | ✅ | `WidgetBoard.kt:231` passes it. Home decides compact from `screenH < 640.dp` (`Home.kt:234,424`). `compactDensity` setter is `LauncherViewModel.kt:964`; no Home call site. |
| 7 | Grid fixed at 4 columns; limits 4/6/8/10/12 | ✅ | `AppViews.kt:231` `list.chunked(4)`. Limit options `Personalization.kt:266-268`. At limit 6 the grid is `4 + 2`, a ragged row. |
| 8 | Interactive controls below 48 dp | ✅ | `Theme.kt:207` Segmented 32 dp; `:230` switch 44×26; `Personalization.kt:130/294` 34 dp; `:173` 32 dp; `:200/208/215` 34 dp; `Home.kt` scrubber letter hit band 10–17 dp; `Overlays.kt:170` 42 dp. The resize handle is 44 dp. Visual size and touch size are not separated anywhere. |
| 9 | Settings entry point hidden | ✅ (nuance) | Home itself has no Settings entry: `QuietHomeHeader` shows only Plan. Reachable via scrubber index 27 (`LauncherViewModel.kt:526`) and the All-Apps panel's visible "Settings" pill (`AppViews.kt:361`). No long-press on empty home (grep: long-press exists only on widgets and app entries). |
| 10 | Icon packs silently fall back to Original | ✅ | `ui/IconPacks.kt:56-60`: no monochrome layer → `return@remember null` → caller uses original. Produces mixed icon sets with no explanation or setting. |
| 11 | Adaptive icons double-shrink | ✅ (math) / ❓ visual | `data/AppRepository.kt:35-53` renders the drawable into the full `px` box with scale 1, so an adaptive icon's 108 dp canvas fills the tile. `IconPacks.kt:108-109` then adds a 20 % inset per side. Visible adaptive art ≈ `0.66 × 0.60 = 0.40` of the tile (~22 dp of 56 dp). Legacy art ≈ 0.60. Need a screenshot to sign off on the target 60–66 %. |
| 12 | Dot pack uses one binary alpha threshold | ✅ | `IconPacks.kt:133-140`: area average alpha `> 64` → dot on, else off. No coverage-sized or multi-level dots. |
| 13 | Outline re-punches the glyph per draw | ✅ (cost exists) / ❓ frame | `IconPacks.kt:149-162`: 12 `drawImage` calls plus `DstOut` on every Canvas draw; no cache. Whether scrolling re-records every frame is unmeasured (no device). |
| 14 | `FillBounds` wallpaper distorts | ❌ | `Wallpapers.build` produces a bitmap at exactly `wPx × hPx` (`Wallpapers.kt:187-209`), and Home rebuilds it on size change. `Home.kt:139` `FillBounds` is therefore 1:1, not distorted. Only a transient stretch is possible before the async rebuild completes. Keep 0.9 as a defensive `ContentScale.Crop`, but it is **not a confirmed user-visible bug**. |
| 15 | Dead `Onboarding` in `FirstRun.kt` | ✅ | `FirstRun.kt:53` `fun Onboarding(...)` has no call site. Live flow is `OnboardingFlow` (`Onboarding.kt:67`), called from Home. |
| 16 | README says 3 onboarding steps; live flow has 5 | ✅ | `README.md:15` says three-step. Live steps are `STEP_VIDEO..STEP_DONE` (`Onboarding.kt:55-59`) = 5. The dead 3-step flow is what the README describes. |
| 17 | `strings.xml` has only 2 strings | ✅ | `app_name`, `notification_signals`. Every other UI string is hard-coded. |
| 18 | Wallpaper presets = 6 | ❌ | Actual presets: `dune`, `mist`, `meadow`, `plain` = **4** (`Wallpapers.kt:34`). Plan for 4 + photo/shuffle, not 6. |
| 19 | No page indicator on Home | ✅ | `pagerState.currentPage` is read only in the back handler (`Home.kt:240`). No dots/label. |
| 20 | No settings search/backup/folders/badges/shortcuts | ✅ | Repo-wide grep for `folder`, `badge`, `Uninstall`, `ShortcutManager`, `LauncherApps`, settings search returned zero. |
| 21 | Tier 1 notification listener was gated before shipping | ❌ **Blocker** | See §5. The gate document exists, but the privacy policy, Play listing and fact sheet were not updated, and the Play Console declaration is still unverified. |
| 22 | Existing suite is 36 tests | ❌ | 98 tests, 17 classes, all green. |
| 23 | Widget row heights are 80/120/180 dp | ❌ (code today) | `height` is a **content cap**: 80/120/180 dp (`Widgets.kt:541-545`), inside 12 dp padding (`:373`), so the rendered frame is about 104/144/204 dp. Compact caps are 60/90/140. Phase 1 must decide whether the span table describes frame height or content height; using 80/120/180 as frame heights would shrink widgets by ~24 dp. |
| 24 | `compactDensity` also affects app grid | ❌ (today) | Only the Board consumes it. App grid density is fixed. |

Additional verified facts useful for the program:

- Widget catalog: 28 built-in types + Android hosted widgets.
- Icon packs: 7 (`Models.kt` list; `IconPacks.kt`).
- Clock styles: 17 (`Widgets.kt:636-639`).
- Accessibility: exactly **one** `contentDescription` in the whole app (`WidgetBoard.kt:115`, "Add widget"). `Theme.tap` supplies `Role.Button`; switches/segmented/swatches have no roles or state descriptions.
- No `values-land`, `values-sw600dp` or `values-night` resource qualifiers; adaptive layout is Compose-only and currently phone-only.

---

## 4. What already exists from earlier prompts

| Area | Status | Evidence / commit |
|---|---|---|
| Clock styles | **Implemented** | 17 styles, one global preference, `ClockStyleTest`; `f31c075`. |
| 7 built-in icon packs | **Implemented** | `a7f1937`; `IconPackTest`; Dot/Outline renderers. |
| Reactive glyph matrix | **Implemented** | 25×25 / 489 cells, event-driven, static when idle; `c4fa154`; `GlyphMatrixSpecTest`. |
| Torch reactions with rails | **Implemented** | `6223a79`; rate/duration/cooldown rules covered by `TorchPolicyTest`. |
| Signals Tier 0 | **Implemented** | Audio-mode call signals, no identity; `885d99b`. |
| Signals Tier 1 | **Implemented, off by default** | Metadata-only listener + in-memory counters; `fcec642`. Compliance incomplete — §5. |
| Settings hub / Personalization | **Partial** | 7 pages exist (`f31c075`): Root, Personalization, Icons, Theme, Wallpaper, Home, Clock. Root is still one long page, no search/reset/backup, controls are undersized, entry is hidden. |
| Preloaded photos | **Implemented** | Seeded photo wallpapers; `PreloadSeedTest`; `ed44073`. |
| Onboarding | **Implemented but stale** | 5-step `OnboardingFlow`; dead 3-step `FirstRun.Onboarding`; README describes the dead one. |
| Play-compliance docs | **Present but stale** | `5e535ef`, `f32d92e`, `e06fc18`, `1db0aab`. They describe Tier 0 only and still say "no special access". |
| Settings/icon-pack addendum referenced by §8 | **Missing** | No `antigravity-settings-iconpacks-addendum.md` or prompt files in the repo. ❓ Supply it before Phase 3. |

Strengths to preserve: deep focus/Mindful/PIN system, 28 student widgets, no-INTERNET/no-ads/no-analytics, glyph matrix and reactions, offline Plan app, and the existing pure-policy + JVM-test pattern.

---

## 5. Tier 1 gate check — BLOCKER

The code is data-minimised and technically sound:

- `SignalsListenerService` reads only package, category, post time, ongoing/summary/important flags.
- `NotificationMeta` has no field for title/body/extras/people/icons.
- `SignalStore` is in-memory only; nothing persisted or transmitted.
- Notification access is a special access, not a `uses-permission`, so the manifest permission set is unchanged.

But the **policy and store documentation are not reconciled**:

| Surface | Finding |
|---|---|
| `privacy.html:63-67` | Lists only `POST_NOTIFICATIONS` and says "That is the only permission the app requests." It never discloses notification-listener access or that the app can read notification metadata. |
| `StudentLauncher/docs/PLAY_STORE.md:51` | Says "no special access." That is now **false**. Notification access is a special access. |
| `docs/play-compliance/FACT_SHEET.md:108` | Describes only Tier 0 call signals and the single permission. Not updated for Tier 1. |
| `docs/play-compliance/SIGNALS_TIER1_GATE.md` | Records developer approval but leaves the Play Console sensitive-access/data-safety declaration unverified. |
| In-app disclosure | The Settings section says "Counts notifications only, never their content" and offers "Grant notification access", but there is no prominent consent screen before the system settings jump and no link to the policy. |

**Verdict:** the technical gate is satisfied; the **Play/privacy gate is not**. Treat as a release blocker, not a code blocker. Recommended handling:

1. Keep Tier 1 off by default.
2. In Phase 0, add a dedicated privacy disclosure and update `privacy.html`, `PLAY_STORE.md`, `FACT_SHEET.md`, `DATA_SAFETY_ANSWERS.md`.
3. Before any upload, verify the Play Console sensitive-access declaration and data-safety answers.
4. Do not ship Tier 1 until that Console check is confirmed.

This needs your decision because §0 says a blocker stops the phase. It does not prevent starting Phase 0 code, but it must be scheduled before release.

---

## 6. Risk list

| # | Risk | Why it matters | Mitigation |
|---|---|---|---|
| R1 | Tier 1 docs/store mismatch | Play policy/deception risk | Fix in Phase 0; Console check before upload |
| R2 | Migrating `half` → `colSpan` changes existing layouts | A lone half currently renders full width. Mapping it to `colSpan=2` would suddenly make it half width. | Decide: preserve **rendered** width (unpaired half → 4) or preserve flag (→ 2). Recommend preserving rendered width, with a one-time preview/undo. Pure migration tests. |
| R3 | Row-height table does not match current frames | 80/120/180 are content caps; frames are ~104/144/204 with padding | Define span heights as **frame** heights and use 104/144/204/264 (or subtract padding) to keep the look; decide at Phase 1 gate |
| R4 | Gesture arbitration | Swipe-up vs widget scroll vs pager vs resize handles vs Board drag vs hosted views | Pure classifier + explicit start zone + axis lock + nested-scroll overscroll only + written arbitration table |
| R5 | Hosted widget touch/resize | `AndroidView` swallows touches; no selection path today | Transparent overlay while selected; report sizes via `updateAppWidgetOptions`; verify API names against SDK 36 |
| R6 | Icon geometry is global | Every icon changes at once; risk of over/under-zoom | Separate adaptive/legacy insets, screenshot review, settings, no cache assumptions |
| R7 | No runtime baseline | Cannot prove frame/startup/memory budgets | Mark ❓; measure on an authorized device or Pixel_5 AVD before/after each phase |
| R8 | Docs drift and missing addendum | Phase 3 design source is absent; README/FACT_SHEET already stale | Supply addendum; make docs updates part of each phase gate |
| R9 | 48 dp hit-area expansion | Bigger invisible boxes can overlap adjacent controls/gestures | Enlarge boxes only where spacing allows; test 130 %/200 % font scale |
| R10 | Hard-coded strings | Localisation later becomes a huge mechanical diff | Start adding new strings to `strings.xml` now; extraction last |

---

## 7. Phase 0 plan (for approval; not started)

### 0.1 Swipe-up All Apps + affordance + page indicator

Files: new `ui/DrawerGesture.kt`, `ui/Home.kt`, `ui/AppViews.kt`, `ui/Widgets.kt` (overscroll hook), `LauncherViewModel.kt`, `res/values/strings.xml`.

- Pure classifier: start zone (bottom band above `WindowInsets.systemGestures`, dock, empty area), dx/dy dominance, 64 dp threshold, fling, axis lock.
- Drive the existing panel progress; release settles open/closed.
- Only on widget column when it cannot scroll further; never while a widget is selected, dragging, or a sheet is open.
- Visible "Apps" affordance; dots for Home/Board.
- Reduced motion: snap/fade, no follow animation.
- Tests: `DrawerGestureTest` (zone, slop, dominance, threshold, fling, scroll-overscroll, cancelled gesture).

### 0.2 Settings entry points

Files: `Home.kt`, `FirstRun.kt`/`Overlays.kt` (menu), `AppViews.kt`.

- Long-press on empty home → Settings / Widgets / Wallpaper.
- Gear/visible control in All-Apps header; keep scrubber route.
- Must not fire on widgets, app icons, or during pager drag.

### 0.3 `compactDensity` on Home

Files: `Home.kt`, pure helper file + test.

- `compact = preferredCompact || screenH < 640.dp`.
- Apply to HomeApps and widgets; test the pure predicate and the resulting limits.

### 0.4 Real grid columns / no ragged row

Files: `LauncherViewModel.kt`, `AppViews.kt`, `Personalization.kt`, `data/Models.kt`, pure helper + test.

- Add `homeColumns` (3/4/5, default 4); `chunked(columns)`.
- Replace app-count options with `columns × rows` (e.g. 4/8/12), or clamp existing values to the nearest valid multiple.
- Pure `gridSlots`/migration function with tests; keep old keys for one release.

### 0.5 Touch targets ≥ 48 dp

Files: `Theme.kt`, `Personalization.kt`, `Home.kt`, `AppViews.kt`, `Overlays.kt`, helper.

- Wrap visuals in ≥48 dp touch boxes; drawn size unchanged.
- Flexible text heights (`heightIn(min=…)`); verify 130 %/200 % scale.
- No layout overlap; test manual + semantics.

### 0.6 Semantics

Files: same as 0.5 plus `Widgets.kt`.

- `GlassSwitch` → `Role.Switch` + state; `Segmented` → selectable group; resize handle description; scrubber current letter; swatches; dock; toast live region.

### 0.7 Undo for remove

Files: `LauncherViewModel.kt`, new `UndoBuffer.kt`, `Home.kt`/`Widgets.kt` pill, tests.

- 5 s Undo pill; restore at original index/order.
- Hosted `appWidgetId` released only after the window.
- Persist pending delete so process death cleans up safely.
- Tests: restore index, expiry, hosted deferral, double-delete.

### 0.8 Onboarding teaches gestures

Files: `Onboarding.kt`, `FirstRun.kt`, `README.md`.

- Add swipe-up, scrubber, long-press select, Widget Board.
- Delete dead `FirstRun.Onboarding`; fix README to the real 5-step flow.

### 0.9 Wallpaper scaling

Files: `Home.kt`, `WallpaperEditor.kt`.

- Replace `FillBounds` with `ContentScale.Crop` defensively. This is a safety change, **not** a confirmed bug fix (see claim 14).

### 0.10 Compliance wording (recommended addition)

Files: `privacy.html`, `docs/PLAY_STORE.md`, `docs/play-compliance/*`.

- Disclose notification-listener access, metadata-only scope, in-memory-only storage, user control.
- Remove "no special access".
- Keep Tier 1 off by default until Console check.

### Phase 0 gate

- `.\gradlew testDebugUnitTest lintDebug assembleDebug` green; report 98 + new tests.
- No new permission; merged manifest diff empty except expected service already present.
- AAB delta vs 4,124,133 bytes.
- Gesture arbitration table written.
- 130 %/200 % font scale and TalkBack pass on changed screens (❓ without device).
- README/Play docs updated.

---

## 8. Phase 1+ findings that need decisions before work starts

1. **Span migration of unpaired halves** — preserve rendered width or flag? (R2)
2. **Span height definition** — content cap vs frame height. Current frames are ~104/144/204, not 80/120/180. (R3)
3. **Hosted widget APIs** — verify against SDK 36 whether to use `updateAppWidgetOptions` with `OPTION_APPWIDGET_MIN_WIDTH/MAX_WIDTH/...` and the API 31 size list; `updateAppWidgetSize` may not be public. ❓
4. **Icon target** — approve 60–66 % visible art for adaptive and legacy separately. ❓
5. **Third-party icon packs** — remains deferred; gate requires manifest `<queries>` diff and Beta flag.
6. **Phase 3 addendum** — not in the repository; supply it before starting settings work. ❓

---

## 9. Approval requested

Phase R is complete. I recommend:

- **Approve Phase R** as above, with the corrections (98 tests, 4 wallpaper presets, no `FillBounds` bug, no "hole").
- **Approve Phase 0** with **0.10 added** for compliance wording.
- Decide the two Phase 1 migration questions (R2/R3) before Phase 1 starts.
- Keep Tier 1 off by default and confirm the Play Console declaration before any upload.

No implementation has begun. Switch to the build agent to execute Phase 0 after approval.
