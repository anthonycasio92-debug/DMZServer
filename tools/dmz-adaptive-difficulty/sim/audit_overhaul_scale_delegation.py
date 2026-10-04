#!/usr/bin/env python3
"""Fail if Legacy Mechanics reimplements dmzrevamp Overhaul prestige scale locally.

Policy:
  - Combat/pool prestige scale must come from dmzrevamp PrestigeSystem.scaleMultiplier only
    (via LmOverhaulPrestigeIntegration.combatScaleMultiplier).
  - LM may apply that value once on getTotalMultiplier (combat) and once on pool max (ENE/STM
    excluded from totalMult). It must not duplicate 1 + count × scaleBonusPerPrestige in Java.
  - Resource pool ratio scaling in OverhaulPrestigeResourceScale is sync-only when Overhaul
    prestige is off; when on, only clamp/refill via DmzResourcePoolClamp.
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
    if "combatScaleMultiplier" not in combat:
        errors.append("StatsDataOverhaulCombatScaleMixin must use combatScaleMultiplier")
    if "integrationActive()" not in combat:
        errors.append("StatsDataOverhaulCombatScaleMixin must gate on integrationActive()")
    if re.search(r"base\s*\*\s*scale\s*\*\s*scale", combat):
        errors.append("StatsDataOverhaulCombatScaleMixin must not double-multiply scale")

    pool = read("progression/DmzResourcePoolClamp.java")
    if "LmOverhaulPrestigeIntegration.combatScaleMultiplier" not in pool:
        errors.append("DmzResourcePoolClamp must use combatScaleMultiplier for pool scale")

    resource_scale = read("progression/bridge/OverhaulPrestigeResourceScale.java")
    if "scaleOnMaxIncrease" not in resource_scale:
        errors.append("OverhaulPrestigeResourceScale missing scaleOnMaxIncrease (document fallback path)")
    if "overhaulPrestigeEnabled()" not in resource_scale:
        errors.append("OverhaulPrestigeResourceScale must branch on overhaulPrestigeEnabled()")

    scaled = read("calc/LmOverhaulScaledCombat.java")
    if scaled.count("* scale(data)") > 2:
        errors.append("LmOverhaulScaledCombat applies scale too many times (melee/ki use getters; defense at most twice)")
    melee_block = scaled.split("public static double melee", 1)[-1].split("public static double strike", 1)[0]
    if "* scale(data)" in melee_block or "* scale(" in melee_block:
        errors.append("LmOverhaulScaledCombat must not multiply melee by scale (getMeleeDamage already scaled)")

    mixins = (ROOT / "src/main/resources/legacymechanics.mixins.json").read_text(encoding="utf-8")
    if '"StatsDataOverhaulCombatScaleMixin"' not in mixins:
        errors.append("legacymechanics.mixins.json must register StatsDataOverhaulCombatScaleMixin")
    if '"StatsDataHudPoolMaxMixin"' not in mixins:
        errors.append("legacymechanics.mixins.json must register StatsDataHudPoolMaxMixin")

    if "return data == null ? 0f : data.getMaxEnergy()" not in pool:
        errors.append("DmzResourcePoolClamp.actualMaxEnergy must be live getMaxEnergy (2.4.115)")

    hud = read("mixin/StatsDataHudPoolMaxMixin.java")
    if "applyOverhaulScale" not in hud or "scaled > value" not in hud:
        errors.append("StatsDataHudPoolMaxMixin must scale the getter return (2.4.115), not replace it")

    print("=== Overhaul scale delegation audit ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  LM delegates prestige scale to dmzrevamp; single apply paths")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
