#!/usr/bin/env python3
"""CNPC GUI package must be absent — Ultra is the only player GUI backend."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CNPC = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/cnpc"
SRC = ROOT / "src/main/java"


def main() -> int:
    errors: list[str] = []

    if CNPC.is_dir():
        java = list(CNPC.rglob("*.java"))
        errors.append(f"gui/cnpc/ must be deleted ({len(java)} .java file(s) remain)")

    # Production src must not import the removed package.
    for path in SRC.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        if "com.dbzlegacy.adaptivedifficulty.gui.cnpc" in text:
            # Allow historical comments that say "do not import gui.cnpc".
            for i, line in enumerate(text.splitlines(), 1):
                if "import com.dbzlegacy.adaptivedifficulty.gui.cnpc" in line:
                    errors.append(f"{path.relative_to(ROOT)}:{i}: imports deleted gui.cnpc")
                if "gui.cnpc." in line and "import " not in line and "does not import" not in line:
                    if "com.dbzlegacy.adaptivedifficulty.gui.cnpc." in line:
                        errors.append(
                            f"{path.relative_to(ROOT)}:{i}: references deleted gui.cnpc type"
                        )

    print("=== CNPC GUI package removed (Ultra-only) ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  gui/cnpc/ absent; no production imports")
    return 0


if __name__ == "__main__":
    sys.exit(main())
