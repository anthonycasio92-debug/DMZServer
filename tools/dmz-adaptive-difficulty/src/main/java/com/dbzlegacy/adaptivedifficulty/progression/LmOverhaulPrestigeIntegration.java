package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dragonminez.common.stats.StatsData;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;

/**
 * Use dmzrevamp Overhaul prestige (Statistics UI, scaling, saga rebirth) while Legacy Mechanics
 * owns playable stat totals via mixins. Combat scale reads Overhaul
 * {@code PrestigeSystem.scaleMultiplier} directly so prestige count changes apply even when
 * the Overhaul JSON cache is stale.
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
     * Overhaul {@code LevelingRevampConfig.prestigeEnabled()}. True results are cached;
     * false is retried so a late JSON load can still turn prestige on.
     */
    public static boolean overhaulPrestigeEnabled() {
        try {
            Class<?> cfg = Class.forName("com.dmzrevamp.config.LevelingRevampConfig");
            Object v = cfg.getMethod("prestigeEnabled").invoke(null);
            boolean ok = v instanceof Boolean b && b;
            if (ok) {
                overhaulPrestigeEnabled = Boolean.TRUE;
            }
            return ok;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void clearConfigCache() {
        overhaulPrestigeEnabled = null;
        overhaulScaleMultiplier = null;
    }

    /**
     * Overhaul {@code PrestigeSystem.scaleMultiplier} = {@code 1 + count × scaleBonusPerPrestige}.
     * Not gated on {@link #overhaulPrestigeEnabled()} so HUD/combat keep the live count scale.
     */
    public static double combatScaleMultiplier(StatsData data) {
        if (data == null) {
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

    public static void logOverhaulPrestigeState() {
        clearConfigCache();
    }

    /** Held and Overhaul prestige are the same number (0…10). */
    public static int toOverhaulCount(int held) {
        return Math.max(0, Math.min(OVERHAUL_MAX_PRESTIGE, held));
    }

    /** Invert {@link #toOverhaulCount} for Overhaul UI → LM held. */
    public static int heldFromOverhaulCount(int overhaulCount) {
        try {
            Class<?> cls = Class.forName(
                    "com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem");
            int max = ((Number) cls.getMethod("maxHeld").invoke(null)).intValue();
            return Math.max(0, Math.min(max, Math.max(0, overhaulCount)));
        } catch (Throwable ignored) {
            return Math.max(0, overhaulCount);
        }
    }

    /** Overhaul Statistics count = LM held (1:1). */
    public static int overhaulCountFromHeld(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        try {
            Class<?> cls = Class.forName(
                    "com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem");
            for (Method method : cls.getMethods()) {
                if ("getHeld".equals(method.getName()) && method.getParameterCount() == 1) {
                    return toOverhaulCount(((Number) method.invoke(null, player)).intValue());
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /** Overhaul native prestige no longer writes back into the LM wallet. */
    public static void syncLmWalletFromOverhaulCount(ServerPlayer player) {}
}
