"""Single source of truth for AdaptiveDifficulty combat scaling (Python sim).

Mirrors PlayerCombatProfile.java on 2.3.175 (formula revision 46).
Update Java and this file together; audit_concept.py string-checks Java literals.
"""
from __future__ import annotations

import math

# Stock tier ladder (DifficultyConfig.unlockTier*EnemyMult)
TIER_PCT: dict[int, float] = {
    1: 0.21,
    2: 0.42,
    3: 0.65,
    4: 0.90,
    5: 1.35,
    6: 1.60,
    7: 2.00,
}

# Incoming soft-cap (incomingSoftCapFrac / LivingDamageEvent ceiling)
SOFT_CAP: dict[int, float] = {
    1: 0.30,
    2: 0.32,
    3: 0.36,
    4: 0.40,
    5: 0.44,
    6: 0.48,
    7: 0.52,
}

# Ki-protection hit-cap tier fractions (kiProtectionHitFrac, pre-DEF)
HIT_CAP_TIER: dict[int, float] = {
    1: 0.22,
    2: 0.28,
    3: 0.34,
    4: 0.40,
    5: 0.44,
    6: 0.48,
    7: 0.52,
}

# God-form threat floors T1–T3 (formBoost > 1.12)
GOD_THREAT: dict[int, float] = {1: 0.35, 2: 0.42, 3: 0.48}

# Form nudges T4–T7
FORM_NUDGE: dict[int, float] = {4: 1.06, 5: 1.11, 6: 1.16, 7: 1.20}

# Live-offense share (liveShare)
LIVE_SHARE: dict[int, float] = {
    1: 0.22,
    2: 0.28,
    3: 0.34,
    4: 0.42,
    5: 0.53,
    6: 0.58,
    7: 0.62,
}

# Landing safety-net fractions (targetLandingDamage landFrac)
LAND_FRAC: dict[int, float] = {
    1: 0.13,
    2: 0.16,
    3: 0.20,
    4: 0.28,
    5: 0.35,
    6: 0.40,
    7: 0.44,
}

# Landing cap fractions (landCap)
LAND_CAP: dict[int, float] = {
    1: 0.18,
    2: 0.22,
    3: 0.26,
    4: 0.32,
    5: 0.38,
    6: 0.42,
    7: 0.46,
}

# Mob HP sponge durability hits (targetMobHealth)
SPONGE_HITS: dict[int, float] = {
    1: 1.00,
    2: 0.90,
    3: 0.78,
    4: 0.68,
    5: 0.58,
    6: 0.52,
    7: 0.48,
}

GLASS_HITS: dict[int, float] = {
    1: 0.82,
    2: 0.72,
    3: 0.62,
    4: 0.54,
    5: 0.48,
    6: 0.44,
    7: 0.40,
}

# DMZ tier unlock gates for paintEase (tierRequiredLevel stock defaults)
TIER_DMZ_GATE: dict[int, int] = {
    1: 1,
    2: 500,
    3: 1000,
    4: 5000,
    5: 10000,
    6: 50000,
    7: 100000,
}

TIER_DMZ_BAND_TOP: dict[int, int] = {
    7: 150000,  # tierCostLevelAnchor for T7 band
}

# Tank / counter knobs
TANK_DEF_RATIO = 0.45
TANK_HP_RATIO = 0.28
COUNTER_PCT_DIVISOR = 0.90
HP_FLOOR_STRENGTH_MIN = 0.60
COUNTER_STRENGTH_MIN = 0.35

# Concept targets (audit_concept.py executable spec)
CONCEPT_EVEN_HITFRAC_MIN: dict[int, float] = {5: 0.25, 7: 0.39}
CONCEPT_DUMP_HITFRAC_MIN = 0.28
CONCEPT_DUMP_VS_EVEN_MIN = 0.58
CONCEPT_GOD_LANDING_MIN: dict[int, float] = {1: 0.03, 5: 0.22, 7: 0.30}
CONCEPT_TIER_RISE_MIN: dict[tuple[int, int], float] = {(1, 5): 1.4, (5, 7): 1.10}

FORMULA_REVISION = 46

# Landing paintEase blend — veterans keep relief without crushing god-form safety-net.
LANDING_EASE_FLOOR = 0.70
LANDING_EASE_SCALE = 0.30

