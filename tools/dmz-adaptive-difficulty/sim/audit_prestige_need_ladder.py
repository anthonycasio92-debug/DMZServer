#!/usr/bin/env python3
"""Sanity-check PrestigeSystem.needForProgress ladder (mirrors server rules)."""
from __future__ import annotations

LEVELS_PER_PRESTIGE = 20_000
MAX_REQUIRED = 100_000
HELD_GATE = 5


def need_for_progress(completed: int, held: int) -> int:
    c = max(0, completed)
    h = max(0, min(10, held))
    if c < HELD_GATE:
        return min(MAX_REQUIRED, (c + 1) * LEVELS_PER_PRESTIGE)
    table = {0: 50_000, 1: 100_000, 2: 145_000}
    return table.get(h, 150_000)


def main() -> None:
    errors = []
    for c, want in [(0, 20_000), (1, 40_000), (2, 60_000), (3, 80_000), (4, 100_000)]:
        got = need_for_progress(c, 0)
        if got != want:
            errors.append(f"completed {c}: want {want}, got {got}")
    for h, want in [(0, 50_000), (1, 100_000), (2, 145_000), (3, 150_000), (9, 150_000)]:
        for c in (5, 6, 8):
            got = need_for_progress(c, h)
            if got != want:
                errors.append(f"completed {c} held {h}: want {want}, got {got}")
    # Must not use (completed+1)×20k in veteran band
    if need_for_progress(8, 0) == 150_000:
        errors.append("completed 8 held 0 must not be 150k (old completed ladder)")
    if errors:
        print("FAIL prestige need ladder:")
        for e in errors:
            print(" ", e)
        raise SystemExit(1)
    print("OK prestige need ladder (completed 0-4 + held gates at C5+)")


if __name__ == "__main__":
    main()
