package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skills;
import java.util.Collection;
import java.util.Iterator;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code Prestige Sync Fabled.js} — Fabled Prestige class level → DMZ {@code prestige} skill.
 * Target DMZ level = Fabled Prestige class level − 1 (floored at 0).
 */
public final class PrestigeSkillSync {
    private static final String FABLED_CLASS_NAME = "Prestige";
    private static final String DMZ_SKILL_ID = "prestige";

    private PrestigeSkillSync() {}

    public static void sync(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enablePrestigeSkillSync) {
            return;
        }
        Object data = FabledBridge.fabledData(player);
        if (data == null) {
            return;
        }

        Object prestigeClass = findPrestigeClass(data);
        int prestigeClassLevel = 0;
        if (prestigeClass != null) {
            prestigeClassLevel = Math.max(0, FabledBridge.invokeInt(prestigeClass, "getLevel"));
        }
        int targetSkillLevel = Math.max(0, prestigeClassLevel - 1);

        StatsData dmz = DmzProgression.stats(player);
        if (dmz == null) {
            return;
        }
        Skills skills = dmz.getSkills();
        if (skills == null) {
            return;
        }

        if (!skills.hasSkill(DMZ_SKILL_ID)) {
            try {
                skills.registerDefaultSkill(DMZ_SKILL_ID, targetSkillLevel);
            } catch (Throwable t) {
                return;
            }
        }

        int current = skills.getSkillLevel(DMZ_SKILL_ID);
        if (current == targetSkillLevel) {
            return;
        }
        skills.setSkillLevel(DMZ_SKILL_ID, targetSkillLevel);
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
        FabledBridge.logSync(
                player,
                "prestige_skill",
                "fabledLevel",
                prestigeClassLevel,
                "dmzLevel",
                targetSkillLevel);
    }

    /** Current Fabled Prestige class level, or 0 if missing. */
    public static int fabledPrestigeLevel(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        Object data = FabledBridge.fabledData(player);
        if (data == null) {
            return 0;
        }
        Object prestigeClass = findPrestigeClass(data);
        if (prestigeClass == null) {
            return 0;
        }
        return Math.max(0, FabledBridge.invokeInt(prestigeClass, "getLevel"));
    }

    /**
     * Lower Fabled Prestige class by {@code amount} (Fabled floors at level 1).
     * Syncs DMZ prestige skill + CNPC faction held immediately.
     *
     * @return levels actually removed
     */
    public static int takePrestigeLevels(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }
        Object data = FabledBridge.fabledData(player);
        if (data == null) {
            return 0;
        }
        Object prestigeClass = findPrestigeClass(data);
        if (prestigeClass == null) {
            return 0;
        }
        int current = Math.max(0, FabledBridge.invokeInt(prestigeClass, "getLevel"));
        // Fabled PlayerClass.setLevel / loseLevels refuse going below 1.
        int canLose = Math.max(0, current - 1);
        int lose = Math.min(amount, canLose);
        if (lose <= 0) {
            return 0;
        }
        boolean ok = false;
        try {
            prestigeClass.getClass().getMethod("loseLevels", int.class).invoke(prestigeClass, lose);
            ok = true;
        } catch (Throwable ignored) {
        }
        if (!ok) {
            try {
                prestigeClass.getClass().getMethod("setLevel", int.class)
                        .invoke(prestigeClass, current - lose);
                ok = true;
            } catch (Throwable ignored) {
                return 0;
            }
        }
        try {
            sync(player);
        } catch (Throwable ignored) {
        }
        try {
            PrestigeFactionSync.forceSync(player);
        } catch (Throwable ignored) {
        }
        EnergyManaSync.sync(player, true);
        FabledLevelGuard.sync(player);
        FabledBridge.logSync(player, "prestige_take", "lost", lose, "level", current - lose);
        return lose;
    }

    /**
     * Raise Fabled Prestige class by {@code amount} via API ({@code giveLevels}).
     *
     * @return levels actually added
     */
    public static int addPrestigeLevels(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }
        Object data = FabledBridge.fabledData(player);
        if (data == null) {
            return 0;
        }
        Object prestigeClass = findPrestigeClass(data);
        if (prestigeClass == null) {
            return 0;
        }
        int before = Math.max(0, FabledBridge.invokeInt(prestigeClass, "getLevel"));
        try {
            prestigeClass.getClass().getMethod("giveLevels", int.class).invoke(prestigeClass, amount);
        } catch (Throwable t) {
            return 0;
        }
        int after = Math.max(0, FabledBridge.invokeInt(prestigeClass, "getLevel"));
        int gained = Math.max(0, after - before);
        if (gained > 0) {
            try {
                sync(player);
            } catch (Throwable ignored) {
            }
            try {
                PrestigeFactionSync.forceSync(player);
            } catch (Throwable ignored) {
            }
            EnergyManaSync.sync(player, true);
            FabledLevelGuard.sync(player);
            FabledBridge.logSync(player, "prestige_add", "gained", gained, "level", after);
        }
        return gained;
    }

    static Object findPrestigeClass(Object fabledData) {
        try {
            Object direct = fabledData.getClass()
                    .getMethod("getClass", String.class)
                    .invoke(fabledData, FABLED_CLASS_NAME);
            if (direct != null) {
                return direct;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object lower = fabledData.getClass()
                    .getMethod("getClass", String.class)
                    .invoke(fabledData, "prestige");
            if (lower != null) {
                return lower;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object classes = fabledData.getClass().getMethod("getClasses").invoke(fabledData);
            if (classes instanceof Collection<?> col) {
                Iterator<?> it = col.iterator();
                while (it.hasNext()) {
                    Object current = it.next();
                    if (current == null) {
                        continue;
                    }
                    Object classData = current.getClass().getMethod("getData").invoke(current);
                    if (classData == null) {
                        continue;
                    }
                    Object name = classData.getClass().getMethod("getName").invoke(classData);
                    if (name != null && FABLED_CLASS_NAME.equalsIgnoreCase(String.valueOf(name))) {
                        return current;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
