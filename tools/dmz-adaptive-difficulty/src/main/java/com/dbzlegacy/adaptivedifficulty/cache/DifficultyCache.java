package com.dbzlegacy.adaptivedifficulty.cache;

import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Cache player difficulty; invalidate on join/leave/level/prestige/settings changes
 * (concept doc performance rules).
 */
public final class DifficultyCache {
    private static final Map<UUID, DifficultySnapshot> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerDifficultyData> DATA = new ConcurrentHashMap<>();

    private DifficultyCache() {}

    public static PlayerDifficultyData data(ServerPlayer player) {
        return DATA.computeIfAbsent(player.m_20148_(), id ->
                PlayerDifficultyData.fromPlayerNbt(PersistentDataAccess.get(player)));
    }

    public static void putData(ServerPlayer player, PlayerDifficultyData data) {
        DATA.put(player.m_20148_(), data);
        invalidate(player.m_20148_());
    }

    public static DifficultySnapshot get(ServerPlayer player) {
        return CACHE.computeIfAbsent(player.m_20148_(), id -> {
            PlayerDifficultyData data = data(player);
            return DifficultyCalculator.snapshot(player, data);
        });
    }

    public static DifficultySnapshot refresh(ServerPlayer player) {
        invalidate(player.m_20148_());
        DifficultySnapshot snap = DifficultyCalculator.snapshot(player, data(player));
        CACHE.put(player.m_20148_(), snap);
        return snap;
    }

    public static void invalidate(UUID id) {
        if (id != null) {
            CACHE.remove(id);
        }
    }

    public static void invalidateAll() {
        CACHE.clear();
    }

    public static void remove(UUID id) {
        CACHE.remove(id);
        DATA.remove(id);
    }

    public static void save(ServerPlayer player) {
        PlayerDifficultyData data = DATA.get(player.m_20148_());
        if (data != null) {
            data.writeToPlayerNbt(PersistentDataAccess.get(player));
        }
    }
}
