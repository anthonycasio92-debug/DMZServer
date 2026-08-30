/*
 * ============================================================
 * LegacyMechanics — Skill Check CNPC (interact)
 * ============================================================
 * Setup (CustomNPCs):
 *   1. Edit NPC → Advanced → Scripts
 *   2. Enable scripts, language: ECMAScript / JavaScript
 *   3. Paste this file into the NPC script (or load as Script File)
 *   4. Install LegacyMechanics-2.3.47+ (Forge soft-match no longer steals this click)
 *
 * Recommended (either works):
 *   - NPC display name contains "Skill Check" / "SkillCheck" / "Skill Progress"
 *   - OR scoreboard tag: /tag @e[type=!player,distance=..3] add lm_skillcheck
 *
 * Do NOT leave Rival / Spar scripts or lm_rival tags on this NPC.
 *
 * Right-click opens Skill Check for ANY player (no LuckPerms needed).
 * Slash /skillcheck still requires legacymechanics.skillcheck.
 * ============================================================
 */

function interact(event) {
    try {
        /* Stop other CNPC dialogs / Forge name steal from fighting this open. */
        try {
            if (event != null && typeof event.setCanceled === "function") {
                event.setCanceled(true);
            }
        } catch (ignoredCancel) {}

        var player = event.player;
        if (player == null) {
            return;
        }

        var SkillCheck = Java.type(
            "com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService"
        );

        var mcPlayer = null;
        try {
            mcPlayer = player.getMCEntity();
        } catch (e1) {
            try {
                mcPlayer = player.getMinecraftEntity();
            } catch (e2) {
                mcPlayer = null;
            }
        }
        if (mcPlayer == null) {
            player.message("\u00A7cSkill Check: could not resolve player entity.");
            return;
        }

        /* Opens CMI/chest Skill Check GUI — no permission check (NPC is the gate). */
        SkillCheck.openFromNpc(mcPlayer, "core");
    } catch (err) {
        try {
            if (event.player != null) {
                event.player.message(
                    "\u00A7cSkill Check failed. Is LegacyMechanics loaded?\n\u00A78" + err
                );
            }
        } catch (ignored) {}
        print("[LM SkillCheck NPC] " + err);
    }
}
