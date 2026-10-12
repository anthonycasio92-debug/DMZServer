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
    "hub": {"reload", "skillcheck_denied"},
    "admin": {"reload"},
    "difficulty": {
        "activate",
        "lower_tier",
        "toggle_personal",
        "summon_end_dragon",
        "mode",
        "equip_title",
        "clear_title",
        "toggle_title_sense",
        "toggle_coin_chat",
    },
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
        "challenge_send",
        "spectate",
        "spectate_stop",
        "declare",
        "silent",
        "accept",
        "decline",
        "remove",
    },
    "spar": {
        "tpmsg",
        "end",
        "mentor_tpmsg",
        "mentor",
        "mentor_accept",
        "mentor_decline",
        "mentor_invite",
        "apprentice_invite",
        "mentor_release",
        "admin",
        "dojo_accept",
        "dojo_war_cancel",
        "dojo_challenge",
    },
    "config": {
        "open_module",
        "save_all",
        "discard_all",
        "reload_module",
        "toggle",
        "step",
        "cycle",
        "undo",
    },
    "skillcheck": set(),
    "skills": set(),
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
        "requiresStaff",
        "rejectStaffGated",
        "StaffAccess.isStaff",
        "That menu action is not available.",
    ):
        if needle not in actions_src:
            errors.append(f"UltraGuiActions missing {needle}")
    if "rejectStaffGated(" not in actions_src or actions_src.count("rejectStaffGated(") < 3:
        # declaration + dispatch + dispatchResolved
        errors.append(
            "UltraGuiActions must call rejectStaffGated from dispatch and dispatchResolved"
        )
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
    if "requireValidActionId" not in spec:
        errors.append("UltraScreenSpec must validate action ids via requireValidActionId")
    if "panelWidth" not in spec or "PANEL_W" not in spec:
        errors.append("UltraScreenSpec must set panelWidth/panelHeight")
    if '"args"' not in spec and "add(\"args\"" not in spec and 'add("args"' not in spec:
        errors.append("UltraScreenSpec buttons must include args {screen,page}")

    style = STYLE.read_text(encoding="utf-8")
    if "PANEL_W = 350" not in style or "PANEL_H = 300" not in style:
        errors.append("UltraStyle panel defaults must be 350×300 (compact Ultra dialog)")
    if "BTN_H = 18" not in style or "BTN_GAP = 6" not in style:
        errors.append("UltraStyle button height/gap must be 18 / 6")
    if "BTN_PAD = 8" not in style or "BTN_MIN_W = 80" not in style:
        errors.append("UltraStyle button pad/min width must be 8 / 80")
    if "PANEL_MARGIN = 10" not in style or "PANEL_MAX_H = 380" not in style:
        errors.append("UltraStyle panel margin/max must be 10 / 380")
    if "LINE_H = 16" not in style or "LINE_GAP = 8" not in style:
        errors.append("UltraStyle body line height/gap must be 16 / 8")
    if "BODY_PAD = 10" not in style or "BODY_MIN_H = 140" not in style:
        errors.append("UltraStyle body pad/min height must be 10 / 140")
    if 'TITLE_ALIGN = "center"' not in style:
        errors.append('UltraStyle TITLE_ALIGN must be "center"')
    if "playerModel" not in SPEC.read_text(encoding="utf-8"):
        errors.append("UltraScreenSpec must support optional playerModel in layoutJson")
    if "scrollBody" not in SPEC.read_text(encoding="utf-8"):
        errors.append("UltraScreenSpec must emit scrollBody in layoutJson")

    packet = PACKET.read_text(encoding="utf-8")
    if "panelWidth" not in packet or "layoutJson" not in packet:
        errors.append("GuiOpenPacket must carry panelWidth/panelHeight/layoutJson")

    net = (ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/net/gui/LmGuiNetwork.java").read_text(
        encoding="utf-8"
    )
    if "validatePanelBounds" not in net:
        errors.append("LmGuiNetwork must validate outgoing panelWidth/panelHeight")
    if "panelWidth <= 0" not in net and "w <= 0" not in net:
        errors.append("LmGuiNetwork must warn when panelWidth/panelHeight <= 0")
    if 'PROTOCOL_VERSION = "1"' not in net:
        errors.append("LmGuiNetwork must define PROTOCOL_VERSION = \"1\"")
    if "runPacketRoundTripSelfTest" not in net:
        errors.append("LmGuiNetwork must run packet round-trip self-test at register")
    if "screensTested" not in net:
        errors.append("LmGuiNetwork round-trip self-test must log screensTested")
    proto = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/net/gui/UltraGuiProtocol.java"
    if not proto.is_file():
        errors.append("Missing UltraGuiProtocol (shared <modid>:gui conventions)")
    else:
        proto_text = proto.read_text(encoding="utf-8")
        if 'PROTOCOL_VERSION = "1"' not in proto_text:
            errors.append("UltraGuiProtocol must define PROTOCOL_VERSION = \"1\"")
        if 'CHANNEL_PATH = "gui"' not in proto_text:
            errors.append("UltraGuiProtocol must define CHANNEL_PATH = \"gui\"")
        if "channel(String" not in proto_text and "channel(String modId)" not in proto_text:
            errors.append("UltraGuiProtocol must expose channel(modId) helper")
        if "MSG_FEATURES" not in proto_text:
            errors.append("UltraGuiProtocol must define MSG_FEATURES for GuiFeaturesPacket")
    features_pkt = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/net/gui/GuiFeaturesPacket.java"
    if not features_pkt.is_file():
        errors.append("Missing GuiFeaturesPacket (client capability advertisement)")
    client_feat = ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/net/gui/UltraClientFeatures.java"
    if not client_feat.is_file():
        errors.append("Missing UltraClientFeatures (server-side feature gap diagnostics)")
    else:
        feat_text = client_feat.read_text(encoding="utf-8")
        if "FEATURE_STYLE" not in feat_text or "FEATURE_ACTIVE_TAB" not in feat_text:
            errors.append("UltraClientFeatures must define FEATURE_STYLE and FEATURE_ACTIVE_TAB")
    if "roundTripStyleAndActiveTab" not in net:
        errors.append("LmGuiNetwork must round-trip style + activeTab in self-test")
    if "GuiFeaturesPacket" not in net:
        errors.append("LmGuiNetwork must register GuiFeaturesPacket")

    ids = (ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/ultra/UltraScreenIds.java").read_text(
        encoding="utf-8"
    )
    if "bareOpenTarget" not in ids:
        errors.append("UltraScreenIds must expose bareOpenTarget for zero-config bare hub actions")
    if "requireValidActionId" not in ids:
        errors.append("UltraScreenIds must enforce requireValidActionId")

    # Every Ultra*Screens builder must go through UltraScreenSpec.send (panel fields 8–10).
    for path in sorted(ULTRA.glob("Ultra*Screens.java")):
        text = path.read_text(encoding="utf-8")
        if "UltraScreenSpec" not in text:
            errors.append(f"{path.name}: must build via UltraScreenSpec")
        if ".send(" not in text and "send(player)" not in text:
            errors.append(f"{path.name}: must call UltraScreenSpec.send(player)")
        if "new GuiOpenPacket" in text or "LmGuiNetwork.sendOpen" in text:
            errors.append(f"{path.name}: must not bypass UltraScreenSpec.send")

    if not DOC.is_file():
        errors.append("Missing docs/KAIPATH_GUI_PROTOCOL.md")
    else:
        doc = DOC.read_text(encoding="utf-8")
        for needle in (
            "panelWidth",
            "center",
            "@<screen>",
            "GuiActionPacket",
            "GuiFeaturesPacket",
            "PROTOCOL_VERSION",
            "requireValidActionId",
            "buttonsJson",
            "activeTab",
            "confirm",
            "danger",
            "dim",
            "normal",
            "open:",
            "do:",
            "tab:",
            "playerModel",
            "scrollBody",
            "buttonPad",
            "titleAlign",
            "bodyPad",
        ):
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
        ("UltraHubScreens.java", "hub", KNOWN_DO["hub"]),
        ("UltraDifficultyScreens.java", "difficulty", KNOWN_DO["difficulty"]),
        ("UltraCharacterScreens.java", "character", KNOWN_DO["character"]),
        ("UltraPrestigeScreens.java", "prestige", KNOWN_DO["prestige"]),
        ("UltraProgressionScreens.java", "progression", KNOWN_DO["progression"]),
        ("UltraRivalScreens.java", "rival", KNOWN_DO["rival"]),
        ("UltraSparScreens.java", "spar", KNOWN_DO["spar"]),
        ("UltraConfigScreens.java", "config", KNOWN_DO["config"]),
        ("UltraSkillCheckScreens.java", "skillcheck", KNOWN_DO["skillcheck"]),
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

    # Hub builder must never call footerBackToMain (hub IS the main menu).
    hub = (ULTRA / "UltraHubScreens.java").read_text(encoding="utf-8")
    if "do:reload" not in hub:
        errors.append("UltraHubScreens missing do:reload")
    if ".footerBackToMain()" in hub:
        errors.append(
            "UltraHubScreens must not call .footerBackToMain() "
            "(hub main has no back; admin uses explicit nav:hub)"
        )
    # Hub main packet shape: of("hub") builder path must not emit nav:hub.
    if 'UltraScreenSpec.of("hub")' in hub:
        main_chunk = hub.split('UltraScreenSpec.of("hub")', 1)[1].split("openAdmin", 1)[0]
        if ".footerBackToMain()" in main_chunk or '"nav:hub"' in main_chunk:
            errors.append(
                'UltraHubScreens openMain (of("hub")) must not add nav:hub / .footerBackToMain()'
            )
    # Footers must not ship a Refresh button.
    spec_text = SPEC.read_text(encoding="utf-8")
    for method in ("footerBackToMain", "footerBackHome"):
        # Crude extract: method body until next public method
        m = re.search(
            rf"public UltraScreenSpec {method}\(\)\s*\{{(.*?)\n    \}}",
            spec_text,
            re.S,
        )
        if m is None:
            errors.append(f"UltraScreenSpec.{method} not found")
        elif "nav:refresh" in m.group(1) or "Refresh" in m.group(1):
            errors.append(f"UltraScreenSpec.{method} must not include Refresh / nav:refresh")

    print("=== Ultra action / panel audit ===")
    for e in errors:
        print(f"  FAIL  {e}")
    if errors:
        print(f"\n{len(errors)} error(s)")
        return 1
    print("  OK  action ids covered; @screen qualify; panel 350×300; protocol doc")
    return 0


if __name__ == "__main__":
    sys.exit(main())
