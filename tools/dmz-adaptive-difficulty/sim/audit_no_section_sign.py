#!/usr/bin/env python3
"""Fail-closed: no raw section-sign (U+00A7) outside UltraStyle.

Standing rule for LegacyMechanics player-facing text:
  - Never embed a bare section-sign character in new Java sources.
  - Route all colors through UltraStyle constants / helpers.
  - UltraStyle.java is the only allowed owner of the mark (\\u00A7).

Enforced on:
  - gui/ultra/** (except UltraStyle.java) — already the Ultra path
  - DifficultyChatMenu.java / DifficultyActions.java — previously
    reintroduced § on feature branches; must stay at zero forever
  - DifficultyTeamGuiApi.java — reachable from Ultra via UltraGuiActions /
    UltraDifficultyScreens; colors must go through UltraStyle
"""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty"
MARK = "\u00a7"

ULTRA = SRC / "gui" / "ultra"
MUST_STAY_CLEAN = [
    SRC / "gui" / "DifficultyChatMenu.java",
    SRC / "service" / "DifficultyActions.java",
    SRC / "gui" / "DifficultyTeamGuiApi.java",
]
ALLOW_MARK = {(ULTRA / "UltraStyle.java").resolve()}


def iter_java() -> list[Path]:
    seen: set[Path] = set()
    out: list[Path] = []
    for path in MUST_STAY_CLEAN:
        if path.is_file():
            rp = path.resolve()
            if rp not in seen:
                seen.add(rp)
                out.append(path)
    if ULTRA.is_dir():
        for path in sorted(ULTRA.glob("*.java")):
            rp = path.resolve()
            if rp in seen:
                continue
            seen.add(rp)
            out.append(path)
    return out


def audit() -> list[str]:
    errors: list[str] = []
    for path in iter_java():
        if path.resolve() in ALLOW_MARK:
            continue
        text = path.read_text(encoding="utf-8")
        for i, line in enumerate(text.splitlines(), 1):
            if MARK in line:
                rel = path.relative_to(ROOT)
                errors.append(f"{rel}:{i}: raw section-sign — use UltraStyle (no bare §)")
    return errors


def main() -> int:
    errors = audit()
    print("=== No raw section-sign outside UltraStyle ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s) — standing rule: all colors via UltraStyle")
        return 1
    print(
        "  OK  DifficultyChatMenu + DifficultyActions + DifficultyTeamGuiApi "
        "+ gui/ultra: zero bare § (UltraStyle owns the mark)"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
