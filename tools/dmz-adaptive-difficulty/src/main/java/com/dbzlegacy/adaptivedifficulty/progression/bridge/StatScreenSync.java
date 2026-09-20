package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code DMZ Stat Screen.js} — DMZ combat stats → Fabled persistent values.
 */
public final class StatScreenSync {
    private StatScreenSync() {}

    public static void sync(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableStatScreenSync) {
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
        try {
            put(data, "Damage", dmz.getMeleeDamage());
            put(data, "StrikeDamage", dmz.getStrikeDamage());
            put(data, "Stamina", DmzResourcePoolClamp.displayMaxStamina(dmz));
            put(data, "Defense", dmz.getDefense());
            put(data, "Health", dmz.getMaxHealth());
            put(data, "KiDamage", dmz.getKiDamage());
            put(data, "MaxKi", DmzResourcePoolClamp.displayMaxEnergy(dmz));
        } catch (Throwable ignored) {
        }
    }

    private static void put(Object fabledData, String key, double value) {
        try {
            fabledData.getClass()
                    .getMethod("setPersistentData", String.class, Object.class)
                    .invoke(fabledData, key, Double.valueOf(value));
        } catch (Throwable ignored) {
        }
    }
}
