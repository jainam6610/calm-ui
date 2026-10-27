# Design and production-readiness audit

Date: 2026-10-26  
Scope: Calm UI 3.2 (`com.studentlauncher`), previously Student Launcher  
Constraint from the brief: **no new features**. Improve what exists, keep it simple, minimal, calm and less distracting. The only requested additions are a binary-digit clock and a plain white Detox look.

---

## 1. Reference set

| Reference | What it is good at | What we should not copy |
|---|---|---|
| **Nothing OS** | One strong visual idea (dot matrix), monochrome hierarchy, a single red accent, generous negative space, glyph identity that is recognisable without decoration. | Novelty for its own sake: many matrix animations, easter eggs, and dotted everything can become noise. |
| **Apple Liquid Glass** | Optical depth is restrained; content stays the hero; surfaces are consistent; motion explains a spatial change rather than decorating it. | Heavy refraction, chromatic fringing, rainbow sheen and glass-on-glass stacking. Those are expensive, reduce contrast, and date quickly. |
| **Niagara Launcher** | Ruthless focus: one vertical list, one hand position, no pages of widgets, no decorative chrome, very few choices, fast to scan. | Taking minimalism so far that power features disappear. Our students still need widgets, focus tools and a plan. |
| **Calm UI today** | Strong local-first privacy story, a real detox mode, a useful plan, focused widgets, and a coherent dark canvas. | It currently combines three visual languages: Nothing dots, coloured glass, and iOS-like cards. The result is capable but not calm. |

### The one-sentence target

> **A quiet, monochrome home screen where the next useful thing is obvious, and nothing moves or glows unless it has a reason.**

---

## 2. Where the app is today

### Strengths

- The launcher has a real point of view: study, focus and detox, not another app drawer.
- Privacy is unusually strong: local-only plan and widgets, metadata-only notification signals, an explicit detox policy.
- The dot-matrix clock, glyph matrix and mono icons are a genuine identity, not stock Material.
- Widget spans, hosted Android widgets, undo, resize and keyboard/gesture handling are unusually complete for a small app.
- The detox countdown survives reboots and is enforced by a foreground service, not just a UI flag.
- Tests cover the policy and layout math, and the tree currently builds green.

### The main problem

The app has **too many ways to look different** and **too many decorative layers at once**:

- 18 clock styles, 7 icon packs, 5 moods, 3 dock styles, 2 home layouts, 4 widget backgrounds, 8 accent colours.
- Coloured "wash" gradients under widgets by type (glyph = magenta, timer = indigo, tasks = orange, battery = lime). The colour carries no meaning, fights the wallpaper, and makes the home screen feel like a dashboard.
- Liquid-glass surfaces add a shadow, a frosted wallpaper sample, a white tint, a diagonal sheen, a specular rim, refraction and chromatic aberration. That is six effects on one card.
- Accent colour is used for selection, progress, signatures, pills, dots, checkmarks and active cards. When everything is accented, nothing is.
- Decorative motion is spread across press-scale, page crossfade, depth blur, widget reveal, quote rotation, breathing pause, launch zoom and matrix bursts.
- Settings uses a ring around every glyph, uppercase tracked kickers, 18 dp cards and checkmarks. It is pretty, but it is not a quiet settings list.

### Production-readiness gaps (not only visual)

| Area | Gap | Severity |
|---|---|---|
| Branding / legal | The detox home is visibly called **"Calm"** and the README says it is "Calm's own launcher, ported unchanged". Calm is a third-party brand. The package is also `com.calm.launcher`. | **P0** |
| Localisation | Almost every user-facing string is hard-coded English. `strings.xml` has only a handful of entries. | **P0** for a store release |
| Accessibility | Many secondary labels are 10–11 sp at 60 % alpha. Contrast and minimum text size are not audited. A 48 dp touch target exists in some places but not all. | **P0** |
| Device evidence | No screenshot/device verification is recorded for the current tree. Visual claims in docs are not backed by runtime captures. | **P0** |
| Home navigation | Settings is only reachable through the All-Apps panel or the A–Z scrubber's final dot. The home screen itself exposes only "Plan". | **P1** |
| Feature discoverability | Widget Board is a second home page with no label; the dot indicator does not say what it is. Long-press is the only path to the home menu. | **P1** |
| Consistency | Corner radii range from 10 to 34 dp. Type sizes range from 8 to 72 sp. "Glass", "Solid", "Frosted" and "None" widget backgrounds have different border behaviour. | **P1** |
| Default distraction | New clock widgets show a rotating motivational quote by default. The quote invites tapping. | **P1** |
| Choice overload | 18 clock styles and 5 "moods" are presented as equally important. A production launcher should have 4–6 first-class choices and an "advanced" drawer. | **P2** |
| Performance | The glass shader and per-card wallpaper sampling are avoided on some devices but not all; every glass card also draws a shadow and two border gradients. | **P2** |

