#!/usr/bin/env python3
"""Fail-closed concept audit for LegacyMechanics 2.3.131.

Encodes the player's stated balance intent:
  1. Buy tiers 1–7 feel progressively harder (stock 21→200%).
  2. Even builds AND VIT/RES/STR/PWR dumps still feel the ladder.
  3. Skills matter — Ki Protection, Ki Infusion, Potential Unlock.
  4. God / high forms must not out-tank packs after DMZ DEF.
  5. Melee AD kits chase / hit (feature presence).
  6. GUI ABI + version handshake stay intact.
  7. Soft-cap + landing ladders stay monotonic; each mid/high buy matters
     (T4&lt;T5&lt;T6&lt;T7 landing; soft-cap T3→T7 rises — 2.3.131 live cal).

Writes:
  /opt/cursor/artifacts/ad-concept-audit.md
  sim/out/ad-concept-audit.md
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from simulate_build_matrix import (  # noqa: E402
    ARCHETYPES,
    INVEST,
    SKILL_LOADOUTS,
    simulate,
)
from simulate_race_forms import (  # noqa: E402
    MAX_FORM,
    TANK_HP_RATIO,
    TIER_PCT,
    apply_mastery,
    load_forms,
    load_stats,
)

OUT = Path("/opt/cursor/artifacts")
OUT.mkdir(parents=True, exist_ok=True)
REPO_OUT = Path(__file__).resolve().parent / "out"
REPO_OUT.mkdir(parents=True, exist_ok=True)

ROOT = Path(__file__).resolve().parents[1]
PROFILE = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/calc/PlayerCombatProfile.java"
EVO = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/evolution/EnemyEvolution.java"
AI = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/ai/AdaptiveAiSystem.java"
MOD = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.java"


def main() -> int:
    errors: list[str] = []
    ok: list[str] = []
    lines = [
        "# LegacyMechanics concept audit (2.3.131)",
        "",
        "Fail-closed checks against the player's stated balance concept.",
        "",
    ]

    def check(label: str, cond: bool, detail: str = "") -> None:
        if cond:
            ok.append(label)
            lines.append(f"- ✅ {label}" + (f" — {detail}" if detail else ""))
        else:
            msg = f"{label}: {detail or 'failed'}"
            errors.append(msg)
            lines.append(f"- ❌ {msg}")

    race = "saiyan"
    stats = load_stats(race)
    forms = load_forms(race)
    st = stats["warrior"]
    base_f = {k: 1.0 for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}

    def pts(arch: str, cls: str = "warrior") -> dict[str, float]:
        inv = INVEST[cls] if arch == "class_default" else ARCHETYPES[arch]
        base = stats[cls]["base"]
        return {k: base.get(k, 0) + inv.get(k, 0) for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}

    lines += ["## 1) Tier ladder (even build)", ""]
    check("stock percents 21→200%", list(TIER_PCT.values()) == [0.21, 0.42, 0.65, 0.90, 1.35, 1.60, 2.00])
    check("stock tankDamageHealthRatio 0.28", abs(TANK_HP_RATIO - 0.28) < 1e-9, f"got {TANK_HP_RATIO}")

    even = {t: simulate(pts("even"), st["scale"], base_f, "warrior", t, SKILL_LOADOUTS["none"]) for t in (1, 3, 5, 7)}
    check("even T1→T5 hitFrac rises ≥1.4×", even[5]["hitFrac"] > even[1]["hitFrac"] * 1.4,
          f"T1={even[1]['hitFrac']:.3f} T5={even[5]['hitFrac']:.3f}")
    check("even T5→T7 hitFrac rises ≥1.10×", even[7]["hitFrac"] > even[5]["hitFrac"] * 1.10,
          f"T5={even[5]['hitFrac']:.3f} T7={even[7]['hitFrac']:.3f}")
    check("even T5 unprotected ≥25% bag (KP recommended)", even[5]["hitFrac"] >= 0.25,
          f"hitFrac={even[5]['hitFrac']:.3f}")
    check("even T7 unprotected ≥40% bag", even[7]["hitFrac"] >= 0.40,
          f"hitFrac={even[7]['hitFrac']:.3f}")

    lines += ["", "## 2) Dump builds feel the ladder (not shrug)", ""]
    for arch in ("vit_dump", "res_dump", "str_dump", "pwr_dump"):
        by_t = {
            t: simulate(pts(arch), st["scale"], base_f, "warrior", t, SKILL_LOADOUTS["none"])
            for t in (1, 5, 7)
        }
        check(
            f"{arch} T5 ≥ 28% bag",
            by_t[5]["hitFrac"] >= 0.28,
            f"hitFrac={by_t[5]['hitFrac']:.3f}",
        )
        check(
            f"{arch} T5 ≥ 60% of even",
            by_t[5]["hitFrac"] >= even[5]["hitFrac"] * 0.60,
            f"{by_t[5]['hitFrac']:.3f} vs even {even[5]['hitFrac']:.3f}",
        )
        check(
            f"{arch} T7 > T1 ×1.8",
            by_t[7]["hitFrac"] > by_t[1]["hitFrac"] * 1.8,
            f"T1={by_t[1]['hitFrac']:.3f} T7={by_t[7]['hitFrac']:.3f}",
        )
        # Post-DEF (~65% mit) still bites at T5.
        check(
            f"{arch} T5 post-DEF ≥ 10% live",
            by_t[5]["hitFrac"] * 0.35 >= 0.10,
            f"postDef~={by_t[5]['hitFrac']*0.35:.3f}",
        )

    tank = simulate(pts("class_default", "tank"), stats["tank"]["scale"], base_f, "tank", 5, SKILL_LOADOUTS["none"])
    check("tank class T5 ≥ 28% bag", tank["hitFrac"] >= 0.28, f"hitFrac={tank['hitFrac']:.3f}")
    check(
        "tank class T5 ≥ 60% of even",
        tank["hitFrac"] >= even[5]["hitFrac"] * 0.60,
        f"tank={tank['hitFrac']:.3f} even={even[5]['hitFrac']:.3f}",
    )

    lines += ["", "## 3) Skills matter", ""]
    none = simulate(pts("even"), st["scale"], base_f, "warrior", 5, SKILL_LOADOUTS["none"])
    kp = simulate(pts("even"), st["scale"], base_f, "warrior", 5, SKILL_LOADOUTS["kp10"])
    check(
        "KP10 reduces landing dmg (pre hitFrac unchanged)",
        abs(kp["hitFrac"] - none["hitFrac"]) < 1e-6 and kp["hitFracAfterKp"] <= none["hitFrac"] * 0.91,
        f"none={none['hitFrac']:.3f} afterKp={kp['hitFracAfterKp']:.3f}",
    )
    inf_none = simulate(pts("pwr_dump"), stats["spiritualist"]["scale"], base_f, "spiritualist", 5, SKILL_LOADOUTS["none"])
    inf = simulate(pts("pwr_dump"), stats["spiritualist"]["scale"], base_f, "spiritualist", 5, SKILL_LOADOUTS["inf10"])
    check(
        "Ki Infusion sponges more pack HP",
        inf["mobHp"] > inf_none["mobHp"] * 1.15,
        f"none={inf_none['mobHp']:.0f} inf={inf['mobHp']:.0f}",
    )

    # Peak / god form for PU
    best = None
    best_boost = 0.0
    for f in forms:
        fmap = {
            "STR": min(MAX_FORM, apply_mastery(f["str"], f["maxMastery"], f["maxStats"], 1.0)),
            "SKP": min(MAX_FORM, apply_mastery(f["skp"], f["maxMastery"], f["maxStats"], 1.0)),
            "PWR": min(MAX_FORM, apply_mastery(f["pwr"], f["maxMastery"], f["maxStats"], 1.0)),
            "ENE": min(MAX_FORM, apply_mastery(f.get("ene", 1), f["maxMastery"], f["maxStats"], 1.0)),
            "VIT": min(MAX_FORM, apply_mastery(f["vit"], f["maxMastery"], f["maxStats"], 1.0)),
            "RES": min(MAX_FORM, apply_mastery(f["def"], f["maxMastery"], f["maxStats"], 1.0)),
        }
        boost = max(fmap.values())
        if boost >= best_boost:
            best_boost = boost
            best = (f"{f['group']}.{f['name']}", fmap)
    assert best is not None
    form_name, fmap = best
    god_none = simulate(pts("even"), st["scale"], fmap, "warrior", 5, SKILL_LOADOUTS["none"])
    god_pu = simulate(pts("even"), st["scale"], fmap, "warrior", 5, SKILL_LOADOUTS["pu30"])
    god_kp = simulate(pts("even"), st["scale"], fmap, "warrior", 5, SKILL_LOADOUTS["kp10"])
    check(
        "Potential Unlock sponges when transformed",
        god_pu["mobHp"] > god_none["mobHp"] * 1.08,
        f"{form_name} none={god_none['mobHp']:.0f} pu={god_pu['mobHp']:.0f}",
    )
    check(
        "KP still saves on god form",
        god_kp["hitFracAfterKp"] < god_none["hitFrac"] * 0.95,
        f"pre={god_none['hitFrac']:.3f} afterKp={god_kp['hitFracAfterKp']:.3f}",
    )

    lines += ["", "## 4) God forms do not out-tank (incl. DMZ DEF-cancel)", ""]
    check(
        f"god-form T5 post-DEF ≥12% ({form_name})",
        god_none["hitFrac"] * 0.35 >= 0.12,
        f"pre={god_none['hitFrac']:.3f} postDef~={god_none['hitFrac']*0.35:.3f} ×{god_none['formBoost']:.1f}",
    )
    # Named SSJG / SSJB if present — must NOT hard-cancel at T7.
    for needle, label in (("supersaiyangod", "SSJG"), ("supersaiyanblue", "SSJB")):
        hit = None
        hit7 = None
        for f in forms:
            key = f"{f['group']}.{f['name']}".lower()
            if needle in key:
                fmap = {
                    "STR": min(MAX_FORM, apply_mastery(f["str"], f["maxMastery"], f["maxStats"], 1.0)),
                    "SKP": min(MAX_FORM, apply_mastery(f["skp"], f["maxMastery"], f["maxStats"], 1.0)),
                    "PWR": min(MAX_FORM, apply_mastery(f["pwr"], f["maxMastery"], f["maxStats"], 1.0)),
                    "ENE": min(MAX_FORM, apply_mastery(f.get("ene", 1), f["maxMastery"], f["maxStats"], 1.0)),
                    "VIT": min(MAX_FORM, apply_mastery(f["vit"], f["maxMastery"], f["maxStats"], 1.0)),
                    "RES": min(MAX_FORM, apply_mastery(f["def"], f["maxMastery"], f["maxStats"], 1.0)),
                }
                hit = simulate(pts("even"), st["scale"], fmap, "warrior", 5, SKILL_LOADOUTS["none"])
                hit7 = simulate(pts("even"), st["scale"], fmap, "warrior", 7, SKILL_LOADOUTS["none"])
                break
        if hit is not None:
            check(
                f"{label} T5 post-DEF ≥12% live",
                hit["hitFrac"] * 0.35 >= 0.12,
                f"pre={hit['hitFrac']:.3f} postDef~={hit['hitFrac']*0.35:.3f}",
            )
        if hit7 is not None:
            # 1.0.25: post-pierce soft-cap can leave extreme DEF forms on the cancel
            # path — that's OK if landing still delivers the tier bite.
            clears = not hit7.get("wouldCancel", True)
            land_ok = hit7.get("landingFrac", 0) >= 0.35
            check(
                f"{label} T7 clears cancel or landing ≥35%",
                clears or land_ok,
                f"wouldCancel={hit7.get('wouldCancel')} dmg={hit7['mobDmg']:.0f} "
                f"flatMit={hit7.get('liveFlatMit', 0):.0f} "
                f"landingFrac={hit7.get('landingFrac', 0):.3f}",
            )
            check(
                f"{label} T7 landing safety-net ≥35% bag",
                land_ok,
                f"landingFrac={hit7.get('landingFrac', 0):.3f}",
            )

    lines += ["", "## 5) Melee AD parity (feature gates)", ""]
    evo = EVO.read_text(encoding="utf-8", errors="replace")
    ai = AI.read_text(encoding="utf-8", errors="replace")
    check("painted melee shock/slam", "paintedMeleeHit" in evo and "dmz_ad_melee_shock" in evo)
    check("Awakened+ chase speed", "AWAKENED" in evo and "m_21573_" in evo)
    check("Adaptive AI speed from Enhanced+", "ENHANCED" in ai)

    lines += ["", "## 6) Version / formula revision", ""]
    mod = MOD.read_text(encoding="utf-8", errors="replace")
    profile = PROFILE.read_text(encoding="utf-8", errors="replace")
    check("VERSION 2.3.131", 'VERSION = "2.3.131"' in mod)

    check("RaceSkillSync present", (ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/progression/bridge/RaceSkillSync.java").is_file())
    check("formula revision 39", "mix(h, 39L)" in profile)
    check("hpFloorStrength present", "hpFloorStrength" in profile)
    check("T1–T3 god-form floors raised", "case 1 -> 0.52" in profile and "case 3 -> 0.80" in profile)
    events = (ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/event/DifficultyEvents.java").read_text(
        encoding="utf-8", errors="replace"
    )
    check("T7 incoming soft-cap in events", "maxFrac" in events and "0.62" in events)
    check("T5 soft-cap 52%", "case 5 -> 0.52" in events)
    check("T6 soft-cap 58%", "case 6 -> 0.58" in events)
    check("T3 soft-cap ≤ T4", "case 3 -> 0.44" in events and "case 4 -> 0.50" in events)
    check("T4 soft-cap ≤ T5", "case 4 -> 0.50" in events and "case 5 -> 0.52" in events)
    check("T5 soft-cap ≤ T6", "case 5 -> 0.52" in events and "case 6 -> 0.58" in events)
    check("T6 soft-cap ≤ T7", "case 6 -> 0.58" in events and "case 7 -> 0.62" in events)
    # Source landFrac must stay strictly progressive (2.3.131 live cal — buys matter).
    check(
        "landFrac ladder progressive T4<T5<T6<T7",
        "case 4 -> 0.40" in profile
        and "case 5 -> 0.50" in profile
        and "case 6 -> 0.54" in profile
        and "default -> 0.58" in profile,
    )
    # Sim: god soft-cap-bound hitFrac must rise T3→T4→T5→T6→T7.
    god_t3 = simulate(pts("even"), st["scale"], fmap, "warrior", 3, SKILL_LOADOUTS["none"])
    god_t4 = simulate(pts("even"), st["scale"], fmap, "warrior", 4, SKILL_LOADOUTS["none"])
    god_t5 = simulate(pts("even"), st["scale"], fmap, "warrior", 5, SKILL_LOADOUTS["none"])
    god_t6 = simulate(pts("even"), st["scale"], fmap, "warrior", 6, SKILL_LOADOUTS["none"])
    god_t7 = simulate(pts("even"), st["scale"], fmap, "warrior", 7, SKILL_LOADOUTS["none"])
    check(
        "god soft-cap ladder T3≤T4",
        god_t3["hitFrac"] <= god_t4["hitFrac"] + 1e-9,
        f"T3={god_t3['hitFrac']:.3f} T4={god_t4['hitFrac']:.3f}",
    )
    check(
        "god soft-cap ladder T4≤T5",
        god_t4["hitFrac"] <= god_t5["hitFrac"] + 1e-9,
        f"T4={god_t4['hitFrac']:.3f} T5={god_t5['hitFrac']:.3f}",
    )
    check(
        "god soft-cap ladder T5≤T6",
        god_t5["hitFrac"] <= god_t6["hitFrac"] + 1e-9,
        f"T5={god_t5['hitFrac']:.3f} T6={god_t6['hitFrac']:.3f}",
    )
    check(
        "god soft-cap ladder T6≤T7",
        god_t6["hitFrac"] <= god_t7["hitFrac"] + 1e-9,
        f"T6={god_t6['hitFrac']:.3f} T7={god_t7['hitFrac']:.3f}",
    )
    check(
        "god landing T5 < T6 (buy matters)",
        god_t5.get("landingFrac", 0) + 0.01 < god_t6.get("landingFrac", 0),
        f"T5={god_t5.get('landingFrac', 0):.3f} T6={god_t6.get('landingFrac', 0):.3f}",
    )
    check(
        "god landing T4 < T5",
        god_t4.get("landingFrac", 0) + 0.01 < god_t5.get("landingFrac", 0),
        f"T4={god_t4.get('landingFrac', 0):.3f} T5={god_t5.get('landingFrac', 0):.3f}",
    )
    check(
        "god landing ≤ soft-cap T5",
        god_t5.get("landingFrac", 0) <= 0.52 + 0.02,
        f"landing={god_t5.get('landingFrac', 0):.3f}",
    )
    check(
        "god landing ≤ soft-cap T6",
        god_t6.get("landingFrac", 0) <= 0.58 + 0.02,
        f"landing={god_t6.get('landingFrac', 0):.3f}",
    )
    tel = (
        ROOT
        / "src/main/java/com/dbzlegacy/adaptivedifficulty/telemetry/BalanceTelemetry.java"
    ).read_text(encoding="utf-8", errors="replace")
    check("combat telemetry present", "shouldLog" in tel and "logIncomingHit" in tel)
    # Landing ladder (cancel path) must rise with tier for god-form tanks.
    land_t1 = simulate(pts("even"), st["scale"], fmap, "warrior", 1, SKILL_LOADOUTS["none"])
    land_t5 = god_t5
    land_t7 = god_t7
    check(
        "god-form landing T1≥10%",
        land_t1.get("landingFrac", 0) >= 0.10,
        f"landingFrac={land_t1.get('landingFrac', 0):.3f}",
    )
    check(
        "god-form landing T5≥28%",
        land_t5.get("landingFrac", 0) >= 0.28,
        f"landingFrac={land_t5.get('landingFrac', 0):.3f}",
    )
    check(
        "god-form landing T7≥40%",
        land_t7.get("landingFrac", 0) >= 0.40,
        f"landingFrac={land_t7.get('landingFrac', 0):.3f}",
    )
    check(
        "god-form landing T7>T1×2.5",
        land_t7.get("landingFrac", 0) > land_t1.get("landingFrac", 0) * 2.5,
        f"T1={land_t1.get('landingFrac', 0):.3f} T7={land_t7.get('landingFrac', 0):.3f}",
    )

    lines += ["", "## Sample numbers (saiyan warrior)", ""]
    lines += [
        "| Build | T1 | T5 | T5 post-DEF | T7 | vs even T5 |",
        "|-------|---:|---:|------------:|---:|-----------:|",
    ]
    for arch in ("even", "vit_dump", "res_dump", "str_dump", "pwr_dump"):
        by_t = {
            t: simulate(pts(arch), st["scale"], base_f, "warrior", t, SKILL_LOADOUTS["none"])
            for t in (1, 5, 7)
        }
        lines.append(
            f"| {arch} | {by_t[1]['hitFrac']:.3f} | {by_t[5]['hitFrac']:.3f} | "
            f"{by_t[5]['hitFrac']*0.35:.3f} | {by_t[7]['hitFrac']:.3f} | "
            f"{by_t[5]['hitFrac']/even[5]['hitFrac']:.2f}× |"
        )
    lines.append(
        f"| tank class | — | {tank['hitFrac']:.3f} | {tank['hitFrac']*0.35:.3f} | — | "
        f"{tank['hitFrac']/even[5]['hitFrac']:.2f}× |"
    )
    lines += [
        "",
        f"| Form | hitFrac | post-DEF | softHits |",
        f"|------|--------:|---------:|---------:|",
        f"| {form_name} | {god_none['hitFrac']:.3f} | {god_none['hitFrac']*0.35:.3f} | {god_none['softHits']:.2f} |",
        "",
        f"**Result:** {'PASS' if not errors else 'FAIL'} — {len(ok)} ok, {len(errors)} error(s).",
        "",
    ]

    report = "\n".join(lines)
    (OUT / "ad-concept-audit.md").write_text(report)
    (REPO_OUT / "ad-concept-audit.md").write_text(report)
    print(report)
    if errors:
        print("\nFAILURES:")
        for e in errors:
            print(" -", e)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
