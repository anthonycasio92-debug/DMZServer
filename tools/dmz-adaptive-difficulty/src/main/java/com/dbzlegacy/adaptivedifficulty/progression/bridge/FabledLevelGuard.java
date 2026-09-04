package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Detects Fabled class level changes (main + prestige) and immediately re-syncs DMZ ki
 * so {@code updatePlayerStat} cannot leave the bar at 0/0 after a level up.
 */
public final class FabledLevelGuard {
    private static final String KEY_SIG = "dmz_fabled_level_sig";

    private FabledLevelGuard() {}

    public static void sync(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableEnergyManaSync) {
            return;
        }
        Object data = FabledBridge.fabledData(player);
        if (data == null) {
            return;
        }
        int sig = levelSignature(data);
        long last = ProgressionData.tempGetLong(player, KEY_SIG, Long.MIN_VALUE);
        if (last == Long.MIN_VALUE) {
            ProgressionData.tempPut(player, KEY_SIG, sig);
            return;
        }
        if (sig == (int) last) {
            return;
        }
        ProgressionData.tempPut(player, KEY_SIG, sig);
        EnergyManaSync.sync(player, true);
        FabledBridge.logSync(player, "level_change", "sig", sig, "prev", last);
    }

    static int levelSignature(Object fabledData) {
        int h = 17;
        try {
            Object main = fabledData.getClass().getMethod("getMainClass").invoke(fabledData);
            if (main != null) {
                h = 31 * h + Math.max(0, FabledBridge.invokeInt(main, "getLevel"));
            }
        } catch (Throwable ignored) {
        }
        Object prestige = PrestigeSkillSync.findPrestigeClass(fabledData);
        if (prestige != null) {
            h = 31 * h + Math.max(0, FabledBridge.invokeInt(prestige, "getLevel"));
        }
        return h;
    }
}
