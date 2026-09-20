package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Session temp + persistent NBT storage for progression scripts
 * (replaces CNPC tempdata / storeddata).
 */
public final class ProgressionData {
    private static final String ROOT = "lm_progression";
    private static final Map<UUID, Map<String, String>> TEMP = new ConcurrentHashMap<>();

    private ProgressionData() {}

    public static void clearPlayer(UUID id) {
        if (id != null) {
            TEMP.remove(id);
        }
    }

    public static Map<String, String> temp(ServerPlayer player) {
        return TEMP.computeIfAbsent(player.m_20148_(), u -> new ConcurrentHashMap<>());
    }

    public static boolean tempHas(ServerPlayer player, String key) {
        Map<String, String> t = TEMP.get(player.m_20148_());
        return t != null && t.containsKey(key);
    }

    public static String tempGet(ServerPlayer player, String key, String fallback) {
        Map<String, String> t = TEMP.get(player.m_20148_());
        if (t == null) {
            return fallback;
        }
        return t.getOrDefault(key, fallback);
    }

    public static long tempGetLong(ServerPlayer player, String key, long fallback) {
        try {
            String v = tempGet(player, key, null);
            if (v == null || v.isBlank()) {
                return fallback;
            }
            return Long.parseLong(v);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static double tempGetDouble(ServerPlayer player, String key, double fallback) {
        try {
            String v = tempGet(player, key, null);
            if (v == null || v.isBlank()) {
                return fallback;
            }
            return Double.parseDouble(v);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static void tempPut(ServerPlayer player, String key, String value) {
        temp(player).put(key, value == null ? "" : value);
    }

    public static void tempPut(ServerPlayer player, String key, long value) {
        tempPut(player, key, Long.toString(value));
    }

    public static void tempPut(ServerPlayer player, String key, double value) {
        tempPut(player, key, Double.toString(value));
    }

    public static void tempRemove(ServerPlayer player, String key) {
        Map<String, String> t = TEMP.get(player.m_20148_());
        if (t != null) {
            t.remove(key);
        }
    }

    public static CompoundTag stored(ServerPlayer player) {
        CompoundTag root = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(root)) {
            return new CompoundTag();
        }
        if (!root.m_128441_(ROOT)) {
            root.m_128365_(ROOT, new CompoundTag());
        }
        return root.m_128469_(ROOT);
    }

    public static boolean storedWritable(ServerPlayer player) {
        return PersistentDataAccess.isWritable(PersistentDataAccess.get(player));
    }

    public static boolean storedHas(ServerPlayer player, String key) {
        CompoundTag tag = stored(player);
        return tag.m_128441_(key);
    }

    public static String storedGet(ServerPlayer player, String key, String fallback) {
        CompoundTag tag = stored(player);
        if (!tag.m_128441_(key)) {
            return fallback;
        }
        return tag.m_128461_(key);
    }

    public static long storedGetLong(ServerPlayer player, String key, long fallback) {
        CompoundTag tag = stored(player);
        if (tag.m_128441_(key)) {
            try {
                return tag.m_128454_(key);
            } catch (Throwable ignored) {
            }
            try {
                return tag.m_128451_(key);
            } catch (Throwable ignored) {
            }
        }
        try {
            String v = storedGet(player, key, null);
            if (v == null || v.isBlank()) {
                return fallback;
            }
            return Long.parseLong(v);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static double storedGetDouble(ServerPlayer player, String key, double fallback) {
        try {
            String v = storedGet(player, key, null);
            if (v == null || v.isBlank()) {
                return fallback;
            }
            return Double.parseDouble(v);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static boolean storedGetBool(ServerPlayer player, String key) {
        String v = storedGet(player, key, "false");
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    public static void storedPut(ServerPlayer player, String key, String value) {
        if (!storedWritable(player)) {
            return;
        }
        stored(player).m_128359_(key, value == null ? "" : value);
    }

    public static void storedPut(ServerPlayer player, String key, long value) {
        storedPut(player, key, Long.toString(value));
    }

    public static void storedPut(ServerPlayer player, String key, double value) {
        storedPut(player, key, Double.toString(value));
    }

    public static void storedPutBool(ServerPlayer player, String key, boolean value) {
        storedPut(player, key, value ? "true" : "false");
    }

    public static void storedRemove(ServerPlayer player, String key) {
        if (!storedWritable(player)) {
            return;
        }
        stored(player).m_128473_(key);
    }
}
