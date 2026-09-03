#!/usr/bin/env python3
"""Compare live telemetry to concept targets and 2.3.161 sim predictions.

Usage:
  python3 retune_from_telemetry.py --dir uploads/live-telemetry-2026-09-03-full
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from datetime import datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from scaling_constants import CONCEPT_EVEN_HITFRAC_MIN, SOFT_CAP, LAND_FRAC
from simulate_build_matrix import ARCHETYPES, SKILL_LOADOUTS, simulate
from simulate_race_forms import load_stats

OUT = Path(__file__).resolve().parent / "out"
OUT.mkdir(parents=True, exist_ok=True)


def band_form(fb: float) -> str:
    if fb <= 1.12:
        return "base"
    if fb < 6:
        return "mid"
    if fb < 25:
        return "high"
    return "god"


def load_rows(paths: list[Path]) -> list[dict]:
    rows: list[dict] = []
    for p in paths:
        for line in p.read_text(encoding="utf-8", errors="replace").splitlines():
            line = line.strip()
            if not line:
                continue
            try:
                rows.append(json.loads(line))
            except json.JSONDecodeError:
                continue
    return rows


def agg_by_tier(rows: list[dict], *, form_band: str | None = None) -> dict[int, dict]:
    out: dict[int, dict] = defaultdict(lambda: {
        "n": 0, "hit_post": 0.0, "hit_pre": 0.0, "above_soft": 0, "landing_n": 0,
    })
    for r in rows:
        if form_band and band_form(float(r.get("formBoost") or 1)) != form_band:
            continue
        t = int(r.get("tier") or 0)
        if t <= 0:
            continue
        a = out[t]
        a["n"] += 1
        hp = float(r.get("hitFracPost") or 0)
        a["hit_post"] += hp
        a["hit_pre"] += float(r.get("hitFracPre") or 0)
        if hp > SOFT_CAP.get(t, 1.0) + 0.02:
            a["above_soft"] += 1
        if r.get("landing"):
            a["landing_n"] += 1
    return dict(out)


def sim_even_bands(dmz_level: int = 5500) -> dict[int, float]:
    stats = load_stats("saiyan")["warrior"]
    base = {k: 1.0 for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}
    return {
        t: simulate(ARCHETYPES["even"], stats["scale"], base, "warrior", t, SKILL_LOADOUTS["none"], dmz_level=dmz_level)["hitFrac"]
        for t in range(1, 8)
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", type=Path, required=True)
    ap.add_argument("--recent-days", type=int, default=3, help="Highlight last N days of files")
    args = ap.parse_args()
    paths = sorted(args.dir.glob("hits-*.jsonl"))
    if not paths:
        print(f"No hits in {args.dir}", file=sys.stderr)
        return 2

    rows = load_rows(paths)
    recent_cutoff = None
    if args.recent_days > 0:
        dates = []
        for p in paths:
            stem = p.stem
            if stem.startswith("hits-"):
                try:
                    dates.append(datetime.strptime(stem[5:], "%Y-%m-%d").date())
                except ValueError:
                    pass
        if dates:
            recent_cutoff = max(dates)
            from datetime import timedelta
            cutoff = recent_cutoff - timedelta(days=args.recent_days - 1)
            recent_paths = [p for p in paths if p.stem[5:] >= cutoff.isoformat()]
            recent_rows = load_rows(recent_paths)
        else:
            recent_rows = rows
    else:
        recent_rows = rows

    lines = [
        f"# Telemetry retune report — {len(rows)} hits ({len(paths)} files)",
        "",
        "## Live aggregate (all hits) — avg hitFracPost by tier",
        "",
        "| Tier | N | Avg post | Avg pre | >soft-cap | soft-cap | concept min |",
        "|-----:|--:|---------:|--------:|----------:|---------:|------------:|",
    ]

    all_agg = agg_by_tier(rows)
    god_agg = agg_by_tier(rows, form_band="god")
    sim = sim_even_bands(5500)
    sim_100k = sim_even_bands(100000)

    for t in range(1, 8):
        a = all_agg.get(t, {"n": 0, "hit_post": 0, "hit_pre": 0, "above_soft": 0})
        n = a["n"]
        avg = a["hit_post"] / n if n else 0
        pre = a["hit_pre"] / n if n else 0
        above = 100.0 * a["above_soft"] / n if n else 0
        concept = CONCEPT_EVEN_HITFRAC_MIN.get(t, "—")
        lines.append(
            f"| T{t} | {n} | {avg:.3f} | {pre:.3f} | {above:.1f}% | {SOFT_CAP[t]:.2f} | {concept} |"
        )

    lines += [
        "",
        "## God-form band (formBoost ≥ 25)",
        "",
        "| Tier | N | Avg post | soft-cap | step vs prev |",
        "|-----:|--:|---------:|---------:|-------------:|",
    ]
    prev = None
    for t in range(1, 8):
        a = god_agg.get(t, {"n": 0, "hit_post": 0})
        n = a["n"]
        avg = a["hit_post"] / n if n else 0
        step = f"{avg / prev:.2f}×" if prev and prev > 0 and n else "—"
        lines.append(f"| T{t} | {n} | {avg:.3f} | {SOFT_CAP[t]:.2f} | {step} |")
        if n and avg > 0:
            prev = avg

    lines += [
        "",
        "## 2.3.161 sim (saiyan even, no skills) vs live god band",
        "",
        "| Tier | Live god avg | Sim 5.5k | Sim 100k | Concept min |",
        "|-----:|-------------:|---------:|---------:|------------:|",
    ]
    for t in (1, 3, 5, 7):
        a = god_agg.get(t, {"n": 0, "hit_post": 0})
        live = a["hit_post"] / a["n"] if a["n"] else 0
        concept = CONCEPT_EVEN_HITFRAC_MIN.get(t, "—")
        lines.append(
            f"| T{t} | {live:.3f} | {sim.get(t, 0):.3f} | {sim_100k.get(t, 0):.3f} | {concept} |"
        )

    lines += [
        "",
        "## Recent window",
        f"Rows: {len(recent_rows)} (last {args.recent_days} day file(s))",
        "",
    ]
    recent_god = agg_by_tier(recent_rows, form_band="god")
    for t in (3, 5, 7):
        a = recent_god.get(t, {"n": 0, "hit_post": 0})
        if a["n"]:
            lines.append(f"- T{t} god recent avg post: **{a['hit_post']/a['n']:.3f}** (n={a['n']})")

    lines += [
        "",
        "## Retune guidance",
        "",
        "- **Live** still reflects **2.3.160** inflated constants until 2.3.161 deploys.",
        "- Target sim bands (rollback): even T5≈0.45, T7≈0.58 at gate; veterans at 100k get paintEase relief.",
        "- God-band live avg should climb monotonically T1→T7 and stay ≤ soft-cap +2%.",
        "- If live god T3≫T2 step after deploy, check paintEase DMZ gates (not difficulty slider max).",
        "",
        f"landFrac ladder: {' < '.join(f'T{t}={LAND_FRAC[t]:.2f}' for t in range(4, 8))}",
    ]

    report = "\n".join(lines)
    out_path = OUT / "telemetry-retune-report.md"
    out_path.write_text(report)
    print(report)
    print(f"\nWrote {out_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
