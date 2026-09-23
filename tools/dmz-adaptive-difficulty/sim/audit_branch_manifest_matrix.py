#!/usr/bin/env python3
"""Compare owner ship-manifest markers across git refs (branches).

Does not require checkout — uses `git show ref:path`.
Writes sim/out/branch-manifest-matrix.md
"""
from __future__ import annotations

import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]

# Always include main; all origin/cursor/*-c766 refs are discovered from git.
DEFAULT_REFS = [
    "main",
    "origin/main",
]


@dataclass(frozen=True)
class Check:
    id: str
    group: str
    paths: tuple[str, ...]
    needle: str
    forbidden: str | None = None


CHECKS: list[Check] = [
    Check("prestige-held-need", "prestige",
          ("tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigeSystem.java",),
          "heldCountForNeed"),
    Check("prestige-veteran-gate", "prestige",
          ("tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/progression/shop/PrestigeSystem.java",),
          "HELD_GATE_MIN_COMPLETED = 5"),
    Check("prestige-gui-need", "prestige",
          ("tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/gui/ProgressionGuiApi.java",),
          "heldCountForNeed(player)"),
    Check("prestige-no-cap-chest", "prestige",
          ("tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/PrestigeChestGui.java",),
          "PrestigeChestGui", forbidden="prestige.main.cap"),
    Check("skillcheck-no-bypass", "access",
          ("tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/progression/shop/SkillCheckService.java",),
          "canUse(player)", forbidden="openFromNpc"),
    Check("skillcheck-plugin-gate", "access",
          ("tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/AdaptiveDifficultyGuiPlugin.java",),
          "ensureSkillsGuiAccess"),
    Check("rival-challenge-pending", "rival",
          ("tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/gui/RivalGuiApi.java",),
          "pendingChallengeCards"),
    Check("rival-challenge-backend", "rival",
          ("tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/rival/RivalChallengeManager.java",),
          "pendingRequestCards"),
    Check("rival-chest-challenge-pending", "rival",
          ("tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/RivalChestGui.java",),
          "challenge_pending"),
    Check("rival-declare-decide", "rival",
          ("tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/RivalChestGui.java",),
          "pending_decide:"),
    Check("spar-pending-decide", "spar",
          ("tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/SparChestGui.java",),
          "pending_decide:"),
    Check("spar-dojo-war-decide", "spar",
          ("tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/SparChestGui.java",),
          "dojo_war_pending_decide:"),
    Check("spar-no-end-session", "spar",
          ("tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/SparChestGui.java",),
          "private Inventory main", forbidden="spar.main.end_session"),
    Check("cnpc-defer-reopen", "cnpc",
          ("tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/gui/cnpc/CnpcGuiSupport.java",),
          "afterGuiClosed"),
    Check("manifest-doc", "process",
          ("docs/LM_SHIP_MANIFEST.md",),
          "prestige-need"),
    Check("deploy-gate", "process",
          ("scripts/deploy-lm-live.sh",),
          "audit_ship_manifest.py"),
    Check("build-prestige-jar-overlay", "prestige",
          ("tools/dmz-adaptive-difficulty/build.sh",),
          "progression/shop/PrestigeSystem.class"),
    Check("manifest-javap-prestige", "prestige",
          ("tools/dmz-adaptive-difficulty/sim/audit_ship_manifest.py",),
          "heldCountForNeed(net.minecraft.server.level.ServerPlayer)"),
    Check("skillcheck-no-redundant-ui", "access",
          ("tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/gui/cnpc/CnpcLmSkillCheckGui.java",),
          "Natural skills", forbidden="Open Skill Check UI"),
    Check("admin-no-cnpc-import-chest", "access",
          ("tools/dmz-adaptive-difficulty-gui/src/main/java/com/dbzlegacy/adaptivedifficulty/bukkit/HubChestGui.java",),
          "Staff Admin", forbidden="hub.admin.migrate"),
    Check("admin-no-cnpc-import-cnpc", "access",
          ("tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/gui/cnpc/CnpcLmAdminGui.java",),
          "Event log", forbidden="migrate-cnpc"),
    Check("cnpc-flash-widget-ids", "cnpc",
          ("tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/gui/cnpc/CnpcGuiSupport.java",),
          "ID_FLASH_LABEL_BASE"),
    Check("rival-tp-copy", "rival",
          ("tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/gui/cnpc/CnpcLmRivalGui.java",),
          'toggleOn("TP")', forbidden="Teleport msgs"),
    Check("gui-tp-audit-script", "process",
          ("tools/dmz-adaptive-difficulty/sim/audit_gui_tp_copy.py",),
          "use TP instead of Teleport"),
]


