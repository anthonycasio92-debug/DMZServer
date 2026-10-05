#!/usr/bin/env python3
"""Full Legacy Mechanics GUI menu audit — CNPC, chest, CMI, ForgeBridge, and routing.

Writes sim/out/full-gui-menu-audit.md and exits non-zero on blocking failures.
"""
from __future__ import annotations

import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MOD = ROOT / "tools" / "dmz-adaptive-difficulty"
GUI = ROOT / "tools" / "dmz-adaptive-difficulty-gui"
CNPC_DIR = MOD / "src/main/java/com/dbzlegacy/adaptivedifficulty/gui/cnpc"
BUKKIT = GUI / "src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit"
SIM = MOD / "sim"

errors: list[str] = []
warnings: list[str] = []
ok_lines: list[str] = []


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def ok(label: str, cond: bool, detail: str = "") -> None:
    if cond:
        ok_lines.append(label)
        print(f"  OK  {label}" + (f" — {detail}" if detail else ""))
    else:
        msg = label + (f": {detail}" if detail else "")
        errors.append(msg)
        print(f" FAIL {label}" + (f" — {detail}" if detail else ""))


def warn(label: str, cond: bool, detail: str = "") -> None:
    if cond:
        ok_lines.append(label)
        print(f"  OK  {label}")
    else:
        warnings.append(label + (f": {detail}" if detail else ""))
        print(f" WARN {label}" + (f" — {detail}" if detail else ""))


@dataclass(frozen=True)
class MenuSpec:
    key: str
    cnpc: str
    chest: str | None
    cmi: str | None
    forge_bridge: str | None  # ForgeBridge method prefix or handleDo name
    cnpc_route: str  # case label in CnpcLmGui


MENUS: list[MenuSpec] = [
    MenuSpec("hub", "CnpcLmHubGui.java", "HubChestGui.java", "CmiHubGui.java", "hubHandleDo", "hub"),
    MenuSpec("difficulty", "CnpcLmDifficultyGui.java", "DifficultyChestGui.java", "CmiDifficultyGui.java", None, "difficulty"),
    MenuSpec("rival", "CnpcLmRivalGui.java", "RivalChestGui.java", "CmiRivalGui.java", "rivalHandleDo", "rival"),
    MenuSpec("spar", "CnpcLmSparGui.java", "SparChestGui.java", "CmiSparGui.java", "sparHandleDo", "spar"),
    MenuSpec("prestige", "CnpcLmPrestigeGui.java", "PrestigeChestGui.java", "CmiPrestigeGui.java", "prestigeHandleDo", "prestige"),
    MenuSpec("progression", "CnpcLmProgressionGui.java", "ProgressionChestGui.java", "CmiProgressionGui.java", "progressionHandleDo", "progression"),
    MenuSpec("skills", "CnpcLmSkillCheckGui.java", "SkillsChestGui.java", "CmiSkillsGui.java", "skillsHandleDo", "skillcheck"),
    MenuSpec("character", "CnpcLmCharacterGui.java", "CharacterServicesChestGui.java", None, "charHandleDo", "character"),
    MenuSpec("logs", "CnpcLmLogsGui.java", None, None, None, "logs"),
    MenuSpec("admin", "CnpcLmAdminGui.java", None, None, None, "admin"),
]

CHEST_HUB_CMD = 'lmdo lm open hub'
CMI_HUB_PATTERNS = ('lmdo lm open hub', 'GuiNav.cmiHubButton', 'return GuiNav.cmiHubButton')


def chest_has_hub_nav(text: str) -> bool:
    return CHEST_HUB_CMD in text or "GuiNav.hubItem()" in text


def cmi_has_hub_nav(text: str) -> bool:
    return any(p in text for p in CMI_HUB_PATTERNS)


def run_subaudit(script: str) -> tuple[int, str]:
    path = SIM / script
    if not path.is_file():
        return 2, f"missing {script}"
    r = subprocess.run(
        [sys.executable, str(path)],
        cwd=str(SIM),
        capture_output=True,
        text=True,
    )
    out = (r.stdout or "") + (r.stderr or "")
    return r.returncode, out[-4000:] if len(out) > 4000 else out


