package com.dbzlegacy.adaptivedifficulty.progression;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.BonusStats;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keep live ki/stamina at or below the client HUD cap.
 *
 * <p>XenoverseHUD / AlternativeHUD call {@code getMaxEnergy()}/{@code getMaxStamina()} on the
 * <em>client</em>. {@code ResourceSyncS2C} only sends current energy/stamina. Server
 * {@code getMax*} adds Mohist / Apotheosis / Potentialist extras that never reach the client
 * bar, so a full server pool reads as over-max (HUD 159/145 vs overlay 159/190).
 *
 * <p>2.4.86 redirected {@code getSecondaryAttributeValue} to 20 and then clamped to
 * {@code getMaxEnergy()}. Live JLDK1310.dat (saved 22 min after that boot) still had
 * CurrentEnergy 11228 / CurrentStamina 76868 with {@code dragonminez:max_energy} Base 20
 * and no persistent modifiers — runtime armor extras kept {@code getMaxEnergy()} ≥ current,
 * so the clamp never fired. Do not call {@code getMax*} for the cap.
 *
 * <p>Cap = DMZ formula with the vanilla secondary default the HUD actually uses:
 * {@code 20 + (invested + bonusMult) × scaling × totalMult + bonusAdd × scaling}.
 *
 * <p>Never clamp current energy to {@code ≤ 1} — {@code Resources#setCurrentEnergy} zeros
 * power release at that threshold.
 */
public final class DmzResourcePoolClamp {
    private static final float EPS = 0.08f;
    /** Below this, {@code setCurrentEnergy} clears Limit Release. */
    private static final float POWER_RELEASE_FLOOR = 1.0f;
    /** {@code StatsData.getSecondaryAttributeValue} default when the client has no extras. */
    private static final double HUD_SECONDARY_DEFAULT = 20.0d;

    private DmzResourcePoolClamp() {}

    /** HUD-matching ki cap (Statistics Max Ki). */
    public static float displayMaxEnergy(StatsData data) {
        return displayMax(data, true);
    }

    /** HUD-matching stamina cap (Statistics Stamina). */
    public static float displayMaxStamina(StatsData data) {
        return displayMax(data, false);
    }

    /** @return true if either pool was lowered */
    public static boolean clamp(StatsData data) {
        if (data == null) {
            return false;
        }
        Resources res = data.getResources();
        if (res == null) {
            return false;
        }
        boolean changed = false;
        try {
            float maxE = displayMaxEnergy(data);
            float curE = res.getCurrentEnergy();
            if (shouldClampCurrent(curE, maxE)) {
                res.setCurrentEnergy(maxE);
                changed = true;
            }
        } catch (Throwable ignored) {
        }
        try {
            float maxS = displayMaxStamina(data);
            float curS = res.getCurrentStamina();
            if (shouldClampCurrent(curS, maxS)) {
                res.setCurrentStamina(maxS);
                changed = true;
            }
        } catch (Throwable ignored) {
        }
        return changed;
    }

    public static void clampAndSync(ServerPlayer player, StatsData data) {
        if (player == null || data == null) {
            return;
        }
        if (clamp(data)) {
            syncToClient(player);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Prefer the HUD formula. {@code live} is ignored — server {@code getMax*} is not the bar.
     */
    public static float toHudMax(float live, StatsData data, boolean energy) {
        return displayMax(data, energy);
    }

    /** True when current should be pulled down — never through the power-release-zero floor. */
    public static boolean shouldClampCurrent(float current, float hudMax) {
        if (!Float.isFinite(current) || !Float.isFinite(hudMax)) {
            return false;
        }
        if (hudMax <= POWER_RELEASE_FLOOR && current > POWER_RELEASE_FLOOR) {
            return false;
        }
        return hudMax > 0f && current > hudMax + EPS;
    }

    private static float displayMax(StatsData data, boolean energy) {
        if (data == null) {
            return 0f;
        }
        try {
            float hud = hudFormulaMax(data, energy);
            if (Float.isFinite(hud) && hud > 0f) {
                return hud;
            }
        } catch (Throwable ignored) {
        }
        return 0f;
    }

    /**
     * Same arithmetic as {@code StatsData.getMaxEnergy}/{@code getMaxStamina} with the
     * secondary attribute forced to 20. Does not call those getters.
     */
    private static float hudFormulaMax(StatsData data, boolean energy) {
        Stats stats = data.getStats();
        if (stats == null) {
            return 0f;
        }
        int invested = energy ? stats.getEnergy() : stats.getResistance();
        String key = energy ? "ENE" : "STM";
        double scaling = sanePositive(data.getStatScaling(key), 1.0d);
        double totalMult = 1.0d;
        try {
            totalMult = sanePositive(data.getTotalMultiplier(key), 1.0d);
        } catch (Throwable ignored) {
        }
        double bonusAdd = 0.0d;
        double bonusMult = 0.0d;
        try {
            BonusStats bonus = data.getBonusStats();
            if (bonus != null) {
                bonusAdd = finiteOrZero(bonus.calculateBonus(key, invested, false));
                bonusMult = finiteOrZero(bonus.calculateBonus(key, invested, true));
            }
        } catch (Throwable ignored) {
        }
        double max = HUD_SECONDARY_DEFAULT
                + (invested + bonusMult) * scaling * totalMult
                + bonusAdd * scaling;
        if (!Double.isFinite(max) || max <= 0.0d) {
            return 0f;
        }
        return (float) Math.min(max, Float.MAX_VALUE);
    }

    private static double sanePositive(double value, double fallback) {
        return Double.isFinite(value) && value > 0.0d ? value : fallback;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0d;
    }
}
