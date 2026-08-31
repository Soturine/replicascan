# Contributing to ReplicaScan

ReplicaScan is proprietary, source-visible software. Issues, reproducible bug
reports, security reports through the private channel, and product feedback are
welcome. External code is not accepted for merge automatically.

## Code contributions

Before submitting code, obtain explicit written agreement from the maintainer
covering the licensing or assignment terms for that contribution. A pull
request opened without that agreement may be reviewed as a proposal but will
not be merged into the proprietary codebase.

Do not submit code, assets, generated output, or copied examples that you do not
have the right to contribute. Third-party material must retain its license and
required attribution.

## Bug reports and proposals

- Search existing issues before opening a new one.
- Include the affected version, Android version, device/emulator, reproduction
  steps, expected behavior, and actual behavior.
- Remove documents, OCR text, paths, and personal information from logs and
  screenshots.
- For vulnerabilities, do not open a public issue; follow
  [SECURITY.md](SECURITY.md).

## Development baseline

- JDK 17
- Android SDK Platform 36
- Gradle Wrapper from this repository

Use a focused branch and Conventional Commits. During implementation run the
smallest relevant tests; before a release milestone run the qualification
commands documented in [docs/testing.md](docs/testing.md).

By participating in project discussions, you agree to follow
[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). Current licensing details and the
historical boundary are in [LICENSING.md](LICENSING.md).
