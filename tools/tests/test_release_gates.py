from __future__ import annotations

import importlib.util
import tempfile
import unittest
from pathlib import Path


TOOLS = Path(__file__).resolve().parents[1]


def load_module(name: str):
    spec = importlib.util.spec_from_file_location(name, TOOLS / f"{name}.py")
    assert spec and spec.loader
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class ReleaseGateTests(unittest.TestCase):
    def test_branding_gate_detects_injected_legacy_name(self) -> None:
        gate = load_module("check_branding")
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            legacy = "Scan" + "ora"
            (root / "current.md").write_text(f"Current product: {legacy}\n", encoding="utf-8")
            findings = gate.scan(root, ["current.md"])
        self.assertEqual(len(findings), 1)
        self.assertIn("current.md:1", findings[0])

    def test_consistency_gate_rejects_version_drift(self) -> None:
        gate = load_module("check_consistency")
        manifest = {
            "product": gate.PRODUCT,
            "version": "0.4.1",
            "versionCode": 19,
            "repository": gate.REPOSITORY,
            "publish": True,
        }
        errors = gate.validate_manifest(manifest, "0.4.0", 19)
        self.assertIn("release version mismatch", errors)


if __name__ == "__main__":
    unittest.main()
