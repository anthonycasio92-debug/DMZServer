package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Race lock is retired. This repeats the old client clear so leftover padlocks drop.
 * Login clear in the KubeJS script still runs as well; both sends are empty payloads.
 */
public final class RaceLockClear {
    public static final int SYNC_INTERVAL_TICKS = 200;
    private static final String CHANNEL = "dmz_race_locks";
    private static Method sendData;
    private static boolean looked;
    private static boolean warned;

    private RaceLockClear() {}

    public static void clear(ServerPlayer player) {
        if (player == null || !HotPathConfig.get().raceLocks.enabled) {
            return;
        }
        Method method = sendData(player);
        if (method == null) {
            return;
        }
        CompoundTag payload = new CompoundTag();
        payload.m_128365_("locked", new ListTag());
        payload.m_128365_("required", new CompoundTag());
        try {
            method.invoke(player, CHANNEL, payload);
        } catch (Throwable t) {
            HotLog.debug("[RaceLockGUI] clear failed: " + t);
        }
    }

    private static Method sendData(ServerPlayer player) {
        if (sendData != null) {
            return sendData;
        }
        if (looked) {
            return null;
        }
        looked = true;
        try {
            for (Method method : player.getClass().getMethods()) {
                if (!"sendData".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] params = method.getParameterTypes();
                if (params[0] == String.class && params[1].isAssignableFrom(CompoundTag.class)) {
                    sendData = method;
                    break;
                }
            }
        } catch (Throwable t) {
            sendData = null;
        }
        if (sendData == null && !warned) {
            warned = true;
            HotLog.debug("[RaceLockGUI] KubeJS sendData is not on the player; periodic clear skipped");
        }
        return sendData;
    }
}
