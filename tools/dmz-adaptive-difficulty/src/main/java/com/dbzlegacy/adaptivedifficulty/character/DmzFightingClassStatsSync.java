package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ProgressionSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.server.level.ServerPlayer;

/**
 * DMZ fighting-class changes: {@link StatsData#relocateStats} resets primaries to the new
 * class template and redistributes invested points — only use when the player is not keeping
 * exact stat totals (Character Services {@code preserveBaseStats}).
 */
public final class DmzFightingClassStatsSync {
    private DmzFightingClassStatsSync() {}

    /**
     * @param preserveExactPrimaries when true, do not call {@code relocateStats} (it would wipe
     *     restored STR/VIT/etc.); only refresh class-scoped limits and push sync packets.
     */
    public static void afterFightingClassChange(
            ServerPlayer player, StatsData data, boolean preserveExactPrimaries) {
        if (player == null || data == null) {
            return;
        }
        updateTransformationLimits(player, data);
        if (!preserveExactPrimaries) {
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
        clampResourcesToDerivedMax(player, data);
        pushDmzSync(player);
    }

    private static void updateTransformationLimits(ServerPlayer player, StatsData data) {
        String race = DmzProgression.race(player);
        if (race == null || race.isBlank()) {
            return;
        }
        try {
            data.updateTransformationSkillLimits(race.trim().toLowerCase());
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] updateTransformationSkillLimits after class change: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
        }
    }

    /** Keep current energy/stamina within new class-derived caps after a class swap. */
    private static void clampResourcesToDerivedMax(ServerPlayer player, StatsData data) {
        if (data == null) {
            return;
        }
        Resources res = data.getResources();
        if (res == null) {
            return;
        }
        try {
            float maxEnergy = data.getMaxEnergy();
            float maxStamina = data.getMaxStamina();
            if (res.getCurrentEnergy() > maxEnergy) {
                res.setCurrentEnergy(maxEnergy);
            }
            if (res.getCurrentStamina() > maxStamina) {
                res.setCurrentStamina(maxStamina);
            }
            float maxHp = data.getMaxHealth();
            if (player != null && player.m_21223_() > maxHp) {
                player.m_21153_(maxHp);
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] resource clamp after class change: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
        }
    }

    private static void pushDmzSync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            NetworkHandler.sendToPlayer(new ProgressionSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
    }

    /**
     * When {@code preserveBaseStats} is false, {@link StatsData#relocateStats} applies the new
     * class template; this helper is only for explicit template-only resets.
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
