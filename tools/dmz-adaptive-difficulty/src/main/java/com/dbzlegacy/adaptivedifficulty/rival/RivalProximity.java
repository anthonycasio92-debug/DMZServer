package com.dbzlegacy.adaptivedifficulty.rival;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.sparring.SparringSystem;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Rival proximity pulse: presence TP + surpass awards.
 * Skips when sparring or in an official challenge.
 */
public final class RivalProximity {
    private static final Map<UUID, Long> LAST_PULSE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MOB_KILL = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_PRESENCE = new ConcurrentHashMap<>();

    private RivalProximity() {}

    public static void markMobKill(UUID uuid) {
        if (uuid != null) {
            LAST_MOB_KILL.put(uuid, System.currentTimeMillis());
        }
    }

    public static void clearPlayer(UUID uuid) {
        if (uuid == null) {
            return;
        }
        LAST_PULSE.remove(uuid);
        LAST_MOB_KILL.remove(uuid);
        String prefix = uuid + ">";
        LAST_PRESENCE.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public static boolean hasRecentMobKill(UUID uuid, long now) {
        if (uuid == null) {
            return false;
        }
        Long at = LAST_MOB_KILL.get(uuid);
        return at != null && now - at <= RivalConstants.ACTIVE_KILL_MS;
    }

    public static void pulse(MinecraftServer server, long now) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableRivalSystem || !cfg.rivalPresenceTp || server == null) {
            return;
        }
        RivalStore store = RivalStore.get();
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player == null || !player.m_6084_()) {
                continue;
            }
            UUID id = player.m_20148_();
            Long last = LAST_PULSE.get(id);
            if (last != null && now - last < RivalConstants.PROXIMITY_PULSE_MS) {
                continue;
            }
            LAST_PULSE.put(id, now);
            processPlayer(server, store, player, now);
        }
    }

    private static void processPlayer(
            MinecraftServer server,
            RivalStore store,
            ServerPlayer player,
            long now
    ) {
        UUID id = player.m_20148_();
        if (RivalChallengeManager.get().isInChallenge(id) || SparringSystem.isSparring(id)) {
            return;
        }
        RivalPlayerRecord record = store.ensurePlayer(player);
        if (record == null || record.rivals.isEmpty()) {
            return;
        }

        double myReleased = DmzRewards.releasedBattlePower(player);
        List<NearRival> near = new ArrayList<>();

        for (Map.Entry<String, RivalLink> e : record.rivals.entrySet()) {
            RivalLink link = e.getValue();
            if (link == null) {
                continue;
            }
            UUID rivalId;
            try {
                rivalId = UUID.fromString(e.getKey());
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ServerPlayer rival = server.m_6846_().m_11259_(rivalId);
            if (rival == null || !rival.m_6084_() || rival.m_9236_() != player.m_9236_()) {
                continue;
            }
            double range = RivalConstants.rangeForPoints(link.points);
            double dist = player.m_20270_(rival);
            if (dist > range) {
                continue;
            }
            link.lastSeenTogetherAt = now;
            link.presenceMs += RivalConstants.PROXIMITY_PULSE_MS;
            record.presenceMs += RivalConstants.PROXIMITY_PULSE_MS;
            store.markDirty();

            // Active rival must have a recent mob kill (anti-AFK).
            if (!hasRecentMobKill(rivalId, now)) {
                continue;
            }

            double rivalReleased = DmzRewards.releasedBattlePower(rival);
            handleSurpass(player, record, link, e.getKey(), myReleased, rivalReleased, now);

            near.add(new NearRival(e.getKey(), link, rival, priority(link)));
        }

        near.sort(Comparator.comparingInt((NearRival n) -> n.priority).reversed());
        int awarded = 0;
        for (NearRival n : near) {
            if (awarded >= RivalConstants.PRESENCE_CAP) {
                break;
            }
            awarded++;
            String key = id + ">" + n.rivalUuid;
            Long last = LAST_PRESENCE.get(key);
            if (last != null && now - last < RivalConstants.PRESENCE_INTERVAL_MS) {
                continue;
            }
            LAST_PRESENCE.put(key, now);
            float presenceTp = presenceAmount(n.link);
            if (presenceTp > 0.0f) {
                float scaled = RivalConstants.scaleTp(presenceTp);
                RivalStatus st = n.link.status();
                DmzRewards.awardTp(
                        player,
                        scaled,
                        "Near " + st.id() + " " + n.link.name,
                        record.tpMessages,
                        "§6[Rival] "
                );
            }
        }
    }

    private static float presenceAmount(RivalLink link) {
        if (link.mutual) {
            return link.status() == RivalStatus.NEMESIS
                    ? RivalConstants.PRESENCE_TP_NEMESIS
                    : RivalConstants.PRESENCE_TP_MUTUAL;
        }
        if (link.declaredByMe) {
            return RivalConstants.PRESENCE_TP_ONE_SIDED;
        }
        return 0.0f;
    }

    private static void handleSurpass(
            ServerPlayer player,
            RivalPlayerRecord record,
            RivalLink link,
            String rivalUuid,
            double myReleased,
            double rivalReleased,
            long now
    ) {
        if (!(rivalReleased > 0.0)) {
            return;
        }
        if (myReleased <= rivalReleased) {
            if (!link.surpassWasBelow) {
                link.surpassWasBelow = true;
                RivalStore.get().markDirty();
            }
            return;
        }
        if (!link.surpassWasBelow) {
            return;
        }
        long lastRival = Math.max(link.lastSurpassAt, record.surpassCooldown.getOrDefault(rivalUuid, 0L));
        long lastGlobal = record.lastSurpassAt;
        if (now - lastRival < RivalConstants.SURPASS_COOLDOWN_MS
                || now - lastGlobal < RivalConstants.SURPASS_GLOBAL_COOLDOWN_MS) {
            link.surpassWasBelow = false;
            RivalStore.get().markDirty();
            return;
        }
        float tp = RivalConstants.scaleTp(RivalConstants.SURPASS_TP);
        if (DmzRewards.awardTp(player, tp, "Surpassed " + link.name, true, "§6[Rival] ")) {
            record.surpassAwards++;
            record.lastSurpassAt = now;
            link.lastSurpassAt = now;
            link.surpassWasBelow = false;
            record.surpassCooldown.put(rivalUuid, now);
            RivalStore.get().markDirty();
            DmzRewards.msg(player, "§8[Rival] Surpass cooldown: 6h for " + link.name + ", 1h global.");
        }
    }

    private static int priority(RivalLink link) {
        RivalStatus st = link.status();
        return switch (st) {
            case NEMESIS -> 50;
            case MUTUAL -> 40;
            case DECLARED -> 30;
            case PENDING -> 20;
            case UNKNOWN -> 10;
            default -> 0;
        } + (int) Math.min(20, link.points / 500.0);
    }

    private record NearRival(String rivalUuid, RivalLink link, ServerPlayer rival, int priority) {}
}
