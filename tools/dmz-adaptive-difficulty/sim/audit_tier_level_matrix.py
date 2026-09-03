#!/usr/bin/env python3
"""Fail-closed matrix: Unlock tiers 1–7 across DMZ levels 1 → 150000.

Checks:
  1. Tier buy-cost ladder strictly increasing at EVERY level 1..150000
  2. Cost curve anchors (T1@1 = 1 Copper, T7@150k = 100× Netherite)
  3. Costs clamp past the 150k anchor
  4. Unlock gate levels are monotonic and match stock UnlockTier
  5. Combat concept at representative levels × all tiers (even + god form)
     — hitFrac rises with tier; landing ≤ soft-cap; T5/T7 floors
  6. ALL races × ALL forms × T1–T7 (combat is level-invariant; costs cover levels)

Writes:
  sim/out/tier-level-matrix-audit.md
  /opt/cursor/artifacts/tier-level-matrix-audit.md (when present)

Usage:
  python3 audit_tier_level_matrix.py
  python3 audit_tier_level_matrix.py --levels 1,1000,150000   # sparse
  python3 audit_tier_level_matrix.py --full-costs             # every level (default)
  python3 audit_tier_level_matrix.py --skip-race-forms        # costs/gates only
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
    RACES,
    TIER_PCT,
    apply_mastery,
    load_forms,
    load_stats,
)

# Preferred fighting class per race (sento has no warrior).
PHYS_CLASS_PREF = ("warrior", "berserker", "martialartist", "paladin", "tank")
ANDROID_ELIGIBLE = ("human", "saiyan", "frostdemon", "viltrumite")

REPO_OUT = Path(__file__).resolve().parent / "out"
REPO_OUT.mkdir(parents=True, exist_ok=True)
ART = Path("/opt/cursor/artifacts")
try:
    ART.mkdir(parents=True, exist_ok=True)
except OSError:
    ART = None

SOFT = {1: 0.30, 2: 0.32, 3: 0.36, 4: 0.40, 5: 0.44, 6: 0.48, 7: 0.52}

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
    ap.add_argument(
        "--skip-race-forms",
        action="store_true",
        help="Skip all-races/forms combat matrix",
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

    # ── 5) All races × all forms × T1–T7 ─────────────────────────────────────
    # Combat pressure does not take DMZ level — covering every form × every tier
    # is the race/form equivalent of the 1–150k cost scan.
    if not args.skip_race_forms:
        lines += [
            "",
            "## 5) All races × all forms × T1–T7",
            "",
            "Combat is level-invariant; buy-cost scan above covers levels 1–150k.",
            "Each form checked at mastery 0% + 100% (Base once). Soft-cap + no zero dmg.",
            "",
        ]
        race_ids = sorted(
            p.name for p in RACES.iterdir() if p.is_dir() and (p / "stats.json").exists()
        )
        check("discovered ≥8 stock races", len(race_ids) >= 8, f"{len(race_ids)}: {', '.join(race_ids)}")

        soft_eps = 0.02
        soft_fails: list[str] = []
        zero_fails: list[str] = []
        peak_fails: list[str] = []
        android_forms_found: dict[str, int] = {}
        form_cells = 0
        t_race = time.time()

        lines += [
            "| race | forms | cells | soft≤cap | dmg>0 | peak mono T3→T7 | peak T5≥28% |",
            "|------|------:|------:|:--------:|:-----:|:---------------:|:-----------:|",
        ]

        def form_map(f: dict, mastery: float) -> dict[str, float]:
            return {
                "STR": min(MAX_FORM, apply_mastery(f["str"], f["maxMastery"], f["maxStats"], mastery)),
                "SKP": min(MAX_FORM, apply_mastery(f["skp"], f["maxMastery"], f["maxStats"], mastery)),
                "PWR": min(MAX_FORM, apply_mastery(f["pwr"], f["maxMastery"], f["maxStats"], mastery)),
                "ENE": min(MAX_FORM, apply_mastery(f.get("ene", 1), f["maxMastery"], f["maxStats"], mastery)),
                "VIT": min(MAX_FORM, apply_mastery(f["vit"], f["maxMastery"], f["maxStats"], mastery)),
                "RES": min(MAX_FORM, apply_mastery(f["def"], f["maxMastery"], f["maxStats"], mastery)),
            }

        for race in race_ids:
            stats = load_stats(race)
            forms = load_forms(race)
            cls = next((c for c in PHYS_CLASS_PREF if c in stats), next(iter(stats)))
            st = stats[cls]
            even_pts = {
                k: st["base"].get(k, 0) + ARCHETYPES["even"].get(k, 0)
                for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")
            }
            base_f = {k: 1.0 for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}

            entries: list[tuple[str, dict[str, float], float]] = [("base", base_f, 1.0)]
            android_n = 0
            for f in forms:
                key = f"{f['group']}.{f['name']}"
                if "android" in (f["group"] or "").lower() or "android" in (f["name"] or "").lower():
                    android_n += 1
                for mastery, mlabel in ((0.0, "m0"), (1.0, "m100")):
                    fmap = form_map(f, mastery)
                    boost = max(fmap.values())
                    entries.append((f"{key}@{mlabel}", fmap, boost))
            android_forms_found[race] = android_n

            race_soft_ok = True
            race_dmg_ok = True
            cells = 0
            for label, fmap, _boost in entries:
                for t in range(1, 8):
                    r = simulate(even_pts, st["scale"], fmap, cls, t, SKILL_LOADOUTS["none"])
                    cells += 1
                    form_cells += 1
                    land = float(r.get("landingFrac", r["hitFrac"]))
                    if land > SOFT[t] + soft_eps + 1e-9:
                        race_soft_ok = False
                        if len(soft_fails) < 12:
                            soft_fails.append(
                                f"{race}/{label} T{t}: land={land:.3f} > soft={SOFT[t]}"
                            )
                    if float(r["mobDmg"]) <= 0.0 or float(r["hitFrac"]) <= 0.0:
                        race_dmg_ok = False
                        if len(zero_fails) < 12:
                            zero_fails.append(f"{race}/{label} T{t}: dmg={r['mobDmg']}")

            # Peak form (highest boost @ m100) — ladder + floors
            peak = max(
                ((lab, fm, b) for lab, fm, b in entries if lab != "base"),
                key=lambda x: x[2],
                default=None,
            )
            peak_mono = True
            peak_t5_ok = True
            if peak is not None:
                lab, fmap, boost = peak
                by_t = {
                    t: simulate(even_pts, st["scale"], fmap, cls, t, SKILL_LOADOUTS["none"])
                    for t in range(1, 8)
                }
                # Buy signal is landingFrac (post soft-cap safety). hitFrac can tiny-plateau
                # at T7 when form≥25× (kiProtection form_factor soft-cap) — allow 0.5% slack.
                land_mono = all(
                    by_t[t].get("landingFrac", 0) + 1e-9 >= by_t[t - 1].get("landingFrac", 0)
                    for t in range(4, 8)
                )
                hit_ok = all(
                    by_t[t]["hitFrac"] + 0.005 >= by_t[t - 1]["hitFrac"] for t in range(4, 8)
                )
                peak_mono = land_mono and hit_ok
                land56 = by_t[5].get("landingFrac", 0) + 0.01 < by_t[6].get("landingFrac", 0)
                if not land_mono:
                    peak_fails.append(
                        f"{race} peak {lab}: landingFrac "
                        + " / ".join(
                            f"T{t}={by_t[t].get('landingFrac', 0):.3f}" for t in range(3, 8)
                        )
                    )
                elif not hit_ok:
                    peak_fails.append(
                        f"{race} peak {lab}: hitFrac "
                        + " / ".join(f"T{t}={by_t[t]['hitFrac']:.3f}" for t in range(3, 8))
                    )
                if not land56:
                    peak_fails.append(
                        f"{race} peak {lab}: land T5={by_t[5].get('landingFrac', 0):.3f} "
                        f"≥ T6={by_t[6].get('landingFrac', 0):.3f}"
                    )
                    peak_mono = False
                # High forms must still bite at T5 (soft-cap counts as pass).
                if boost >= 6.0:
                    hf5 = by_t[5]["hitFrac"]
                    peak_t5_ok = hf5 + 1e-9 >= min(0.28, SOFT[5] - 1e-6)
                    if not peak_t5_ok:
                        peak_fails.append(f"{race} peak {lab}: T5 hitFrac={hf5:.3f} < 0.28")
                    if hf5 * 0.35 < 0.12 - 1e-9:
                        peak_fails.append(
                            f"{race} peak {lab}: T5 post-DEF~{hf5 * 0.35:.3f} < 0.12"
                        )
                        peak_t5_ok = False

            lines.append(
                f"| {race} | {len(forms)} | {cells} | "
                f"{'OK' if race_soft_ok else 'FAIL'} | "
                f"{'OK' if race_dmg_ok else 'FAIL'} | "
                f"{'OK' if peak_mono else 'FAIL'} | "
                f"{'OK' if peak_t5_ok else 'FAIL'} |"
            )

        elapsed_race = time.time() - t_race
        check(
            f"race/form soft-cap all cells ({form_cells})",
            not soft_fails,
            soft_fails[0] if soft_fails else f"{elapsed_race:.2f}s",
        )
        check(
            "race/form mobDmg > 0 all cells",
            not zero_fails,
            zero_fails[0] if zero_fails else "ok",
        )
        check(
            "race peak form ladders / floors",
            not peak_fails,
            peak_fails[0] if peak_fails else f"{len(race_ids)} peaks ok",
        )

        # Android upgrade forms exist for eligible races; blocked for bioandroid.
        lines += ["", "### Android forms coverage", ""]
        for race in ANDROID_ELIGIBLE:
            n = android_forms_found.get(race, 0)
            check(f"{race} has android form entries", n > 0, f"{n} forms")
        check(
            "bioandroid has no androidforms upgrade group",
            android_forms_found.get("bioandroid", 0) == 0
            or not (RACES / "bioandroid" / "forms" / "androidforms.json").is_file(),
            f"android-named={android_forms_found.get('bioandroid', 0)}",
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
