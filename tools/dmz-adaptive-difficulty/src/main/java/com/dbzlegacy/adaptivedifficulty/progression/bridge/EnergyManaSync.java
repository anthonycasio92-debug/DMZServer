package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
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
        }
        if (currentEnergy > maxEnergy) {
            currentEnergy = maxEnergy;
        }

        double fabledMana = FabledBridge.invokeDouble(data, "getMana");
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

        if (last != null && fabledMana < last) {
            double spent = last - fabledMana;
            if (spent > 0) {
                try {
                    resources.removeEnergy((float) spent);
                } catch (Throwable t) {
                    resources.setCurrentEnergy((float) Math.max(0, currentEnergy - spent));
                }
                currentEnergy = resources.getCurrentEnergy();
                if (currentEnergy < 0) {
                    currentEnergy = 0;
                    resources.setCurrentEnergy(0);
                }
                FabledBridge.logSync(player, "energy_spend", "spent", spent, "energy", currentEnergy);
            }
        }

        if (currentEnergy > maxEnergy) {
            currentEnergy = maxEnergy;
        }

        FabledBridge.setManaAndMax(data, currentEnergy, maxEnergy);
        LAST_MANA.put(player.m_20148_(), currentEnergy);
        ProgressionData.tempPut(player, LAST_MANA_KEY, currentEnergy);

        if (scheduleBukkitFollowup) {
            final double cur = currentEnergy;
            final double max = maxEnergy;
            FabledBridge.runOnBukkit(player, () -> {
                Object again = FabledBridge.fabledData(player);
                if (again == null) {
                    return;
                }
                FabledBridge.setManaAndMax(again, cur, max);
                LAST_MANA.put(player.m_20148_(), cur);
            });
        }
    }

    private static double readMaxEnergy(StatsData dmz, Resources resources, double currentEnergy) {
        // Live script tried resources.getMaxEnergy() first (not on current DMZ API), then StatsData.
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
        try {
            float max = dmz.getMaxEnergy();
            if (Float.isFinite(max) && max > 0) {
                return max;
            }
            return max;
        } catch (Throwable t) {
            return currentEnergy;
        }
    }
}
