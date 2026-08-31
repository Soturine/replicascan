# ADR 0001 — ReplicaScan identity and licensing boundary

**Status:** accepted — 2026-08-31

## Context

The Android product and public repository need a durable identity that is independent from the historical releases published through v0.3.1. Retaining the old application ID would blur storage, package and licensing boundaries.

## Decision

- Current product and repository name: ReplicaScan / `Soturine/replicascan`.
- Android identity: `com.soturine.replicascan`; current runtime resources and managed storage namespaces use ReplicaScan.
- The v0.4.0 package installs separately. No code attempts to read, migrate, uninstall or delete the historical app or its export folder.
- Current original code is proprietary and All Rights Reserved. Historical releases through v0.3.1 retain their Apache License 2.0 grants; third-party licenses remain independent.
- External code is accepted only after a written licensing agreement.

## Alternatives

Keeping the old package would simplify in-place upgrades but contradict the requested identity boundary. Rewriting history or relicensing old tags was rejected because public historical grants are immutable.

## Consequences

Testers must export important historical documents before uninstalling the old package. Room/DataStore begin under the new application sandbox; schemas and migration invariants remain versioned. Repository redirects may help old links, but current docs and automation use the canonical new URL.
