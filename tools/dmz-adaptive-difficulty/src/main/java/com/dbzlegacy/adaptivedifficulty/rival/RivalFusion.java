package com.dbzlegacy.adaptivedifficulty.rival;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.BonusStats;
import com.dragonminez.common.stats.character.Status;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fusion hooks from Rival System 4.7.10.
 * Tick bonus near mutual rivals when fused; kill TP share to fusion partner.
 * Offense STR/SKP bonus only applied when {@link RivalConstants#OFFENSE_ENABLED}.
 */
public final class RivalFusion {
    public static final float KILL_TP = 650.0f;
    public static final double MAX_FUSION_BONUS = 0.25;
    public static final String BONUS_NAME = "Rival Fusion";
    private static final long TICK_MS = 1000L;
    private static final Map<UUID, Long> LAST_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PARTNER = new ConcurrentHashMap<>();

    private RivalFusion() {}

    public static void clear(UUID uuid) {
        if (uuid != null) {
            LAST_TICK.remove(uuid);
            PARTNER.remove(uuid);
        }
    }

    public static void pulse(MinecraftServer server, long now) {
        if (!DifficultyConfig.get().enableRivalSystem || server == null) {
            return;
        }
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player == null || !player.m_6084_()) {
                continue;
            }
            UUID id = player.m_20148_();
            Long last = LAST_TICK.get(id);
            if (last != null && now - last < TICK_MS) {
                continue;
            }
            LAST_TICK.put(id, now);
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        try {
            StatsData data = DmzProgression.stats(player);
            if (data == null) {
                return;
            }
            Status status = data.getStatus();
            if (status == null || !status.isFused()) {
                clearBonus(data);
                PARTNER.remove(player.m_20148_());
                return;
            }
            UUID partnerId = status.getFusionPartnerUUID();
            if (partnerId == null) {
                clearBonus(data);
                return;
            }
            PARTNER.put(player.m_20148_(), partnerId);
            double pts = mutualPoints(player.m_20148_().toString(), partnerId.toString());
            if (pts <= 0.0) {
                clearBonus(data);
                return;
            }
            if (RivalConstants.OFFENSE_ENABLED) {
                applyBonus(player, data, fusionMult(pts));
            }
        } catch (Throwable ignored) {
            // DMZ API best-effort
        }
    }

    public static void onMobKill(ServerPlayer killer) {
        if (killer == null || !DifficultyConfig.get().enableRivalSystem) {
            return;
        }
        try {
            StatsData data = DmzProgression.stats(killer);
            if (data == null) {
                return;
            }
            Status status = data.getStatus();
            if (status == null || !status.isFused()) {
                return;
            }
            UUID partnerId = status.getFusionPartnerUUID();
            if (partnerId == null) {
                return;
            }
            double pts = mutualPoints(killer.m_20148_().toString(), partnerId.toString());
            if (pts <= 0.0) {
                return;
            }
            MinecraftServer server = killer.m_20194_();
            if (server == null) {
                return;
            }
            ServerPlayer partner = server.m_6846_().m_11259_(partnerId);
            if (partner == null || !partner.m_6084_()) {
                return;
            }
            float tp = RivalTpCurve.scale(partner, KILL_TP, "burst");
            DmzRewards.awardTp(partner, tp, "Fusion share near rival", true, "§d[Fusion Rival] ");
        } catch (Throwable ignored) {
        }
    }

    private static double mutualPoints(String a, String b) {
        RivalPlayerRecord ra = RivalStore.get().get(a);
        RivalPlayerRecord rb = RivalStore.get().get(b);
        if (ra == null || rb == null) {
            return 0.0;
        }
        RivalLink la = ra.rivals.get(b);
        RivalLink lb = rb.rivals.get(a);
        if (la == null || lb == null || !la.mutual || !lb.mutual) {
            return 0.0;
        }
        return Math.max(0.0, la.points);
    }

    private static double fusionMult(double points) {
        double bonus = Math.min(MAX_FUSION_BONUS, 0.05 + (Math.max(0.0, points) / 15000.0) * 0.20);
        return 1.0 + bonus;
    }

    private static void clearBonus(StatsData data) {
        if (data == null || !RivalConstants.OFFENSE_ENABLED) {
            return;
        }
        try {
            BonusStats b = data.getBonusStats();
            if (b != null) {
                b.removeBonus("STR", BONUS_NAME);
                b.removeBonus("SKP", BONUS_NAME);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void applyBonus(ServerPlayer player, StatsData data, double mult) {
        if (data == null || !RivalConstants.OFFENSE_ENABLED) {
            return;
        }
        try {
            BonusStats b = data.getBonusStats();
            if (b == null) {
                return;
            }
            clearBonus(data);
            if (mult <= 1.001) {
                sync(player);
                return;
            }
            double str = 0;
            double skp = 0;
            try {
                str = data.getCurrentStatValue("STR");
            } catch (Throwable ignored) {
            }
            try {
                skp = data.getCurrentStatValue("SKP");
            } catch (Throwable ignored) {
            }
            String key = skp > str ? "SKP" : "STR";
            b.addBonus(key, BONUS_NAME, "*", mult);
            sync(player);
        } catch (Throwable ignored) {
        }
    }

    private static void sync(ServerPlayer player) {
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
    }
}
