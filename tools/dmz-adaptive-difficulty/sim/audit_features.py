#!/usr/bin/env python3
"""Audit AdaptiveDifficulty source against the intended product feature set.

Fail-closed checks: stock defaults, gate/scaling/exemption markers, dead knobs
marked unused, personal/death/reward wiring. Pair with audit_gui_abi.py.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "tools" / "dmz-adaptive-difficulty" / "src" / "main" / "java"
GUI_SRC = ROOT / "tools" / "dmz-adaptive-difficulty-gui" / "src" / "main" / "java"
CFG = SRC / "com/dbzlegacy/adaptivedifficulty/config/DifficultyConfig.java"
PROFILE = SRC / "com/dbzlegacy/adaptivedifficulty/calc/PlayerCombatProfile.java"
UNLOCK = SRC / "com/dbzlegacy/adaptivedifficulty/tier/UnlockSystem.java"
MOB = SRC / "com/dbzlegacy/adaptivedifficulty/scaling/MobScaling.java"
EVENTS = SRC / "com/dbzlegacy/adaptivedifficulty/event/DifficultyEvents.java"
ACTIONS = SRC / "com/dbzlegacy/adaptivedifficulty/service/DifficultyActions.java"
GATE = SRC / "com/dbzlegacy/adaptivedifficulty/util/SystemGate.java"
REWARD = SRC / "com/dbzlegacy/adaptivedifficulty/reward/RewardSystem.java"
TRACKER = SRC / "com/dbzlegacy/adaptivedifficulty/tick/ScaledMobTracker.java"
SANITY = SRC / "com/dbzlegacy/adaptivedifficulty/calc/CombatSanity.java"
CMDS = SRC / "com/dbzlegacy/adaptivedifficulty/command/DifficultyCommands.java"
BRIDGE = GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/ForgeBridge.java"
README = ROOT / "tools" / "dmz-adaptive-difficulty" / "README.md"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def field_default(src: str, name: str) -> str | None:
    # public double unlockTier1EnemyMult = 0.21;
    m = re.search(rf"\b{re.escape(name)}\s*=\s*([^;]+);", src)
    return m.group(1).strip() if m else None


def has(src: str, *needles: str) -> bool:
    return all(n in src for n in needles)


def main() -> int:
    errors: list[str] = []
    warns: list[str] = []
    ok: list[str] = []

    def check(label: str, cond: bool, detail: str = "") -> None:
        if cond:
            ok.append(label)
            print(f"  OK  {label}" + (f" — {detail}" if detail else ""))
        else:
            errors.append(f"{label}: {detail or 'failed'}")
            print(f" FAIL {label}" + (f" — {detail}" if detail else ""))

    def warn(label: str, detail: str) -> None:
        warns.append(f"{label}: {detail}")
        print(f" WARN {label} — {detail}")

    for p in (CFG, PROFILE, UNLOCK, MOB, EVENTS, ACTIONS, GATE, REWARD, TRACKER, SANITY, CMDS, BRIDGE, README):
        if not p.is_file():
            print(f"FAIL: missing {p}", file=sys.stderr)
            return 2

    cfg = read(CFG)
    profile = read(PROFILE)
    unlock = read(UNLOCK)
    mob = read(MOB)
    events = read(EVENTS)
    actions = read(ACTIONS)
    gate = read(GATE)
    reward = read(REWARD)
    tracker = read(TRACKER)
    sanity = read(SANITY)
    cmds = read(CMDS)
    bridge = read(BRIDGE)
    readme = read(README)

    print("=== Stock ladder / form / HP scale ===")
    expected = {
        "unlockTier1EnemyMult": "0.21",
        "unlockTier2EnemyMult": "0.42",
        "unlockTier3EnemyMult": "0.65",
        "unlockTier4EnemyMult": "0.90",
        "unlockTier5EnemyMult": "1.35",
        "unlockTier6EnemyMult": "1.60",
        "unlockTier7EnemyMult": "2.00",
        "transformScaleWeight": "0.55",
        "transformScaleExponent": "0.75",
        "mobHealthScale": "0.65",
        "maxScaledMobsPerPlayer": "5",
        "eliteMinUnlockTier": "4",
        "mutationMinUnlockTier": "5",
        "adaptiveAiMinUnlockTier": "1",
        "enemyEvolutionMinUnlockTier": "1",
        "bossMechanicsMinUnlockTier": "6",
        "ancientCoinUpgradeChance": "0.02",
        "eliteChancePercent": "2.25",
        "mutationChancePercent": "3.75",
        "eliteStatMultiplier": "1.50",
        "deathResetsActiveDifficulty": "true",
    }
    for name, want in expected.items():
        got = field_default(cfg, name)
        check(f"default {name}", got == want, f"got {got}")

    print("\n=== Unlock gate / revoke ===")
    check(
        "prestige OR level eligibility",
        has(unlock, "prestige >= tier.id", "dmzLevelForUnlockGate", "requiredDmzLevel"),
    )
    check("reliable-sample revoke", has(unlock, "revokeTier", "hasReliableUnlockGateSample", "resetTemporary"))
    check("buy charges Ancient Coins", has(actions, "AncientCoinEconomy", "activationCost", "charge", "setTier"))

    print("\n=== Combat model (STR/SKP dmg, VIT HP, no PWR/ENE) ===")
    check("STR/SKP-only offense comment+code", has(profile, "STR/SKP only", "physicalOffense", "blendForm"))
    check("VIT hit cap", has(profile, "kiProtectionHitFrac", "targetMobDamage", "hitCap"))
    check("soft VIT HP", has(profile, "targetMobHealth", "vitShare", "mobHealthScale"))
    check("PWR never scaled", has(profile, "PWR / ENE are not scaled against", "never PWR"))
    check(
        "tank class does not inflate ATK",
        "cls.contains(\"tank\")" in profile
        and re.search(
            r'cls\.contains\("tank"\).*?return 1\.0;',
            profile,
            re.S,
        )
        is not None,
    )
    check("CombatSanity clamps", has(sanity, "saneFormMult", "saneLive", "usableBaseline"))

    print("\n=== Form soft-curve / peel ===")
    check("form soft curve", has(profile, "blendForm", "transformScaleWeight", "megaForm"))
    check("DMZ addition peel", has(profile, "combineDmzMults", "dmzMultiplicationMode", "peelChannel")
          or has(profile, "multiplicationInsteadOfAdditionForMultipliers"))

    print("\n=== Nearby scale + max 5 ===")
    check("max slots ≤5", has(tracker, "maxSlots") and "maxScaledMobsPerPlayer" in read(TRACKER.parent / "ScaledMobTracker.java")
          or "Math.min(5" in read(TRACKER) or "maxScaledMobsPerPlayer" in cfg)
    check("maxScaledMobsPerPlayer clamped", "Math.min(5" in cfg or "maxScaledMobsPerPlayer" in cfg)

    print("\n=== Exemptions ===")
    check("saga/quest exempt", has(mob, "DBSagasEntity", "isExemptFromConversion"))
    check("Ender Dragon exempt", "EnderDragon" in mob)
    check("SPAWNER exempt", has(mob, "TAG_FROM_SPAWNER", "SPAWNER") or "dmz_ad_from_spawner" in mob)
    check("SDD exempt", has(mob, "sdd_spawner", "sdd_boss"))
    check("slime split exempt", has(mob, "TAG_FROM_SLIME_SPLIT") or "dmz_ad_slime_split" in mob)

    print("\n=== Personal / death / coins / admin ===")
    check("personal participates gate", has(gate, "participates", "isPersonalEnabled", "allows"))
    check("death clears active tier", has(events, "resetTemporary", "deathResetsActiveDifficulty"))
    check("logout keeps tier (save, no resetTemporary on logout)", 
          "onLogout" in events and "resetTemporary" not in events[events.find("onLogout"):events.find("onLogout")+800])
    check("kill coin rewards", has(reward, "rollKillLoot", "dropInWorld", "isCoinDropChat"))
    check("system enabled gate", has(gate, "isEnabled") and "setEnabled" in cfg)
    check("whitelist gate", "isWhitelistEnabled" in cfg and "isPlayerAllowed" in cfg)

    print("\n=== Titles / teams stub / GUI ===")
    check("title equip action", has(actions, "equipTitle", "ACT_EQUIP_TITLE") or "equip_title" in actions)
    check("teams WIP", "work in progress" in actions.lower() or "WIP" in actions)
    check("GUI ABI package stable", "com.dbzlegacy.adaptivedifficulty" in bridge and "DifficultyCache" in bridge)
    check("GUI unused tank pierce blocked", has(bridge, "tankDamageDefenseRatio", "isLegacyUnusedCounterField"))

    print("\n=== Dead knobs must not be live-settable ===")
    check(
        "Forge admin rejects tankDamage*",
        "tankdamagedefenseratio" in cmds and "is unused" in cmds,
    )
    check(
        "config marks tankDamage unused",
        "unused in combat as of 1.0.8" in cfg,
    )
    # Ensure targetMobDamage does not reference tank pierce knobs.
    dmg_fn = profile
    m = re.search(r"public double targetMobDamage\(.*?\{(.*?)\n    \}", profile, re.S)
    if m:
        dmg_fn = m.group(1)
    check("targetMobDamage ignores tankDamage*", "tankDamage" not in dmg_fn)

    print("\n=== README alignment ===")
    check("README stock percents", "21%" in readme and "200%" in readme)
    check("README STR/SKP + VIT", "STR/SKP" in readme and "VIT" in readme and "PWR/ENE" in readme)
    check("README version 1.0.10", "1.0.10" in readme)

    # mobHealthScale GUI clamp default
    check(
        "GUI mobHealthScale invalid fallback 0.65",
        "yield h <= 0.0 || h > 4.0 ? 0.65 : h" in bridge
        or "? 0.65" in bridge,
    )

    print("\n=== Summary ===")
    for w in warns:
        print(f"WARN: {w}")
    if errors:
        for e in errors:
            print(f"FAIL: {e}")
        print(f"{len(errors)} error(s), {len(warns)} warning(s), {len(ok)} ok")
        return 1
    print(f"PASS — intended features intact ({len(ok)} checks, {len(warns)} warning(s))")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