---

## 3. Comparison against the three references

### 3.1 Nothing OS

**Borrow**

- One canvas, one type family, one accent. The dot matrix is the signature, not a background effect.
- Put the signature in **one** place (the clock and glyph widget), not under every card.
- Use negative space as a material. A widget does not need a wash, a border, a shadow and a glow.
- Keep the red accent semantic: a signal, a warning, a blocked state. Do not paint selected cards red.

**Avoid**

- More matrix modes. The current catalog already has 17 modes. The resting state should be the default and the burst library can stay a niche.
- Making the glyph matrix the centre of attention on the home screen. It should feel like a quiet instrument.

### 3.2 Apple Liquid Glass

**Borrow**

- A surface is either **content** or **chrome**, not both. Bottom sheets and the dock can float; widgets should be readable.
- One optical rule for chrome: a blurred wallpaper sample, a light adaptive tint, a specular rim and a soft shadow. Edge lensing is optional and static. No chromatic aberration, no rainbow sheen, no animated refraction.
- Content (widgets, cards) keeps a flat frosted fill and a hairline. Chrome is the only layer allowed to bend light.
- Motion explains where something came from. The launch zoom and sheet spring are enough; the rest can be a short fade.

**Avoid**

- Glass on glass: sheets over widgets over wallpaper over a gradient. Flatten the stack.
- Using glass to hide a weak layout. A clear type hierarchy works better than blur.

### 3.3 Niagara Launcher

**Borrow**

- A single obvious axis: time, then the next action, then apps. The current home has time, date, Plan, pager dots, app handle, dock, A–Z rail and a widget area competing for the same screen.
- Few choices at the top level. Put icon packs, moods and 18 clock styles behind "Personalisation", not on the home path.
- Hands-first reachability: the dock and the app handle are good; the top-right Plan pill is not.
- Empty space is a feature. Do not fill the widget window just because it exists.

**Avoid**

- Removing the widget board entirely. Students use timers, tasks and exams; the board is useful if it is discoverable and quiet.
- Copying Niagara's letter list exactly. The current A–Z rail and letter pages are already a good compromise.

---

## 4. What we changed in this pass

These are the focused, low-risk changes for this pass. They do not add new product areas.

### 4.1 Glyph matrix and coloured glass

- **Removed every `cardWash` and `Ink.washFor` colour wash.** The magenta glyph card, the indigo timer card, the orange task card and the lime battery card are gone.
- The Glyph Studio preview and the Personalisation preview now use the same neutral tile as the rest of the app.
- **Split `Modifier.glass()` into two levels.** `Chrome` (dock, sheets, search, handle, menus, toasts) may cast a shadow and use a static edge-lensing shader; `Content` (widgets) stays flat and readable.
- The material now samples a 160 px blurred wallpaper, picks its tint from the wallpaper's luminance, carries a directional specular rim and gives chrome a soft shadow. Android 13+ adds subtle edge lensing; older versions keep the identical tint, rim and shadow without refraction.
- **Frosted** widget backgrounds are now visibly milkier than **Glass**, so the two choices in the widget sheet mean different things.

### 4.2 Binary digit clock

- Added a **Binary Digits** clock style. Each decimal digit is shown as a 4-bit value, hours and minutes separated, with the ordinary time underneath.
- The existing **Binary** style remains as the BCD dot clock; the settings label now distinguishes "Binary dots" from "Binary digits".

### 4.3 Detox in plain white

- The detox home now uses a **plain white** palette by default while a session is active.
- White canvas, near-black text, a single grey secondary tone and a hairline rule. No accent colour, no coloured progress, no branded "Calm" label in the UI.
- Removed the breathing animation from the pause screen. The pause is now a still, quiet wait with a thin ring and a timer. Less engaging on purpose.
- Status-bar icons switch to dark glyphs on the light canvas.

