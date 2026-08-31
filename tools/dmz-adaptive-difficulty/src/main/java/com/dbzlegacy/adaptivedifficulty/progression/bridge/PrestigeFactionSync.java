package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Port of {@code Fabled Prestige Faction Sync.js} — Prestige class level → CNPC faction 4 points.
 * Soft-depends on CustomNPCs via reflection ({@code PlayerData.get} / {@code PlayerFactionData}).
 */
public final class PrestigeFactionSync {
    private static final int FACTION_ID = 4;
    private static final String CLASS_NAME = "Prestige";
    private static final String NEXT_KEY = "prestige_faction_lock_next";
    private static final long CHECK_INTERVAL_MS = 5000L;

    private PrestigeFactionSync() {}

    /** Clear the throttle and sync faction held from Fabled Prestige immediately. */
    public static void forceSync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ProgressionData.storedPut(player, NEXT_KEY, 0L);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enablePrestigeFactionSync) {
            return;
        }
        long now = System.currentTimeMillis();
        long next = ProgressionData.storedGetLong(player, NEXT_KEY, 0L);
        if (now < next) {
            return;
        }
        ProgressionData.storedPut(player, NEXT_KEY, now + CHECK_INTERVAL_MS);

        Object data = FabledBridge.fabledData(player);
        if (data == null) {
            return;
        }

        int prestigeLevel = 0;
        try {
            Object prestigeClass = data.getClass()
                    .getMethod("getClass", String.class)
                    .invoke(data, CLASS_NAME);
            if (prestigeClass != null) {
                prestigeLevel = Math.max(0, FabledBridge.invokeInt(prestigeClass, "getLevel"));
            }
        } catch (Throwable ignored) {
        }

        int targetFaction = Math.max(0, prestigeLevel - 1);
        Integer current = readFactionPoints(player, FACTION_ID);
        if (current == null) {
            return;
        }
        if (current == targetFaction) {
            return;
        }
        int difference = targetFaction - current;
        if (!addFactionPoints(player, FACTION_ID, difference)) {
            return;
        }
        FabledBridge.logSync(
                player,
                "prestige_faction",
                "from",
                current,
                "to",
                targetFaction,
                "prestigeLevel",
                prestigeLevel);
    }

    private static Integer readFactionPoints(ServerPlayer player, int factionId) {
        // Prefer NpcAPI IPlayer
        try {
            Class<?> npcApi = Class.forName("noppes.npcs.api.NpcAPI");
            Object available = npcApi.getMethod("IsAvailable").invoke(null);
            if (available instanceof Boolean ok && ok) {
                Object api = npcApi.getMethod("Instance").invoke(null);
                Object entity = api.getClass()
                        .getMethod("getIEntity", net.minecraft.world.entity.Entity.class)
                        .invoke(api, player);
                if (entity != null) {
                    Object pts = entity.getClass()
                            .getMethod("getFactionPoints", int.class)
                            .invoke(entity, factionId);
                    if (pts instanceof Number n) {
                        return n.intValue();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        // Fallback: PlayerData capability
        try {
            Class<?> pd = Class.forName("noppes.npcs.controllers.data.PlayerData");
            Object data = pd.getMethod("get", Player.class).invoke(null, player);
            if (data == null) {
                return null;
            }
            Object factionData = data.getClass().getField("factionData").get(data);
            if (factionData == null) {
                return null;
            }
            Object pts = factionData.getClass()
                    .getMethod("getFactionPoints", Player.class, int.class)
                    .invoke(factionData, player, factionId);
            if (pts instanceof Number n) {
                return n.intValue();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean addFactionPoints(ServerPlayer player, int factionId, int delta) {
        try {
            Class<?> npcApi = Class.forName("noppes.npcs.api.NpcAPI");
            Object available = npcApi.getMethod("IsAvailable").invoke(null);
            if (available instanceof Boolean ok && ok) {
                Object api = npcApi.getMethod("Instance").invoke(null);
                Object entity = api.getClass()
                        .getMethod("getIEntity", net.minecraft.world.entity.Entity.class)
                        .invoke(api, player);
                if (entity != null) {
                    entity.getClass()
                            .getMethod("addFactionPoints", int.class, int.class)
                            .invoke(entity, factionId, delta);
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> pd = Class.forName("noppes.npcs.controllers.data.PlayerData");
            Object data = pd.getMethod("get", Player.class).invoke(null, player);
            if (data == null) {
                return false;
            }
            Object factionData = data.getClass().getField("factionData").get(data);
            if (factionData == null) {
                return false;
            }
            factionData.getClass()
                    .getMethod("increasePoints", Player.class, int.class, int.class)
                    .invoke(factionData, player, factionId, delta);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
