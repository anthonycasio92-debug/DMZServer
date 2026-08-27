package com.dbzlegacy.adaptivedifficulty.progression.tp;

import com.dbzlegacy.adaptivedifficulty.progression.FabledSkills;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Port of Farming TP Skill.js — mature crop harvest awards Farming×10 TP.
 */
public final class FarmingTp {
    private static final String SKILL = "Farming";
    private static final int TP_PER_LEVEL = 10;
    private static final Set<String> CROP_IDS = Set.of(
            "minecraft:wheat",
            "minecraft:carrots",
            "minecraft:potatoes",
            "minecraft:beetroots",
            "minecraft:nether_wart",
            "minecraft:cocoa",
            "minecraft:sweet_berry_bush"
    );

    private FarmingTp() {}

    public static void onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!ProgressionConfig.farmingTp() || player == null || state == null || pos == null) {
            return;
        }
        try {
            String id = blockId(state);
            if (!isCrop(id)) {
                return;
            }
            if (!isMature(id, state)) {
                return;
            }
            long now = System.currentTimeMillis();
            String posKey = "farm_last_" + pos.m_123341_() + "_" + pos.m_123342_() + "_" + pos.m_123343_();
            long last = ProgressionData.tempGetLong(player, posKey, 0L);
            if (last > 0L && now - last < 500L) {
                return;
            }
            ProgressionData.tempPut(player, posKey, now);

            int skillLevel = FabledSkills.skillLevel(player, SKILL);
            if (skillLevel <= 0) {
                return;
            }
            float tp = skillLevel * TP_PER_LEVEL;
            if (DmzRewards.awardTp(player, tp, "farming", false, "§a[Farming] ")) {
                SystemTelemetry.log("progression", "farming_tp", player, null,
                        Map.of("tp", (int) tp, "level", skillLevel, "block", id));
            }
        } catch (Throwable ignored) {
        }
    }

    private static String blockId(BlockState state) {
        try {
            var key = ForgeRegistries.BLOCKS.getKey(state.m_60734_());
            return key == null ? "" : key.toString().toLowerCase(Locale.ROOT);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static boolean isCrop(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        if (CROP_IDS.contains(id)) {
            return true;
        }
        return id.startsWith("pamhc2crops:") || id.startsWith("pamhc2trees:");
    }

    private static boolean isMature(String id, BlockState state) {
        int age = readAge(state);
        if (age < 0) {
            // CropBlock max-age fallback
            Block block = state.m_60734_();
            if (block instanceof CropBlock crop) {
                return crop.m_52307_(state); // isMaxAge
            }
            return false;
        }
        if (id.contains("beetroots") || id.contains("nether_wart")) {
            return age >= 3;
        }
        if (id.contains("cocoa")) {
            return age >= 2;
        }
        if (id.contains("sweet_berry")) {
            return age >= 3;
        }
        return age >= 7;
    }

    private static int readAge(BlockState state) {
        try {
            if (state.m_61138_(BlockStateProperties.f_61413_)) { // AGE_7
                return state.m_61143_(BlockStateProperties.f_61413_);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (state.m_61138_(BlockStateProperties.f_61414_)) { // AGE_3
                return state.m_61143_(BlockStateProperties.f_61414_);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (state.m_61138_(BlockStateProperties.f_61415_)) { // AGE_2
                return state.m_61143_(BlockStateProperties.f_61415_);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (state.m_61138_(NetherWartBlock.f_54967_)) {
                return state.m_61143_(NetherWartBlock.f_54967_);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (state.m_61138_(SweetBerryBushBlock.f_57244_)) {
                return state.m_61143_(SweetBerryBushBlock.f_57244_);
            }
        } catch (Throwable ignored) {
        }
        for (var prop : state.m_61147_()) {
            if ("age".equals(prop.m_61708_()) && prop instanceof IntegerProperty ip) {
                return state.m_61143_(ip);
            }
        }
        return -1;
    }
}
