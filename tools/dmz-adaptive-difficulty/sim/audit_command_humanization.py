#!/usr/bin/env python3
"""Command trees should use LmCommandMessages for common denials (no robotic literals)."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CMD = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/command"

FORBIDDEN = [
    '"Usage:',
    '"Players only."',
    '"Staff only."',
    '"No permission (need op or difficulty.admin)."',
    '"Player not online:',
]

ALLOWLIST_FILES = {"LmCommandMessages.java"}


def main() -> int:
    errors: list[str] = []
    for path in sorted(CMD.glob("*.java")):
        if path.name in ALLOWLIST_FILES:
            continue
        text = path.read_text(encoding="utf-8")
        for token in FORBIDDEN:
            if token in text:
                errors.append(f"{path.name}: still contains {token!r}")
    out = ROOT / "sim/out/command-humanization-audit.md"
    out.parent.mkdir(parents=True, exist_ok=True)
    if errors:
        out.write_text("# Command humanization audit\n\nFAIL\n\n" + "\n".join(f"- {e}" for e in errors) + "\n")
        print("\n".join(errors), file=sys.stderr)
        return 1
    out.write_text("# Command humanization audit\n\nPASS — shared LmCommandMessages for common lines\n")
    print("PASS — command humanization audit")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
