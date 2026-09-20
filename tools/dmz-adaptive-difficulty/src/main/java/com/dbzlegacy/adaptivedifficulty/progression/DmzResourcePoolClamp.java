package com.dbzlegacy.adaptivedifficulty.progression;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keep live ki/stamina at or below the HUD cap.
 *
 * {@link com.dbzlegacy.adaptivedifficulty.mixin.StatsDataHudPoolMaxMixin} makes
 * {@link StatsData#getMaxEnergy()} / {@code getMaxStamina()} use the vanilla secondary
 * default (20) so Mohist extras cannot sit above the client bar (Ki 159/145, STM 188/101).
 *
 * <p>Never clamp current energy to {@code ≤ 1} — {@code Resources#setCurrentEnergy} zeros
 * power release at that threshold.
 */
public final class DmzResourcePoolClamp {
    private static final float EPS = 0.08f;
    /** Below this, {@code setCurrentEnergy} clears Limit Release. */
    private static final float POWER_RELEASE_FLOOR = 1.0f;

    private DmzResourcePoolClamp() {}

    /** HUD-matching ki cap (Statistics Max Ki). */
    public static float displayMaxEnergy(StatsData data) {
        return displayMax(data, true);
    }

    /** HUD-matching stamina cap (Statistics Stamina). */
    public static float displayMaxStamina(StatsData data) {
        return displayMax(data, false);
    }

    /** @return true if either pool was lowered */
    public static boolean clamp(StatsData data) {
        if (data == null) {
            return false;
        }
        Resources res = data.getResources();
        if (res == null) {
            return false;
        }
        boolean changed = false;
        try {
            float maxE = displayMaxEnergy(data);
            float curE = res.getCurrentEnergy();
            if (shouldClampCurrent(curE, maxE)) {
                res.setCurrentEnergy(maxE);
                changed = true;
            }
        } catch (Throwable ignored) {
        }
        try {
            float maxS = displayMaxStamina(data);
            float curS = res.getCurrentStamina();
            if (shouldClampCurrent(curS, maxS)) {
                res.setCurrentStamina(maxS);
                changed = true;
            }
        } catch (Throwable ignored) {
        }
        return changed;
    }

    public static void clampAndSync(ServerPlayer player, StatsData data) {
        if (player == null || data == null) {
            return;
        }
        if (clamp(data)) {
            syncToClient(player);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
    }

    /**
     * {@link com.dbzlegacy.adaptivedifficulty.mixin.StatsDataHudPoolMaxMixin} already makes
     * {@code getMax*} match the HUD (vanilla secondary default). Do not subtract extras again.
     */
    public static float toHudMax(float live, StatsData data, boolean energy) {
        return live;
    }

    /** True when current should be pulled down — never through the power-release-zero floor. */
    public static boolean shouldClampCurrent(float current, float hudMax) {
        if (!Float.isFinite(current) || !Float.isFinite(hudMax)) {
            return false;
        }
        if (hudMax <= POWER_RELEASE_FLOOR && current > POWER_RELEASE_FLOOR) {
            return false;
        }
        return hudMax > 0f && current > hudMax + EPS;
    }

    private static float displayMax(StatsData data, boolean energy) {
        if (data == null) {
            return 0f;
        }
        float live = energy ? data.getMaxEnergy() : data.getMaxStamina();
        return Float.isFinite(live) && live > 0f ? live : 0f;
    }
}
