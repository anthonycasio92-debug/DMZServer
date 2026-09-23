#!/usr/bin/env python3
"""Fail-closed: owner-requested LM behaviors must be present before ship/deploy.

See docs/LM_SHIP_MANIFEST.md. Exits non-zero on any missing requirement.
Writes sim/out/ship-manifest-audit.md
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MOD = ROOT / "tools" / "dmz-adaptive-difficulty"
GUI = ROOT / "tools" / "dmz-adaptive-difficulty-gui"
SIM = MOD / "sim"
SRC = MOD / "src/main/java/com/dbzlegacy/adaptivedifficulty"
BUKKIT = GUI / "src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit"

errors: list[str] = []
ok: list[str] = []


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def check(label: str, cond: bool, detail: str = "") -> None:
    if cond:
        ok.append(label)
        print(f"  OK  {label}" + (f" — {detail}" if detail else ""))
    else:
        msg = label + (f": {detail}" if detail else "")
        errors.append(msg)
        print(f" FAIL {label}" + (f" — {detail}" if detail else ""))


def run_py(script: str) -> bool:
    path = SIM / script
    if not path.is_file():
        errors.append(f"missing audit script {script}")
        return False
    r = subprocess.run([sys.executable, str(path)], cwd=str(SIM), capture_output=True, text=True)
    if r.returncode != 0:
        tail = (r.stdout or "") + (r.stderr or "")
        errors.append(f"{script} failed")
        print(tail[-2000:] if len(tail) > 2000 else tail)
        return False
    ok.append(script)
    print(f"  OK  {script}")
    return True


def main() -> int:
    print("=== LM ship manifest (owner requests) ===\n")

    manifest = read(ROOT / "docs/LM_SHIP_MANIFEST.md")
    check("LM_SHIP_MANIFEST.md present", "prestige-need" in manifest and "rival-challenge-pending" in manifest)

    print("\n--- Sub-audits (must PASS) ---")
    for script in (
        "audit_prestige_need_ladder.py",
        "audit_prestige_gui_flow.py",
        "audit_gui_menus_comprehensive.py",
        "audit_gui_tp_copy.py",
    ):
        run_py(script)

    print("\n--- § prestige (source markers) ---")
    prestige = read(SRC / "progression/shop/PrestigeSystem.java")
    gui_api = read(SRC / "gui/ProgressionGuiApi.java")
    check("PrestigeSystem HELD_GATE_MIN_COMPLETED = 5", "HELD_GATE_MIN_COMPLETED = 5" in prestige)
    check("PrestigeSystem heldCountForNeed", "heldCountForNeed" in prestige)
    check("PrestigeSystem veteran needForProgress", "needForProgress" in prestige and "completed >= HELD_GATE_MIN_COMPLETED" in prestige)
    check("ProgressionGuiApi prestige uses heldCountForNeed", "heldCountForNeed(player)" in gui_api)
    chest = read(BUKKIT / "PrestigeChestGui.java")
    cmi = read(BUKKIT / "CmiPrestigeGui.java")
    cnpc = read(SRC / "gui/cnpc/CnpcLmPrestigeGui.java")
    check("no prestige.main.cap on chest", "prestige.main.cap" not in chest)
    check("no Level Cap Breakthrough on chest main", "Level Cap Breakthrough" not in chest.split("private Inventory main", 1)[1].split("private Inventory turnIn", 1)[0] if "private Inventory main" in chest else False)
    check("CNPC prestige no cap button on main", "Level Cap" not in cnpc or '"cap", "breakthrough" -> paintMain' in cnpc)

    print("\n--- § access (skillcheck + staff admin) ---")
    skill_svc = read(SRC / "progression/shop/SkillCheckService.java")
    staff = read(SRC / "util/StaffAccess.java")
    plugin = read(BUKKIT / "AdaptiveDifficultyGuiPlugin.java")
    yml = read(GUI / "src/main/resources/plugin.yml")
    check("SkillCheck NPC requires canUse", "canUse(player)" in skill_svc.split("tryOpenFromNpc", 1)[1])
    check("no openFromNpc bypass", "openFromNpc" not in skill_svc)
    check("hasSkillCheck does not auto-grant staff", "isStaff(player)" not in staff.split("hasSkillCheck", 1)[1].split("hasBukkitIsOp", 1)[0])
    check("ensureSkillsGuiAccess in plugin", "ensureSkillsGuiAccess" in plugin)
    check("skillcheck permission in plugin.yml", "legacymechanics.skillcheck" in yml)
    check("skills staff permission", "permission: difficulty.admin" in yml)
    hub = read(BUKKIT / "HubChestGui.java")
    check("hub Staff Admin tile staff-gated", "ForgeBridge.isStaff(player)" in hub and "Staff Admin" in hub)
    cnpc_admin = read(SRC / "gui/cnpc/CnpcLmAdminGui.java")
    check("admin hub no CNPC import/migrate tile", "hub.admin.migrate" not in hub and "CNPC migrate" not in hub
          and "migrate-cnpc" not in hub.split("private Inventory admin", 1)[1].split("private Inventory", 1)[0]
          if "private Inventory admin" in hub else "hub.admin.migrate" not in hub)
    check("CNPC staff admin no CNPC migrate button", "CNPC data migrate" not in cnpc_admin and "migrate-cnpc" not in cnpc_admin)
    check("hub Skill Check tile donator-gated", "ForgeBridge.hasSkillCheck(player)" in hub)

    print("\n--- § rival ---")
    rival_api = read(SRC / "gui/RivalGuiApi.java")
    rival_mgr = read(SRC / "rival/RivalChallengeManager.java")
    rival_chest = read(BUKKIT / "RivalChestGui.java")
    cmi_rival = read(BUKKIT / "CmiRivalGui.java")
    cnpc_rival = read(SRC / "gui/cnpc/CnpcLmRivalGui.java")
    check("pendingChallengeCards API", "pendingChallengeCards" in rival_api)
    check("pendingRequestCards backend", "pendingRequestCards" in rival_mgr)
    check("chest challenge_pending page", "challenge_pending" in rival_chest)
    check("CMI challenge_pending page", "challenge_pending" in cmi_rival)
    check("CNPC challenge pending paint", "pendingChallengeCards" in cnpc_rival)
    check("rival declare pending_decide", "pending_decide:" in rival_chest and "pending_decide:" in cmi_rival)
    cnpc_rival = read(SRC / "gui/cnpc/CnpcLmRivalGui.java")
    check("rival CNPC TP toggle label", 'toggleOn("TP")' in cnpc_rival and "Teleport msgs" not in cnpc_rival)
    check("rival chest TP toggle label", '"&aTP ON"' in rival_chest and "TP Msg ON" not in rival_chest)
    spar_chest = read(BUKKIT / "SparChestGui.java")
    cnpc_spar = read(SRC / "gui/cnpc/CnpcLmSparGui.java")
    check("spar chest TP toggle label", '"&aTP ON"' in spar_chest and "TP Msg ON" not in spar_chest)
    check("spar CNPC TP toggle label", 'toggleOn("TP")' in cnpc_spar and "teleport" not in cnpc_spar.lower())

    print("\n--- § spar ---")
    spar_chest = read(BUKKIT / "SparChestGui.java")
    cmi_spar = read(BUKKIT / "CmiSparGui.java")
    check("spar pending_decide", "pending_decide:" in spar_chest and "pending_decide:" in cmi_spar)
    check("spar dojo war pending decide", "dojo_war_pending_decide:" in spar_chest)
    spar_main = spar_chest.split("private Inventory main", 1)[1].split("private Inventory top", 1)[0] if "private Inventory main" in spar_chest else ""
    check("spar main no End Session button", "spar.main.end_session" not in spar_main)

    print("\n--- § difficulty ---")
    cnpc_diff = read(SRC / "gui/cnpc/CnpcLmDifficultyGui.java")
    check("CNPC tiers locked when personal OFF", "paintTiersLocked" in cnpc_diff
          and "Turn personal difficulty ON first" in cnpc_diff)
    check("CNPC difficulty uses handleArgNoReopen", "handleArgNoReopen" in cnpc_diff)

    print("\n--- § cnpc ---")
    cnpc_support = read(SRC / "gui/cnpc/CnpcGuiSupport.java")
    check("CNPC afterGuiClosed defer reopen", "afterGuiClosed" in cnpc_support)
    check("CNPC flash notice separate widget ids", "ID_FLASH_LABEL_BASE" in cnpc_support)
    preview_tex = read(SRC / "gui/cnpc/DmzPreviewTexture.java")
    check("CNPC preview race textures (namekian/bio)", "namekian" in preview_tex and "bioandroid" in preview_tex)
    check("PrestigeSystem shrinkNeedFloor early band", "shrinkNeedFloor" in prestige)

    print("\n--- § version ---")
    mod = read(MOD / "src/main/java/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.java")
    toml = read(MOD / "src/main/resources/META-INF/mods.toml")
    ver_m = re.search(r'VERSION = "([^"]+)"', mod)
    ver_t = re.search(r'^version = "([^"]+)"', toml, re.M)
    ver_y = re.search(r"^version:\s*['\"]?([^'\"\n]+)", yml, re.M)
    v_mod = ver_m.group(1) if ver_m else None
    v_toml = ver_t.group(1) if ver_t else None
    v_yml = ver_y.group(1).strip() if ver_y else None
    check("VERSION == mods.toml == plugin.yml", v_mod == v_toml == v_yml, f"{v_mod!r} / {v_toml!r} / {v_yml!r}")
    if v_mod:
        forge_jar = ROOT / "mods" / f"LegacyMechanics-{v_mod}.jar"
        gui_jar = ROOT / "plugins" / f"LegacyMechanicsGUI-{v_mod}.jar"
        check(f"built jars exist for {v_mod}", forge_jar.is_file() and gui_jar.is_file())
        if forge_jar.is_file():
            r = subprocess.run(
                ["javap", "-classpath", str(forge_jar), "-public",
                 "com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem"],
                capture_output=True,
                text=True,
            )
            prestige_abi = (r.stdout or "") + (r.stderr or "")
            check(
                "shipped PrestigeSystem.heldCountForNeed(ServerPlayer)",
                r.returncode == 0 and "heldCountForNeed(net.minecraft.server.level.ServerPlayer)" in prestige_abi,
                "rebuild overlay — ProgressionGuiApi calls method missing from base jar merge",
            )

    out = SIM / "out/ship-manifest-audit.md"
    out.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        "# Ship manifest audit",
        "",
        f"- **OK:** {len(ok)}",
        f"- **Errors:** {len(errors)}",
        "",
    ]
    if errors:
        lines.append("## FAIL\n")
        lines.extend(f"- {e}" for e in errors)
    else:
        lines.append("## Result\n\n**PASS** — all owner manifest checks satisfied.\n")
    lines.append("\nSee [LM_SHIP_MANIFEST.md](../../../docs/LM_SHIP_MANIFEST.md).\n")
    out.write_text("\n".join(lines), encoding="utf-8")
    print(f"\nWrote {out.relative_to(ROOT)}")

    print("\n=== Summary ===")
    if errors:
        print(f"FAIL — {len(errors)} error(s). Do not deploy.")
        for e in errors:
            print(f"  - {e}")
        return 1
    print(f"PASS — ship manifest OK ({len(ok)} checks)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
