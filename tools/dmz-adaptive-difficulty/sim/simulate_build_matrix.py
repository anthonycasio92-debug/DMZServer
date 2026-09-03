#!/usr/bin/env python3
"""Full AdaptiveDifficulty 1.0.25 build matrix — race × class × archetype × skills × tier.

Concept targets (Buy Tier feel):
  T1 Awakened  — warm-up pressure; AI/evo Awakened only
  T2 Enhanced  — noticeable; KP optional
  T3 Elite     — full counters; KP helpful
  T4 Advanced  — elite rarity; KP recommended (unprotected hurts)
  T5 Master    — mutation; infusion sponge matters
  T6 Legendary — boss kits; serious transform pressure
  T7 God       — zenith ceiling; KP + sponge load-bearing

Writes:
  /opt/cursor/artifacts/ad-build-matrix-report.md
  sim/out/ad-build-matrix-report.md
Fail-closed with --check.
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from scaling_constants import (  # noqa: E402
    FORM_NUDGE,
    GOD_THREAT,
    GLASS_HITS,
    HP_FLOOR_STRENGTH_MIN,
    LAND_CAP,
    LAND_FRAC,
    LIVE_SHARE,
    SOFT_CAP,
    SPONGE_HITS,
    COUNTER_PCT_DIVISOR,
    COUNTER_STRENGTH_MIN,
    TIER_DMZ_BAND_TOP,
    TIER_DMZ_GATE,
)
from simulate_race_forms import (  # noqa: E402
    CLASS_DMG_MULT,
    CLASS_HP_MULT,
    ENERGY_OFFENSE_FACTOR,
    INVEST,
    MAX_FORM,
    MOB_HP_SCALE,
    OVERLAY_CAP,
    RACES,
    STRONG_MULT,
    TANK_DEF_RATIO,
    TANK_HP_RATIO,
    TIER_PCT,
    TOP2_SECONDARY,
    TW_BASE,
    TW_EXP,
    apply_mastery,
    blend_form,
    blended_offense,
    channel_damage,
    channel_hp,
    hit_cap_health,
    ki_protection_hit_frac,
    load_forms,
    load_stats,
    mega_bulk_exp,
    mega_bulk_tw,
    mega_exp_cap,
    mega_t,
    mega_tw_scale,
    paint_ease,
)

OUT = Path("/opt/cursor/artifacts")
OUT.mkdir(parents=True, exist_ok=True)
REPO_OUT = Path(__file__).resolve().parent / "out"
REPO_OUT.mkdir(parents=True, exist_ok=True)

# Fighting classes only
CLASSES = [
    "warrior",
    "berserker",
    "martialartist",
    "spiritualist",
    "cleric",
    "paladin",
    "tank",
]

ARCHETYPES = {
    "class_default": None,  # use INVEST[class]
    "even": dict(STR=400, SKP=400, RES=400, VIT=400, PWR=400, ENE=400),
    "vit_dump": dict(STR=150, SKP=150, RES=200, VIT=1200, PWR=100, ENE=150),
    "res_dump": dict(STR=150, SKP=150, RES=1200, VIT=300, PWR=100, ENE=150),
    "str_dump": dict(STR=1200, SKP=200, RES=150, VIT=200, PWR=100, ENE=150),
    "pwr_dump": dict(STR=100, SKP=150, RES=150, VIT=250, PWR=1200, ENE=900),
}

SKILL_LOADOUTS = {
    "none": dict(kp=0, inf=0, inf_on=False, pu=0),
    "kp10": dict(kp=10, inf=0, inf_on=False, pu=0),
    "inf10": dict(kp=0, inf=10, inf_on=True, pu=0),
    "pu30": dict(kp=0, inf=0, inf_on=False, pu=30),
    "full": dict(kp=10, inf=10, inf_on=True, pu=30),
}

# DMZ combat.json
KP_MITIGATION_PER_LEVEL = 0.01
INFUSION_DMG_PER_LEVEL = 0.025


def _counter_strength(pct: float) -> float:
    return max(0.0, min(1.0, pct / COUNTER_PCT_DIVISOR))


def eased_floor(base: float, floor: float, ramp: float) -> float:
    if floor <= base + 1e-6 or ramp >= 0.999:
        return max(base, floor)
    return base + (floor - base) * ramp


def _blend_counter(bias: float, pct: float) -> float:
    s = _counter_strength(pct)
    return 1.0 + (max(1.0, bias) - 1.0) * s


def _top2(pts: dict[str, float]) -> list[str]:
    return [k for k, _ in sorted(pts.items(), key=lambda kv: kv[1], reverse=True)[:2]]


def _class_dmg_bias(cls: str) -> float:
    mult = CLASS_DMG_MULT
    c = (cls or "").lower()
    if "berserk" in c:
        return 1.0 + (mult - 1.0) * 1.10
    if "tank" in c or "paladin" in c:
        return 1.0 + (mult - 1.0) * 1.12
    if "warrior" in c:
        return mult
    if "martial" in c:
        return 1.0 + (mult - 1.0) * 1.00
    if any(x in c for x in ("spirit", "cleric", "mage", "kiuser", "energy")):
        return 1.0 + (mult - 1.0) * 0.90
    return mult


def _class_hp_bias(cls: str) -> float:
    mult = CLASS_HP_MULT
    c = (cls or "").lower()
    if any(x in c for x in ("spirit", "cleric", "mage", "kiuser", "energy")):
        return 1.0 + (mult - 1.0) * 0.90
    if any(x in c for x in ("martial", "berserk", "warrior")):
        return 1.0 + (mult - 1.0) * 0.55
    if "tank" in c or "paladin" in c:
        return 1.0 + (mult - 1.0) * 0.35
    return 1.0 + (mult - 1.0) * 0.40


def _dmg_stat_bias(stat: str) -> float:
    bump = STRONG_MULT - 1.0
    return {
        "STR": 1.0 + bump * 0.55,
        "SKP": 1.0 + bump * 0.55,
        "PWR": 1.0 + bump * 0.65,
        "ENE": 1.0 + bump * 0.65,
        "RES": 1.0 + bump * 0.85,
        "VIT": 1.0 + bump * 0.75,
    }.get(stat, 1.0)


def _hp_stat_bias(stat: str) -> float:
    bump = STRONG_MULT - 1.0
    return {
        "STR": 1.0 + bump * 0.70,
        "SKP": 1.0 + bump * 0.70,
        "PWR": 1.0 + bump * 0.85,
        "ENE": 1.0 + bump * 0.85,
        "RES": 1.0 + bump * 0.30,
        "VIT": 1.0 + bump * 0.30,
    }.get(stat, 1.0)


def _combine_top2(bias_fn, top: list[str]) -> float:
    if not top:
        return 1.0
    primary = max(1.0, bias_fn(top[0]))
    if len(top) < 2:
        return primary
    secondary = max(1.0, bias_fn(top[1]))
    return 1.0 + (primary - 1.0) + (secondary - 1.0) * TOP2_SECONDARY


def simulate(
    pts: dict[str, float],
    scales: dict[str, float],
    forms: dict[str, float],
    cls: str,
    tier: int,
    skills: dict,
    dmz_level: int = 5500,
):
    str_f = forms.get("STR", 1.0)
    skp_f = forms.get("SKP", 1.0)
    pwr_f = forms.get("PWR", 1.0)
    ene_f = forms.get("ENE", 1.0)
    vit_f = forms.get("VIT", 1.0)
    res_f = forms.get("RES", 1.0)
    live_m = channel_damage(pts["STR"], scales.get("STR", 1), str_f)
    live_s = channel_damage(pts["SKP"], scales.get("SKP", 1), skp_f)
    live_k = channel_damage(pts["PWR"], scales.get("PWR", 1), pwr_f)
    live_e = channel_damage(pts["ENE"], scales.get("ENE", 1), ene_f)
    live_def = channel_damage(pts["RES"], scales.get("RES", 1), res_f)
    live_hp = channel_hp(pts["VIT"], scales.get("VIT", 1), vit_f)

    # Ki Infusion outgoing (DMZ) — applied to player punch estimate only.
    inf_mul = 1.0 + skills["inf"] * INFUSION_DMG_PER_LEVEL
    if skills["inf_on"]:
        inf_mul *= 1.0  # level already counts; active flag boosts sponge below

    pct = TIER_PCT[tier]
    form_boost = min(MAX_FORM, max(str_f, skp_f, pwr_f, ene_f, vit_f, res_f, 1.0))
    base_m = live_m / str_f if str_f > 1.08 else live_m
    base_s = live_s / skp_f if skp_f > 1.08 else live_s
    base_k = live_k / pwr_f if pwr_f > 1.08 else live_k
    base_e = live_e / ene_f if ene_f > 1.08 else live_e
    base_hp = live_hp / vit_f if vit_f > 1.08 else live_hp
    base_def = live_def / res_f if res_f > 1.08 else live_def

    tier_damp = max(0.55, 1.0 - 0.40 * min(1.0, pct))
    tw_off = TW_BASE * tier_damp
    exp = TW_EXP
    if 2 <= tier <= 3 and 1.12 < form_boost < 6.0:
        tw_off = max(tw_off, min(1.0, TW_BASE + (0.14 if tier == 2 else 0.10)))
    if form_boost >= 6.0:
        t = mega_t(form_boost)
        exp = min(exp, mega_exp_cap(t))
        tw_off = min(tw_off, TW_BASE * mega_tw_scale(t))
    tw_bulk = min(0.85, max(TW_BASE, TW_BASE * 1.15) * (0.80 + 0.20 * tier_damp))
    bulk_exp = 0.85
    if form_boost >= 6.0:
        t = mega_t(form_boost)
        tw_bulk = min(tw_bulk, TW_BASE * mega_bulk_tw(t))
        bulk_exp = mega_bulk_exp(t)

    melee = blend_form(base_m, live_m, tw_off, exp)
    strike = blend_form(base_s, live_s, tw_off, exp)
    ki = blend_form(base_k, live_k, tw_off, exp)
    energy = blend_form(base_e, live_e, tw_off, exp)
    defense = blend_form(base_def, live_def, tw_bulk, bulk_exp)
    hp = blend_form(base_hp, live_hp, tw_bulk, bulk_exp)
    offense = blended_offense(melee, strike, ki, energy)
    live_off = blended_offense(live_m, live_s, live_k, live_e)

    offense_share = offense * pct
    dmg = offense_share
    cap_hp = hit_cap_health(hp, live_hp, form_boost)
    ease = paint_ease(tier, dmz_level)
    floor_strength = max(COUNTER_STRENGTH_MIN, min(1.0, _counter_strength(pct)))
    hp_floor_strength = max(HP_FLOOR_STRENGTH_MIN, floor_strength)
    tank_floor = max(defense * pct * TANK_DEF_RATIO * floor_strength,
                     cap_hp * pct * TANK_HP_RATIO * hp_floor_strength)
    dmg = eased_floor(dmg, tank_floor, ease)
    if 1 <= tier <= 3 and form_boost > 1.12:
        threat = GOD_THREAT[tier]
        soft = offense * threat
        if form_boost >= 6.0:
            t = mega_t(form_boost)
            soft = min(soft, offense_share * max(1.25, 1.55 - 0.08 * min(1.25, t)))
        dmg = eased_floor(dmg, soft, ease)
    if tier >= 4 and form_boost > 1.12:
        nudge = FORM_NUDGE[tier]
        dmg = max(dmg, offense_share * nudge)
    if form_boost > 1.12 and live_off > offense * 1.05:
        live_share = LIVE_SHARE[tier]
        if form_boost >= 6.0:
            mega_boost = 1.0 + (0.18 if tier >= 7 else 0.35) * min(1.0, mega_t(form_boost))
        else:
            mega_boost = 1.0
        live_floor = live_off * pct * live_share * mega_boost
        dmg = eased_floor(dmg, live_floor, ease)

    top = _top2(pts)
    dmg_ov = 1.0
    dmg_ov *= _blend_counter(_combine_top2(_dmg_stat_bias, top), pct)
    dmg_ov *= _blend_counter(_class_dmg_bias(cls), pct)
    dmg_ov = max(1.0, min(OVERLAY_CAP, dmg_ov))
    dmg = max(1.0, dmg * dmg_ov)

    cap_frac = ki_protection_hit_frac(tier, form_boost)
    hit_cap = cap_hp * cap_frac
    dmg = min(dmg, hit_cap)

    # DMZ hard-cancel pierce — T4+ always; T3 god-forms (1.0.25).
    base_def = channel_damage(pts["RES"], scales.get("RES", 1), 1.0)
    live_flat = base_def * max(res_f, 1.0)
    cancel_thr = 2.5
    would_cancel = live_flat >= dmg * cancel_thr
    allow_pierce = tier >= 4 or (tier >= 3 and form_boost >= 6.0)
    if would_cancel and allow_pierce and live_flat > 1.0:
        dmg = max(dmg, live_flat / cancel_thr * 1.08)
        would_cancel = live_flat >= dmg * cancel_thr
    # 1.0.25: clamp post-pierce to live incoming soft-cap.
    soft_cap_frac = SOFT_CAP[tier]
    dmg = min(dmg, max(20.0, live_hp) * soft_cap_frac)
    if ease < 0.999:
        if dmg > offense_share + 1e-6:
            dmg = offense_share + (dmg - offense_share) * ease
        elif dmz_level > (TIER_DMZ_GATE.get(tier + 1, TIER_DMZ_BAND_TOP.get(7, 150000)) if tier < 7 else TIER_DMZ_BAND_TOP.get(7, 150000)):
            dmg = max(1.0, dmg * ease)
    would_cancel = live_flat >= dmg * cancel_thr
    land_frac = LAND_FRAC[tier]
    if form_boost > 1.12:
        t = min(1.0, math.log(max(1.12, form_boost)) / math.log(80.0))
        bump = 0.06 if tier <= 2 else 0.12
        land_frac *= 1.0 + bump * t
    bag = max(cap_hp, live_hp * 0.90)
    landing = bag * land_frac
    if skills["kp"] > 0:
        landing *= max(0.65, 1.0 - skills["kp"] * 0.015)
    land_cap = LAND_CAP[tier]
    min_land = live_hp * max(0.05, pct * 0.08)
    landing = eased_floor(min_land, landing, ease)
    landing = min(landing, live_hp * land_cap)
    if ease < 0.999:
        landing *= ease

    # Post-KP landing (DMZ 1%/lvl)
    kp_mit = min(0.10, skills["kp"] * KP_MITIGATION_PER_LEVEL)
    dmg_after_kp = dmg * (1.0 - kp_mit)

    hp_ov = 1.0
    hp_ov *= _blend_counter(_combine_top2(_hp_stat_bias, top), pct)
    hp_ov *= _blend_counter(_class_hp_bias(cls), pct)
    hp_ov = min(1.20, max(1.0, min(OVERLAY_CAP, hp_ov)))

    vit_share = hp * pct
    base_hp_mob = vit_share
    if 1 <= tier <= 2 and form_boost > 1.12:
        base_hp_mob = max(base_hp_mob, hp * (0.20 if tier == 1 else 0.28))
    if offense > hp * 0.30:
        hits = SPONGE_HITS[tier]
        durability = offense * pct * hits
        offense_vit = offense / max(1.0, hp)
        vit_cap_mul = 3.2
        if offense_vit > 1.15:
            vit_cap_mul = min(8.0, 3.2 + (offense_vit - 1.15) * 1.05)
        base_hp_mob = max(base_hp_mob, min(durability, vit_share * vit_cap_mul))
    form_pad = 1.0
    if form_boost > 1.12:
        form_pad = 1.0 + 0.40 * min(1.0, math.log(form_boost) / math.log(80.0))
    hard = hp * max(pct, 0.20) * form_pad * 1.55
    if offense > hp * 1.15:
        glass_hits = GLASS_HITS[tier]
        glass_hard = offense * pct * glass_hits
        hard = max(hard, min(glass_hard, vit_share * 8.0))
    skill_hp = (
        1.0
        + min(0.22, skills["inf"] * 0.022)
        + (0.06 if skills["inf_on"] else 0.0)
        + (min(0.12, skills["pu"] / 30.0 * 0.12) if form_boost > 1.12 else 0.0)
    )
    mob_hp = min(base_hp_mob, hard) * skill_hp * MOB_HP_SCALE * hp_ov
    mob_hp = max(10.0, mob_hp)

    player_punch = max(live_m, live_s, live_k) * inf_mul
    soft_hits = mob_hp / max(1.0, offense * pct)
    live_hits = mob_hp / max(1.0, player_punch)

    return dict(
        tier=tier,
        pct=pct,
        formBoost=form_boost,
        top2=">".join(top),
        offense=offense,
        mobDmg=dmg,
        mobDmgAfterKp=dmg_after_kp,
        mobHp=mob_hp,
        hitFrac=dmg / max(1.0, live_hp),
        hitFracAfterKp=dmg_after_kp / max(1.0, live_hp),
        capFrac=cap_frac,
        softHits=soft_hits,
        liveHits=live_hits,
        kpSave=dmg - dmg_after_kp,
        liveHp=live_hp,
        livePunch=player_punch,
        liveFlatMit=live_flat,
        wouldCancel=would_cancel,
        landing=landing,
        landingFrac=landing / max(1.0, live_hp),
    )


def main() -> int:
    check_only = "--check" in sys.argv
    errors: list[str] = []
    ok: list[str] = []
    lines = [
        "# AdaptiveDifficulty build matrix (1.0.25)",
        "",
        "Race × class × archetype × skill loadout × tier.",
        "Skills: kiprotection / ki_infusion / potentialunlock (DMZ combat.json rates).",
        "",
    ]

    def check(label: str, cond: bool, detail: str = "") -> None:
        if cond:
            ok.append(label)
        else:
            errors.append(f"{label}: {detail or 'failed'}")

    races = sorted(p.name for p in RACES.iterdir() if p.is_dir() and (p / "stats.json").exists())
    rows = []

    # Representative matrix: all races × classes × key archetypes × skills @ T1/T4/T7 base + peak form
    for race in races:
        stats = load_stats(race)
        forms = load_forms(race)
        for cls in CLASSES:
            if cls not in stats:
                continue
            st = stats[cls]
            for arch_name, arch_pts in ARCHETYPES.items():
                inv = arch_pts if arch_pts is not None else INVEST.get(cls, INVEST["warrior"])
                pts = {k: st["base"].get(k, 0) + inv.get(k, 0) for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}
                # Base form + peak form
                form_sets = [("base", {k: 1.0 for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")})]
                if forms:
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
                    if best:
                        form_sets.append(best)
                for form_name, fmap in form_sets:
                    for sk_name, sk in SKILL_LOADOUTS.items():
                        # Sparse skill grid — base covers KP/infusion/full; forms cover none/full.
                        if form_name == "base" and sk_name not in ("none", "kp10", "inf10", "full"):
                            continue
                        if form_name != "base" and sk_name not in ("none", "full"):
                            continue
                        for tier in (1, 3, 5, 7):
                            r = simulate(pts, st["scale"], fmap, cls, tier, sk)
                            rows.append(
                                dict(
                                    race=race,
                                    cls=cls,
                                    arch=arch_name,
                                    form=form_name,
                                    skills=sk_name,
                                    **r,
                                )
                            )

    # ── Concept checks ────────────────────────────────────────────────────
    # 1) Tier ladder for even build base, no skills, human/saiyan/warrior
    for race in ("human", "saiyan", "namekian"):
        if race not in races:
            continue
        sample = [
            r
            for r in rows
            if r["race"] == race
            and r["cls"] == "warrior"
            and r["arch"] == "even"
            and r["form"] == "base"
            and r["skills"] == "none"
        ]
        by_t = {r["tier"]: r for r in sample}
        if not by_t:
            continue
        check(
            f"{race} even T1→T5 hitFrac rises",
            by_t[5]["hitFrac"] > by_t[1]["hitFrac"] * 1.4,
            f"T1={by_t[1]['hitFrac']:.3f} T5={by_t[5]['hitFrac']:.3f}",
        )
        check(
            f"{race} even T5→T7 hitFrac rises",
            by_t[7]["hitFrac"] > by_t[5]["hitFrac"] * 1.10,
            f"T5={by_t[5]['hitFrac']:.3f} T7={by_t[7]['hitFrac']:.3f}",
        )
        check(
            f"{race} even T4+ unprotected ≥25% bag",
            by_t[5]["hitFrac"] >= 0.25,
            f"T5 hitFrac={by_t[5]['hitFrac']:.3f}",
        )
        # God-form: peak form at T5 must still hurt after worst-case 65% DEF.
        god_rows = [
            r
            for r in rows
            if r["race"] == race
            and r["cls"] == "warrior"
            and r["arch"] == "even"
            and r["form"] != "base"
            and r["skills"] == "none"
            and r["tier"] == 5
        ]
        if god_rows:
            g = max(god_rows, key=lambda x: x["formBoost"])
            check(
                f"{race} god-form T5 post-DEF ≥12% live",
                g["hitFrac"] * 0.35 >= 0.12,
                f"pre={g['hitFrac']:.3f} postDef~={g['hitFrac']*0.35:.3f} form×{g['formBoost']:.0f}",
            )

    # 2) KP saves damage at T5+
    for race in races[:3]:
        none = next(
            (
                r
                for r in rows
                if r["race"] == race
                and r["cls"] == "warrior"
                and r["arch"] == "even"
                and r["form"] == "base"
                and r["skills"] == "none"
                and r["tier"] == 5
            ),
            None,
        )
        kp = next(
            (
                r
                for r in rows
                if r["race"] == race
                and r["cls"] == "warrior"
                and r["arch"] == "even"
                and r["form"] == "base"
                and r["skills"] == "kp10"
                and r["tier"] == 5
            ),
            None,
        )
        if none and kp:
            check(
                f"{race} KP10 reduces landing dmg at T5",
                kp["hitFracAfterKp"] <= none["hitFrac"] * 0.91 + 1e-6
                and abs(kp["hitFrac"] - none["hitFrac"]) < 1e-6,
                f"none={none['hitFrac']:.3f} kpPre={kp['hitFrac']:.3f} kpAfter={kp['hitFracAfterKp']:.3f}",
            )

    # 3) Infusion raises pack HP
    for race in races[:3]:
        none = next(
            (
                r
                for r in rows
                if r["race"] == race
                and r["cls"] == "spiritualist"
                and r["arch"] == "pwr_dump"
                and r["form"] == "base"
                and r["skills"] == "none"
                and r["tier"] == 5
            ),
            None,
        )
        inf = next(
            (
                r
                for r in rows
                if r["race"] == race
                and r["cls"] == "spiritualist"
                and r["arch"] == "pwr_dump"
                and r["form"] == "base"
                and r["skills"] == "inf10"
                and r["tier"] == 5
            ),
            None,
        )
        if none and inf:
            check(
                f"{race} infusion sponges more HP at T5",
                inf["mobHp"] > none["mobHp"] * 1.15,
                f"none={none['mobHp']:.0f} inf={inf['mobHp']:.0f}",
            )

    # 4) Dump builds feel ladder + VIT dumps track even builds
    even_t5 = next(
        (
            r
            for r in rows
            if r["race"] == "saiyan"
            and r["cls"] == "warrior"
            and r["arch"] == "even"
            and r["form"] == "base"
            and r["skills"] == "none"
            and r["tier"] == 5
        ),
        None,
    )
    for arch in ("vit_dump", "res_dump", "str_dump", "pwr_dump"):
        sample = [
            r
            for r in rows
            if r["race"] == "saiyan"
            and r["cls"] == "warrior"
            and r["arch"] == arch
            and r["form"] == "base"
            and r["skills"] == "none"
        ]
        by_t = {r["tier"]: r for r in sample}
        if len(by_t) >= 3:
            check(
                f"saiyan {arch} T7 > T1 pressure",
                by_t[7]["hitFrac"] > by_t[1]["hitFrac"] * 1.8,
                f"T1={by_t[1]['hitFrac']:.3f} T7={by_t[7]['hitFrac']:.3f}",
            )
            check(
                f"saiyan {arch} T5 ≥ 28% bag",
                by_t[5]["hitFrac"] >= 0.28,
                f"T5={by_t[5]['hitFrac']:.3f}",
            )
            if even_t5 is not None:
                check(
                    f"saiyan {arch} T5 ≥ 58% of even",
                    by_t[5]["hitFrac"] >= even_t5["hitFrac"] * 0.58,
                    f"{arch}={by_t[5]['hitFrac']:.3f} even={even_t5['hitFrac']:.3f}",
                )
    tank_t5 = next(
        (
            r
            for r in rows
            if r["race"] == "saiyan"
            and r["cls"] == "tank"
            and r["arch"] == "class_default"
            and r["form"] == "base"
            and r["skills"] == "none"
            and r["tier"] == 5
        ),
        None,
    )
    if tank_t5 is not None:
        check(
            "saiyan tank class T5 ≥ 28% bag",
            tank_t5["hitFrac"] >= 0.28,
            f"T5={tank_t5['hitFrac']:.3f}",
        )

    # 4b) Potential Unlock sponge when transformed
    for race in races[:3]:
        none = next(
            (
                r
                for r in rows
                if r["race"] == race
                and r["cls"] == "warrior"
                and r["arch"] == "even"
                and r["form"] != "base"
                and r["skills"] == "none"
                and r["tier"] == 5
            ),
            None,
        )
        # pu30 only on base in sparse grid — use full vs none on transformed for PU+infusion
        full = next(
            (
                r
                for r in rows
                if r["race"] == race
                and r["cls"] == "warrior"
                and r["arch"] == "even"
                and r["form"] != "base"
                and r["skills"] == "full"
                and r["tier"] == 5
            ),
            None,
        )
        if none and full:
            check(
                f"{race} transformed full loadout sponges vs none",
                full["mobHp"] > none["mobHp"] * 1.20,
                f"none={none['mobHp']:.0f} full={full['mobHp']:.0f}",
            )

    # 5) Soft hits — transformed packs shouldn't vaporize instantly for typical warrior
    soft_ok = 0
    soft_n = 0
    for r in rows:
        if r["form"] != "base" and r["skills"] == "none" and r["tier"] == 5 and r["cls"] == "warrior" and r["arch"] == "class_default":
            soft_n += 1
            if r["softHits"] >= 0.35:
                soft_ok += 1
    check(
        "T5 transformed warrior softHits ≥0.35 for most races",
        soft_n > 0 and soft_ok / soft_n >= 0.70,
        f"{soft_ok}/{soft_n}",
    )

    # 6) Class diversity — tank takes harder hits than spiritualist (base even-ish)
    # Report summary tables
    lines += ["## Concept checklist", ""]
    for e in errors:
        lines.append(f"- ❌ {e}")
    for label in ok:
        lines.append(f"- ✅ {label}")
    if not errors and not ok:
        lines.append("- (no checks ran)")

    lines += ["", "## Sample: saiyan warrior even (base, no skills)", ""]
    lines += ["| Tier | HitFrac | After KP0 | SoftHits | MobDmg | MobHp |", "|-----:|--------:|----------:|---------:|-------:|------:|"]
    for r in sorted(
        (
            x
            for x in rows
            if x["race"] == "saiyan"
            and x["cls"] == "warrior"
            and x["arch"] == "even"
            and x["form"] == "base"
            and x["skills"] == "none"
        ),
        key=lambda x: x["tier"],
    ):
        lines.append(
            f"| T{r['tier']} | {r['hitFrac']:.3f} | {r['hitFracAfterKp']:.3f} | "
            f"{r['softHits']:.2f} | {r['mobDmg']:.0f} | {r['mobHp']:.0f} |"
        )

    lines += ["", "## Sample: skills at T5 saiyan warrior even (base)", ""]
    lines += ["| Skills | HitFrac | AfterKP | MobHp | KP save |", "|--------|--------:|--------:|------:|--------:|"]
    for sk in ("none", "kp10", "inf10", "full"):
        r = next(
            (
                x
                for x in rows
                if x["race"] == "saiyan"
                and x["cls"] == "warrior"
                and x["arch"] == "even"
                and x["form"] == "base"
                and x["skills"] == sk
                and x["tier"] == 5
            ),
            None,
        )
        if r:
            lines.append(
                f"| {sk} | {r['hitFrac']:.3f} | {r['hitFracAfterKp']:.3f} | "
                f"{r['mobHp']:.0f} | {r['kpSave']:.0f} |"
            )

    lines += ["", f"Rows: {len(rows)}. Races: {', '.join(races)}.", ""]
    report = "\n".join(lines)
    (OUT / "ad-build-matrix-report.md").write_text(report)
    (REPO_OUT / "ad-build-matrix-report.md").write_text(report)

    print(report)
    print(f"\n{'FAIL' if errors else 'PASS'} — {len(ok)} ok, {len(errors)} error(s)")
    if check_only and errors:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
