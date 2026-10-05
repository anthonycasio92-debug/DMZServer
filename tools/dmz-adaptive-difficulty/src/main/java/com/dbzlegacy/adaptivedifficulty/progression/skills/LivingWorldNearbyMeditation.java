package com.dbzlegacy.adaptivedifficulty.progression.skills;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.skills.Skills;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * DragonMineZ Meditation levels while the player is already in Living World's
 * meditation state and either another meditating player is within range, or
 * two living NPCs that are meditating are within range. One NPC is not
 * enough. Standing nearby without meditating does not count. This class does
 * not start meditation for them.
 *
 * <p>Living World 2.4.3 {@code beginSharedMeditation} calls
 * {@code beginMeditation}, which clears {@code MEDITATION_CIRCLE_MEMBER}.
 * The pose a player can stand next to is {@code isMeditating}. The circle
 * flag is resolved so a Living World build without it disables this pulse,
 * but it is not required. Mohist can load {@code AmbientFighterEntity} on
 * another class loader than {@code Class.forName}, so a fighter is also
 * recognized by class name and {@code isMeditating} is invoked on that
 * entity's own class. Player meditation is
 * {@code MeditationCompat.isPlayerMeditating}. Two players, or two NPCs,
 * still credit one sample. Extra players do not raise it. Two living NPCs
 * credit 75% of the player rate. Each held prestige adds 10% on top of
 * whichever rate is already in use.
 */
public final class LivingWorldNearbyMeditation {
    private static final String FIGHTER = "com.dmzlivingworld.entity.AmbientFighterEntity";
    private static final String PLAYER_STATE = "com.dmzlivingworld.compat.MeditationCompat";
    private static final String SKILL = "meditation";
    static final String KEY_PROGRESS = "lm.lw_med.progress_ms";
    static final String KEY_LIFETIME = "lm.lw_med.lifetime_ms";
    static final String KEY_TRACKED = "lm.lw_med.tracked_level";
    private static final String TEMP_NEAR = "lm.lw_med.near";
    private static final String TEMP_LAST = "lm.lw_med.last_ms";
    /** Ignore a stalled pulse longer than this so a hung tick cannot dump hours. */
    private static final long MAX_DELTA_MS = 60_000L;
    /** Two living NPCs credit this percent of the same time spent with a player. */
    private static final int NPC_RATE_PERCENT = 75;
    /** Each held prestige adds this percent. More nearby players do not. */
    private static final int HELD_RATE_PERCENT = 10;

    private static volatile boolean lookupFailed;
    private static Class<?> fighterClass;
    private static Method isMeditating;
    private static Method isMeditationCircleMember;
    private static Method isPlayerMeditating;
    /** Methods from the entity's own class when Mohist split the fighter class. */
    private static final Map<Class<?>, Method> meditatingOnClass = new HashMap<>();
    /** Class-name walk result. Hot entities otherwise repeat the hierarchy walk. */
    private static final Map<Class<?>, Boolean> fighterType = new HashMap<>();

    private LivingWorldNearbyMeditation() {}

