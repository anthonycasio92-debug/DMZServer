package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skills;
import net.minecraft.server.level.ServerPlayer;

/** Shared DMZ skill sync / level helpers for progression modules. */
public final class DmzSkillUtil {
    private DmzSkillUtil() {}

    public static Skills skills(ServerPlayer player) {
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return null;
        }
        try {
            return data.getSkills();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] progression skill sync failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
        }
    }

    public static int level(Skills skills, String id) {
        if (skills == null || id == null) {
            return 0;
        }
        try {
            return Math.max(0, skills.getSkillLevel(id));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static int maxLevel(Skills skills, String id, int fallbackCap) {
        if (skills == null || id == null) {
            return fallbackCap;
        }
        try {
            int max = skills.getMaxSkillLevel(id);
            if (max <= 0) {
                return fallbackCap;
            }
            return Math.min(max, fallbackCap);
        } catch (Throwable ignored) {
            return fallbackCap;
        }
    }

    public static boolean setLevel(Skills skills, String id, int level) {
        if (skills == null || id == null) {
            return false;
        }
        try {
            skills.setSkillLevel(id, Math.max(0, level));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void ensureRegistered(Skills skills, String id, int max) {
        if (skills == null || id == null) {
            return;
        }
        try {
            skills.registerDefaultSkill(id, max);
            skills.refreshNonFormSkillMaxLevels();
        } catch (Throwable ignored) {
        }
    }

    public static boolean isActive(Skills skills, String id) {
        if (skills == null || id == null) {
            return false;
        }
        try {
            return skills.isSkillActive(id);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
