# Release runbook

Decision record: [ADR 0003](adr/0003-owner-published-evaluation-releases.md).

## Evaluation release (GitHub)

1. Align `gradle/libs.versions.toml`, `release/manifest.json`, `CHANGELOG.md`, `README.md` and `docs/releases/v<version>.md`.
2. Run the local qualification from `AGENTS.md` once, on the commit to be released.
3. Push `main`. The *ReplicaScan Release* workflow qualifies that SHA asynchronously; check it once, do not poll.
4. Prepare the artifact from the build you just qualified — never a rebuild:

   ```powershell
   python tools/release_artifact.py prepare --source-sha (git rev-parse HEAD)
   ```

5. Tag and publish exactly the three prepared assets:

   ```powershell
   git tag -a v<version> <sha> -m "ReplicaScan v<version>"
   git push origin v<version>
   gh release create v<version> dist/* --verify-tag --title "ReplicaScan v<version>" --notes-file docs/releases/v<version>.md
   ```

6. The tag push runs `verify-published`, which downloads the public assets and checks names, checksum, metadata, badging and the tagged commit.

## Invariants

- Manifest product/version/code match the Android build and the APK badging (`com.soturine.replicascan`).
- Public assets are exactly `replicascan-v<version>-debug.apk`, `SHA256SUMS.txt` and `build-metadata.json`, and `build-metadata.json.sourceSha` is the tagged commit.
- CI never creates or moves tags and never uploads release assets.
- A pending or failed remote run is reported as such in the release notes, never as green.

## Recovery

- **Local gate red:** fix in a new commit; nothing is tagged.
- **Remote qualification red after publishing:** do not move the tag or replace assets. Fix forward in the next patch version and document the finding.
- **`verify-published` red:** the public release does not match what was qualified. Mark the release as pre-release/draft, investigate, and publish a new version; never overwrite assets.
- **Tag exists but release upload failed:** re-run `gh release create` (or `gh release upload`) with the same prepared `dist/`; `prepare` output must be byte-identical.
- **Pages failure:** repair the independent Pages workflow; it never justifies moving a release tag.

## Play production (not covered here)

Requires, at minimum: signed AAB with Play signing, R8 enabled with justified keep rules, stable API 36 automation, physical QA (camera, TalkBack, 200% font, RTL, document and OCR corpus), Data safety form and store assets.
