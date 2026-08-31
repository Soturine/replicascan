#!/usr/bin/env python3
"""Reject legacy branding outside explicitly historical repository records."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path
from typing import Iterable


TEXT_SUFFIXES = {
    ".gradle", ".html", ".java", ".json", ".kt", ".kts", ".md",
    ".properties", ".pro", ".py", ".toml", ".txt", ".xml", ".yaml", ".yml",
}
HISTORICAL_FILES = {
    "CHANGELOG.md",
    "LICENSING.md",
    "docs/adr/0001-replicascan-identity.md",
    "docs/releases/v0.4.0.md",
    "docs/v0.3.1-implementation-matrix.md",
    "docs/validation-v0.3.1.md",
    "docs/visual-assets-v0.3.1.md",
}
HISTORICAL_PREFIXES = (
    "core-data/schemas/com.soturine." + "scan" + "ora.",
)


def forbidden_patterns() -> tuple[re.Pattern[str], ...]:
    legacy = "Scan" + "ora"
    old_package = "com.soturine." + legacy.lower()
    old_repo = "Soturine/" + legacy.lower()
    return (
        re.compile(re.escape(legacy), re.IGNORECASE),
        re.compile(re.escape(old_package), re.IGNORECASE),
        re.compile(re.escape(old_repo), re.IGNORECASE),
    )


def is_historical(path: str) -> bool:
    normalized = path.replace("\\", "/")
    return normalized in HISTORICAL_FILES or normalized.startswith(HISTORICAL_PREFIXES)


def repository_files(root: Path) -> list[str]:
    result = subprocess.run(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard"],
        cwd=root,
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    return [line for line in result.stdout.splitlines() if line]


def scan(root: Path, paths: Iterable[str]) -> list[str]:
    findings: list[str] = []
    patterns = forbidden_patterns()
    for relative in paths:
        normalized = relative.replace("\\", "/")
        if is_historical(normalized):
            continue
        file_path = root / relative
        if not file_path.is_file() or file_path.suffix.lower() not in TEXT_SUFFIXES:
            continue
        try:
            lines = file_path.read_text(encoding="utf-8").splitlines()
        except UnicodeDecodeError:
            continue
        for line_number, line in enumerate(lines, start=1):
            if any(pattern.search(line) for pattern in patterns):
                findings.append(f"{normalized}:{line_number}: {line.strip()}")
    return findings


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    root = args.root.resolve()
    findings = scan(root, repository_files(root))
    if findings:
        print("Legacy branding found outside the historical allowlist:")
        print("\n".join(findings))
        return 1
    print("Branding gate passed: current surfaces use ReplicaScan.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
