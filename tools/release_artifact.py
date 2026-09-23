#!/usr/bin/env python3
"""Prepare and verify the ReplicaScan evaluation release artifact.

`release/manifest.json` is the single source for product, version, versionCode and repository;
the tag (`v<version>`) and artifact name are derived from it.

    prepare  copy the qualified APK into dist/, check its badging, write SHA256SUMS.txt and
             build-metadata.json. Never rebuilds anything.
    verify   check a directory of release assets (local dist/ or downloaded from GitHub) against
             the manifest, the checksums and the commit the tag points to.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "com.soturine.replicascan"
ARTIFACT_TYPE = "evaluation-debug-apk"


def load_manifest(root: Path = ROOT) -> dict:
    return json.loads((root / "release/manifest.json").read_text(encoding="utf-8"))


def tag_name(manifest: dict) -> str:
    return f"v{manifest['version']}"


def artifact_name(manifest: dict) -> str:
    return f"replicascan-{tag_name(manifest)}-debug.apk"


def expected_assets(manifest: dict) -> list[str]:
    return sorted([artifact_name(manifest), "SHA256SUMS.txt", "build-metadata.json"])


def parse_badging(text: str) -> dict:
    match = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", text)
    if not match:
        return {}
    return {"package": match.group(1), "versionCode": int(match.group(2)), "versionName": match.group(3)}


def check_identity(badging: dict, manifest: dict) -> list[str]:
    errors = []
    if badging.get("package") != PACKAGE:
        errors.append(f"package mismatch: {badging.get('package')} != {PACKAGE}")
    if badging.get("versionName") != manifest["version"]:
        errors.append(f"versionName mismatch: {badging.get('versionName')} != {manifest['version']}")
    if badging.get("versionCode") != manifest["versionCode"]:
        errors.append(f"versionCode mismatch: {badging.get('versionCode')} != {manifest['versionCode']}")
    return errors


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1 << 20), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build_metadata(manifest: dict, source_sha: str, artifact_sha: str) -> dict:
    return {
        "product": manifest["product"],
        "version": manifest["version"],
        "versionCode": manifest["versionCode"],
        "package": PACKAGE,
        "sourceSha": source_sha,
        "tag": tag_name(manifest),
        "artifact": artifact_name(manifest),
        "artifactSha256": artifact_sha,
        "artifactType": ARTIFACT_TYPE,
    }


def verify_dir(directory: Path, manifest: dict, tag_sha: str, badging: dict | None = None) -> list[str]:
    """Return every problem found; an empty list means the assets are exactly the qualified release."""
    errors: list[str] = []
    present = sorted(p.name for p in directory.iterdir() if p.is_file())
    if present != expected_assets(manifest):
        errors.append(f"unexpected asset set: {present} != {expected_assets(manifest)}")
        return errors
    apk = directory / artifact_name(manifest)
    actual = sha256(apk)
    sums = (directory / "SHA256SUMS.txt").read_text(encoding="utf-8").split()
    if sums[:2] != [actual, artifact_name(manifest)]:
        errors.append("checksum mismatch between APK and SHA256SUMS.txt")
    metadata = json.loads((directory / "build-metadata.json").read_text(encoding="utf-8"))
    expected = build_metadata(manifest, tag_sha, actual)
    for key, value in expected.items():
        if metadata.get(key) != value:
            errors.append(f"metadata {key} mismatch: {metadata.get(key)!r} != {value!r}")
    if badging is not None:
        errors.extend(check_identity(badging, manifest))
    return errors


def find_aapt() -> str:
    home = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not home:
        local = ROOT / "local.properties"
        if local.exists():
            match = re.search(r"sdk\.dir=(.+)", local.read_text(encoding="utf-8"))
            if match:
                home = match.group(1).strip().replace("\\:", ":").replace("\\\\", "\\")
    if not home:
        raise SystemExit("ANDROID_HOME is not set")
    candidates = sorted(Path(home, "build-tools").glob("*/aapt*"), key=lambda p: [int(x) for x in re.findall(r"\d+", p.parent.name)])
    candidates = [c for c in candidates if c.stem == "aapt"]
    if not candidates:
        raise SystemExit("aapt not found in build-tools")
    return str(candidates[-1])


def read_badging(apk: Path) -> dict:
    output = subprocess.run([find_aapt(), "dump", "badging", str(apk)], check=True, capture_output=True, text=True).stdout
    return parse_badging(output)


def prepare(apk: Path, out: Path, source_sha: str, manifest: dict) -> list[str]:
    badging = read_badging(apk)
    errors = check_identity(badging, manifest)
    if errors:
        return errors
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
    target = out / artifact_name(manifest)
    shutil.copyfile(apk, target)
    digest = sha256(target)
    (out / "SHA256SUMS.txt").write_text(f"{digest}  {target.name}\n", encoding="utf-8")
    (out / "build-metadata.json").write_text(json.dumps(build_metadata(manifest, source_sha, digest), indent=2) + "\n", encoding="utf-8")
    return verify_dir(out, manifest, source_sha, badging)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    p = sub.add_parser("prepare")
    p.add_argument("--apk", type=Path, default=ROOT / "app/build/outputs/apk/debug/app-debug.apk")
    p.add_argument("--out", type=Path, default=ROOT / "dist")
    p.add_argument("--source-sha", required=True)
    v = sub.add_parser("verify")
    v.add_argument("--dir", type=Path, required=True)
    v.add_argument("--tag-sha", required=True)
    v.add_argument("--badging", action="store_true", help="also run aapt on the APK")
    sub.add_parser("names", help="print tag and artifact name derived from the manifest")
    args = parser.parse_args(argv)
    manifest = load_manifest()

    if args.command == "names":
        print(f"tag={tag_name(manifest)}")
        print(f"artifact={artifact_name(manifest)}")
        print(f"version={manifest['version']}")
        print(f"versionCode={manifest['versionCode']}")
        return 0
    if args.command == "prepare":
        errors = prepare(args.apk, args.out, args.source_sha, manifest)
    else:
        badging = read_badging(args.dir / artifact_name(manifest)) if args.badging else None
        errors = verify_dir(args.dir, manifest, args.tag_sha, badging)
    if errors:
        print("Release artifact check failed:")
        print("\n".join(f"- {error}" for error in errors))
        return 1
    print(f"Release artifact OK: {artifact_name(manifest)} for {tag_name(manifest)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
