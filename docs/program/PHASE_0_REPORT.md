# Phase 0 — launcher parity foundation

Date: 2026-10-22  
Baseline: `fcec642` (Phase R report committed at `e9c9e96`)  
Phase 0 code head: `348eb5a`  
Measured code commit: `eacd089`

> Three later **Detox** commits (`2ca2ee0`, `abe81f3`, `eacd089`) landed after Phase 0. They are outside this phase but are included in the measured commit below. Uncommitted working-tree changes are **not** included.

## Completed items

| Item | Commit | Result |
|---|---|---|
| 0.1 Apps drawer entry | `d142d80` | Pure swipe-up classifier (7 tests), visible Apps handle, page dots, long-press Home menu |
| 0.2 Settings affordance | `049cf40` | Visible `⚙ Settings` entry in All-Apps header |
| 0.3 Compact home | `3eb06d8` | Home honours the user's compact-density preference, not only small screens |
| 0.4 Grid columns | `c99c74a` | Configurable 3–5 columns, whole-row app limits, dynamic limit options |
| 0.5 Touch targets | `0b860b9` | 48 dp areas for segmented controls, switches, sliders, chips, swatches, wallpaper slots, scrubber, menus, resize handle |
| 0.6 Semantics | `58cf76d` | Radio/switch roles, selected state, scrubber state, labels, toast live region |
| 0.7 Undo remove | `f3f4a3e` | Staged widget delete, deferred hosted `appWidgetId` release, persisted 5 s undo entry, visible Undo pill |
| 0.8 Onboarding | `af07f23` | Five-step flow now teaches Apps handle, A-Z rail, widget resize, Widget Board and app actions; dead three-step flow removed |
| 0.9 Wallpaper crop | `576c54d` | `FillBounds` replaced with `Crop` on Home and in the editor |
| 0.10 Compliance wording | `8c302b7`, `b2a758a` | Privacy page, Play checklist, fact sheet and data-safety sheet disclose optional metadata-only notification access |
| Follow-ups | `87c7514`, `269b62c`, `63f3a45`, `65f60a8`, `348eb5a` | Full 48 dp chip target, expired-undo id release, lambda offsets, KTX preference edits, strings moved to `strings.xml` |

Phase 0 adds 17 unit tests (98 → 115). Later Detox tests bring HEAD to 128.

## Gate evidence

Run in a detached clean worktree so unrelated uncommitted edits could not affect the result. The worktree was first gated at `abe81f3`, then checked out to `eacd089` and the full command was rerun:

```text
.\gradlew testDebugUnitTest lintDebug assembleDebug bundleRelease
BUILD SUCCESSFUL in 6m 57s
```

| Measurement | Baseline `fcec642` | Measured `eacd089` |
|---|---|---|
| Unit tests | 98 passed | **128 passed, 0 failed** |
| Lint | 0 errors / 63 warnings | **0 errors / 66 warnings** |
| AAB size | 4,124,133 bytes (3.93 MiB) | **4,183,522 bytes (3.99 MiB)** |
| AAB SHA-256 | `9E3994C09F1A464E5BC3F27FF7D7D8B1B6DEA3A57F314C4000B55FF14ACCEAD3` | `7D779C561EC20C396EEF47B1494833CF04DA2D3067D9B8E01EE83203295AD01D` |

AAB delta: **+59,389 bytes (+1.44%)**, including the three later Detox commits. Phase 0 alone measured 0 errors / 62 warnings after the string move. The four extra measured warnings are `UseKtx` suggestions in the later Detox code.

Manifest: `git diff fcec642..HEAD -- StudentLauncher/app/src/main/AndroidManifest.xml` is empty. Merged release manifest still contains only:

```text
android.permission.POST_NOTIFICATIONS
com.studentlauncher.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
```

There is no `INTERNET` permission, no ads, no analytics and no new `uses-permission`. `BIND_NOTIFICATION_LISTENER_SERVICE` remains held by the system for the optional listener.

## Functional smoke test

Emulator: `Pixel_5`, Android 14 (API 34), debug APK installed with `adb install -r`.

Verified by `uiautomator dump` and touch input:

- Onboarding completes and lands on Home.
- Home shows the Apps handle, page indicator, visible Settings menu entry and the selected grid.
- Apps panel opens from the handle; All/Study segments and A-Z rail are present.
- Settings opens from the launcher and from the All-Apps gear.
- Grid columns changed 3 → 5; the app-limit options changed 8 → 10; the summary became `3/10`.
- A widget was added from Widget Center, selected, removed, and the **`Next up removed` / `Undo`** pill appeared.
- Widget Board is reachable by swiping Home and reports `Page 2 of 2`.

No `FATAL EXCEPTION` for `com.studentlauncher` appeared in logcat.

Runtime budgets are **not claimed**: the emulator is not a phone-valid performance target, so no cold-start or frame-time number is reported.

## Invariants

- No `INTERNET`; manifest unchanged.
- No ads, analytics, tracking or new permissions.
- New Phase 0 user-facing text is in `res/values/strings.xml`.
- Widget undo persists across process death and cleans expired entries on next start.
- Existing preference keys and widget JSON remain readable; no migration was required.
- Touch targets use 48 dp hit areas without enlarging the visual chrome.
- Reduced-motion handling still snaps the drawer instead of animating.
- Privacy/store copy now matches the optional metadata-only notification listener.

## Observed follow-ups (not changed in Phase 0)

- Settings → **Home** changes destination only; if the user entered Settings from All Apps, Home returns to the All Apps surface. This behaviour predates Phase 0.
- Interactive widget content can consume long-press, so selection works best on the widget body/handle. Unit-tested logic is unaffected.
- Notification Tier 1 shipping remains blocked on Play/privacy reconciliation and Console verification.

## Blockers before Phase 1

1. Decide unpaired-half migration semantics: preserve rendered width or preserve the `half` flag.
2. Define span height as frame heights (~104/144/204/264 dp) or content heights (80/120/180 dp).
3. Supply `antigravity-settings-iconpacks-addendum.md` before Phase 3.

Phase 1 has **not** started. Awaiting approval of Phase 0.
