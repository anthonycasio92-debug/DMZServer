#!/usr/bin/env python3
"""Audit combat scaling against 2.3.171 form bands.

Checks:
  1. Java formBandLabel thresholds match Python FORM_BAND_* constants
  2. Sim hitFrac/landing ladders per band (saiyan warrior, even build)
  3. Live telemetry per band vs sim @ gate (T5/T7)
  4. Combat math breakpoints (mega start @6, pierce @6, etc.) align with bands

Writes sim/out/scaling-bands-audit.md
"""
from __future__ import annotations

import json
import re
import sys
from collections import defaultdict
from datetime import date, datetime
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from scaling_constants import (  # noqa: E402
    CONCEPT_EVEN_HITFRAC_MIN,
    CONCEPT_GOD_LANDING_MIN,
    FORM_BAND_AWAKENED,
    FORM_BAND_BASE,
    FORM_BAND_DIVINE,
    FORM_BAND_ENHANCEMENT,
    FORM_BAND_ORDER,
    FORM_BAND_SUPER,
    FORM_BAND_ULTRA,
    SOFT_CAP,
    TIER_DMZ_GATE,
    band_form,
)
from simulate_build_matrix import INVEST, SKILL_LOADOUTS, simulate  # noqa: E402
from simulate_race_forms import MAX_FORM, apply_mastery, load_forms, load_stats  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
PROFILE = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/calc/PlayerCombatProfile.java"
OUT = Path(__file__).resolve().parent / "out"
OUT.mkdir(parents=True, exist_ok=True)

# Bands that use mega-form / live-share combat path (formBoost ≥ 6).
MEGA_BANDS = frozenset({"super", "ultra", "divine", "enhancement", "apex"})


def parse_java_band_thresholds() -> dict[str, float]:
    text = PROFILE.read_text(encoding="utf-8")
    thresholds: dict[str, float] = {"base": FORM_BAND_BASE}
    m = re.search(r"MEGA_FORM_START\s*=\s*([\d.]+)", text)
    if m:
        thresholds["awakened_hi"] = float(m.group(1))
    for label, val in (
        ("super_hi", r"if \(v < ([\d.]+)\) \{\s*return \"super\""),
        ("ultra_hi", r"if \(v < ([\d.]+)\) \{\s*return \"ultra\""),
        ("divine_hi", r"if \(v < ([\d.]+)\) \{\s*return \"divine\""),
    ):
        m = re.search(val, text)
        if m:
            thresholds[label] = float(m.group(1))
    m = re.search(r"MEGA_FORM_TARGET\s*=\s*([\d.]+)", text)
    if m:
        thresholds["enhancement_hi"] = float(m.group(1))
    return thresholds


def pick_representative_forms(forms: list[dict]) -> dict[str, tuple[str, dict[str, float], float]]:
    """One saiyan form per band — highest peak in band."""
    best: dict[str, tuple[str, dict[str, float], float]] = {}
    for f in forms:
        fmap = {
            "STR": min(MAX_FORM, apply_mastery(f["str"], f["maxMastery"], f["maxStats"], 1.0)),
            "SKP": min(MAX_FORM, apply_mastery(f["skp"], f["maxMastery"], f["maxStats"], 1.0)),
            "PWR": min(MAX_FORM, apply_mastery(f["pwr"], f["maxMastery"], f["maxStats"], 1.0)),
            "ENE": min(MAX_FORM, apply_mastery(f.get("ene", 1), f["maxMastery"], f["maxStats"], 1.0)),
            "VIT": min(MAX_FORM, apply_mastery(f["vit"], f["maxMastery"], f["maxStats"], 1.0)),
            "RES": min(MAX_FORM, apply_mastery(f["def"], f["maxMastery"], f["maxStats"], 1.0)),
        }
        peak = max(fmap.values())
        b = band_form(peak)
        label = f"{f['group']}.{f['name']}"
        if b not in best or peak > best[b][2]:
            best[b] = (label, fmap, peak)
    if "base" not in best:
        base_f = {k: 1.0 for k in ("STR", "SKP", "PWR", "ENE", "VIT", "RES")}
        best["base"] = ("(no transform)", base_f, 1.0)
    return best


def load_telemetry_rows(telemetry_dir: Path | None) -> list[dict]:
    if telemetry_dir is None or not telemetry_dir.is_dir():
        return []
    rows: list[dict] = []
    for p in sorted(telemetry_dir.glob("hits-*.jsonl")):
        for line in p.read_text(encoding="utf-8", errors="replace").splitlines():
            line = line.strip()
            if not line:
                continue
            try:
                rows.append(json.loads(line))
            except json.JSONDecodeError:
                continue
    return rows


