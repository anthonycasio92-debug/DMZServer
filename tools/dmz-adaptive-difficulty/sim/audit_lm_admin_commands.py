#!/usr/bin/env python3
"""Forge /lm admin tree must cover Bukkit handleLmAdmin subcommands (Forge-only Mohist)."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MECH = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/command/MechanicsCommands.java"
PROG = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/command/ProgressionCommands.java"

# Bukkit AdaptiveDifficultyGuiPlugin.handleLmAdmin sub + Forge-only extras
REQUIRED_LM_ADMIN = [
    "help",
    "reload",
    "migrate-cnpc",
    "migratecnpc",
    "clear",
    "character",
    "char",
    "charservices",
    "cooldown",
    "clear",
    "stafffree",
    "syslog",
    "open",
    "inspect",
    "testgui",
]

REQUIRED_FORGE_ROOTS = [
    "padmin",
    "prestigeadmin",
    "prestige",
    "progression",
    "skills",
    "skillcheck",
    "difficulty",
    "rival",
    "spar",
]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []
    mech = read(MECH)
    prog = read(PROG)

    for token in REQUIRED_LM_ADMIN:
        if token not in mech:
            errors.append(f"MechanicsCommands missing literal/handler token: {token!r}")

    if "clearCharacterCooldowns" not in mech:
        errors.append("MechanicsCommands must call PlayerDataClear.clearCharacterCooldowns")
    if "LmAdminArgCoalesce" not in (ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/command/MohistCommandBridge.java").read_text():
        errors.append("MohistCommandBridge must use LmAdminArgCoalesce")

    for root in REQUIRED_FORGE_ROOTS:
        if root == "padmin":
            if 'm_82127_("padmin")' not in prog and 'literal("padmin")' not in prog:
                errors.append("ProgressionCommands must register /padmin")
        elif root in ("prestigeadmin",):
            if 'm_82127_("prestigeadmin")' not in prog:
                errors.append("ProgressionCommands must register /prestigeadmin")

    if "PrestigeAdminCommandTree" not in prog:
        errors.append("ProgressionCommands must use PrestigeAdminCommandTree")

    out = ROOT / "sim/out/lm-admin-command-audit.md"
    out.parent.mkdir(parents=True, exist_ok=True)
    lines = ["# LM admin command audit", ""]
    if errors:
        lines.append("FAIL")
        for e in errors:
            lines.append(f"- {e}")
        out.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print("\n".join(errors), file=sys.stderr)
        return 1
    lines.append("PASS — /lm admin Forge tree covers Mohist/Bukkit admin surface")
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("PASS — lm admin command audit")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
