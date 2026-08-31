#!/usr/bin/env python3
"""Validate local links/assets and canonical current metadata in the static site."""

from __future__ import annotations

import re
import sys
from pathlib import Path


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    site = root / "site"
    index = (site / "index.html").read_text(encoding="utf-8")
    errors: list[str] = []
    for value in re.findall(r'(?:href|src)="([^"]+)"', index):
        if value.startswith(("http://", "https://", "#", "mailto:")):
            continue
        if not (site / value).is_file():
            errors.append(f"missing local site asset: {value}")
    for expected in ("ReplicaScan", "Versão 0.4.0", "Soturine/replicascan", "replicascan_icon.png"):
        if expected not in index:
            errors.append(f"missing site metadata: {expected}")
    if errors:
        print("Site gate failed:")
        print("\n".join(f"- {error}" for error in errors))
        return 1
    print("Site gate passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
