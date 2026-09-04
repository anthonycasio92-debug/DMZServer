#!/usr/bin/env python3
"""Fail-closed sync between scaling_constants.py and Java combat profile.

Run on every mod build. If Python sim constants and PlayerCombatProfile /
DifficultyConfig literals diverge, scaling audits will lie and balance will drift.
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from audit_lib import (  # noqa: E402
    contains_literal,
    extract_method,
    mod_version,
    read_config,
    read_profile,
    SIMULATE_MATRIX,
)
from scaling_constants import (  # noqa: E402
    FORM_NUDGE,
    FORMULA_REVISION,
    GOD_THREAT,
    HIT_CAP_TIER,
    LAND_CAP,
    LAND_FRAC,
    LIVE_SHARE,
    SOFT_CAP,
    TIER_PCT,
)


def main() -> int:
    profile = read_profile()
    cfg = read_config()
    errors: list[str] = []

    def check(label: str, ok: bool, detail: str = "") -> None:
        if not ok:
            errors.append(f"{label}" + (f": {detail}" if detail else ""))

    soft_body = extract_method(profile, "incomingSoftCapFrac")
    hit_body = extract_method(profile, "kiProtectionHitFrac")
    land_body = extract_method(profile, "targetLandingDamage")
    dmg_body = extract_method(profile, "targetMobDamage")

    for tier, val in SOFT_CAP.items():
        default = 1 if tier == 1 else None
        check(
            f"SOFT_CAP T{tier}",
            contains_literal(soft_body, tier, val, default_tier=default),
            f"expected {val}",
        )

    for tier, val in HIT_CAP_TIER.items():
        default = 7 if tier == 7 else None
        check(
            f"HIT_CAP_TIER T{tier}",
            contains_literal(hit_body, tier, val, default_tier=default),
            f"expected {val}",
        )

    for tier, val in FORM_NUDGE.items():
        default = 7 if tier == 7 else None
        check(
            f"FORM_NUDGE T{tier}",
            contains_literal(dmg_body, tier, val, default_tier=default),
            f"expected {val}",
        )

    for tier, val in LIVE_SHARE.items():
        default = 7 if tier == 7 else None
        check(
            f"LIVE_SHARE T{tier}",
            contains_literal(dmg_body, tier, val, default_tier=default),
            f"expected {val}",
        )

    for tier, val in LAND_FRAC.items():
        default = 7 if tier == 7 else None
        check(
            f"LAND_FRAC T{tier}",
            contains_literal(land_body, tier, val, default_tier=default),
            f"expected {val}",
        )

    for tier, val in LAND_CAP.items():
        default = 7 if tier == 7 else None
        check(
            f"LAND_CAP T{tier}",
            contains_literal(land_body, tier, val, default_tier=default),
            f"expected {val}",
        )

    for tier, val in GOD_THREAT.items():
        check(
            f"GOD_THREAT T{tier}",
            contains_literal(dmg_body, tier, val),
            f"expected {val}",
        )

    check("T7 mega live-slice 0.24", "0.24 * Math.min" in dmg_body)
    check("formula revision 45", f"mix(h, {FORMULA_REVISION}L)" in profile)

    for tier, pct in TIER_PCT.items():
        field = f"unlockTier{tier}EnemyMult = {pct}"
        alt = f"unlockTier{tier}EnemyMult = {pct:.2f}".rstrip("0").rstrip(".")
        check(f"TIER_PCT T{tier}", field in cfg or alt in cfg, f"expected {pct}")

    sim_text = SIMULATE_MATRIX.read_text(encoding="utf-8", errors="replace")
    for key in ("dmgOverlay=", "kiHitCap=", "bagCap=", "offenseShare="):
        check(f"simulate_build_matrix returns {key.rstrip('=')}", key in sim_text)

    print(f"# Scaling sync audit — mod {mod_version()}, formula rev {FORMULA_REVISION}\n")
    if errors:
        for e in errors:
            print(f"FAIL {e}")
        print(f"\nFAIL — {len(errors)} Java/Python drift(s)")
        print("Update scaling_constants.py AND PlayerCombatProfile.java together.")
        return 1

    print("PASS — Java literals match scaling_constants.py")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
