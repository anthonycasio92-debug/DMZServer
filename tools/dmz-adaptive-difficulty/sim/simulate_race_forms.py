#!/usr/bin/env python3
"""Simulate AdaptiveDifficulty against all DMZ race/class/forms on this pack.

Reads:  config/dragonminez/races/*/stats.json + forms/*.json
Writes: /opt/cursor/artifacts/ad-race-form-simulation.csv
        /opt/cursor/artifacts/ad-race-form-balance-report.md

Mirrors PlayerCombatProfile 1.0.15 formulas:
STR/SKP/PWR (+ mild ENE) offense, VIT HP, class + top-2 counters,
VIT/RES floors, live-bag hit-cap, live-offense transform pressure, skill sponge.
"""
from __future__ import annotations

import csv
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RACES = ROOT / "config" / "dragonminez" / "races"
OUT = Path("/opt/cursor/artifacts")
OUT.mkdir(parents=True, exist_ok=True)
REPO_OUT = Path(__file__).resolve().parent / "out"
REPO_OUT.mkdir(parents=True, exist_ok=True)

TIER_PCT = {1: 0.21, 2: 0.42, 3: 0.65, 4: 0.90, 5: 1.35, 6: 1.60, 7: 2.00}
TW_BASE = 0.65
TW_EXP = 0.75
MOB_HP_SCALE = 1.15
TANK_DEF_RATIO = 0.45
TANK_HP_RATIO = 0.28
MEGA_START, MEGA_TARGET = 6.0, 80.0
MAX_FORM = 100.0
RELEASE = 1.0
STRONG_MULT = 1.08
CLASS_DMG_MULT = 1.06
CLASS_HP_MULT = 1.05
OVERLAY_CAP = 1.25
TOP2_SECONDARY = 0.60

INVEST = {
    "warrior": dict(STR=800, SKP=200, RES=300, VIT=400, PWR=100, ENE=200),
    "berserker": dict(STR=900, SKP=150, RES=200, VIT=500, PWR=50, ENE=150),
    "martialartist": dict(STR=300, SKP=900, RES=250, VIT=450, PWR=150, ENE=250),
    "spiritualist": dict(STR=150, SKP=200, RES=250, VIT=350, PWR=900, ENE=800),
    "cleric": dict(STR=100, SKP=150, RES=350, VIT=500, PWR=800, ENE=900),
    "paladin": dict(STR=350, SKP=350, RES=700, VIT=500, PWR=200, ENE=300),
    "tank": dict(STR=200, SKP=200, RES=800, VIT=700, PWR=100, ENE=200),
    # Archetypes for challenge-feel checks (even / dumps).
    "even": dict(STR=400, SKP=400, RES=400, VIT=400, PWR=400, ENE=400),
    "vit_dump": dict(STR=150, SKP=150, RES=200, VIT=1200, PWR=100, ENE=150),
    "res_dump": dict(STR=150, SKP=150, RES=1200, VIT=300, PWR=100, ENE=150),
    "str_dump": dict(STR=1200, SKP=200, RES=150, VIT=200, PWR=100, ENE=150),
}


def mega_t(fb: float) -> float:
    if fb < MEGA_START:
        return 0.0
    return math.log(fb / MEGA_START) / math.log(MEGA_TARGET / MEGA_START)


def mega_exp_cap(t: float) -> float:
    t = max(0.0, t)
    if t <= 1.0:
        return 0.58 - 0.16 * t
    return max(0.36, 0.42 - 0.08 * (t - 1.0))


def mega_tw_scale(t: float) -> float:
    t = max(0.0, min(1.25, t))
    return max(0.72, 1.0 - 0.18 * min(1.0, t))


def mega_bulk_tw(t: float) -> float:
    t = max(0.0, min(1.25, t))
    return max(0.72, 1.15 - 0.35 * min(1.0, t))


def mega_bulk_exp(t: float) -> float:
    t = max(0.0, min(1.25, t))
    return max(0.50, 0.85 - 0.30 * min(1.0, t))


