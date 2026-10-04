package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dragonminez.common.stats.StatsData;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ki and stamina maxima come from DragonMineZ and the other mods.
 * This class only pulls a current pool down when it sits above that live max.
 * It does not multiply current stamina or ki when the max changes.
 */
public final class OverhaulPrestigeResourceScale {
    private OverhaulPrestigeResourceScale() {}

    /** Pull current ki/stamina down if they sit above the live DragonMineZ max. */
    public static void pulse(ServerPlayer player) {
        if (player == null) {
            return;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        if (DmzResourcePoolClamp.clamp(data)) {
            afterPoolsChanged(player, data);
        }
    }

    public static void clear(UUID id) {
    }

    /**
     * @param maxEnergyBefore unused; kept so callers do not need a signature change
     * @param maxStaminaBefore unused; kept so callers do not need a signature change
     */
    public static void afterSetCount(
            ServerPlayer player,
            StatsData data,
            float maxEnergyBefore,
            float maxStaminaBefore
    ) {
        if (player == null || data == null) {
            return;
        }
        if (DmzResourcePoolClamp.clamp(data)) {
            afterPoolsChanged(player, data);
        }
        pulse(player);
    }

    private static void afterPoolsChanged(ServerPlayer player, StatsData data) {
        try {
            DmzSkillUtil.sync(player);
        } catch (Throwable ignored) {
        }
        try {
            DmzResourcePoolClamp.syncToClient(player);
        } catch (Throwable ignored) {
        }
    }
}
