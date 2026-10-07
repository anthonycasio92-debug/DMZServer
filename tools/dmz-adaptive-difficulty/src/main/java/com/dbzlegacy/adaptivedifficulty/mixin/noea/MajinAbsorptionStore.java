package com.dbzlegacy.adaptivedifficulty.mixin.noea;

import com.butterjaffa.noeabosses.MajinAbsorptionService;
import com.butterjaffa.noeabosses.V090Data;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Stored Majin absorption bonus on Noea's player data. */
public final class MajinAbsorptionStore {
    private MajinAbsorptionStore() {}

    /** Zero the bonus numbers and save them. The absorbed-fighter list is left in place. */
    public static void clear(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        try {
            V090Data data = MajinAbsorptionService.data(serverPlayer);
            if (data == null) {
                return;
            }
            data.absorptionMelee = 0;
            data.absorptionKi = 0;
            data.absorptionPower = 0;
            V090Data.save(serverPlayer, data);
            MajinAbsorptionService.sync(serverPlayer);
        } catch (Throwable ignored) {
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
        } catch (Throwable ignored) {
        }
    }
}
