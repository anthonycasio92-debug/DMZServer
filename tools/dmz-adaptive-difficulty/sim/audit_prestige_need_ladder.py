#!/usr/bin/env python3
"""Sanity-check PrestigeSystem.needForProgress ladder (mirrors server rules)."""
from __future__ import annotations

LEVELS_PER_PRESTIGE = 20_000
HELD_GATE = 4


def need_for_progress(completed: int, held: int) -> int:
    c = max(0, completed)
    h = max(0, min(10, held))
    if c < HELD_GATE:
        return LEVELS_PER_PRESTIGE
    return 50_000 if h <= 0 else 100_000


def main() -> None:
    errors = []
    for c in (0, 1, 2, 3):
        got = need_for_progress(c, 0)
        if got != 20_000:
            errors.append(f"completed {c}: want 20000, got {got}")
    for h, want in [(0, 50_000), (1, 100_000), (2, 100_000), (3, 100_000), (9, 100_000)]:
        for c in (4, 5, 8):
            got = need_for_progress(c, h)
            if got != want:
                errors.append(f"completed {c} held {h}: want {want}, got {got}")
    if need_for_progress(3, 0) != 20_000:
        errors.append("fourth prestige must stay 20k")
    if need_for_progress(8, 0) != 50_000:
        errors.append("completed 8 held 0 must be 50k")
    if errors:
        print("FAIL prestige need ladder:")
        for e in errors:
            print(" ", e)
        raise SystemExit(1)
    print("OK prestige need ladder (first 4 at 20k; 5+ is 50k or 100k by held)")


if __name__ == "__main__":
    main()
