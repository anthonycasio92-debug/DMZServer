package com.dbzlegacy.adaptivedifficulty.progression.tp;

import com.dbzlegacy.adaptivedifficulty.progression.FabledSkills;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of kubejs building_tp_place.js — block place awards Building-level TP (silent).
 * Award = min(10000, floor(1 + (level-1)*1)) ≡ Building skill level when level ≥ 1.
 */
public final class BuildingTp {
    private static final String SKILL = "Building";
    private static final double VALUE_BASE = 1.0;
    private static final double VALUE_SCALE = 1.0;
    private static final int MAX_AWARD = 10_000;
    private static final long DEDUPE_MS = 50L;
    private static final String TEMP_TICK = "build_tp_tick";
    private static final String TEMP_MS = "build_tp_ms";

    private BuildingTp() {}

    public static void onBlockPlace(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!ProgressionConfig.buildingTp() || player == null) {
            return;
        }
        try {
            long tick = player.m_9236_().m_46467_();
            long lastTick = ProgressionData.tempGetLong(player, TEMP_TICK, -1L);
            if (lastTick == tick) {
                return;
            }
            long now = System.currentTimeMillis();
            long lastMs = ProgressionData.tempGetLong(player, TEMP_MS, 0L);
            if (lastMs > 0L && now - lastMs < DEDUPE_MS) {
                return;
            }

            int skillLevel = FabledSkills.skillLevel(player, SKILL);
            int amount = amountFromLevel(skillLevel);
            if (amount < 1) {
                return;
            }

            ProgressionData.tempPut(player, TEMP_TICK, tick);
            ProgressionData.tempPut(player, TEMP_MS, now);

            if (DmzRewards.awardTp(player, (float) amount, "building", false, "")) {
                SystemTelemetry.log("progression", "building_tp", player, null,
                        Map.of("tp", amount, "level", skillLevel));
            }
        } catch (Throwable ignored) {
        }
    }

    private static int amountFromLevel(int level) {
        if (level < 1) {
            return 0;
        }
        double amount = VALUE_BASE + (level - 1) * VALUE_SCALE;
        if (!Double.isFinite(amount) || amount < 1.0) {
            return 0;
        }
        return Math.min(MAX_AWARD, (int) Math.floor(amount));
    }
}
