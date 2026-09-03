#!/usr/bin/env python3
"""Validate form-band thresholds against pack form JSON + write reference doc."""
from __future__ import annotations

import json
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from scaling_constants import FORM_BAND_ORDER, FORM_BAND_LABELS, FORM_BAND_EXAMPLES, band_form

ROOTS = [
    Path(__file__).resolve().parents[3] / "uploads/live-race-data-2026-09-03/config/dragonminez/races",
    Path(__file__).resolve().parents[3] / "config/dragonminez/races",
]
OUT = Path(__file__).resolve().parent / "out"
OUT.mkdir(parents=True, exist_ok=True)


def peak_at_m100(form: dict) -> float:
    m = float(form.get("maxStatsMultiplier") or 1.0)
    mults = []
    for k in ("strMultiplier", "skpMultiplier", "pwrMultiplier", "eneMultiplier", "vitMultiplier", "defMultiplier"):
        base = float(form.get(k) or 1.0)
        mults.append(base * (1.0 + 1.0 * (m - 1.0)))
    return max(mults) if mults else 1.0


def load_catalog() -> list[dict]:
    seen: dict[str, dict] = {}
    for root in ROOTS:
        if not root.is_dir():
            continue
        for race_dir in sorted(root.iterdir()):
            forms_dir = race_dir / "forms"
            if not forms_dir.is_dir():
                continue
            for fp in sorted(forms_dir.glob("*.json")):
                try:
                    data = json.loads(fp.read_text(encoding="utf-8"))
                except (json.JSONDecodeError, OSError):
                    continue
                group = data.get("groupName") or fp.stem
                for name, form in (data.get("forms") or {}).items():
                    key = f"{race_dir.name}/{group}/{name}"
                    if key in seen:
                        continue
                    peak = round(peak_at_m100(form), 1)
                    seen[key] = {
                        "race": race_dir.name,
                        "group": group,
                        "form": name,
                        "peak": peak,
                        "band": band_form(peak),
                    }
    return list(seen.values())


def write_reference(catalog: list[dict]) -> Path:
    hist: dict[str, list[dict]] = defaultdict(list)
    for f in catalog:
        hist[f["band"]].append(f)

    lines = [
        "# Form band reference (telemetry formBoost)",
        "",
        "`formBoost` = peak live form multiplier across STR/SKP/PWR/ENE/VIT/RES (cap ~100).",
        "Bands derived from natural gaps in pack form JSON (@100% mastery).",
        "",
        "| Band | formBoost range | Player meaning |",
        "|------|-----------------|----------------|",
    ]
    ranges = [
        ("base", "≤ 1.12"),
        ("awakened", "1.12 – 6"),
        ("super", "6 – 15"),
        ("ultra", "15 – 22"),
        ("divine", "22 – 50"),
        ("enhancement", "50 – 80"),
        ("apex", "≥ 80"),
    ]
    for b, rng in ranges:
        lines.append(f"| **{FORM_BAND_LABELS[b]}** (`{b}`) | {rng} | {FORM_BAND_EXAMPLES[b]} |")

    lines += ["", f"Pack catalog: **{len(catalog)}** forms.", ""]
    for b in FORM_BAND_ORDER:
        items = sorted(hist.get(b, []), key=lambda x: -x["peak"])
        lines.append(f"## {FORM_BAND_LABELS[b]} — {len(items)} forms")
        lines.append("")
        if not items:
            lines.append("_(none in catalog)_")
            lines.append("")
            continue
        for f in items[:12]:
            lines.append(f"- **{f['peak']}×** `{f['race']}/{f['group']}/{f['form']}`")
        if len(items) > 12:
            lines.append(f"- _… +{len(items) - 12} more_")
        lines.append("")

    lines.append("## Saiyan ladder (reference)")
    lines.append("")
    for f in sorted([x for x in catalog if x["race"] == "saiyan"], key=lambda x: x["peak"]):
        lines.append(f"- {f['peak']:4.1f}× `{f['group']}/{f['form']}` → **{f['band']}**")

    path = OUT / "form-band-reference.md"
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def main() -> int:
    catalog = load_catalog()
    if not catalog:
        print("FAIL: no forms found", file=sys.stderr)
        return 2
    path = write_reference(catalog)
    print(f"Catalogued {len(catalog)} forms → {path}")
    for b in FORM_BAND_ORDER:
        n = sum(1 for f in catalog if f["band"] == b)
        print(f"  {b:12s} {n:3d} forms")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
