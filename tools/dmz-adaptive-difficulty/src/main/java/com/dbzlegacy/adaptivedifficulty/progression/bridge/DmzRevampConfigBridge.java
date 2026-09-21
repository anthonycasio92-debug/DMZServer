package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulCapMath;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import net.minecraftforge.fml.ModList;

/**
 * Reloads Overhaul leveling config on boot so live {@code LevelingRevamp.json} edits apply
 * without hunting down cached defaults. Pins {@code initialLevelCap=100k} /
 * {@code maxLevel=150k} so native Statistics can express breakthroughs; mixins +
 * client KubeJS still replace the prestige ladder with 100k+10k×BT.
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
