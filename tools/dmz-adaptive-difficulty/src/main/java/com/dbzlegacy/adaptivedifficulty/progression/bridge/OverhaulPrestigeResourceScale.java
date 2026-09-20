package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.level.ServerPlayer;

/**
 * When dmzrevamp Overhaul prestige {@code count} is restored after LM prestige /
 * {@code dmzstats reset}, max ki/stamina scale with overhaul multipliers but current pools
 * often stay at the post-reset 1× values until relog.
 */
public final class OverhaulPrestigeResourceScale {
    private OverhaulPrestigeResourceScale() {}

    /**
     * @param maxEnergyBefore {@link StatsData#getMaxEnergy()} immediately before {@code setCount}
     * @param maxStaminaBefore {@link StatsData#getMaxStamina()} immediately before {@code setCount}
     */
    public static void afterSetCount(
            ServerPlayer player,
            StatsData data,
            float maxEnergyBefore,
            float maxStaminaBefore
    ) {
        if (player == null || data == null) {
            return;
        }
        Resources res = data.getResources();
        if (res == null) {
            return;
        }
        boolean changed = false;

        float maxEAfter = safeMax(data.getMaxEnergy());
        float curE = res.getCurrentEnergy();
        if (maxEAfter > maxEnergyBefore + 0.5f && maxEnergyBefore > 0.5f && curE < maxEAfter - 0.5f) {
            float next = curE * (maxEAfter / maxEnergyBefore);
            next = Math.min(maxEAfter, Math.max(curE, next));
            if (next > curE + 0.01f) {
                res.setCurrentEnergy(next);
                changed = true;
            }
        }

        float maxSAfter = safeMax(data.getMaxStamina());
        float curS = res.getCurrentStamina();
        if (maxSAfter > maxStaminaBefore + 0.5f && maxStaminaBefore > 0.5f && curS < maxSAfter - 0.5f) {
            float next = curS * (maxSAfter / maxStaminaBefore);
            next = Math.min(maxSAfter, Math.max(curS, next));
            if (next > curS + 0.01f) {
                res.setCurrentStamina(next);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }
        try {
            com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil.sync(player);
        } catch (Throwable ignored) {
        }
        try {
            EnergyManaSync.clear(player.m_20148_());
            EnergyManaSync.sync(player, true);
        } catch (Throwable ignored) {
        }
        try {
            StatScreenSync.sync(player);
        } catch (Throwable ignored) {
        }
    }

    private static float safeMax(float v) {
        return Float.isFinite(v) && v > 0f ? v : 0f;
    }
}
