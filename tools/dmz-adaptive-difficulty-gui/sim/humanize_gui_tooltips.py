#!/usr/bin/env python3
"""Rewrite gui-tooltips.json lore/name copy to sound more natural. Run from repo root."""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "src" / "main" / "resources" / "gui-tooltips.json"
CATALOG_REVISION = 189

# Longer phrases first.
REPLACEMENTS: list[tuple[str, str]] = [
    ("&eClick to pay and open", "&ePay, then open the appearance editor"),
    ("&eClick to review and confirm", "&eReview the summary, then confirm"),
    ("&eClick · confirm within 10s", "&eClick again within 10 seconds to confirm"),
    ("&eClick · choose player", "&eChoose a player next"),
    ("&eClick · Convert / Remove", "&eConvert or remove Android"),
    ("&eClick to confirm", "&eConfirm when you're ready"),
    ("&eClick to continue", "&eContinue"),
    ("&eClick to browse", "&eBrowse head parts"),
    ("&eClick to select", "&eSelect this option"),
    ("&eClick to start", "&eStart"),
    ("&eClick to end", "&eEnd"),
    ("&eClick to open", "&eOpen"),
    ("&eClick to convert", "&eConvert"),
    ("&eClick to mute", "&eHide these messages"),
    ("&eClick to show again", "&eShow these messages again"),
    ("&7Click to turn OFF for you only", "&7Turn off just for you"),
    ("&7Click to turn ON for you only", "&7Turn back on for you"),
    ("&7Click to mute drop messages", "&7Hide coin drop messages"),
    ("&7Click to show drop messages", "&7Show coin drop messages again"),
    ("&7Click to mute rival TP messages", "&7Hide rival TP messages"),
    ("&7Click to show rival TP messages", "&7Show rival TP messages again"),
    ("&7Click to disable Rival Instinct", "&7Turn off Rival Instinct alerts"),
    ("&7Click to enable Rival Instinct", "&7Turn on Rival Instinct alerts"),
    ("&8Click to mute mentor TP chat", "&8Hide mentor TP in chat"),
    ("&8Click to show in chat again", "&8Show mentor TP in chat again"),
    ("&8Click to toggle", "&8Toggle"),
    ("&8Click to send", "&8Send"),
    ("&8Staff only", "&8For staff"),
    ("&7Staff-only tools", "&7Staff tools"),
    ("&7Staff breakdown", "&7Staff-only breakdown"),
    ("&8From DMZ character.json", "&8Your race's default part"),
    ("Opens the DragonMineZ look editor", "Opens the in-game appearance editor"),
    ("&7Gain and spend ancient coins- tiers are lost on death",
     "&7Earn Ancient Coins from tougher fights — you lose tier progress on death"),
    ("&8Resets DMZ stats · Awards 1 held prestige",
     "&8Resets your stats · You keep one held prestige"),
    ("&7Unlock via DMZ level or Prestige", "&7Unlock with character level or Prestige"),
    ("&7Unlock with DMZ level or Prestige", "&7Unlock with character level or Prestige"),
    ("&7Unlock via DMZ or Prestige — keeps after lowering tier",
     "&7Unlock with character level or Prestige — stays unlocked if you lower tier"),
    ("All purchases are &4PERMANENT", "&4Permanent &7— survives prestiges"),
    ("&7Pay-up OK · change returned", "&7If you cancel, you get a refund"),
    ("&7Cosmetic look & head parts", "&7Change how you look and your head parts"),
    ("&7without wiping your whole build", "&7without wiping your whole character"),
    ("&7Staff tools for skill checks", "&7Staff tools for skill checks"),
    ("&7Natural · Saga progress", "&7Story progress and natural training"),
    ("&7Prestiges · Prestige shop", "&7Prestige up and visit the shop"),
    ("&7Race, class, reskin, head parts", "&7Change race, class, look, or head parts"),
    ("&8Paid with Ancient Coins", "&8Uses Ancient Coins"),
    ("&7Turned off or no permission", "&7Not available right now"),
    ("&7Close this menu", "&7Close"),
    ("&7Return to &f/lm", "&7Back to the main menu"),
    ("&eClick", "&eSelect"),
    ("Click to prestige", "Prestige when you're ready"),
    ("&7Click a higher unlocked tier to buy", "&7Select a higher tier you've unlocked to buy it"),
    ("&7Click a lower unlocked tier to step down (free)",
     "&7Step down to a lower tier you've unlocked (free)"),
    ("&aClick a head to Accept / Decline", "&aClick a player to accept or decline"),
    ("&7Use Actions → Declare to start", "&7Use Actions → Declare to find a rival"),
    ("Double-click to confirm", "Click twice to confirm"),
    ("&7Pick a new race and choose how much", "&7Choose a race and how much progress"),
    ("&7of your core stats carry over", "&7of your stats you want to keep"),
    ("&7Your base stats stay — class skills reset",
     "&7Your core stats stay — class skills reset"),
    ("&7Cosmetics & head parts — no stat changes", "&7Looks only — stats don't change"),
    ("&7This cannot be undone automatically", "&7You can't undo this from the menu"),
    ("&7Pay the shown cost and switch races", "&7Pay the listed cost to change race"),
    ("&7Pay and switch fighting class", "&7Pay the listed cost to change class"),
    ("&7Go back without paying", "&7Back out — no charge"),
    ("&7See preservation options and cost", "&7See what you keep and what it costs"),
    ("&7Review cost before you pay", "&7Check the cost before you pay"),
    ("&7Equip this race's default head part", "&7Use your race's default head part"),
    ("&7Remove cross-race parts", "&7Clear extra head parts"),
    ("&7Unlock & equip cross-race ears, horns, etc.", "&7Unlock ears, horns, and other head parts"),
    ("&7Cosmetics only — &cclass cannot change", "&7Looks only — &cyou can't change class here"),
    ("&7Level, stats, and race stay the same", "&7Level, stats, and race stay put"),
    ("&7Choose a system", "&7Pick what you want to do"),
    ("&7Use the rival system for more TP", "&7Rivals help you earn more TP"),
    ("&7Sparring TP & mentor bonds", "&7Spar for TP and mentor bonds"),
    ("&7Reload configs and open staff tools", "&7Reload configs and staff tools"),
    ("&7Toggle or flush server logs", "&7Turn logging on/off or save logs to disk"),
    ("&7Write buffered logs to disk", "&7Save buffered logs to disk"),
    ("&7Server event logging is on", "&7Event logging is on"),
    ("&7Turn server event logging back on", "&7Turn event logging back on"),
    ("&7Challenge yourself to custom scaled mobs", "&7Fight mobs scaled to your difficulty"),
    ("&cWarning: &7Scaled mobs can attack other players as well",
     "&cHeads up: &7scaled mobs can hit other players too"),
    ("&7Mutual rivals can raise your tier ceiling",
     "&7Mutual rivals can raise how high a tier you can buy"),
    ("&7Team modes also boost elite, mutant, and boss spawns",
     "&7Team modes bring more elites, mutants, and bosses"),
    ("&7Only dojo masters manage wars", "&7Only the dojo master can declare war"),
    ("&7Use the Pending menu instead", "&7Open the Pending tab for this"),
    ("&7End bond with &f{name}", "&7Leave your mentor &f{name}"),
    ("&7Dojo &f{name}", "&7Dojo &f{name}"),
    ("&7Your mentor and training partners", "&7Your mentor and training partners"),
    ("&7Invite apprentice or ask a mentor", "&7Invite an apprentice or ask someone to mentor you"),
    ("&7Incoming shows when they invite you", "&7Invites show up when someone picks you"),
    ("&7Other players must be online", "&7Someone else needs to be online"),
    ("&7Nothing to show right now", "&7Nothing to show yet"),
    ("&7Pick a skill below", "&7Pick a skill below"),
    ("&7Nothing here yet.", "&7Nothing here yet."),
]

