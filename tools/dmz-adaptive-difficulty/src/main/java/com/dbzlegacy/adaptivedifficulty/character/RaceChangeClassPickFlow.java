package com.dbzlegacy.adaptivedifficulty.character;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.OpenRecustomizeS2C;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * After a preserved-stat race change when the old fighting class does not exist on the new race,
 * opens DMZ recustomize so the player picks a class at no extra cost (included in race change).
 */
public final class RaceChangeClassPickFlow {
    private static final long SESSION_MS = 30L * 60L * 1000L;
    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();

    private RaceChangeClassPickFlow() {}

    public static void begin(ServerPlayer player, String targetRaceId, String priorFightingClass) {
        if (player == null || targetRaceId == null || targetRaceId.isBlank()) {
            return;
        }
        String prior = priorFightingClass == null ? "" : priorFightingClass.trim().toLowerCase();
        ACTIVE.put(
                player.m_20148_(),
                new Session(targetRaceId.trim().toLowerCase(), System.currentTimeMillis(), prior));
    }

    public static boolean isActive(ServerPlayer player) {
        return session(player) != null;
    }

    public static String targetRaceId(ServerPlayer player) {
        Session session = session(player);
        return session == null ? "" : session.targetRaceId;
    }

    public static String priorFightingClass(ServerPlayer player) {
        Session session = session(player);
        return session == null ? "" : session.priorFightingClass;
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            ACTIVE.remove(player.m_20148_());
        }
    }

    public static void openRecustomizeEditor(ServerPlayer player) {
        if (player == null) {
            return;
        }
        var server = player.m_20194_();
        Runnable open = () -> {
            try {
                NetworkHandler.sendToPlayer(new OpenRecustomizeS2C(), player);
            } catch (Throwable ignored) {
            }
        };
        if (server != null) {
            server.execute(open);
        } else {
            open.run();
        }
    }

    private static Session session(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        Session s = ACTIVE.get(player.m_20148_());
        if (s == null) {
            return null;
        }
        if (System.currentTimeMillis() - s.startedAt > SESSION_MS) {
            ACTIVE.remove(player.m_20148_());
            return null;
        }
        return s;
    }

    private record Session(String targetRaceId, long startedAt, String priorFightingClass) {}
}
