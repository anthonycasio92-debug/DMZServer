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
        "reload",
        "syslog",
        "weight_quote",
        "weight_buy",
    },
    "character": {
        "reskin_confirm",
        "race_confirm",
        "class_confirm",
        "bone_race_default",
        "bone_toggle",
        "bone_unlock",
        "bone_equip",
        "bone_unequip",
        "bone_preview",
        "bone_restore",
        "saga_confirm",
        "saga_reset",
        "android_remove",
        "android",
    },
    "prestige": {
        "confirm",
        "turnin",
        "skill",
        "tier",
        "majin",
        "mutant",
        "unmajin",
        "unmutant",
    },
    "progression": {"boost", "toggle_staff_free_coins"},
    "rival": {
        "tpmsg",
        "instinct",
        "accept_replace",
        "cancel_mutual_replace",
        "admin",
        "challenge",
        "challenge_accept",
        "challenge_decline",
        "challenge_cancel",
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
        "set",
        "add",
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
    if "PANEL_MARGIN = 10" not in style or "PANEL_MAX_H = 420" not in style:
        errors.append("UltraStyle panel margin/max must be 10 / 420")
    if "LINE_H = 16" not in style or "LINE_GAP = 8" not in style:
        errors.append("UltraStyle body line height/gap must be 16 / 8")
    if "BODY_PAD = 10" not in style or "BODY_MIN_H = 140" not in style:
        errors.append("UltraStyle body pad/min height must be 10 / 140")
    if 'TITLE_ALIGN = "center"' not in style:
        errors.append('UltraStyle TITLE_ALIGN must be "center"')
    if "fitAsciiLabel" not in style or "CHAR_W_EST" not in style:
        errors.append("UltraStyle must pre-fit labels (fitAsciiLabel) so KaiPath fitLabel never runs")
    if "fitAsciiLabel" not in (SPEC.read_text(encoding="utf-8")):
        errors.append("UltraScreenSpec must fitAsciiLabel tab/button labels before wire")
    char_tabs = (ULTRA / "UltraCharacterScreens.java").read_text(encoding="utf-8")
    if "android|Android Removal" in char_tabs or "saga|Saga Reset" in char_tabs:
        errors.append(
            "UltraCharacterScreens tabs must stay short (Home/Saga/Android) — long labels mojibake on KaiPath"
        )
    for const in ("STYLE_NORMAL", "STYLE_CONFIRM", "STYLE_DANGER", "STYLE_DIM"):
        if f"{const} =" not in style:
            errors.append(f"UltraStyle must define {const} for button wire styles")
    spec_text_full = SPEC.read_text(encoding="utf-8")
    if "playerModel" not in spec_text_full:
        errors.append("UltraScreenSpec must support optional playerModel in layoutJson")
    parts_helper = ULTRA / "PlayerModelParts.java"
    char_screens = (ULTRA / "UltraCharacterScreens.java").read_text(encoding="utf-8")
    if not parts_helper.is_file():
        errors.append("PlayerModelParts helper must exist for playerModel.parts snapshots")
    else:
        parts_text = parts_helper.read_text(encoding="utf-8")
        if "SCHEMA = 1" not in parts_text or "headBones" not in parts_text:
            errors.append("PlayerModelParts schema 1 must serialize headBones (+ form/extras)")
        if 'addProperty("parts"' not in spec_text_full and 'pm.add("parts"' not in spec_text_full:
            errors.append("UltraScreenSpec must emit optional playerModel.parts")
    if "playerModel(player, 248, 60," not in char_screens and ".playerModel(player, 248, 60," not in char_screens:
        # allow either chained form
        if "248, 60, 86, 120" not in char_screens:
            errors.append(
                "UltraCharacterScreens playerModel y must be 60 (below tab strip; 36 clips head)"
            )
    if "scrollBody" not in spec_text_full:
        errors.append("UltraScreenSpec must emit scrollBody in layoutJson")
    if "buttonShift" not in spec_text_full or "targetBodyH" not in spec_text_full:
        errors.append(
            "UltraScreenSpec must grow panel toward PANEL_MAX_H and shift buttons for dense body text"
        )
    if 'titleAlign' not in spec_text_full or "TITLE_ALIGN" not in spec_text_full:
        errors.append("UltraScreenSpec must emit layoutJson.titleAlign from UltraStyle.TITLE_ALIGN")
    if "textField" not in spec_text_full or "inputsJson" not in PACKET.read_text(encoding="utf-8"):
        errors.append("UltraScreenSpec/GuiOpenPacket must support textField → inputsJson (field 11)")
    if "bindInput" not in spec_text_full:
        errors.append("UltraScreenSpec buttons must support args.bindInput for text submit")
    config_screens = (ULTRA / "UltraConfigScreens.java").read_text(encoding="utf-8")
    number_block = config_screens.split("case NUMBER")[1].split("case ENUM")[0] if "case NUMBER" in config_screens else ""
    enum_block = config_screens.split("case ENUM")[1].split("case LIST")[0] if "case ENUM" in config_screens else ""
    list_block = (
        config_screens.split("case LIST, MAP, GROUP")[1].split("case TEXT")[0]
        if "case LIST, MAP, GROUP" in config_screens
        else ""
    )
    if "textFieldRow" not in number_block:
        errors.append("UltraConfigScreens NUMBER detail must offer textFieldRow (typed value), not only +/- steps")
    if "textFieldRow" not in enum_block:
        errors.append("UltraConfigScreens ENUM detail must offer textFieldRow + Apply")
    if "textFieldRow" not in list_block or "do:add:" not in list_block or "buttonBound" not in list_block:
        errors.append(
            "UltraConfigScreens LIST/MAP/GROUP detail must offer textFieldRow + buttonBound Apply (do:add)"
        )
    if 'dim("Back")' in list_block:
        errors.append("UltraConfigScreens LIST/MAP/GROUP must not stack a Back button (use Entries + one Back)")
    if "typedValueFromArgs" not in config_screens:
        errors.append("UltraConfigScreens set/add must log and resolve typed args via typedValueFromArgs")
    bridge_gaps = ROOT.parent.parent / "docs" / "KAIPATH_BRIDGE_GAPS.md"
    if not bridge_gaps.is_file():
        # ROOT = .../tools/dmz-adaptive-difficulty
        bridge_gaps = Path(__file__).resolve().parents[3] / "docs" / "KAIPATH_BRIDGE_GAPS.md"
    if not bridge_gaps.is_file():
        errors.append("docs/KAIPATH_BRIDGE_GAPS.md must document LM vs KaiPath gap matrix")
    if 'STYLE_DIM' not in (ultra_hub := (ULTRA / "UltraHubScreens.java").read_text(encoding="utf-8")):
        errors.append("UltraHubScreens must wire STYLE_DIM for unavailable system tiles")
    if "skillcheck_denied" not in ultra_hub or "STYLE_DIM" not in ultra_hub:
        errors.append("Hub Skill Check denied tile must send STYLE_DIM")

    packet = PACKET.read_text(encoding="utf-8")
    if "panelWidth" not in packet or "layoutJson" not in packet:
        errors.append("GuiOpenPacket must carry panelWidth/panelHeight/layoutJson")
    if 'titleAlign\\":\\"center' not in packet and 'titleAlign":"center' not in packet:
        errors.append('GuiOpenPacket.defaultLayoutJson must include "titleAlign":"center"')
    if "sanitizeLayoutJson" not in packet:
        errors.append("GuiOpenPacket must sanitize layoutJson (force titleAlign, never mid-JSON trim)")
    if "LAYOUT_JSON_MAX = 4096" not in packet and "LAYOUT_JSON_MAX=4096" not in packet:
        errors.append("GuiOpenPacket LAYOUT_JSON_MAX must be 4096")
    if "inputsJson" not in packet:
        errors.append("GuiOpenPacket must carry inputsJson (field 11)")
    mech = (
        ROOT
        / "src/main/java/com/dbzlegacy/adaptivedifficulty/command/MechanicsCommands.java"
    ).read_text(encoding="utf-8")
    if "§" in mech:
        errors.append("MechanicsCommands must not contain bare § literals — use UltraStyle")

    net = (ROOT / "src/main/java/com/dbzlegacy/adaptivedifficulty/net/gui/LmGuiNetwork.java").read_text(
        encoding="utf-8"
    )
    if 'o.addProperty("titleAlign", UltraStyle.TITLE_ALIGN)' not in net:
        errors.append("LmGuiNetwork.withLogicalSystem must force layoutJson.titleAlign=center")
    if 'titleAlign\\":\\"center' not in net and '"titleAlign":"center"' not in net:
        errors.append("LmGuiNetwork.withLogicalSystem catch fallback must include titleAlign=center")
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
        for needle in (
            "FEATURE_STYLE",
            "FEATURE_ACTIVE_TAB",
            "FEATURE_TITLE_ALIGN",
            "FEATURE_PLAYER_MODEL",
            "FEATURE_TEXT_INPUT",
        ):
            if needle not in feat_text:
                errors.append(f"UltraClientFeatures must define {needle}")
        if "titleAlign" not in feat_text or "playerModel" not in feat_text:
            errors.append(
                "UltraClientFeatures impact/missing lists must cover titleAlign + playerModel"
            )
        if "textInput" not in feat_text:
            errors.append("UltraClientFeatures must cover textInput (inputsJson)")
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
        ("UltraWeightShopScreens.java", "difficulty", KNOWN_DO["difficulty"]),
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
    # Full Ultra menu UI shape audit (stacked Back / Cancel+footer / non-ASCII / unbound fields).
    ui_audit = ROOT / "sim" / "audit_ultra_ui_menus.py"
    if ui_audit.is_file():
        import subprocess

        r = subprocess.run(
            [sys.executable, str(ui_audit)],
            cwd=str(ROOT),
            capture_output=True,
            text=True,
        )
        if r.returncode != 0:
            errors.append("audit_ultra_ui_menus.py failed (broken Ultra menu UI patterns)")
            if r.stdout:
                for line in r.stdout.strip().splitlines()[-12:]:
                    errors.append("  " + line)
    else:
        errors.append("missing sim/audit_ultra_ui_menus.py")

    # Wire text must sanitize fancy punctuation (Â· / â€ mojibake) and Back must not use "<".
    style_src = (ULTRA / "UltraStyle.java").read_text(encoding="utf-8")
    if "wireText(" not in style_src:
        errors.append("UltraStyle.wireText must sanitize Ultra wire copy to ASCII (+ section-sign)")
    if 'BACK = LABEL + "< Back"' in style_src or 'BACK = LABEL + "<Back"' in style_src:
        errors.append('UltraStyle.BACK must not use a leading "<" (renders as garbage on some clients)')
    if 'BACK = LABEL + "Back"' not in style_src:
        errors.append('UltraStyle.BACK should be LABEL + "Back"')
    spec_src = (ULTRA / "UltraScreenSpec.java").read_text(encoding="utf-8")
    if "UltraStyle.wireText" not in spec_src:
        errors.append("UltraScreenSpec must run UltraStyle.wireText on lines/titles before send")

    # Weight shop must accept up to MAX_WEIGHT_VALUE digits (not a stale 4-digit / 9999 cap).
    weight_shop = (ULTRA / "UltraWeightShopScreens.java").read_text(encoding="utf-8")
    if "MAX_WEIGHT_VALUE = 100_000" not in weight_shop and "MAX_WEIGHT_VALUE = 100000" not in weight_shop:
        errors.append("UltraWeightShopScreens.MAX_WEIGHT_VALUE must be 100000")
    if "s.length() > 4" in weight_shop or "length() > 4" in weight_shop:
        errors.append("UltraWeightShopScreens.parseWeightValue must not hard-cap at 4 digits (9999)")
    if "MAX_WEIGHT_DIGITS" not in weight_shop:
        errors.append("UltraWeightShopScreens must define MAX_WEIGHT_DIGITS from MAX_WEIGHT_VALUE")
    # Dual-back OK when contextual names a destination; forbid generic "Back" + footer.
    for path in sorted(ULTRA.glob("Ultra*Screens.java")):
        src = path.read_text(encoding="utf-8")
        for m in re.finditer(
            r"(?:private|public|static)\s+[^{;]+?\b(open\w*)\s*\([^)]*\)\s*\{",
            src,
        ):
            name = m.group(1)
            i = src.find("{", m.end() - 1)
            depth = 0
            end = i
            for j in range(i, len(src)):
                if src[j] == "{":
                    depth += 1
                elif src[j] == "}":
                    depth -= 1
                    if depth == 0:
                        end = j + 1
                        break
            body = src[m.start() : end]
            if "footerBackHome()" not in body and "footerBackToMain()" not in body:
                continue
            # Generic exact "Back" next to footer = same-feeling duplicate.
            if re.search(r'(?:dim|confirm|danger)\(\s*"Back"\s*\)', body):
                errors.append(
                    f'{path.name}.{name}: dim/confirm("Back") with footer - name the destination '
                    "or drop the contextual Back"
                )
            if "UltraStyle.BACK" in body and "UltraStyle.backTo(" not in body:
                # BACK const + footer without a named backTo nearby is a duplicate hub exit.
                errors.append(
                    f"{path.name}.{name}: UltraStyle.BACK + footerBack* - use UltraStyle.backTo("
                    '"Destination") for contextual exits'
                )
            # True duplicate: page:main Back next to footerBackHome (same destination often).
            if 'dim("Back")' in body or "dim(\"Back\")" in body:
                errors.append(f"{path.name}.{name}: dim(\"Back\") with footer - keep one Back")

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

    # Tabs are hub-style buttons under the title line; tabsJson stays empty for every screen.
    self_test_path = ULTRA / "UltraScreenSelfTest.java"
    self_test = self_test_path.read_text(encoding="utf-8")
    if "TABLESS_SCREEN_IDS" not in self_test or "isTablessScreen" not in self_test:
        errors.append(
            "UltraScreenSelfTest must define TABLESS_SCREEN_IDS / isTablessScreen "
            "(skillcheck + skills must not call .tabs())"
        )
    else:
        for sid in ("skillcheck", "skills"):
            if f'"{sid}"' not in self_test.split("TABLESS_SCREEN_IDS", 1)[1].split(";", 1)[0]:
                errors.append(f"UltraScreenSelfTest.TABLESS_SCREEN_IDS missing {sid}")
    if "tabsJson must be empty" not in self_test and "tabs are buttons under the title" not in self_test:
        errors.append(
            "UltraScreenSelfTest.validatePacket must require empty tabsJson "
            "(tabs render as buttons under the title line)"
        )
    if "appendTabButtons" not in spec_text_full or "tabsUnderTitle" not in spec_text_full:
        errors.append(
            "UltraScreenSpec must emit tabsUnderTitle + appendTabButtons "
            "(hub-like tabs under the header line)"
        )
    prestige = (ULTRA / "UltraPrestigeScreens.java").read_text(encoding="utf-8")
    if "openShopHub" not in prestige or "openTiers" not in prestige or "openEffects" not in prestige:
        errors.append("UltraPrestigeScreens must expose shop hub + Skills/Tiers/Effects submenus")
    if "page:skills" not in prestige or "page:tiers" not in prestige or "page:effects" not in prestige:
        errors.append("UltraPrestigeScreens shop hub must link page:skills / page:tiers / page:effects")
    if "skills_p" not in prestige or "skillsPageId" not in prestige:
        errors.append("UltraPrestigeScreens skill shop must paginate via skills_pN page ids")
    if 'buttonLeft("open:prestige"' in prestige or 'buttonRight("tab:shop"' in prestige:
        errors.append(
            "UltraPrestigeScreens must not use open:prestige / tab:shop for skill-shop Prev/Next "
            "(those reopen Home / page 0)"
        )
    if re.search(r'"page:skills:\d+"', prestige):
        errors.append(
            "UltraPrestigeScreens must not emit page:skills:N buttons "
            "(use page:skills_pN - nested colons are fragile)"
        )
    if "do:tier:" not in prestige or "do:majin" not in prestige or "do:mutant" not in prestige:
        errors.append("UltraPrestigeScreens must wire do:tier / do:majin / do:mutant favor buys")
    if 'labelValue("Lifetime prestiges"' not in prestige and "Lifetime prestiges" not in prestige:
        errors.append('UltraPrestigeScreens must label wallet "Lifetime prestiges" (not Completed)')
    if "Completed prestiges" in prestige:
        errors.append('UltraPrestigeScreens must not use "Completed prestiges"')
    difficulty_titles = (ULTRA / "UltraDifficultyScreens.java").read_text(encoding="utf-8")
    if "titles_p" not in difficulty_titles or "TITLES_PER_PAGE" not in difficulty_titles:
        errors.append("UltraDifficultyScreens must paginate titles via titles_pN (no hard 10-cap drop)")
    if re.search(r"placed\s*>=\s*10", difficulty_titles):
        errors.append("UltraDifficultyScreens must not hard-drop titles after placed >= 10")
    # openShop historically stacked "page:main" Back + footer (same destination).
    shop_m = re.search(
        r"static void openShop\b.*?^\s*static void ",
        weight_shop,
        re.S | re.M,
    )
    shop_body = shop_m.group(0) if shop_m else ""
    if '"page:main"' in shop_body and "footerBackHome()" in shop_body:
        errors.append(
            "UltraWeightShopScreens.openShop must not stack page:main Back with footerBackHome"
        )
    rival = (ULTRA / "UltraRivalScreens.java").read_text(encoding="utf-8")
    if 'confirm("Reload")' in rival and 'confirm("Reload data")' not in rival:
        errors.append('UltraRivalScreens staff admin must label refresh "Reload data"')
    # Challenge Accept/Decline only via placePerChallengeButtons on Challenge tab.
    home_m = re.search(
        r"private static void openHome\b.*?^\s*private static void ",
        rival,
        re.S | re.M,
    )
    actions_m = re.search(
        r"private static void openActions\b.*?^\s*private static void ",
        rival,
        re.S | re.M,
    )
    home_body = home_m.group(0) if home_m else ""
    actions_body = actions_m.group(0) if actions_m else ""
    if "challenge_accept" in home_body or "challenge_decline" in home_body:
        errors.append("UltraRivalScreens.openHome must not duplicate challenge Accept/Decline")
    if "challenge_accept" in actions_body or "challenge_decline" in actions_body:
        errors.append("UltraRivalScreens.openActions must not duplicate challenge Accept/Decline")
    if "placePerChallengeButtons" not in rival or "challenge_accept" not in rival:
        errors.append("UltraRivalScreens must keep per-challenge Accept on Challenge via placePerChallengeButtons")
    detail_m = re.search(
        r"private static void openListDetail\b.*?^\s*private static void ",
        rival,
        re.S | re.M,
    )
    detail = detail_m.group(0) if detail_m else ""
    if "displayPickerArg" not in detail:
        errors.append("UltraRivalScreens.openListDetail must resolve Target via RivalGuiApi.displayPickerArg")
    list_m = re.search(
        r"private static void openList\b.*?^\s*private static void ",
        rival,
        re.S | re.M,
    )
    list_block = list_m.group(0) if list_m else ""
    if "displayPickerArg" not in list_block:
        errors.append("UltraRivalScreens.openList must resolve button labels via RivalGuiApi.displayPickerArg")
    if "lastIndexOf(':')" in list_block or "substring(arg.lastIndexOf" in list_block:
        errors.append("UltraRivalScreens.openList must not substring uuid: args for button labels")
    replace_m = re.search(
        r"private static void openReplacePick\b.*?^\s*private static void ",
        rival,
        re.S | re.M,
    )
    replace_block = replace_m.group(0) if replace_m else ""
    if "labeledArgs" not in replace_block and (
        "displayPickerArg" not in replace_block or '+ "|"' not in replace_block
    ):
        errors.append(
            "UltraRivalScreens.openReplacePick must resolve Mutual names via labeledArgs/displayPickerArg"
        )
    if "replace_confirm:" not in replace_block and "replace_confirm:" not in rival:
        errors.append("UltraRivalScreens mutual drop must go through replace_confirm before accept_replace")
    if 'placeNameButtons(spec, "accept_replace", RivalGuiApi.currentRivalArgs' in rival:
        errors.append(
            "UltraRivalScreens.openReplacePick must not pass raw currentRivalArgs to placeNameButtons"
        )
    if 'placeNameButtons(spec, "accept", incoming' in rival or 'placeNameButtons(spec, "decline", incoming' in rival:
        errors.append(
            "UltraRivalScreens.openRequests must label incoming declares via displayPickerArg "
            "(not raw placeNameButtons)"
        )
    if "onlinePlayerNames" in rival and "onlinePlayerArgs" not in rival:
        errors.append("UltraRivalScreens player pickers must use onlinePlayerArgs (UUID), not names only")
    if "PICK_PER_PAGE" not in rival and "pick_declare:" not in rival:
        errors.append("UltraRivalScreens player pickers must paginate (pick_*:N)")
    if 'do:challenge:accept"' in rival or "do:challenge:accept\"" in rival:
        # Generic challenge accept without a counterparty is ambiguous when multiple pending.
        if "challenge_accept:" not in rival:
            errors.append("UltraRivalScreens must use per-challenge do:challenge_accept:uuid (not generic)")
    if "do:challenge:accept" in rival and "challenge_accept:" not in rival:
        errors.append("UltraRivalScreens must wire per-challenge Accept via challenge_accept")
    if "hasIncomingDeclareFrom" not in rival:
        errors.append("UltraRivalScreens declare picker must Offer Accept when they already declared you")
    if "Accept their declare" not in rival or "accept_confirm:" not in rival:
        errors.append(
            "UltraRivalScreens must confirm Accept their declare (not silent Declare→Accept)"
        )
    if "Silent (private)" not in rival and "Tiers: Silent" not in rival:
        errors.append("UltraRivalScreens Home must explain Silent/Declared/Mutual/Nemesis tiers")
    if "cancel_mutual_replace" not in rival or "replace_confirm:" not in rival:
        errors.append("UltraRivalScreens mutual swap needs Decline instead + replace_confirm step")
    if "demote" not in rival.lower() or "Mutual to Declared" not in rival:
        errors.append("UltraRivalScreens replace confirm must warn Mutual→Declared demotion")
    if "needsMutualReplacePick" not in (ULTRA / "UltraGuiActions.java").read_text(encoding="utf-8"):
        errors.append("UltraGuiActions must reopen pick_replace_mutual after Accept hits the slot cap")
    config = (ULTRA / "UltraConfigScreens.java").read_text(encoding="utf-8")
    review_m = re.search(
        r"private static void openReview\b.*?^\s*(?:public|private) static ",
        config,
        re.S | re.M,
    )
    review = review_m.group(0) if review_m else ""
    if 'buttonLeft("do:save_all"' in review and 'buttonRight("do:discard_all"' in review:
        errors.append("UltraConfigScreens.openReview must stack Confirm save / Discard all (not side-by-side)")
    typed_m = re.search(
        r"private static String typedValueFromArgs\b.*?^\s*(?:public|private) static ",
        config,
        re.S | re.M,
    )
    typed_block = typed_m.group(0) if typed_m else ""
    if "LOGGER.info(" in typed_block:
        errors.append("UltraConfigScreens.typedValueFromArgs must log at DEBUG (not INFO)")
    if "Typed fields need KaiPath textInput" in config:
        errors.append("UltraConfigScreens player copy must not mention protocol field textInput")
    weight = (ULTRA / "UltraWeightShopScreens.java").read_text(encoding="utf-8")
    if "GuiOpen field 11" in weight or "field 11 / textInput" in weight:
        errors.append("UltraWeightShopScreens player copy must not mention GuiOpen field 11 / textInput")
    spar = (ULTRA / "UltraSparScreens.java").read_text(encoding="utf-8")
    mentor_m = re.search(
        r"private static void openPendingDecide\b.*?^\s*private static void ",
        spar,
        re.S | re.M,
    )
    mentor = mentor_m.group(0) if mentor_m else ""
    if 'buttonLeft("do:mentor_accept:' in mentor and 'buttonRight("do:mentor_decline:' in mentor:
        errors.append("UltraSparScreens mentor Accept/Decline must stack vertically (misclick risk)")
    dojo_m = re.search(
        r"private static void openDojoWarDecide\b.*?^\s*private static void ",
        spar,
        re.S | re.M,
    )
    dojo = dojo_m.group(0) if dojo_m else ""
    if 'buttonLeft("do:dojo_accept:' in dojo and 'buttonRight("do:dojo_war_cancel:' in dojo:
        errors.append("UltraSparScreens dojo war Accept/Cancel must stack vertically (misclick risk)")
    if "print status" in spar:
        errors.append('UltraSparScreens admin must say "show status" not "print status"')
    hub = (ULTRA / "UltraHubScreens.java").read_text(encoding="utf-8")
    admin_tile = re.search(r'new HubTile\(\s*"admin"[\s\S]*?true\)', hub)
    if not admin_tile or "STYLE_DANGER" not in admin_tile.group(0):
        errors.append("UltraHubScreens Staff Admin tile must use STYLE_DANGER (red)")
    # Hub: yellow/confirm means selected — nothing is selected on the hub.
    open_main = hub.split("private static void openMain", 1)[-1].split(
        "private static void openAdmin", 1
    )[0]
    for system in ("difficulty", "skillcheck", "skills", "character", "rival", "spar", "prestige"):
        tile = re.search(rf'new HubTile\(\s*"{system}"[\s\S]*?true\)', open_main)
        if not tile:
            tile = re.search(rf'new HubTile\(\s*"{system}"[\s\S]*?false\)', open_main)
        if tile and "STYLE_CONFIRM" in tile.group(0):
            errors.append(
                f'UltraHubScreens "{system}" tile must not use STYLE_CONFIRM on the hub'
            )
    # Tab styles: active=confirm, inactive=dim, staff/admin=danger.
    spec_src = (ULTRA / "UltraScreenSpec.java").read_text(encoding="utf-8")
    if "tabStyle(" not in spec_src or "staffOnly()" not in spec_src:
        errors.append("UltraScreenSpec must style staff tabs danger via tabStyle/staffOnly")
    if "STYLE_CONFIRM" not in spec_src or "STYLE_DIM" not in spec_src:
        errors.append("UltraScreenSpec tab styles must keep confirm (active) and dim (inactive)")
    # Rival Challenge/Actions Accept+Decline must stack (same misclick risk as spar mentor/dojo).
    if re.search(
        r'buttonLeft\("do:challenge:accept"[^\n]*\n[^\n]*buttonRight\("do:challenge:decline"',
        rival,
    ):
        errors.append(
            "UltraRivalScreens Accept/Decline challenge must stack vertically (not side-by-side)"
        )
    difficulty = (ULTRA / "UltraDifficultyScreens.java").read_text(encoding="utf-8")
    for label, text in (
        ("UltraDifficultyScreens", difficulty),
        ("UltraWeightShopScreens", weight),
    ):
        if "free for staff" in text or "free (staff)" in text:
            errors.append(f'{label} must capitalize staff free cost as "Free ..."')
    if "TAB_ROW_Y" not in style:
        errors.append("UltraStyle must define TAB_ROW_Y for tabs under the title line")
    skill_ui = (ULTRA / "UltraSkillCheckScreens.java").read_text(encoding="utf-8")
    if ".tabs(" in skill_ui:
        errors.append("UltraSkillCheckScreens must not call .tabs() (no tab bar)")

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
