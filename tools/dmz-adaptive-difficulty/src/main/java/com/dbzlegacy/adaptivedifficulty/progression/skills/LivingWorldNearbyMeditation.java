package com.dbzlegacy.adaptivedifficulty.progression.skills;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.skills.Skills;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * DragonMineZ Meditation levels from time spent near an active Living World
 * meditation circle. Players do not join the circle.
 *
 * <p>Living World 2.4.3 marks a real circle only after
 * {@code beginMeditationCircle}: {@code beginMeditation} clears
 * {@code MEDITATION_CIRCLE_MEMBER}, then {@code setMeditationCircleCenter}
 * sets it again. Solo NPC meditation stays {@code isMeditating} with the
 * circle flag off, so both public checks are required. There is no public
 * circle-center getter; the meditating fighter's position is the center.
 * Overlapping circles credit a player once per real-time sample.
 */
public final class LivingWorldNearbyMeditation {
    private static final String FIGHTER = "com.dmzlivingworld.entity.AmbientFighterEntity";
    private static final String SKILL = "meditation";
    static final String KEY_PROGRESS = "lm.lw_med.progress_ms";
    static final String KEY_LIFETIME = "lm.lw_med.lifetime_ms";
    static final String KEY_TRACKED = "lm.lw_med.tracked_level";
    private static final String TEMP_NEAR = "lm.lw_med.near";
    private static final String TEMP_LAST = "lm.lw_med.last_ms";
    /** Ignore a stalled pulse longer than this so a hung tick cannot dump hours. */
    private static final long MAX_DELTA_MS = 60_000L;

    private static volatile boolean lookupFailed;
    private static Class<?> fighterClass;
    private static Method isMeditating;
    private static Method isMeditationCircleMember;

    private LivingWorldNearbyMeditation() {}

