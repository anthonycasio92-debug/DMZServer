#!/usr/bin/env python3
"""LM GUI copy: use TP (not teleport / TP Msg / TP messages) on player-facing menus."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MOD_GUI = ROOT / "tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/gui"
CNPC = MOD_GUI / "cnpc"
BUKKIT = ROOT / "tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit"
TOOLTIPS = ROOT / "tools/dmz-adaptive-difficulty-gui/src/main/resources/gui-tooltips.json"
MOD = ROOT / "tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty"
EXTRA = [
    MOD / "sparring/SparringSystem.java",
    MOD / "rival/RivalSystem.java",
    MOD / "progression/end/EndPortalGuard.java",
    MOD / "progression/end/EndDimensionStrength.java",
]

SKIP_FILES = {
    "CnpcGuiOpener.java",  # CNPC mod ids (npcteleporter)
}

FORBIDDEN = [
    (re.compile(r"Teleport", re.I), "use TP instead of Teleport"),
    (re.compile(r"TP\s+Msg\b"), "use TP ON/OFF instead of TP Msg"),
    (re.compile(r"TP\s+Messages\b", re.I), "use TP ON/OFF instead of TP Messages"),
    (re.compile(r"TP\s+messages\b"), "use TP chat ON/OFF instead of TP messages"),
]


def scan_file(path: Path) -> list[str]:
    if path.name in SKIP_FILES:
        return []
    text = path.read_text(encoding="utf-8")
    issues = []
    for i, line in enumerate(text.splitlines(), 1):
        stripped = line.strip()
        if stripped.startswith("//") or stripped.startswith("*"):
            continue
        for pat, hint in FORBIDDEN:
            if pat.search(line):
                issues.append(f"{path.relative_to(ROOT)}:{i}: {hint} — {stripped[:100]}")
                break
    return issues


def main() -> int:
    paths: list[Path] = []
    if TOOLTIPS.is_file():
        paths.append(TOOLTIPS)
    for base in (CNPC, MOD_GUI, BUKKIT):
        if base.is_dir():
            paths.extend(sorted(base.rglob("*.java")))
    for p in EXTRA:
        if p.is_file():
            paths.append(p)

    all_issues: list[str] = []
    for p in paths:
        all_issues.extend(scan_file(p))

    print("=== LM GUI TP copy audit ===")
    if all_issues:
        print("FAIL — forbidden teleport/TP Msg phrasing in menus:")
        for issue in all_issues:
            print(f"  - {issue}")
        return 1
    print("PASS — no Teleport / TP Msg / TP messages in LM GUI surfaces")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
