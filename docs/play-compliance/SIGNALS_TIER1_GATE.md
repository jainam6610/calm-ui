# Signals — Tier 1 policy gate (NotificationListenerService)

Status: **Not implemented.** Tier 0 shipped (`signals/SignalsTier0.kt`, commit `885d99b`).
Tier 1 waits on the answers below and an explicit go-ahead from the developer.

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

- **Do not build Tier 1 yet.** Two open items block it: (1) confirming whether Play requires a
  sensitive-access declaration for notification access, and (2) the developer's decision that the
  feature is worth the listing/Data-safety surface.
- If it is approved, keep it strictly to the design: metadata only, in-memory counters, never any
  content, same opt-in + plain-language notice pattern as the torch, and update the listing + Data
  safety answers in the same change.
- Regardless of the outcome, the app already ships the useful half (Tier 0 call signals) with **zero**
  added permissions.

> Note: `websearch` was unavailable during this session, so the policy wording above is cited by URL
> and marked ❓ where it could not be quoted verbatim from a successful fetch.
