# Release runbook

## Normal flow

1. Align version catalog, `release/manifest.json`, changelog and release notes.
2. Run the local qualification listed in `AGENTS.md`.
3. Push the prepared `main` SHA. Do not create the tag locally.
4. GitHub validates, builds the APK once, tests API 36, scans, checks the site and publishes only when all required jobs pass.
5. Query the initial run once; do not poll. GitHub owns the remaining wait.

## Invariants

- Manifest product/version/code must match the Android build.
- `publish` must be `true`, tag must be absent or already point to the exact SHA, and the downloaded APK checksum must match.
- Release publication never rebuilds and never moves a tag.
- Public assets are exactly `replicascan-v0.4.0-debug.apk`, `SHA256SUMS.txt` and `build-metadata.json`.

## Recovery

- **Gate red:** fix in a new commit; no tag/release should exist.
- **Tag exists, upload failed:** rerun the same SHA. The workflow verifies the tag target and resumes; never retag.
- **Artifact/checksum mismatch:** stop. Do not publish or overwrite. Rebuild from a new reviewed commit.
- **Release already exists:** verify tag SHA and exact asset names; the workflow exits successfully only when state already matches.
- **Repository rename partially succeeded:** inspect `gh repo view Soturine/replicascan`, update `origin` and metadata, then push a consistency-only commit.
- **Pages failure:** repair the independent Pages workflow; it does not justify moving a release tag.
