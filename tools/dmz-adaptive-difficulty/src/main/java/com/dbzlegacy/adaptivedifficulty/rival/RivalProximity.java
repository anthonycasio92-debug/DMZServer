package com.dbzlegacy.adaptivedifficulty.rival;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.sparring.SparringSystem;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.LmChat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Rival proximity pulse: presence TP + surpass + kill-near / underdog / anti-gank.
 * Skips when sparring or in an official challenge.
 */
public final class RivalProximity {
    private static final Map<UUID, Long> LAST_PULSE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MOB_KILL = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_PRESENCE = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_UNDERDOG_ENGAGE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_ANTIGANK_HIT = new ConcurrentHashMap<>();

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
        LAST_ANTIGANK_HIT.remove(uuid);
        String prefix = uuid + ">";
        LAST_PRESENCE.keySet().removeIf(k -> k.startsWith(prefix));
        LAST_UNDERDOG_ENGAGE.keySet().removeIf(k -> k.startsWith(prefix));
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
                float scaled = RivalTpCurve.scale(player, presenceTp, "drip");
                RivalStatus st = n.link.status();
                if (DmzRewards.awardTp(
                        player,
                        scaled,
                        "Near " + st.id() + " " + n.link.name,
                        record.tpMessages,
                        "Rival");
                )) {
                    SystemTelemetry.log("rival", "presence_tp", player, n.rival,
                            SystemTelemetry.fields("tp", scaled, "status", st.id()));
                }
            }
        }
    }

    /** Mob (or player) kill near rivals — kill TP + anti-gank witness. */
    public static void handleKillNearRivals(ServerPlayer killer, LivingEntity victim) {
        if (!DifficultyConfig.get().enableRivalSystem || killer == null) {
            return;
        }
        if (SparringSystem.isSparring(killer.m_20148_())
                || RivalChallengeManager.get().isInChallenge(killer.m_20148_())) {
            return;
        }
        RivalStore store = RivalStore.get();
        RivalPlayerRecord record = store.ensurePlayer(killer);
        if (record == null || record.rivals.isEmpty()) {
            return;
        }
        MinecraftServer server = killer.m_20194_();
        if (server == null) {
            return;
        }
        double killerReleased = DmzRewards.releasedBattlePower(killer);
        boolean victimIsPlayer = victim instanceof ServerPlayer;
        String victimUuid = victimIsPlayer ? victim.m_20148_().toString() : "";

        if (victimIsPlayer) {
            RivalLink direct = record.rivals.get(victimUuid);
            // Underdog win: you rivaled them and they are stronger (any status).
            if (direct != null && direct.declaredByMe) {
                double victimReleased = DmzRewards.releasedBattlePower((ServerPlayer) victim);
                if (victimReleased > killerReleased) {
                    float tp = RivalTpCurve.scale(killer, RivalConstants.UNDERDOG_WIN_TP, "burst");
                    DmzRewards.awardTp(killer, tp, "Underdog victory vs " + direct.name, true, "Rival");
                    SystemTelemetry.log("rival", "underdog_win", killer, (ServerPlayer) victim,
                            SystemTelemetry.fields("tp", tp, "status", direct.status().id()));
                }
            }
        }

        List<NearRival> nearKillList = new ArrayList<>();
        for (Map.Entry<String, RivalLink> e : record.rivals.entrySet()) {
            if (victimIsPlayer && e.getKey().equals(victimUuid)) {
                continue;
            }
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
            if (rival == null || !rival.m_6084_() || rival.m_9236_() != killer.m_9236_()) {
                continue;
            }
            if (killer.m_20270_(rival) > RivalConstants.rangeForPoints(link.points)) {
                continue;
            }
            if (!hasRecentMobKill(rivalId, System.currentTimeMillis())) {
                continue;
            }
            nearKillList.add(new NearRival(e.getKey(), link, rival, priority(link)));
        }
        nearKillList.sort(Comparator.comparingInt((NearRival n) -> n.priority).reversed());
        int cap = RivalConstants.NEAR_RIVAL_KILL_CAP;
        for (int i = 0; i < nearKillList.size() && i < cap; i++) {
            NearRival near = nearKillList.get(i);
            int tier = RivalConstants.tierIndex(near.link.points);
            float tp = RivalConstants.KILL_TP_BASE + tier * RivalConstants.KILL_TP_PER_TIER;
            RivalStatus st = near.link.status();
            if (st == RivalStatus.NEMESIS) {
                tp = (float) Math.floor(tp * RivalConstants.KILL_TP_MUTUAL_MULT * 1.25);
            } else if (st == RivalStatus.MUTUAL) {
                tp = (float) Math.floor(tp * RivalConstants.KILL_TP_MUTUAL_MULT);
            }
            float scaled = RivalTpCurve.scale(killer, tp, "burst");
            DmzRewards.awardTp(killer, scaled, "Near " + st.id() + " " + near.link.name,
                    record.tpMessages, "Rival");
            SystemTelemetry.log("rival", "kill_near_tp", killer, near.rival,
                    SystemTelemetry.fields("tp", scaled, "status", st.id()));
            store.markDirty();

            RivalPlayerRecord rivalRecord = store.get(near.rivalUuid);
            if (rivalRecord == null) {
                continue;
            }
            RivalLink theirLink = rivalRecord.rivals.get(killer.m_20148_().toString());
            if (theirLink == null || !theirLink.declaredByMe) {
                continue;
            }
            double rivalReleased = DmzRewards.releasedBattlePower(near.rival);
            if (killerReleased > 0 && rivalReleased <= killerReleased * RivalConstants.ANTIGANK_RATIO) {
                float witness = RivalTpCurve.scale(killer, RivalConstants.ANTIGANK_WITNESS_KILL_TP, "burst");
                DmzRewards.awardTp(killer, witness, "Rivals watching", record.tpMessages, "Rival");
                SystemTelemetry.log("rival", "antigank_witness", killer, near.rival,
                        SystemTelemetry.fields("tp", witness));
            }
        }
    }

    /** Underdog engage when you rival a stronger player (any status you declared). */
    public static void tryUnderdogEngage(ServerPlayer underdog, ServerPlayer stronger) {
        if (!DifficultyConfig.get().enableRivalSystem || underdog == null || stronger == null) {
            return;
        }
        if (RivalChallengeManager.get().isInChallenge(underdog.m_20148_())
                || SparringSystem.isSparring(underdog.m_20148_())) {
            return;
        }
        RivalPlayerRecord record = RivalStore.get().ensurePlayer(underdog);
        RivalLink link = record.rivals.get(stronger.m_20148_().toString());
        // Benefits require declaredByMe — Silent / Declared / Mutual / Nemesis all count.
        if (link == null || !link.declaredByMe) {
            return;
        }
        double myReleased = DmzRewards.releasedBattlePower(underdog);
        double theirReleased = DmzRewards.releasedBattlePower(stronger);
        if (theirReleased <= myReleased) {
            return;
        }
        long now = System.currentTimeMillis();
        String key = underdog.m_20148_() + ">" + stronger.m_20148_();
        Long last = LAST_UNDERDOG_ENGAGE.get(key);
        if (last != null && now - last < RivalConstants.UNDERDOG_ENGAGE_COOLDOWN_MS) {
            return;
        }
        LAST_UNDERDOG_ENGAGE.put(key, now);
        float tp = RivalTpCurve.scale(underdog, RivalConstants.UNDERDOG_ENGAGE_TP, "drip");
        DmzRewards.awardTp(underdog, tp, "Engaging rival", record.tpMessages, "Rival");
        SystemTelemetry.log("rival", "underdog_engage", underdog, stronger,
                SystemTelemetry.fields("tp", tp, "status", link.status().id()));
    }

    public static void handleDamagedByRival(ServerPlayer victim, ServerPlayer attacker) {
        if (victim == null || attacker == null) {
            return;
        }
        tryUnderdogEngage(victim, attacker);
        tryUnderdogEngage(attacker, victim);
        handleStrongDamagedNearWeak(victim);
    }

    /** Weak rivals gain drip TP when their stronger declared target takes hits. */
    public static void handleStrongDamagedNearWeak(ServerPlayer victim) {
        if (!DifficultyConfig.get().enableRivalSystem || victim == null) {
            return;
        }
        if (RivalChallengeManager.get().isInChallenge(victim.m_20148_())) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_ANTIGANK_HIT.get(victim.m_20148_());
        if (last != null && now - last < RivalConstants.ANTIGANK_HIT_COOLDOWN_MS) {
            return;
        }
        RivalStore store = RivalStore.get();
        RivalPlayerRecord record = store.ensurePlayer(victim);
        if (record == null || record.rivals.isEmpty()) {
            return;
        }
        MinecraftServer server = victim.m_20194_();
        if (server == null) {
            return;
        }
        double victimReleased = DmzRewards.releasedBattlePower(victim);
        if (!(victimReleased > 0.0)) {
            return;
        }
        boolean awarded = false;
        for (Map.Entry<String, RivalLink> e : record.rivals.entrySet()) {
            RivalPlayerRecord rivalRecord = store.get(e.getKey());
            if (rivalRecord == null) {
                continue;
            }
            RivalLink theirLink = rivalRecord.rivals.get(victim.m_20148_().toString());
            if (theirLink == null || !theirLink.declaredByMe) {
                continue;
            }
            UUID rivalId;
            try {
                rivalId = UUID.fromString(e.getKey());
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ServerPlayer rival = server.m_6846_().m_11259_(rivalId);
            if (rival == null || !rival.m_6084_()) {
                continue;
            }
            RivalLink myLink = e.getValue();
            if (myLink == null || victim.m_20270_(rival) > RivalConstants.rangeForPoints(myLink.points)) {
                continue;
            }
            double rivalReleased = DmzRewards.releasedBattlePower(rival);
            if (rivalReleased > victimReleased * RivalConstants.ANTIGANK_RATIO) {
                continue;
            }
            float tp = RivalTpCurve.scale(rival, RivalConstants.ANTIGANK_HIT_TP, "drip");
            DmzRewards.awardTp(rival, tp, "Rival under fire", rivalRecord.tpMessages, "Rival");
            SystemTelemetry.log("rival", "antigank_hit", rival, victim,
                    SystemTelemetry.fields("tp", tp));
            awarded = true;
        }
        if (awarded) {
            LAST_ANTIGANK_HIT.put(victim.m_20148_(), now);
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
        float tp = RivalTpCurve.scale(player, RivalConstants.SURPASS_TP, "burst");
        if (DmzRewards.awardTp(player, tp, "Surpassed " + link.name, true, "Rival")) {
            record.surpassAwards++;
            record.lastSurpassAt = now;
            link.lastSurpassAt = now;
            link.surpassWasBelow = false;
            record.surpassCooldown.put(rivalUuid, now);
            RivalStore.get().markDirty();
            SystemTelemetry.log("rival", "surpass", player, null,
                    SystemTelemetry.fields("tp", tp, "rival", link.name));
            DmzRewards.msg(player, LmChat.info("Rival",
                    "Surpass cooldown: 6h for " + link.name + ", 1h global."));
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
