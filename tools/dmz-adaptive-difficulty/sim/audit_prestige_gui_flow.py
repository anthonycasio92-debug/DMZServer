#!/usr/bin/env python3
"""Prestige GUI + Need wiring audit (Forge ProgressionGuiApi + Bukkit/CNPC backends)."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty"
GUI = ROOT.parent / "dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit"
CNPC = SRC / "gui/cnpc"

errors: list[str] = []
warnings: list[str] = []


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def ok(name: str, cond: bool, detail: str = "") -> None:
    mark = "OK" if cond else "FAIL"
    line = f"  {mark}  {name}" + (f" — {detail}" if detail and not cond else "")
    print(line)
    if not cond:
        errors.append(name + (f": {detail}" if detail else ""))


def warn(name: str, cond: bool, detail: str = "") -> None:
    if cond:
        print(f"  OK  {name}")
    else:
        print(f"  WARN  {name}" + (f" — {detail}" if detail else ""))
        warnings.append(name)


def main() -> None:
    print("=== Prestige Need ladder (logic) ===")
    import audit_prestige_need_ladder  # noqa: E402

    try:
        audit_prestige_need_ladder.main()
    except SystemExit as e:
        if e.code:
            errors.append("prestige need ladder sim")

    print("\n=== Forge API (ProgressionGuiApi + PrestigeSystem) ===")
    gui_api = read(SRC / "gui/ProgressionGuiApi.java")
    prestige_sys = read(SRC / "progression/shop/PrestigeSystem.java")
    ok("prestigePlaceholders uses heldCountForNeed", "heldCountForNeed(player)" in gui_api)
    ok("prestigePlaceholders uses requiredLevel(player)", "PrestigeSystem.requiredLevel(player)" in gui_api)
    ok("required_fmt exposed", 'out.put("required_fmt"' in gui_api)

    lines_block = gui_api.split("prestigeLines(", 1)[1].split("handlePrestigeDo", 1)[0]
    pre_switch = lines_block.split("switch (p)", 1)[0]
    ok("main wallet lore has no Cap line before switch", "§7Cap:" not in pre_switch)
    ok("main default shows Need", "required_fmt" in lines_block and "default ->" in lines_block)
    ok("main lore documents C0-4 and held gates", "C0–4" in lines_block or "C0-4" in lines_block)

    ok("PrestigeSystem veteran branch", "completed >= HELD_GATE_MIN_COMPLETED" in prestige_sys)
    ok("PrestigeSystem heldCountForNeed", "heldCountForNeed" in prestige_sys)
    ok("PrestigeSystem reconcileNeedFloor on login hook",
       "reconcileNeedFloor" in read(SRC / "progression/shop/PrestigePointsSystem.java"))

    print("\n=== CNPC Prestige menu ===")
    cnpc = read(CNPC / "CnpcLmPrestigeGui.java")
    ok("CNPC uses prestigeLines main", 'prestigeLines(player, "main")' in cnpc)
    ok("CNPC confirm via handlePrestigeDo", 'handlePrestigeDo(player, "confirm"' in cnpc)
    ok("CNPC no cap subpage button on main", "Level Cap" not in cnpc)
    ok("CNPC cap route falls back to main", '"cap", "breakthrough" -> paintMain' in cnpc)
    ok("CNPC turn-in packs", "TURN_IN_AMOUNTS" in cnpc)

    print("\n=== Chest Prestige GUI ===")
    chest = read(GUI / "PrestigeChestGui.java")
    main_block = chest.split("private Inventory main(", 1)[1].split("private Inventory turnIn", 1)[0]
    ok("Chest wallet putWallet + prestigeLines", "putWallet" in main_block and "prestigeLines" in chest)
    ok("Chest main no Level Cap Breakthrough button", "prestige.main.cap" not in main_block)
    ok("Chest uses lmdo prestige confirm", "lmdo prestige confirm" in chest or "SlotAction.act(\"confirm\"" in chest)
    ok("Chest turn-in amounts 1/2/3/6/9", "putTurnIn" in chest)

    print("\n=== CMI Prestige GUI ===")
    cmi = read(GUI / "CmiPrestigeGui.java")
    cmi_main = cmi.split("private static void openMain", 1)[1].split("private static void openTurnIn", 1)[0]
    ok("CMI main no cap nav button", "prestige.main.cap" not in cmi_main)
    ok("CMI lmdo prestige confirm", "lmdo prestige confirm" in cmi)
    ok("CMI lmdo prestige turnin", "lmdo prestige turnin" in cmi)

    print("\n=== ForgeBridge wiring ===")
    bridge = read(GUI / "ForgeBridge.java")
    ok("ForgeBridge prestigePlaceholders", "prestigePlaceholders" in bridge)
    ok("ForgeBridge prestigeLines", "prestigeLines" in bridge)
    ok("ForgeBridge prestigeHandleDo", "prestigeHandleDo" in bridge or "handlePrestigeDo" in bridge)

    print("\n=== Cross-backend parity ===")
    ok("All backends: Turn-in + Shop + Effects + Tiers on main",
       all(x in cnpc for x in ("Turn-in", "Skill shop", "Effects", "§6Tiers"))
       and "Turn In Prestiges" in chest and "Skill Shop" in chest)

    out = ROOT / "sim/out/prestige-gui-audit.md"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(
        "# Prestige GUI audit\n\n"
        f"- Errors: **{len(errors)}**\n"
        f"- Warnings: **{len(warnings)}**\n\n"
        + ("## Failures\n" + "\n".join(f"- {e}" for e in errors) + "\n" if errors else "## Result\nPASS\n")
        + ("## Warnings\n" + "\n".join(f"- {w}" for w in warnings) + "\n" if warnings else ""),
        encoding="utf-8",
    )
    print(f"\nWrote {out.relative_to(ROOT.parents[1])}")

    print("\n=== Summary ===")
    if errors:
        print(f"FAIL — {len(errors)} error(s)")
        for e in errors:
            print(f"  - {e}")
        raise SystemExit(1)
    print(f"PASS — prestige GUI/Need wiring OK ({len(warnings)} warning(s))")


if __name__ == "__main__":
    main()
