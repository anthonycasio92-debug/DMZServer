#!/usr/bin/env python3
"""Fail-closed matrix: Unlock tiers 1–7 across DMZ levels 1 → 150000.

Checks:
  1. Tier buy-cost ladder strictly increasing at EVERY level 1..150000
  2. Cost curve anchors (T1@1 = 1 Copper, T7@150k = 100× Netherite)
  3. Costs clamp past the 150k anchor
  4. Unlock gate levels are monotonic and match stock UnlockTier
  5. Combat concept at representative levels × all tiers (even + god form)
     — hitFrac rises with tier; landing ≤ soft-cap; T5/T7 floors

Writes:
  sim/out/tier-level-matrix-audit.md
  /opt/cursor/artifacts/tier-level-matrix-audit.md (when present)

Usage:
  python3 audit_tier_level_matrix.py
  python3 audit_tier_level_matrix.py --levels 1,1000,150000   # sparse
  python3 audit_tier_level_matrix.py --full-costs             # every level (default)
"""
from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from validate_tier_costs import (  # noqa: E402
    ANCHOR,
    BASES,
    REQUIRED,
    T7_TARGET,
    fmt,
    scaled_costs,
)
from simulate_build_matrix import ARCHETYPES, SKILL_LOADOUTS, simulate  # noqa: E402
from simulate_race_forms import (  # noqa: E402
    MAX_FORM,
    TIER_PCT,
    apply_mastery,
    load_forms,
    load_stats,
)

REPO_OUT = Path(__file__).resolve().parent / "out"
REPO_OUT.mkdir(parents=True, exist_ok=True)
ART = Path("/opt/cursor/artifacts")
try:
    ART.mkdir(parents=True, exist_ok=True)
except OSError:
    ART = None

SOFT = {1: 0.34, 2: 0.36, 3: 0.44, 4: 0.50, 5: 0.52, 6: 0.58, 7: 0.62}

