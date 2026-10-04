#!/usr/bin/env python3
"""Fail if Legacy Mechanics applies its own prestige scale.

Policy:
  - DragonMineZ and dmzrevamp own combat, ki, and stamina scale.
  - LM may read PrestigeSystem.scaleMultiplier for display only.
  - LM must not multiply getTotalMultiplier, getMaxEnergy, getMaxStamina, or defense by that scale.
  - LM must not duplicate 1 + count × scaleBonusPerPrestige in Java.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty"


def read(rel: str) -> str:
    return (SRC / rel).read_text(encoding="utf-8")


def main() -> int:
    errors: list[str] = []

    integration = read("progression/LmOverhaulPrestigeIntegration.java")
    if 'getMethod("scaleMultiplier", StatsData.class)' not in integration:
        errors.append("LmOverhaulPrestigeIntegration must reflect PrestigeSystem.scaleMultiplier")
    if "PrestigeSystem" not in integration:
        errors.append("LmOverhaulPrestigeIntegration must target com.dmzrevamp.revamp.prestige.PrestigeSystem")

    # No local prestige scale formula in Java (comments OK).
    for path in SRC.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        rel = path.relative_to(SRC)
        if "scaleBonusPerPrestige" in text:
            stripped = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
            stripped = re.sub(r"//.*", "", stripped)
            if "scaleBonusPerPrestige" in stripped:
                errors.append(f"{rel}: scaleBonusPerPrestige must not appear in code (delegate to Overhaul)")

    combat = read("mixin/StatsDataOverhaulCombatScaleMixin.java")
    if "combatScaleMultiplier" in combat or "base * scale" in combat or "setReturnValue" in combat:
        errors.append("StatsDataOverhaulCombatScaleMixin must not multiply getTotalMultiplier")

    pool = read("progression/DmzResourcePoolClamp.java")
    if "combatScaleMultiplier" in pool:
        errors.append("DmzResourcePoolClamp must not apply combatScaleMultiplier")
    if re.search(r"base\s*\*\s*scale", pool):
        errors.append("DmzResourcePoolClamp must not multiply pool max by scale")

    resource_scale = read("progression/bridge/OverhaulPrestigeResourceScale.java")
    if "scaleOnMaxIncrease" not in resource_scale:
        errors.append("OverhaulPrestigeResourceScale missing scaleOnMaxIncrease (document fallback path)")
    if "overhaulPrestigeEnabled()" not in resource_scale:
        errors.append("OverhaulPrestigeResourceScale must branch on overhaulPrestigeEnabled()")

    scaled = read("calc/LmOverhaulScaledCombat.java")
    if "* scale(data)" in scaled or "* scale(" in scaled:
        errors.append("LmOverhaulScaledCombat must not multiply combat stats by scale")

    mixins = (ROOT / "src/main/resources/legacymechanics.mixins.json").read_text(encoding="utf-8")
    if '"StatsDataOverhaulCombatScaleMixin"' in mixins:
        errors.append("legacymechanics.mixins.json must not register StatsDataOverhaulCombatScaleMixin")
    if '"StatsDataHudPoolMaxMixin"' in mixins:
        errors.append("legacymechanics.mixins.json must not register StatsDataHudPoolMaxMixin")

    if "return data == null ? 0f : data.getMaxEnergy()" not in pool:
        errors.append("DmzResourcePoolClamp.actualMaxEnergy must be live getMaxEnergy")

    hud = read("mixin/StatsDataHudPoolMaxMixin.java")
    if "applyOverhaulScale" in hud or "setReturnValue" in hud:
        errors.append("StatsDataHudPoolMaxMixin must not change getMaxEnergy/getMaxStamina")

    print("=== Overhaul scale delegation audit ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  LM does not apply its own prestige scale")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
