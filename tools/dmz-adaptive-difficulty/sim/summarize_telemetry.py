#!/usr/bin/env python3
"""Summarize AdaptiveDifficulty whitelist combat telemetry JSONL.

Usage:
  python3 summarize_telemetry.py [path/to/hits-YYYY-MM-DD.jsonl ...]
  python3 summarize_telemetry.py --dir /path/to/config/adaptivedifficulty/telemetry

Reads hit lines logged by BalanceTelemetry and prints cancel rates / pressure
by tier, formBoost band, race, and class — for live balance tuning.
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from pathlib import Path


def load_rows(paths: list[Path]) -> list[dict]:
    rows: list[dict] = []
    for p in paths:
        if not p.is_file():
            print(f"skip missing: {p}", file=sys.stderr)
            continue
        for line in p.read_text(encoding="utf-8", errors="replace").splitlines():
            line = line.strip()
            if not line:
                continue
            try:
                rows.append(json.loads(line))
            except json.JSONDecodeError:
                continue
    return rows


def band_form(fb: float) -> str:
    if fb <= 1.12:
        return "base"
    if fb < 6:
        return "mid"
    if fb < 25:
        return "high"
    return "god"


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("files", nargs="*", type=Path, help="JSONL hit logs")
    ap.add_argument("--dir", type=Path, help="Directory of hits-*.jsonl files")
    args = ap.parse_args()
    paths = list(args.files)
    if args.dir:
        paths.extend(sorted(args.dir.glob("hits-*.jsonl")))
    if not paths:
        print("No files. Pass JSONL paths or --dir config/adaptivedifficulty/telemetry", file=sys.stderr)
        return 2

    rows = load_rows(paths)
    if not rows:
        print("No rows parsed.")
        return 1

    print(f"# Telemetry summary — {len(rows)} hits from {len(paths)} file(s)\n")

    def bucket(key_fn):
        agg: dict[str, dict] = defaultdict(lambda: {
            "n": 0, "cancel": 0, "would": 0, "pre0": 0,
            "hitPost": 0.0, "hitPre": 0.0,
        })
        for r in rows:
            k = key_fn(r)
            a = agg[k]
            a["n"] += 1
            if r.get("cancelled"):
                a["cancel"] += 1
            if r.get("wouldCancel"):
                a["would"] += 1
            if float(r.get("preDmg") or 0) <= 0:
                a["pre0"] += 1
            a["hitPost"] += float(r.get("hitFracPost") or 0)
            a["hitPre"] += float(r.get("hitFracPre") or 0)
        return agg

    def show(title: str, agg: dict[str, dict]) -> None:
        print(f"## {title}")
        print("| key | n | cancelled% | wouldCancel% | preDmg=0% | avg hitFracPost |")
        print("|-----|--:|----------:|-------------:|----------:|----------------:|")
        for k, a in sorted(agg.items(), key=lambda kv: (-kv[1]["n"], kv[0])):
            n = max(1, a["n"])
            print(
                f"| {k} | {a['n']} | {100*a['cancel']/n:.1f}% | {100*a['would']/n:.1f}% | "
                f"{100*a['pre0']/n:.1f}% | {a['hitPost']/n:.3f} |"
            )
        print()

    show("By tier", bucket(lambda r: f"T{r.get('tier', '?')}"))
    show("By form band", bucket(lambda r: band_form(float(r.get("formBoost") or 1))))
    show("By race", bucket(lambda r: str(r.get("race") or "?")))
    show("By class", bucket(lambda r: str(r.get("class") or "?")))
    show(
        "By player",
        bucket(lambda r: str(r.get("player") or r.get("uuid") or "?")),
    )

    # Worst cancel offenders
    bad = [r for r in rows if r.get("cancelled") or r.get("wouldCancel") or float(r.get("preDmg") or 0) <= 0]
    print(f"## Flags — {len(bad)} / {len(rows)} hits cancelled or wouldCancel or preDmg=0")
    if bad:
        print("| player | tier | form | flatMit | paintedAtk | pre | post | mob |")
        print("|--------|-----:|-----:|--------:|-----------:|----:|-----:|-----|")
        for r in bad[:25]:
            print(
                f"| {r.get('player')} | {r.get('tier')} | {r.get('formBoost')} | "
                f"{r.get('flatMit')} | {r.get('paintedAtk')} | {r.get('preDmg')} | "
                f"{r.get('postDmg')} | {r.get('mob')} |"
            )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