# Telemetry form bands — thresholds from pack form JSON peaks (see sim/out/form-band-reference.md).
# formBoost = max live form mult across STR/SKP/PWR/ENE/VIT/RES (cap ~100).
#
# Saiyan ladder (@m100): base → awakened(~5) → super(~9–15) → ultra(~22) → divine(~24–40)
# → enhancement(~54) → apex(≥80). Most races cap in ultra (~21.8×).
FORM_BAND_BASE = 1.12          # AD "not transformed" gate
FORM_BAND_AWAKENED = 6.0       # SSJ1 / oozaru / semi-perfect cluster
FORM_BAND_SUPER = 15.0         # SSJ2, perfect cell, aspect viltrumite
FORM_BAND_ULTRA = 22.0         # SSJ3/4, ultra perfect — default ceiling for most races
FORM_BAND_DIVINE = 50.0        # SSG, SSB, beyond god, transcendent lines
FORM_BAND_ENHANCEMENT = 80.0   # Android overclock, metal overdrive (MEGA_FORM_TARGET)
FORM_BAND_APEX = 80.0          # Rare cap / primal apex (same cut as enhancement top)
FORM_BAND_ORDER = (
    "base", "awakened", "super", "ultra", "divine", "enhancement", "apex",
)
FORM_BAND_LABELS: dict[str, str] = {
    "base": "Base",
    "awakened": "Awakened",
    "super": "Super",
    "ultra": "Ultra",
    "divine": "Divine",
    "enhancement": "Enhancement",
    "apex": "Apex",
}
# Example anchors per band (saiyan unless noted)
FORM_BAND_EXAMPLES: dict[str, str] = {
    "base": "No transform (×1)",
    "awakened": "SSJ1, Oozaru, Semi-Perfect (~2–6×)",
    "super": "SSJ2, SSJ3, Perfect Cell (~6–15×)",
    "ultra": "SSJ4, Ultra Perfect, Super Namek (~15–22×)",
    "divine": "SSG, SSB, Beyond God (~22–50×)",
    "enhancement": "Overclock, SSDroid4, Metal Overdrive (~50–80×)",
    "apex": "Primal God / cap forms (≥80×)",
}


def band_form(fb: float) -> str:
    """Classify live formBoost for telemetry (not combat math)."""
    v = max(1.0, fb)
    if v <= FORM_BAND_BASE:
        return "base"
    if v < FORM_BAND_AWAKENED:
        return "awakened"
    if v < FORM_BAND_SUPER:
        return "super"
    if v < FORM_BAND_ULTRA:
        return "ultra"
    if v < FORM_BAND_DIVINE:
        return "divine"
    if v < FORM_BAND_ENHANCEMENT:
        return "enhancement"
    return "apex"


def band_form_label(fb: float) -> str:
    return FORM_BAND_LABELS.get(band_form(fb), band_form(fb))


def band_form_legacy(fb: float) -> str:
    """Pre-2.3.171 bands (base / mid / high / god≥25) — historical comparison."""
    v = max(1.0, fb)
    if v <= 1.12:
        return "base"
    if v < 6:
        return "mid"
    if v < 25:
        return "high"
    return "god"


DEFENSE_MIT_RELIEF_BASE = 0.68  # mitigated fraction below which no paint relief
DEFENSE_MIT_RELIEF_SCALE = 0.28
DEFENSE_MIT_RELIEF_CAP = 0.12
DEFENSIVE_PAINT_RELIEF_CAP = 0.22

# DMZ defense mitigation probe (flat + % — enchant DEF included in getDefense)
DMZ_FLAT_ABSORB_FRAC = 0.65
DMZ_DEF_REDUCTION_SCALE = 12.0  # stock floor: max(12, maxValue * scaling * 0.15)
DMZ_BASE_REDUCTION_CAP = 0.75
DMZ_ENCHANT_REDUCTION_CAP = 0.85

# Ki Protection — DMZ combat.json + AD paint relief (2.3.163)
KP_MITIGATION_PER_LEVEL = 0.01  # post-mit DMZ
KP_HIT_CAP_RELIEF_PER_LEVEL = 0.010  # pre-DEF hit-cap fraction
KP_LANDING_RELIEF_PER_LEVEL = 0.015  # safety-net landing
KP_PAINT_RELIEF_PER_LEVEL = 0.010  # soft-cap / hit-cap stack


