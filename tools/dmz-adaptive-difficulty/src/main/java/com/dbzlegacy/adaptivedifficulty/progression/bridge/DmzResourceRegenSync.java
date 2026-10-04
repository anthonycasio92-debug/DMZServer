package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.level.ServerPlayer;

/** After DMZ regen ticks, clamp ki and stamina to the live DragonMineZ max. */
public final class DmzResourceRegenSync {
    private DmzResourceRegenSync() {}

    public static void afterEnergyRegen(ServerPlayer player, StatsData data, double amount) {
        if (player == null || data == null || amount <= 0) {
            return;
        }
        try {
            DmzResourcePoolClamp.clamp(data);
        } catch (Throwable ignored) {
        }
        try {
            OverhaulPrestigeResourceScale.pulse(player);
        } catch (Throwable ignored) {
        }
    }

    public static void afterStaminaRegen(ServerPlayer player, StatsData data, double amount) {
        if (player == null || data == null || amount <= 0) {
            return;
        }
        Resources res = data.getResources();
        if (res == null) {
            return;
        }
        try {
            DmzResourcePoolClamp.clamp(data);
        } catch (Throwable ignored) {
        }
        try {
            OverhaulPrestigeResourceScale.pulse(player);
        } catch (Throwable ignored) {
        }
    }
}
