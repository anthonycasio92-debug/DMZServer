"""Form-band classification for telemetry — derived from pack form JSON peaks.

`formBoost` in hit logs = max(STR/SKP/PWR/ENE/VIT/RES form mult) after DMZ stack,
clamped to ~100. Bands use natural gaps in config + live-race form ladders.

Saiyan reference ladder (@100% mastery, live-race-data 2026-09-03):
  base ≤1.12  →  no transform
  awakened    →  SSJ1 (~5×), Oozaru (~1.8×)
  super       →  SSJ2 (~9×), SSJ3 (~15×)
  ultra       →  SSJ4 / golden (~22×) — most non-saiyan races cap here (~21.8×)
  divine      →  Beyond God, SSG, SSB, Blue Evolved (~22–40×)
  enhancement →  Android overclock / SSDroid4 / metal overdrive (~50–75×)
  apex        →  Rare cap forms (≥80×) — combat mega target
"""
from __future__ import annotations

from scaling_constants import (
    FORM_BAND_AWAKENED,
    FORM_BAND_APEX,
    FORM_BAND_BASE,
    FORM_BAND_DIVINE,
    FORM_BAND_ENHANCEMENT,
    FORM_BAND_SUPER,
    FORM_BAND_ULTRA,
    band_form,
    band_form_label,
)

__all__ = [
    "band_form",
    "band_form_label",
    "band_form_legacy",
    "classify_peak",
    "FORM_BAND_ORDER",
]

from scaling_constants import FORM_BAND_ORDER, band_form_legacy  # noqa: E402


def classify_peak(peak_mult: float) -> str:
    """Classify a raw form peak multiplier (same thresholds as telemetry formBoost)."""
    return band_form(peak_mult)
