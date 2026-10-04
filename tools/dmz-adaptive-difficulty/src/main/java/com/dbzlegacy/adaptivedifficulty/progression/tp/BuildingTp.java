package com.dbzlegacy.adaptivedifficulty.progression.tp;

import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block place awards a flat 1 TP (the old Building level-1 amount), silently.
 */
public final class BuildingTp {
    private static final int AWARD = 1;
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

            ProgressionData.tempPut(player, TEMP_TICK, tick);
            ProgressionData.tempPut(player, TEMP_MS, now);

            if (DmzRewards.awardTp(player, AWARD, "building", false, "")) {
                SystemTelemetry.log("progression", "building_tp", player, null,
                        Map.of("tp", AWARD));
            }
        } catch (Throwable ignored) {
        }
    }

}
