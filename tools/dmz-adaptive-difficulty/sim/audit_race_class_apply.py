#!/usr/bin/env python3
"""Static audit: race/class apply wiring vs intended DMZ flows."""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src" / "main" / "java" / "com" / "dbzlegacy" / "adaptivedifficulty"
CHAR = SRC / "character"
MIXIN = SRC / "mixin"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def check(name: str, ok: bool, errors: list[str]) -> None:
    if not ok:
        errors.append(name)


def main() -> int:
    errors: list[str] = []

    hooks = read(CHAR / "DmzCharacterClassChangeHooks.java")
    services = read(CHAR / "CharacterServicesSystem.java")
    apply = read(CHAR / "DmzClassCommandApply.java")
    pick = read(CHAR / "RaceChangeClassPickFlow.java")
    create = read(CHAR / "RaceChangeCreationFlow.java")
    update_mixin = read(MIXIN / "UpdateCharacterC2SMixin.java")
    create_mixin = read(MIXIN / "CreateCharacterC2SMixin.java")

    check("DmzClassCommandApply mirrors dmzclass steps", "restoreMultiplierGains" in apply and "StatsSyncS2C" in apply, errors)
    check("Class validation uses ConfigManager getAllClasses contains", "getAllClasses" in apply and "contains" in apply, errors)
    check("Paid class snapshots before mutate", re.search(
        r"resourceSnapshot\s*=\s*data\.snapshotMultiplierResources\(\);\s*\n\s*if \(cost", services, re.M
    ) is not None, errors)
    check("Auto race remap snapshots before applyRaceAndFightingClass", re.search(
        r"float\[\]\s+resourceSnapshot\s*=\s*data\.snapshotMultiplierResources\(\);\s*\n\s*String\s+mappedClass\s*=\s*\n\s*RaceChangeClassMapper\.applyRaceAndFightingClass",
        services,
        re.M,
    ) is not None, errors)
    check("Auto race uses onServicesRaceChangeApplied", "onServicesRaceChangeApplied" in services, errors)
    check("Class picker stores resource backup", "pickerResourceSnapshot" in services and "resourceSnapshot" in pick, errors)
    check("Class pick finish passes prior fallback", "onServicesRaceChangeApplied" in hooks and "prior" in hooks, errors)
    check("UpdateCharacter captures snap at HEAD for pick session", "RaceChangeClassPickFlow.isActive" in update_mixin and "DmzClassChangeCapture.store" in update_mixin, errors)
    check("CreateCharacter captures snap at HEAD when active", "DmzClassChangeCapture.store" in create_mixin and "RaceChangeCreationFlow.isActive" in create_mixin, errors)
    check("Mixin targets lambda handle not handle return", 'method = "lambda$handle$0"' in update_mixin and 'method = "lambda$handle$0"' in create_mixin, errors)
    check("0% wipe uses onServicesRaceChangeApplied on create", "onServicesRaceChangeApplied" in create, errors)
    check("Packet guards canonicalize class", "canonicalPacketClass" in read(CHAR / "RaceChangeClassPickPacketGuard.java"), errors)

    mapper = subprocess.run(
        [sys.executable, str(ROOT / "sim" / "test_race_class_mapper.py")],
        capture_output=True,
        text=True,
    )
    check("test_race_class_mapper.py passes", mapper.returncode == 0, errors)
    if mapper.returncode != 0:
        errors.append(mapper.stdout + mapper.stderr)

    out = ROOT / "sim" / "out" / "race-class-apply-audit.md"
    out.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        "# Race / class apply audit",
        "",
        "## Flow matrix (intended)",
        "",
        "| Flow | Snapshot timing | Class apply | Sync |",
        "|------|-----------------|-------------|------|",
        "| Paid class change | Before commit | `/dmzclass` via `onPaidClassChange` | Stats + Fabled perms |",
        "| Race auto-remap | Before race+class commit | `onServicesRaceChangeApplied` | dmzclass + transform limits + appearance |",
        "| Race + class picker | At race change + UpdateCharacter HEAD | On recustomize complete | same |",
        "| 0% race wipe | CreateCharacter HEAD | `onCharacterCreated` → race hook | same |",
        "",
        "## DMZ reference (`/dmzclass`)",
        "",
        "- `isValidClass` → `getAllClasses().contains(lowercase)`",
        "- `snapshotMultiplierResources` → `setCharacterClass` → `restoreMultiplierGains` → `StatsSyncS2C`",
        "",
        "## Static checks",
        "",
    ]
    if errors:
        lines.append("**FAIL** — issues:")
        for e in errors:
            lines.append(f"- {e}")
    else:
        lines.append("**PASS** — wiring and mapper sim OK.")
    lines.append("")
    out.write_text("\n".join(lines), encoding="utf-8")
    print(out.read_text(encoding="utf-8"))

    if errors:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