def main() -> int:
    print("=== Full GUI menu inventory ===")
    router = read(CNPC_DIR.parent / "cnpc/CnpcLmGui.java")
    plugin = read(BUKKIT / "AdaptiveDifficultyGuiPlugin.java")
    bridge = read(BUKKIT / "ForgeBridge.java")
    gui_nav = read(BUKKIT / "GuiNav.java")

    ok("GuiNav hub command", CHEST_HUB_CMD in gui_nav, "central CMI hub routing")

    for spec in MENUS:
        cnpc_path = CNPC_DIR / spec.cnpc
        ok(f"{spec.key}: CNPC class", cnpc_path.is_file(), spec.cnpc)
        if cnpc_path.is_file():
            cnpc_text = read(cnpc_path)
            ok(f"{spec.key}: CNPC open()", "public static void open(" in cnpc_text)
            ok(
                f"{spec.key}: routed in CnpcLmGui",
                f'case "{spec.cnpc_route}"' in router
                or (spec.key == "skills" and '"skills"' in router),
            )

        if spec.chest:
            chest_path = BUKKIT / spec.chest
            ok(f"{spec.key}: chest GUI", chest_path.is_file(), spec.chest)
            chest_text = read(chest_path)
            ok(f"{spec.key}: chest registered", spec.chest.replace(".java", "") in plugin)
            if spec.key != "hub":
                warn(
                    f"{spec.key}: chest hub footer",
                    chest_has_hub_nav(chest_text),
                    "expected lmdo lm open hub or GuiNav.hubItem",
                )
            ok(
                f"{spec.key}: chest avoids bare lm cmd",
                'SlotAction.cmd("lm")' not in chest_text,
            )

        if spec.cmi:
            cmi_path = BUKKIT / spec.cmi
            ok(f"{spec.key}: CMI GUI", cmi_path.is_file(), spec.cmi)
            cmi_text = read(cmi_path)
            ok(f"{spec.key}: CMI available()+open", "static boolean available()" in cmi_text and "static boolean open(" in cmi_text)
            if spec.key != "hub":
                warn(f"{spec.key}: CMI hub via GuiNav", cmi_has_hub_nav(cmi_text))

        if spec.forge_bridge:
            ok(f"{spec.key}: ForgeBridge hook", spec.forge_bridge in bridge)

    print("\n=== Cross-system navigation (hub opens all systems) ===")
    hub_chest = read(BUKKIT / "HubChestGui.java")
    hub_cmi = read(BUKKIT / "CmiHubGui.java")
    for sys in ("difficulty", "rival", "spar", "prestige", "progression", "skillcheck", "character"):
        ok(f"hub chest opens {sys}", f'open("{sys}"' in hub_chest or f'"{sys}"' in hub_chest)
        ok(f"hub CMI opens {sys}", f'"{sys}"' in hub_cmi or f"open {sys}" in hub_cmi.lower())

    print("\n=== Mohist /lmdo lm open routing ===")
    for token in (
        "openHubRespectingConfig",
        "openPrestigeRespectingConfig",
        "openRivalRespectingConfig",
        "openSparRespectingConfig",
        "openCharacterServicesRespectingConfig",
        "openProgressionRespectingConfig",
    ):
        ok(f"plugin {token}", token in plugin)

    print("\n=== Skill Check / Staff Admin access gates ===")
    skill_svc = read(MOD / "src/main/java/com/dbzlegacy/adaptivedifficulty/progression/shop/SkillCheckService.java")
    ok("Skill Check NPC opens for any player",
       "openAtNpc(player)" in skill_svc.split("tryOpenFromNpc", 1)[1].split("trigger21", 1)[0]
       and "canUse(player)" in skill_svc)
    ok("no openFromNpc permission bypass", "openFromNpc" not in skill_svc)
    cnpc_sk = read(CNPC_DIR / "CnpcLmSkillCheckGui.java")
    ok("CNPC skillcheck donator-only open", "SkillCheckService.canUse(player)" in cnpc_sk
       and "StaffAccess.isStaff(player)" not in cnpc_sk.split("public static void open", 1)[1].split("openSkillsAdmin", 1)[0])
    ok("CNPC skills admin staff-only", "openSkillsAdmin" in cnpc_sk and "StaffAccess.isStaff(player)" in cnpc_sk)
    ok("CNPC skillcheck no redundant open-ui button", "Open Skill Check UI" not in cnpc_sk)
    ok("plugin ensureSkillsGuiAccess", "ensureSkillsGuiAccess" in plugin)
    ok("plugin skillcheck lmdo no staff bypass",
       "case \"skillcheck\"" in plugin and "ForgeBridge.isStaff(player)" not in plugin.split("case \"skillcheck\"", 1)[1].split("default ->", 1)[0])
    ok("skills command permission", "permission: difficulty.admin" in read(GUI / "src/main/resources/plugin.yml"))
    ok("skillcheck command permission", "permission: legacymechanics.skillcheck" in read(GUI / "src/main/resources/plugin.yml"))

    print("\n=== Difficulty tier unlock copy (DMZ or Prestige) ===")
    board = read(BUKKIT / "GuiBoardHelper.java")
    ok(
        "tier unlock lore",
        "DMZ {req_level}" in board and "Prestige {req_prestige}" in board,
    )
    ok("ForgeBridge tier req placeholders", "tier_" in bridge and "_req_level" in bridge)

    print("\n=== Prestige Need (regression guard) ===")
    prestige_flow = SIM / "audit_prestige_gui_flow.py"
    if prestige_flow.is_file():
        code, _ = run_subaudit("audit_prestige_gui_flow.py")
        ok("prestige GUI flow sub-audit", code == 0)

    print("\n=== CNPC style sub-audit ===")
    code, out = run_subaudit("audit_cnpc_gui_style.py")
    ok("CNPC GUI style", code == 0)
    if code != 0 and "WARN" in out:
        for line in out.splitlines():
            if "WARN" in line:
                warnings.append(line.strip())

    print("\n=== GUI tooltips sub-audit ===")
    code, _ = run_subaudit("audit_gui_tooltips.py")
    ok("GUI tooltips policy", code == 0)

    print("\n=== GUI ABI (when jars present) ===")
    mods = list((ROOT / "mods").glob("LegacyMechanics-*.jar"))
    plugins = list((ROOT / "plugins").glob("LegacyMechanicsGUI-*.jar"))
    if mods and plugins:
        code, _ = run_subaudit("audit_gui_abi.py")
        ok("Forge↔Bukkit ABI", code == 0)
    else:
        warnings.append("skipped audit_gui_abi.py (jars not in workspace)")

    # Anti-pattern: raw /difficulty do in chest click handlers
    bad_do = re.compile(r'addCommand\s*\(\s*"[^"]*\s+do\s+')
    for chest in BUKKIT.glob("*ChestGui.java"):
        text = read(chest)
        if bad_do.search(text):
            errors.append(f"{chest.name}: uses addCommand with bare 'do' — prefer lmdo")

    out_md = SIM / "out/full-gui-menu-audit.md"
    out_md.parent.mkdir(parents=True, exist_ok=True)
    body = [
        "# Full GUI menu audit",
        "",
        f"- **OK checks:** {len(ok_lines)}",
        f"- **Errors:** {len(errors)}",
        f"- **Warnings:** {len(warnings)}",
        "",
        "## Menus covered",
        "",
        "| System | CNPC | Chest | CMI |",
        "|--------|------|-------|-----|",
    ]
    for spec in MENUS:
        body.append(
            f"| {spec.key} | {spec.cnpc.replace('.java', '')} | "
            f"{spec.chest.replace('.java', '') if spec.chest else '—'} | "
            f"{spec.cmi.replace('.java', '') if spec.cmi else '—'} |"
        )
    body.append("")
    if errors:
        body.append("## Failures\n")
        body.extend(f"- {e}" for e in errors)
        body.append("")
    else:
        body.append("## Result\n\n**PASS** — all blocking menu/system checks passed.\n")
    if warnings:
        body.append("## Warnings\n")
        body.extend(f"- {w}" for w in warnings)
        body.append("")

    out_md.write_text("\n".join(body), encoding="utf-8")
    print(f"\nWrote {out_md.relative_to(ROOT)}")

    print("\n=== Summary ===")
    if errors:
        print(f"FAIL — {len(errors)} error(s), {len(warnings)} warning(s)")
        for e in errors:
            print(f"  - {e}")
        return 1
    print(f"PASS — full GUI menu audit OK ({len(warnings)} warning(s))")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