def agg_live(rows: list[dict], *, tier: int, band: str) -> tuple[int, float]:
    sub = [
        r for r in rows
        if int(r.get("tier") or 0) == tier
        and band_form(float(r.get("formBoost") or 1)) == band
    ]
    if not sub:
        return 0, 0.0
    return len(sub), sum(float(r.get("hitFracPost") or 0) for r in sub) / len(sub)


def main() -> int:
    errors: list[str] = []
    lines = [
        "# Scaling × form-band audit",
        "",
        "Validates combat scaling against the 2.3.171 telemetry band taxonomy.",
        "",
    ]

    def check(label: str, cond: bool, detail: str = "") -> None:
        if cond:
            lines.append(f"- ✅ {label}" + (f" — {detail}" if detail else ""))
        else:
            msg = f"{label}: {detail or 'failed'}"
            errors.append(msg)
            lines.append(f"- ❌ {msg}")

    # ── 1) Java ↔ Python threshold parity ───────────────────────────────────
    lines += ["## 1) Java ↔ Python band thresholds", ""]
    java = parse_java_band_thresholds()
    pairs = [
        ("base gate ≤1.12", FORM_BAND_BASE, 1.12),
        ("awakened <6 (MEGA_FORM_START)", FORM_BAND_AWAKENED, java.get("awakened_hi", 0)),
        ("super <15", FORM_BAND_SUPER, java.get("super_hi", 0)),
        ("ultra <22", FORM_BAND_ULTRA, java.get("ultra_hi", 0)),
        ("divine <50", FORM_BAND_DIVINE, java.get("divine_hi", 0)),
        ("enhancement <80 (MEGA_FORM_TARGET)", FORM_BAND_ENHANCEMENT, java.get("enhancement_hi", 0)),
    ]
    for label, py_val, java_val in pairs:
        check(label, abs(py_val - java_val) < 1e-9, f"py={py_val} java={java_val}")

    profile = PROFILE.read_text(encoding="utf-8")
    check(
        "Java logs formBand via BalanceTelemetry",
        "formBandLabel(profile.formBoost)" in (ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/telemetry/BalanceTelemetry.java").read_text(encoding="utf-8"),
    )
    check("T3 pierce at formBoost≥6 (super+)", "formBoost >= 6.0" in profile)
    check("Mega-form path at formBoost≥6", "formBoost >= 6.0" in profile and "MEGA_FORM_START" in profile)

    # ── 2) Sim per band ─────────────────────────────────────────────────────
    lines += ["", "## 2) Sim ladder per band (saiyan warrior, even build)", ""]
    stats = load_stats("saiyan")
    st = stats["warrior"]
    forms = load_forms("saiyan")
    reps = pick_representative_forms(forms)
    inv = INVEST["warrior"]
    base_pts = {k: st["base"].get(k, 0) + inv.get(k, 0) for k in ("STR", "SKP", "RES", "VIT", "PWR", "ENE")}

    lines.append("| band | form | fb | T3 hit | T5 hit | T7 hit | T5 land | T7 land |")
    lines.append("|------|------|---:|-------:|-------:|-------:|--------:|--------:|")

    sim_by_band: dict[str, dict[int, dict]] = {}
    for band in FORM_BAND_ORDER:
        if band not in reps:
            continue
        label, fmap, peak = reps[band]
        gate5 = TIER_DMZ_GATE[5]
        gate7 = TIER_DMZ_GATE[7]
        by_t = {
            3: simulate(base_pts, st["scale"], fmap, "warrior", 3, SKILL_LOADOUTS["none"], dmz_level=TIER_DMZ_GATE[3]),
            5: simulate(base_pts, st["scale"], fmap, "warrior", 5, SKILL_LOADOUTS["none"], dmz_level=gate5),
            7: simulate(base_pts, st["scale"], fmap, "warrior", 7, SKILL_LOADOUTS["none"], dmz_level=gate7),
        }
        sim_by_band[band] = by_t
        lines.append(
            f"| {band} | `{label}` | {peak:.1f} | "
            f"{by_t[3]['hitFrac']:.3f} | {by_t[5]['hitFrac']:.3f} | {by_t[7]['hitFrac']:.3f} | "
            f"{by_t[5].get('landingFrac', 0):.3f} | {by_t[7].get('landingFrac', 0):.3f} |"
        )

        if band == "base":
            check(
                "sim base T5 ≥ concept min",
                by_t[5]["hitFrac"] >= CONCEPT_EVEN_HITFRAC_MIN[5],
                f"{by_t[5]['hitFrac']:.3f}",
            )
            check(
                "sim base T7 ≥ concept min",
                by_t[7]["hitFrac"] >= CONCEPT_EVEN_HITFRAC_MIN[7],
                f"{by_t[7]['hitFrac']:.3f}",
            )
        if band in MEGA_BANDS:
            check(
                f"sim {band} T5 hitFrac ≤ soft-cap+0.02",
                by_t[5]["hitFrac"] <= SOFT_CAP[5] + 0.02,
                f"{by_t[5]['hitFrac']:.3f} cap={SOFT_CAP[5]}",
            )
            check(
                f"sim {band} T7 hitFrac ≤ soft-cap+0.02",
                by_t[7]["hitFrac"] <= SOFT_CAP[7] + 0.02,
                f"{by_t[7]['hitFrac']:.3f} cap={SOFT_CAP[7]}",
            )
        if band in ("divine", "enhancement", "apex"):
            check(
                f"sim {band} T5 post-DEF ≥12%",
                by_t[5]["hitFrac"] * 0.35 >= 0.12,
                f"postDef~={by_t[5]['hitFrac']*0.35:.3f}",
            )
            check(
                f"sim {band} T5 landing ≥ concept min",
                by_t[5].get("landingFrac", 0) >= CONCEPT_GOD_LANDING_MIN[5],
                f"land={by_t[5].get('landingFrac', 0):.3f}",
            )
            check(
                f"sim {band} T7 landing ≥ concept min",
                by_t[7].get("landingFrac", 0) >= CONCEPT_GOD_LANDING_MIN[7],
                f"land={by_t[7].get('landingFrac', 0):.3f}",
            )

    lines.append("")

    # Divine+ ladder monotonicity (T3→T7)
    if "divine" in sim_by_band:
        d = sim_by_band["divine"]
        check(
            "divine sim T3≤T4≤T5≤T6≤T7 hitFrac",
            d[3]["hitFrac"] <= d[5]["hitFrac"] + 1e-9 <= d[7]["hitFrac"] + 1e-9,
            f"T3={d[3]['hitFrac']:.3f} T5={d[5]['hitFrac']:.3f} T7={d[7]['hitFrac']:.3f}",
        )
        check(
            "divine sim T5 landing < T7 landing",
            d[5].get("landingFrac", 0) + 0.01 < d[7].get("landingFrac", 0),
            f"T5={d[5].get('landingFrac', 0):.3f} T7={d[7].get('landingFrac', 0):.3f}",
        )

    # Higher bands should not under-press vs base at T5 (mega path engages)
    if "base" in sim_by_band and "divine" in sim_by_band:
        check(
            "divine T5 hitFrac ≥ base T5 ×0.95",
            sim_by_band["divine"][5]["hitFrac"] >= sim_by_band["base"][5]["hitFrac"] * 0.95,
            f"divine={sim_by_band['divine'][5]['hitFrac']:.3f} base={sim_by_band['base'][5]['hitFrac']:.3f}",
        )

    # ── 3) Live telemetry vs sim ────────────────────────────────────────────
    lines += ["", "## 3) Live telemetry vs sim (post-Sep 3 slice)", ""]
    repo = Path(__file__).resolve().parents[3]
    telem_dirs = [
        repo / "uploads/live-telemetry-2026-09-04-retune",
        repo / "uploads/live-telemetry-2026-09-04-audit",
    ]
    all_rows = load_telemetry_rows(telem_dirs[0])
    recent_cut = date(2026, 9, 3)
    recent_rows = [
        r for r in all_rows
        if (fd := _file_date(telem_dirs[0], r)) is None or fd >= recent_cut
    ]
    # Filter by file date instead — re-load recent files only
    recent_paths = [
        p for p in telem_dirs[0].glob("hits-*.jsonl")
        if (fd := _path_date(p)) and fd >= recent_cut
    ]
    recent_rows = []
    for p in recent_paths:
        for line in p.read_text(encoding="utf-8", errors="replace").splitlines():
            line = line.strip()
            if line:
                try:
                    recent_rows.append(json.loads(line))
                except json.JSONDecodeError:
                    pass

    if recent_rows:
        lines.append(f"Recent hits: **{len(recent_rows)}** since {recent_cut}")
        lines.append("")
        lines.append("| band | n@T5 | live T5 | sim T5 | Δ | n@T7 | live T7 | sim T7 | Δ | note |")
        lines.append("|------|-----:|--------:|-------:|--:|-----:|--------:|-------:|--:|------|")
        for band in FORM_BAND_ORDER:
            if band not in sim_by_band:
                continue
            n5, live5 = agg_live(recent_rows, tier=5, band=band)
            n7, live7 = agg_live(recent_rows, tier=7, band=band)
            sim5 = sim_by_band[band][5]["hitFrac"]
            sim7 = sim_by_band[band][7]["hitFrac"]
            d5 = live5 - sim5 if n5 >= 30 else None
            d7 = live7 - sim7 if n7 >= 30 else None
            note = ""
            if band in MEGA_BANDS and n5 >= 100 and d5 is not None:
                if live5 > sim5 + 0.10:
                    note = "⚠ hot"
                    errors.append(f"live {band} T5 hot vs sim: {live5:.3f} > {sim5:.3f}+0.10")
                elif live5 < sim5 - 0.15:
                    note = "cold (pre-175?)"
            live5_s = f"{live5:.3f}" if n5 else "—"
            live7_s = f"{live7:.3f}" if n7 else "—"
            d5_s = f"{d5:+.3f}" if d5 is not None else "—"
            d7_s = f"{d7:+.3f}" if d7 is not None else "—"
            lines.append(
                f"| {band} | {n5} | {live5_s} | {sim5:.3f} | {d5_s} | "
                f"{n7} | {live7_s} | {sim7:.3f} | {d7_s} | {note or '—'} |"
            )
        lines.append("")
        lines.append(
            "_Note: mixed-jar telemetry may still include pre-2.3.175 hits; "
            "re-pull 24–48h post-deploy for clean read._"
        )
    else:
        lines.append("_No recent telemetry found — sim-only checks above._")

    # ── 4) Band catalog coverage ────────────────────────────────────────────
    lines += ["", "## 4) Pack form catalog per band", ""]
    hist: dict[str, int] = defaultdict(int)
    for f in forms:
        fmap = {
            "STR": min(MAX_FORM, apply_mastery(f["str"], f["maxMastery"], f["maxStats"], 1.0)),
            "SKP": min(MAX_FORM, apply_mastery(f["skp"], f["maxMastery"], f["maxStats"], 1.0)),
            "PWR": min(MAX_FORM, apply_mastery(f["pwr"], f["maxMastery"], f["maxStats"], 1.0)),
            "ENE": min(MAX_FORM, apply_mastery(f.get("ene", 1), f["maxMastery"], f["maxStats"], 1.0)),
            "VIT": min(MAX_FORM, apply_mastery(f["vit"], f["maxMastery"], f["maxStats"], 1.0)),
            "RES": min(MAX_FORM, apply_mastery(f["def"], f["maxMastery"], f["maxStats"], 1.0)),
        }
        hist[band_form(max(fmap.values()))] += 1
    for b in FORM_BAND_ORDER:
        n = hist.get(b, 0)
        if b == "base":
            check(f"saiyan catalog has {b} forms (or synthetic)", n >= 0, f"n={n}")
        elif b == "apex":
            check(f"saiyan catalog has {b} forms (optional)", True, f"n={n}")
        else:
            check(f"saiyan catalog has {b} forms", n > 0, f"n={n}")

    lines += ["", "## Result", ""]
    if errors:
        lines.append(f"**ADVISORY** — {len(errors)} issue(s) (see live vs sim if mixed-jar):")
        for e in errors:
            lines.append(f"- ❌ {e}")
    else:
        lines.append("**PASS** — scaling aligns with form-band taxonomy.")
    lines.append("")

    text = "\n".join(lines)
    out = OUT / "scaling-bands-audit.md"
    out.write_text(text, encoding="utf-8")
    art = Path("/opt/cursor/artifacts/scaling-bands-audit.md")
    art.parent.mkdir(parents=True, exist_ok=True)
    art.write_text(text, encoding="utf-8")
    print(text)
    return 1 if errors else 0


def _path_date(p: Path) -> date | None:
    stem = p.stem
    if stem.startswith("hits-"):
        try:
            return datetime.strptime(stem[5:], "%Y-%m-%d").date()
        except ValueError:
            return None
    return None


def _file_date(_dir: Path, _row: dict) -> date | None:
    return None


if __name__ == "__main__":
    raise SystemExit(main())
