/*
 * ============================================================
 * LegacyMechanics — Skill Check CNPC (interact)
 * ============================================================
 * Setup (CustomNPCs):
 *   1. Edit NPC → Advanced → Scripts
 *   2. Enable scripts, language: ECMAScript / JavaScript
 *   3. Paste this file into the NPC script (or load as Script File)
 *   4. Ensure LegacyMechanics-2.3.46+ is installed
 *
 * Right-click opens Skill Check for ANY player (no LuckPerms needed).
 * Slash /skillcheck still requires legacymechanics.skillcheck.
 * ============================================================
 */

function interact(event) {
    try {
        var player = event.player;
        if (player == null) {
            return;
        }

        var SkillCheck = Java.type(
            "com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService"
        );

        var mcPlayer = player.getMCEntity();
        if (mcPlayer == null) {
            player.message("\u00A7cSkill Check: could not resolve player entity.");
            return;
        }

        // Opens CMI/chest Skill Check GUI — no permission check (NPC is the gate).
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
