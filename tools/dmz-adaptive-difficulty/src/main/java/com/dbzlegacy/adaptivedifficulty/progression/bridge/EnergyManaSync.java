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
        double maxEnergy;
        try {
            maxEnergy = dmz.getMaxEnergy();
        } catch (Throwable t) {
            maxEnergy = currentEnergy;
        }
        if (currentEnergy < 0) {
            currentEnergy = 0;
        }
        if (maxEnergy < 0) {
            maxEnergy = 0;
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
    }
}
