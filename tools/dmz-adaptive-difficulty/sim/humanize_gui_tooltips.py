#!/usr/bin/env python3
"""Apply player-facing copy polish to gui-tooltips.json (in-repo catalog)."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
TOOLTIPS = ROOT / "tools" / "dmz-adaptive-difficulty-gui" / "src" / "main" / "resources" / "gui-tooltips.json"

# Global phrase replacements (order: longer first)
PHRASES: list[tuple[str, str]] = [
    ("&7Gain and spend ancient coins- tiers", "&7Earn and spend Ancient Coins on tiers"),
    ("&8/lm", "&8Open with &f/lm"),
    ("&eClick to open", "&8Opens this menu"),
    ("&eClick to select", "&8Select this option"),
    ("&eClick to start", "&8Start this boost"),
    ("&eClick to end", "&8Stop the active boost"),
    ("&eClick to apply", "&8Set as your dojo banner"),
    ("&eClick to convert", "&8Convert now"),
    ("&eClick to mute", "&8Click to hide in chat"),
    ("&eClick to show again", "&8Click to show in chat again"),
    ("&eClick · choose player", "&8Pick a player next"),
    ("&eClick · Convert / Remove", "&8Opens convert or remove tools"),
    ("&eClick · confirm within 10s", "&8Confirm again within 10 seconds"),
    ("&8Click to toggle", "&8Click to switch"),
    ("&8Click to send", "&8Send with this duration"),
    ("Visible declare → they Accept → Mutual", "Declare publicly — they accept to become mutual"),
    ("&7Use Actions → Declare to start", "&7Use Actions → Declare to add a rival"),
    ("&7Pick a player to ask as your master", "&7Pick someone to request as your mentor"),
    ("&7Use the Pending menu instead", "&7Open Pending from Mentor Actions"),
    ("&7Enabled + path summary", "&7Shows whether spar is on and where data is stored"),
    ("&7Reload stores from disk", "&7Reload rivalry data from disk"),
    ("&7Save data, check status, reset cooldowns", "&7Save data, view status, or clear mentor cooldown"),
    ("&7Only your own tier ceiling counts", "&7Only your tier counts toward the team ceiling"),
    ("&7Leaderboard", "&7Who has the most sparring TP"),
    ("&7Total TP", "&7Ranked by total training TP"),
    ("&7Sessions", "&7Ranked by spar session count"),
    ("&7Perfect spars", "&7Ranked by perfect spar wins"),
    ("&7Sort by ranking points this season", "&7Sort dojos by season ranking points"),
    ("&7Sort by TP earned against other dojos", "&7Sort by TP earned vs other dojos"),
    ("&7See how dojos rank this season", "&7Season ladder for every dojo"),
    ("&7Spar rival dojos to climb the ladder", "&7Spar outside your dojo to earn points"),
    ("&8Ranking points come from cross-dojo spars", "&8Points come from spars against other dojos"),
    ("&7Unlocked in the skill saga — some level there, some level naturally after.",
     "&7Saga skills — some unlock in the saga, others level naturally over time."),
    ("&7Race lock · Yardrat · Spiritualist · Android", "&7Race lock, Yardrat, Spiritualist, and Android"),
    ("&7Ki weapons · Piercing · DoT · Apothic", "&7Ki weapons, piercing, damage over time, and Apothic tweaks"),
    ("&7Energy, TP/SP, race class, etc.", "&7Energy, TP/SP, race class, and related toggles"),
    ("&7Fabled race and form options", "&7Fabled race and transformation options"),
    ("&a You are a Permanent Majin", "&aYou are a Permanent Majin"),
    ("&7Future prestige Need scales up to your new cap", "&7Future prestige requirements scale with your new cap"),
    ("&7Click to turn OFF for you only", "&8Turns scaling off for you only"),
    ("&7Click to turn ON for you only", "&8Turns scaling on for you only"),
    ("&7Click to mute drop messages", "&8Hides Ancient Coin drop chat"),
    ("&7Click to show drop messages", "&8Shows Ancient Coin drop chat again"),
    ("&7Click to mute rival TP messages", "&8Hides rival TP chat messages"),
    ("&7Click to show rival TP messages", "&8Shows rival TP chat messages again"),
    ("&7Click to disable Rival Instinct", "&8Turns rival proximity alerts off"),
    ("&7Click to enable Rival Instinct", "&8Turns rival proximity alerts on"),
    ("&7Click to prestige (confirm within 10s)", "&8Prestige — confirm again within 10 seconds"),
    ("&8Both Silent → Declared (both notified)", "&8If you both used Silent, this becomes Declared"),
]

KEY_OVERRIDES: dict[str, dict] = {
    "hub.main.difficulty": {
        "lore": [
            "&7Earn Ancient Coins from scaled mobs and spend them on tiers",
            "&7Tiers reset when you die — higher tiers mean tougher fights",
            "&cNote: &7Scaled mobs can hurt other players nearby",
            "&8Opens this menu",
        ],
    },
    "hub.main.rival": {
        "lore": [
            "&7Rivalries earn bonus training TP and ranking points",
            "&7Declare rivals, run challenges, and track seasons",
            "&8Opens this menu",
        ],
    },
    "hub.main.spar": {
        "lore": [
            "&7Spar for training TP and mentor bonds",
            "&7Dojo seasons, apprentices, and rankings",
            "&8Opens this menu",
        ],
    },
    "spar.mentor.dojo": {
        "lore": [
            "&7Your mentor and training partners",
            "&7Switch view if you also mentor others",
            "&7Roster: &f{name}",
        ],
    },
    "spar.mentor.pending": {
        "lore": [
            "&7Incoming and outgoing mentor invites",
            "&8Use Accept / Decline on each invite",
        ],
    },
    "rival.main.actions": {
        "lore": [
            "&7Declare, accept, decline, or remove rivals",
            "{pending}",
        ],
    },
    "rival.actions.declare": {
        "lore": ["&7Send a public declare — they accept to become mutual"],
    },
    "rival.actions.accept": {
        "lore": [
            "&7Accept a pending declare or upgrade to mutual",
            "&8Silent declares from both sides appear here too",
        ],
    },
    "common.back": {
        "lore": ["&7Go back to the previous screen"],
    },
}

# Keep in sync with GuiTooltips.CATALOG_REVISION (bukkit/GuiTooltips.java)
CATALOG_REVISION = 185


def walk_replace(obj):
    if isinstance(obj, str):
        out = obj
        for old, new in PHRASES:
            out = out.replace(old, new)
        return out
    if isinstance(obj, list):
        return [walk_replace(x) for x in obj]
    if isinstance(obj, dict):
        return {k: walk_replace(v) for k, v in obj.items()}
    return obj


def flatten_keys(obj: dict, prefix: str = "") -> dict[str, dict]:
    out: dict[str, dict] = {}
    for k, v in obj.items():
        if k.startswith("_"):
            continue
        path = f"{prefix}.{k}" if prefix else k
        if isinstance(v, dict) and "name" not in v and "lore" not in v:
            out.update(flatten_keys(v, path))
        else:
            out[path] = v if isinstance(v, dict) else {"lore": v}
    return out


def apply_key_overrides(root: dict) -> None:
    flat = flatten_keys(root)
    for key, patch in KEY_OVERRIDES.items():
        parts = key.split(".")
        node = root
        for p in parts[:-1]:
            node = node.setdefault(p, {})
        leaf = parts[-1]
        entry = node.setdefault(leaf, {})
        if isinstance(entry, dict):
            entry.update(patch)


def main() -> int:
    if not TOOLTIPS.is_file():
        print(f"Missing {TOOLTIPS}", file=sys.stderr)
        return 2
    data = json.loads(TOOLTIPS.read_text(encoding="utf-8"))
    data = walk_replace(data)
    apply_key_overrides(data)
    data["_catalogRevision"] = CATALOG_REVISION
    comment = data.get("_comment", "")
    if "catalogRevision" not in comment:
        data["_comment"] = (
            comment.rstrip()
            + " _catalogRevision bumps on humanize passes; /lm admin reload upgrades older on-disk files from the jar."
        )
    TOOLTIPS.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Humanized {TOOLTIPS} → catalog revision {CATALOG_REVISION}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