def blend_form(base: float, live: float, weight: float, exp: float) -> float:
    b = max(1e-9, base)
    l = max(b, live)
    w = max(0.0, min(1.0, weight))
    if w <= 0:
        return b
    e = max(0.20, min(1.0, exp))
    surplus = max(0.0, l / b - 1.0)
    seen = 1.0 + (surplus**e) * w
    cap = 1.0 + 10.0 * w
    return b * min(seen, cap)


ENERGY_OFFENSE_FACTOR = 0.08


def blended_offense(melee: float, strike: float, ki: float = 1.0, energy: float = 1.0) -> float:
    m, s, k = max(1.0, melee), max(1.0, strike), max(1.0, ki)
    e = max(1.0, energy * ENERGY_OFFENSE_FACTOR)
    peak = max(m, s, k, e)
    avg = (m + s + k + e) * 0.25
    return peak * 0.55 + avg * 0.45


def apply_mastery(base_mult: float, max_mastery: float, max_stats_mult: float, mastery_pct: float) -> float:
    if base_mult <= 1.0 or max_mastery <= 0:
        return max(1.0, base_mult)
    frac = max(0.0, min(1.0, mastery_pct))
    return base_mult * (1.0 + frac * (max(1.0, max_stats_mult) - 1.0))


def load_forms(race: str):
    forms = []
    fdir = RACES / race / "forms"
    if not fdir.is_dir():
        return forms
    for p in sorted(fdir.glob("*.json")):
        d = json.loads(p.read_text())
        group = d.get("groupName") or p.stem
        raw = d.get("forms") or {}
        if not isinstance(raw, dict):
            continue
        for name, f in raw.items():
            if not isinstance(f, dict):
                continue
            forms.append(
                {
                    "group": group,
                    "name": f.get("name") or name,
                    "str": float(f.get("strMultiplier") or 1),
                    "skp": float(f.get("skpMultiplier") or 1),
                    "def": float(f.get("defMultiplier") or 1),
                    "vit": float(f.get("vitMultiplier") or 1),
                    "pwr": float(f.get("pwrMultiplier") or 1),
                    "ene": float(f.get("eneMultiplier") or 1),
                    "maxMastery": float(f.get("maxMastery") or 0),
                    "maxStats": float(f.get("maxStatsMultiplier") or 1),
                }
            )
    return forms