HEADER_FIXES = {
    "hub.main.header.name": "&f&lLegacy Mechanics",
    "character.race_pct.header.name": "&e&lHow much do you want to keep?",
    "character.race_confirm.header.name": "&c&lSure about this?",
    "character.class_confirm.header.name": "&c&lConfirm class change",
}


def humanize_string(s: str) -> str:
    if not s or s.startswith("{") and s.endswith("}"):
        return s
    out = s
    for old, new in REPLACEMENTS:
        out = out.replace(old, new)
    return out


def walk(obj, path: str = "") -> None:
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k.startswith("_"):
                continue
            p = f"{path}.{k}" if path else k
            if p in HEADER_FIXES and isinstance(v, dict) and "name" in v:
                v["name"] = HEADER_FIXES[p]
            walk(v, p)
    elif isinstance(obj, list):
        for i, item in enumerate(obj):
            if isinstance(item, str):
                obj[i] = humanize_string(item)
            else:
                walk(item, path)
    elif isinstance(obj, str):
        pass


def main() -> None:
    data = json.loads(PATH.read_text(encoding="utf-8"))
    data["_catalogRevision"] = CATALOG_REVISION
    data["_comment"] = (
        "LegacyMechanics GUI button names and lore. Edit freely, then /lm admin reload. "
        "Placeholders like {cost} fill in at runtime. New keys from the mod merge in on reload."
    )
    walk(data)
    PATH.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Humanized {PATH} (catalog rev {CATALOG_REVISION})")


if __name__ == "__main__":
    main()
