package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.OverhaulPrestigeResourceScale;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Use dmzrevamp Overhaul prestige (Statistics UI, scaling, saga rebirth) while Legacy Mechanics
 * overrides only {@link com.dmzrevamp.revamp.prestige.PrestigeSystem#levelCap} and stat totals.
 */
public final class LmOverhaulPrestigeIntegration {
    private static volatile Boolean overhaulPrestigeEnabled;

    private LmOverhaulPrestigeIntegration() {}

    public static boolean integrationActive() {
        DifficultyConfig cfg = DifficultyConfig.get();
        return cfg != null
                && cfg.enablePrestigeSystem
                && cfg.enableOverhaulPrestigeIntegration
                && overhaulPrestigeEnabled();
    }

    /**
     * Overhaul prestige is on only when {@code LevelingRevamp.json} has both
     * {@code levelsAndAttributes.enabled} and {@code Prestige.enabled} (and no {@code dmzprestige} mod).
     */
    public static boolean overhaulPrestigeEnabled() {
        if (!ModList.get().isLoaded("dmzrevamp")) {
            return false;
        }
        Boolean cached = overhaulPrestigeEnabled;
        if (cached != null) {
            return cached;
        }
        boolean ok = false;
        try {
            Class<?> cfg = Class.forName("com.dmzrevamp.config.LevelingRevampConfig");
            Object v = cfg.getMethod("prestigeEnabled").invoke(null);
            ok = v instanceof Boolean b && b;
        } catch (Throwable ignored) {
        }
        overhaulPrestigeEnabled = ok;
        return ok;
    }

    public static void clearConfigCache() {
        overhaulPrestigeEnabled = null;
    }

    /** Log both Overhaul JSON toggles + whether native prestige is active (after {@code reload}). */
    public static void logOverhaulPrestigeState() {
        if (!ModList.get().isLoaded("dmzrevamp")) {
            return;
        }
        clearConfigCache();
        boolean levels = false;
        boolean prestigeFlag = false;
        try {
            Class<?> cfgCls = Class.forName("com.dmzrevamp.config.LevelingRevampConfig");
            Object revampCfg = cfgCls.getMethod("get").invoke(null);
            Object levelsObj = revampCfg.getClass().getField("levelsAndAttributes").get(revampCfg);
            levels = levelsObj.getClass().getField("enabled").getBoolean(levelsObj);
            Object prestigeObj = revampCfg.getClass().getField("Prestige").get(revampCfg);
            prestigeFlag = prestigeObj.getClass().getField("enabled").getBoolean(prestigeObj);
        } catch (Throwable ignored) {
        }
        boolean enabled = overhaulPrestigeEnabled();
        DifficultyConfig lmCfg = DifficultyConfig.get();
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] Overhaul prestige: levelsAndAttributes.enabled={} Prestige.enabled={} "
                        + "prestigeEnabled()={} LM integration={} LM resourceScale={}",
                AdaptiveDifficultyMod.MOD_ID,
                levels,
                prestigeFlag,
                enabled,
                lmCfg != null && lmCfg.enableOverhaulPrestigeIntegration,
                lmCfg != null && lmCfg.enablePrestigeResourceScaling);
    }

    /** After native {@link com.dmzrevamp.revamp.prestige.PrestigeService#tryPrestige}. */
    public static void syncLmWalletFromOverhaulCount(ServerPlayer player) {
        if (player == null || !integrationActive()) {
            return;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        int overhaul = Math.max(0, DmzRevampPrestigeBridge.overhaulCount(data));
        int lmCompleted = PrestigeSystem.getCompleted(player);
        if (overhaul <= lmCompleted) {
            return;
        }
        int delta = overhaul - lmCompleted;
        PrestigeSystem.setCompletedPublic(player, overhaul);
        int held = PrestigeSystem.getHeld(player);
        int maxHeld = PrestigeSystem.maxHeld();
        if (held < maxHeld) {
            PrestigeSystem.setHeldPublic(player, Math.min(maxHeld, held + delta));
        }
        try {
            com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync.sync(player);
        } catch (Throwable ignored) {
        }
        try {
            PrestigePointsSystem.scheduleReapplyAfterPrestige(player);
        } catch (Throwable ignored) {
        }
        try {
            OverhaulPrestigeResourceScale.pulse(player);
        } catch (Throwable ignored) {
        }
    }
}
