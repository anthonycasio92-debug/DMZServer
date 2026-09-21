#!/usr/bin/env python3
"""Verify race-change class mapping against config/dragonminez/races/*/stats.json."""

from __future__ import annotations

import json
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
RACES = REPO / "config" / "dragonminez" / "races"

PREFERRED_FALLBACK = ["warrior", "cleric", "martialartist", "berserker", "assassin"]


def class_ids_for_race(race_id: str) -> list[str]:
    stats = RACES / race_id / "stats.json"
    if not stats.is_file():
        return []
    data = json.loads(stats.read_text(encoding="utf-8"))
    classes = data.get("classes") or {}
    return [k.strip().lower() for k in classes.keys() if k]


def canonical_id(class_id: str, allowed: list[str]) -> str | None:
    want = (class_id or "").strip().lower()
    if not want:
        return None
    for aid in allowed:
        if want == aid.lower():
            return aid
    return None


def resolve_class_for_race(prior: str, race_id: str) -> str:
    allowed = class_ids_for_race(race_id)
    prior = (prior or "").strip().lower()
    if not allowed:
        return prior or "warrior"
    c = canonical_id(prior, allowed)
    if c:
        return c
    for pref in PREFERRED_FALLBACK:
        for aid in allowed:
            if pref == aid.lower():
                return aid
    return allowed[0]


def resolve_after_change(current: str, prior: str, race_id: str) -> str:
    allowed = class_ids_for_race(race_id)
    if not allowed:
        return resolve_class_for_race(prior, race_id)
    c = canonical_id(current, allowed)
    if c:
        return c
    return resolve_class_for_race(prior, race_id)


def requires_picker(current: str, race_id: str) -> bool:
    allowed = class_ids_for_race(race_id)
    if not allowed:
        return True
    if not (current or "").strip():
        return True
    return canonical_id(current, allowed) is None


def main() -> int:
    errors: list[str] = []

    # Saiyan spiritualist -> sento_saiyan: must remap, picker required
    if not requires_picker("spiritualist", "sento_saiyan"):
        errors.append("spiritualist on sento_saiyan should require picker")
    mapped = resolve_after_change("spiritualist", "spiritualist", "sento_saiyan")
    if mapped not in class_ids_for_race("sento_saiyan"):
        errors.append(f"spiritualist->sento mapped to invalid {mapped}")
    if mapped == "spiritualist":
        errors.append("spiritualist should not stay on sento_saiyan")

    # Warrior saiyan -> human: keep warrior
    kept = resolve_after_change("warrior", "warrior", "human")
    if kept != "warrior":
        errors.append(f"warrior should stay warrior on human, got {kept}")
    if requires_picker("warrior", "human"):
        errors.append("warrior on human should not require picker")

    # Player picked cleric after sento race change (current), prior spiritualist
    pick = resolve_after_change("cleric", "spiritualist", "sento_saiyan")
    if pick != "cleric":
        errors.append(f"player pick cleric should stay cleric, got {pick}")

    for race_dir in sorted(RACES.iterdir()):
        if not race_dir.is_dir():
            continue
        race = race_dir.name
        allowed = class_ids_for_race(race)
        if not allowed:
            continue
        for cls in allowed:
            out = resolve_after_change(cls, "warrior", race)
            if out != cls:
                errors.append(f"{cls} on {race} should keep itself, got {out}")

    if errors:
        print("FAIL race class mapper checks:")
        for e in errors:
            print(" -", e)
        return 1
    print("PASS race class mapper checks")
    return 0


if __name__ == "__main__":
    sys.exit(main())
