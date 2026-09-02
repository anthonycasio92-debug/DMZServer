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
    check("VERSION 2.3.152", 'VERSION = "2.3.152"' in mod)

    med = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/skills/MeditationProgression.java")
    check("meditation hints use ScreenNotify subtitle", "ScreenNotify.hint" in med and "med2_progress_subtitle" in med)
    check("meditation full-ki hint only while charging", 'if (charging)' in med and "Ki full" in med)
    check("meditation no chat tellCondition", "DmzRewards.msg(player, LmChat.tagged(\"Meditation\", text))" not in med)

    mob_scaling = read(SRC / "com/dbzlegacy/adaptivedifficulty/scaling/MobScaling.java")
    end_str = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/end/EndDimensionStrength.java")
    check("End Dragon AD profile paint", "applyEndDragonAdProfile" in mob_scaling)
    check("player dragon skips End DEF sponge", "No End DEF mitigation" in end_str or "Adaptive Difficulty attributes own the fight" in end_str)
    check("applySummonerAdStats uses MobScaling AD paint", "MobScaling.applyEndDragonAdProfile" in end_str)

    race_skill = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/bridge/RaceSkillSync.java")
    check("RaceSkillSync discovers DMZ races", "discoverDmzRaceIds" in race_skill)
    check("RaceSkillSync grants Fabled skill", "addSkillExternally" in race_skill and "race_skill" in race_skill)
    check("RaceSkillSync skips race-lock purchase gates", "isPurchaseGatedSkill" in race_skill)

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
        "tierCostLevelAnchor": "150_000.0",
        "tierCostT7TargetCopper": "10_000_000L",
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
    check("DMZ raw-damage tag clear after AD landing", "clearDmzRawDamageOverride" in events and "dmz_raw_damage" in events)
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
    check("formula revision 41", "mix(h, 41L)" in profile)
    check("README pierce clamp", "Post-pierce" in readme or "post-pierce" in readme)

    print("\n=== Telemetry retune (1.0.28) ===")
    check("T1 threat floor 0.52", "case 1 -> 0.52" in profile)
    check("T6 form nudge 1.90", "case 6 -> 1.90" in profile)
    check("T6 liveShare 0.80", "case 6 -> 0.80" in profile)
    check("README 1.0.28 retune", "1.0.28" in readme and "hits-2026-08-05" in readme)

    print("\n=== Mob HP trim (1.0.29 / 1.0.30) ===")
    check("stock mobHealthScale 0.75", field_default(cfg, "mobHealthScale") == "0.75")
    check("mobHpTrimMigratedV1", "mobHpTrimMigratedV1" in cfg)
    check("mobHpTrimMigratedV2", "mobHpTrimMigratedV2" in cfg)
    check("README 1.0.30 HP trim", "1.0.30" in readme and "0.75" in readme)

    print("\n=== Telemetry retune (1.0.24) ===")
    check("formula revision 41", "mix(h, 41L)" in profile)
    check("T1 landCap 0.18", "case 1 -> 0.18" in profile)
    check("T1 landFrac 0.13", "case 1 -> 0.13" in profile)
    check("T6 landFrac 0.58", "case 6 -> 0.58" in profile)
    check("T4 landFrac 0.48", "case 4 -> 0.48" in profile)
    check("T5 landFrac 0.50", "case 5 -> 0.50" in profile)
    check("KP landing 1.5%/lvl", "kiProtectionLevel * 0.015" in profile)
    check("fill-to-landing floor", "preAmount < land)" in events and "land * 0.45" not in events)
    check("progressive soft-caps", "case 5 -> 0.52" in events and "case 6 -> 0.60" in events and "case 7 -> 0.62" in events)
    check("README 1.0.25 balance", "1.0.25" in readme and "T4 tank" in readme)
    check("README 2.3.61 tier costs", "2.3.61" in readme and "tierCostLevelAnchor" in readme and "100× Netherite" in readme)
    check("README 2.3.62 gui level pull", "2.3.62" in readme and "prepareGui" in actions)
    check("README 2.3.63 death drop guard", "DeathDropGuard" in readme and "AllowCombatItemDrop" in readme)
    check("scheduleLevelPull on login race", "scheduleLevelPull" in actions)
    check("Character-null not transformed", "never freeze sampling" in progression)
    check("guiDisplayDmzLevel for Buy GUI", "guiDisplayDmzLevel" in progression)
    check("recompute level when getLevel placeholder", "recomputeLevelFromStats" in progression)
    check("form paint clears AreaDifficulty", "AreaDifficulty.clearCache" in read(SRC / "com/dbzlegacy/adaptivedifficulty/service/DifficultyActions.java"))
    check("Rival form surge uses form mult", "liveFormMultiplier" in read(SRC / "com/dbzlegacy/adaptivedifficulty/rival/RivalInstinct.java"))
    check("CR releasedStatPower", "releasedStatPower" in read(SRC / "com/dbzlegacy/adaptivedifficulty/calc/CombatRating.java"))
    check("CombatRating DISPLAY_ABS_CAP", "DISPLAY_ABS_CAP" in read(SRC / "com/dbzlegacy/adaptivedifficulty/calc/CombatRating.java"))
    check("polluted highestDmzLevel<=1 ignored", "hw <= 1L" in unlock)
    check("prepareDifficultyGui bridge", "prepareDifficultyGui" in bridge)
    gui_plugin_src = read(GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/AdaptiveDifficultyGuiPlugin.java")
    death_guard = read(GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/DeathDropGuard.java")
    check("DeathDropGuard registered", "new DeathDropGuard()" in gui_plugin_src)
    check("DeathDropGuard uncancels when dead", "isDead()" in death_guard and "setCancelled(false)" in death_guard)
    cache_src = read(SRC / "com/dbzlegacy/adaptivedifficulty/cache/DifficultyCache.java")
    check("refresh samples DMZ level", "sampleLevelOnGuiOpen" in cache_src)
    check("tierCostDivisorMigratedV1", "tierCostDivisorMigratedV1" in cfg)
    check("tierCostCurveMigratedV2", "tierCostCurveMigratedV2" in cfg)
    check("tierCostLevelMultiplier", "tierCostLevelMultiplier" in cfg)
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
    check("README version 2.3.63", "2.3.63" in readme)

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
    check("T3 soft-cap 0.44", "case 3 -> 0.44" in events and "case 3 -> 0.44" in profile)
    check("T4 soft-cap 0.50", "case 4 -> 0.50" in events and "case 4 -> 0.50" in profile)
    check("T1 soft-cap 0.34", "default -> 0.34" in events and "default -> 0.34" in profile)
    check("T2 soft-cap 0.36", "case 2 -> 0.36" in events and "case 2 -> 0.36" in profile)
    check("README soft-cap ladder", "0.36" in readme and "soft-cap" in readme.lower())

    print("\n=== Ladder retune (1.0.35) ===")
    check("T4 form nudge 1.58", "case 4 -> 1.58" in profile)
    check("T5 form nudge 1.78", "case 5 -> 1.78" in profile)
    check("T7 form nudge 1.62", "default -> 1.62" in profile)
    check("T4 liveShare 0.60", "case 4 -> 0.60" in profile)
    check("T5 liveShare 0.74", "case 5 -> 0.74" in profile)
    check("T4 landCap 0.50", "case 4 -> 0.50" in profile)
    check("T5 landCap 0.52", "case 5 -> 0.52" in profile)
    check("T3 landFrac 0.30", "case 3 -> 0.30" in profile)
    check("T7 landFrac 0.60", "default -> 0.60" in profile)
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

    print("\n=== Coin drop chances (2.3.54) ===")
    coins = read(SRC / "com/dbzlegacy/adaptivedifficulty/currency/AncientCoinEconomy.java")
    check("drop chance gate in rollKillLoot", "ancientCoinDropChance" in coins and "roll >= dropChance" in coins)
    check("dual upgrade band uses same roll", "roll < upgradeChance" in coins)
    check("coinDropChanceMigratedV1", "coinDropChanceMigratedV1" in cfg)
    check("admin set drop chance", "ancientcoindropchance" in cmds or "coindropchance" in cmds)
    check("README 5% / 0.5%", "5%" in readme and "0.5%" in readme and "2.3.63" in readme)

    print("\n=== Mohist CMI /lmdo routing (2.3.54) ===")
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

    print("\n=== CNPC migrate safety (2.3.54) ===")
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

    print("\n=== Staff clear player (2.3.54) ===")
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

    print("\n=== Spar recent sessions (2.3.54) ===")
    spar_store = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparStore.java")
    spar_sys = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.java")
    check("RECENT_SESSION_LIMIT 3", "RECENT_SESSION_LIMIT = 3" in spar_store)
    check("recentSessions map", "recentSessions" in spar_store and "pushRecent" in spar_store)
    check("recordRecentSession on end", "recordRecentSession" in spar_sys)
    check("statsLines individual reports", "Spar Report #" in spar_sys and "appendRecentReport" in spar_sys)
    check("statsLines no live TP this session", "TP this session" not in spar_sys)

    print("\n=== Skill Check perm gate (2.3.54) ===")
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

    print("\n=== Skill Check icons (2.3.54) ===")
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

    print("\n=== GUI coherence (2.3.54) ===")
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
    check("Prestige wallet GOLD_INGOT",
          'Material.GOLD_INGOT' in prestige and "putWallet" in prestige)
    check("Prestige Progression BREWING_STAND",
          'Material.BREWING_STAND, "&dProgression"' in prestige)
    check("Rival Progress compact hub",
          'Inventory inv = Bukkit.createInventory(holder, 27' in rival
          and 'SlotAction.page("records")' in rival
          and '"&eStats"' in rival)
    check("Rival Progress HOF+Journal secondary",
          'SlotAction.page("hof")' in rival and 'SlotAction.page("journal")' in rival
          and "records" in rival)

    print("\n=== Rival relationship semantics (2.3.142+) ===")
    rival_sys = read(SRC / "com/dbzlegacy/adaptivedifficulty/rival/RivalSystem.java")
    rival_st = read(SRC / "com/dbzlegacy/adaptivedifficulty/rival/RivalStatus.java")
    rival_link = read(SRC / "com/dbzlegacy/adaptivedifficulty/rival/RivalLink.java")
    check("Silent label (not Unknown)", 'case UNKNOWN -> "Silent"' in rival_st)
    check("remove demotes mutual to one-way", "demoteToOneWayDeclare" in rival_sys)
    check("remove archives remover history", "archiveRivalLink(me, them.uuid, myLink)" in rival_sys)
    check("pending excluded from rival list", "st == RivalStatus.PENDING" in rival_sys)
    check("visibleDeclare flag", "visibleDeclare" in rival_sys and "visibleDeclare" in rival_link)
    check("declare sets visibleDeclare", "myLink.visibleDeclare = true" in rival_sys)
    check("accept/decline resolve uuid", 'regionMatches(true, 0, "uuid:"' in rival_sys)
    check("nemesis only from challenge death KO", "challengeKo" in rival_sys and "Phase.ACTIVE" in rival_sys)
    check("dual silent → Declared", "promoteDeclared" in rival_sys)
    rival_chest = read(gui_root / "RivalChestGui.java")
    cmi_rival = read(gui_root / "CmiRivalGui.java")
    check("pending_decide submenu page",
          "pending_decide:" in rival_chest and "pendingDecide" in rival_chest
          and "pending_decide:" in cmi_rival and "openPendingDecide" in cmi_rival)
    check("chest pending head opens decide submenu",
          'SlotAction.page("pending_decide:' in rival_chest)
    check("CMI pending head opens decide submenu",
          'lmdo rival page pending_decide:' in cmi_rival)
    check("acceptedMutualOffer flag", "acceptedMutualOffer" in rival_link)
    check("needsMutualConfirm helper", "needsMutualConfirm" in rival_link)
    check("promoteDeclared sets inviteReceived both",
          "inviteReceived = true" in rival_sys and "Both must Accept" in rival_sys)
    check("accept waits for other Mutual confirm",
          "Waiting for them to Accept" in rival_sys or "waiting for them to Accept" in rival_sys)
    check("acceptReplace Mutual slot pick",
          "acceptReplace" in rival_sys and "needsMutualReplacePick" in rival_sys
          and "pendingMutualAcceptUuid" in read(SRC / "com/dbzlegacy/adaptivedifficulty/rival/RivalPlayerRecord.java"))
    check("GUI pick_replace_mutual page",
          "pick_replace_mutual" in rival_chest and "pick_replace_mutual" in cmi_rival)
    check("rival list status order helper", "rivalListOrder" in rival_sys and "statusListRank" in rival_sys)
    prox = read(SRC / "com/dbzlegacy/adaptivedifficulty/rival/RivalProximity.java")
    under_fn = prox.split("tryUnderdogEngage", 1)[1].split("handleDamagedByRival", 1)[0] if "tryUnderdogEngage" in prox else ""
    check("underdog any declaredByMe status",
          "!link.declaredByMe" in under_fn and "link.mutual" not in under_fn)
    check("Actions is Declare Pending Remove Silent",
          'page("pick_declare")' in rival_chest
          and 'page("pending")' in rival_chest
          and 'page("pick_remove")' in rival_chest
          and 'page("pick_silent")' in rival_chest
          and "Accept Declared" not in rival_chest
          and "Accept Declared" not in cmi_rival)
    check("Spar Stats button BOOK",
          'Material.BOOK, "&eStats"' in spar)
    check("Skills admin header BOOK",
          "skillCheckUi ? Material.EXPERIENCE_BOTTLE : Material.BOOK" in skills)

    print("\n=== Android convert + remove (2.3.54) ===")
    android = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/race/AndroidConversion.java")
    prog_chest = read(gui_root / "ProgressionChestGui.java")
    prog_cmi = read(gui_root / "CmiProgressionGui.java")
    prog_tree = read(gui_root / "ProgressionCommandTree.java")
    forge_bridge = read(gui_root / "ForgeBridge.java")
    gui_api = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/ProgressionGuiApi.java")
    check("AndroidConversion.remove", "public static String remove(" in android)
    check("AndroidConversion two-click confirm", "CONFIRM_MS = 10_000L" in android)
    check("AndroidConversion restore superforms", 'ensureSkillAtZero(skills, "superforms")' in android)
    check("Android blocks bioandroid only", 'BLOCKED = Set.of("bioandroid")' in android)
    check("Android gate via androidforms TP costs", "getFormSkillTpCosts" in android and "ANDROID_FORM_GROUP" in android)
    check("Android eligibleRaceHint", "eligibleRaceHint" in android and "configuredAndroidRaceIds" in android)
    check("Android isAndroidUpgraded helper", "public static boolean isAndroidUpgraded(" in android)
    check("Android deny message not humans-only", "Only races with android forms (humans)" not in android)
    check("GUI lists all Android-capable races", "Frost Demon" in prog_chest and "Viltrumite" in prog_chest
          and "Frost Demon" in prog_cmi)
    tel = read(SRC / "com/dbzlegacy/adaptivedifficulty/telemetry/BalanceTelemetry.java")
    check(
        "telemetry logs android upgrade flag",
        '\\"android\\"' in tel and "isAndroidUpgraded" in tel,
    )
    races_root = Path("/workspace/config/dragonminez/races")
    if not races_root.is_dir():
        races_root = ROOT.parents[1] / "config" / "dragonminez" / "races"
    for race in ("human", "saiyan", "frostdemon", "viltrumite"):
        check(
            f"stock {race} androidforms.json",
            (races_root / race / "forms" / "androidforms.json").is_file(),
        )
        ch_path = races_root / race / "character.json"
        ch = ch_path.read_text(encoding="utf-8", errors="replace") if ch_path.is_file() else ""
        check(f"stock {race} androidforms TP costs", '"androidforms"' in ch and '"prices"' in ch)
    check(
        "bioandroid has no androidforms upgrade group",
        not (races_root / "bioandroid" / "forms" / "androidforms.json").is_file(),
    )
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

    print("\n=== Player Prestige access (2.3.54) ===")
    prestige_menu = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/PrestigeMenu.java")
    prestige_sys = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigeSystem.java")
    prog_cmds = read(SRC / "com/dbzlegacy/adaptivedifficulty/command/ProgressionCommands.java")
    check("PrestigeMenu no staff gate", "StaffAccess.isStaff" not in prestige_menu)
    check("PrestigeSystem.open no staff gate",
          "Staff only" not in prestige_sys.split("public static void open")[1].split("public static void confirmOrPrompt")[0]
          if "public static void open" in prestige_sys else False)
    check("Forge /prestige slash staff-only",
          '.requires(ProgressionCommands::staff)' in prog_cmds.split('m_82127_("prestige")')[1].split('m_82127_("skills")')[0]
          if 'm_82127_("prestige")' in prog_cmds else False)
    check("Prestige chat uses lmdo not /prestige",
          "/lmdo prestige confirm" in prestige_sys and "/prestige do confirm" not in prestige_sys)
    check("Hub Prestige for everyone",
          'SlotAction.open("prestige")' in hub
          and hub.count('SlotAction.open("prestige")') >= 1)
    check("handlePrestige redirects players to /lm",
          "/lm" in plugin.split("private boolean handlePrestige")[1].split("private boolean handleSkills")[0]
          and "isStaff(player)" in plugin.split("private boolean handlePrestige")[1].split("private boolean handleSkills")[0]
          if "private boolean handlePrestige" in plugin else False)

    print("\n=== Player command whitelist (2.3.54) ===")
    prog_tree = read(gui_root / "ProgressionCommandTree.java")
    gui_api = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/ProgressionGuiApi.java")
    # Non-staff commandHelp block (first join after if (!staff)).
    ns_help = gui_api.split("if (!staff)")[1].split("return String.join")[1].split(");")[0] if "if (!staff)" in gui_api else ""
    check("non-staff commandHelp has meditation", "/progression meditation" in ns_help)
    check("non-staff commandHelp no android slash", "/progression android remove" not in ns_help)
    check("non-staff commandHelp points to /lm", "/lm" in ns_help)
    check("player bare progression lists /lm /skillcheck",
          "Everything else: open" in prog_tree and "/skillcheck" in prog_tree)
    check("Forge helpOrGui no android slash",
          "/progression android remove" not in prog_cmds.split("helpOrGui")[1].split("private static int gui")[0]
          if "helpOrGui" in prog_cmds else False)
    check("Forge android remove hints GUI for players", "androidRemoveGuiHint" in prog_cmds)

    print("\n=== End ki purge silent (2.3.54) ===")
    end_str = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/end/EndDimensionStrength.java")
    purge_body = end_str.split("private static void purgeEndKiCommands")[1].split("private static EntityType")[0] \
        if "private static void purgeEndKiCommands" in end_str else ""
    check("purgeEndKi uses collectEndKiEntities", "collectEndKiEntities(end)" in purge_body)
    check("purgeEndKi no console kill ki_laser",
          'kill @e[type=dragonminez:ki_laser]' not in end_str)
    check("purgeEndKi no console kill ki_blast",
          'kill @e[type=dragonminez:ki_blast]' not in end_str)
    check("clearEndCrystals no empty kill spam",
          'kill @e[type=minecraft:end_crystal]' not in end_str)

    print("\n=== Dragon ki launch fix (2.3.55) ===")
    spawn_fire = end_str.split("private static boolean spawnAndFireKi")[1].split(
        "private static void launchDragonKiToward")[0] if "private static boolean spawnAndFireKi" in end_str else ""
    check("spawnAndFireKi never aborts on failed add",
          "return false" not in spawn_fire.split("end.m_7967_")[1].split("int life")[0]
          if "end.m_7967_" in spawn_fire else False)
    check("spawnAndFireKi always fireHability", "fireHability" in spawn_fire)
    check("dragon ki explicit launchToward", "launchDragonKiToward" in end_str)
    blast_fn = end_str.split("private static boolean fireDragonKiBlast")[1].split(
        "private static boolean spawnAndFireKi")[0] if "private static boolean fireDragonKiBlast" in end_str else ""
    beam_fn = end_str.split("private static boolean fireDragonKiBeam")[1].split(
        "private static boolean fireDragonKiBlast")[0] if "private static boolean fireDragonKiBeam" in end_str else ""
    check("dragon blast uses mob setupKiLargeBlast first",
          "setupKiLargeBlast" in blast_fn.split("catch")[0] if blast_fn else False)
    check("dragon beam prefers setupKiLaser cast 0",
          "setupKiLaser" in beam_fn.split("catch")[0] if beam_fn else False)
    check("dragon blast unparks controllable",
          "setParked(false)" in blast_fn and "setControllable(false)" in blast_fn)

    print("\n=== End exit podium repair (2.3.56) ===")
    check("repairEndExitPodium present", "repairEndExitPodium" in end_str)
    check("cmdRepairPodium present", "cmdRepairPodium" in end_str)
    enddragon_cmds = read(SRC / "com/dbzlegacy/adaptivedifficulty/command/ProgressionCommands.java")
    check("enddragon repair subcommand", 'm_82127_("repair")' in enddragon_cmds)
    check("egg clear uses dragon_egg SRG f_50260_",
          "Blocks.f_50260_" in end_str and "isDragonEggBlock" in end_str)
    check("end portal uses SRG f_50257_",
          "Blocks.f_50257_" in end_str and "isEndPortalBlock" in end_str)
    portal_guard = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/end/EndPortalGuard.java")
    check("EndPortalGuard end_portal f_50257_", "Blocks.f_50257_" in portal_guard)
    check("EndPortalGuard end_gateway f_50446_", "Blocks.f_50446_" in portal_guard)
    check("EndPortalGuard no end_stone as portal fast-path",
          "block == Blocks.f_50259_" not in portal_guard)

    print("\n=== Skill Check real max levels (2.3.54) ===")
    skill_unlock = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/SkillUnlockService.java")
    saga_fn = skill_unlock.split("private static void appendSagaSkill")[1].split("private static void appendStrengthLine")[0] \
        if "private static void appendSagaSkill" in skill_unlock else ""
    check("appendSagaSkill uses skillMax", "skillMax(skills, id, fallbackMax)" in saga_fn)
    check("appendSagaSkill no hardcoded max param alone",
          "int max = skillMax" in saga_fn)
    check("appendStrengthLine uses skillMax without Math.min(10",
          "Math.min(10, skillMax" not in skill_unlock.split("private static void appendStrengthLine")[1].split("private static void how")[0]
          if "private static void appendStrengthLine" in skill_unlock else False)

    print("\n=== Spar TP message toggle (2.3.54) ===")
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

    
    
    print("\n=== Spar CNPC multiplier parity (2.3.145) ===")
    spar_c145 = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparCombat.java")
    build_fn = spar_c145.split("buildTotalMultiplier", 1)[1].split("maxTpForAction", 1)[0] if "buildTotalMultiplier" in spar_c145 else ""
    check("buildTotal has BP rival release gravity weight",
          "battlePowerMultiplier" in build_fn and "rivalMultiplier" in build_fn
          and "releaseMultiplier" in build_fn and "gravityMultiplier" in build_fn
          and "weightMultiplier" in build_fn)
    check("buildTotal has prestige momentum session streak style perfect",
          "prestigeMultiplier" in build_fn and "momentumMultiplier" in build_fn
          and "sessionBonusMultiplier" in build_fn and "streakMultiplier" in build_fn
          and "styleMultiplier" in build_fn and "PERFECT_TRAINING_MULTIPLIER" in build_fn)
    check("GLOBAL_TP_GAIN_MULT 1.50", "GLOBAL_TP_GAIN_MULT = 1.50f" in spar_c145)
    check("sparPrestigeLevel Fabled fallback", "sparPrestigeLevel" in spar_c145 and "fabledPrestigeLevel" in spar_c145)
    check("momentum messages", "SHOW_MOMENTUM_MESSAGES" in spar_c145 and "Momentum " in spar_c145)
    check("TP activeBonusTags", "activeBonusTags" in spar_c145)
    check("MOMENTUM_MULTIPLIERS script values",
          "1.05f, 1.10f, 1.20f, 1.35f, 1.50f, 2.00f" in spar_c145)

    print("\n=== Spar TP lines player/staff (2.3.147) ===")
    spar_c147 = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparCombat.java")
    flush_fn = spar_c147.split("public static void flushTpMessage", 1)[1].split(
        "public static void updateMomentum", 1)[0] if "public static void flushTpMessage" in spar_c147 else ""
    check("flushTpMessage staff gate",
          "StaffAccess.isStaff(player)" in flush_fn)
    check("player TP line is simple +TP (label)",
          'DmzRewards.msg(player, base)' in flush_fn
          and "activeBonusTags" not in flush_fn.split("if (!StaffAccess.isStaff(player))", 1)[0]
          if "if (!StaffAccess.isStaff(player))" in flush_fn else False)
    check("staff TP line has bonus + stack + session",
          "activeBonusTags" in flush_fn and "staffStackTags" in flush_fn
          and "session" in flush_fn and "m/k/c" in flush_fn)
    check("staffStackTags API", "staffStackTags(" in spar_c147 and "burstLabel(" in spar_c147)

    print("\n=== Mentor Actions GUI (2.3.146) ===")
    spar_chest146 = read(gui_root / "SparChestGui.java")
    spar_cmi146 = read(gui_root / "CmiSparGui.java")
    mentor_hub = spar_chest146.split("private Inventory mentor(", 1)[1].split("private Inventory pending(", 1)[0] \
        if "private Inventory mentor(" in spar_chest146 else ""
    cmi_mentor_hub = spar_cmi146.split("private static void openMentor(", 1)[1].split(
        "private static void openPending(", 1)[0] if "private static void openMentor(" in spar_cmi146 else ""
    check("chest mentor Actions hub",
          "Mentor Actions" in mentor_hub
          and 'page("pick_apprentice")' in mentor_hub
          and 'page("pending")' in mentor_hub
          and 'page("dojo")' in mentor_hub
          and 'page("pick_accept")' not in mentor_hub
          and 'page("pick_decline")' not in mentor_hub)
    check("chest pending_decide + outgoing cancel",
          "pending_decide:" in spar_chest146 and "pendingDecide" in spar_chest146
          and 'SlotAction.act("mentor_cancel"' in spar_chest146)
    check("CMI mentor Actions + pending_decide + dojo",
          "openPendingDecide" in spar_cmi146 and "openDojo" in spar_cmi146
          and "Mentor Actions" in spar_cmi146
          and 'lmdo spar page pending_decide:' in spar_cmi146)
    check("CMI mentor hub has no Accept…/Decline…",
          "spar.mentor.accept" not in cmi_mentor_hub
          and "spar.mentor.decline" not in cmi_mentor_hub
          and "pick_apprentice" in cmi_mentor_hub
          and '"dojo"' in cmi_mentor_hub)

    print("\n=== Mentor invite + dojo peer bonus (2.3.149) ===")
    spar_sys149 = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.java")
    spar_c149 = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparCombat.java")
    check("mentor invite 1h constant", "MENTOR_INVITE_MS = 60L * 60L * 1000L" in spar_sys149)
    check("dojo peer helper", "isSparringWithDojoPeer" in spar_sys149)
    check("DOJO_PEER_SPAR_BONUS_PCT 0.10", "DOJO_PEER_SPAR_BONUS_PCT = 0.10f" in spar_c149)
    check("awardCombatTp uses dojo peer bonus",
          "withDojoPeer" in spar_c149 and "DOJO_PEER_SPAR_BONUS_PCT" in spar_c149)

    print("\n=== Mentor cooldown tooltip sanitize (2.3.150) ===")
    gui_tips = read(GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/GuiTooltips.java")
    tips_json = read(ROOT / "tools/dmz-adaptive-difficulty-gui/src/main/resources/gui-tooltips.json")
    check("STALE_COPY_FIXES present", "STALE_COPY_FIXES" in gui_tips and "sanitizeStaleCopy" in gui_tips)
    check("stale 7-day → 12-hour rewrite", '"&87-day cooldown"' in gui_tips and '"&812-hour cooldown"' in gui_tips)
    check("jar leave lore is 12-hour", '"&812-hour cooldown after leaving"' in tips_json)
    check("jar leave lore not 7-day", '"&87-day cooldown"' not in tips_json)

    print("\n=== Mentor tip placeholders (2.3.152) ===")
    chest = read(GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/SparChestGui.java")
    cmi = read(GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/CmiSparGui.java")
    check("leave_none tip key", "spar.mentor.leave_none" in chest and "spar.mentor.leave_none" in cmi)
    check("release_none tip key", "spar.mentor.release_none" in chest and "spar.mentor.release_none" in cmi)
    check("FORCE_JAR_LORE_KEYS mentor tips", "FORCE_JAR_LORE_KEYS" in gui_tips and "forceJarLoreKeysToDisk" in gui_tips)
    check("jar leave_none present", '"leave_none"' in tips_json and "You have no mentor" in tips_json)
    check("jar release uses {name} dojo", '"&7Dojo &f{name}"' in tips_json)
    check("CMI pageBtn accepts vars", "Map<String, String> vars, String... tips" in cmi)


    print("\n=== Dojo membership roster (2.3.152) ===")
    spar_sys152 = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.java")
    spar_api152 = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/SparGuiApi.java")
    chest152 = read(GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/SparChestGui.java")
    cmi152 = read(GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/CmiSparGui.java")
    bridge152 = read(GUI_SRC / "com/dbzlegacy/adaptivedifficulty/bukkit/ForgeBridge.java")
    check("membershipDojoCards system", "membershipDojoCards" in spar_sys152)
    check("membershipDojoCards SparGuiApi", "membershipDojoCards" in spar_api152)
    check("ForgeBridge membershipDojoCards", "sparMembershipDojoCards" in bridge152)
    check("Chest dojo_member/dojo_mine", "dojo_member" in chest152 and "dojo_mine" in chest152)
    check("CMI dojo_member/dojo_mine", "dojo_member" in cmi152 and "dojo_mine" in cmi152)
    check("Dojo opens for apprentices", 'hasMentor || hasApprentice' in chest152 and 'hasMentor || hasApprentice' in cmi152)

    print("\n=== Mentor dojo multi-apprentice (2.3.144) ===")
    spar_store144 = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparStore.java")
    spar_sys144 = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.java")
    spar_combat144 = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparCombat.java")
    check("12h mentor cooldown", "12L * 60L * 60L * 1000L" in spar_sys144)
    check("MAX_APPRENTICES 8", "MAX_APPRENTICES = 8" in spar_sys144)
    check("apprentices list field", "List<ApprenticeRef> apprentices" in spar_store144)
    check("normalizeApprentices migration", "normalizeApprentices" in spar_store144)
    check("share diluted by roster", "MENTOR_SHARE_PCT / (float) roster" in spar_sys144)
    check("apprenticeCards API", "apprenticeCards" in spar_sys144)
    check("MENTOR_SHARE_PCT 0.15", "MENTOR_SHARE_PCT = 0.15f" in spar_combat144)

    print("\n=== Spar active TP + ki-charge hold (2.3.143) ===")
    spar_rt = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparPlayerRuntime.java")
    spar_sys143 = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.java")
    events143 = read(SRC / "com/dbzlegacy/adaptivedifficulty/event/DifficultyEvents.java")
    hold_fn = spar_sys143.split("private static boolean holdSparForKiOrClash", 1)[1].split(
        "public static void markKiCharging", 1)[0] if "holdSparForKiOrClash" in spar_sys143 else ""
    release_fn = spar_sys143.split("private static void tickReleaseControl", 1)[1].split(
        "private static void tickPerfectBanner", 1)[0] if "private static void tickReleaseControl" in spar_sys143 else ""
    check("lastCombatOut clocks", "lastCombatOutAt" in spar_rt and "lastCombatOutPartner" in spar_rt)
    check("holdUntil + chargingUntil fields", "holdUntil" in spar_rt and "chargingUntil" in spar_rt)
    check("hold does not stampHitActivity", "stampHitActivity" not in hold_fn)
    check("markKiCharging public API", "public static void markKiCharging(" in spar_sys143)
    check("KiChargeEvent wires spar hold", "SparringSystem.markKiCharging(player)" in events143)
    check("release drip uses lastCombatOut", "hasRecentCombatOut" in release_fn and "lastCombatOut" in spar_sys143)
    check("release drip skips grace", "graceUntil" in release_fn)
    check("release drip requires live both-clash or combat",
          "bothClashing" in release_fn and "hasRecentCombatOut" in release_fn)
    check("KI_CHARGE_HOLD_MS present", "KI_CHARGE_HOLD_MS" in spar_sys143)

    print("\n=== Global TP boost stacking (2.3.54) ===")
    boost = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/tp/GlobalTpBoost.java")
    check("stack keeps max multiplier", "Math.max(prevMult, multiplier)" in boost)
    check("stack always adds duration", "Math.max(now, currentEnd) + durationMs" in boost)
    check("highest wins messaging", "highest wins" in boost)

    print("\n=== Global TP boost persistence (2.3.60) ===")
    cfg_paths = read(SRC / "com/dbzlegacy/adaptivedifficulty/config/ConfigPaths.java")
    events = read(SRC / "com/dbzlegacy/adaptivedifficulty/event/DifficultyEvents.java")
    check("globalTpBoostPath", "global-tp-boost.json" in cfg_paths)
    check("GlobalTpBoost.load", "public static synchronized void load()" in boost)
    check("GlobalTpBoost.save", "public static synchronized void save()" in boost)
    check("persist on activate", "persistNow()" in boost)
    check("login strips leftover when inactive",
          "removeEffect(player)" in boost.split("onLogin")[1].split("startBoost")[0])
    check("server starting loads boost", "GlobalTpBoost.load()" in events)
    check("server stopping saves boost", "GlobalTpBoost.save()" in events)
    check("SCRIPT-AUDIT notes persistence",
          "global-tp-boost.json" in read(ROOT / "tools" / "dmz-adaptive-difficulty" / "SCRIPT-AUDIT.md"))

    print("\n=== Console TP boost (2.3.60) ===")
    prog_tree = read(gui_root / "ProgressionCommandTree.java")
    gui_api = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/ProgressionGuiApi.java")
    forge_cmds = read(SRC / "com/dbzlegacy/adaptivedifficulty/command/ProgressionCommands.java")
    forge_bridge = read(gui_root / "ForgeBridge.java")
    check("console androidify leaf", "executeAndroidify" in prog_tree and "androidConvertConsole" in forge_bridge)
    check("console boost example", "progression boost start 2 30" in prog_tree)
    check("GuiApi console default purchaser", 'actor == null ? "Server"' in gui_api)
    check("GuiApi console usage hint", "Console OK (store)" in gui_api)
    check("ForgeBridge boost null actor", "actor == null ? null : nmsPlayer(actor)" in forge_bridge)
    check("brigadier minutes purchaser",
          'IntegerArgumentType.getInteger(ctx, "minutes")' in forge_cmds
          and 'StringArgumentType.getString(ctx, "purchaser")' in forge_cmds)

    print("\n=== GUI action feedback (2.3.54) ===")
    gui_chat = read(gui_root / "GuiChat.java")
    gui_fb = read(gui_root / "GuiFeedback.java")
    check("GuiFeedback class", "class GuiFeedback" in gui_fb)
    check("sendResult prefers GUI", "GuiFeedback.preferGui" in gui_chat and "GuiFeedback.setFromResult" in gui_chat)
    check("chest open paints feedback", "openChest" in gui_fb and "paintChest" in gui_fb)
    check("CMI open paints feedback", "openCmi" in gui_fb)
    prestige = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigeSystem.java")
    check("prestige confirm returns String", "public static String confirmOrPrompt" in prestige)
    check("prestige prefers GUI feedback", "preferGuiFeedback" in prestige)

    print("\n=== Rival title bonuses (2.3.54) ===")
    rival_const = read(SRC / "com/dbzlegacy/adaptivedifficulty/rival/RivalConstants.java")
    rival_prog = read(SRC / "com/dbzlegacy/adaptivedifficulty/rival/RivalProgression.java")
    rival_tp = read(SRC / "com/dbzlegacy/adaptivedifficulty/rival/RivalTpCurve.java")
    check("RpTier has tpMult + perk", "tpMult" in rival_const and "perk" in rival_const)
    check("Mythic Rival 2.00 title mult", '2.00' in rival_const and "Mythic Rival" in rival_const)
    check("titleLines shows Perk", "Perk:" in rival_prog)
    check("titleLines shows TP Gain", "TP Gain:" in rival_prog)
    check("RivalTpCurve applies title mult", "tpMult()" in rival_tp)

    print("\n=== Spar individual reports (2.3.54) ===")
    spar_sys = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.java")
    check("Spar Report # title", "Spar Report #" in spar_sys)
    check("appendRecentReport helper", "appendRecentReport" in spar_sys)
    check("recent stores melee/ki", "rec.melee" in spar_sys and "rec.ki" in spar_sys)

    print("\n=== Script parity fixes (2.3.54) ===")
    spar = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/SparringSystem.java")
    med = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/skills/MeditationProgression.java")
    check("mentor invite 1 hour", "MENTOR_INVITE_MS = 60L * 60L * 1000L" in spar)
    check(
        "invite not 24h / not 2 min",
        "MENTOR_INVITE_MS = 24L" not in spar and "MENTOR_INVITE_MS = 120_000L" not in spar,
    )
    check("manual meditation 30 min", "MANUAL_TRIAL_DURATION_MS = 30L" in med)
    check("auto meditation 15 min", "TRIAL_DURATION_MS = 15L" in med)

    print("\n=== CNPC GUI open no-perm (2.3.54) ===")
    cnpc = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/CnpcGuiOpener.java")
    skill = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/SkillCheckService.java")
    events = read(SRC / "com/dbzlegacy/adaptivedifficulty/event/DifficultyEvents.java")
    end = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/end/EndDimensionStrength.java")
    check("CnpcGuiOpener class", "class CnpcGuiOpener" in cnpc)
    check("CnpcGuiOpener opens rival/spar/hub", '"rival"' in cnpc and '"spar"' in cnpc and '"hub"' in cnpc)
    check("no soft contains(rival)", 'hay.contains("rival")' not in cnpc)
    check("events call tryOpenFromNpc", "CnpcGuiOpener.tryOpenFromNpc" in events)
    check("CnpcGuiOpener tryOpenFromNpc uses tags or names", "tryOpenFromTags" in cnpc and "tryOpenFromName" in cnpc)
    check("Skill Check NPC skips canUse", "openFromNpc(player, \"core\")" in skill and "canUse(player)" not in skill.split("tryOpenFromNpc")[1].split("trigger21")[0])
    check("End hitcap not undone by raw minFrac", "setAmount(mitigated)" in end and "Math.max(mitigated, raw" not in end)
    check("egg clear not end_portal", "isDragonEggBlock" in end and "Blocks.f_50259_" not in end.split("clearDragonEggBlocks")[1].split("findDragons")[0])

    print("\n=== GUI + command audit (2.3.54) ===")
    plugin = read(gui_root / "AdaptiveDifficultyGuiPlugin.java")
    prestige_chest = read(gui_root / "PrestigeChestGui.java")
    prestige_cmi = read(gui_root / "CmiPrestigeGui.java")
    rival_chest = read(gui_root / "RivalChestGui.java")
    spar_chest = read(gui_root / "SparChestGui.java")
    diff_chest2 = read(gui_root / "DifficultyChestGui.java")
    prog_chest2 = read(gui_root / "ProgressionChestGui.java")
    skills_chest = read(gui_root / "SkillsChestGui.java")
    cmi_rival = read(gui_root / "CmiRivalGui.java")
    cmi_spar = read(gui_root / "CmiSparGui.java")
    cmi_diff = read(gui_root / "CmiDifficultyGui.java")
    cmi_skills = read(gui_root / "CmiSkillsGui.java")
    cmi_prog = read(gui_root / "CmiProgressionGui.java")
    mech_cmds = read(SRC / "com/dbzlegacy/adaptivedifficulty/command/MechanicsCommands.java")
    prog_cmds = read(SRC / "com/dbzlegacy/adaptivedifficulty/command/ProgressionCommands.java")
    diff_cmds = read(SRC / "com/dbzlegacy/adaptivedifficulty/command/DifficultyCommands.java")
    mech_chat = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/MechanicsChatMenu.java")
    rival_chat = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/RivalChatMenu.java")
    spar_chat = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/SparChatMenu.java")
    diff_chat = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/DifficultyChatMenu.java")
    prog_menu = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/ProgressionMenu.java")
    audit_md = read(ROOT / "tools" / "dmz-adaptive-difficulty" / "GUI-COMMAND-AUDIT.md")
    check("GUI-COMMAND-AUDIT.md present", "GUI + command-tree audit" in audit_md or "Ship 2.3" in audit_md)
    check("no bare SlotAction.cmd(lm)", 'SlotAction.cmd("lm")' not in rival_chest
          and 'SlotAction.cmd("lm")' not in spar_chest
          and 'SlotAction.cmd("lm")' not in diff_chest2
          and 'SlotAction.cmd("lm")' not in prog_chest2
          and 'SlotAction.cmd("lm")' not in skills_chest
          and 'SlotAction.cmd("lm")' not in prestige_chest)
    check("hub buttons use lmdo lm open hub", 'lmdo lm open hub' in rival_chest and 'lmdo lm open hub' in prestige_cmi)
    check("CMI hub buttons use lmdo", 'lmdo lm open hub' in cmi_rival and 'lmdo lm open hub' in cmi_spar
          and 'lmdo lm open hub' in cmi_diff and 'lmdo lm open hub' in cmi_skills
          and 'lmdo lm open hub' in cmi_prog)
    check("no Prestige SlotAction.open", "SlotAction.open(" not in prestige_chest)
    check("inspect android_remove case", 'case "android_remove"' in plugin or '"android_remove"' in plugin.split("openInspectSystem")[1].split("openInspectForUuid")[0])
    check("lmdo skills staff gate",
          'case "skills" ->' in plugin
          and 'Staff only. Use Skill Check if you have access.' in plugin)
    check("lmdo skillcheck donator gate",
          'case "skillcheck" ->' in plugin
          and "hasSkillCheck(player)" in plugin
          and "Skill Check requires donator access." in plugin)
    check("lmdo skillcheck allows NPC session",
          "inSkillCheckSession(player)" in plugin)
    check("CMI skillcheck page uses lmdo skillcheck",
          'skillCheckUi ? "skillcheck" : "skills"' in cmi_skills
          or 'lmdo skillcheck page' in cmi_skills)
    check("CNPC editor tool skip",
          "holdingEditorTool" in read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/CnpcGuiOpener.java")
          and "holdingEditorTool" in read(SRC / "com/dbzlegacy/adaptivedifficulty/event/DifficultyEvents.java"))
    check("npcscripter id listed",
          "customnpcs:npcscripter" in read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/CnpcGuiOpener.java"))
    check("android_remove inventory force", "isAndroidRemovePage" in plugin)
    check("enddragon uses StaffAccess", "ProgressionCommands::staff" in prog_cmds and "enddragon" in prog_cmds)
    check("skillcheck no level-2 grant", "src.m_6761_(2)" not in prog_cmds.split("skillCheck(")[1].split("helpOrGui")[0])
    check("progression helpOrGui", "helpOrGui" in prog_cmds)
    check("DifficultyCommands StaffAccess", "StaffAccess.isStaff" in diff_cmds)
    check("chat hub prestige + android_remove", 'lm open prestige' in mech_chat and 'android_remove' in mech_chat)
    check("rival/spar chat hub lmdo", 'lmdo lm open hub' in rival_chat and 'lmdo lm open hub' in spar_chat)
    check("difficulty chat hub button", 'lmdo lm open hub' in diff_chat)
    check("ProgressionMenu allows android_remove", "isAndroidRemovePage" in prog_menu)
    check("MechanicsCommands android_remove opens GUI", "openProgression(player, \"android_remove\")" in mech_cmds)

    print("\n=== Script parity deep check (2.3.54) ===")
    yardrat = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/race/YardratProgression.java")
    race_lock = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/race/RaceLock.java")
    shadow = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/dummy/ShadowDummyLimiter.java")
    boost = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/tp/GlobalTpBoost.java")
    script_audit = read(ROOT / "tools" / "dmz-adaptive-difficulty" / "SCRIPT-AUDIT.md")
    check("Yardrat uses kimanipulation", 'SKILL_ONE = "kimanipulation"' in yardrat)
    check("Yardrat uses kicontrol", 'SKILL_TWO = "kicontrol"' in yardrat)
    check("Yardrat no underscore ki ids", "ki_manipulation" not in yardrat and "ki_control" not in yardrat)
    check("RaceLock clearStuckSagaDifficulty", "clearStuckSagaDifficulty" in race_lock)
    check("RaceLock maybeAutoUnlockStuckDifficulty", "maybeAutoUnlockStuckDifficulty" in race_lock)
    check("RaceLock requestDifficultyReselect", "requestDifficultyReselect" in race_lock)
    check("ShadowDummy legacy CD key", "dmz_minigame_shadow_dummy_cooldown_until" in shadow)
    check("GlobalTpBoost END_LOCK_MS 5s", "END_LOCK_MS = 5000L" in boost)
    check("SCRIPT-AUDIT documents 2.3.54", "2.3.54" in script_audit)

    print("\n=== Conversation commitments (2.3.54) ===")
    conv = read(ROOT / "tools" / "dmz-adaptive-difficulty" / "CONVERSATION-CHECKLIST.md")
    check("CONVERSATION-CHECKLIST.md present", "Conversation commitments checklist" in conv)
    check("conv: mentor 1 hour locked", "Mentor invite **1 hour**" in conv)
    check("conv: spar 3 cards locked", "individual** report cards" in conv)
    check("conv: CNPC-free locked", "CNPC-free" in conv)
    check("conv: player prestige+android locked", "Player Prestige + Remove Android" in conv)
    check("conv: enddragon StaffAccess locked", "StaffAccess" in conv and "/enddragon" in conv)
    # Code still matches conversation — robust needles (avoid javadoc / call-site splits).
    check("conv code: no soft hay.contains(rival)",
          'hay.contains("rival")' not in cnpc and 'contains("rival")' not in cnpc.replace(
              'Soft substrings like {@code contains("rival")}', ''))
    check("conv code: inspect android_remove case",
          'case "android_remove", "androidremove"' in plugin
          and "progressionChestGui.open(admin, \"android_remove\")" in plugin)
    check("conv code: lmdo hub everywhere chest",
          'SlotAction.cmd("lm")' not in rival_chest
          and 'lmdo lm open hub' in rival_chest
          and 'lmdo lm open hub' in prestige_chest)
    check("conv code: chat hub android+prestige",
          "android_remove" in mech_chat and "prestige" in mech_chat)

    print("\n=== Prestige points shop (2.3.81) ===")
    pp = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigePointsSystem.java")
    mixin = read(SRC / "com/dbzlegacy/adaptivedifficulty/mixin/StatsDataMixin.java")
    mixins_json = read(ROOT / "tools" / "dmz-adaptive-difficulty" / "src" / "main" / "resources" / "legacymechanics.mixins.json")
    gui_api_pp = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/ProgressionGuiApi.java")
    hub_pp = read(gui_root / "HubChestGui.java")
    cmi_hub_pp = read(gui_root / "CmiHubGui.java")
    prestige_chest_pp = read(gui_root / "PrestigeChestGui.java")
    prestige_cmi_pp = read(gui_root / "CmiPrestigeGui.java")
    check("potentialunlock 2 levels per point",
          "POTENTIAL_UNLOCK_LEVELS_PER_POINT = 2" in pp
          and "levelsPerPoint" in pp)
    check("tier point cost ladder",
          "tierPointCost" in pp and "buyDifficultyTier" in pp)
    check("prestige wallet GUI",
          "putWallet" in prestige_chest_pp and "Difficulty Tiers" in prestige_chest_pp)
    check("turnIn triangular pack bonus", "packs * (packs + 1) / 2" in pp and "pointsForTurnIn" in pp)
    check("turnIn amounts 1/2/3/6/9", "TURN_IN_AMOUNTS = {1, 2, 3, 6, 9}" in pp)
    prestige_skill_sync = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/bridge/PrestigeSkillSync.java")
    prestige_faction_sync = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/bridge/PrestigeFactionSync.java")
    check("takePrestigeLevels API", "takePrestigeLevels" in prestige_skill_sync and "loseLevels" in prestige_skill_sync)
    check("addPrestigeLevels API", "addPrestigeLevels" in prestige_skill_sync and "giveLevels" in prestige_skill_sync)
    check("turnIn uses Fabled API take", "takePrestigeLevels" in pp)
    check("PrestigeFactionSync forceSync", "forceSync" in prestige_faction_sync)
    prestige_admin = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigeAdmin.java")
    prestige_sys = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigeSystem.java")
    check("PrestigeAdmin present", "class PrestigeAdmin" in prestige_admin)
    check("requiredLevel uses personal cap",
          "effectiveMaxLevel(player)" in prestige_sys
          and "requiredLevel(ServerPlayer" in prestige_sys)
    check("prestige need floor",
          "prestige_need_floor" in prestige_sys and "raiseNeedFloor" in prestige_sys)
    check("completed never from Fabled-only",
          "inferCompletedFromShop" in prestige_sys and "KEY_NEED_FLOOR" in prestige_sys)
    check("prestige admin command registered",
          '"admin"' in read(SRC / "com/dbzlegacy/adaptivedifficulty/command/ProgressionCommands.java")
          and "prestigeAdminAdjust" in
          read(SRC / "com/dbzlegacy/adaptivedifficulty/command/ProgressionCommands.java"))
    check("breakthrough mentions prestige Need scale",
          "Future prestige requirements now scale" in pp)
    check("handlePrestigeAdmin API", "handlePrestigeAdmin" in gui_api_pp)
    check("Bukkit prestige admin via ForgeBridge",
          "prestigeAdmin" in forge_bridge
          and "isPrestigeAdminSub" in
          read(gui_root / "AdaptiveDifficultyGuiPlugin.java"))
    check("form XOR majin/mutant", "buyMajin" in pp and "buyMutant" in pp and "unbuyMajin" in pp)
    # MutantManager.grant always chats message.dragonminez.mutant.gained — pulse must not call it.
    check("silent mutant ensure (no grant chat spam)",
          "ensureMutantSilent" in pp
          and "MutantManager.grant(" not in pp
          and "reconcileHolder" in pp)
    check("silent majin ensure (no dmzeffect give chat)",
          "ensureEffectSilent" in pp
          and '"dmzeffect give "' not in pp
          and "'dmzeffect give '" not in pp)
    lm_tips = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/LmTips.java")
    check("LM onboarding tips class",
          "class LmTips" in lm_tips
          and "Use command /lm to open the GUI" in lm_tips)
    check("LM tips use action bar",
          "ScreenNotify.actionBar" in lm_tips
          and "void actionBar" in read(SRC / "com/dbzlegacy/adaptivedifficulty/util/ScreenNotify.java"))
    check("LM tips LP frequent groups",
          "lmTipFrequentGroups" in lm_tips
          and "getPrimaryGroup" in lm_tips
          and "novice" in read(CFG))
    check("LM tips wired on login+pulse",
          "LmTips.onLogin" in read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/ProgressionSystem.java")
          and "LmTips.pulse" in
          read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/ProgressionSystem.java"))
    check("LM tips config flag", "enableLmTips" in read(CFG) and "lmTipFrequentGroups" in read(CFG))
    check("breakthrough costs 15..35", "breakthroughCost" in pp and "15" in pp and "35" in pp)
    check("MAX_BREAKTHROUGHS 5", "MAX_BREAKTHROUGHS = 5" in pp)
    check("StatsDataMixin personal cap", "getConfiguredMaxValue" in mixin and "effectiveMaxLevel" in mixin)
    check("StatsDataMixin remap false",
          'remap = false' in mixin and '@Mixin(value = StatsData.class, remap = false)' in mixin)
    check("StatsDataMixin clamps down to personal",
          "personal != serverMax.intValue()" in mixin or "personal != serverMax" in mixin)
    check("StatsDataMixin gated on prestige flag", "enablePrestigeSystem" in mixin)
    manifest = read(ROOT / "tools" / "dmz-adaptive-difficulty" / "src" / "main" / "resources" / "META-INF" / "MANIFEST.MF")
    check("MANIFEST MixinConfigs for Mohist",
          "MixinConfigs: legacymechanics.mixins.json" in manifest)
    check("mixins.json registers StatsDataMixin", '"StatsDataMixin"' in mixins_json)
    check("AD referenceMaxLevel 150k", "referenceMaxLevel = 150_000L" in
          read(SRC / "com/dbzlegacy/adaptivedifficulty/config/DifficultyConfig.java"))
    dmz_general = read(ROOT / "config" / "dragonminez" / "general-server.json")
    check("DMZ maxValue 150000 for client UI", '"maxValue": 150000' in dmz_general)
    events_pp = read(SRC / "com/dbzlegacy/adaptivedifficulty/event/DifficultyEvents.java")
    check("TP soft-lock at personal cap", "onTpGain" in events_pp and "effectiveMaxLevel" in events_pp)
    check("TP soft-lock silent (no chat spam)",
          "event.setTpGain(0)" in events_pp
          and "pp_cap_msg_next" not in events_pp)
    check("Stat soft-lock at personal*6", "onStatChange" in events_pp and "personal * 6" in events_pp)
    check("Stat soft-lock uses ScreenNotify",
          "ScreenNotify.hint" in events_pp and "pp_stat_cap_title" in events_pp)
    check("Chest lore maxValue 150000 soft-lock",
          "DMZ maxValue 150000" in prestige_chest_pp and "Server hardcap stays" not in prestige_chest_pp)
    check("CMI lore maxValue 150000 soft-lock",
          "DMZ maxValue 150000" in prestige_cmi_pp and "Server hardcap stays" not in prestige_cmi_pp)
    check("grant staff-gated",
          'if (!StaffAccess.isStaff(player))' in gui_api_pp.split('give_points')[1].split('balance')[0]
          if "give_points" in gui_api_pp else False)
    check("spend ungated for NPC",
          "spendForNpc" in gui_api_pp.split('"spend"')[1].split('"grant"')[0]
          if '"spend"' in gui_api_pp else False)
    check("configuredMaxLevel helper", "configuredMaxLevel" in
          read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/DmzSkillUtil.java"))
    check("skillMax prefers skills.json", "configuredMaxLevel(id)" in
          read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/SkillUnlockService.java"))
    check("prestige shop Skill Check skills only",
          "skillOffers()" in pp and "skillCheckSkillIds" in
          read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/DmzSkillUtil.java"))
    check("skillCheckSkillIds excludes ultimate",
          "skillCheckSkillIds" in read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/DmzSkillUtil.java")
          and '"ultimate"' not in "".join(
              read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/DmzSkillUtil.java")
              .split("SKILL_CHECK_NATURAL")[1]
              .split("allNonFormSkillIds")[0]
          ))
    check("resolveOffer rejects non-SkillCheck",
          "isSkillCheckSkill" in pp and "Only Skill Check skills" in pp)
    check("Chest skill shop tip Skill Check only",
          "Skill Check skills only" in prestige_chest_pp)
    check("CMI skill shop tip Skill Check only",
          "Skill Check skills only" in prestige_cmi_pp)
    check("kiboost fallback max 4", '"kiboost", "Ki Boost", "§3", 4' in
          read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/SkillUnlockService.java"))
    check("kicontrol fallback max 1", '"kicontrol", "Ki Control", "§3", 1' in
          read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/shop/SkillUnlockService.java"))
    check("Chest pages turnin/shop/forms/cap",
          'case "turnin", "points"' in prestige_chest_pp
          and 'p.startsWith("shop")' in prestige_chest_pp
          and 'case "forms", "form"' in prestige_chest_pp
          and 'case "cap", "breakthrough"' in prestige_chest_pp)
    check("CMI pages turnin/shop/forms/cap",
          '"turnin", "points"' in prestige_cmi_pp
          and 'p.startsWith("shop")' in prestige_cmi_pp
          and '"forms", "form"' in prestige_cmi_pp
          and '"cap", "breakthrough"' in prestige_cmi_pp)
    check("CMI uses lmdo prestige",
          'lmdo prestige confirm' in prestige_cmi_pp
          and 'lmdo prestige turnin' in prestige_cmi_pp
          and 'lmdo prestige breakthrough' in prestige_cmi_pp)
    check("Hub Prestige tip mentions points shop",
          "skill/forms shop" in hub_pp and "skill/forms shop" in cmi_hub_pp)
    check("Papi prestige_points bridge",
          "prestige_points" in forge_bridge and "prestige_level_cap" in forge_bridge)

    print("\n=== End Dragon AD summon (2.3.65) ===")
    end_str = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/end/EndDimensionStrength.java")
    diff_actions = read(SRC / "com/dbzlegacy/adaptivedifficulty/service/DifficultyActions.java")
    chest = read(gui_root / "DifficultyChestGui.java")
    cmi_diff = read(gui_root / "CmiDifficultyGui.java")
    check("cmdPlayerSummon present", "cmdPlayerSummon" in end_str)
    check("summoner NBT stamp", "end_dragon_summoner" in end_str)
    check("owner-only damage gate", "Only the summoner can damage" in end_str)
    check("AD scaled power", "adScaledPower" in end_str)
    check("despawnOwnedDragon", "despawnOwnedDragon" in end_str)
    check("summoner live AD retarget", "retargetPlayerDragonToSummoner" in end_str)
    check("summoner form apply", "applySummonerAdStats" in end_str)
    check("orphan despawn pulse", "maybeDespawnOrphanedPlayerDragon" in end_str)
    check("DifficultyActions summon_end_dragon", "ACT_SUMMON_END_DRAGON" in diff_actions)
    check("ForgeBridge allows summon_end_dragon", "summon_end_dragon" in forge_bridge)
    check("Chest GUI summon button", "summon_end_dragon" in chest and "Summon End Dragon" in chest)
    check("CMI GUI summon button", "summon_end_dragon" in cmi_diff and "Summon End Dragon" in cmi_diff)
    check("natural spawn default off", "enableEndNaturalDragonSpawn = false" in
          read(SRC / "com/dbzlegacy/adaptivedifficulty/config/DifficultyConfig.java"))
    check("reject unauthorized dragon join", "rejectUnauthorizedDragonJoin" in end_str)
    check("hasAliveSummonedDragon", "hasAliveSummonedDragon" in end_str)
    race_lock_cfg = read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/race/RaceLockConfig.java")
    check("RaceLockConfig file", "race-lock.json" in race_lock_cfg)
    check("RaceLock uses config", "RaceLockConfig.findByRaceId" in read(SRC / "com/dbzlegacy/adaptivedifficulty/progression/race/RaceLock.java"))
    check("dragon phase steer", "maybeSteerDragonPhase" in end_str)
    check("AD off during dragon", "hasAliveSummonedDragon" in
          read(ROOT / "tools" / "dmz-adaptive-difficulty" / "src" / "main" / "java" /
               "com/dbzlegacy/adaptivedifficulty/tick/NearbyMobScaler.java"))
    check("natural spawn forced off sanitize",
          'cfg.enableEndNaturalDragonSpawn = false' in
          read(SRC / "com/dbzlegacy/adaptivedifficulty/config/DifficultyConfig.java"))
    check("3 netherite cost constant", "PLAYER_SUMMON_NETHERITE_COST = 3" in end_str)

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
