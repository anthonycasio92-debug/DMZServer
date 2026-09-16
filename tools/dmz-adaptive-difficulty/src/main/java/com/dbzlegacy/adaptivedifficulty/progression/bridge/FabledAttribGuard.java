package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;

/**
 * Prevents negative Fabled attribute points (AP) and DMZ pending attribute points.
 * Overspend in the Fabled attributes GUI can drive AP below zero without a hard floor.
 */
public final class FabledAttribGuard {
    private FabledAttribGuard() {}

    public static void sync(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableAttrMultiBonus) {
            return;
        }
        Object data = FabledBridge.fabledData(player);
        if (data != null) {
            clampFabledAttribPoints(player, data);
        }
        clampDmzPendingAp(player);
    }

    private static void clampFabledAttribPoints(ServerPlayer player, Object fabledData) {
        for (String getter : new String[] {"getAttribPoints", "getAttributePoints"}) {
            int ap = invokeInt(fabledData, getter);
            if (ap == Integer.MIN_VALUE) {
                continue;
            }
            if (ap < 0) {
                setAttribPoints(fabledData, 0);
                FabledBridge.logSync(player, "ap_clamp", "was", ap);
                try {
                    fabledData.getClass().getMethod("updateScoreboard").invoke(fabledData);
                } catch (Throwable ignored) {
                }
            }
            return;
        }
    }

    private static void setAttribPoints(Object fabledData, int value) {
        for (String setter : new String[] {"setAttribPoints", "setAttributePoints"}) {
            if (invokeVoid(fabledData, setter, value)) {
                return;
            }
        }
        // Fallback: givePoints only adjusts skill points — try invest rollback via giveAttribPoints.
        for (String give : new String[] {"giveAttribPoints", "giveAttributePoints"}) {
            invokeVoid(fabledData, give, Math.max(0, value));
        }
    }

    private static void clampDmzPendingAp(ServerPlayer player) {
        StatsData dmz = DmzProgression.stats(player);
        if (dmz == null) {
            return;
        }
        Resources resources = dmz.getResources();
        if (resources == null) {
            return;
        }
        boolean changed = false;
        for (String getter : new String[] {
                "getPendingAttributePoints",
                "getAttributePoints",
                "getPendingAp"
        }) {
            float pending = invokeFloat(resources, getter);
            if (Float.isNaN(pending)) {
                continue;
            }
            if (pending < 0) {
                for (String setter : new String[] {
                        "setPendingAttributePoints",
                        "setAttributePoints",
                        "setPendingAp"
                }) {
                    if (invokeVoid(resources, setter, 0f)) {
                        changed = true;
                        FabledBridge.logSync(player, "dmz_ap_clamp", "was", pending);
                        break;
                    }
                }
            }
            break;
        }
        if (changed) {
            try {
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            } catch (Throwable ignored) {
            }
        }
    }

    private static int invokeInt(Object target, String method) {
        try {
            Object v = target.getClass().getMethod(method).invoke(target);
            if (v instanceof Number n) {
                return n.intValue();
            }
        } catch (Throwable ignored) {
        }
        return Integer.MIN_VALUE;
    }

    private static float invokeFloat(Object target, String method) {
        try {
            Object v = target.getClass().getMethod(method).invoke(target);
            if (v instanceof Number n) {
                return n.floatValue();
            }
        } catch (Throwable ignored) {
        }
        return Float.NaN;
    }

    private static boolean invokeVoid(Object target, String method, float value) {
        try {
            Method m = target.getClass().getMethod(method, float.class);
            m.invoke(target, value);
            return true;
        } catch (Throwable ignored) {
        }
        try {
            Method m = target.getClass().getMethod(method, int.class);
            m.invoke(target, (int) value);
            return true;
        } catch (Throwable ignored) {
        }
        try {
            Method m = target.getClass().getMethod(method, double.class);
            m.invoke(target, (double) value);
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean invokeVoid(Object target, String method, int value) {
        try {
            Method m = target.getClass().getMethod(method, int.class);
            m.invoke(target, value);
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }
}
