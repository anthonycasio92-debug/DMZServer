package com.dbzlegacy.adaptivedifficulty.rival;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Status;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Rival Instinct alerts for mutual / nemesis rivals (arrive / BP / charging / form / fusion).
 */
public final class RivalInstinct {
    private static final Map<UUID, Long> LAST_PULSE = new ConcurrentHashMap<>();
    private static final Map<String, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> WAS_NEAR = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> LAST_FORM_MULT = new ConcurrentHashMap<>();

    private RivalInstinct() {}

    public static void clearPlayer(UUID uuid) {
        if (uuid == null) {
            return;
        }
        LAST_PULSE.remove(uuid);
        LAST_FORM_MULT.remove(uuid);
        String prefix = uuid + ">";
        COOLDOWNS.keySet().removeIf(k -> k.startsWith(prefix));
        WAS_NEAR.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public static void pulse(MinecraftServer server, long now) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableRivalSystem || !cfg.rivalInstinct || server == null) {
            return;
        }
        RivalStore store = RivalStore.get();
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player == null || !player.m_6084_()) {
                continue;
            }
            UUID id = player.m_20148_();
            if (RivalChallengeManager.get().isInChallenge(id)) {
                continue;
            }
            Long last = LAST_PULSE.get(id);
            if (last != null && now - last < RivalConstants.INSTINCT_PULSE_MS) {
                continue;
            }
            LAST_PULSE.put(id, now);
            process(server, store, player, now);
        }
    }

    private static void process(MinecraftServer server, RivalStore store, ServerPlayer player, long now) {
        RivalPlayerRecord record = store.ensurePlayer(player);
        if (record == null) {
            return;
        }
        UUID id = player.m_20148_();
        double myReleased = DmzRewards.releasedBattlePower(player);

        for (Map.Entry<String, RivalLink> e : record.rivals.entrySet()) {
            RivalLink link = e.getValue();
            if (link == null || !link.mutual) {
                continue;
            }
            RivalStatus st = link.status();
            if (st != RivalStatus.MUTUAL && st != RivalStatus.NEMESIS) {
                continue;
            }
            UUID rivalId;
            try {
                rivalId = UUID.fromString(e.getKey());
            } catch (IllegalArgumentException ex) {
                continue;
            }
            String nearKey = id + ">" + rivalId;
            ServerPlayer rival = server.m_6846_().m_11259_(rivalId);
            double range = RivalConstants.rangeForPoints(link.points);
            boolean near = rival != null
                    && rival.m_6084_()
                    && rival.m_9236_() == player.m_9236_()
                    && player.m_20270_(rival) <= range;

            boolean wasNear = Boolean.TRUE.equals(WAS_NEAR.get(nearKey));
            if (!near) {
                WAS_NEAR.put(nearKey, false);
                continue;
            }
            WAS_NEAR.put(nearKey, true);
            double dist = player.m_20270_(rival);
            double theirReleased = DmzRewards.releasedBattlePower(rival);
            String tag = st == RivalStatus.NEMESIS ? "Nemesis" : "Mutual";

            if (!wasNear) {
                String arrive = "§6[Rival Instinct] §e" + link.name
                        + " §7arrived (" + (int) dist + "m)";
                arrive += " §7- feels " + relative(myReleased, theirReleased);
                arrive += " §8[" + tag + "]";
                alert(nearKey + ":arrive", arrive, player, now, RivalConstants.INSTINCT_ARRIVE_CD_MS);
            } else {
                alert(nearKey + ":bp",
                        "§b[Rival Instinct] §e" + link.name
                                + " §7still nearby - released BP ~ §f" + DmzRewards.formatWhole(theirReleased),
                        player, now, RivalConstants.INSTINCT_STATUS_CD_MS);
            }

            try {
                StatsData data = DmzProgression.stats(rival);
                Status status = data == null ? null : data.getStatus();
                if (status != null) {
                    if (status.isChargingKi() || status.isActionCharging()) {
                        alert(nearKey + ":charge",
                                "§c[Rival Instinct] §e" + link.name + " §cis charging ki!",
                                player, now, RivalConstants.INSTINCT_EVENT_CD_MS);
                    }
                    if (status.isAuraActive()) {
                        alert(nearKey + ":aura",
                                "§d[Rival Instinct] §e" + link.name + " §dtransformation / aura surge!",
                                player, now, RivalConstants.INSTINCT_EVENT_CD_MS);
                    }
                    if (status.getFusionPartnerUUID() != null || status.isFusionLeader()) {
                        String fname = status.getFusionName();
                        alert(nearKey + ":fusion",
                                "§5[Rival Instinct] §e" + link.name + " §5is fused"
                                        + (fname == null || fname.isBlank() ? "!" : " (" + fname + ")!"),
                                player, now, RivalConstants.INSTINCT_EVENT_CD_MS);
                    }
                }
                double formMult = DmzProgression.transformationPower(rival);
                Double prev = LAST_FORM_MULT.put(rivalId, formMult);
                if (prev != null && formMult > prev * 1.15) {
                    alert(nearKey + ":form",
                            "§d[Rival Instinct] §e" + link.name + " §dpower surged!",
                            player, now, RivalConstants.INSTINCT_EVENT_CD_MS);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void alert(String key, String message, ServerPlayer player, long now, long cd) {
        Long last = COOLDOWNS.get(key);
        if (last != null && now - last < cd) {
            return;
        }
        COOLDOWNS.put(key, now);
        DmzRewards.msg(player, message);
    }

    private static String relative(double mine, double theirs) {
        if (!(mine > 0.0) || !(theirs > 0.0)) {
            return "unclear";
        }
        double ratio = theirs / mine;
        if (ratio >= 1.35) {
            return "much stronger";
        }
        if (ratio >= 1.10) {
            return "stronger";
        }
        if (ratio <= 0.75) {
            return "much weaker";
        }
        if (ratio <= 0.90) {
            return "weaker";
        }
        return "even";
    }
}
