#!/usr/bin/env python3
"""Check whether live telemetry is sufficient to retune post-deploy.

Filters hits to:
  - timestamps on/after a deploy cutoff (default: 2.3.180 upload)
  - divine + apex form bands (formBoost >= 22) — god-line retune target

Usage:
  python3 audit_retune_readiness.py --dir uploads/live-pull-2026-09-05/telemetry
  python3 audit_retune_readiness.py --dir uploads/live-pull-2026-09-05/telemetry --since 2026-09-04T14:43:00
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from scaling_constants import CONCEPT_EVEN_HITFRAC_MIN, band_form

DEFAULT_SINCE = "2026-09-04T14:43:00"
RETUNE_TIERS = (5, 6, 7)
MIN_HITS = 200
MIN_PLAYERS = 20


def load_hits(dir_path: Path) -> list[dict]:
    rows: list[dict] = []
    for path in sorted(dir_path.glob("hits-*.jsonl")):
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            line = line.strip()
            if not line:
                continue
            try:
                rows.append(json.loads(line))
            except json.JSONDecodeError:
                continue
    return rows


def is_god_line(row: dict) -> bool:
    band = band_form(float(row.get("formBoost") or 1))
    return band in ("divine", "apex")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--dir", type=Path, required=True, help="Telemetry folder with hits-*.jsonl")
    ap.add_argument("--since", default=DEFAULT_SINCE, help="ISO timestamp cutoff (inclusive)")
    ap.add_argument("--min-hits", type=int, default=MIN_HITS, help="Min divine+ hits per retune tier")
    ap.add_argument("--min-players", type=int, default=MIN_PLAYERS, help="Min unique players in window")
    args = ap.parse_args()

    if not args.dir.is_dir():
        print(f"MISSING dir: {args.dir}", file=sys.stderr)
        return 2

    all_rows = load_hits(args.dir)
    post = [r for r in all_rows if (r.get("ts") or "") >= args.since]
    god = [r for r in post if is_god_line(r)]
    players = {r.get("player") for r in god if r.get("player")}

    by_tier: dict[int, list[float]] = defaultdict(list)
    for r in god:
        tier = int(r.get("tier") or 0)
        if tier > 0:
            by_tier[tier].append(float(r.get("hitFracPost") or 0))

    print(f"# Retune readiness — {args.dir}")
    print(f"Deploy cutoff: {args.since}Z")
    print(f"Post-deploy hits (all): {len(post)} | divine+ apex: {len(god)} | players: {len(players)}")
    print(f"Thresholds: >= {args.min_hits} divine+ hits/tier for T{RETUNE_TIERS}, >= {args.min_players} players\n")

    errors: list[str] = []
    warns: list[str] = []

    if len(players) < args.min_players:
        errors.append(f"players {len(players)} < {args.min_players}")

    print("| Tier | divine+ n | avg hitFracPost | concept min | sample | pressure |")
    print("|-----:|----------:|----------------:|------------:|:------:|:--------:|")
    for tier in RETUNE_TIERS:
        hits = by_tier.get(tier, [])
        n = len(hits)
        avg = sum(hits) / n if n else 0.0
        concept = CONCEPT_EVEN_HITFRAC_MIN.get(tier)
        concept_s = f"{concept:.2f}" if concept is not None else "—"
        sample = "OK" if n >= args.min_hits else "LOW"
        if n < args.min_hits:
            errors.append(f"T{tier} sample {n} < {args.min_hits}")
        pressure = "—"
        if concept is not None and n > 0:
            if avg >= concept:
                pressure = "OK"
            elif avg >= concept * 0.9:
                pressure = "WATCH"
                warns.append(f"T{tier} avg {avg:.3f} slightly under concept {concept:.2f}")
            else:
                pressure = "LOW"
                warns.append(f"T{tier} avg {avg:.3f} under concept {concept:.2f}")
        print(f"| T{tier} | {n} | {avg:.3f} | {concept_s} | {sample} | {pressure} |")

    print()
    if errors:
        print("NOT READY — fix sample gaps before retuning:")
        for e in errors:
            print(f"  - {e}")
    else:
        print("SAMPLE READY — enough divine+ hits per tier (review pressure column before changing constants).")

    if warns:
        print("\nPressure notes (informational):")
        for w in warns:
            print(f"  - {w}")

    if errors:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
