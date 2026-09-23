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

# Refs to scan (newest consolidated first).
DEFAULT_REFS = [
    "origin/cursor/rival-spar-merge-deploy-c766",
    "origin/main",
    "origin/cursor/skillcheck-donator-gates-c766",
    "origin/cursor/prestige-menu-need-fix-c766",
    "origin/cursor/rival-challenge-pending-c766",
    "origin/cursor/dojo-war-rankings-fix-c766",
    "origin/cursor/difficulty-teams-dragon-c766",
    "origin/cursor/cnpc-menu-polish-c766",
    "origin/cursor/ui-humanize-scroll-c766",
    "origin/cursor/ad-post-overhaul-stats-c766",
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

    # Best ref = most checks pass
    scores = {ref: sum(1 for v in rows[ref].values() if v) for ref in rows}
    best_ref = max(scores, key=scores.get) if scores else None
    canonical = "origin/cursor/rival-spar-merge-deploy-c766"
    main_ref = "origin/main"

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
        lines.append(f"- **Highest score:** `{best_ref}` ({scores[best_ref]}/{len(CHECKS)})")
    if main_ref in rows:
        main_score = scores[main_ref]
        if main_ref != best_ref:
            lines.append(f"- **`main` is incomplete** ({main_score}/{len(CHECKS)}) — merge `{canonical}` (or equivalent) before the next deploy.")
    if canonical in rows:
        canon_score = scores[canonical]
        failed = [c.id for c in CHECKS if not rows[canonical][c.id]]
        if failed:
            lines.append(f"- **Canonical branch missing:** {', '.join(failed)}")
        else:
            lines.append(f"- **Canonical branch `{canonical}` passes all matrix checks** — this matches live **4.5.69** intent.")

    # Branches with unique partial features (score between main and best)
    lines.append("")
    lines.append("## Branches with extra work not in canonical")
    lines.append("")
    if canonical in rows:
        canon_set = {c.id for c in CHECKS if rows[canonical][c.id]}
        for ref in sorted(rows, key=lambda x: -scores[x]):
            if ref == canonical:
                continue
            extra_pass = [c.id for c in CHECKS if rows[ref][c.id] and c.id not in canon_set]
            if extra_pass:
                lines.append(f"- `{ref}`: has {extra_pass} not in canonical (investigate merge)")

    out = ROOT / "tools/dmz-adaptive-difficulty/sim/out/branch-manifest-matrix.md"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(out.read_text(encoding="utf-8"))

    # Fail if canonical branch doesn't pass all
    if canonical in rows:
        bad = [c.id for c in CHECKS if not rows[canonical][c.id]]
        if bad:
            print(f"\nFAIL canonical branch missing: {bad}", file=sys.stderr)
            return 1
    print("\nMatrix written; canonical branch OK.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
