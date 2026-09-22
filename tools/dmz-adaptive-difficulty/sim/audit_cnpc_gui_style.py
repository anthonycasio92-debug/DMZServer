#!/usr/bin/env python3
"""Fail-closed style checks for CustomNPCs Legacy Mechanics menus."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CNPC = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/cnpc"

LEGACY_HEADER = re.compile(r"CnpcGuiSupport\.title\s*\(\s*gui")
DOUBLE_SEP = re.compile(r"  §8·  ")
LOWERCASE_STAFF_ADMIN = re.compile(r"Staff admin")
SYSTEM_DISABLED = re.compile(r"system is disabled", re.I)


def main() -> int:
    errors: list[str] = []
    if not CNPC.is_dir():
        print(f"Missing {CNPC}", file=sys.stderr)
        return 1

    for path in sorted(CNPC.glob("CnpcLm*.java")):
        text = path.read_text(encoding="utf-8")
        rel = path.relative_to(ROOT)
        if LEGACY_HEADER.search(text):
            errors.append(f"{rel}: uses legacy title() header (use paintHeader)")
        if DOUBLE_SEP.search(text):
            errors.append(f"{rel}: double-spaced §8· separator (use CnpcGuiStyle.SEP)")
        if LOWERCASE_STAFF_ADMIN.search(text):
            errors.append(f"{rel}: use 'Staff Admin' capitalization")
        if SYSTEM_DISABLED.search(text):
            errors.append(f"{rel}: use CnpcGuiStyle.MSG_* for disabled systems")

    style = CNPC / "CnpcGuiStyle.java"
    if not style.is_file():
        errors.append("Missing CnpcGuiStyle.java")

    if errors:
        print("=== CNPC GUI style audit ===")
        for e in errors:
            print(f"  FAIL  {e}")
        print(f"\n{len(errors)} error(s)")
        return 1

    print("=== CNPC GUI style audit ===")
    print("  OK  headers, separators, disabled copy")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