    public static void pulse(MinecraftServer server, int tick) {
        if (server == null || !ProgressionConfig.livingWorldMeditation()) {
            return;
        }
        if (tick % 20 != 0 || !resolveApi()) {
            return;
        }
        List<ServerPlayer> players = server.m_6846_().m_11314_();
        long now = System.currentTimeMillis();
        int radius = DifficultyConfig.get().meditationDetectionRadius;
        double r2 = (double) radius * (double) radius;
        List<ServerPlayer> meditating = new ArrayList<>();
        for (ServerPlayer player : players) {
            if (player == null) {
                continue;
            }
            if (!player.m_6084_() || player.m_5833_() || !playerMeditating(player)) {
                clearNear(player);
                continue;
            }
            meditating.add(player);
        }
        if (meditating.isEmpty()) {
            return;
        }
        List<Entity> circles = meditatingFightersNear(meditating, radius);
        for (ServerPlayer player : meditating) {
            boolean withPlayer = nearMeditatingPlayer(player, players, r2);
            boolean withNpcs = nearTwoLivingCircleNpcs(player, circles, r2);
            if (!withPlayer && !withNpcs) {
                clearNear(player);
                continue;
            }
            credit(player, now, withPlayer);
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
        out.add("§8  - §7Meditate near another player, or near two living NPCs.");
        out.add("§8  - §7NPCs count at 75%. More players do not add time.");
        out.add("§8  - §7Each held prestige adds 10%.");
    }

    private static boolean resolveApi() {
        if (isMeditating != null && isMeditationCircleMember != null
                && isPlayerMeditating != null && fighterClass != null) {
            return true;
        }
        if (lookupFailed) {
            return false;
        }
        try {
            Class<?> cls = Class.forName(FIGHTER);
            Class<?> compat = Class.forName(PLAYER_STATE);
            isMeditating = cls.getMethod("isMeditating");
            isMeditationCircleMember = cls.getMethod("isMeditationCircleMember");
            isPlayerMeditating = compat.getMethod("isPlayerMeditating", ServerPlayer.class);
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

    /** Fighters inside the detection box of a meditating player. Not every entity in every level. */
    private static List<Entity> meditatingFightersNear(List<ServerPlayer> meditating, double radius) {
        List<Entity> centers = new ArrayList<>();
        Set<Entity> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        double reach = Math.max(1.0, radius);
        for (ServerPlayer player : meditating) {
            if (player == null || !(player.m_9236_() instanceof ServerLevel level)) {
                continue;
            }
            AABB box = player.m_20191_().m_82400_(reach);
            for (LivingEntity entity : level.m_45976_(LivingEntity.class, box)) {
                if (entity == null || !seen.add(entity)) {
                    continue;
                }
                if (circleMember(entity)) {
                    centers.add(entity);
                }
            }
        }
        return centers;
    }

    /**
     * True while this living fighter is meditating. One nearby fighter is not
     * a pair. {@code isMeditationCircleMember} is not the gate: shared
     * meditation clears that flag inside {@code beginMeditation}.
     */
    private static boolean circleMember(Entity entity) {
        if (entity == null || !entity.m_6084_() || !isFighter(entity)) {
            return false;
        }
        return fighterMeditating(entity);
    }

    private static boolean isFighter(Entity entity) {
        if (fighterClass != null && fighterClass.isInstance(entity)) {
            return true;
        }
        Class<?> type = entity.getClass();
        Boolean known = fighterType.get(type);
        if (known != null) {
            return known;
        }
        boolean match = false;
        for (Class<?> cursor = type; cursor != null && cursor != Object.class; cursor = cursor.getSuperclass()) {
            if (FIGHTER.equals(cursor.getName())) {
                match = true;
                break;
            }
        }
        fighterType.put(type, match);
        return match;
    }

    private static boolean fighterMeditating(Entity entity) {
        if (fighterClass != null && fighterClass.isInstance(entity) && isMeditating != null) {
            try {
                return Boolean.TRUE.equals(isMeditating.invoke(entity));
            } catch (Throwable ignored) {
                // The resolved class is not this entity's class. Use its own method.
            }
        }
        Class<?> type = entity.getClass();
        Method live = meditatingOnClass.get(type);
        if (live == null) {
            try {
                live = type.getMethod("isMeditating");
                meditatingOnClass.put(type, live);
            } catch (Throwable ignored) {
                return false;
            }
        }
        try {
            return Boolean.TRUE.equals(live.invoke(entity));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Living World's own player session. Standing nearby does not set this. */
    private static boolean playerMeditating(ServerPlayer player) {
        if (player == null || isPlayerMeditating == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(isPlayerMeditating.invoke(null, player));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** One other meditating player is the full rate. A third player does not raise it. */
    private static boolean nearMeditatingPlayer(
            ServerPlayer player, List<ServerPlayer> players, double r2) {
        if (players == null) {
            return false;
        }
        for (ServerPlayer other : players) {
            if (other == null || other == player || !other.m_6084_() || other.m_5833_()) {
                continue;
            }
            if (player.m_9236_() != other.m_9236_() || !playerMeditating(other)) {
                continue;
            }
            if (dist2(player, other) <= r2) {
                return true;
            }
        }
        return false;
    }

    /** One living NPC is not enough. Both must be alive, meditating, and in range. */
    private static boolean nearTwoLivingCircleNpcs(
            ServerPlayer player, List<Entity> circles, double r2) {
        if (circles == null) {
            return false;
        }
        int found = 0;
        for (Entity npc : circles) {
            if (npc == null || !npc.m_6084_() || player.m_9236_() != npc.m_9236_()) {
                continue;
            }
            if (dist2(player, npc) > r2) {
                continue;
            }
            found++;
            if (found >= 2) {
                return true;
            }
        }
        return false;
    }

    private static double dist2(Entity a, Entity b) {
        double dx = a.m_20185_() - b.m_20185_();
        double dy = a.m_20186_() - b.m_20186_();
        double dz = a.m_20189_() - b.m_20189_();
        return dx * dx + dy * dy + dz * dz;
    }

    private static void clearNear(ServerPlayer player) {
        ProgressionData.tempPut(player, TEMP_NEAR, "");
        ProgressionData.tempPut(player, TEMP_LAST, 0L);
    }

    /**
     * First sample while meditating inside the radius only stamps the clock.
     * Time spent standing nearby, or time after meditation ends, is not added.
     */
    private static void credit(ServerPlayer player, long now, boolean withPlayer) {
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
        addTime(player, creditedMs(player, delta, withPlayer));
    }

    /**
     * A player partner is full time. Two living NPCs are {@value #NPC_RATE_PERCENT}%
     * of that. Each held prestige then adds {@value #HELD_RATE_PERCENT}%.
     * A player nearby wins over NPCs, so the two rates are not added together.
     */
    private static long creditedMs(ServerPlayer player, long deltaMs, boolean withPlayer) {
        long base = withPlayer ? deltaMs : (deltaMs * NPC_RATE_PERCENT) / 100L;
        int held = 0;
        try {
            held = PrestigeSystem.heldCountForNeed(player);
        } catch (Throwable ignored) {
        }
        if (held < 0) {
            held = 0;
        }
        if (held > 10) {
            held = 10;
        }
        return base + (base * held * HELD_RATE_PERCENT) / 100L;
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
