#!/usr/bin/env python3
"""Full Legacy Mechanics GUI menu audit — Ultra-only (CNPC/chest/CMI retired)."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MOD = ROOT / "tools" / "dmz-adaptive-difficulty"
ULTRA = MOD / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/ultra"
CNPC_DIR = MOD / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/cnpc"
OUT = MOD / "sim" / "out"
OUT.mkdir(parents=True, exist_ok=True)

errors: list[str] = []


def ok(label: str, cond: bool, detail: str = "") -> None:
    if cond:
        print(f"  OK  {label}" + (f" — {detail}" if detail else ""))
    else:
        errors.append(label + (f": {detail}" if detail else ""))
        print(f" FAIL {label}" + (f" — {detail}" if detail else ""))


MENUS = [
    ("hub", "UltraHubScreens.java"),
    ("difficulty", "UltraDifficultyScreens.java"),
    ("rival", "UltraRivalScreens.java"),
    ("spar", "UltraSparScreens.java"),
    ("prestige", "UltraPrestigeScreens.java"),
    ("progression", "UltraProgressionScreens.java"),
    ("skillcheck", "UltraSkillCheckScreens.java"),
    ("character", "UltraCharacterScreens.java"),
    ("config", "UltraConfigScreens.java"),
]


def main() -> int:
    print("=== Full GUI menu inventory (Ultra-only) ===")
    ok("CNPC package deleted", not CNPC_DIR.is_dir())
    for key, fname in MENUS:
        path = ULTRA / fname
        ok(f"{key}: Ultra screen", path.is_file(), fname)
        if path.is_file():
            text = path.read_text(encoding="utf-8")
            ok(f"{key}: Ultra open()", "public static void open(" in text)

    char = (ULTRA / "UltraCharacterScreens.java").read_text(encoding="utf-8") if (ULTRA / "UltraCharacterScreens.java").is_file() else ""
    for needle in ("Change race", "Change class", "Model customization"):
        ok(f"character Home has {needle}", needle in char)

    hub = (ULTRA / "UltraHubScreens.java").read_text(encoding="utf-8") if (ULTRA / "UltraHubScreens.java").is_file() else ""
    ok("hub hides Staff Admin from non-staff", "StaffAccess.isStaff" in hub and "Staff Admin" in hub)
    ok("hub has no · off status text", "· off" not in hub)

    gui = (MOD / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/ultra/UltraGui.java").read_text(encoding="utf-8")
    ok("UltraGui requires KaiPath", "Install KaiPath" in gui and "CnpcLmGui" not in gui)

    md = OUT / "full-gui-menu-audit.md"
    lines = ["# Full GUI menu audit (Ultra-only)", "", "| System | Ultra |", "| --- | --- |"]
    for key, fname in MENUS:
        lines.append(f"| {key} | {fname} |")
    lines.append("")
    lines.append("CNPC / chest / CMI backends removed.")
    md.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {md.relative_to(MOD)}")

    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  Ultra menu inventory")
    return 0


if __name__ == "__main__":
    sys.exit(main())
