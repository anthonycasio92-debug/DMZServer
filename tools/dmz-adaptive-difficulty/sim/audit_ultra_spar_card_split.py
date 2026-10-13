#!/usr/bin/env python3
"""Ultra Spar screens must parse tab-encoded SparGuiApi cards (not pipe)."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/ultra/UltraSparScreens.java"


def main() -> int:
    errors: list[str] = []
    if not SRC.is_file():
        print("FAIL missing UltraSparScreens.java")
        return 1
    text = SRC.read_text(encoding="utf-8")
    if "static String[] splitCard" not in text:
        errors.append("UltraSparScreens: missing splitCard() helper")
    if 'card.split("\\\\|")' in text or 'card.split("|")' in text or 'card.split("\\|")' in text:
        errors.append("UltraSparScreens: still splits cards on pipe — use splitCard (tab-encoded)")
    if "splitCard(card)" not in text:
        errors.append("UltraSparScreens: pending/release/war card loops must call splitCard(card)")
    # SparringSystem encodes with tab.
    spar = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.java"
    if spar.is_file():
        st = spar.read_text(encoding="utf-8")
        if '"\\t"' not in st and "'\\t'" not in st and "+ \"\\t\" +" not in st and '+"\\t"+' not in st:
            # looser check
            if "\\t" not in st:
                errors.append("SparringSystem: expected tab-encoded pendingMentorInviteCards")
    print("=== Ultra Spar card split (tab, not pipe) ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  UltraSparScreens.splitCard used; no card.split('|')")
    return 0


if __name__ == "__main__":
    sys.exit(main())
