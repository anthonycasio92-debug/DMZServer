#!/usr/bin/env python3
"""Fail if gui/ultra screen builders embed raw section-sign color codes.

UltraStyle may contain the mark (built at runtime). Every other ultra classfile
must have zero UTF-8 § (C2 A7) bytes — colors go through UltraStyle constants
without compile-time folding into callers.
"""
from __future__ import annotations

import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/ultra"
MARK = "\u00a7".encode("utf-8")  # C2 A7


def audit_sources() -> list[str]:
    errors: list[str] = []
    if not SRC.is_dir():
        return [f"Missing {SRC}"]
    for path in sorted(SRC.glob("*.java")):
        text = path.read_text(encoding="utf-8")
        for i, line in enumerate(text.splitlines(), 1):
            if MARK.decode("utf-8") in line:
                errors.append(f"{path.name}:{i}: bare section-sign — use UltraStyle / char 0xA7 helper")
    return errors


def audit_jar(jar: Path) -> list[str]:
    errors: list[str] = []
    if not jar.is_file():
        return [f"Missing jar {jar}"]
    with zipfile.ZipFile(jar) as zf:
        for name in sorted(zf.namelist()):
            if not name.endswith(".class"):
                continue
            if "/gui/ultra/" not in name.replace("\\", "/"):
                continue
            short = name.rsplit("/", 1)[-1]
            data = zf.read(name)
            n = data.count(MARK)
            # UltraStyle builds the mark at runtime from char 0xA7 — callers must have zero.
            if short.startswith("UltraStyle"):
                continue
            if n:
                errors.append(f"{short}: {n} hardcoded section-sign byte(s) — use UltraStyle constants")
    return errors


def main() -> int:
    errors = audit_sources()
    jar_arg = Path(sys.argv[1]) if len(sys.argv) > 1 else None
    if jar_arg is None:
        # Prefer newest LegacyMechanics jar under repo mods/
        mods = ROOT.parents[1] / "mods"
        jars = sorted(mods.glob("LegacyMechanics-*.jar"), key=lambda p: p.name)
        jar_arg = jars[-1] if jars else None
    if jar_arg is not None:
        errors.extend(audit_jar(jar_arg))
    print("=== Ultra style codes (no bare § outside UltraStyle) ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  ultra screens use UltraStyle only; jar has no folded § in callers")
    return 0


if __name__ == "__main__":
    sys.exit(main())
