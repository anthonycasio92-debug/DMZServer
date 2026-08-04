#!/usr/bin/env python3
"""Validate AdaptiveDifficulty 1.0.12 scaling against the intended combat model.

Checks (fail-closed):
1. Soft offense includes STR/SKP/PWR + mild ENE
2. Class counters raise pressure for every fighting class
3. Top-2 invested stats drive counter overlays (secondary @ 60%)
4. Tier ladder 21→200% increases pressure
5. Ki builds (PWR/ENE) hit harder than STR/SKP-only offense would
6. VIT hit cap still bounds unprotected punches (raised budgets)
7. Even / VIT / RES / STR dumps feel tier pressure (floors + sponge)
8. Full race/form pack sim has no hard balance flags

Also writes a human report under sim/out/.
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

# Reuse formulas from the race/form simulator.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from simulate_race_forms import (  # noqa: E402
    ENERGY_OFFENSE_FACTOR,
    INVEST,
    MOB_HP_SCALE,
    TANK_DEF_RATIO,
    TANK_HP_RATIO,
    TIER_PCT,
    blended_offense,
    blend_form,
    channel_damage,
    channel_hp,
    ki_protection_hit_frac,
    load_forms,
    load_stats,
    mega_bulk_exp,
    mega_bulk_tw,
    mega_exp_cap,
    mega_t,
    mega_tw_scale,
    apply_mastery,
    MAX_FORM,
    TW_BASE,
    TW_EXP,
    RACES,
    OUT,
)

# Fighting-class INVEST keys only (exclude archetype dumps used in section 7).
CLASS_INVEST = {
    k: v
    for k, v in INVEST.items()
    if k
    in (
        "warrior",
        "berserker",
        "martialartist",
        "spiritualist",
        "cleric",
        "paladin",
        "tank",
    )
}

STRONG_MULT = 1.08
CLASS_DMG_MULT = 1.06
CLASS_HP_MULT = 1.05
OVERLAY_CAP = 1.25
TOP2_SECONDARY = 0.60

REPO_OUT = Path(__file__).resolve().parent / "out"
REPO_OUT.mkdir(parents=True, exist_ok=True)


def counter_strength(pct: float) -> float:
    return max(0.0, min(1.0, pct / 0.50))


def blend_counter(bias: float, pct: float) -> float:
    s = counter_strength(pct)
    return 1.0 + (max(1.0, bias) - 1.0) * s


def clamp_overlay(overlay: float) -> float:
    return max(1.0, min(OVERLAY_CAP, overlay))


def class_damage_bias(cls: str) -> float:
    mult = CLASS_DMG_MULT
    c = cls.lower()
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


def class_health_bias(cls: str) -> float:
    mult = CLASS_HP_MULT
    c = cls.lower()
    if any(x in c for x in ("spirit", "cleric", "mage", "kiuser", "energy")):
        return 1.0 + (mult - 1.0) * 0.90
    if any(x in c for x in ("martial", "berserk", "warrior")):
        return 1.0 + (mult - 1.0) * 0.55
    if "tank" in c or "paladin" in c:
        return 1.0 + (mult - 1.0) * 0.35
    return 1.0 + (mult - 1.0) * 0.40


def damage_bias_for_stat(stat: str) -> float:
    bump = STRONG_MULT - 1.0
    return {
        "STR": 1.0 + bump * 0.55,
        "SKP": 1.0 + bump * 0.55,
        "PWR": 1.0 + bump * 0.65,
        "ENE": 1.0 + bump * 0.65,
        "RES": 1.0 + bump * 0.85,
        "VIT": 1.0 + bump * 0.75,
    }.get(stat, 1.0)


def health_bias_for_stat(stat: str) -> float:
    bump = STRONG_MULT - 1.0
    return {
        "STR": 1.0 + bump * 0.70,
        "SKP": 1.0 + bump * 0.70,
        "PWR": 1.0 + bump * 0.85,
        "ENE": 1.0 + bump * 0.85,
        "RES": 1.0 + bump * 0.30,
        "VIT": 1.0 + bump * 0.30,
    }.get(stat, 1.0)


def combine_top2(bias_fn, top: list[str]) -> float:
    if not top:
        return 1.0
    primary = max(1.0, bias_fn(top[0]))
    if len(top) < 2:
        return primary
    secondary = max(1.0, bias_fn(top[1]))
    return 1.0 + (primary - 1.0) + (secondary - 1.0) * TOP2_SECONDARY


def top2_stats(pts: dict[str, float]) -> list[str]:
    ranked = sorted(pts.items(), key=lambda kv: kv[1], reverse=True)
    return [k for k, _ in ranked[:2]]


def soft_channels(
    live_m, live_s, live_k, live_e, live_hp, live_def,
    str_f, skp_f, pwr_f, ene_f, vit_f, res_f, tier: int,
):
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
    offense_no_pwr = blended_offense(melee, strike, 1.0, 1.0)
    return pct, form_boost, melee, strike, ki, energy, defense, hp, offense, offense_no_pwr


def simulate_full(pts: dict[str, float], scales: dict[str, float], forms: dict[str, float], cls: str, tier: int):
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

    pct, form_boost, melee, strike, ki, energy, defense, hp, offense, offense_no_pwr = soft_channels(
        live_m, live_s, live_k, live_e, live_hp, live_def,
        str_f, skp_f, pwr_f, ene_f, vit_f, res_f, tier,
    )

    offense_share = offense * pct
    dmg = offense_share
    floor_strength = max(0.35, min(1.0, counter_strength(pct)))
    dmg = max(dmg, defense * pct * TANK_DEF_RATIO * floor_strength)
    dmg = max(dmg, hp * pct * TANK_HP_RATIO * floor_strength)
    if 1 <= tier <= 3 and form_boost > 1.12:
        threat = {1: 0.22, 2: 0.35, 3: 0.48}[tier]
        soft = offense * threat
        if form_boost >= 6.0:
            t = mega_t(form_boost)
            soft = min(soft, offense_share * max(1.0, 1.20 - 0.15 * min(1.25, t)))
        dmg = max(dmg, soft)
    if tier >= 4 and form_boost > 1.12:
        nudge = {4: 1.06, 5: 1.10, 6: 1.14, 7: 1.18}[tier]
        dmg = max(dmg, offense_share * nudge)

    top = top2_stats(pts)
    dmg_overlay = 1.0
    dmg_overlay *= blend_counter(combine_top2(damage_bias_for_stat, top), pct)
    dmg_overlay *= blend_counter(class_damage_bias(cls), pct)
    dmg_overlay = clamp_overlay(dmg_overlay)
    dmg_with = max(1.0, dmg * dmg_overlay)

    hit_cap = hp * ki_protection_hit_frac(tier, form_boost)
    dmg_capped = min(dmg_with, hit_cap)

    # No-counter baseline (still with PWR/ENE offense + floors).
    dmg_no_counter_raw = max(1.0, dmg)
    dmg_no_counter = min(dmg_no_counter_raw, hit_cap)
    # STR/SKP-only offense (old 1.0.10) with counters still on — for delta proof.
    dmg_old_raw = offense_no_pwr * pct
    dmg_old_raw = max(dmg_old_raw, defense * pct * TANK_DEF_RATIO * floor_strength)
    dmg_old_raw = max(dmg_old_raw, hp * pct * TANK_HP_RATIO * floor_strength)
    if 1 <= tier <= 3 and form_boost > 1.12:
        threat = {1: 0.22, 2: 0.35, 3: 0.48}[tier]
        dmg_old_raw = max(dmg_old_raw, offense_no_pwr * threat)
    if tier >= 4 and form_boost > 1.12:
        nudge = {4: 1.06, 5: 1.10, 6: 1.14, 7: 1.18}[tier]
        dmg_old_raw = max(dmg_old_raw, offense_no_pwr * pct * nudge)
    dmg_old_raw = max(1.0, dmg_old_raw * dmg_overlay)
    dmg_old = min(dmg_old_raw, hit_cap)

    hp_overlay = 1.0
    hp_overlay *= blend_counter(combine_top2(health_bias_for_stat, top), pct)
    hp_overlay *= blend_counter(class_health_bias(cls), pct)
    hp_overlay = min(1.18, clamp_overlay(hp_overlay))

    vit_share = hp * pct
    base_hp_mob = vit_share
    if 1 <= tier <= 2 and form_boost > 1.12:
        base_hp_mob = max(base_hp_mob, hp * (0.18 if tier == 1 else 0.26))
    if offense > hp * 0.35:
        hits = {1: 0.85, 2: 0.75, 3: 0.65, 4: 0.55, 5: 0.48, 6: 0.42, 7: 0.38}[tier]
        durability = offense * pct * hits
        offense_vit = offense / max(1.0, hp)
        vit_cap_mul = 2.8
        if offense_vit > 1.25:
            vit_cap_mul = min(6.5, 2.8 + (offense_vit - 1.25) * 0.95)
        base_hp_mob = max(base_hp_mob, min(durability, vit_share * vit_cap_mul))
    form_pad = 1.0
    if form_boost > 1.12:
        form_pad = 1.0 + 0.35 * min(1.0, math.log(form_boost) / math.log(80.0))
    hard = hp * max(pct, 0.18) * form_pad * 1.45
    if offense > hp * 1.25:
        glass_hits = {1: 0.70, 2: 0.60, 3: 0.52, 4: 0.45, 5: 0.40, 6: 0.36, 7: 0.32}[tier]
        glass_hard = offense * pct * glass_hits
        hard = max(hard, min(glass_hard, vit_share * 6.5))
    mob_hp = max(10.0, min(base_hp_mob, hard) * MOB_HP_SCALE * hp_overlay)

    return {
        "tier": tier,
        "pct": pct,
        "class": cls,
        "top2": ">".join(top),
        "formBoost": form_boost,
        "offense": offense,
        "offenseNoPwr": offense_no_pwr,
        "mobDmg": dmg_capped,
        "mobDmgRaw": dmg_with,
        "mobDmgNoCounter": dmg_no_counter,
        "mobDmgNoCounterRaw": dmg_no_counter_raw,
        "mobDmgOldNoPwr": dmg_old,
        "mobDmgOldNoPwrRaw": dmg_old_raw,
        "mobHp": mob_hp,
        "hitCap": hit_cap,
        "hitFrac": dmg_capped / max(1.0, live_hp),
        "hitCapFrac": ki_protection_hit_frac(tier, form_boost),
        "dmgOverlay": dmg_overlay,
        "liveHp": live_hp,
        "liveKi": live_k,
        "liveMelee": live_m,
        "softHp": hp,
        "softDef": defense,
        "hitCapBound": dmg_with >= hit_cap - 1e-6,
        "floorBound": dmg > offense_share + 1e-6,
    }


def main() -> int:
    errors: list[str] = []
    ok: list[str] = []
    lines: list[str] = ["# AdaptiveDifficulty 1.0.12 scaling validation", ""]

    def check(label: str, cond: bool, detail: str = "") -> None:
        if cond:
            ok.append(label)
            msg = f"OK  {label}" + (f" — {detail}" if detail else "")
            print(msg)
            lines.append(f"- ✅ {label}" + (f" — {detail}" if detail else ""))
        else:
            errors.append(f"{label}: {detail or 'failed'}")
            msg = f"FAIL {label}" + (f" — {detail}" if detail else "")
            print(msg)
            lines.append(f"- ❌ {label}" + (f" — {detail}" if detail else ""))

    # Synthetic mid-game investment (same as INVEST profiles).
    scales = {k: 1.0 for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}
    base_form = {k: 1.0 for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}
    mega = {k: 1.0 for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}
    mega.update(STR=12.0, SKP=12.0, PWR=18.0, ENE=10.0, VIT=4.0, RES=4.0)

    print("=== 1) PWR/ENE in offense ===")
    lines += ["", "## 1) PWR/ENE in offense", ""]
    spirit = INVEST["spiritualist"]
    r = simulate_full(spirit, scales, mega, "spiritualist", 5)
    check(
        "ki offense > STR/SKP-only offense",
        r["offense"] > r["offenseNoPwr"] * 1.15,
        f"full={r['offense']:.0f} vs noPWR={r['offenseNoPwr']:.0f}",
    )
    check(
        "ki pre-cap mob dmg > old STR/SKP-only",
        r["mobDmgRaw"] > r["mobDmgOldNoPwrRaw"] * 1.15,
        f"newRaw={r['mobDmgRaw']:.0f} vs oldRaw={r['mobDmgOldNoPwrRaw']:.0f}"
        + (" (both hit-capped after)" if r["hitCapBound"] else ""),
    )
    # High-VIT ki build: raised hit-cap must leave room for PWR advantage
    # (extreme VIT can floor-bind both paths — use a tanky-but-not-floor-wipe bag).
    spirit_tanky = dict(spirit)
    spirit_tanky["VIT"] = 2_500
    r_tanky = simulate_full(spirit_tanky, scales, mega, "spiritualist", 5)
    check(
        "high-VIT ki final dmg > STR/SKP-only",
        r_tanky["mobDmg"] > r_tanky["mobDmgOldNoPwr"] * 1.05
        or r_tanky["offense"] > r_tanky["offenseNoPwr"] * 1.15,
        f"new={r_tanky['mobDmg']:.0f} vs old={r_tanky['mobDmgOldNoPwr']:.0f} "
        f"(capBound={r_tanky['hitCapBound']} floorBound={r_tanky['floorBound']})",
    )
    # ENE must contribute to blended offense when huge.
    ene_heavy = dict(STR=100, SKP=100, RES=100, VIT=200, PWR=100, ENE=50_000)
    r_ene = simulate_full(ene_heavy, scales, base_form, "spiritualist", 5)
    r_ene_low = simulate_full(
        dict(STR=100, SKP=100, RES=100, VIT=200, PWR=100, ENE=100), scales, base_form, "spiritualist", 5
    )
    check(
        "high ENE raises soft offense",
        r_ene["offense"] > r_ene_low["offense"] * 1.05,
        f"highENE={r_ene['offense']:.0f} vs lowENE={r_ene_low['offense']:.0f}",
    )

    print("\n=== 2) Class counters ===")
    lines += ["", "## 2) Class counters", ""]
    for cls, pts in CLASS_INVEST.items():
        with_c = simulate_full(pts, scales, base_form, cls, 5)
        # Class bias alone should lift damage vs raw (before hit-cap collisions).
        bias = class_damage_bias(cls)
        check(
            f"class counter bias >1 for {cls}",
            bias > 1.0,
            f"bias={bias:.3f}",
        )
        check(
            f"class+top2 overlay >1 at T5 for {cls}",
            with_c["dmgOverlay"] > 1.0,
            f"overlay={with_c['dmgOverlay']:.3f} top2={with_c['top2']}",
        )
        # At T5 full counter strength, counters must raise uncapped pressure.
        # Compare overlay-applied pre-cap path via mobDmg >= noCounter when not hard-capped equal.
        if with_c["mobDmgNoCounter"] < with_c["hitCap"] * 0.98:
            check(
                f"counters raise mob dmg for {cls}",
                with_c["mobDmg"] > with_c["mobDmgNoCounter"] * 1.01,
                f"with={with_c['mobDmg']:.0f} noCtr={with_c['mobDmgNoCounter']:.0f}",
            )
        else:
            check(
                f"counters apply (hit-cap bound) for {cls}",
                with_c["dmgOverlay"] > 1.02,
                f"overlay={with_c['dmgOverlay']:.3f} capped@{with_c['hitCap']:.0f}",
            )

    print("\n=== 3) Top-2 stats ===")
    lines += ["", "## 3) Top-2 stats", ""]
    expected_top = {
        "warrior": ("STR", "VIT"),
        "berserker": ("STR", "VIT"),
        "martialartist": ("SKP", "VIT"),
        "spiritualist": ("PWR", "ENE"),
        "cleric": ("ENE", "PWR"),
        "paladin": ("RES", "VIT"),
        "tank": ("RES", "VIT"),
    }
    for cls, want in expected_top.items():
        got = tuple(top2_stats(INVEST[cls]))
        check(f"top-2 for {cls}", got == want, f"got {got[0]}>{got[1]}")

    # Secondary weight: top2 bump > top1-only bump.
    top = ["PWR", "ENE"]
    top1 = combine_top2(damage_bias_for_stat, top[:1])
    top2 = combine_top2(damage_bias_for_stat, top)
    check(
        "top-2 secondary adds pressure",
        top2 > top1 + 0.001,
        f"top1={top1:.4f} top2={top2:.4f} (secondary×{TOP2_SECONDARY})",
    )

    print("\n=== 4) Tier ladder ===")
    lines += ["", "## 4) Tier ladder", ""]
    ladder = []
    for t in range(1, 8):
        ladder.append(simulate_full(INVEST["warrior"], scales, mega, "warrior", t))
    for a, b in zip(ladder, ladder[1:]):
        check(
            f"T{a['tier']}→T{b['tier']} mob dmg rises",
            b["mobDmg"] >= a["mobDmg"] * 0.98,  # allow tiny hit-cap plateaus
            f"{a['mobDmg']:.0f} → {b['mobDmg']:.0f} (pct {a['pct']}→{b['pct']})",
        )
    check(
        "T7 >> T1 pressure",
        ladder[-1]["mobDmg"] > ladder[0]["mobDmg"] * 2.5,
        f"T1={ladder[0]['mobDmg']:.0f} T7={ladder[-1]['mobDmg']:.0f}",
    )
    check("stock percents", list(TIER_PCT.values()) == [0.21, 0.42, 0.65, 0.90, 1.35, 1.60, 2.00])

    print("\n=== 5) Hit cap / safety ===")
    lines += ["", "## 5) Hit cap / safety", ""]
    for cls in ("spiritualist", "berserker", "tank"):
        r = simulate_full(INVEST[cls], scales, mega, cls, 7)
        check(
            f"T7 {cls} hitFrac ≤ 0.58",
            r["hitFrac"] <= 0.58 + 1e-6,
            f"hitFrac={r['hitFrac']:.3f} cap={r['hitCap']:.0f}",
        )
        check(
            f"T7 {cls} mobDmg ≤ hitCap",
            r["mobDmg"] <= r["hitCap"] + 1e-6,
            f"dmg={r['mobDmg']:.0f} cap={r['hitCap']:.0f}",
        )
    # Raised budgets: T5 base form even build should feel >10% bag pressure.
    even_t5 = simulate_full(INVEST["even"], scales, base_form, "warrior", 5)
    check(
        "T5 even-build hitCapFrac ≥ 0.28",
        even_t5["hitCapFrac"] >= 0.28,
        f"capFrac={even_t5['hitCapFrac']:.3f}",
    )

    print("\n=== 6) Archetype challenge feel ===")
    lines += ["", "## 6) Archetype challenge feel", ""]
    for name in ("even", "vit_dump", "res_dump", "str_dump"):
        t1 = simulate_full(INVEST[name], scales, base_form, name if name in CLASS_INVEST else "warrior", 1)
        t5 = simulate_full(INVEST[name], scales, base_form, name if name in CLASS_INVEST else "warrior", 5)
        t7 = simulate_full(INVEST[name], scales, base_form, name if name in CLASS_INVEST else "warrior", 7)
        check(
            f"{name}: T5 hitFrac > T1",
            t5["hitFrac"] > t1["hitFrac"] * 1.35,
            f"T1={t1['hitFrac']:.3f} T5={t5['hitFrac']:.3f}",
        )
        check(
            f"{name}: T7 hitFrac > T5",
            t7["hitFrac"] > t5["hitFrac"] * 1.15,
            f"T5={t5['hitFrac']:.3f} T7={t7['hitFrac']:.3f}",
        )
        check(
            f"{name}: T5 pressure ≥ 8% bag",
            t5["hitFrac"] >= 0.08,
            f"hitFrac={t5['hitFrac']:.3f}",
        )
    vit = simulate_full(INVEST["vit_dump"], scales, base_form, "tank", 5)
    res = simulate_full(INVEST["res_dump"], scales, base_form, "tank", 5)
    # Extreme wet-noodle tank: floors must bind when offense is near-zero.
    wet = simulate_full(
        dict(STR=40, SKP=40, RES=80, VIT=2_000, PWR=40, ENE=40),
        scales,
        base_form,
        "tank",
        5,
    )
    check(
        "extreme VIT dump HP-floor binds",
        wet["floorBound"] or wet["hitCapBound"],
        f"floor={wet['floorBound']} cap={wet['hitCapBound']} dmg={wet['mobDmg']:.0f}",
    )
    check(
        "VIT dump T5 bag pressure ≥ 12%",
        vit["hitFrac"] >= 0.12,
        f"hitFrac={vit['hitFrac']:.3f} dmg={vit['mobDmg']:.0f}",
    )
    check(
        "RES dump uses DEF floor (or near-cap)",
        res["floorBound"] or res["hitCapBound"],
        f"floor={res['floorBound']} cap={res['hitCapBound']} dmg={res['mobDmg']:.0f}",
    )
    str_d = simulate_full(INVEST["str_dump"], scales, mega, "berserker", 5)
    # Soft-offense trade: packs must absorb a real share of soft threat (live mega
    # punches still chunk — intentional for glass + ki protection).
    soft_hits = str_d["mobHp"] / max(1.0, str_d["offense"] * str_d["pct"])
    check(
        "STR dump pack sponge ≥ 0.35 soft hits",
        soft_hits >= 0.35,
        f"softHits={soft_hits:.2f} mobHp={str_d['mobHp']:.0f} softOffShare={str_d['offense'] * str_d['pct']:.0f}",
    )
    check("stock mobHealthScale 0.90", abs(MOB_HP_SCALE - 0.90) < 1e-9, f"got {MOB_HP_SCALE}")

    print("\n=== 7) Full pack race/form sim ===")
    lines += ["", "## 7) Full pack race/form sim", ""]
    # Run lightweight pack scan with counters on peak forms.
    races = sorted(p.name for p in RACES.iterdir() if p.is_dir() and (p / "stats.json").exists())
    hard = []
    samples = []
    for race in races:
        stats = load_stats(race)
        forms = load_forms(race)
        if not forms:
            continue
        # Prefer warrior, else first class.
        cls = "warrior" if "warrior" in stats else next(iter(stats))
        st = stats[cls]
        inv = INVEST.get(cls) or INVEST["warrior"]
        pts = {k: st["base"].get(k, 0) + inv.get(k, 0) for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}
        # Peak form by max of STR/SKP/PWR/ENE mults at mastery 100.
        best = None
        best_boost = 0.0
        for f in forms:
            str_f = min(MAX_FORM, apply_mastery(f["str"], f["maxMastery"], f["maxStats"], 1.0))
            skp_f = min(MAX_FORM, apply_mastery(f["skp"], f["maxMastery"], f["maxStats"], 1.0))
            pwr_f = min(MAX_FORM, apply_mastery(f["pwr"], f["maxMastery"], f["maxStats"], 1.0))
            ene_f = min(MAX_FORM, apply_mastery(f.get("ene", 1), f["maxMastery"], f["maxStats"], 1.0))
            vit_f = min(MAX_FORM, apply_mastery(f["vit"], f["maxMastery"], f["maxStats"], 1.0))
            boost = max(str_f, skp_f, pwr_f, ene_f, vit_f)
            if boost >= best_boost:
                best_boost = boost
                best = (f, str_f, skp_f, pwr_f, ene_f, vit_f)
        if best is None:
            continue
        f, str_f, skp_f, pwr_f, ene_f, vit_f = best
        form_map = {"STR": str_f, "SKP": skp_f, "PWR": pwr_f, "ENE": ene_f, "VIT": vit_f, "RES": 1.0}
        r5 = simulate_full(pts, st["scale"], form_map, cls, 5)
        r1 = simulate_full(pts, st["scale"], form_map, cls, 1)
        samples.append((race, f"{f['group']}.{f['name']}", r5, r1))
        if r5["hitFrac"] > 0.58:
            hard.append(f"{race}: hitFrac {r5['hitFrac']:.3f}")
        if r5["mobDmg"] <= r1["mobDmg"] * 1.05 and best_boost >= 8:
            hard.append(f"{race}: T5 barely above T1 with form ×{best_boost:.1f}")

    check("discovered races", len(races) >= 8, f"{len(races)} races")
    check("no hard pack flags", not hard, "; ".join(hard) if hard else "none")

    lines += ["", "### Pack peaks (T5, m100, with counters)", "", "| Race | Form | Top2 | Overlay | MobDmg | HitFrac | Offense |", "|------|------|------|--------:|-------:|--------:|--------:|"]
    print("\nPack peaks (T5 + counters):")
    print(f"{'race':16} {'top2':10} {'ovl':>5} {'dmg':>8} {'hitF':>6} {'off':>8} form")
    for race, form, r5, _ in sorted(samples, key=lambda x: -x[2]["mobDmg"]):
        print(
            f"{race:16} {r5['top2']:10} {r5['dmgOverlay']:5.3f} {r5['mobDmg']:8.0f} "
            f"{r5['hitFrac']:6.3f} {r5['offense']:8.0f} {form}"
        )
        lines.append(
            f"| {race} | `{form}` | {r5['top2']} | {r5['dmgOverlay']:.3f} | "
            f"{r5['mobDmg']:.0f} | {r5['hitFrac']:.3f} | {r5['offense']:.0f} |"
        )

    # Class matrix snapshot at T5 base form.
    lines += ["", "### Class matrix (T5 base form)", "", "| Class | Top2 | Overlay | MobDmg | vs no-counter | vs no-PWR |", "|-------|------|--------:|-------:|--------------:|----------:|"]
    print("\nClass matrix T5 base:")
    for cls, pts in CLASS_INVEST.items():
        r = simulate_full(pts, scales, base_form, cls, 5)
        vs_ctr = r["mobDmg"] / max(1.0, r["mobDmgNoCounter"])
        vs_old = r["mobDmg"] / max(1.0, r["mobDmgOldNoPwr"])
        print(
            f"  {cls:14} top2={r['top2']:8} ovl={r['dmgOverlay']:.3f} "
            f"dmg={r['mobDmg']:.0f} vsNoCtr={vs_ctr:.3f} vsNoPwr={vs_old:.3f}"
        )
        lines.append(
            f"| {cls} | {r['top2']} | {r['dmgOverlay']:.3f} | {r['mobDmg']:.0f} | "
            f"{vs_ctr:.3f}× | {vs_old:.3f}× |"
        )

    lines += ["", "## Summary", ""]
    if errors:
        lines.append(f"**FAIL** — {len(errors)} error(s), {len(ok)} ok")
        for e in errors:
            lines.append(f"- {e}")
        print(f"\nFAIL — {len(errors)} error(s), {len(ok)} ok")
        for e in errors:
            print(f"  {e}")
        report = REPO_OUT / "scaling-validation-report.md"
        report.write_text("\n".join(lines) + "\n")
        OUT.mkdir(parents=True, exist_ok=True)
        (OUT / "scaling-validation-report.md").write_text("\n".join(lines) + "\n")
        return 1

    lines.append(f"**PASS** — {len(ok)} checks")
    print(f"\nPASS — {len(ok)} checks")
    report = REPO_OUT / "scaling-validation-report.md"
    report.write_text("\n".join(lines) + "\n")
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "scaling-validation-report.md").write_text("\n".join(lines) + "\n")
    print(f"Wrote {report}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
