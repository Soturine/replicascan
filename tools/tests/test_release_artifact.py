from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("release_artifact", TOOLS / "release_artifact.py")
assert spec and spec.loader
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)

MANIFEST = {"product": "ReplicaScan", "version": "9.8.7", "versionCode": 42, "repository": "Soturine/replicascan", "publish": True}
SHA = "a" * 40


def write_release(directory: Path, manifest: dict = MANIFEST, source_sha: str = SHA, payload: bytes = b"apk") -> None:
    apk = directory / release.artifact_name(manifest)
    apk.write_bytes(payload)
    digest = release.sha256(apk)
    (directory / "SHA256SUMS.txt").write_text(f"{digest}  {apk.name}\n", encoding="utf-8")
    (directory / "build-metadata.json").write_text(
        json.dumps(release.build_metadata(manifest, source_sha, digest)), encoding="utf-8",
    )


class ReleaseArtifactTests(unittest.TestCase):
    def test_names_are_derived_from_the_manifest(self) -> None:
        self.assertEqual(release.tag_name(MANIFEST), "v9.8.7")
        self.assertEqual(release.artifact_name(MANIFEST), "replicascan-v9.8.7-debug.apk")

    def test_matching_badging_passes(self) -> None:
        badging = release.parse_badging("package: name='com.soturine.replicascan' versionCode='42' versionName='9.8.7' platformBuildVersionName='16'")
        self.assertEqual(release.check_identity(badging, MANIFEST), [])

    def test_version_mismatch_fails(self) -> None:
        badging = {"package": release.PACKAGE, "versionCode": 42, "versionName": "9.8.6"}
        self.assertTrue(any("versionName" in e for e in release.check_identity(badging, MANIFEST)))

    def test_version_code_mismatch_fails(self) -> None:
        badging = {"package": release.PACKAGE, "versionCode": 41, "versionName": "9.8.7"}
        self.assertTrue(any("versionCode" in e for e in release.check_identity(badging, MANIFEST)))

    def test_package_mismatch_fails(self) -> None:
        badging = {"package": "com.example.other", "versionCode": 42, "versionName": "9.8.7"}
        self.assertTrue(any("package" in e for e in release.check_identity(badging, MANIFEST)))

    def test_correct_published_state_verifies_and_is_idempotent(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            write_release(root)
            self.assertEqual(release.verify_dir(root, MANIFEST, SHA), [])
            self.assertEqual(release.verify_dir(root, MANIFEST, SHA), [])

    def test_checksum_mismatch_fails(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            write_release(root)
            (root / release.artifact_name(MANIFEST)).write_bytes(b"rebuilt apk")
            errors = release.verify_dir(root, MANIFEST, SHA)
            self.assertTrue(any("checksum" in e for e in errors))

    def test_wrong_tag_sha_fails(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            write_release(root, source_sha="b" * 40)
            errors = release.verify_dir(root, MANIFEST, SHA)
            self.assertTrue(any("sourceSha" in e for e in errors))

    def test_extra_or_missing_asset_fails(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            write_release(root)
            (root / "other.apk").write_bytes(b"x")
            self.assertTrue(release.verify_dir(root, MANIFEST, SHA))

    def test_assets_from_another_version_fail(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            write_release(root, manifest={**MANIFEST, "version": "9.8.6"})
            self.assertTrue(release.verify_dir(root, MANIFEST, SHA))


class ConsistencyGateTests(unittest.TestCase):
    def setUp(self) -> None:
        spec = importlib.util.spec_from_file_location("check_consistency", TOOLS / "check_consistency.py")
        assert spec and spec.loader
        self.gate = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.gate)

    def test_repository_is_consistent(self) -> None:
        self.assertEqual(self.gate.validate(TOOLS.parent), [])

    def test_workflow_version_literals_are_detected_but_action_pins_are_not(self) -> None:
        pinned = "      uses: actions/checkout@" + "f" * 40 + " # v5.1.0\n"
        self.assertEqual(self.gate.hardcoded_versions(pinned), [])
        self.assertEqual(self.gate.hardcoded_versions(pinned + "  TAG: v0.4.0\n"), ["v0.4.0"])

    def test_version_code_drift_fails(self) -> None:
        errors = self.gate.validate_manifest({**MANIFEST, "product": self.gate.PRODUCT, "repository": self.gate.REPOSITORY}, "9.8.7", 43)
        self.assertIn("release versionCode mismatch", errors)


if __name__ == "__main__":
    unittest.main()
