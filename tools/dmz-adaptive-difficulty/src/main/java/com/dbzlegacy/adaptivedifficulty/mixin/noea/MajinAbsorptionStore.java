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
        System.out.println("[LM] clear() called for " + name);
        if (player == null) {
            System.out.println("[LM] ABORT: player is null, cannot wipe");
            return;
        }
        try {
            V090Data data = MajinAbsorptionService.data(player);
            System.out.println("[LM] data object: " + (data == null
                    ? "NULL"
                    : "present, melee=" + data.absorptionMelee
                            + " ki=" + data.absorptionKi
                            + " power=" + data.absorptionPower
                            + " id=" + System.identityHashCode(data)));
            if (data == null) {
                System.out.println("[LM] ABORT: data is null, cannot wipe");
                return;
            }
            data.absorptionMelee = 0;
            data.absorptionKi = 0;
            data.absorptionPower = 0;
            System.out.println("[LM] fields zeroed, calling save...");
            V090Data.save(player, data);
            System.out.println("[LM] save complete, calling sync...");
            if (player instanceof ServerPlayer serverPlayer) {
                MajinAbsorptionService.sync(serverPlayer);
            }
            V090Data again = MajinAbsorptionService.data(player);
            System.out.println("[LM] readback melee=" + (again == null ? "NULL" : again.absorptionMelee)
                    + " ki=" + (again == null ? "NULL" : again.absorptionKi)
                    + " power=" + (again == null ? "NULL" : again.absorptionPower)
                    + " id=" + (again == null ? "NULL" : System.identityHashCode(again)));
            System.out.println("[LM] wipe complete");
        } catch (Throwable t) {
            System.out.println("[LM] wipe FAILED: " + t);
            t.printStackTrace();
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
            com.dbzlegacy.adaptivedifficulty.mixin.AbsorptionClearLog.failure(t);
        }
    }
}
