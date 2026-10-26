# Security policy

## Supported versions

| Version | Supported |
|---|---|
| 3.2.x (current) | ✅ |
| ≤ 3.1 | ❌ |

Security fixes land on the default branch and, when relevant, in the next Play release.

## Reporting a vulnerability

**Do not open a public issue for a security problem.**

Use GitHub's private reporting:

1. Go to the [Security tab](https://github.com/jainam6610/calm-ui/security) → **Report a vulnerability**.
2. Describe the issue, the affected version, and the smallest reproduction you can.

If you cannot use GitHub advisories, email **ruplaikarnawat01@gmail.com** with `Calm UI security` in the subject.

Please include:

- Affected version and install source (Play or self-built)
- Android version and device
- Impact: what an attacker gains, and what they need (physical access, a malicious app, notification access, etc.)
- Steps or a proof of concept

Never attach real personal data, notification content, task content or photos from your device.

## What to expect

- **Acknowledgement** within 5 working days.
- **Assessment and a fix plan** within 14 days for confirmed issues.
- **Credit** in the advisory and release notes if you want it.

This is a volunteer project, so these are best-effort targets rather than a commercial SLA. There is no bug bounty.

## Scope

In scope:

- The launcher app (`com.studentlauncher`) and its Calm/Detox flows
- The PIN gate and Detox early-exit rules
- Data handling: storage, backup rules, intent handling, exported components
- The privacy policy site in this repository

Out of scope:

- Issues that require a rooted device or a modified system
- Vulnerabilities in Android itself or in third-party apps/widgets
- Denial of service from a user deliberately filling their own storage
- Reports generated only by automated scanners without a demonstration

## Design commitments

These are intentional and are not vulnerabilities by themselves:

- The app has no accounts, analytics or crash reporting, and adds no network traffic beyond the optional Calm browser.
- Notification access is optional and reads **metadata only** (package, category, post time, group summary, importance). Titles and bodies are never read by the signals listener. The separate Calm digest stores silenced titles locally, only when the user grants it.
- Data is stored in app-private storage; `allowBackup` is off and backup/transfer rules exclude all app data.
- Detoxify is a self-imposed focus tool. It covers what a launcher can cover; it is not a device-management or parental-control product and cannot block uninstalled apps, system settings or recovery mode.

## Safe harbour

We will not pursue legal action against researchers who:

- Act in good faith and follow this policy
- Avoid privacy violations, data destruction and service disruption
- Give us reasonable time to fix an issue before public disclosure
- Do not exploit an issue beyond the minimum needed to demonstrate it