def load_stats(race: str):
    d = json.loads((RACES / race / "stats.json").read_text())
    out = {}
    for cls, c in (d.get("classes") or {}).items():
        base = c.get("baseStats") or {}
        sc = c.get("statScaling") or {}
        out[cls] = {
            "base": {k: float(base.get(k, 0) or 0) for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")},
            "scale": {
                "STR": float(sc.get("STR_scaling") or 1),
                "SKP": float(sc.get("SKP_scaling") or 1),
                "RES": float(sc.get("DEF_scaling") or sc.get("RES_scaling") or 1),
                "VIT": float(sc.get("VIT_scaling") or 1),
                "PWR": float(sc.get("PWR_scaling") or 1),
                "ENE": float(sc.get("ENE_scaling") or 1),
            },
        }
    return out


def channel_damage(stat_points: float, scaling: float, form_mult: float) -> float:
    return max(1.0, stat_points * scaling * form_mult * RELEASE)


def channel_hp(vit_points: float, vit_scaling: float, vit_form: float) -> float:
    return max(20.0, 20.0 + vit_points * vit_scaling * vit_form)


def hit_cap_health(soft_hp: float, live_hp: float, form_boost: float) -> float:
    soft = max(20.0, soft_hp)
    live = max(soft, live_hp)
    if form_boost <= 1.12:
        blend = 0.20
    else:
        t = min(1.0, math.log(max(1.12, form_boost)) / math.log(80.0))
        blend = 0.30 + 0.45 * t
    return soft + (live - soft) * blend


def ki_protection_hit_frac(tier: int, form_boost: float, kp_level: int = 0) -> float:
    # 1.0.19 — raise T1–T6 bite; soft-cap T7 (live telemetry one-shots).
    del kp_level
    tier_frac = {1: 0.26, 2: 0.34, 3: 0.42, 4: 0.52, 5: 0.58, 6: 0.64, 7: 0.64}[tier]
    if form_boost <= 1.12:
        form_factor = 0.78
    else:
        t = min(1.0, math.log(max(1.12, form_boost)) / math.log(80.0))
        form_factor = 0.78 + 0.22 * t
        if tier >= 7 and form_boost >= 25.0:
            form_factor = min(form_factor, 0.88)
    return max(0.12, min(0.75, tier_frac * form_factor))


def _counter_strength(pct: float) -> float:
    return max(0.0, min(1.0, pct / 0.50))


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


def simulate_ad(
    live_melee,
    live_strike,
    live_ki,
    live_energy,
    live_hp,
    str_form,
    skp_form,
    pwr_form,
    ene_form,
    vit_form,
    res_form,
    tier: int,
    invested: dict[str, float] | None = None,
    fighting_class: str = "warrior",
    live_def: float | None = None,
):
    pct = TIER_PCT[tier]
    form_boost = min(MAX_FORM, max(str_form, skp_form, pwr_form, ene_form, vit_form, res_form, 1.0))

    base_melee = live_melee / str_form if str_form > 1.08 else live_melee
    base_strike = live_strike / skp_form if skp_form > 1.08 else live_strike
    base_ki = live_ki / pwr_form if pwr_form > 1.08 else live_ki
    base_energy = live_energy / ene_form if ene_form > 1.08 else live_energy
    base_hp = live_hp / vit_form if vit_form > 1.08 else live_hp
    if live_def is None:
        live_def = 1.0
    base_def = live_def / res_form if res_form > 1.08 else live_def

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

    melee = blend_form(base_melee, live_melee, tw_off, exp)
    strike = blend_form(base_strike, live_strike, tw_off, exp)
    ki = blend_form(base_ki, live_ki, tw_off, exp)
    energy = blend_form(base_energy, live_energy, tw_off, exp)
    defense = blend_form(base_def, live_def, tw_bulk, bulk_exp)
    hp = blend_form(base_hp, live_hp, tw_bulk, bulk_exp)
    offense = blended_offense(melee, strike, ki, energy)
    live_off = blended_offense(live_melee, live_strike, live_ki, live_energy)

    offense_share = offense * pct
    dmg = offense_share
    cap_hp = hit_cap_health(hp, live_hp, form_boost)
    # Live VIT/RES floors — tank dumps / god forms must feel the ladder.
    floor_strength = max(0.35, min(1.0, _counter_strength(pct)))
    hp_floor_strength = max(0.80, floor_strength)
    dmg = max(dmg, defense * pct * TANK_DEF_RATIO * floor_strength)
    dmg = max(dmg, cap_hp * pct * TANK_HP_RATIO * hp_floor_strength)
    if 1 <= tier <= 3 and form_boost > 1.12:
        threat = {1: 0.48, 2: 0.64, 3: 0.78}[tier]
        soft = offense * threat
        if form_boost >= 6.0:
            t = mega_t(form_boost)
            soft = min(soft, offense_share * max(1.25, 1.55 - 0.08 * min(1.25, t)))
        dmg = max(dmg, soft)
    if tier >= 4 and form_boost > 1.12:
        nudge = {4: 1.35, 5: 1.50, 6: 1.65, 7: 1.42}[tier]
        dmg = max(dmg, offense_share * nudge)
    if form_boost > 1.12 and live_off > offense * 1.05:
        live_share = {1: 0.28, 2: 0.40, 3: 0.50, 4: 0.55, 5: 0.62, 6: 0.68, 7: 0.48}[tier]
        if form_boost >= 6.0:
            mega_boost = 1.0 + (0.18 if tier >= 7 else 0.35) * min(1.0, mega_t(form_boost))
        else:
            mega_boost = 1.0
        dmg = max(dmg, live_off * pct * live_share * mega_boost)

    # Class + top-2 counter overlays.
    pts = invested or {"STR": 1, "SKP": 1, "RES": 1, "VIT": 1, "PWR": 1, "ENE": 1}
    top = _top2(pts)
    dmg_ov = 1.0
    dmg_ov *= _blend_counter(_combine_top2(_dmg_stat_bias, top), pct)
    dmg_ov *= _blend_counter(_class_dmg_bias(fighting_class), pct)
    dmg_ov = max(1.0, min(OVERLAY_CAP, dmg_ov))
    dmg = max(1.0, dmg * dmg_ov)

    hit_cap = cap_hp * ki_protection_hit_frac(tier, form_boost)
    dmg = min(dmg, hit_cap)

    # DMZ DEF-cancel pierce — T4+ always; T3 god-forms (1.0.20).
    live_flat = base_def * max(res_form, 1.0)
    cancel_thr = 2.5
    allow_pierce = tier >= 4 or (tier >= 3 and form_boost >= 6.0)
    if live_flat > 1.0 and dmg * cancel_thr <= live_flat and allow_pierce:
        dmg = max(dmg, live_flat / cancel_thr * 1.08)

    hp_ov = 1.0
    hp_ov *= _blend_counter(_combine_top2(_hp_stat_bias, top), pct)
    hp_ov *= _blend_counter(_class_hp_bias(fighting_class), pct)
    hp_ov = min(1.20, max(1.0, min(OVERLAY_CAP, hp_ov)))

    vit_share = hp * pct
    base_hp_mob = vit_share
    if 1 <= tier <= 2 and form_boost > 1.12:
        base_hp_mob = max(base_hp_mob, hp * (0.20 if tier == 1 else 0.28))
    if offense > hp * 0.30:
        hits = {1: 1.00, 2: 0.90, 3: 0.78, 4: 0.68, 5: 0.58, 6: 0.52, 7: 0.48}[tier]
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
        glass_hits = {1: 0.82, 2: 0.72, 3: 0.62, 4: 0.54, 5: 0.48, 6: 0.44, 7: 0.40}[tier]
        glass_hard = offense * pct * glass_hits
        hard = max(hard, min(glass_hard, vit_share * 8.0))
    mob_hp = min(base_hp_mob, hard) * MOB_HP_SCALE * hp_ov
    mob_hp = max(10.0, mob_hp)

    player_punch = max(live_melee, live_strike, live_ki)
    return dict(
        formBoost=round(form_boost, 2),
        softOffense=round(offense, 1),
        liveOffense=round(live_off, 1),
        inherit=round(offense / max(1.0, live_off), 3),
        top2=">".join(top),
        dmgOverlay=round(dmg_ov, 3),
        mobDmg=round(dmg, 1),
        mobHp=round(mob_hp, 1),
        hitsToKill=round(mob_hp / max(1.0, player_punch), 2),
        hitFracPlayer=round(dmg / max(1.0, live_hp), 3),
        hitCapFrac=round(ki_protection_hit_frac(tier, form_boost), 3),
        hitCapHp=round(cap_hp, 1),
        softHp=round(hp, 1),
        liveHp=round(live_hp, 1),
    )


def main() -> None:
    import sys

    check_only = "--check" in sys.argv
    rows = []
    # Auto-discovers any new race folders under config/dragonminez/races/
    races = sorted(p.name for p in RACES.iterdir() if p.is_dir() and (p / "stats.json").exists())
    for race in races:
        stats = load_stats(race)
        forms = load_forms(race)
        form_list = [
            {
                "group": "base",
                "name": "Base",
                "str": 1,
                "skp": 1,
                "def": 1,
                "vit": 1,
                "pwr": 1,
                "ene": 1,
                "maxMastery": 0,
                "maxStats": 1,
            }
        ] + forms
        for cls, st in stats.items():
            inv = INVEST.get(cls) or INVEST["warrior"]
            pts = {k: st["base"].get(k, 0) + inv.get(k, 0) for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}
            for f in form_list:
                for mastery_pct, mlabel in ((0.0, "m0"), (1.0, "m100")):
                    if f["name"] == "Base" and mastery_pct > 0:
                        continue
                    str_f = apply_mastery(f["str"], f["maxMastery"], f["maxStats"], mastery_pct) if f["name"] != "Base" else 1.0
                    skp_f = apply_mastery(f["skp"], f["maxMastery"], f["maxStats"], mastery_pct) if f["name"] != "Base" else 1.0
                    pwr_f = apply_mastery(f["pwr"], f["maxMastery"], f["maxStats"], mastery_pct) if f["name"] != "Base" else 1.0
                    ene_f = apply_mastery(f.get("ene", 1), f["maxMastery"], f["maxStats"], mastery_pct) if f["name"] != "Base" else 1.0
                    vit_f = apply_mastery(f["vit"], f["maxMastery"], f["maxStats"], mastery_pct) if f["name"] != "Base" else 1.0
                    res_f = apply_mastery(f["def"], f["maxMastery"], f["maxStats"], mastery_pct) if f["name"] != "Base" else 1.0
                    str_f = min(MAX_FORM, max(1.0, str_f))
                    skp_f = min(MAX_FORM, max(1.0, skp_f))
                    pwr_f = min(MAX_FORM, max(1.0, pwr_f))
                    ene_f = min(MAX_FORM, max(1.0, ene_f))
                    vit_f = min(MAX_FORM, max(1.0, vit_f))
                    res_f = min(MAX_FORM, max(1.0, res_f))
                    live_melee = channel_damage(pts["STR"], st["scale"]["STR"], str_f)
                    live_strike = channel_damage(pts["SKP"], st["scale"]["SKP"], skp_f)
                    live_ki = channel_damage(pts["PWR"], st["scale"].get("PWR", 1), pwr_f)
                    live_energy = channel_damage(pts["ENE"], st["scale"].get("ENE", 1), ene_f)
                    live_def = channel_damage(pts["RES"], st["scale"].get("RES", 1), res_f)
                    live_hp = channel_hp(pts["VIT"], st["scale"]["VIT"], vit_f)
                    for tier in (1, 3, 5, 7):
                        ad = simulate_ad(
                            live_melee, live_strike, live_ki, live_energy, live_hp,
                            str_f, skp_f, pwr_f, ene_f, vit_f, res_f, tier,
                            invested=pts,
                            fighting_class=cls,
                            live_def=live_def,
                        )
                        rows.append(
                            {
                                "race": race,
                                "class": cls,
                                "formGroup": f["group"],
                                "form": f["name"],
                                "mastery": mlabel,
                                "tier": tier,
                                "strForm": round(str_f, 2),
                                "skpForm": round(skp_f, 2),
                                "pwrForm": round(pwr_f, 2),
                                "eneForm": round(ene_f, 2),
                                "vitForm": round(vit_f, 2),
                                "resForm": round(res_f, 2),
                                "liveMelee": round(live_melee, 1),
                                "liveStrike": round(live_strike, 1),
                                "liveKi": round(live_ki, 1),
                                **ad,
                            }
                        )

    csv_path = OUT / "ad-race-form-simulation.csv"
    with csv_path.open("w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)
    # Mirror into repo sim/out for source control.
    repo_csv = REPO_OUT / "ad-race-form-simulation.csv"
    with repo_csv.open("w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)

    summary = []
    for race in races:
        sub = [
            r
            for r in rows
            if r["race"] == race and r["tier"] == 5 and r["mastery"] == "m100" and r["class"] == "warrior" and r["form"] != "Base"
        ]
        base = [r for r in rows if r["race"] == race and r["tier"] == 5 and r["class"] == "warrior" and r["form"] == "Base"]
        if not sub:
            sub = [r for r in rows if r["race"] == race and r["tier"] == 5 and r["mastery"] == "m100" and r["class"] == "berserker" and r["form"] != "Base"]
            base = [r for r in rows if r["race"] == race and r["tier"] == 5 and r["class"] == "berserker" and r["form"] == "Base"]
        if not sub:
            continue
        top = max(sub, key=lambda r: r["formBoost"])
        b = base[0] if base else None
        summary.append(
            {
                "race": race,
                "forms": len({(r["formGroup"], r["form"]) for r in rows if r["race"] == race and r["form"] != "Base"}),
                "topForm": f"{top['formGroup']}.{top['form']}",
                "topFormBoost": top["formBoost"],
                "topInherit": top["inherit"],
                "topMobDmg": top["mobDmg"],
                "topMobHp": top["mobHp"],
                "topHitsToKill": top["hitsToKill"],
                "topHitFrac": top["hitFracPlayer"],
                "hitCapFrac": top["hitCapFrac"],
                "dmgJump": round(top["mobDmg"] / max(1.0, b["mobDmg"]), 2) if b else None,
                "hpJump": round(top["mobHp"] / max(1.0, b["mobHp"]), 2) if b else None,
            }
        )

    md = [
        "# AdaptiveDifficulty race/form simulation (1.0.15)",
        "",
        "Source: `config/dragonminez/races/*`.",
        "Model: soft STR/SKP/PWR (+ mild ENE) × tier% + live-bag hit-cap + god-form live-offense pressure.",
        f"Rows: {len(rows)}.",
        "",
        "## Per-race peak (T5, mastery 100%, physical class)",
        "",
        "| Race | Forms | Top boost | Inherit | Dmg jump | HP jump | Hits | Hit/playerHP | Cap | Top form |",
        "|------|------:|----------:|--------:|---------:|--------:|-----:|-------------:|----:|----------|",
    ]
    for s in sorted(summary, key=lambda x: -x["topFormBoost"]):
        md.append(
            f"| {s['race']} | {s['forms']} | {s['topFormBoost']:.1f} | {s['topInherit']:.3f} | "
            f"{s['dmgJump']} | {s['hpJump']} | {s['topHitsToKill']} | {s['topHitFrac']} | "
            f"{s['hitCapFrac']} | `{s['topForm']}` |"
        )

    # Informational notes (glass packs vs mega forms are intentional — RES counters STR).
    notes = []
    # Hard --check failures: future races that would break the difficulty contract.
    hard = []
    for s in summary:
        if (s["hpJump"] or 0) > 2.5:
            hard.append(f"- **{s['race']}**: HP jump {s['hpJump']}× on `{s['topForm']}` (limit 2.5×)")
        elif (s["hpJump"] or 0) > 2.0:
            notes.append(f"- **{s['race']}**: HP jump {s['hpJump']}× on `{s['topForm']}`")
        if (s["topHitFrac"] or 0) > 0.75:
            hard.append(f"- **{s['race']}**: hitFrac {s['topHitFrac']} exceeds 0.75 VIT hard ceiling")
        if (s["dmgJump"] or 0) < 1.05 and s["topFormBoost"] >= 15:
            hard.append(f"- **{s['race']}**: form ×{s['topFormBoost']} barely moves dmg ({s['dmgJump']}×)")
        if (s["topHitsToKill"] or 0) < 0.08:
            notes.append(f"- **{s['race']}**: packs die in {s['topHitsToKill']} live hits (glass OK if RES counters)")

    md += ["", "## Hard flags (--check)", ""]
    md.extend(hard or ["None."])
    md += ["", "## Notes", ""]
    md.extend(notes or ["None."])
    md += ["", f"CSV: `{csv_path}`", ""]
    report = OUT / "ad-race-form-balance-report.md"
    report.write_text("\n".join(md))
    (REPO_OUT / "ad-race-form-balance-report.md").write_text("\n".join(md))

    print("=== T5 m100 physical peak ===")
    print(f"{'race':16} {'boost':>7} {'dmgJ':>6} {'hpJ':>5} {'hits':>6} {'hitF':>6} {'cap':>5} form")
    for s in sorted(summary, key=lambda x: -x["topFormBoost"]):
        print(
            f"{s['race']:16} {s['topFormBoost']:7.1f} {s['dmgJump']:6} {s['hpJump']:5} "
            f"{s['topHitsToKill']:6.2f} {s['topHitFrac']:6.3f} {s['hitCapFrac']:5.3f} {s['topForm']}"
        )
    print("\nHard flags:")
    print("\n".join(hard) if hard else "(none)")
    print("\nNotes:")
    print("\n".join(notes) if notes else "(none)")
    print("Wrote", csv_path, report)
    print(f"Discovered races ({len(races)}): {', '.join(races)}")
    if check_only and hard:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
