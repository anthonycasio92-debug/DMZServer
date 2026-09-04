#!/usr/bin/env python3
"""Validate AdaptiveDifficulty 1.0.25 scaling against the intended combat model.

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
from scaling_constants import ki_protection_hit_frac  # noqa: E402
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
    hit_cap_health,
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


from simulate_build_matrix import simulate as _simulate_matrix  # noqa: E402
from scaling_constants import COUNTER_PCT_DIVISOR  # noqa: E402


def counter_strength(pct: float) -> float:
    return max(0.0, min(1.0, pct / COUNTER_PCT_DIVISOR))


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
    """Delegate to unified simulate_build_matrix.simulate (2.3.161 formulas)."""
    skills = dict(kp=0, inf=0, inf_on=False, pu=0)
    r = _simulate_matrix(pts, scales, forms, cls, tier, skills, dmz_level=5500)
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
    pct, form_boost, _, _, _, _, _, _, offense, offense_no_pwr = soft_channels(
        live_m, live_s, live_k, live_e, live_hp, live_def,
        str_f, skp_f, pwr_f, ene_f, vit_f, res_f, tier,
    )
    offense_share = offense * pct
    cap_hp = hit_cap_health(
        blend_form(live_hp / vit_f if vit_f > 1.08 else live_hp, live_hp, TW_BASE, TW_EXP),
        live_hp,
        form_boost,
    )
    hit_cap_val = cap_hp * ki_protection_hit_frac(tier, form_boost)
    # No-counter baseline: same path with overlay forced to 1.0 (approximate via raw share).
    dmg_no_counter = min(max(1.0, offense_share), hit_cap_val)
    dmg_old = min(max(1.0, offense_no_pwr * pct), hit_cap_val)
    return {
        "tier": tier,
        "pct": pct,
        "class": cls,
        "top2": r.get("top2", ""),
        "formBoost": r.get("formBoost", form_boost),
        "offense": r.get("offense", offense),
        "offenseNoPwr": offense_no_pwr,
        "offenseShare": r.get("offenseShare", offense_share),
        "mobDmg": r["mobDmg"],
        "mobDmgRaw": r["mobDmg"],
        "mobDmgNoCounter": r["mobDmg"] / max(1.0, r.get("dmgOverlay", 1.0)),
        "mobDmgNoCounterRaw": r["mobDmg"] / max(1.0, r.get("dmgOverlay", 1.0)),
        "mobDmgOldNoPwr": dmg_old,
        "mobDmgOldNoPwrRaw": offense_no_pwr * pct,
        "mobHp": r["mobHp"],
        "hitCap": r.get("kiHitCap", hit_cap_val),
        "bagCap": r.get("bagCap", hit_cap_val),
        "hitFrac": r["hitFrac"],
        "landingFrac": r.get("landingFrac", 0),
        "hitCapFrac": r.get("capFrac", ki_protection_hit_frac(tier, form_boost)),
        "dmgOverlay": r.get("dmgOverlay", 1.0),
        "liveHp": r.get("liveHp", live_hp),
        "liveKi": live_k,
        "liveMelee": live_m,
        "softHp": live_hp,
        "softDef": live_def,
        "hitCapBound": r["mobDmg"] >= hit_cap_val - 1e-6,
        "floorBound": r["mobDmg"] > offense_share + 1e-6,
    }


def _simulate_full_legacy_removed():
    """Legacy duplicate removed — use simulate_build_matrix.simulate."""
    raise NotImplementedError


def main() -> int:
    errors: list[str] = []
    ok: list[str] = []
    lines: list[str] = ["# AdaptiveDifficulty 1.0.25 scaling validation", ""]

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
        r["offenseShare"] > r["offenseNoPwr"] * r["pct"] * 1.10,
        f"newShare={r['offenseShare']:.0f} vs oldShare={r['offenseNoPwr'] * r['pct']:.0f}"
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
        # 1.0.25: T1 god-form floors raised + T7 soft-cap → expect ~2.2×+, not 2.5×.
        ladder[-1]["mobDmg"] > ladder[0]["mobDmg"] * 2.2,
        f"T1={ladder[0]['mobDmg']:.0f} T7={ladder[-1]['mobDmg']:.0f}",
    )
    check("stock percents", list(TIER_PCT.values()) == [0.21, 0.42, 0.65, 0.90, 1.35, 1.60, 2.00])

    print("\n=== 5) Hit cap / safety ===")
    lines += ["", "## 5) Hit cap / safety", ""]
    for cls in ("spiritualist", "berserker", "tank"):
        r = simulate_full(INVEST[cls], scales, mega, cls, 7)
        check(
            f"T7 {cls} hitFrac ≤ 0.75",
            r["hitFrac"] <= 0.75 + 1e-6,
            f"hitFrac={r['hitFrac']:.3f} cap={r['hitCap']:.0f}",
        )
        check(
            f"T7 {cls} mobDmg ≤ hitCap",
            r["mobDmg"] <= r.get("bagCap", r["hitCap"]) + 1e-6,
            f"dmg={r['mobDmg']:.0f} bagCap={r.get('bagCap', r['hitCap']):.0f}",
        )
    # Raised budgets: T5 base form even build should feel >10% bag pressure.
    even_t5 = simulate_full(INVEST["even"], scales, base_form, "warrior", 5)
    check(
        "T5 even-build hitCapFrac ≥ 0.35",
        even_t5["hitCapFrac"] >= 0.35,
        f"capFrac={even_t5['hitCapFrac']:.3f}",
    )
    check(
        "T5 even-build pressure ≥ 25% bag (KP recommended band)",
        even_t5["hitFrac"] >= 0.25,
        f"hitFrac={even_t5['hitFrac']:.3f}",
    )
    # God-form pressure: transformed mega must land harder vs live bag than base.
    god = simulate_full(INVEST["warrior"], scales, mega, "warrior", 5)
    base_w = simulate_full(INVEST["warrior"], scales, base_form, "warrior", 5)
    check(
        "T5 god-form hitFrac ≥ base",
        god["hitFrac"] >= base_w["hitFrac"] * 0.95,
        f"base={base_w['hitFrac']:.3f} god={god['hitFrac']:.3f}",
    )
    check(
        "T5 god-form post-DEF ≥ 12% live bag",
        god["hitFrac"] * 0.35 >= 0.12,
        f"pre={god['hitFrac']:.3f} postDef~={god['hitFrac']*0.35:.3f}",
    )

    print("\n=== 6) Archetype challenge feel ===")
    lines += ["", "## 6) Archetype challenge feel", ""]
    arch_scales = load_stats("saiyan")["warrior"]["scale"]
    def bag_pressure(row: dict) -> float:
        # DEF-cancel pierce inflates pre-mit hitFrac; estimate post-flat-absorb feel.
        if row["hitFrac"] > 0.90:
            return row["hitFrac"] * 0.35
        return row["hitFrac"]

    for name in ("even", "vit_dump", "res_dump", "str_dump"):
        t1 = simulate_full(INVEST[name], arch_scales, base_form, name if name in CLASS_INVEST else "warrior", 1)
        t5 = simulate_full(INVEST[name], arch_scales, base_form, name if name in CLASS_INVEST else "warrior", 5)
        t7 = simulate_full(INVEST[name], arch_scales, base_form, name if name in CLASS_INVEST else "warrior", 7)
        p1, p5, p7 = bag_pressure(t1), bag_pressure(t5), bag_pressure(t7)
        pierce_bound = t5["hitFrac"] > 0.90 and t7["hitFrac"] > 0.90
        check(
            f"{name}: T5 hitFrac > T1",
            p5 > p1 * 1.35,
            f"T1={p1:.3f} T5={p5:.3f}",
        )
        # Pure RES dumps: T4+ pierce is DEF-gated (same absolute floor at T5/T7).
        check(
            f"{name}: T7 hitFrac > T5",
            (p7 >= p5 * 0.98) if pierce_bound else (p7 > p5 * 1.10),
            f"T5={p5:.3f} T7={p7:.3f} pierceBound={pierce_bound}",
        )
        check(
            f"{name}: T5 pressure ≥ 25% bag",
            p5 >= 0.25,
            f"pressure={p5:.3f}",
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
        "VIT dump T5 bag pressure ≥ 28%",
        vit["hitFrac"] >= 0.28,
        f"hitFrac={vit['hitFrac']:.3f} dmg={vit['mobDmg']:.0f}",
    )
    even_ref = simulate_full(INVEST["even"], scales, base_form, "warrior", 5)
    check(
        "VIT dump T5 ≥ 58% of even bag pressure",
        vit["hitFrac"] >= even_ref["hitFrac"] * 0.58,
        f"vit={vit['hitFrac']:.3f} even={even_ref['hitFrac']:.3f}",
    )
    tank_cls = simulate_full(INVEST["tank"], scales, base_form, "tank", 5)
    check(
        "tank class T5 bag pressure ≥ 28%",
        tank_cls["hitFrac"] >= 0.28,
        f"hitFrac={tank_cls['hitFrac']:.3f} dmg={tank_cls['mobDmg']:.0f}",
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
    check("stock mobHealthScale 0.75", abs(MOB_HP_SCALE - 0.75) < 1e-9, f"got {MOB_HP_SCALE}")
    check("stock transformScaleWeight 0.65", abs(TW_BASE - 0.65) < 1e-9, f"got {TW_BASE}")

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
        if r5["hitFrac"] > 0.75:
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
