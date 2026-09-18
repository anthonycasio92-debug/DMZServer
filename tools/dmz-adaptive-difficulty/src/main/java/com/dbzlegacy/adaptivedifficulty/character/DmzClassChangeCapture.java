package com.dbzlegacy.adaptivedifficulty.character;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** {@link StatsData#snapshotMultiplierResources()} taken before DMZ applies a new fighting class. */
public final class DmzClassChangeCapture {
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private DmzClassChangeCapture() {}

    /** Snapshot + fighting class id on the character before DMZ applies {@code UpdateCharacterC2S}. */
    public record Pending(float[] resources, String fightingClassBefore) {}

    public static void store(ServerPlayer player, float[] snapshot) {
        store(player, snapshot, null);
    }

    public static void store(ServerPlayer player, float[] snapshot, String fightingClassBefore) {
        if (player == null || snapshot == null || snapshot.length < 3) {
            return;
        }
        String prior =
                fightingClassBefore == null || fightingClassBefore.isBlank()
                        ? ""
                        : fightingClassBefore.trim();
        PENDING.put(player.m_20148_(), new Pending(snapshot.clone(), prior));
    }

    public static Pending takePending(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        return PENDING.remove(player.m_20148_());
    }

    public static float[] take(ServerPlayer player) {
        Pending pending = takePending(player);
        return pending == null ? null : pending.resources();
    }
}
