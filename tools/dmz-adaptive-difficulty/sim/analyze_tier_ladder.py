#!/usr/bin/env python3
"""Analyze live telemetry tier ladder vs concept expectations.

Checks:
  - Monotonic avg hitFracPost T1→T7 (per form band)
  - Per-tier vs concept soft-cap ceilings
  - Tier-to-tier step ratios
  - T3 anomaly vs neighbors
  - Recent (last 2 days) vs older aggregate
"""
from __future__ import annotations

import json
import sys
from collections import defaultdict
from datetime import date, datetime
from pathlib import Path

from scaling_constants import FORM_BAND_ORDER, SOFT_CAP, band_form

# Expected even-build hitFrac bands from sim (saiyan warrior, no skills, 2.3.161)
SIM_EVEN = {1: 0.129, 3: 0.353, 5: 0.447, 7: 0.583}


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


def tier_stats(rows: list[dict], *, form_band: str | None = None) -> dict[int, dict]:
    agg: dict[int, dict] = defaultdict(lambda: {
        "n": 0, "hit_post": 0.0, "hit_pre": 0.0, "landing": 0.0,
        "cancel": 0, "would": 0, "above_soft": 0,
    })
    for r in rows:
        if form_band and band_form(float(r.get("formBoost") or 1)) != form_band:
            continue
        t = int(r.get("tier") or 0)
        if t <= 0:
            continue
        a = agg[t]
        a["n"] += 1
        hp = float(r.get("hitFracPost") or 0)
        a["hit_post"] += hp
        a["hit_pre"] += float(r.get("hitFracPre") or 0)
        if r.get("landing"):
            a["landing"] += 1
        if r.get("cancelled"):
            a["cancel"] += 1
        if r.get("wouldCancel"):
            a["would"] += 1
        cap = SOFT_CAP.get(t, 1.0)
        if hp > cap + 0.02:
            a["above_soft"] += 1
    return dict(agg)


def file_date(p: Path) -> date | None:
    # hits-2026-09-03.jsonl
    stem = p.stem
    if stem.startswith("hits-"):
        try:
            return datetime.strptime(stem[5:], "%Y-%m-%d").date()
        except ValueError:
            return None
    return None


