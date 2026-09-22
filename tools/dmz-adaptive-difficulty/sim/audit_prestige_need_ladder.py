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
        got = need_for_progress(6, h)
        if got != want:
            errors.append(f"completed 6 held {h}: want {want}, got {got}")
    if errors:
        print("FAIL prestige need ladder:")
        for e in errors:
            print(" ", e)
        raise SystemExit(1)
    print("OK prestige need ladder (completed 0-4 + held gates at C5+)")


if __name__ == "__main__":
    main()
