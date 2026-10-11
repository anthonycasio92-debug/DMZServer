#!/usr/bin/env python3
"""Ensure player menus and /lm routing do not require the Bukkit GUI plugin."""
from __future__ import annotations

import re
import subprocess
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
        if "GuiBackend.fromConfig()" not in text and "GuiBackend.CNPC" not in text:
            errors.append(f"{rel}: expected GuiBackend routing")

    backend = BACKEND.read_text(encoding="utf-8")
    if "ULTRA" not in backend:
        errors.append("GuiBackend.java: missing ULTRA backend")
    if "CNPC" not in backend:
        errors.append("GuiBackend.java: CNPC must remain a valid explicit backend")
    if "unrecognized guiBackend" not in backend and "falling back" not in backend:
        errors.append("GuiBackend.java: must warn and fall back to ultra for unknown values")
    # Retired cmi/chest must not silently resolve to CNPC anymore (config migrates them).
    if re.search(r'case\s+"cmi".*->\s*CNPC', backend, re.S):
        errors.append("GuiBackend.java: cmi must not map to CNPC (migrate to ultra in config)")

    config = (ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/config/DifficultyConfig.java").read_text(
        encoding="utf-8"
    )
    if 'guiBackend = "ultra"' not in config and "guiBackend = \"ultra\"" not in config:
        errors.append("DifficultyConfig: default guiBackend must be ultra")
    if "migrated guiBackend" not in config:
        errors.append("DifficultyConfig: must migrate retired cmi/chest guiBackend to ultra")

    net = (ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/net/gui/LmGuiNetwork.java").read_text(
        encoding="utf-8"
    )
    if "isVersionAtLeast" not in net or "1.2.5" not in net:
        errors.append("LmGuiNetwork: must compare KaiPath version >= 1.2.5")
    if 'equals("1.2.5")' in net or "equals(\"1.2.5\")" in net:
        errors.append("LmGuiNetwork: must not use exact equals(\"1.2.5\") for KaiPath")

    mods_toml = (ROOT / "src/main/resources/META-INF/mods.toml").read_text(encoding="utf-8")
    if 'modId = "kaipath"' not in mods_toml:
        errors.append("mods.toml: missing soft dependency on kaipath")
    elif "[1.2.5,)" not in mods_toml:
        errors.append("mods.toml: kaipath versionRange must be [1.2.5,)")

    ultra = GUI / "ultra" / "UltraGui.java"
    if not ultra.is_file():
        errors.append("Missing gui/ultra/UltraGui.java")
    else:
        utext = ultra.read_text(encoding="utf-8")
        if "preferUltra" not in utext or "clientHasKaiPath" not in utext:
            errors.append("UltraGui.java: must soft-fall back when KaiPath is missing")
        if "CnpcLmGui" not in utext:
            errors.append("UltraGui.java: missing CNPC fallback path")

    mechanics = (ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/command/MechanicsCommands.java").read_text(
        encoding="utf-8"
    )
    if "CmiGuiBridge" in mechanics or "BukkitGuiBridge" in mechanics:
        errors.append("MechanicsCommands.java: /lm must not require Bukkit GUI plugin")

    # Forge menu entrypoints should open CNPC or chat — not reflect into plugin.
    for name in ("DifficultyMenu", "RivalMenu", "SparMenu", "PrestigeMenu", "CharacterServicesMenu"):
        path = GUI / f"{name}.java"
        if not path.is_file():
            errors.append(f"Missing {name}.java")
            continue
        body = path.read_text(encoding="utf-8")
        if re.search(r"CmiGuiBridge\s*\.\s*open", body):
            errors.append(f"{name}.java: still opens CmiGuiBridge")

    ultra_actions = ROOT / "sim" / "audit_ultra_actions.py"
    if ultra_actions.is_file():
        r = subprocess.run([sys.executable, str(ultra_actions)], check=False)
        if r.returncode != 0:
            errors.append("audit_ultra_actions.py failed")
    else:
        errors.append("missing sim/audit_ultra_actions.py")

    print("=== Forge GUI backend (no Bukkit plugin dependency) ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  *Menu.java routes Ultra/CNPC; cmi/chest migrate to ultra; KaiPath >= 1.2.5")
    print("  OK  Ultra actions + panel bounds audit")
    return 0


if __name__ == "__main__":
    sys.exit(main())
