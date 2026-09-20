package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * When dmzrevamp Overhaul prestige {@code count} is restored after LM prestige /
 * {@code dmzstats reset}, max ki/stamina scale with overhaul multipliers but current pools
 * often stay at the post-reset 1× values until relog. The same happens when prestige-shop
 * skill floors / multipliers reapply and raise {@link StatsData#getMaxEnergy()} later.
 */
public final class OverhaulPrestigeResourceScale {
    private static final Map<UUID, Float> LAST_MAX_ENERGY = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> LAST_MAX_STAMINA = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_RESTORE_MS = new ConcurrentHashMap<>();

    private OverhaulPrestigeResourceScale() {}

    /** Track max jumps from any source (shop reapply, login, delayed overhaul sync). */
    public static void pulse(ServerPlayer player) {
        if (player == null) {
            return;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        UUID id = player.m_20148_();
        Resources res = data.getResources();
        boolean changed = false;

        Float prevE = LAST_MAX_ENERGY.get(id);
        Float prevS = LAST_MAX_STAMINA.get(id);
        if (prevE != null) {
            changed |= scaleOnMaxIncrease(data, prevE, true);
        }
        if (prevS != null) {
            changed |= scaleOnMaxIncrease(data, prevS, false);
        }
        if (res != null) {
            changed |= restoreIfStuckAtOldCap(player, data, res);
        }
        if (changed) {
            afterPoolsChanged(player, data);
        }
        LAST_MAX_ENERGY.put(id, safeMax(data.getMaxEnergy()));
        LAST_MAX_STAMINA.put(id, safeMax(data.getMaxStamina()));
    }

    public static void clear(UUID id) {
        if (id == null) {
            return;
        }
        LAST_MAX_ENERGY.remove(id);
        LAST_MAX_STAMINA.remove(id);
        LAST_RESTORE_MS.remove(id);
    }

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
        boolean changed = scaleOnMaxIncrease(data, maxEnergyBefore, true);
        changed |= scaleOnMaxIncrease(data, maxStaminaBefore, false);
        if (changed) {
            afterPoolsChanged(player, data);
        }
        pulse(player);
    }

    private static boolean scaleOnMaxIncrease(StatsData data, float maxBefore, boolean energy) {
        if (data == null || maxBefore <= 0.5f) {
            return false;
        }
        Resources res = data.getResources();
        if (res == null) {
            return false;
        }
        float maxAfter = energy ? safeMax(data.getMaxEnergy()) : safeMax(data.getMaxStamina());
        if (maxAfter <= maxBefore + 0.5f) {
            return false;
        }
        float cur = energy ? res.getCurrentEnergy() : res.getCurrentStamina();
        if (cur >= maxAfter - 0.5f) {
            return false;
        }
        float ratio = maxAfter / maxBefore;
        float next = cur * ratio;
        // If the player was effectively full at the old cap, refill to the new cap.
        if (cur >= maxBefore * 0.98f) {
            next = maxAfter;
        } else {
            next = Math.min(maxAfter, Math.max(cur, next));
        }
        if (next <= cur + 0.01f) {
            return false;
        }
        if (energy) {
            res.setCurrentEnergy(next);
        } else {
            res.setCurrentStamina(next);
        }
        return true;
    }

    /**
     * HUD max already includes Overhaul/form (e.g. 30912) while current is still the old full
     * bar (15465). {@link StatsData#restoreMultiplierGains} adds (newMax - oldMax).
     */
    private static boolean restoreIfStuckAtOldCap(ServerPlayer player, StatsData data, Resources res) {
        float maxE = safeMax(data.getMaxEnergy());
        float maxS = safeMax(data.getMaxStamina());
        float curE = res.getCurrentEnergy();
        float curS = res.getCurrentStamina();
        boolean energyStuck = looksLikeOldFullCap(curE, maxE);
        boolean staminaStuck = looksLikeOldFullCap(curS, maxS);
        if (!energyStuck && !staminaStuck) {
            return false;
        }
        UUID id = player.m_20148_();
        long now = System.currentTimeMillis();
        Long last = LAST_RESTORE_MS.get(id);
        if (last != null && now - last < 8_000L) {
            return false;
        }
        LAST_RESTORE_MS.put(id, now);
        try {
            boolean changedDirect = false;
            float[] snap = data.snapshotMultiplierResources();
            if (snap == null || snap.length < 3) {
                snap = new float[] {data.getMaxHealth(), maxE, maxS};
            }
            if (energyStuck) {
                res.setCurrentEnergy(maxE);
                changedDirect = true;
            }
            if (staminaStuck) {
                res.setCurrentStamina(maxS);
                changedDirect = true;
            }
            if (changedDirect) {
                return true;
            }
            if (energyStuck) {
                snap[1] = curE;
            }
            if (staminaStuck) {
                snap[2] = curS;
            }
            data.restoreMultiplierGains(player, snap);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean looksLikeOldFullCap(float current, float max) {
        if (current < 8f || max <= current + 8f) {
            return false;
        }
        double ratio = max / current;
        long nearest = Math.round(ratio);
        return nearest >= 2L && Math.abs(ratio - nearest) < 0.02;
    }

    private static void afterPoolsChanged(ServerPlayer player, StatsData data) {
        try {
            DmzSkillUtil.sync(player);
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