def main() -> int:
    if len(sys.argv) < 2:
        print("Usage: analyze_tier_ladder.py <telemetry-dir>", file=sys.stderr)
        return 2
    d = Path(sys.argv[1])
    paths = sorted(d.glob("hits-*.jsonl"))
    if not paths:
        print(f"No hits-*.jsonl in {d}", file=sys.stderr)
        return 2

    all_rows = load_rows(paths)
    recent_cut = date(2026, 9, 2)
    recent_paths = [p for p in paths if (fd := file_date(p)) and fd >= recent_cut]
    recent_rows = load_rows(recent_paths) if recent_paths else []

    lines = [
        f"# Tier ladder analysis — {len(all_rows)} hits ({len(paths)} files)",
        f"Recent slice: {len(recent_rows)} hits from {len(recent_paths)} file(s) since {recent_cut}",
        "",
    ]
    errors: list[str] = []

    def section(title: str, rows: list[dict]) -> None:
        lines.append(f"## {title}")
        lines.append("")
        lines.append("| tier | n | avg hitPost | p90 hitPost | cancel% | would% | >softCap% | landing% |")
        lines.append("|-----|--:|------------:|------------:|--------:|-------:|----------:|---------:|")

        by_tier = tier_stats(rows)
        tiers = sorted(by_tier)
        prev_avg = None
        for t in tiers:
            a = by_tier[t]
            n = a["n"]
            if n == 0:
                continue
            avg = a["hit_post"] / n
            # p90 approx via sort subset — cheap for telemetry size
            hits = sorted(
                float(r.get("hitFracPost") or 0)
                for r in rows
                if int(r.get("tier") or 0) == t
            )
            p90 = hits[int(len(hits) * 0.9)] if hits else 0.0
            lines.append(
                f"| T{t} | {n} | {avg:.3f} | {p90:.3f} | "
                f"{100*a['cancel']/n:.1f}% | {100*a['would']/n:.1f}% | "
                f"{100*a['above_soft']/n:.1f}% | {100*a['landing']/n:.1f}% |"
            )
            if prev_avg is not None and avg + 0.015 < prev_avg:
                errors.append(f"{title}: T{t} avg {avg:.3f} < prior {prev_avg:.3f} (non-monotonic)")
            prev_avg = avg
        lines.append("")

    section("All hits", all_rows)
    section("Ultra band (fb 15–22)", [r for r in all_rows if band_form(float(r.get("formBoost") or 1)) == "ultra"])
    section("Divine band (fb 22–50)", [r for r in all_rows if band_form(float(r.get("formBoost") or 1)) == "divine"])
    section("Enhancement band (fb 50–80)", [r for r in all_rows if band_form(float(r.get("formBoost") or 1)) == "enhancement"])
    section("Apex band (fb ≥ 80)", [r for r in all_rows if band_form(float(r.get("formBoost") or 1)) == "apex"])
    section("Base-form band only", [r for r in all_rows if band_form(float(r.get("formBoost") or 1)) == "base"])
    if recent_rows:
        section(f"Recent since {recent_cut}", recent_rows)

    # Concept comparison for divine band (formBoost 22–50)
    lines.append("## Divine band (formBoost 22–50) vs concept soft-cap")
    lines.append("")
    lines.append("| tier | live avg | soft-cap | delta | sim even |")
    lines.append("|-----:|---------:|---------:|------:|---------:|")
    divine = tier_stats(all_rows, form_band="divine")
    for t in sorted(divine):
        n = divine[t]["n"]
        if n < 50:
            continue
        avg = divine[t]["hit_post"] / n
        cap = SOFT_CAP.get(t, 0)
        sim = SIM_EVEN.get(t, "—")
        delta = avg - cap
        flag = " ⚠" if delta > 0.05 else ""
        lines.append(f"| T{t} | {avg:.3f} | {cap:.2f} | {delta:+.3f}{flag} | {sim} |")
        if t == 3 and avg > 0.38:
            errors.append(f"T3 divine avg {avg:.3f} high vs cap {cap} — cliff risk")
    prev_d_avg = None
    prev_d_t = None
    for t in sorted(divine):
        n = divine[t]["n"]
        if n < 50:
            continue
        avg = divine[t]["hit_post"] / n
        if prev_d_avg is not None and avg < prev_d_avg - 0.02:
            errors.append(f"Divine T{t} avg {avg:.3f} dropped below T{prev_d_t} {prev_d_avg:.3f}")
        prev_d_avg = avg
        prev_d_t = t
    lines.append("")

    # Tier step ratios (divine)
    lines.append("## Divine band tier step ratios")
    lines.append("")
    tiers_sorted = sorted(t for t in divine if divine[t]["n"] >= 100)
    for i in range(1, len(tiers_sorted)):
        a, b = tiers_sorted[i - 1], tiers_sorted[i]
        av = divine[a]["hit_post"] / divine[a]["n"]
        bv = divine[b]["hit_post"] / divine[b]["n"]
        ratio = bv / av if av > 0 else 0
        note = ""
        if ratio < 1.0:
            note = " ⚠ REGRESSION"
            errors.append(f"Divine T{a}→T{b} ratio {ratio:.2f} < 1.0")
        elif b - a > 1 and ratio > 1.35:
            note = " ⚠ BIG JUMP"
        lines.append(f"- T{a}→T{b}: {av:.3f} → {bv:.3f} ({ratio:.2f}×){note}")
    lines.append("")

    # Per-tier by form for T1-T7 ladder visual
    lines.append("## Hit pressure matrix (avg hitFracPost)")
    lines.append("")
    band_hdr = " | ".join(FORM_BAND_ORDER)
    lines.append(f"| tier | {band_hdr} | all |")
    lines.append("|-----:|" + "|".join(["-----:" for _ in FORM_BAND_ORDER]) + "|-----:|")
    for t in range(1, 8):
        cells = []
        for band in FORM_BAND_ORDER:
            sub = [r for r in all_rows if int(r.get("tier") or 0) == t and band_form(float(r.get("formBoost") or 1)) == band]
            cells.append(f"{sum(float(r.get('hitFracPost') or 0) for r in sub)/len(sub):.3f}" if sub else "—")
        all_t = [r for r in all_rows if int(r.get("tier") or 0) == t]
        cells.append(f"{sum(float(r.get('hitFracPost') or 0) for r in all_t)/len(all_t):.3f}" if all_t else "—")
        lines.append(f"| T{t} | " + " | ".join(cells) + " |")
    lines.append("")

    lines.append("## Result")
    if errors:
        lines.append(f"**FAIL** — {len(errors)} issue(s):")
        for e in errors:
            lines.append(f"- ❌ {e}")
    else:
        lines.append("**PASS** — tier ladder looks monotonic and within concept bands.")
    lines.append("")

    out = Path("/opt/cursor/artifacts/tier-ladder-analysis.md")
    out.parent.mkdir(parents=True, exist_ok=True)
    text = "\n".join(lines)
    out.write_text(text, encoding="utf-8")
    repo_out = Path(__file__).resolve().parent / "out" / "tier-ladder-analysis.md"
    repo_out.write_text(text, encoding="utf-8")
    print(text)
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
