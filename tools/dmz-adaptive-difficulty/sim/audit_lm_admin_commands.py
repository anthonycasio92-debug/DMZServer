#!/usr/bin/env python3
"""Forge command trees: one canonical path per feature; shortcuts only /diff and /padmin."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MECH = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/command/MechanicsCommands.java"
PROG = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/command/ProgressionCommands.java"
BRIDGE = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/command/MohistCommandBridge.java"

REQUIRED_LM_ADMIN = [
    "help",
    "reload",
    "migrate-cnpc",
    "clear",
    "character",
    "cooldown",
    "syslog",
    "open",
    "inspect",
    "testgui",
]

FORBIDDEN_LITERALS = [
    'm_82127_("legacymechanics")',
    'm_82127_("prog")',
    'm_82127_("prestigeadmin")',
    'm_82127_("cleardragons")',
    'm_82127_("spawndragon")',
    'm_82127_("killdragons")',
    'm_82127_("spawndragon")',
    'register(build("legacymechanics"))',
    'List.of("legacymechanics")',
    'List.of("prog")',
    'List.of("prestigeadmin")',
    'List.of("sparring")',
]

REQUIRED_FORGE_ROOTS = [
    ("padmin", PROG),
    ("progression", PROG),
    ("difficulty", ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/command/DifficultyCommands.java"),
    ("lm", MECH),
]


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []
    mech = read(MECH)
    prog = read(PROG)
    bridge = read(BRIDGE)

    for token in REQUIRED_LM_ADMIN:
        if token not in mech:
            errors.append(f"MechanicsCommands missing: {token!r}")

    if "clearCharacterCooldowns" not in mech:
        errors.append("MechanicsCommands must call PlayerDataClear.clearCharacterCooldowns")
    if "stafffree" in mech and "adminStaffFree" in mech:
        errors.append("stafffree belongs under /difficulty admin only (remove from /lm admin)")

    if "LmAdminArgCoalesce" not in bridge:
        errors.append("MohistCommandBridge must use LmAdminArgCoalesce")

    if 'm_82127_("padmin")' not in prog:
        errors.append("ProgressionCommands must register /padmin")
    if "PrestigeAdminCommandTree" not in prog:
        errors.append("ProgressionCommands must use PrestigeAdminCommandTree")
    if 'm_82127_("admin")' in prog and "PrestigeAdminCommandTree.attach(Commands.m_82127_(\"admin\"))" in prog:
        errors.append("Do not attach PrestigeAdminCommandTree under /prestige admin (use /padmin)")

    for forbidden in FORBIDDEN_LITERALS:
        hay = mech + prog + bridge
        if forbidden in hay:
            errors.append(f"Removed shortcut/duplicate still present: {forbidden}")

    if 'register(build("lm"))' not in mech and 'm_82127_("lm")' not in mech:
        errors.append("MechanicsCommands must register /lm")

    out = ROOT / "sim/out/lm-admin-command-audit.md"
    out.parent.mkdir(parents=True, exist_ok=True)
    lines = ["# LM command tree audit", ""]
    if errors:
        lines.append("FAIL")
        for e in errors:
            lines.append(f"- {e}")
        out.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print("\n".join(errors), file=sys.stderr)
        return 1
    lines.append("PASS — canonical command trees (/diff and /padmin are the only shortcuts)")
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("PASS — lm admin command audit")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
