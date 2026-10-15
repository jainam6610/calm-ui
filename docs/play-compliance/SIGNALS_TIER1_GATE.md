# Signals — Tier 1 policy gate (NotificationListenerService)

Status: **Implemented, off by default** (see "How it shipped" at the end). Tier 0 shipped first
(`signals/SignalsTier0.kt`, commit `885d99b`); Tier 1 followed after the developer reviewed this
gate and approved it.

## What Tier 1 would be

Per the Signals design sheet, Tier 1 is a `NotificationListenerService` that reads **notification
metadata only**:

- allowed: `packageName`, `category`, `postTime`, `isOngoing`, `isSummary`, ranking/group flags
- never: `extras`, `title`, `text`, `people`, `largeIcon`/`smallIcon`, or any content
- storage: counters held **in memory only**; nothing persisted, nothing transmitted
- reactions: message pulse, digest summary, and per-app "signature" reactions, all from counts
- caller identity: none at any tier

Tier 2 (caller names, SMS bodies, contacts, call log, `READ_PHONE_STATE`) remains **never**, by design.

## Technical facts

| Fact | Status |
|---|---|
| Requires a `<service>` with `android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"` in the manifest | ✅ platform-documented
| Adds **no** `uses-permission` entry — it is a *special access*, granted by the user in Settings (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`), not a runtime permission | ✅ platform-documented
| The bound service is bound by the system once granted, so it is not a polling loop | ✅ platform-documented
| Reading `Notification.extras` is possible from that service, which is exactly why the design forbids it | ✅ platform-documented

## Google Play findings

| Question | Finding | Source |
|---|---|---|
| Does Play permit a launcher to request notification access at all? | Play has **no dedicated "notification access" policy**; the operative rule is the *Permissions and APIs that Access Sensitive Information* policy, which says an app may only request sensitive access that is **necessary to implement current features or services promoted in the listing**. A matrix reaction to a message is a promotable feature, so the gate is "is it core and advertised?", not "is it banned?" | `support.google.com/googleplay/android-developer/answer/9888170` (fetched; exact wording not re-verified this session → ❓ exact quote) |
| Is notification metadata "personal and sensitive user data"? | Notification metadata is derived from a user's communications, so it falls under the User Data policy; that policy requires limiting access, using it only for disclosed purposes, and (for sensitive data) an in-app disclosure/consent. Our design already does the limiting part (metadata only, no storage) | `support.google.com/googleplay/android-developer/answer/10144311` (policy URL; page fetch returned navigation only this session → ❓ section wording) |
| Must it go in the Data safety form? | Play defines "collect" as *transmitting data off the device*; data processed on-device and not sent off-device does not need to be disclosed. Counts held in memory and never transmitted should therefore stay out of scope — the same reasoning the repo already uses for the rest of the app | `support.google.com/googleplay/android-developer/answer/10787469`, already cited in `docs/PLAY_STORE.md` §Data safety form |
| Is there a mandatory sensitive-access **declaration form** in Play Console for notification access? | ❓ **Not verified.** Play Console shows declaration forms for some sensitive access (e.g. Accessibility, all-files, SMS/call log). Whether notification access triggers one must be confirmed in App content → Sensitive permissions before writing any code | ❓ needs a Console check |
| Abuse-risk posture | Notification access is a well-known vector for OTP/2FA interception, and Play enforces hard rules for SMS/call-log permissions specifically. A launcher asking for broad notification access may attract review scrutiny even when only metadata is used | ❓ general guidance not verified in this session (web search unavailable) |

## Conclusion

- ~~**Do not build Tier 1 yet.**~~ The developer approved building it; see below.
- The remaining open item is the Console check: **confirm whether Play requires a
  sensitive-access declaration form for notification access** before submitting an update that
  includes the listener.
- Regardless, the shipped design keeps strictly to the plan: metadata only, in-memory counters,
  never any content, opt-in with a plain-language notice, and no new `uses-permission`.

## How it shipped

- `signals/NotificationSignals.kt` — `NotificationMeta` has **no field** for a title, body,
  extras, people or icons, so content cannot be captured even by mistake. `isCountableSignal`
  drops the app's own notifications, ongoing work and service/progress/navigation/call noise.
  `SignalTally` keeps counts in a rolling one-hour window, in memory only.
- `signals/SignalsListenerService.kt` — reads package name, category, post time, the
  group-summary flag and the ranking importance. Never `Notification.extras` or any content.
- Opt-in switch in Settings with a grant-access button (the user must enable notification access
  in system Settings; the launcher never auto-grants and cannot).
- Reactions: a bundled digest plays a wide sweep, a loud (high-importance) notification plays the
  priority pulse plus the torch pattern **only if torch reactions are also opted in**, anything
  else plays one quiet pulse. A 2-second rate limiter keeps a chatty group chat from strobing.
- Permission surface: unchanged. The manifest gains the bound `<service>` (the system holds
  `BIND_NOTIFICATION_LISTENER_SERVICE`); the app still requests only `POST_NOTIFICATIONS`.

> Note: `websearch` was unavailable during this session, so the policy wording above is cited by URL
> and marked ❓ where it could not be quoted verbatim from a successful fetch.
