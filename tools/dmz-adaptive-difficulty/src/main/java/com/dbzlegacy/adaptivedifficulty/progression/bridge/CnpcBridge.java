package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Writes CustomNPCs player tempdata from Java when a caller still needs it.
 */
public final class CnpcBridge {
    private CnpcBridge() {}

    public static void putTempString(ServerPlayer player, String key, String value) {
        if (player == null || key == null || value == null) {
            return;
        }
        try {
            Class<?> npcApi = Class.forName("noppes.npcs.api.NpcAPI");
            Object available = npcApi.getMethod("IsAvailable").invoke(null);
            if (!(available instanceof Boolean ok) || !ok) {
                return;
            }
            Object api = npcApi.getMethod("Instance").invoke(null);
            Object iEntity = api.getClass()
                    .getMethod("getIEntity", Entity.class)
                    .invoke(api, player);
            if (iEntity == null) {
                return;
            }
            Object temp = iEntity.getClass().getMethod("getTempdata").invoke(iEntity);
            if (temp == null) {
                return;
            }
            temp.getClass().getMethod("put", String.class, Object.class).invoke(temp, key, value);
        } catch (Throwable ignored) {
        }
    }
}
