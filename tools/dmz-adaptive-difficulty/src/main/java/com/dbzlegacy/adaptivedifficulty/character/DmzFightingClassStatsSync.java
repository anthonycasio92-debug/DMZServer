package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.server.level.ServerPlayer;

/** Re-apply DMZ per-race {@code stats.json} class scaling/passives after fighting-class changes. */
public final class DmzFightingClassStatsSync {
    private DmzFightingClassStatsSync() {}

    public static void afterFightingClassChange(ServerPlayer player, StatsData data) {
        if (player == null || data == null) {
            return;
        }
        String race = DmzProgression.race(player);
        if (race != null && !race.isBlank()) {
            try {
                data.updateTransformationSkillLimits(race.trim().toLowerCase());
            } catch (Throwable t) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] updateTransformationSkillLimits after class change: {}",
                        AdaptiveDifficultyMod.MOD_ID,
                        t.toString());
            }
        }
        try {
            data.relocateStats(player);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] relocateStats after class change for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    player.m_6302_(),
                    t.toString());
        }
    }

    /**
     * When {@code preserveBaseStats} is false, apply this race+class {@code baseStats} block from
     * {@code config/dragonminez/races/<race>/stats.json}.
     */
    public static void applyClassBaseStats(ServerPlayer player, String raceId, String classId) {
        if (player == null || raceId == null || raceId.isBlank() || classId == null || classId.isBlank()) {
            return;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        Stats stats = data.getStats();
        if (stats == null) {
            return;
        }
        RaceStatsConfig raceStats;
        try {
            raceStats = ConfigManager.getRaceStats(raceId.trim().toLowerCase());
        } catch (Throwable t) {
            raceStats = null;
        }
        if (raceStats == null) {
            return;
        }
        RaceStatsConfig.ClassStats classStats;
        try {
            classStats = raceStats.getClassStats(classId.trim().toLowerCase());
        } catch (Throwable t) {
            classStats = null;
        }
        if (classStats == null) {
            return;
        }
        RaceStatsConfig.BaseStats base;
        try {
            base = classStats.getBaseStats();
        } catch (Throwable t) {
            base = null;
        }
        if (base == null) {
            return;
        }
        setIfPresent(stats, base.getStrength(), stats::setStrength);
        setIfPresent(stats, base.getStrikePower(), stats::setStrikePower);
        setIfPresent(stats, base.getResistance(), stats::setResistance);
        setIfPresent(stats, base.getVitality(), stats::setVitality);
        setIfPresent(stats, base.getKiPower(), stats::setKiPower);
        setIfPresent(stats, base.getEnergy(), stats::setEnergy);
    }

    private static void setIfPresent(Stats stats, Integer value, java.util.function.IntConsumer setter) {
        if (value != null && setter != null) {
            setter.accept(value);
        }
    }
}
