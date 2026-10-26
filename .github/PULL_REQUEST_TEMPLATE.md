# Contributing to Calm UI

Thanks for the pull request. Please fill this in so review is quick.

## What this changes

<!-- One or two sentences. What problem does it solve? -->

Fixes #

## How it works

<!-- The approach, and anything a reviewer might not expect. -->

## Checklist

- [ ] I read [`CONTRIBUTING.md`](../CONTRIBUTING.md) and the [Code of Conduct](../CODE_OF_CONDUCT.md).
- [ ] `./gradlew testDebugUnitTest lintDebug assembleDebug` passes locally.
- [ ] New logic has unit tests (or I explain below why it cannot be tested).
- [ ] No new dependencies, permissions, analytics or network calls.
- [ ] Widget surfaces stay flat (`Content`); only floating chrome uses `Chrome` glass.
- [ ] Motion respects the existing motion scale / reduced-motion setting.
- [ ] Interactive elements have a role and a label.
- [ ] I updated docs, strings or the changelog where behaviour changed.

## Visual change

<!-- Screenshots or a short recording, light and dark. Delete if not applicable. -->

## Notes for reviewers

<!-- Optional: trade-offs, follow-up work, things you are unsure about. -->
