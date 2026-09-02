package com.dbzlegacy.adaptivedifficulty.progression.skills;

import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.InvestedStrength;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.LmChat;
import com.dragonminez.common.stats.skills.Skills;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of SprintJump.js 1.1.0 — unlock Sprint/Jump from invested STR points.
 */
public final class SprintJumpProgression {
    private static final int MAX_LEVEL = 10;
    private static final int[] REQUIREMENTS = {
            0, 20, 100, 250, 500, 1000, 1500, 2000, 2500, 3000, 3500
    };
    private static final String[][] SKILLS = {
            {"sprint", "Sprint", "SPRINT"},
            {"jump", "Jump", "Jump"}
    };

    private SprintJumpProgression() {}

    public static void pulse(ServerPlayer player, long nowMs) {
        if (!ProgressionConfig.sprintJump() || player == null) {
            return;
        }
        if (nowMs < ProgressionData.tempGetLong(player, "sprintjump_skill_next_check", 0L)) {
            return;
        }
        ProgressionData.tempPut(player, "sprintjump_skill_next_check", nowMs + 1000L);
        try {
            Skills skills = DmzSkillUtil.skills(player);
            if (skills == null) {
                return;
            }
            int invested = InvestedStrength.points(player);
            boolean changed = false;
            for (String[] info : SKILLS) {
                if (updateSkill(player, skills, info[0], info[1], info[2], invested)) {
                    changed = true;
                }
            }
            if (changed) {
                DmzSkillUtil.sync(player);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean updateSkill(
            ServerPlayer player,
            Skills skills,
            String id,
            String label,
            String tag,
            int invested
    ) {
        DmzSkillUtil.ensureRegistered(skills, id, MAX_LEVEL);
        int current = DmzSkillUtil.level(skills, id);
        int max = DmzSkillUtil.maxLevel(skills, id, MAX_LEVEL);
        int target = current;
        for (int level = 1; level <= max; level++) {
            if (level < REQUIREMENTS.length && invested >= REQUIREMENTS[level]) {
                target = level;
            }
        }
        if (target <= current) {
            return false;
        }
        DmzSkillUtil.setLevel(skills, id, target);
        if (current < 1) {
            DmzRewards.msg(player, LmChat.ok(tag, label + " unlocked at level "
                    + target + " (" + invested + " STR invested)."));
        } else {
            DmzRewards.msg(player, LmChat.ok(tag, "Increased to level "
                    + target + " (" + invested + " STR invested)."));
        }
        if (target >= max) {
            DmzRewards.msg(player, LmChat.ok(tag, label + " is now maxed."));
        }
        SystemTelemetry.log("progression", "sprintjump_level", player, null,
                Map.of("skill", id, "level", target, "investedStr", invested));
        return true;
    }
}