def paint_ease(tier: int, dmz_level: int = 5500) -> float:
    """DMZ-level paint ease — mirrors PlayerCombatProfile.paintEase."""
    min_l = TIER_DMZ_GATE.get(tier, 1)
    if tier < 7:
        next_gate = TIER_DMZ_GATE.get(tier + 1, min_l * 2)
    else:
        next_gate = TIER_DMZ_BAND_TOP.get(7, 150000)
    level = max(1, dmz_level)
    if level < min_l:
        return 1.0
    clamped = max(min_l, min(next_gate, level))
    band_t = (clamped - min_l) / max(1, next_gate - min_l)
    band_ease = 0.78 + 0.22 * max(0.0, min(1.0, band_t))
    over_ease = 1.0
    if level > next_gate:
        over_ease = max(0.40, min(1.0, (next_gate / level) ** 0.5))
    ease = band_ease * over_ease
    if tier >= 4 and level >= 50000:
        vet = max(0.78, min(1.0, (50000 / level) ** 0.25))
        ease *= vet
    return max(0.35, min(1.0, ease))


def dmz_mitigate_damage(raw: float, defense: float, res_form: float = 1.0) -> float:
    """Port of DMZ StatsData.calculatePostMitigationDamage core (no cancel path)."""
    raw = max(0.0, raw)
    def_val = max(0.0, defense * max(1.0, res_form))
    if def_val <= 0.0 or raw <= 0.0:
        return raw
    flat_cap = raw * DMZ_FLAT_ABSORB_FRAC
    flat_absorb = min(def_val, flat_cap)
    remaining = max(0.0, raw - flat_absorb)
    ratio = def_val / (DMZ_DEF_REDUCTION_SCALE + def_val)
    ratio = min(DMZ_BASE_REDUCTION_CAP, max(0.0, ratio))
    return remaining * (1.0 - ratio)


def estimate_mitigation_relief(defense: float, res_form: float = 1.0, kp_level: int = 0) -> float:
    """Mirrors PlayerCombatProfile.estimateMitigationRelief."""
    probe = 10_000.0
    kp_mit = min(0.10, kp_level * KP_MITIGATION_PER_LEVEL)
    def_eff = defense * max(1.0, res_form) * max(0.0, 1.0 - kp_mit)
    post = dmz_mitigate_damage(probe, def_eff)
    mitigated = 1.0 - min(1.0, post / probe)
    excess = max(0.0, mitigated - DEFENSE_MIT_RELIEF_BASE)
    return min(DEFENSE_MIT_RELIEF_CAP, excess * DEFENSE_MIT_RELIEF_SCALE)


def defensive_paint_relief(kp_level: int, defense: float, res_form: float = 1.0) -> float:
    relief = estimate_mitigation_relief(defense, res_form, kp_level)
    if kp_level > 0:
        relief += kp_level * KP_PAINT_RELIEF_PER_LEVEL
    return min(DEFENSIVE_PAINT_RELIEF_CAP, relief)


def ki_protection_hit_frac(
    tier: int, form_boost: float, kp_level: int = 0, mit_relief: float = 0.0
) -> float:
    tier_frac = HIT_CAP_TIER[tier]
    if form_boost <= 1.12:
        form_factor = 0.78
    else:
        t = min(1.0, math.log(max(1.12, form_boost)) / math.log(80.0))
        form_factor = 0.78 + 0.22 * t
        if tier >= 7 and form_boost >= 25.0:
            form_factor = min(form_factor, 0.88)
    frac = tier_frac * form_factor
    if kp_level > 0:
        frac *= max(0.82, 1.0 - kp_level * KP_HIT_CAP_RELIEF_PER_LEVEL)
    if mit_relief > 1e-6:
        frac *= max(0.85, 1.0 - mit_relief * 0.75)
    return max(0.12, min(0.75, frac))


def incoming_soft_cap_frac(tier: int, relief: float = 0.0) -> float:
    base = SOFT_CAP[tier]
    if relief > 1e-6:
        base *= max(0.78, 1.0 - relief * 0.90)
    return base

