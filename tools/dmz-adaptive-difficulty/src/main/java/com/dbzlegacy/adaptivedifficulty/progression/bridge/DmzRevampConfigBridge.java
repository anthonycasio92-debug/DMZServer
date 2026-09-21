package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulCapMath;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import net.minecraftforge.fml.ModList;

/**
 * Reloads Overhaul leveling config on boot so live {@code LevelingRevamp.json} edits apply
 * without hunting down cached defaults. Also flattens native {@code initialLevelCap}
 * and {@code maxLevel} to 100k so prestige count cannot raise the cap; breakthroughs
 * raise the personal cap per-player via mixins + client KubeJS.
 */
public final class DmzRevampConfigBridge {
    private DmzRevampConfigBridge() {}

    public static void onServerStarting() {
        if (!ModList.get().isLoaded("dmzrevamp") || !DifficultyConfig.get().enablePrestigeSystem) {
            return;
        }
        try {
            Class<?> cfg = Class.forName("com.dmzrevamp.config.LevelingRevampConfig");
            cfg.getMethod("reload").invoke(null);
            try {
                Object revampCfg = cfg.getMethod("get").invoke(null);
                Object prestige = revampCfg.getClass().getField("Prestige").get(revampCfg);
                prestige.getClass().getField("maxPrestigeCount")
                        .setInt(prestige, LmOverhaulPrestigeIntegration.OVERHAUL_MAX_PRESTIGE);
            } catch (Throwable ignored) {
            }
            LmOverhaulCapMath.pinOverhaulLevelCaps();
            LmOverhaulPrestigeIntegration.logOverhaulPrestigeState();
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] dmzrevamp config reload skipped: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
        }
    }
}
