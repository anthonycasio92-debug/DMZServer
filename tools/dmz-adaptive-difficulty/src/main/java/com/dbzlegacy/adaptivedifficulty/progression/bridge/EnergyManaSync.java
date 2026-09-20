package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code DMZ Energy.js} — DMZ energy ↔ Fabled mana.
 * <p>
 * Fabled {@code updatePlayerStat} recalculates {@code maxMana} from class mana (often 0 on this
 * pack) and overwrites our field writes. Re-apply immediately and again on the next Bukkit tick
 * so the side menu stays in sync with DMZ ki.
 */
public final class EnergyManaSync {
    private static final String LAST_MANA_KEY = "dmz_fabled_last_mana";
    private static final double ENERGY_EPS = 0.5;
    /** CNPC script mirror drains above this are treated as real Fabled skill costs. */
    private static final float MAX_MIRROR_DRAIN = 48f;
    private static final Map<UUID, Double> LAST_MANA = new ConcurrentHashMap<>();

    private EnergyManaSync() {}

    public static void clear(UUID id) {
        if (id != null) {
            LAST_MANA.remove(id);
        }
    }

    public static void sync(ServerPlayer player) {
        sync(player, true);
    }

    /**
     * CNPC {@code DMZ Fabled Bridge.js} drains DMZ when Fabled mana drops below temp last-mana.
     * Cancel those mirror drains while DMZ regen / scaling is raising the real pool.
     */
    public static boolean shouldBlockFabledMirrorDrain(
            ServerPlayer player, Resources resources, float amount
    ) {
        if (player == null || resources == null || !DifficultyConfig.get().enableEnergyManaSync) {
            return false;
        }
        if (amount <= 0f || amount > MAX_MIRROR_DRAIN) {
            return false;
        }
        Double last = LAST_MANA.get(player.m_20148_());
        if (last == null) {
            return false;
        }
        Object fabled = FabledBridge.fabledData(player);
        if (fabled == null) {
            return false;
        }
        double fMana = FabledBridge.invokeDouble(fabled, "getMana");
        if (!Double.isFinite(fMana)) {
            return false;
        }
        float cur = resources.getCurrentEnergy();
        StatsData dmz = resources.getStatsData();
        float max = dmz != null ? DmzResourcePoolClamp.displayMaxEnergy(dmz) : cur;
        // Fabled lag behind DMZ / last sync — do not pull DMZ ki back down.
        if (fMana + ENERGY_EPS < last && amount <= (float) (last - fMana) + ENERGY_EPS + 2f) {
            return true;
        }
        // Regen climbing above stale Fabled bar while still below scaled max.
        if (cur + ENERGY_EPS < max && fMana + ENERGY_EPS < cur && amount <= cur - fMana + ENERGY_EPS) {
            return true;
        }
        return false;
    }

    /** @param scheduleBukkitFollowup when true, re-apply after Fabled's Bukkit tick overwrites mana */
    public static void sync(ServerPlayer player, boolean scheduleBukkitFollowup) {
        if (player == null || !DifficultyConfig.get().enableEnergyManaSync) {
            return;
        }
        Object data = FabledBridge.fabledData(player);
        if (data == null) {
            return;
        }
        StatsData dmz = DmzProgression.stats(player);
        if (dmz == null) {
            return;
        }
        Resources resources = dmz.getResources();
        if (resources == null) {
            return;
        }

        double currentEnergy = resources.getCurrentEnergy();
        double maxEnergy = readMaxEnergy(dmz, resources, currentEnergy);
        if (currentEnergy < 0) {
            currentEnergy = 0;
        }
        if (maxEnergy < 0) {
            maxEnergy = 0;
        }
        // Avoid clobbering a healthy Fabled bar with 0/0 before DMZ stats are ready.
        if (maxEnergy <= 0) {
            try {
                var status = dmz.getStatus();
                if (status == null || !status.isHasCreatedCharacter()) {
                    return;
                }
            } catch (Throwable ignored) {
            }
            // Character exists but caps not ready — never clamp live ki to 0 (blocks regen).
            return;
        }
        if (currentEnergy > maxEnergy) {
            currentEnergy = maxEnergy;
        }

        Double last = LAST_MANA.get(player.m_20148_());
        if (last == null) {
            String stored = ProgressionData.tempGet(player, LAST_MANA_KEY, null);
            if (stored != null) {
                try {
                    last = Double.parseDouble(stored);
                } catch (Throwable ignored) {
                    last = null;
                }
            }
        }

        // DMZ owns ki pool + regen on this pack — mirror to Fabled for the side menu only.
        if (last != null && last > currentEnergy + ENERGY_EPS) {
            last = currentEnergy;
        }

        if (currentEnergy > maxEnergy) {
            currentEnergy = maxEnergy;
        }

        FabledBridge.setManaAndMax(data, currentEnergy, maxEnergy);
        publishLastMana(player, currentEnergy);

        boolean gap = currentEnergy + ENERGY_EPS < maxEnergy;
        if (scheduleBukkitFollowup || gap) {
            scheduleFollowup(player, currentEnergy, maxEnergy);
        }
    }

    private static void publishLastMana(ServerPlayer player, double currentEnergy) {
        LAST_MANA.put(player.m_20148_(), currentEnergy);
        ProgressionData.tempPut(player, LAST_MANA_KEY, currentEnergy);
        CnpcBridge.putTempString(player, LAST_MANA_KEY, String.valueOf(currentEnergy));
    }

    private static void scheduleFollowup(ServerPlayer player, double currentEnergy, double maxEnergy) {
        final double cur = currentEnergy;
        final double max = maxEnergy;
        FabledBridge.runOnBukkit(player, () -> {
            Object again = FabledBridge.fabledData(player);
            if (again == null) {
                return;
            }
            FabledBridge.setManaAndMax(again, cur, max);
            publishLastMana(player, cur);
        });
        FabledBridge.runOnBukkit(player, () -> FabledBridge.runOnBukkit(player, () -> {
            Object again = FabledBridge.fabledData(player);
            if (again == null) {
                return;
            }
            FabledBridge.setManaAndMax(again, cur, max);
            publishLastMana(player, cur);
        }));
    }

    private static double readMaxEnergy(StatsData dmz, Resources resources, double currentEnergy) {
        // HUD-formula cap — not StatsData#getMaxEnergy, which includes server-only extras.
        try {
            float max = DmzResourcePoolClamp.displayMaxEnergy(dmz);
            if (Float.isFinite(max) && max > 0) {
                return max;
            }
        } catch (Throwable ignored) {
        }
        try {
            Object v = resources.getClass().getMethod("getMaxEnergy").invoke(resources);
            if (v instanceof Number n) {
                double d = n.doubleValue();
                if (Double.isFinite(d) && d > 0) {
                    return d;
                }
            }
        } catch (Throwable ignored) {
        }
        return currentEnergy;
    }
}
