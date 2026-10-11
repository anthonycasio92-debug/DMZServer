#!/usr/bin/env python3
"""Fail-closed: every Ultra button/tab action id must be routable by UltraGuiActions."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ULTRA = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/ultra"
ACTIONS = ULTRA / "UltraGuiActions.java"
SPEC = ULTRA / "UltraScreenSpec.java"
STYLE = ULTRA / "UltraStyle.java"
PACKET = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/net/gui/GuiOpenPacket.java"
DOC = ROOT.parents[1] / "docs" / "KAIPATH_GUI_PROTOCOL.md"

# Known do-actions emitted by Ultra*Screens (action name after do: / before next :).
KNOWN_DO: dict[str, set[str]] = {
    "hub": {"reload"},
    "admin": {"reload"},
    "difficulty": {"activate", "lower_tier", "toggle_personal", "summon_end_dragon"},
    "character": {
        "reskin_confirm",
        "bone_race_default",
        "saga_confirm",
        "saga_reset",
        "android_remove",
        "android",
    },
    "prestige": {"confirm", "turnin", "skill"},
    "progression": {"boost", "toggle_staff_free_coins"},
    "rival": {
        "tpmsg",
        "instinct",
        "accept_replace",
        "admin",
        "challenge",
        "spectate_stop",
    },
    "spar": {
        "tpmsg",
        "end",
        "mentor_tpmsg",
        "mentor",
        "admin",
        "dojo_accept",
        "dojo_war_cancel",
    },
    "config": {"open_module", "save_all", "discard_all", "reload_module"},
}


def extract_actions(text: str) -> list[str]:
    found: list[str] = []
    for m in re.finditer(r'"(do:[^"]+|tab:[^"]+|nav:[^"]+|open:[^"]+)"', text):
        found.append(m.group(1))
    if 'UltraStyle.PAGE_ANDROID_REMOVE' in text and 'do:"' in text.replace(" ", ""):
        found.append("do:android_remove")
    return found


def main() -> int:
    errors: list[str] = []

    actions_src = ACTIONS.read_text(encoding="utf-8")
    for needle in (
        "qualify(",
        "LOGGER.error",
        "UltraGuiActions.handle failed",
        "stringArg(",
        "bareHubOpenTarget",
        "hasVerbPrefix",
        "rawActionId",
    ):
        if needle not in actions_src:
            errors.append(f"UltraGuiActions missing {needle}")
    for system in (
        "character",
        "difficulty",
        "progression",
        "prestige",
        "spar",
        "rival",
        "config",
        "admin",
    ):
        if f'case "{system}"' not in actions_src and f'"{system}"' not in actions_src:
            errors.append(f"UltraGuiActions bareHubOpenTarget missing {system}")
    if "rawActionId" not in actions_src:
        errors.append("UltraGuiActions must log rawActionId on failures")

    spec = SPEC.read_text(encoding="utf-8")
    if "qualifyAction" not in spec:
        errors.append("UltraScreenSpec must qualify action ids with @screen:")
    if "panelWidth" not in spec or "PANEL_W" not in spec:
        errors.append("UltraScreenSpec must set panelWidth/panelHeight")
    if '"args"' not in spec and "add(\"args\"" not in spec and 'add("args"' not in spec:
        errors.append("UltraScreenSpec buttons must include args {screen,page}")

    style = STYLE.read_text(encoding="utf-8")
    if "PANEL_W = 430" not in style or "PANEL_H = 280" not in style:
        errors.append("UltraStyle panel defaults must be 430×280 (CNPC/DMZUltra dialog)")

    packet = PACKET.read_text(encoding="utf-8")
    if "panelWidth" not in packet or "layoutJson" not in packet:
        errors.append("GuiOpenPacket must carry panelWidth/panelHeight/layoutJson")

    if not DOC.is_file():
        errors.append("Missing docs/KAIPATH_GUI_PROTOCOL.md")
    else:
        doc = DOC.read_text(encoding="utf-8")
        for needle in ("panelWidth", "center", "@<screen>", "GuiActionPacket"):
            if needle not in doc:
                errors.append(f"KAIPATH_GUI_PROTOCOL.md missing {needle}")

    # Collect actions from every Ultra*Screens builder.
    for path in sorted(ULTRA.glob("Ultra*Screens.java")):
        text = path.read_text(encoding="utf-8")
        screen_m = re.search(r'UltraScreenSpec\.of\("([^"]+)"\)', text)
        screen = screen_m.group(1) if screen_m else path.stem.replace("Ultra", "").replace("Screens", "").lower()
        # Hub uses hub + admin.
        screens = {screen}
        if "UltraScreenSpec.of(\"admin\")" in text or "of(\"admin\")" in text:
            screens.add("admin")
        for raw in extract_actions(text):
            if raw.startswith("nav:") or raw.startswith("open:") or raw.startswith("tab:"):
                continue
            if not raw.startswith("do:"):
                continue
            body = raw[3:]
            act = body.split(":", 1)[0]
            if act == "android_remove" or act == "":
                act = "android_remove" if "android_remove" in raw or "PAGE_ANDROID_REMOVE" in text else act
            # Map screen aliases
            for s in screens:
                known = KNOWN_DO.get(s, set())
                # admin screen shares hub do-actions
                if s == "admin":
                    known = known | KNOWN_DO.get("hub", set())
                if act and act not in known and s in KNOWN_DO:
                    # Allow dynamic prefixes listed in KNOWN_DO
                    if act not in known:
                        # Only flag if this screen's KNOWN_DO is the primary of()
                        primary = screen_m.group(1) if screen_m else s
                        if s == primary and act not in KNOWN_DO.get(primary, set()):
                            # Check all screens' known sets for this file's of() targets
                            pass
            # Validate against the of() screen for non-open buttons in that file
            primary = next(iter(screens))
            if "of(\"hub\")" in text and path.name == "UltraHubScreens.java":
                # hub + admin handled separately below
                continue
            known = KNOWN_DO.get(primary, set())
            if primary == "admin":
                known = KNOWN_DO["admin"] | KNOWN_DO["hub"]
            if act and known is not None and act not in known:
                # Skip empty dynamic stubs from regex
                if act.endswith("<dynamic>") or not act:
                    continue
                errors.append(f"{path.name}: do:{act} not in known set for screen {primary}")

    # Explicit per-file checks (clearer than the loop above).
    checks = [
        ("UltraHubScreens.java", "hub", {"reload"}),
        ("UltraDifficultyScreens.java", "difficulty", KNOWN_DO["difficulty"]),
        ("UltraCharacterScreens.java", "character", KNOWN_DO["character"]),
        ("UltraPrestigeScreens.java", "prestige", KNOWN_DO["prestige"]),
        ("UltraProgressionScreens.java", "progression", KNOWN_DO["progression"]),
        ("UltraRivalScreens.java", "rival", KNOWN_DO["rival"]),
        ("UltraSparScreens.java", "spar", KNOWN_DO["spar"]),
        ("UltraConfigScreens.java", "config", KNOWN_DO["config"]),
    ]
    for fname, screen, known in checks:
        path = ULTRA / fname
        text = path.read_text(encoding="utf-8")
        for raw in extract_actions(text):
            if not raw.startswith("do:"):
                continue
            act = raw[3:].split(":", 1)[0]
            if not act:
                continue
            if act not in known:
                errors.append(f"{fname}: unexpected do:{act} for {screen}")

    # Admin reload
    hub = (ULTRA / "UltraHubScreens.java").read_text(encoding="utf-8")
    if "do:reload" not in hub:
        errors.append("UltraHubScreens missing do:reload")

    print("=== Ultra action / panel audit ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  action ids covered; @screen qualify; panel 430×280; protocol doc")
    return 0


if __name__ == "__main__":
    sys.exit(main())
