# ADR 0003 — Owner-published evaluation releases, CI qualification and verification

**Status:** accepted — 2026-09-23. Supersedes the publication part of [ADR 0002](0002-async-release-pipeline.md).

## Context

Under ADR 0002 the release workflow built, tested and then created the tag and GitHub Release itself. v0.4.0 never shipped: the API 36 job failed for harness reasons (emulator without KVM, three modules installing concurrently on one device), and any owner-side tag would have raced the workflow's own tag/release creation. Version, tag and artifact name were also repeated by hand in the workflow.

## Decision

- `release/manifest.json` is the single source for product, version, versionCode and repository. Tag (`v<version>`) and artifact name (`replicascan-v<version>-debug.apk`) are derived by `tools/release_artifact.py`.
- Every push to `main` runs **qualification only**: repository gates, unit/lint/check/builds, an APK identity check, API 36 instrumentation on a Gradle Managed Device (KVM enabled, one module at a time), static site and CodeQL. The workflow never creates tags or releases.
- The **owner** publishes a GitHub *evaluation* release from a locally qualified build: `prepare` copies the exact APK, verifies `aapt` badging, writes `SHA256SUMS.txt` and `build-metadata.json`; the owner creates an annotated tag on the qualified SHA and uploads exactly those three assets.
- Pushing the tag runs `verify-published`, which downloads the public assets and fails if the asset set, checksum, metadata, badging or tag commit disagree.
- The consistency gate fails if the workflow hardcodes a version or regains tag/release creation.

## Consequences

- No race: exactly one actor (the owner) creates tags and releases; tags are never moved.
- An evaluation APK is debug-signed on the publishing machine; later evaluation builds must come from the same keystore to update in place. Play production (signed AAB, R8, Play signing, Data safety) remains a separate, future process.
- Remote CI for the release SHA may still be running when the release is published. Release notes state its status as observed once, never as green before it is.
