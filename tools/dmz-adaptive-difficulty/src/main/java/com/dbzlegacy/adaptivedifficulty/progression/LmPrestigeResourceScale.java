package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Lifetime LM completed prestiges → ki/stamina (and Overhaul {@code scaleMultiplier}) bonus.
 * Level caps / stat totals stay on {@link LmOverhaulCapMath} + {@link com.dbzlegacy.adaptivedifficulty.mixin.StatsDataMixin}
 * — not dmzrevamp {@code Prestige.enabled} ladder.
 */
public final class LmPrestigeResourceScale {
    private LmPrestigeResourceScale() {}

    /** Same formula as Overhaul {@code 1 + count × scaleBonusPerPrestige}. */
    public static double multiplier(StatsData data) {
        if (LmOverhaulPrestigeIntegration.overhaulPrestigeEnabled()) {
            return 1.0;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        if (cfg == null || !cfg.enablePrestigeSystem || !cfg.enablePrestigeResourceScaling) {
            return 1.0;
        }
        double per = cfg.prestigeResourceScalePerCompleted;
        if (!Double.isFinite(per) || per <= 0.0) {
            return 1.0;
        }
        int count = completedPrestiges(data);
        if (count <= 0) {
            return 1.0;
        }
        double mult = 1.0 + count * per;
        return Double.isFinite(mult) && mult > 0.0 ? mult : 1.0;
    }

    public static float scaleMax(float baseMax, StatsData data) {
        if (baseMax <= 0f || !Float.isFinite(baseMax)) {
            return baseMax;
        }
        double mult = multiplier(data);
        if (mult <= 1.000_001) {
            return baseMax;
        }
        double scaled = baseMax * mult;
        if (!Double.isFinite(scaled)) {
            return baseMax;
        }
        return (float) Math.min(3.4028235E38, scaled);
    }

    public static int completedPrestiges(StatsData data) {
        if (data == null) {
            return 0;
        }
        ServerPlayer sp = LmStatsDataAccess.serverPlayer(data);
        if (sp != null) {
            return Math.max(0, PrestigeSystem.getCompleted(sp));
        }
        return Math.max(0, DmzRevampPrestigeBridge.overhaulCount(data));
    }
}
