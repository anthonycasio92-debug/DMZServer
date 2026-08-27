package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.BonusStats;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code Attr Fabled Multi bonus.js} — Fabled attributes → DMZ multiplicative bonuses.
 * Each invested attribute point = +1% multiplier (0 → 1.00x).
 */
public final class AttrMultiBonus {
    /** Matches live CNPC script name (color code included for remove/add identity). */
    public static final String BONUS_NAME = "\u00A76Prestige Bonus";

    private AttrMultiBonus() {}

    public static void sync(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableAttrMultiBonus) {
            return;
        }
        Object data = FabledBridge.fabledData(player);
        if (data == null) {
            return;
        }
        StatsData dmz = DmzProgression.stats(player);
        if (dmz == null) {
            return;
        }
        BonusStats bonusStats = dmz.getBonusStats();
        if (bonusStats == null) {
            return;
        }

        double fStr = attr(data, "str");
        double fSkp = attr(data, "skp");
        double fRes = attr(data, "res");
        double fVit = attr(data, "vit");
        double fPwr = attr(data, "pwr");
        double fEne = attr(data, "ene");

        clearBonus(bonusStats, "STR");
        clearBonus(bonusStats, "SKP");
        clearBonus(bonusStats, "VIT");
        clearBonus(bonusStats, "PWR");
        clearBonus(bonusStats, "ENE");
        try {
            bonusStats.removeBonusSplit("RES", BONUS_NAME);
        } catch (Throwable ignored) {
        }

        boolean changed = false;
        if (fStr > 0) {
            bonusStats.addBonus("STR", BONUS_NAME, "*", toMultiplier(fStr));
            changed = true;
        }
        if (fSkp > 0) {
            bonusStats.addBonus("SKP", BONUS_NAME, "*", toMultiplier(fSkp));
            changed = true;
        }
        if (fRes > 0) {
            bonusStats.addBonusSplit("RES", BONUS_NAME, "*", toMultiplier(fRes), false);
            changed = true;
        }
        if (fVit > 0) {
            bonusStats.addBonus("VIT", BONUS_NAME, "*", toMultiplier(fVit));
            changed = true;
        }
        if (fPwr > 0) {
            bonusStats.addBonus("PWR", BONUS_NAME, "*", toMultiplier(fPwr));
            changed = true;
        }
        if (fEne > 0) {
            bonusStats.addBonus("ENE", BONUS_NAME, "*", toMultiplier(fEne));
            changed = true;
        }

        if (changed) {
            try {
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            } catch (Throwable ignored) {
            }
        }
    }

    private static double attr(Object fabledData, String key) {
        try {
            Object v = fabledData.getClass().getMethod("getAttribute", String.class).invoke(fabledData, key);
            if (v instanceof Number n) {
                double d = n.doubleValue();
                return Double.isFinite(d) && d > 0 ? d : 0;
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static double toMultiplier(double points) {
        return 1.0 + (Math.max(0, points) * 0.01);
    }

    private static void clearBonus(BonusStats bonusStats, String stat) {
        try {
            bonusStats.removeBonus(stat, BONUS_NAME);
        } catch (Throwable ignored) {
        }
        try {
            bonusStats.clearBonus(stat, BONUS_NAME);
        } catch (Throwable ignored) {
        }
    }
}
