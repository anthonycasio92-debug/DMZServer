#!/usr/bin/env python3
"""Runtime check: race keep-% charges stay distinct under Ancient Coin cap.

Mirrors CharacterServicesSystem.raceCost + compressRaceScaledUnderCap +
AncientCoinEconomy.normalizeCost with live-shaped defaults (level mult on,
preservation ladder 10→100k … 100→1M, T7 base 1500 → target 10M).
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

CAP = 128 * 100_000  # AncientCoinEconomy.maxNormalizedCost
LADDER = [1, 10, 100, 1_000, 10_000, 100_000]
MAX_COINS = 128

# character-services.default.json / live character-services.json
PRESERVATION = {
    0: 0,
    10: 100_000,
    20: 200_000,
    30: 300_000,
    40: 400_000,
    50: 500_000,
    60: 600_000,
    70: 700_000,
    80: 800_000,
    90: 900_000,
    100: 1_000_000,
}


def lookup_preservation_copper(pct: int) -> int:
    if pct <= 0:
        return 0
    pct = min(100, pct)
    if pct in PRESERVATION:
        return PRESERVATION[pct]
    keys = sorted(PRESERVATION)
    lower = max(k for k in keys if k <= pct)
    higher = min(k for k in keys if k >= pct)
    if lower == higher:
        return PRESERVATION[lower]
    span = higher - lower
    rise = PRESERVATION[higher] - PRESERVATION[lower]
    return round(PRESERVATION[lower] + rise * (pct - lower) / span)


def normalize_cost(copper: int) -> int:
    if copper <= 0:
        return 0
    value = copper
    for i, unit in enumerate(LADDER):
        count = (value + unit - 1) // unit
        top = i == len(LADDER) - 1
        if count <= MAX_COINS or top:
            if top:
                count = min(count, MAX_COINS)
            return count * unit
        value = count * unit
    return value


def compress_race_scaled_under_cap(scaled: int, scaled100: int) -> int:
    if scaled <= 0:
        return 0
    if scaled100 > CAP and scaled100 > 0:
        return max(1, round(scaled * (CAP / scaled100)))
    return scaled


def tier_cost_level_multiplier(dmz_level: int, t7_base: int = 1500, target: int = 10_000_000, anchor: float = 150_000.0) -> float:
    end_mult = max(1.0, target / max(1, t7_base))
    progress = (max(1, dmz_level) - 1.0) / (max(2.0, anchor) - 1.0)
    if progress <= 0.0:
        return 1.0
    if progress >= 1.0:
        return end_mult
    import math
    return math.exp(progress * math.log(end_mult))


def race_cost_legacy(base: int, mult: float) -> int:
    """Old path: payableCost = normalize(scaled) — collapses at the cap."""
    scaled = max(1, round(base * mult))
    return normalize_cost(scaled)


def race_cost_fixed(base: int, base100: int, mult: float) -> int:
    scaled = max(1, round(base * mult))
    scaled100 = max(1, round(base100 * mult))
    return normalize_cost(compress_race_scaled_under_cap(scaled, scaled100))


def main() -> int:
    mult = tier_cost_level_multiplier(150_000)
    pcts = (25, 50, 75, 100)
    bases = {p: lookup_preservation_copper(p) for p in pcts}
    base100 = bases[100]

    print(f"mult@{150_000} = {mult:.6f}")
    print(f"cap = {CAP}")
    print("bases:", bases)

    print("\n=== BEFORE (normalize only) ===")
    before = {p: race_cost_legacy(bases[p], mult) for p in pcts}
    for p in pcts:
        print(f"  {p}% -> {before[p]}")
    if len(set(before.values())) != 1:
        print("FAIL: expected collapse to one charge before fix")
        return 1
    print("  collapsed to one charge (bug repro OK)")

    print("\n=== AFTER (compressRaceScaledUnderCap) ===")
    after = {p: race_cost_fixed(bases[p], base100, mult) for p in pcts}
    for p in pcts:
        print(f"  {p}% -> {after[p]}")

    ordered = [after[p] for p in pcts]
    if not (ordered[0] < ordered[1] < ordered[2] < ordered[3]):
        print("FAIL: charges must strictly increase 25 < 50 < 75 < 100")
        return 1
    if after[100] != CAP:
        print(f"FAIL: 100% should sit at cap {CAP}, got {after[100]}")
        return 1
    # Low level: no compression, ladder still distinct
    low_mult = tier_cost_level_multiplier(1)
    low = {p: race_cost_fixed(bases[p], base100, low_mult) for p in pcts}
    if not (low[25] < low[50] < low[100]):
        print("FAIL: low-level ladder should remain distinct")
        return 1
    print("\nPASS: 25/50/75/100% charge differently under high level mult")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
