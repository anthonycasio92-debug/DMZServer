#!/usr/bin/env python3
"""Ensure player menus and /lm routing do not require the Bukkit GUI plugin."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GUI = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui"
BACKEND = GUI / "GuiBackend.java"


def main() -> int:
    errors: list[str] = []

    menu_files = sorted(p for p in GUI.glob("*Menu.java") if not p.name.endswith("ChatMenu.java"))
    for path in menu_files:
        text = path.read_text(encoding="utf-8")
        rel = path.relative_to(ROOT)
        if "CmiGuiBridge" in text or "BukkitGuiBridge" in text:
            errors.append(f"{rel}: player menu must not call Bukkit/CMI bridge")
        if "GuiBackend.CNPC" not in text and "GuiBackend.fromConfig()" not in text:
            errors.append(f"{rel}: expected GuiBackend CNPC routing")

    backend = BACKEND.read_text(encoding="utf-8")
    if "case \"cmi\"" not in backend or "-> CNPC" not in backend:
        errors.append("GuiBackend.java: legacy cmi/chest aliases must map to CNPC")
    if "ULTRA" not in backend or "case \"ultra\"" not in backend:
        errors.append("GuiBackend.java: ultra/dmzultra backend missing")

    mechanics = (ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/command/MechanicsCommands.java").read_text(
        encoding="utf-8"
    )
    if "CmiGuiBridge" in mechanics or "BukkitGuiBridge" in mechanics:
        errors.append("MechanicsCommands.java: /lm must not require Bukkit GUI plugin")

    # Forge menu entrypoints should open Ultra, CNPC, or chat — not reflect into plugin.
    for name in ("DifficultyMenu", "RivalMenu", "SparMenu", "PrestigeMenu", "CharacterServicesMenu", "MechanicsMenu"):
        path = GUI / f"{name}.java"
        if not path.is_file():
            errors.append(f"Missing {name}.java")
            continue
        body = path.read_text(encoding="utf-8")
        if re.search(r"CmiGuiBridge\s*\.\s*open", body):
            errors.append(f"{name}.java: still opens CmiGuiBridge")

    print("=== Forge GUI backend (no Bukkit plugin dependency) ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  *Menu.java routes Ultra/CNPC/chat; GuiBackend maps legacy bukkit/cmi to CNPC")
    return 0


if __name__ == "__main__":
    sys.exit(main())
