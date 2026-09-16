package com.dbzlegacy.adaptivedifficulty.character;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** {@link StatsData#snapshotMultiplierResources()} taken before DMZ applies a new fighting class. */
public final class DmzClassChangeCapture {
    private static final Map<UUID, float[]> PENDING = new ConcurrentHashMap<>();

    private DmzClassChangeCapture() {}

    public static void store(ServerPlayer player, float[] snapshot) {
        if (player == null || snapshot == null || snapshot.length < 3) {
            return;
        }
        PENDING.put(player.m_20148_(), snapshot.clone());
    }

    public static float[] take(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        return PENDING.remove(player.m_20148_());
    }
}
