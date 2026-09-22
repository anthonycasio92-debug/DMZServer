package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dragonminez.common.stats.StatsData;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Mirrors the server personal level cap onto player persistent data for DMZ client UI
 * (LegacyMechanics is server-only — clients read Overhaul/DMZ configured max, not our mixins).
 */
public final class PersonalLevelCapMirror {
    public static final String KEY = "lm_personal_level_cap";
    public static final String KEY_BREAKTHROUGHS = "lm_personal_breakthroughs";
    /** Redundant root int — survives if {@code lm_progression} subtree is partial. */
    public static final String ROOT_BREAKTHROUGHS = "pp_level_breakthroughs";

    private static final Map<UUID, Integer> CAP_BY_UUID = new ConcurrentHashMap<>();
    /**
     * Mohist often leaves {@code StatsData.player} null and identity-compare of
     * {@code DmzProgression.stats(sp) == data} fails across copies. Bind the live
     * cap onto the StatsData instance used by Overhaul mixins.
     */
    private static final Map<StatsData, Integer> CAP_BY_DATA =
            Collections.synchronizedMap(new WeakHashMap<>());

    private PersonalLevelCapMirror() {}

    /** Write cap + breakthrough count without re-reading (padmin set must stick). */
    public static void overwrite(ServerPlayer player, int breakthroughs, int cap) {
        if (player == null) {
            return;
        }
        int n = Math.max(0, breakthroughs);
        int c = Math.max(PrestigePointsSystem.BASE_LEVEL_CAP, cap);
        UUID id = player.m_20148_();
        CAP_BY_UUID.put(id, c);
        bindStatsData(player, c);
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(tag)) {
            return;
        }
        tag.m_128405_(KEY, c);
        tag.m_128405_(KEY_BREAKTHROUGHS, n);
        PersistentDataAccess.putInt(player, ROOT_BREAKTHROUGHS, n);
    }

    /** Remember this StatsData's personal cap so mixins work when {@code getPlayer()} is null. */
    public static void bind(StatsData data, int cap) {
        if (data == null) {
            return;
        }
        int c = Math.max(PrestigePointsSystem.BASE_LEVEL_CAP, cap);
        CAP_BY_DATA.put(data, c);
    }

    public static void bindStatsData(ServerPlayer player, int cap) {
        if (player == null) {
            return;
        }
        try {
            bind(DmzProgression.stats(player), cap);
        } catch (Throwable ignored) {
        }
    }

    public static void publish(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int breakthroughs = PrestigePointsSystem.getBreakthroughs(player);
        overwrite(player, breakthroughs, PrestigePointsSystem.effectiveMaxLevel(breakthroughs));
        try {
            DmzSkillUtil.sync(player);
        } catch (Throwable ignored) {
        }
    }

    public static int read(Player player) {
        if (player == null) {
            return 0;
        }
        int cached = CAP_BY_UUID.getOrDefault(player.m_20148_(), 0);
        if (cached > 0) {
            return cached;
        }
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!tag.m_128441_(KEY)) {
            return inferCapFromBreakthroughs(player);
        }
        return Math.max(0, tag.m_128451_(KEY));
    }

    /** Mohist-safe cap when {@link StatsData#getPlayer()} is null during mixin stat buys. */
    public static int resolveCap(StatsData data) {
        if (data == null) {
            return PrestigePointsSystem.BASE_LEVEL_CAP;
        }
        Integer bound = CAP_BY_DATA.get(data);
        if (bound != null && bound > 0) {
            return bound;
        }
        try {
            Player owner = data.getPlayer();
            if (owner instanceof ServerPlayer sp) {
                int cap = read(sp);
                if (cap > 0) {
                    bind(data, cap);
                    return cap;
                }
            }
        } catch (Throwable ignored) {
        }
        ServerPlayer resolved = LmStatsDataAccess.serverPlayer(data);
        if (resolved != null) {
            int cap = read(resolved);
            if (cap > 0) {
                bind(data, cap);
                return cap;
            }
            int live = PrestigePointsSystem.effectiveMaxLevel(resolved);
            bind(data, live);
            return live;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer sp : server.m_6846_().m_11314_()) {
                try {
                    if (DmzProgression.stats(sp) == data) {
                        int cap = read(sp);
                        int live = cap > 0 ? cap : PrestigePointsSystem.effectiveMaxLevel(sp);
                        bind(data, live);
                        return live;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return PrestigePointsSystem.ABSOLUTE_LEVEL_CAP;
    }

    private static int inferCapFromBreakthroughs(Player player) {
        return PrestigePointsSystem.ABSOLUTE_LEVEL_CAP;
    }
}
