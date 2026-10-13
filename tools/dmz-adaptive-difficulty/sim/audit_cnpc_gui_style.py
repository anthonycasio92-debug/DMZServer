#!/usr/bin/env python3
"""CNPC GUI package must be absent — Ultra is the only player GUI backend."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
# tools/dmz-adaptive-difficulty → sibling tools/dmz-adaptive-difficulty-gui
GUI_PLUGIN = ROOT.parent / "dmz-adaptive-difficulty-gui"
REPO = GUI_PLUGIN.parent.parent if GUI_PLUGIN.is_dir() else ROOT.parent
CNPC = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/cnpc"
SRC = ROOT / "src/main/java"
BUKKIT = GUI_PLUGIN / "src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit"


def scan_java(path: Path, errors: list[str], rel_root: Path) -> None:
    text = path.read_text(encoding="utf-8")
    rel = path.relative_to(rel_root)
    for i, line in enumerate(text.splitlines(), 1):
        if "import com.dbzlegacy.adaptivedifficulty.gui.cnpc" in line:
            errors.append(f"{rel}:{i}: imports deleted gui.cnpc")
        if 'gui.cnpc.CnpcLmGui' in line or "gui.cnpc.CnpcLmGui" in line:
            # loadClass("...gui.cnpc.CnpcLmGui") would CNFE after package delete.
            if "deleted" in line or "retired" in line or "must not" in line:
                continue
            errors.append(f"{rel}:{i}: still loads deleted gui.cnpc.CnpcLmGui")
        if "com.dbzlegacy.adaptivedifficulty.gui.cnpc." in line and "import " not in line:
            if "does not import" in line or "deleted" in line or "retired" in line:
                continue
            errors.append(f"{rel}:{i}: references deleted gui.cnpc type")


def main() -> int:
    errors: list[str] = []

    if CNPC.is_dir():
        java = list(CNPC.rglob("*.java"))
        errors.append(f"gui/cnpc/ must be deleted ({len(java)} .java file(s) remain)")

    for path in SRC.rglob("*.java"):
        if "com.dbzlegacy.adaptivedifficulty.gui.cnpc" in path.read_text(encoding="utf-8"):
            scan_java(path, errors, ROOT)

    # Bukkit bridge must open UltraGui, never CnpcLmGui (package gone in 4.6.115+).
    if BUKKIT.is_dir():
        bridge = BUKKIT / "ForgeBridge.java"
        plugin = BUKKIT / "AdaptiveDifficultyGuiPlugin.java"
        for path in (bridge, plugin):
            if path.is_file():
                scan_java(path, errors, REPO)
        if bridge.is_file():
            btext = bridge.read_text(encoding="utf-8")
            if "gui.ultra.UltraGui" not in btext:
                errors.append("ForgeBridge.java: openUltraLm must load gui.ultra.UltraGui")
            if 'return "cnpc"' in btext:
                errors.append('ForgeBridge.guiBackend must not default/return "cnpc"')

    print("=== CNPC GUI package removed (Ultra-only) ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  gui/cnpc/ absent; Forge+Bukkit routes Ultra (no CnpcLmGui load)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
