package com.dbzlegacy.adaptivedifficulty.noea;

import com.butterjaffa.noeabosses.MajinAbsorptionService;
import com.butterjaffa.noeabosses.V090Data;
import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Stored Majin absorption bonus on Noea's player data.
 * Lives outside the mixin package so the server classloader can find it.
 */
public final class MajinAbsorptionStore {
    private MajinAbsorptionStore() {}

    /**
     * Zero the bonus numbers and save them. The absorbed-fighter list is left in place.
     * {@code data()} is a copy read from player NBT, so the readback is a second copy.
     */
    public static void clear(Player player) {
        String name = "null";
        if (player != null) {
            try {
                name = player.m_7755_().getString();
            } catch (Throwable ignored) {
                name = "?";
            }
        }
        try {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] MajinAbsorptionStore.clear() for {}", AdaptiveDifficultyMod.MOD_ID, name);
            if (player == null) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] MajinAbsorptionStore.clear aborted: player is null",
                        AdaptiveDifficultyMod.MOD_ID);
                return;
            }
            V090Data data = MajinAbsorptionService.data(player);
            if (data == null) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] MajinAbsorptionStore.clear aborted: data is null for {}",
                        AdaptiveDifficultyMod.MOD_ID,
                        name);
                return;
            }
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] MajinAbsorptionStore data melee={} ki={} power={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    data.absorptionMelee,
                    data.absorptionKi,
                    data.absorptionPower);
            data.absorptionMelee = 0;
            data.absorptionKi = 0;
            data.absorptionPower = 0;
            V090Data.save(player, data);
            if (player instanceof ServerPlayer serverPlayer) {
                MajinAbsorptionService.sync(serverPlayer);
            }
            V090Data again = MajinAbsorptionService.data(player);
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] MajinAbsorptionStore wipe complete readback melee={} ki={} power={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    again == null ? "NULL" : again.absorptionMelee,
                    again == null ? "NULL" : again.absorptionKi,
                    again == null ? "NULL" : again.absorptionPower);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] MajinAbsorptionStore.clear failed for {}", AdaptiveDifficultyMod.MOD_ID, name, t);
        }
    }

    /** Wipe only when character creation is about to replace the current race. */
    public static void clearIfRaceChanges(StatsData stats, String nextRace) {
        if (stats == null || nextRace == null || nextRace.isBlank()) {
            return;
        }
        String current = "";
        try {
            Character character = stats.getCharacter();
            if (character != null && character.getRace() != null) {
                current = character.getRace();
            }
        } catch (Throwable ignored) {
            return;
        }
        if (current.isBlank() || current.equalsIgnoreCase(nextRace.trim())) {
            return;
        }
        try {
            clear(stats.getPlayer());
        } catch (Throwable t) {
            try {
                AbsorptionClearLog.failure(t);
            } catch (Throwable ignored) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] MajinAbsorptionStore.clearIfRaceChanges failed",
                        AdaptiveDifficultyMod.MOD_ID,
                        t);
            }
        }
    }
}
