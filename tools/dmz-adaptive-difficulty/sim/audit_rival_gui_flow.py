#!/usr/bin/env python3
"""Static audit: Rival GUI hub vs Pending board flow (declares vs duels)."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "tools" / "dmz-adaptive-difficulty" / "src" / "main" / "java"
GUI = ROOT / "tools" / "dmz-adaptive-difficulty-gui" / "src" / "main" / "java"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def main() -> int:
    errors: list[str] = []
    cnpc = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/cnpc/CnpcLmRivalGui.java")
    api = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/RivalGuiApi.java")
    chest = read(GUI / "com/dbzlegacy/adaptivedifficulty/bukkit/RivalChestGui.java")
    cmi = read(GUI / "com/dbzlegacy/adaptivedifficulty/bukkit/CmiRivalGui.java")
    bridge = read(GUI / "com/dbzlegacy/adaptivedifficulty/bukkit/ForgeBridge.java")

    def check(name: str, ok: bool) -> None:
        if ok:
            print(f"  OK  {name}")
        else:
            errors.append(name)
            print(f"  FAIL  {name}")

    print("=== Rival GUI flow audit ===")

    # Hub pages must not expose Accept/Decline/Cancel for duels (Pending board only).
    for label, blob in [
        ("CNPC challenge hub", cnpc.split("paintChallenge", 1)[1].split("paintChallengePending", 1)[0]),
        ("Chest challenge hub", chest.split("private Inventory challenge(", 1)[1].split("private Inventory challengePending", 1)[0]),
        ("CMI challenge hub", cmi.split("openChallenge(Player", 1)[1].split("openChallengePending", 1)[0]),
    ]:
        check(
            f"{label} has no challenge accept act",
            'act(player, "challenge", "accept"' not in blob
            and '"challenge", "accept"' not in blob
            and '"challenge_accept"' not in blob,
        )

    check("Forge pendingChallengeCards bridge", "pendingChallengeCards" in bridge and "rivalPendingChallengeCards" in bridge)
    check("API pendingChallengeCards", "pendingChallengeCards" in api and "pending_challenges" in api)
    check("CNPC challenge_pending page", "challenge_pending" in cnpc and "paintChallengePending" in cnpc)
    check("CNPC pending decide routes", "challenge_pending_decide:" in cnpc and "challenge_accept" in cnpc)
    check("Chest challenge_pending pages", "challengePending" in chest and "challenge_pending_decide:" in chest)
    check("CMI challenge_pending pages", "openChallengePending" in cmi and "challenge_pending_decide:" in cmi)

    check(
        "Actions hub: no Remove on hub",
        'page("pick_remove")' not in chest.split("private Inventory actions(", 1)[1].split("private Inventory history", 1)[0]
        and "pick_remove" not in cmi.split("openActions(", 1)[1].split("openHistory", 1)[0],
    )
    check(
        "Actions hub: Pending declares path",
        'page("pending")' in chest and "Pending declares" in chest,
    )
    check(
        "Challenge hub: Pending duels path",
        'page("challenge_pending")' in chest and "Pending duels" in chest,
    )
    check("CNPC scroll labels pendingCardLabels", "pendingCardLabels" in cnpc)
    check("Copy separates declares vs duels", "Pending declares" in cnpc and "Pending duels" in cnpc)

    spar_cnpc = read(SRC / "com/dbzlegacy/adaptivedifficulty/gui/cnpc/CnpcLmSparGui.java")
    dojo = read(SRC / "com/dbzlegacy/adaptivedifficulty/sparring/DojoRankings.java")
    spar_chest = read(GUI / "com/dbzlegacy/adaptivedifficulty/bukkit/SparChestGui.java")

    print("\n=== Dojo war GUI flow audit ===")
    hub = spar_cnpc.split("private static void paintDojoWar(", 1)[1].split("private static void paintDojoWarPending", 1)[0]
    check("CNPC dojo war hub no inline accept/decline",
          'act(player, "dojo_accept"' not in hub and 'act(player, "dojo_decline"' not in hub)
    check("CNPC dojo war hub has pending board",
          "dojo_war_pending" in hub and "Pending wars" in hub and "Declare war" in hub)
    check("rival dojo picker online-only filter", "isOnline(server, masterUuid)" in dojo)
    check("chest dojo pending opens decide for all rows",
          'SlotAction.page("dojo_war_pending_decide:' in spar_chest
          and "dojo_war_cancel" not in spar_chest.split("dojoWarPending", 1)[1].split("dojoWarPendingDecide", 1)[0])

    if errors:
        print(f"\nFAIL: {len(errors)} GUI flow check(s)")
        for e in errors:
            print(f"  - {e}")
        return 1
    print("PASS — rival + dojo war GUI flow")
    return 0


if __name__ == "__main__":
    sys.exit(main())
