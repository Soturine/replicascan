# ADR 0002 — Asynchronous fail-closed releases

**Status:** accepted — 2026-08-31

## Context

Waiting interactively for long Android and emulator jobs wastes agent time and can lead to premature claims. Rebuilding after validation also breaks artifact provenance.

## Decision

`release/manifest.json` is the versioned release intent. A single GitHub Actions run builds the evaluation APK once, records its SHA-256, validates independent quality/security/site/API 36 gates, downloads that exact artifact and only then creates an annotated tag and GitHub Release. Publication uses least privilege and is idempotent.

The Codex agent does not poll CI. It pushes the exact release-intent SHA, queries the initial workflow state once and hands control to GitHub.

## Consequences

A red or cancelled gate produces no tag. If upload fails after tagging, rerunning for the same SHA reuses the tag and artifact but never moves or overwrites them. API 35 remains scheduled compatibility coverage rather than a release blocker.