    public static void pulse(MinecraftServer server, int tick) {
        if (server == null || !ProgressionConfig.livingWorldMeditation()) {
            return;
        }
        if (tick % 20 != 0 || !resolveApi()) {
            return;
        }
        List<Entity> centers = activeCircles(server);
        long now = System.currentTimeMillis();
        int radius = DifficultyConfig.get().meditationDetectionRadius;
        double r2 = (double) radius * (double) radius;
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player == null) {
                continue;
            }
            if (!player.m_6084_() || player.m_5833_() || !nearAny(player, centers, r2)) {
                clearNear(player);
                continue;
            }
            credit(player, now);
        }
    }

    /** Skill Check lines. Continuation rows stay on the same tile. */
    public static void appendLines(List<String> out, ServerPlayer player, Skills skills) {
        if (out == null || player == null) {
            return;
        }
        int cap = cap();
        int level = Math.max(0, DmzSkillUtil.level(skills, SKILL));
        if (cap <= 0) {
            return;
        }
        if (level >= cap) {
            out.add("§5Meditation§7: §6§lMAX§r §7(" + level + "/" + cap + ")");
            out.add("§8  - §7Progress §fMAX");
            return;
        }
        long need = requirementMs(level);
        long progress = shownProgress(player, level, need);
        long left = Math.max(0L, need - progress);
        out.add("§5Meditation§7: §f" + level + "/" + cap);
        out.add("§8  - §7Progress §f" + compact(progress) + " / " + compact(need));
        out.add("§8  - §7Remaining §f" + compact(left));
        if (level <= 0 && progress <= 0L) {
            out.add("§8  - §7Stay near a Living World meditation circle.");
        }
    }

    private static boolean resolveApi() {
        if (isMeditating != null && isMeditationCircleMember != null && fighterClass != null) {
            return true;
        }
        if (lookupFailed) {
            return false;
        }
        try {
            Class<?> cls = Class.forName(FIGHTER);
            isMeditating = cls.getMethod("isMeditating");
            isMeditationCircleMember = cls.getMethod("isMeditationCircleMember");
            fighterClass = cls;
            return true;
        } catch (Throwable t) {
            lookupFailed = true;
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Living World meditation circle API unavailable: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
            return false;
        }
    }

    private static List<Entity> activeCircles(MinecraftServer server) {
        List<Entity> centers = new ArrayList<>();
        for (ServerLevel level : server.m_129785_()) {
            if (level == null) {
                continue;
            }
            for (Entity entity : level.m_8583_()) {
                if (circleMember(entity)) {
                    centers.add(entity);
                }
            }
        }
        return centers;
    }

    /** True only while this fighter is meditating as a circle member. */
    private static boolean circleMember(Entity entity) {
        if (entity == null || fighterClass == null || !fighterClass.isInstance(entity) || !entity.m_6084_()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(isMeditating.invoke(entity))
                    && Boolean.TRUE.equals(isMeditationCircleMember.invoke(entity));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean nearAny(ServerPlayer player, List<Entity> centers, double r2) {
        if (centers == null || centers.isEmpty()) {
            return false;
        }
        for (Entity center : centers) {
            if (center == null || player.m_9236_() != center.m_9236_()) {
                continue;
            }
            double dx = player.m_20185_() - center.m_20185_();
            double dy = player.m_20186_() - center.m_20186_();
            double dz = player.m_20189_() - center.m_20189_();
            if (dx * dx + dy * dy + dz * dz <= r2) {
                return true;
            }
        }
        return false;
    }

    private static void clearNear(ServerPlayer player) {
        ProgressionData.tempPut(player, TEMP_NEAR, "");
        ProgressionData.tempPut(player, TEMP_LAST, 0L);
    }

    /**
     * First sample inside the radius only stamps the clock. Away time is not
     * added, and a later circle does not pay for the gap.
     */
    private static void credit(ServerPlayer player, long now) {
        boolean wasNear = "1".equals(ProgressionData.tempGet(player, TEMP_NEAR, ""));
        long last = ProgressionData.tempGetLong(player, TEMP_LAST, 0L);
        ProgressionData.tempPut(player, TEMP_NEAR, "1");
        ProgressionData.tempPut(player, TEMP_LAST, now);
        if (!wasNear || last <= 0L) {
            return;
        }
        long delta = now - last;
        if (delta <= 0L) {
            return;
        }
        if (delta > MAX_DELTA_MS) {
            delta = MAX_DELTA_MS;
        }
        addTime(player, delta);
    }

    private static void addTime(ServerPlayer player, long deltaMs) {
        Skills skills = DmzSkillUtil.skills(player);
        if (skills == null) {
            return;
        }
        int cap = cap();
        if (cap <= 0) {
            return;
        }
        int live = Math.max(0, DmzSkillUtil.level(skills, SKILL));
        long tracked = ProgressionData.storedGetLong(player, KEY_TRACKED, -1L);
        long progress = Math.max(0L, ProgressionData.storedGetLong(player, KEY_PROGRESS, 0L));
        // Shop or admin skill changes do not keep the old level's partial bar.
        if (tracked < 0L || tracked != live) {
            progress = 0L;
            tracked = live;
        }
        long lifetime = Math.max(0L, ProgressionData.storedGetLong(player, KEY_LIFETIME, 0L)) + deltaMs;
        if (live >= cap) {
            ProgressionData.storedPut(player, KEY_PROGRESS, 0L);
            ProgressionData.storedPut(player, KEY_LIFETIME, lifetime);
            ProgressionData.storedPut(player, KEY_TRACKED, live);
            return;
        }
        progress += deltaMs;
        int level = live;
        while (level < cap) {
            long need = requirementMs(level);
            if (need <= 0L || progress < need) {
                break;
            }
            progress -= need;
            level++;
        }
        if (level >= cap) {
            progress = 0L;
        }
        if (level == live) {
            ProgressionData.storedPut(player, KEY_PROGRESS, progress);
            ProgressionData.storedPut(player, KEY_LIFETIME, lifetime);
            ProgressionData.storedPut(player, KEY_TRACKED, level);
            return;
        }
        DmzSkillUtil.ensureRegistered(skills, SKILL, cap);
        if (!DmzSkillUtil.setLevel(skills, SKILL, level)) {
            long restored = progress;
            for (int step = live; step < level; step++) {
                restored += requirementMs(step);
            }
            ProgressionData.storedPut(player, KEY_PROGRESS, restored);
            ProgressionData.storedPut(player, KEY_LIFETIME, lifetime);
            ProgressionData.storedPut(player, KEY_TRACKED, live);
            return;
        }
        ProgressionData.storedPut(player, KEY_PROGRESS, progress);
        ProgressionData.storedPut(player, KEY_LIFETIME, lifetime);
        ProgressionData.storedPut(player, KEY_TRACKED, level);
        DmzSkillUtil.sync(player);
        announce(player, level, cap);
    }

    private static void announce(ServerPlayer player, int level, int cap) {
        DmzRewards.msg(player, "§5§lMEDITATION BREAKTHROUGH!");
        DmzRewards.msg(player, "§7Your mind has reached a deeper state of awareness.");
        DmzRewards.msg(player, "§dMeditation Level: §f" + level);
        DmzRewards.msg(player, "§7Next breakthrough:");
        if (level >= cap) {
            DmzRewards.msg(player, "§fNone");
            return;
        }
        DmzRewards.msg(player, "§f" + words(requirementMs(level)));
    }

    /** Seconds to leave {@code level} and reach {@code level + 1}. Index 0 is level 1. */
    private static long requirementMs(int level) {
        int[] seconds = DifficultyConfig.get().meditationLevelSeconds;
        if (seconds == null || level < 0 || level >= seconds.length) {
            return 0L;
        }
        return Math.max(1, seconds[level]) * 1000L;
    }

    private static int cap() {
        int[] table = DifficultyConfig.get().meditationLevelSeconds;
        int tableLen = table == null ? 0 : table.length;
        int dmz = DmzSkillUtil.configuredMaxLevel(SKILL);
        if (dmz <= 0) {
            dmz = 10;
        }
        return Math.min(10, Math.min(tableLen, dmz));
    }

    private static long shownProgress(ServerPlayer player, int level, long need) {
        long tracked = ProgressionData.storedGetLong(player, KEY_TRACKED, -1L);
        if (tracked >= 0L && tracked != level) {
            return 0L;
        }
        long progress = Math.max(0L, ProgressionData.storedGetLong(player, KEY_PROGRESS, 0L));
        if (need > 0L && progress > need) {
            return need;
        }
        return progress;
    }

    static String compact(long ms) {
        long sec = Math.max(0L, ms / 1000L);
        if (sec < 60L) {
            return sec + "s";
        }
        if (sec < 3600L) {
            long min = sec / 60L;
            long rem = sec % 60L;
            return rem == 0L ? min + "m" : min + "m " + rem + "s";
        }
        long hr = sec / 3600L;
        long min = (sec % 3600L) / 60L;
        return min == 0L ? hr + "h" : hr + "h " + min + "m";
    }

    static String words(long ms) {
        long sec = Math.max(0L, ms / 1000L);
        if (sec > 0L && sec % 3600L == 0L) {
            long hr = sec / 3600L;
            return hr == 1L ? "1 hour" : hr + " hours";
        }
        if (sec > 0L && sec % 60L == 0L) {
            long min = sec / 60L;
            return min == 1L ? "1 minute" : min + " minutes";
        }
        return compact(ms);
    }
}
