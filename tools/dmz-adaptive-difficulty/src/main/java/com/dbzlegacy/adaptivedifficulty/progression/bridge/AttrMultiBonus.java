package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.BonusStats;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code Attr Fabled Multi bonus.js} — Fabled attributes → DMZ multiplicative bonuses.
 * Each invested attribute point = +1% multiplier (0 → 1.00x).
 */
public final class AttrMultiBonus {
    /** Matches live CNPC script name (color code included for remove/add identity). */
    public static final String BONUS_NAME = "\u00A76Prestige Bonus";
    /** Old typo from duplicate script — clear so it cannot stack. */
    private static final String LEGACY_BONUS_NAME = "\u00A76Prestrige Bonus";

    private AttrMultiBonus() {}

    /** Strip live “Prestige Bonus” rows when the bridge is disabled. */
    public static void clearIfPresent(ServerPlayer player) {
        if (player == null) {
            return;
        }
        StatsData dmz = DmzProgression.stats(player);
        if (dmz == null || dmz.getBonusStats() == null) {
            return;
        }
        clearAllNamedBonuses(dmz.getBonusStats());
        pushSync(player);
    }

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

        /*
         * Fabled attribute points survive DMZ wipe. Never re-apply while the character is wiped /
         * not created, or the wipe looks like it failed (matches Attr Fabled Multi bonus.js).
         */
        boolean characterCreated = false;
        try {
            var status = dmz.getStatus();
            characterCreated = status != null && status.isHasCreatedCharacter();
        } catch (Throwable ignored) {
            characterCreated = false;
        }

        clearAllNamedBonuses(bonusStats);

        if (!characterCreated) {
            pushSync(player);
            return;
        }

        double fStr = attr(data, "str");
        double fSkp = attr(data, "skp");
        double fRes = attr(data, "res");
        double fVit = attr(data, "vit");
        double fPwr = attr(data, "pwr");
        double fEne = attr(data, "ene");

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
            pushSync(player);
        }
    }

    private static void clearAllNamedBonuses(BonusStats bonusStats) {
        for (String name : new String[] {BONUS_NAME, LEGACY_BONUS_NAME}) {
            clearBonus(bonusStats, "STR", name);
            clearBonus(bonusStats, "SKP", name);
            clearBonus(bonusStats, "VIT", name);
            clearBonus(bonusStats, "PWR", name);
            clearBonus(bonusStats, "ENE", name);
            try {
                bonusStats.removeBonusSplit("RES", name);
            } catch (Throwable ignored) {
            }
            try {
                bonusStats.clearBonusSplit("RES", name);
            } catch (Throwable ignored) {
            }
        }
    }

    private static double attr(Object fabledData, String key) {
        double v = readAttrMethod(fabledData, "getAttribute", key);
        if (v <= 0) {
            v = readAttrMethod(fabledData, "getInvestedAttribute", key);
        }
        if (v <= 0) {
            v = readAttrMap(fabledData, key);
        }
        return v;
    }

    private static double readAttrMethod(Object fabledData, String method, String key) {
        try {
            Object v = fabledData.getClass().getMethod(method, String.class).invoke(fabledData, key);
            if (v instanceof Number n) {
                double d = n.doubleValue();
                return Double.isFinite(d) && d > 0 ? d : 0;
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static double readAttrMap(Object fabledData, String key) {
        try {
            Object raw = fabledData.getClass().getMethod("getAttributes").invoke(fabledData);
            if (raw instanceof Map<?, ?> map) {
                Object v = map.get(key);
                if (v == null) {
                    for (Map.Entry<?, ?> e : map.entrySet()) {
                        if (e.getKey() != null && key.equalsIgnoreCase(String.valueOf(e.getKey()))) {
                            v = e.getValue();
                            break;
                        }
                    }
                }
                if (v instanceof Number n) {
                    double d = n.doubleValue();
                    return Double.isFinite(d) && d > 0 ? d : 0;
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static double toMultiplier(double points) {
        return 1.0 + (Math.max(0, points) * 0.01);
    }

    private static void clearBonus(BonusStats bonusStats, String stat, String name) {
        try {
            bonusStats.removeBonus(stat, name);
        } catch (Throwable ignored) {
        }
        try {
            bonusStats.clearBonus(stat, name);
        } catch (Throwable ignored) {
        }
    }

    private static void pushSync(ServerPlayer player) {
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
    }
}
