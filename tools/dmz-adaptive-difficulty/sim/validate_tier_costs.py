#!/usr/bin/env python3
"""Validate AdaptiveDifficulty tier buy-cost scaling (stock economy).

Guards the 100k / T7-unlock regression:
  - buying at each unlock ≈ that tier's base (before tiny normalize snaps)
  - ladder stays strictly increasing
  - T7 at 100k must not be ladder-dragged into Emerald+/Netherite piles
"""
from __future__ import annotations

import sys

BASES = {1: 1, 2: 5, 3: 15, 4: 50, 5: 150, 6: 500, 7: 1500}
REQUIRED = {1: 1, 2: 500, 3: 1000, 4: 5000, 5: 10000, 6: 50000, 7: 100000}
DIVISOR = 50_000.0
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
    # step by preferred unit (largest exact divisor)
    kind = DENOMS[0]
    for unit in reversed(DENOMS):
        if current % unit == 0 and 0 < current // unit <= MAX_COINS:
            kind = unit
            break
    nxt = normalize(current + kind)
    if nxt <= current:
        nxt = normalize(current + 1)
    return min(cap, max(current + 1, nxt))


def scaled_costs(level: int, divisor: float = DIVISOR) -> dict[int, int]:
    costs: dict[int, int] = {}
    for t in range(1, 8):
        excess = max(0, level - REQUIRED[t])
        mult = 1.0 + excess / divisor
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

    print("=== Unlock ≈ base (stock divisor 50000) ===")
    for t, req in REQUIRED.items():
        costs = scaled_costs(req)
        # Allow normalize snap + at most one ladder step above base for safety floor.
        # With divisor 50000, T7 at 100k should be exactly base (1500 → 15× Gold).
        ratio = costs[t] / BASES[t]
        check(
            ratio <= 1.35,
            f"T{t} at unlock lvl {req}: {fmt(costs[t])} ({costs[t]} copper) "
            f"≤ 1.35× base {BASES[t]} (ratio={ratio:.2f})",
        )

    print("\n=== Monotonic ladder at key levels ===")
    for level in (1, 500, 1000, 5000, 10000, 50000, 100000, 200000, 500000):
        costs = scaled_costs(level)
        mono = all(costs[t] > costs[t - 1] for t in range(2, 8))
        line = ", ".join(f"T{t}={fmt(costs[t])}" for t in range(1, 8))
        check(mono, f"lvl {level}: {line}")

    print("\n=== 100k regression (T7 must stay Gold-tier, not Emerald pile) ===")
    c100 = scaled_costs(100_000)
    check(c100[7] <= 2_000, f"T7 at 100k = {fmt(c100[7])} ({c100[7]} copper) ≤ 2000")
    check(c100[6] < c100[7], f"T6 ({c100[6]}) < T7 ({c100[7]}) at 100k")
    check(c100[7] <= BASES[7] * 1.35, f"T7 not ladder-dragged past 1.35× base")

    # Old broken divisor for contrast (documentation only).
    broken = scaled_costs(100_000, divisor=1_000.0)
    print(
        f"\n(reference) old divisor=1000 at 100k → T7={fmt(broken[7])} "
        f"({broken[7]} copper) — should look obviously worse"
    )

    if fails:
        print(f"\n{fails} check(s) failed", file=sys.stderr)
        return 1
    print("\nAll tier-cost checks passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
