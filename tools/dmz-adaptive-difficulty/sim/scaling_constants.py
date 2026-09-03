"""Single source of truth for AdaptiveDifficulty combat scaling (Python sim).

Mirrors PlayerCombatProfile.java on 2.3.162 (formula revision 44).
Update Java and this file together; audit_concept.py string-checks Java literals.
"""
from __future__ import annotations

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
FORM_NUDGE: dict[int, float] = {4: 1.06, 5: 1.10, 6: 1.14, 7: 1.18}

# Live-offense share (liveShare)
LIVE_SHARE: dict[int, float] = {
    1: 0.22,
    2: 0.28,
    3: 0.34,
    4: 0.42,
    5: 0.50,
    6: 0.55,
    7: 0.58,
}

# Landing safety-net fractions (targetLandingDamage landFrac)
LAND_FRAC: dict[int, float] = {
    1: 0.13,
    2: 0.16,
    3: 0.20,
    4: 0.28,
    5: 0.33,
    6: 0.38,
    7: 0.42,
}

# Landing cap fractions (landCap)
LAND_CAP: dict[int, float] = {
    1: 0.18,
    2: 0.22,
    3: 0.26,
    4: 0.32,
    5: 0.36,
    6: 0.40,
    7: 0.44,
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
CONCEPT_EVEN_HITFRAC_MIN: dict[int, float] = {5: 0.25, 7: 0.40}
CONCEPT_DUMP_HITFRAC_MIN = 0.28
CONCEPT_DUMP_VS_EVEN_MIN = 0.58
CONCEPT_GOD_LANDING_MIN: dict[int, float] = {1: 0.03, 5: 0.22, 7: 0.30}
CONCEPT_TIER_RISE_MIN: dict[tuple[int, int], float] = {(1, 5): 1.4, (5, 7): 1.10}

FORMULA_REVISION = 44


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
        vet = max(0.70, min(1.0, (50000 / level) ** 0.25))
        ease *= vet
    return max(0.35, min(1.0, ease))