### 4.4 Quieter defaults

- New clock widgets no longer show a rotating quote by default.
- Pager dots use the foreground colour instead of the accent.
- Personalisation cards use a foreground outline for selection instead of an accent wash. Colour is reserved for the actual accent swatches and semantic state.
- Settings glyphs no longer sit inside a decorative ring.
- The root Settings list opens the home-app picker directly instead of bouncing through another page.

### 4.5 Verified usability fixes

- **Search covers every visible app**, including apps already on Home or in the Dock, and marks them "on home" or "in dock".
- **All Apps search sits at the top.** The navigation pills and All/Study chips step aside while typing, so the IME never covers the field or the results.
- **The Apps handle is a toggle.** It reads "Home ↓" while All Apps is open, closes on tap or swipe-down, and still opens on tap or swipe-up.
- **The light Personalisation preview** uses a light card and border instead of a hardcoded near-black panel.
- **The Home widget window fades at its bottom edge** while more widgets are scrollable, so a half-visible line reads as "more below" rather than a clipping bug.

---

## 5. Backlog (not done in this pass)

Ordered by production impact, not novelty.

### P0 — must be true before a store build

1. **Remove the third-party brand from the product.** Rename visible "Calm" strings to "Detox home" / "Detox apps". Decide whether the `com.calm.launcher` package is renamed or kept as an internal implementation detail.
2. **Move user-facing strings to `strings.xml`.** At minimum: home, all-apps, settings root, detox sheets, plan, permission rationales and error toasts.
3. **Accessibility pass.** Raise all secondary text to at least 12 sp; verify 4.5:1 contrast in both themes; ensure every control has a 48 dp target and a content description.
4. **Capture device evidence.** Home, All Apps, Settings root, Personalisation, Clock, Glyph Studio, Detox start, Detox home, one widget edit sheet, one light and one dark wallpaper.
5. **Verify release build and R8.** `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `bundleRelease` on the final tree.

### P1 — visual hierarchy and discoverability

6. **Give Settings a home affordance.** A single small "settings" control in the header, or make the Plan pill a two-item segmented control. Do not rely on a long-press.
7. **Label the Widget Board.** Replace the anonymous second dot with a tiny label or a short first-run hint.
8. **Collapse the clock list by default.** Show Dot, Digital, Words, Analog and Binary Digits first; put the other 13 under "More styles".
9. **Reduce the accent surface area.** Keep accent for: distracting-app dot, overdue task, timer progress, and the selected accent itself. Remove it from pager dots, selected cards, signatures and checkmarks.
10. **Unify radii.** One card radius (18 dp), one sheet radius (28 dp), one pill radius (full). Update the README's "12 dp" claim.
11. **Remove decorative typeframes.** Replace the 8 sp tracked "SIGNATURE" badge and 10.5 sp uppercase kickers with 12 sp sentence case.
12. **Make the home menu discoverable.** A short empty-state hint or a first-run lesson already exists; make sure it actually mentions long-press.

### P2 — simplification and performance

13. **Cut the mood system to 2 presets** (Quiet, Ink) or move it behind an "Advanced" page. Five moods multiply the number of visual states without adding a daily-use benefit.
14. **Reduce icon packs from 7 to 3 + Original** (Mono, Hairline, Dot). The rest can stay as legacy values but not as first-class cards.
15. **Make quote / fact widgets opt-in.** Keep the widgets, but do not put a rotating phrase on a clock.
16. **Remove the remaining decorative motion** that does not explain a state change: press-scale on static rows, staggered settings reveal, quote crossfade, launch zoom if it costs a frame on low-end devices.
17. **Cold-start and frame budget.** Measure first-frame time, home swipe, widget scroll and All Apps open on the minimum supported device. Remove the blurred-bitmap sampling from the glass if it does not earn its cost.
18. **Audit the detox browser and WebView.** It is a large surface with its own settings, ad lists and adult-site lists. It needs a privacy review and a clear offline/error state before it is marketed.

---

## 6. Design rules for the next passes

1. **One signature.** The dot matrix appears in the clock and the glyph widget. It does not appear as a background wash.
2. **One accent.** The accent is a signal, not a decoration. If a control is selected, use weight and a hairline, not a coloured fill.
3. **One surface rule.** A card is a neutral tile with a 1 dp hairline. Chrome (dock, sheet, toast) may be frosted. Never both.
4. **One motion rule.** Animate a change of place or state. Do not animate an idle surface.
5. **One home axis.** Time → next action → apps → tools. If a new control cannot fit that sentence, it belongs in Settings.
6. **Default to quiet.** Quotes, bursts, glows, breathing and confetti are opt-in, never the first-run state.
7. **If it is not readable at arm's length, it is not done.** 12 sp minimum for secondary text, 48 dp minimum for touch, 4.5:1 minimum contrast.

---

## 7. Verification for this pass

Status on 2026-10-26: **implemented and green**.

| Gate | Command | Result |
|---|---|---|
| Unit tests | `./gradlew testDebugUnitTest` | **BUILD SUCCESSFUL** (includes `BinaryClockTest`) |
| Lint | `./gradlew lintDebug` | **BUILD SUCCESSFUL**, no new errors |
| Debug build | `./gradlew assembleDebug` | **BUILD SUCCESSFUL** |
| Visual check | device/emulator screenshots | still owed (see P0 backlog) |

The source changes in this pass are limited to:

- `ui/Glass.kt` — two-level Liquid Glass: 200 px blurred sample, luminance-adaptive tint, specular rim, chrome-only shadow and API 33+ edge lensing with a same-looking fallback.
- `ui/Wallpapers.kt` — 160 px frosted copy plus wallpaper luminance for the tint.
- `ui/Theme.kt` — wash palette removed.
- `ui/GlyphStudio.kt`, `ui/Personalization.kt`, `ui/Widgets.kt` — wash call sites removed; binary clock added; quieter previews; light preview fixed; widget scroll fade.
- `ui/Home.kt`, `ui/AppViews.kt`, `ui/SettingsUI.kt`, `ui/FirstRun.kt`, `ui/Overlays.kt` — neutral selection colour, settings navigation, calmer pause; search scope and placement; toggleable Apps handle.
- `LauncherViewModel.kt` — search pool includes Home/Dock apps and reports placement.
- `com/calm/launcher/MainActivity.kt` — plain white detox palette, dark status-bar icons, still pause.
- `ui/WidgetSheets.kt` — quote default off.
- `README.md` — accurate description of the surface and clock list.

Glass-pass verification (2026-10-26, SM-G990E / Android 16): cold start 785 ms; no crashes or ANRs; light and dark Home, glass dock/handle, All Apps search above the IME with an "on home" badge, light Personalisation preview, widget fade and the Detox sheet all inspected on device.

## Addendum — 3.3.2, liquid glass held back (2026-10-27)

The neutral panel of 3.3.0 read as a flat card, and wallpaper structure was gone. Liquid glass
returns, but held back so it can never read as coloured glass:

- The surface is a blurred wallpaper sample (one 200 px bitmap per wallpaper), desaturated to
  about two thirds of its chroma, then overlaid with a theme-coloured veil. A photo can tint the
  rim a little, but never recolour a panel.
- Chrome alone runs the AGSL edge-lensing shader (API 33+) with a hint of dispersion; it also
  casts a shadow. Widgets and cards take the cheap backdrop crop so a board of twenty surfaces
  stays cheap.
- Every surface gets a specular streak and an inner bevel — light at the top-left, shade at the
  bottom-right — which is what reads as thickness.

`ui/Glass.kt` keeps both levels (`Content`, `Chrome`), `GlassEnv` / `LocalGlass` supply the
blurred sample and luminance, and `Wallpapers.blur` feeds the sample plus the optional wallpaper
accent colour.

The trade-off is deliberate again: panels follow the theme rather than the photo, so contrast is
predictable everywhere, and the shader runs on the few chrome surfaces only.

## Addendum — 3.3, the neutral panel (2026-10-27)

The liquid-glass material above shipped in 3.2 and was removed in 3.3 after using it on a photo
wallpaper: the sampled tint made sheets, the dock and menus take on the photo's colour, which read
as "coloured glass" rather than quiet chrome.

`ui/Glass.kt` drew one material for both levels — a neutral fill, a hairline rim, a subtle top
sheen on chrome, and a shadow on chrome only. There was no wallpaper sampling, no luminance-driven
tint and no AGSL shader left in the file. `GlassEnv` / `LocalGlass` are gone, and `Wallpapers.blur`
survives only to derive a wallpaper accent colour when that option is on.

