#!/usr/bin/env python3
"""Validate AdaptiveDifficulty tier buy-cost scaling (stock economy).

Stock curve (2.3.61+): exponential in absolute DMZ level from 1 → 150000
so that:
  - T1 at level 1 = 1× Copper
  - T7 at level 150000 = 100× Netherite (10_000_000 copper-value)
Ladder stays strictly increasing; progress clamps at the anchor.
"""
from __future__ import annotations

import math
import sys

BASES = {1: 1, 2: 5, 3: 15, 4: 50, 5: 150, 6: 500, 7: 1500}
REQUIRED = {1: 1, 2: 500, 3: 1000, 4: 5000, 5: 10000, 6: 50000, 7: 100000}
ANCHOR = 150_000.0
T7_TARGET = 10_000_000  # 100× Netherite
MAX_COINS = 128
DENOMS = [1, 10, 100, 1_000, 10_000, 100_000]  # Copper → Netherite


def normalize(copper: int) -> int:
    if copper <= 0:
        return 0
    value = copper
    for i, unit in enumerate(DENOMS):
        count = (value + unit - 1) // unit
        top = i == len(DENOMS) - 1
        if count <= MAX_COINS or top:
            if top:
                count = min(count, MAX_COINS)
            return count * unit
        value = count * unit
    return value


def cost_strictly_above(copper: int) -> int:
    current = normalize(max(0, copper))
    cap = MAX_COINS * DENOMS[-1]
    if current >= cap:
        return cap
    kind = DENOMS[0]
    for unit in reversed(DENOMS):
        if current % unit == 0 and 0 < current // unit <= MAX_COINS:
            kind = unit
            break
    nxt = normalize(current + kind)
    if nxt <= current:
        nxt = normalize(current + 1)
    return min(cap, max(current + 1, nxt))


def level_mult(level: int, anchor: float = ANCHOR, target: int = T7_TARGET) -> float:
    t7 = max(1, BASES[7])
    end = max(1.0, target / t7)
    a = max(2.0, anchor)
    progress = (max(1, level) - 1.0) / (a - 1.0)
    if progress <= 0.0:
        return 1.0
    if progress >= 1.0:
        return end
    return math.exp(progress * math.log(end))


def scaled_costs(level: int, anchor: float = ANCHOR, target: int = T7_TARGET) -> dict[int, int]:
    mult = level_mult(level, anchor=anchor, target=target)
    costs: dict[int, int] = {}
    for t in range(1, 8):
        raw = max(BASES[t], int(round(BASES[t] * mult)))
        cost = normalize(raw)
        if t > 1:
            prev = costs[t - 1]
            if cost <= prev:
                floor = prev + max(1, prev // 4)
                cost = normalize(max(raw, floor))
                if cost <= prev:
                    cost = cost_strictly_above(prev)
        costs[t] = cost
    return costs


def fmt(c: int) -> str:
    for unit, name in (
        (100_000, "Netherite"),
        (10_000, "Diamond"),
        (1_000, "Emerald"),
        (100, "Gold"),
        (10, "Iron"),
        (1, "Copper"),
    ):
        if c % unit == 0 and 0 < c // unit <= MAX_COINS:
            return f"{c // unit}× {name}"
    return f"{c} copper"


def main() -> int:
    fails = 0

    def check(ok: bool, msg: str) -> None:
        nonlocal fails
        mark = "PASS" if ok else "FAIL"
        print(f"[{mark}] {msg}")
        if not ok:
            fails += 1

    print("=== Anchors (2.3.61 stock curve) ===")
    c1 = scaled_costs(1)
    check(c1[1] == 1, f"T1 at lvl 1 = {fmt(c1[1])} ({c1[1]} copper) == 1× Copper")
    c150 = scaled_costs(150_000)
    check(
        c150[7] == T7_TARGET,
        f"T7 at lvl 150000 = {fmt(c150[7])} ({c150[7]} copper) == 100× Netherite",
    )

    print("\n=== Unlock prices (informative; no longer forced ≈ base) ===")
    for t, req in REQUIRED.items():
        costs = scaled_costs(req)
        print(f"  T{t} at unlock lvl {req}: {fmt(costs[t])} ({costs[t]} copper)")

    print("\n=== Monotonic ladder at key levels ===")
    for level in (1, 500, 1000, 5000, 10000, 50000, 100000, 150000, 200000, 500000):
        costs = scaled_costs(level)
        mono = all(costs[t] > costs[t - 1] for t in range(2, 8))
        line = ", ".join(f"T{t}={fmt(costs[t])}" for t in range(1, 8))
        check(mono, f"lvl {level}: {line}")

    print("\n=== Clamp past anchor (200k == 150k) ===")
    c200 = scaled_costs(200_000)
    check(c200[7] == c150[7], f"T7 at 200k ({c200[7]}) == T7 at 150k ({c150[7]})")
    check(c200[1] == c150[1], f"T1 at 200k ({c200[1]}) == T1 at 150k ({c150[1]})")

    print("\n=== Mid-ladder sanity ===")
    check(c150[1] < c150[7], f"T1 ({c150[1]}) < T7 ({c150[7]}) at 150k")
    check(c150[6] < c150[7], f"T6 ({fmt(c150[6])}) < T7 ({fmt(c150[7])}) at 150k")
    check(c1[7] == BASES[7], f"T7 at lvl 1 stays near base ({fmt(c1[7])})")

    if fails:
        print(f"\n{fails} check(s) failed", file=sys.stderr)
        return 1
    print("\nAll tier-cost checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
