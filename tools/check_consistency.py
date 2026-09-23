#!/usr/bin/env python3
"""Validate ReplicaScan version, identity, license and release intent."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


PRODUCT = "ReplicaScan"
PACKAGE = "com.soturine.replicascan"
REPOSITORY = "Soturine/replicascan"


def require(condition: bool, message: str, errors: list[str]) -> None:
    if not condition:
        errors.append(message)


def catalog_value(text: str, key: str) -> str | None:
    match = re.search(rf'^\s*{re.escape(key)}\s*=\s*"([^"]+)"', text, re.MULTILINE)
    return match.group(1) if match else None


def validate_manifest(manifest: dict, version: str, version_code: int) -> list[str]:
    errors: list[str] = []
    require(manifest.get("product") == PRODUCT, "release product mismatch", errors)
    require(manifest.get("version") == version, "release version mismatch", errors)
    require(manifest.get("versionCode") == version_code, "release versionCode mismatch", errors)
    require(manifest.get("repository") == REPOSITORY, "release repository mismatch", errors)
    require(isinstance(manifest.get("publish"), bool), "release publish must be boolean", errors)
    return errors


def hardcoded_versions(workflow: str) -> list[str]:
    """Versions written into the workflow, ignoring `# vX.Y.Z` comments on SHA-pinned actions."""
    lines = [line for line in workflow.splitlines() if not re.search(r"uses: \S+@[0-9a-f]{40}", line)]
    return re.findall(r"(?<![@\w.-])v?\d+\.\d+\.\d+(?![\w.])", "\n".join(lines))


def validate(root: Path) -> list[str]:
    errors: list[str] = []
    manifest = json.loads((root / "release/manifest.json").read_text(encoding="utf-8"))
    version = str(manifest.get("version", ""))
    catalog = (root / "gradle/libs.versions.toml").read_text(encoding="utf-8")
    catalog_version = catalog_value(catalog, "versionName") or ""
    code_raw = catalog_value(catalog, "versionCode")
    code = int(code_raw) if code_raw and code_raw.isdigit() else -1
    errors.extend(validate_manifest(manifest, catalog_version, code))

    app_build = (root / "app/build.gradle.kts").read_text(encoding="utf-8")
    require(f'namespace = "{PACKAGE}"' in app_build, "app namespace mismatch", errors)
    require(f'applicationId = "{PACKAGE}"' in app_build, "applicationId mismatch", errors)
    settings = (root / "settings.gradle.kts").read_text(encoding="utf-8")
    require('rootProject.name = "ReplicaScan"' in settings, "rootProject name mismatch", errors)

    strings = (root / "app/src/main/res/values/strings.xml").read_text(encoding="utf-8")
    require(re.search(r'<string name="app_name"[^>]*>ReplicaScan</string>', strings) is not None, "app_name mismatch", errors)

    license_text = (root / "LICENSE").read_text(encoding="utf-8")
    require("Proprietary License" in license_text, "current LICENSE is not proprietary", errors)
    require("all rights reserved" in license_text.lower(), "LICENSE lacks All Rights Reserved", errors)
    for path in ("LICENSING.md", "THIRD_PARTY_NOTICES.md", f"docs/releases/v{version}.md"):
        require((root / path).is_file(), f"missing {path}", errors)

    readme = (root / "README.md").read_text(encoding="utf-8")
    require(REPOSITORY in readme, "README repository URL mismatch", errors)
    require("código corrente" in readme.lower() and "proprietário" in readme.lower(), "README license disclosure missing", errors)
    require(f"v{version}" in readme, "README does not mention the release version", errors)
    changelog = (root / "CHANGELOG.md").read_text(encoding="utf-8")
    require(f"[{version}]" in changelog, "CHANGELOG has no entry for the release version", errors)

    workflow = (root / ".github/workflows/release.yml").read_text(encoding="utf-8")
    require(not hardcoded_versions(workflow), "release workflow hardcodes a version", errors)
    require("tools/release_artifact.py" in workflow, "release workflow must use tools/release_artifact.py", errors)
    require("api36DebugAndroidTest" in workflow, "API 36 managed-device gate missing", errors)
    require("gh release create" not in workflow and "git tag" not in workflow,
            "release workflow must not create tags or releases (owner publishes; CI verifies)", errors)
    return errors


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    errors = validate(root)
    if errors:
        print("Consistency gate failed:")
        print("\n".join(f"- {error}" for error in errors))
        return 1
    manifest = json.loads((root / "release/manifest.json").read_text(encoding="utf-8"))
    print(f"Consistency gate passed for ReplicaScan {manifest['version']} ({manifest['versionCode']}).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
