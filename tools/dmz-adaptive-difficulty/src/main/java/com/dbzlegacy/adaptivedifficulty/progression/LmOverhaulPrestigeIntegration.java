package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.OverhaulPrestigeResourceScale;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem;
import com.dragonminez.common.stats.StatsData;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Use dmzrevamp Overhaul prestige (Statistics UI, scaling, saga rebirth) while Legacy Mechanics
 * owns playable stat totals via mixins. Overhaul keeps native {@code levelCap} for hex/scale math.
 */
public final class LmOverhaulPrestigeIntegration {
    /** Overhaul Statistics prestige count hard cap. */
    public static final int OVERHAUL_MAX_PRESTIGE = 10;

    private static volatile Boolean overhaulPrestigeEnabled;
    private static volatile Method overhaulScaleMultiplier;

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
        overhaulScaleMultiplier = null;
    }

    /**
     * Overhaul {@code PrestigeSystem.scaleMultiplier} = {@code 1 + count × scaleBonusPerPrestige}.
     * Live {@code scaleBonusPerPrestige} is 1.0, so prestige 10 is 11×. Used for combat
     * total-multipliers (not ki/stamina pools).
     */
    public static double combatScaleMultiplier(StatsData data) {
        if (data == null || !overhaulPrestigeEnabled()) {
            return 1.0d;
        }
        try {
            Method m = overhaulScaleMultiplier;
            if (m == null) {
                Class<?> cls = Class.forName("com.dmzrevamp.revamp.prestige.PrestigeSystem");
                m = cls.getMethod("scaleMultiplier", StatsData.class);
                overhaulScaleMultiplier = m;
            }
            Object v = m.invoke(null, data);
            if (v instanceof Number n) {
                double d = n.doubleValue();
                if (Double.isFinite(d) && d > 0.0d) {
                    return d;
                }
            }
        } catch (Throwable ignored) {
        }
        return 1.0d;
    }

    /** Ki/stamina pool keys — Overhaul scale stays off these (2.4.85). */
    public static boolean isResourcePoolStat(String stat) {
        if (stat == null || stat.isBlank()) {
            return false;
        }
        return "ENE".equalsIgnoreCase(stat)
                || "STM".equalsIgnoreCase(stat)
                || "ENERGY".equalsIgnoreCase(stat)
                || "STAMINA".equalsIgnoreCase(stat);
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
                        + "prestigeEnabled()={} LM integration={}",
                AdaptiveDifficultyMod.MOD_ID,
                levels,
                prestigeFlag,
                enabled,
                lmCfg != null && lmCfg.enableOverhaulPrestigeIntegration);
    }

    /**
     * Fabled Prestige class starts at level 1. Overhaul count is 0-based, same as
     * DMZ {@code prestige} skill and CNPC faction 4: {@code n → max(0, n − 1)}.
     */
    public static int toOverhaulCount(int oneBasedLevel) {
        return Math.max(0, Math.min(OVERHAUL_MAX_PRESTIGE, oneBasedLevel - 1));
    }

    /** Invert {@link #toOverhaulCount} for Overhaul UI → LM held. */
    public static int heldFromOverhaulCount(int overhaulCount) {
        int n = Math.max(0, overhaulCount) + 1;
        return Math.max(0, Math.min(PrestigeSystem.maxHeld(), n));
    }

    /** Held wallet vs Fabled class — both 1-based floors — then {@code n − 1} for Overhaul. */
    public static int overhaulCountFromHeld(ServerPlayer player) {
        int held = player == null ? 0 : PrestigeSystem.getHeld(player);
        int fabled = 0;
        try {
            fabled = PrestigeSkillSync.fabledPrestigeLevel(player);
        } catch (Throwable ignored) {
        }
        return toOverhaulCount(Math.max(held, fabled));
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
        int overhaul = Math.max(0, Math.min(
                OVERHAUL_MAX_PRESTIGE, DmzRevampPrestigeBridge.overhaulCount(data)));
        int mapped = overhaulCountFromHeld(player);
        if (overhaul <= mapped) {
            return;
        }
        int newHeld = heldFromOverhaulCount(overhaul);
        int held = PrestigeSystem.getHeld(player);
        int delta = Math.max(0, newHeld - held);
        PrestigeSystem.setHeldPublic(player, newHeld);
        if (delta > 0) {
            PrestigeSystem.setCompletedPublic(player, PrestigeSystem.getCompleted(player) + delta);
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
