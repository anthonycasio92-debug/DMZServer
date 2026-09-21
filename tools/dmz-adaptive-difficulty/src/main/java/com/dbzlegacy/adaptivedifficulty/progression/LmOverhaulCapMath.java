package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import com.dragonminez.common.stats.StatsData;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/** LM personal level cap → Overhaul-compatible max stat totals (matches dmzrevamp formula). */
public final class LmOverhaulCapMath {
    /** Stock Overhaul {@code levelUpPerPoints} when {@code LevelingRevamp.json} is default. */
    public static final int LEVEL_UP_PER_POINTS = 6;
    /**
     * Overhaul Statistics / {@code PrestigeSystem.levelCap} / hex reference at 0 breakthroughs.
     * Stock Overhaul {@code initialLevelCap} is 50000 and is <b>not</b> synced to clients;
     * prestige 0 would show 50k and block Need 60k+. Pin the Overhaul floor to 100k.
     * Breakthroughs raise the live cap toward {@link PrestigePointsSystem#ABSOLUTE_LEVEL_CAP}.
     */
    public static final int OVERHAUL_LEVEL_CAP = PrestigePointsSystem.BASE_LEVEL_CAP;
    public static final int OVERHAUL_ABSOLUTE_LEVEL_CAP = PrestigePointsSystem.ABSOLUTE_LEVEL_CAP;

    private static volatile Method revampInitialStatTotal;
    private static volatile Method revampPointsPerLevel;

    private LmOverhaulCapMath() {}

    public static int personalLevelCap(ServerPlayer player) {
        if (player == null) {
            return PrestigePointsSystem.BASE_LEVEL_CAP;
        }
        int cap = PrestigePointsSystem.effectiveMaxLevel(player);
        PersonalLevelCapMirror.publish(player);
        return cap;
    }

    /** Server-authoritative cap from {@link StatsData} (Mohist-safe player lookup). */
    public static int personalLevelCap(com.dragonminez.common.stats.StatsData data) {
        if (data == null) {
            return PrestigePointsSystem.BASE_LEVEL_CAP;
        }
        net.minecraft.server.level.ServerPlayer sp = LmStatsDataAccess.serverPlayer(data);
        if (sp != null) {
            return personalLevelCap(sp);
        }
        return PersonalLevelCapMirror.resolveCap(data);
    }

    public static int maxAssignableTotal(StatsData data, int levelCap) {
        if (data == null || levelCap < 1) {
            return 0;
        }
        int initial = initialStatTotal(data);
        int perLevel = statPointsPerLevel();
        long max = (long) initial + (long) Math.max(0, levelCap - 1) * perLevel;
        return (int) Math.min(Integer.MAX_VALUE, max);
    }

    public static int maxAssignableTotal(StatsData data, ServerPlayer player) {
        return maxAssignableTotal(data, personalLevelCap(player));
    }

    public static int maxAssignableTotal(StatsData data) {
        return maxAssignableTotal(data, personalLevelCap(data));
    }

    /**
     * Prestige-0 / 0-breakthrough Overhaul max (100k). Native {@code levelCap}
     * at count 0 is {@code initialLevelCap} (stock 50k); we never return that.
     */
    public static int overhaulLevelCap() {
        return OVERHAUL_LEVEL_CAP;
    }

    /**
     * Live Overhaul {@code levelCap}: personal 100k + 10k×breakthroughs (max 150k).
     * Held prestige does not change this — 0 breakthroughs stays 100k at held 0 and 1.
     */
    public static int overhaulLevelCap(StatsData data) {
        int personal = personalLevelCap(data);
        if (personal < OVERHAUL_LEVEL_CAP) {
            return OVERHAUL_LEVEL_CAP;
        }
        return Math.min(OVERHAUL_ABSOLUTE_LEVEL_CAP, personal);
    }

    /**
     * Force live Overhaul {@code initialLevelCap} to 100k and {@code maxLevel} to 150k
     * so prestige 0 is not stock 50k and 0-breakthrough players can reach Need 60k+.
     */
    public static void pinOverhaulLevelCaps() {
        if (!ModList.get().isLoaded("dmzrevamp")) {
            return;
        }
        try {
            Class<?> cfgCls = Class.forName("com.dmzrevamp.config.LevelingRevampConfig");
            Object cfg = cfgCls.getMethod("get").invoke(null);
            if (cfg == null) {
                return;
            }
            Object levels = cfg.getClass().getField("levelsAndAttributes").get(cfg);
            Object prestige = cfg.getClass().getField("Prestige").get(cfg);
            java.lang.reflect.Field maxLevel = levels.getClass().getField("maxLevel");
            java.lang.reflect.Field initial = prestige.getClass().getField("initialLevelCap");
            if (maxLevel.getInt(levels) != OVERHAUL_ABSOLUTE_LEVEL_CAP) {
                maxLevel.setInt(levels, OVERHAUL_ABSOLUTE_LEVEL_CAP);
            }
            if (initial.getInt(prestige) != OVERHAUL_LEVEL_CAP) {
                initial.setInt(prestige, OVERHAUL_LEVEL_CAP);
            }
        } catch (Throwable ignored) {
        }
    }

    private static int initialStatTotal(StatsData data) {
        if (data != null && ModList.get().isLoaded("dmzrevamp")) {
            try {
                if (revampInitialStatTotal == null) {
                    Class<?> helper = Class.forName("com.dmzrevamp.revamp.DmzRevampHelper");
                    revampInitialStatTotal = helper.getMethod("getInitialStatTotal", StatsData.class);
                }
                Object v = revampInitialStatTotal.invoke(null, data);
                if (v instanceof Number n) {
                    return Math.max(0, n.intValue());
                }
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }

    private static int statPointsPerLevel() {
        if (ModList.get().isLoaded("dmzrevamp")) {
            try {
                if (revampPointsPerLevel == null) {
                    Class<?> helper = Class.forName("com.dmzrevamp.revamp.DmzRevampHelper");
                    revampPointsPerLevel = helper.getMethod("getConfiguredStatPointsPerLevel");
                }
                Object v = revampPointsPerLevel.invoke(null);
                if (v instanceof Number n && n.intValue() > 0) {
                    return n.intValue();
                }
            } catch (Throwable ignored) {
            }
        }
        return LEVEL_UP_PER_POINTS;
    }
}