def git_show(ref: str, path: str) -> str | None:
    r = subprocess.run(
        ["git", "-C", str(ROOT), "show", f"{ref}:{path}"],
        capture_output=True,
        text=True,
    )
    if r.returncode != 0:
        return None
    return r.stdout


def ref_exists(ref: str) -> bool:
    r = subprocess.run(
        ["git", "-C", str(ROOT), "rev-parse", "--verify", ref],
        capture_output=True,
    )
    return r.returncode == 0


def version_at(ref: str) -> str | None:
    text = git_show(ref, "tools/dmz-adaptive-difficulty/src/main/java/com/dbzlegacy/adaptivedifficulty/AdaptiveDifficultyMod.java")
    if not text:
        return None
    import re
    m = re.search(r'VERSION = "([^"]+)"', text)
    return m.group(1) if m else None


def eval_check(ref: str, chk: Check) -> bool:
    combined = ""
    for p in chk.paths:
        t = git_show(ref, p)
        if t is None:
            return False
        combined += t
    if chk.forbidden and chk.forbidden in combined:
        return False
    return chk.needle in combined


def main() -> int:
    refs = DEFAULT_REFS
    # Include any other origin/cursor/*-c766 LM branches
    r = subprocess.run(
        ["git", "-C", str(ROOT), "branch", "-r"],
        capture_output=True,
        text=True,
    )
    extra = []
    for line in (r.stdout or "").splitlines():
        line = line.strip()
        if not line.startswith("origin/cursor/") or not line.endswith("-c766"):
            continue
        ref = line.replace(" ", "")
        if ref not in refs:
            extra.append(ref)
    refs = refs + sorted(extra)

    rows: dict[str, dict[str, bool]] = {}
    versions: dict[str, str | None] = {}
    missing_refs: list[str] = []

    for ref in refs:
        if not ref_exists(ref):
            missing_refs.append(ref)
            continue
        versions[ref] = version_at(ref)
        rows[ref] = {}
        for chk in CHECKS:
            rows[ref][chk.id] = eval_check(ref, chk)

    scores = {ref: sum(1 for v in rows[ref].values() if v) for ref in rows}

    def version_key(ref: str) -> tuple[int, ...]:
        v = versions.get(ref) or "0"
        try:
            return tuple(int(x) for x in v.split("."))
        except ValueError:
            return (0,)

    best_score = max(scores.values()) if scores else 0
    best_refs = [r for r, s in scores.items() if s == best_score]
    best_ref = max(best_refs, key=version_key) if best_refs else None
    canonical = "main" if "main" in rows else "origin/main"
    main_ref = canonical

    lines = [
        "# Branch manifest matrix",
        "",
        "Owner-request markers from [LM_SHIP_MANIFEST.md](../../../docs/LM_SHIP_MANIFEST.md).",
        "✓ = present at branch tip · ✗ = missing or regressed",
        "",
    ]
    if missing_refs:
        lines.append(f"Missing refs (not fetched): {', '.join(missing_refs)}\n")

    header = "| Branch | LM ver | " + " | ".join(c.id for c in CHECKS) + " | score |"
    sep = "|---|---|" + "|".join(["---"] * len(CHECKS)) + "|---|"
    lines.extend([header, sep])

    for ref in refs:
        if ref not in rows:
            continue
        short = ref.replace("origin/", "")
        ver = versions.get(ref) or "?"
        cells = []
        for chk in CHECKS:
            cells.append("✓" if rows[ref][chk.id] else "✗")
        sc = scores[ref]
        mark = "**" if ref == canonical else ""
        lines.append(f"| {mark}{short}{mark} | {ver} | " + " | ".join(cells) + f" | {sc}/{len(CHECKS)} |")

    lines.append("")
    lines.append("## Interpretation")
    lines.append("")
    if best_ref:
        lines.append(
            f"- **Best tip:** `{best_ref}` ({scores[best_ref]}/{len(CHECKS)}, LM {versions.get(best_ref) or '?'})"
        )
    if main_ref in rows and best_ref:
        main_score = scores[main_ref]
        if main_score < best_score or main_ref != best_ref and scores[main_ref] < scores.get(best_ref, 0):
            failed_vs_best = [c.id for c in CHECKS if rows[best_ref][c.id] and not rows[main_ref][c.id]]
            lines.append(
                f"- **`main` behind best** ({main_score}/{len(CHECKS)}, LM {versions.get(main_ref) or '?'}) "
                f"— merge `{best_ref}` before the next deploy."
            )
            if failed_vs_best:
                lines.append(f"- **`main` missing vs best:** {', '.join(failed_vs_best)}")
    if canonical in rows:
        failed = [c.id for c in CHECKS if not rows[canonical][c.id]]
        if failed:
            lines.append(f"- **`main` fails matrix:** {', '.join(failed)}")
        elif best_ref == canonical:
            lines.append(f"- **`main` matches best** — safe canonical for deploy.")

    lines.append("")
    lines.append("## Branch regressions (vs best tip)")
    lines.append("")
    regressions: list[str] = []
    if best_ref and best_ref in rows:
        for ref in sorted(rows):
            if ref == best_ref:
                continue
            lost = [c.id for c in CHECKS if rows[best_ref][c.id] and not rows[ref][c.id]]
            if lost:
                regressions.append(f"- `{ref}` regressed: missing {lost} (present on `{best_ref}`)")
    if regressions:
        lines.extend(regressions)
    else:
        lines.append("- None — every scanned branch includes all markers from the best tip.")

    lines.append("")
    lines.append("## Extra markers on feature branches (not on main)")
    lines.append("")
    if main_ref in rows:
        main_set = {c.id for c in CHECKS if rows[main_ref][c.id]}
        any_extra = False
        for ref in sorted(rows, key=lambda x: -scores[x]):
            if ref == main_ref:
                continue
            extra_pass = [c.id for c in CHECKS if rows[ref][c.id] and c.id not in main_set]
            if extra_pass:
                any_extra = True
                lines.append(f"- `{ref}`: {extra_pass}")
        if not any_extra:
            lines.append("- None — merge feature branches into `main` if version is ahead.")

    out = ROOT / "tools/dmz-adaptive-difficulty/sim/out/branch-manifest-matrix.md"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(out.read_text(encoding="utf-8"))

    exit_code = 0
    if best_ref and best_ref in rows:
        for ref in rows:
            if ref == best_ref:
                continue
            lost = [c.id for c in CHECKS if rows[best_ref][c.id] and not rows[ref][c.id]]
            if lost:
                print(f"\nFAIL {ref} regressed vs {best_ref}: {lost}", file=sys.stderr)
                exit_code = 1
    if canonical in rows and best_ref:
        if scores[canonical] < scores[best_ref]:
            missing = [c.id for c in CHECKS if rows[best_ref][c.id] and not rows[canonical][c.id]]
            print(f"\nFAIL main behind {best_ref}: {missing}", file=sys.stderr)
            exit_code = 1
        bad = [c.id for c in CHECKS if not rows[canonical][c.id]]
        if bad:
            print(f"\nFAIL main missing matrix checks: {bad}", file=sys.stderr)
            exit_code = 1
    if exit_code == 0:
        print("\nMatrix written; main matches best — no branch regressions.")
    else:
        print("\nMatrix written; FAIL — consolidate branches into main.", file=sys.stderr)
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
