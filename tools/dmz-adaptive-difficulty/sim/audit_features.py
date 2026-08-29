#!/usr/bin/env python3
"""Audit AdaptiveDifficulty source against the intended product feature set.

Fail-closed checks: stock defaults, gate/scaling/exemption markers, dead knobs
marked unused, personal/death/reward wiring, PWR/ENE + class/top-2 counters.
Pair with audit_gui_abi.py.
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
EVO = SRC / "com/dbzlegacy/adaptivedifficulty/evolution/EnemyEvolution.java"
AI = SRC / "com/dbzlegacy/adaptivedifficulty/ai/AdaptiveAiSystem.java"
PROGRESSION = SRC / "com/dbzlegacy/adaptivedifficulty/calc/DmzProgression.java"
BRIDGE = GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/ForgeBridge.java"
GUI_PLUGIN = GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/AdaptiveDifficultyGuiPlugin.java"
README = ROOT / "tools" / "dmz-adaptive-difficulty" / "README.md"
MOD = SRC / "com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.java"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def field_default(src: str, name: str) -> str | None:
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

    for p in (
        CFG, PROFILE, UNLOCK, MOB, EVENTS, ACTIONS, GATE, REWARD, TRACKER,
        SANITY, CMDS, EVO, AI, PROGRESSION, BRIDGE, README, MOD,
    ):
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
    evo = read(EVO)
    ai = read(AI)
    progression = read(PROGRESSION)
    reward = read(REWARD)
    tracker = read(TRACKER)
    sanity = read(SANITY)
    cmds = read(CMDS)
    bridge = read(BRIDGE)
    readme = read(README)
    mod = read(MOD)

    print("=== Version ===")
    check("VERSION 2.3.44", 'VERSION = "2.3.44"' in mod)

    print("\n=== Stock ladder / form / HP scale ===")
    expected = {
        "unlockTier1EnemyMult": "0.21",
        "unlockTier2EnemyMult": "0.42",
        "unlockTier3EnemyMult": "0.65",
        "unlockTier4EnemyMult": "0.90",
        "unlockTier5EnemyMult": "1.35",
        "unlockTier6EnemyMult": "1.60",
        "unlockTier7EnemyMult": "2.00",
        "transformScaleWeight": "0.65",
        "transformScaleExponent": "0.75",
        "mobHealthScale": "0.75",
        "tankDamageDefenseRatio": "0.45",
        "tankDamageHealthRatio": "0.28",
        "maxScaledMobsPerPlayer": "5",
        "eliteMinUnlockTier": "4",
        "mutationMinUnlockTier": "5",
        "adaptiveAiMinUnlockTier": "1",
        "enemyEvolutionMinUnlockTier": "1",
        "bossMechanicsMinUnlockTier": "6",
        "ancientCoinDropChance": "0.05",
        "ancientCoinUpgradeChance": "0.005",
        "eliteChancePercent": "2.25",
        "mutationChancePercent": "3.75",
        "eliteStatMultiplier": "1.50",
        "deathResetsActiveDifficulty": "true",
        "enableClassCounters": "true",
        "enableStrongStatCounters": "true",
        "tierCostLevelDivisor": "50_000.0",
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

    print("\n=== Combat model (STR/SKP/PWR + ENE, class + top-2) ===")
    check("blended offense includes PWR/ENE", has(profile, "blendedOffense", "ENERGY_OFFENSE_FACTOR", "getKiDamage", "getMaxEnergy"))
    check("ENERGY WeakStat", "ENERGY," in profile or "ENERGY\n" in profile)
    check("form peak includes PWR/ENE", has(profile, '"PWR"', '"ENE"'))
    check("top-2 counters", has(profile, "top 2", "Math.min(2") or "topCount = Math.min(2" in profile)
    check("class counters enabled in damage path", has(profile, "enableClassCounters", "classDamageBias"))
    check("strong-stat top-2 combine", has(profile, "combineTopStatBiases", "0.60"))
    check("VIT hit cap kept", has(profile, "kiProtectionHitFrac", "targetMobDamage", "hitCap"))
    check(
        "raised hit-cap budgets (1.0.28)",
        "case 1 -> 0.30" in profile and "default -> 0.74" in profile and "formFactor = 0.78" in profile,
    )
    check("live-bag landing ladder", "liveMaxHealth" in profile and "landFrac" in profile)
    check("T3 god-form pierce", "activeTier >= 3 && formBoost >= 6.0" in profile or "formBoost >= 6.0" in profile)
    check("telemetryFeelMigratedV1", "telemetryFeelMigratedV1" in cfg)
    check("event incoming soft-cap", "softCap" in events or "maxFrac" in events)
    check("VIT/RES damage floors live", has(profile, "tankDamageDefenseRatio", "tankDamageHealthRatio", "defFloor", "hpFloor"))
    check("VIT floor uses raised early-tier strength", "hpFloorStrength" in profile)
    check("vitDumpPressureMigratedV1 present", "vitDumpPressureMigratedV1" in cfg)
    check("reads kiprotection / ki_infusion / potentialunlock", has(profile, '"kiprotection"', '"ki_infusion"', '"potentialunlock"'))
    check("skill sponge on mob HP", has(profile, "kiInfusionLevel", "potentialUnlockLevel", "skillHp"))
    check("live-bag hit-cap", "hitCapHealth" in profile)
    check("live-offense transform pressure", "liveOffense" in profile and "liveShare" in profile or "liveFloor" in profile)
    check("KP is post-mitigation advantage", "kiProtectionLevel" in profile and "formFactor = 0.78" in profile)
    check("reads getFlatMitigation", "getFlatMitigation" in profile)
    check("DEF-cancel pierce floor", "dmzCancelMitigationThreshold" in profile and "pierce" in profile)
    check("landing safety net method", "targetLandingDamage" in profile)
    check("LivingDamageEvent receiveCanceled", "receiveCanceled = true" in events and "targetLandingDamage" in events)
    check("isAdPainted helper", "isAdPainted" in mob)
    check("CombatSanity clamps", has(sanity, "saneFormMult", "saneLive", "usableBaseline"))
    check("live offense poll includes PWR/ENE", has(events, "getKiDamage", "getMaxEnergy"))

    print("\n=== Form soft-curve / peel ===")
    check("form soft curve", has(profile, "blendForm", "transformScaleWeight", "megaForm"))
    check("DMZ addition peel", has(profile, "combineDmzMults", "dmzMultiplicationMode", "peelChannel"))

    print("\n=== Nearby scale + max 5 ===")
    check("max slots ≤5", "maxScaledMobsPerPlayer" in cfg and "Math.min(5" in tracker)
    check("maxScaledMobsPerPlayer clamped", "Math.min(5" in cfg or "maxScaledMobsPerPlayer" in cfg)

    print("\n=== Exemptions ===")
    check("saga/quest exempt", has(mob, "DBSagasEntity", "isExemptFromConversion"))
    check("Ender Dragon exempt", "EnderDragon" in mob)
    check("SPAWNER exempt", has(mob, "TAG_FROM_SPAWNER") or "dmz_ad_from_spawner" in mob)
    check("SDD exempt", has(mob, "sdd_spawner", "sdd_boss"))
    check("slime split exempt", has(mob, "TAG_FROM_SLIME_SPLIT") or "dmz_ad_slime_split" in mob)

    print("\n=== Personal / death / coins / admin ===")
    check("personal participates gate", has(gate, "participates", "isPersonalEnabled", "allows"))
    check("death clears active tier", has(events, "resetTemporary", "deathResetsActiveDifficulty"))
    check(
        "logout keeps tier (save, no resetTemporary on logout)",
        "onLogout" in events and "resetTemporary" not in events[events.find("onLogout") : events.find("onLogout") + 800],
    )
    check("kill coin rewards", has(reward, "rollKillLoot", "dropInWorld", "isCoinDropChat"))
    check("system enabled gate", has(gate, "isEnabled") and "setEnabled" in cfg)
    check("whitelist gate", "isWhitelistEnabled" in cfg and "isPlayerAllowed" in cfg)

    print("\n=== Titles / teams stub / GUI ===")
    check("title equip action", has(actions, "equipTitle", "ACT_EQUIP_TITLE") or "equip_title" in actions)
    check("teams WIP", "work in progress" in actions.lower() or "WIP" in actions)
    check("GUI ABI package stable", "com.dbzlegacy.adaptivedifficulty" in bridge and "DifficultyCache" in bridge)

    print("\n=== Balance telemetry ===")
    tel = read(SRC / "com/dbzlegacy/adaptivedifficulty/telemetry/BalanceTelemetry.java")
    gui_plugin = read(GUI_PLUGIN)
    check("BalanceTelemetry class", "class BalanceTelemetry" in tel)
    check("shouldLog when telemetry enabled", "shouldLog" in tel and "isEnabled()" in tel)
    check("JSONL hits path", "hits-" in tel and "telemetry" in tel)
    check("admin telemetry commands", "telemetryRoot" in cmds or '"telemetry"' in cmds)
    check("logs before/with safety net", "logIncomingHit" in events)
    check("default telemetry off", "balanceTelemetryEnabled = false" in cfg)
    check("writeTestProbe", "writeTestProbe" in tel)
    # Mohist: Bukkit owns /difficulty — telemetry must be wired through GUI, not Forge-only.
    check("Bukkit telemetry subcommand", "handleTelemetry" in gui_plugin and '"telemetry"' in gui_plugin)
    check("Bukkit ForgeBridge telemetry API", "setTelemetryEnabled" in bridge and "telemetryTest" in bridge)
    check("telemetry not whitelist-gated", "isWhitelisted" not in tel)

    print("\n=== Live challenge knobs (1.0.17) ===")
    check("Forge admin sets tankDamageDefenseRatio", "tankdamagedefenseratio" in cmds and "tankDamageDefenseRatio =" in cmds)
    check("Forge admin sets tankDamageHealthRatio", "tankdamagehealthratio" in cmds and "tankDamageHealthRatio =" in cmds)
    check("Forge admin sets mobHealthScale", "mobhealthscale" in cmds and "mobHealthScale =" in cmds)
    check("GUI allowlists tankDamage*", '"tankDamageDefenseRatio"' in bridge and '"tankDamageHealthRatio"' in bridge)
    unused_fn = re.search(
        r"isLegacyUnusedCounterField\(.*?\{(.*?)\n    \}", bridge, re.S
    )
    unused_body = unused_fn.group(1) if unused_fn else ""
    check(
        "GUI does not mark tankDamage unused",
        "tankDamageDefenseRatio" not in unused_body and "tankDamageHealthRatio" not in unused_body,
    )
    m = re.search(r"public double targetMobDamage\(.*?\{(.*?)\n    \}", profile, re.S)
    dmg_fn = m.group(1) if m else profile
    check("targetMobDamage uses tankDamage*", "tankDamage" in dmg_fn)

    print("\n=== Skills + melee AI parity ===")
    check("DmzProgression.skillLevel helper", "skillLevel(" in progression and "skillActive(" in progression)
    check("melee painted shock / slam damage", has(evo, "paintedMeleeHit", "dmz_ad_melee_shock"))
    check("zombie Awakened+ chase speed", "AWAKENED" in evo and "m_21573_" in evo)
    check("Adaptive AI speed from Enhanced+", "ENHANCED" in ai and "f_19596_" in ai)
    check("tier move bump on paint", "tierBump" in mob or "case 1 -> 1.06" in mob)

    print("\n=== Combat gravity cleanup (1.0.21) ===")
    gravity = read(SRC / "com/dbzlegacy/adaptivedifficulty/evolution/CombatGravity.java")
    check("removeSource API", "removeSource(" in gravity)
    check("zero-crossing always applies", "Crossing to/from ~0" in gravity or "boolean clearing" in gravity)
    check("death clears Enderman/Warden gravity", has(events, "CombatGravity.removeSource", "EnderMan", "Warden"))
    check("README sticky-gravity fix", "gravity" in readme.lower() and "1.0.21" in readme)

    print("\n=== Personal OFF on boot (1.0.22) ===")
    pdata = read(SRC / "com/dbzlegacy/adaptivedifficulty/data/PlayerDifficultyData.java")
    check("personal default false", "personalEnabled = false" in pdata)
    check("NBT missing personal → false", "tag.m_128441_(\"personalEnabled\") && tag.m_128471_(\"personalEnabled\")" in pdata)
    check("boot reset set cleared on starting", "PERSONAL_OFF_THIS_BOOT.clear()" in events)
    check("first login forces personal OFF", "PERSONAL_OFF_THIS_BOOT.add" in events and "setPersonalEnabled(false)" in events)
    check("README personal OFF on restart", "forces personal OFF" in readme or "Personal difficulty starts **OFF**" in readme)

    print("\n=== Post-pierce soft-cap clamp (1.0.25) ===")
    check("incomingSoftCapFrac helper", "incomingSoftCapFrac" in profile)
    check("post-pierce bagCap clamp", "bagCap" in profile and "incomingSoftCapFrac()" in profile)
    check("formula revision 37", "mix(h, 37L)" in profile)
    check("README pierce clamp", "Post-pierce" in readme or "post-pierce" in readme)

    print("\n=== Telemetry retune (1.0.28) ===")
    check("T1 threat floor 0.56", "case 1 -> 0.56" in profile)
    check("T6 form nudge 1.82", "case 6 -> 1.82" in profile)
    check("T6 liveShare 0.74", "case 6 -> 0.74" in profile)
    check("README 1.0.28 retune", "1.0.28" in readme and "hits-2026-08-05" in readme)

    print("\n=== Mob HP trim (1.0.29 / 1.0.30) ===")
    check("stock mobHealthScale 0.75", field_default(cfg, "mobHealthScale") == "0.75")
    check("mobHpTrimMigratedV1", "mobHpTrimMigratedV1" in cfg)
    check("mobHpTrimMigratedV2", "mobHpTrimMigratedV2" in cfg)
    check("README 1.0.30 HP trim", "1.0.30" in readme and "0.75" in readme)

    print("\n=== Telemetry retune (1.0.24) ===")
    check("formula revision 37", "mix(h, 37L)" in profile)
    check("T1 landCap 0.21", "case 1 -> 0.21" in profile)
    check("T1 landFrac 0.14", "case 1 -> 0.14" in profile)
    check("T6 landFrac 0.50", "case 6 -> 0.50" in profile)
    check("T4 landFrac 0.37", "case 4 -> 0.37" in profile)
    check("T5 landFrac 0.48", "case 5 -> 0.48" in profile)
    check("KP landing 1.5%/lvl", "kiProtectionLevel * 0.015" in profile)
    check("fill-to-landing floor", "preAmount < land)" in events and "land * 0.45" not in events)
    check("progressive soft-caps", "case 5 -> 0.52" in events and "case 6 -> 0.58" in events and "case 7 -> 0.62" in events)
    check("README 1.0.25 balance", "1.0.25" in readme and "T4 tank" in readme)
    check("README 1.0.26 tier costs", "1.0.26" in readme and "tierCostLevelDivisor" in readme)
    check("tierCostDivisorMigratedV1", "tierCostDivisorMigratedV1" in cfg)
    unlock_tier = read(SRC / "com/dbzlegacy/adaptivedifficulty/tier/UnlockTier.java")
    cmi_gui = read(GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/CmiDifficultyGui.java")
    check("UnlockTier.requirementTip", "requirementTip" in unlock_tier and "requiredPrestige" in unlock_tier)
    check("GUI shows DMZ or Prestige req", "Need &fDMZ" in cmi_gui and "_req_level" in bridge)
    check("README unlock req UX", "Need DMZ" in readme and "Prestige" in readme)

    print("\n=== Ghost scaling fix (1.0.23) ===")
    check(
        "isAdPainted ignores bare TAG_SCALED",
        "unlockTierOf(entity) > 0" in mob and ("Spawn init stamps" in mob or "spawn-init" in mob.lower()),
    )
    check(
        "landing safety net requires participates",
        "onDamageDone" in events and "SystemGate.participates(player)" in events[events.find("onDamageDone"):events.find("onDamageDone")+900],
    )
    check("README ghost-scaling fix", "Ghost difficulty" in readme or "spawn-init shells" in readme)

    print("\n=== README alignment ===")
    check("README stock percents", "21%" in readme and "200%" in readme)
    check("README PWR/ENE + top-2", "PWR" in readme and "ENE" in readme and "top-2" in readme)
    check("README version 2.3.44", "2.3.44" in readme)

    print("\n=== Ghast aim fix (1.0.31) ===")
    ki = read(SRC / "com/dbzlegacy/adaptivedifficulty/evolution/KiAttackHelper.java")
    check("large blast cast 0", "setupKiLargeBlast(" in ki and ", 0);" in ki)
    check("large blast launchToward", "fireLargeBlast" in ki and "launchToward(blast, shooter, target, speed)" in ki)
    check("laser/beam launchToward", "launchToward(laser" in ki and "launchToward(wave" in ki)
    check("faceTarget public", "public static void faceTarget" in ki)
    check("ghastTick faceTarget", "KiAttackHelper.faceTarget(ghast, target)" in evo)
    check("README ghast aim", "Ghast" in readme and ("aim" in readme.lower() or "look" in readme.lower()))
    check("README skill-aware / sponge", "Ki Infusion" in readme or "ki_infusion" in readme or "1.05" in readme)

    print("\n=== Soft-cap ladder (1.0.33) ===")
    check("T3 soft-cap 0.46", "case 3 -> 0.46" in events and "case 3 -> 0.46" in profile)
    check("T4 soft-cap 0.50", "case 4 -> 0.50" in events and "case 4 -> 0.50" in profile)
    check("T1 soft-cap 0.40", "default -> 0.40" in events and "default -> 0.40" in profile)
    check("T2 soft-cap 0.43", "case 2 -> 0.43" in events and "case 2 -> 0.43" in profile)
    check("README soft-cap ladder", "0.46" in readme and "soft-cap" in readme.lower())

    print("\n=== Ladder retune (1.0.35) ===")
    check("T4 form nudge 1.58", "case 4 -> 1.58" in profile)
    check("T5 form nudge 1.78", "case 5 -> 1.78" in profile)
    check("T7 form nudge 1.62", "default -> 1.62" in profile)
    check("T4 liveShare 0.60", "case 4 -> 0.60" in profile)
    check("T5 liveShare 0.74", "case 5 -> 0.74" in profile)
    check("T4 landCap 0.48", "case 4 -> 0.48" in profile)
    check("T5 landCap 0.52", "case 5 -> 0.52" in profile)
    check("T3 landFrac 0.30", "case 3 -> 0.30" in profile)
    check("T7 landFrac 0.58", "default -> 0.58" in profile)
    check("README 1.0.35 retune", "1.0.35" in readme and "hits-2026-08-06" in readme)

    print("\n=== Level clamp + mount guard (1.0.35) ===")
    prog = read(SRC / "com/dbzlegacy/adaptivedifficulty/calc/DmzProgression.java")
    host = read(SRC / "com/dbzlegacy/adaptivedifficulty/scaling/HostileMobs.java")
    check("clampDmzLevel helper", "clampDmzLevel" in prog and "configuredMaxDmzLevel" in prog)
    check("dmzLevel uses clamp", "clampDmzLevel(data.getLevel()" in prog)
    check("HostileMobs.isMountPair", "isMountPair" in host and "m_20202_" in host)
    check("hurt cancels mount pair", "isMountPair" in events)
    check("skeleton rejects mount target", "isMountPair(mob, target)" in evo)
    check("README level clamp", "level clamp" in readme.lower() or "maxValue" in readme)
    check("README skeleton mount", "mount" in readme.lower() and "jockey" in readme.lower())

    print("\n=== Coin drop chances (2.3.44) ===")
    coins = read(SRC / "com/dbzlegacy/adaptivedifficulty/currency/AncientCoinEconomy.java")
    check("drop chance gate in rollKillLoot", "ancientCoinDropChance" in coins and "roll >= dropChance" in coins)
    check("dual upgrade band uses same roll", "roll < upgradeChance" in coins)
    check("coinDropChanceMigratedV1", "coinDropChanceMigratedV1" in cfg)
    check("admin set drop chance", "ancientcoindropchance" in cmds or "coindropchance" in cmds)
    check("README 5% / 0.5%", "5%" in readme and "0.5%" in readme and "2.3.44" in readme)

    print("\n=== Mohist CMI /lmdo routing (2.3.44) ===")
    gui_root = GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit"
    cmi_files = [
        "CmiDifficultyGui.java",
        "CmiHubGui.java",
        "CmiProgressionGui.java",
        "CmiPrestigeGui.java",
        "CmiSkillsGui.java",
        "CmiRivalGui.java",
        "CmiSparGui.java",
    ]
    bad_do = (
        'addCommand("difficulty do ',
        'addCommand("lm do ',
        'addCommand("progression do ',
        'addCommand("prestige do ',
        'addCommand("skills do ',
        'addCommand("skillcheck do ',
        'addCommand("rival do ',
        'addCommand("spar do ',
    )
    for name in cmi_files:
        path = gui_root / name
        text = read(path) if path.is_file() else ""
        check(f"{name} exists", path.is_file())
        for pat in bad_do:
            check(f"{name} no {pat.strip()}", pat not in text)
        if path.is_file():
            check(f"{name} uses lmdo", "lmdo " in text)

    chest = read(gui_root / "DifficultyChestGui.java")
    check(
        "DifficultyChestGui no performCommand difficulty do",
        'performCommand("difficulty do' not in chest,
    )
    check(
        "DifficultyChestGui uses handleActionResult",
        "handleActionResult" in chest,
    )
    plugin = read(gui_root / "AdaptiveDifficultyGuiPlugin.java")
    check("Bukkit difficulty admin syslog", 'case "syslog", "systemlog"' in plugin or 'case "syslog"' in plugin)
    check("Bukkit difficulty admin resynclevel", "resynclevel" in plugin)
    check("ForgeBridge.syslogCommand", "syslogCommand" in read(gui_root / "ForgeBridge.java"))
    check("ForgeBridge.resyncLevel", "resyncLevel" in read(gui_root / "ForgeBridge.java"))
    check(
        "DifficultyCommands.resyncLevel public",
        "public static String resyncLevel" in cmds,
    )
    skills_chest = read(gui_root / "SkillsChestGui.java")
    check("SkillsChestGui progression via lmdo", "lmdo lm open progression" in skills_chest)
    check("SkillsChestGui no lm do open", 'cmd("lm do open' not in skills_chest)

    print("\n=== CNPC migrate safety (2.3.44) ===")
    migrator = read(SRC / "com/dbzlegacy/adaptivedifficulty/data/CnpcDataMigrator.java")
    check("resolveWorldBlob present", "resolveWorldBlob" in migrator)
    check("fromScriptControllerCompound", "fromScriptControllerCompound" in migrator)
    check("pickRichestBlob", "pickRichestBlob" in migrator)
    check("enrichRivalRawFromBackupKeys", "enrichRivalRawFromBackupKeys" in migrator)
    check("force aborts without usable data", "Force aborted" in migrator)
    check("NBTJsonUtil LoadFile", "NBTJsonUtil" in migrator and "LoadFile" in migrator)
    check("cnpcLevelScriptsDir LevelResource", "LevelResource" in migrator and "cnpcLevelScriptsDir" in migrator)
    check("fromBackupDir present", "fromBackupDir" in migrator)
    check("fromWorldDataFile present", "fromWorldDataFile" in migrator)
    check("clear only after import", "imported && blob.liveStored" in migrator)
    check("no fake empty marker", "empty-no-cnpc-data" not in migrator)
    check("force uses runWorldMigrate", "runWorldMigrate(server, forceOverwrite)" in migrator)
    check("honest zero-import message", "No rows imported" in migrator)
    check("richer-CNPC replace", "cnpcRich > lmRich" in migrator)

    print("\n=== Staff clear player (2.3.44) ===")
    clearer = read(SRC / "com/dbzlegacy/adaptivedifficulty/data/PlayerDataClear.java")
    mech = read(SRC / "com/dbzlegacy/adaptivedifficulty/command/MechanicsCommands.java")
    gui_plugin = read(gui_root / "AdaptiveDifficultyGuiPlugin.java")
    bridge = read(gui_root / "ForgeBridge.java")
    check("PlayerDataClear.clear API", "public static String clear(" in clearer)
    check("scopes rival/spar/difficulty/progression",
          "clearRival" in clearer and "clearSpar" in clearer
          and "clearDifficulty" in clearer and "clearProgression" in clearer)
    check("Forge /lm admin clear", 'Commands.m_82127_("clear")' in mech)
    check("Bukkit handleLmAdmin clear", '"clear", "wipe", "resetplayer"' in gui_plugin
          or 'case "clear"' in gui_plugin)
    check("ForgeBridge.clearPlayerData", "clearPlayerData" in bridge)
    check("RivalProgression.clearPlayer", "clearPlayer(String uuid)" in read(
        SRC / "com/dbzlegacy/adaptivedifficulty/rival/RivalProgression.java"))

    print("\n=== Spar recent sessions (2.3.44) ===")
    spar_store = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparStore.java")
    spar_sys = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.java")
    check("RECENT_SESSION_LIMIT 3", "RECENT_SESSION_LIMIT = 3" in spar_store)
    check("recentSessions map", "recentSessions" in spar_store and "pushRecent" in spar_store)
    check("recordRecentSession on end", "recordRecentSession" in spar_sys)
    check("statsLines Last 3", "Last 3 sessions" in spar_sys)
    check("statsLines no live TP this session", "TP this session" not in spar_sys)

    print("\n=== Skill Check perm gate (2.3.44) ===")
    staff_access = read(SRC / "com/dbzlegacy/adaptivedifficulty/util/StaffAccess.java")
    # Extract hasSkillCheck body — must not auto-grant via isStaff()
    start = staff_access.find("public static boolean hasSkillCheck")
    end = staff_access.find("private static boolean hasBukkitIsOp", start)
    body = staff_access[start:end] if start >= 0 and end > start else staff_access
    check("hasSkillCheck no staff auto-grant", "isStaff(player)" not in body)
    check("hasSkillCheck uses skillCheckPermission", "skillCheckPermission" in body)
    gui_yml = read(ROOT / "tools/dmz-adaptive-difficulty-gui/src/main/resources/plugin.yml")
    check("plugin.yml skillcheck permission", "permission: legacymechanics.skillcheck" in gui_yml)
    hub = read(gui_root / "HubChestGui.java")
    check("HubChestGui gates on hasSkillCheck", "ForgeBridge.hasSkillCheck(player)" in hub)

    print("\n=== Skill Check icons (2.3.44) ===")
    lore = read(gui_root / "GuiLoreChunks.java")
    skills_chest = read(gui_root / "SkillsChestGui.java")
    cmi_skills = read(gui_root / "CmiSkillsGui.java")
    check("skillIcon never defaults PAPER",
          "return Material.PAPER" not in lore.split("static Material skillIcon")[1].split("private static int")[0]
          if "static Material skillIcon" in lore else False)
    check("Flight uses ELYTRA", "Material.ELYTRA" in lore)
    check("Skill Check header EXPERIENCE_BOTTLE",
          "skillCheckUi ? Material.EXPERIENCE_BOTTLE" in skills_chest
          and "skillCheckUi ? Material.EXPERIENCE_BOTTLE" in cmi_skills)
    check("Hub Skill Check EXPERIENCE_BOTTLE",
          'Material.EXPERIENCE_BOTTLE, "&eSkill Check"' in hub
          or 'Material.EXPERIENCE_BOTTLE, "&eSkill Check"' in read(gui_root / "CmiHubGui.java"))

    print("\n=== GUI coherence (2.3.44) ===")
    diff_chest = read(gui_root / "DifficultyChestGui.java")
    prestige = read(gui_root / "PrestigeChestGui.java")
    rival = read(gui_root / "RivalChestGui.java")
    spar = read(gui_root / "SparChestGui.java")
    skills = read(gui_root / "SkillsChestGui.java")
    check("Difficulty unavailable close @35",
          'put(holder, inv, 35, closeBtn()' in diff_chest)
    check("Difficulty Lower header IRON_INGOT",
          'Material.IRON_INGOT, "&f&lLower Tier"' in diff_chest)
    check("Difficulty Clear Title not BARRIER",
          'Material.NAME_TAG, "&cClear Title"' in diff_chest)
    check("Prestige header GOLDEN_APPLE",
          'Material.GOLDEN_APPLE, "&6&lPrestige"' in prestige)
    check("Prestige Progression BREWING_STAND",
          'Material.BREWING_STAND, "&dProgression"' in prestige)
    check("Rival Progress tiles BOOK/WRITABLE_BOOK/MAP",
          "Material.BOOK, Material.CLOCK, Material.WRITABLE_BOOK" in rival
          and "Material.GOLD_BLOCK, Material.MAP, Material.NAME_TAG" in rival)
    check("Spar Stats button BOOK",
          'Material.BOOK, "&eStats"' in spar)
    check("Skills admin header BOOK",
          "skillCheckUi ? Material.EXPERIENCE_BOTTLE : Material.BOOK" in skills)

    print("\n=== Android convert + remove (2.3.44) ===")
    android = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/race/AndroidConversion.java")
    prog_chest = read(gui_root / "ProgressionChestGui.java")
    prog_cmi = read(gui_root / "CmiProgressionGui.java")
    prog_tree = read(gui_root / "ProgressionCommandTree.java")
    forge_bridge = read(gui_root / "ForgeBridge.java")
    gui_api = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/ProgressionGuiApi.java")
    check("AndroidConversion.remove", "public static String remove(" in android)
    check("AndroidConversion two-click confirm", "CONFIRM_MS = 10_000L" in android)
    check("AndroidConversion restore superforms", 'ensureSkillAtZero(skills, "superforms")' in android)
    check("Chest android_panel", "android_panel" in prog_chest and "androidPanel" in prog_chest)
    check("Chest android_remove picker", "androidRemovePicker" in prog_chest)
    check("CMI android_panel", "openAndroidPanel" in prog_cmi)
    check("CMI android_remove", "openAndroidRemove" in prog_cmi)
    check("Cmd android remove", '("remove".equalsIgnoreCase(args[1])' in prog_tree)
    check("ForgeBridge.androidRemove", "public static String androidRemove(" in forge_bridge)
    check("GuiApi androidRemove", "public static String androidRemove(" in gui_api)
    check("GuiApi player self-remove only",
          "You can only remove your own Android upgrade" in gui_api)
    hub = read(gui_root / "HubChestGui.java")
    cmi_hub = read(gui_root / "CmiHubGui.java")
    check("Hub Remove Android button", 'SlotAction.open("android_remove")' in hub)
    check("CMI Hub Remove Android", '"android_remove"' in cmi_hub and "Remove Android" in cmi_hub)
    plugin = read(gui_root / "AdaptiveDifficultyGuiPlugin.java")
    check("openSystem android_remove for players",
          'case "android_remove"' in plugin or '"android_remove"' in plugin)

    print("\n=== Player Prestige access (2.3.44) ===")
    prestige_menu = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/PrestigeMenu.java")
    prestige_sys = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigeSystem.java")
    prog_cmds = read(SRC / "com/dbzlegacy/adaptivedifficulty/command/ProgressionCommands.java")
    check("PrestigeMenu no staff gate", "StaffAccess.isStaff" not in prestige_menu)
    check("PrestigeSystem.open no staff gate",
          "Staff only" not in prestige_sys.split("public static void open")[1].split("public static void confirmOrPrompt")[0]
          if "public static void open" in prestige_sys else False)
    check("Forge /prestige no requires staff",
          '.requires(ProgressionCommands::staff)' not in prog_cmds.split('m_82127_("prestige")')[1].split('m_82127_("skills")')[0]
          if 'm_82127_("prestige")' in prog_cmds else False)
    check("Hub Prestige for everyone",
          'SlotAction.open("prestige")' in hub
          and hub.count('SlotAction.open("prestige")') >= 1)
    check("handlePrestige no staff gate",
          'Staff only' not in plugin.split("private boolean handlePrestige")[1].split("private boolean handleSkills")[0]
          if "private boolean handlePrestige" in plugin else False)

    print("\n=== Skill Check real max levels (2.3.44) ===")
    skill_unlock = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/SkillUnlockService.java")
    saga_fn = skill_unlock.split("private static void appendSagaSkill")[1].split("private static void appendStrengthLine")[0] \
        if "private static void appendSagaSkill" in skill_unlock else ""
    check("appendSagaSkill uses skillMax", "skillMax(skills, id, fallbackMax)" in saga_fn)
    check("appendSagaSkill no hardcoded max param alone",
          "int max = skillMax" in saga_fn)
    check("appendStrengthLine uses skillMax without Math.min(10",
          "Math.min(10, skillMax" not in skill_unlock.split("private static void appendStrengthLine")[1].split("private static void how")[0]
          if "private static void appendStrengthLine" in skill_unlock else False)

    print("\n=== Spar TP message toggle (2.3.44) ===")
    spar_store = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparStore.java")
    spar_combat = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparCombat.java")
    spar_sys = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.java")
    spar_api = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/SparGuiApi.java")
    spar_cmds = read(SRC / "com/dbzlegacy/adaptivedifficulty/command/SparCommands.java")
    spar_chest = read(gui_root / "SparChestGui.java")
    spar_cmi = read(gui_root / "CmiSparGui.java")
    bukkit_plugin = read(gui_root / "AdaptiveDifficultyGuiPlugin.java")
    check("SparStore tpMessages prefs", "tpMessagesOn" in spar_store and "setTpMessages" in spar_store)
    check("SparCombat tickTpMessages", "tickTpMessages" in spar_combat)
    check("flushTpMessage respects prefs", "tpMessagesOn" in spar_combat)
    check("SparringSystem.setTpMsg", "setTpMsg" in spar_sys)
    check("SparGuiApi tpmsg action", '"tpmsg"' in spar_api)
    check("Spar GUI TP Msg toggle", 'SlotAction.act("tpmsg"' in spar_chest)
    check("CMI Spar TP Msg toggle", '"tpmsg"' in spar_cmi and "TP Msg" in spar_cmi)
    check("no public Forge /spar tpmsg", 'm_82127_("tpmsg")' not in spar_cmds)
    check("no public Bukkit /spar tpmsg",
          'ForgeBridge.sparHandleDo(player, "tpmsg"' not in bukkit_plugin)

    print("\n=== Global TP boost stacking (2.3.44) ===")
    boost = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/tp/GlobalTpBoost.java")
    check("stack keeps max multiplier", "Math.max(prevMult, multiplier)" in boost)
    check("stack always adds duration", "Math.max(now, currentEnd) + durationMs" in boost)
    check("highest wins messaging", "highest wins" in boost)

    print("\n=== GUI action feedback (2.3.44) ===")
    gui_chat = read(gui_root / "GuiChat.java")
    gui_fb = read(gui_root / "GuiFeedback.java")
    check("GuiFeedback class", "class GuiFeedback" in gui_fb)
    check("sendResult prefers GUI", "GuiFeedback.preferGui" in gui_chat and "GuiFeedback.setFromResult" in gui_chat)
    check("chest open paints feedback", "openChest" in gui_fb and "paintChest" in gui_fb)
    check("CMI open paints feedback", "openCmi" in gui_fb)
    prestige = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigeSystem.java")
    check("prestige confirm returns String", "public static String confirmOrPrompt" in prestige)
    check("prestige prefers GUI feedback", "preferGuiFeedback" in prestige)

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