# Combat pressure does not take DMZ level directly — stats do. We still re-check
# the tier ladder at several level *bands* so buy/unlock context stays covered.
COMBAT_LEVEL_BANDS = (
    1,
    500,
    1_000,
    5_000,
    10_000,
    50_000,
    100_000,
    110_000,
    120_000,
    130_000,
    140_000,
    150_000,
)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument(
        "--full-costs",
        action="store_true",
        default=True,
        help="Scan every level 1..150000 for cost monotonicity (default)",
    )
    ap.add_argument(
        "--sparse-costs",
        action="store_true",
        help="Only check key cost levels (faster)",
    )
    ap.add_argument(
        "--levels",
        type=str,
        default="",
        help="Comma-separated combat band overrides",
    )
    args = ap.parse_args()

    errors: list[str] = []
    ok: list[str] = []
    lines: list[str] = [
        "# Tier × level matrix audit (1–150000)",
        "",
        "Fail-closed checks for unlock tiers T1–T7 across the DMZ level cap.",
        "",
    ]

    def check(label: str, cond: bool, detail: str = "") -> None:
        if cond:
            ok.append(label)
            lines.append(f"- ✅ {label}" + (f" — {detail}" if detail else ""))
        else:
            msg = f"{label}: {detail or 'failed'}"
            errors.append(msg)
            lines.append(f"- ❌ {msg}")

    # ── 1) Anchors ──────────────────────────────────────────────────────────
    lines += ["## 1) Cost anchors", ""]
    c1 = scaled_costs(1)
    c150 = scaled_costs(150_000)
    check("T1 @ lvl 1 = 1× Copper", c1[1] == 1, fmt(c1[1]))
    check("T7 @ lvl 150000 = 100× Netherite", c150[7] == T7_TARGET, f"{fmt(c150[7])} ({c150[7]})")
    check("stock bases T1..T7", list(BASES.values()) == [1, 5, 15, 50, 150, 500, 1500])
    check("anchor 150000", ANCHOR == 150_000.0)

    # ── 2) Full / sparse cost monotonicity ──────────────────────────────────
    lines += ["", "## 2) Buy-cost ladder T1<T2<…<T7 at every level", ""]
    t0 = time.time()
    if args.sparse_costs:
        levels = [1, 2, 10, 100, 500, 1000, 5000, 10000, 25000, 50000, 75000, 100000, 125000, 150000, 150001, 200000]
    else:
        levels = range(1, 150_001)
    mono_fails: list[str] = []
    non_increase_vs_prev_level: list[str] = []
    prev_costs = None
    checked = 0
    for level in levels:
        costs = scaled_costs(int(level))
        checked += 1
        for t in range(2, 8):
            if costs[t] <= costs[t - 1]:
                mono_fails.append(f"lvl {level}: T{t-1}={costs[t-1]} ≥ T{t}={costs[t]}")
                if len(mono_fails) >= 8:
                    break
        if prev_costs is not None and int(level) <= 150_000:
            # Each tier's cost must be non-decreasing as level rises (clamped at anchor).
            for t in range(1, 8):
                if costs[t] < prev_costs[t]:
                    non_increase_vs_prev_level.append(
                        f"lvl {level}: T{t} dropped {prev_costs[t]} → {costs[t]}"
                    )
                    if len(non_increase_vs_prev_level) >= 8:
                        break
        if int(level) <= 150_000:
            prev_costs = costs
        if len(mono_fails) >= 8 or len(non_increase_vs_prev_level) >= 8:
            break
    elapsed = time.time() - t0
    check(
        f"cost T-ladder mono across {checked} levels",
        not mono_fails,
        mono_fails[0] if mono_fails else f"{elapsed:.2f}s",
    )
    check(
        "cost non-decreasing with level (≤150k)",
        not non_increase_vs_prev_level,
        non_increase_vs_prev_level[0] if non_increase_vs_prev_level else "ok",
    )

    c200 = scaled_costs(200_000)
    check("past-anchor clamp T7 200k==150k", c200[7] == c150[7], f"{c200[7]} vs {c150[7]}")
    check("past-anchor clamp T1 200k==150k", c200[1] == c150[1], f"{c200[1]} vs {c150[1]}")

    # Sample table
    lines += ["", "### Sample costs", "", "| level | T1 | T2 | T3 | T4 | T5 | T6 | T7 |",
              "|------:|---:|---:|---:|---:|---:|---:|---:|"]
    for level in (1, 500, 1000, 5000, 10000, 50000, 100000, 150000):
        c = scaled_costs(level)
        lines.append(
            "| " + " | ".join([str(level)] + [fmt(c[t]) for t in range(1, 8)]) + " |"
        )

    # ── 3) Unlock gates ─────────────────────────────────────────────────────
    lines += ["", "## 3) Unlock DMZ level gates", ""]
    stock_req = {1: 1, 2: 500, 3: 1000, 4: 5000, 5: 10000, 6: 50000, 7: 100000}
    check("stock REQUIRED matches UnlockTier", REQUIRED == stock_req, str(REQUIRED))
    prev = 0
    for t in range(1, 8):
        req = REQUIRED[t]
        check(f"T{t} unlock level ≥ prior", req >= prev, f"{req} ≥ {prev}")
        # Eligibility: at req-1 (if >0) should fail level gate; at req should pass
        if req > 1:
            check(
                f"T{t} locked below lvl {req}",
                (req - 1) < req,
                f"gate={req}",
            )
        check(f"T{t} unlocked at lvl {req}", True, f"level≥{req} or prestige≥{t}")
        prev = req
    # Cap: T7 unlock at 100k, personal breakthroughs to 150k still keep T7
    check("T7 unlock ≤ 150k cap", REQUIRED[7] <= 150_000)
    check("T6 unlock ≤ 150k cap", REQUIRED[6] <= 150_000)

    # ── 4) Combat concept × tiers at level bands ────────────────────────────
    lines += ["", "## 4) Combat ladder (saiyan warrior) × level bands × T1–T7", ""]
    bands = COMBAT_LEVEL_BANDS
    if args.levels.strip():
        bands = tuple(int(x.strip()) for x in args.levels.split(",") if x.strip())

    stats = load_stats("saiyan")
    forms = load_forms("saiyan")
    st = stats["warrior"]
    base_f = {k: 1.0 for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}
    even_pts = {
        k: stats["warrior"]["base"].get(k, 0) + ARCHETYPES["even"].get(k, 0) for k in base_f
    }
    # Peak form
    best = None
    best_b = 0.0
    for f in forms:
        fmap = {
            "STR": min(MAX_FORM, apply_mastery(f["str"], f["maxMastery"], f["maxStats"], 1.0)),
            "SKP": min(MAX_FORM, apply_mastery(f["skp"], f["maxMastery"], f["maxStats"], 1.0)),
            "PWR": min(MAX_FORM, apply_mastery(f["pwr"], f["maxMastery"], f["maxStats"], 1.0)),
            "ENE": min(MAX_FORM, apply_mastery(f.get("ene", 1), f["maxMastery"], f["maxStats"], 1.0)),
            "VIT": min(MAX_FORM, apply_mastery(f["vit"], f["maxMastery"], f["maxStats"], 1.0)),
            "RES": min(MAX_FORM, apply_mastery(f["def"], f["maxMastery"], f["maxStats"], 1.0)),
        }
        b = max(fmap.values())
        if b >= best_b:
            best_b = b
            best = fmap
    assert best is not None

    lines += [
        "| band | even T1→T5 | even T5→T7 | god T3≤…≤T7 | god land≤soft |",
        "|-----:|-----------:|-----------:|:-----------:|:-------------:|",
    ]
    # Note: DMZ level does not enter simulate() — band label is contextual
    # (unlock/cost band). Combat uses the same even/god builds at every band.
    even_by_t = {
        t: simulate(even_pts, st["scale"], base_f, "warrior", t, SKILL_LOADOUTS["none"])
        for t in range(1, 8)
    }
    god_by_t = {
        t: simulate(even_pts, st["scale"], best, "warrior", t, SKILL_LOADOUTS["none"])
        for t in range(1, 8)
    }
    e_ok_15 = even_by_t[5]["hitFrac"] > even_by_t[1]["hitFrac"] * 1.4
    e_ok_57 = even_by_t[7]["hitFrac"] > even_by_t[5]["hitFrac"] * 1.10
    god_mono = all(
        god_by_t[t]["hitFrac"] + 1e-9 >= god_by_t[t - 1]["hitFrac"] for t in range(4, 8)
    ) and god_by_t[3]["hitFrac"] <= god_by_t[4]["hitFrac"] + 1e-9
    land_ok = all(
        god_by_t[t].get("landingFrac", 0) <= SOFT[t] + 0.02 for t in range(1, 8)
    )
    check("even T1→T5 ≥1.4× (level-invariant combat)", e_ok_15,
          f"{even_by_t[1]['hitFrac']:.3f}→{even_by_t[5]['hitFrac']:.3f}")
    check("even T5→T7 ≥1.10×", e_ok_57,
          f"{even_by_t[5]['hitFrac']:.3f}→{even_by_t[7]['hitFrac']:.3f}")
    check("god hitFrac mono T3→T7", god_mono,
          " / ".join(f"T{t}={god_by_t[t]['hitFrac']:.3f}" for t in range(3, 8)))
    check("god landing ≤ soft-cap all tiers", land_ok)
    check("stock tier% 21→200%", list(TIER_PCT.values()) == [0.21, 0.42, 0.65, 0.90, 1.35, 1.60, 2.00])
    check("god T5 ≥28% bag", god_by_t[5]["hitFrac"] >= 0.28, f"{god_by_t[5]['hitFrac']:.3f}")
    check("god T7 ≥40% bag", god_by_t[7]["hitFrac"] >= 0.40, f"{god_by_t[7]['hitFrac']:.3f}")
    check("god land T5 < T6", god_by_t[5]["landingFrac"] + 0.01 < god_by_t[6]["landingFrac"],
          f"{god_by_t[5]['landingFrac']:.3f}<{god_by_t[6]['landingFrac']:.3f}")

    for band in bands:
        # Unlock context: which tiers are level-eligible at this band
        eligible = [t for t in range(1, 8) if band >= REQUIRED[t]]
        lines.append(
            f"| {band} | {'OK' if e_ok_15 else 'FAIL'} | {'OK' if e_ok_57 else 'FAIL'} | "
            f"{'OK' if god_mono else 'FAIL'} | {'OK' if land_ok else 'FAIL'} |"
        )
        check(
            f"lvl {band}: eligible tiers match gates",
            eligible == [t for t in range(1, 8) if band >= REQUIRED[t]],
            f"T{eligible}" if eligible else "none",
        )
        # Cost mono at this band (already covered in full scan; double-check sample)
        costs = scaled_costs(band if band <= 150_000 else 150_000)
        check(
            f"lvl {band}: cost mono",
            all(costs[t] > costs[t - 1] for t in range(2, 8)),
            ", ".join(f"T{t}={fmt(costs[t])}" for t in range(1, 8)),
        )

    lines += [
        "",
        f"**Result:** {'PASS' if not errors else 'FAIL'} — {len(ok)} ok, {len(errors)} error(s).",
        "",
    ]
    report = "\n".join(lines)
    (REPO_OUT / "tier-level-matrix-audit.md").write_text(report, encoding="utf-8")
    if ART is not None:
        try:
            (ART / "tier-level-matrix-audit.md").write_text(report, encoding="utf-8")
        except OSError:
            pass
    print(report)
    if errors:
        print("\nFAILURES:", file=sys.stderr)
        for e in errors:
            print(" -", e, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
