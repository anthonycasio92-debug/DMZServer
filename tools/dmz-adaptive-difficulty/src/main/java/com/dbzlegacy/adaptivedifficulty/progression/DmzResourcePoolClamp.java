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
 * One prestige-aware ki/stamina maximum used by the HUD, clamps, and Overhaul sync.
 *
 * <p>{@code actualMaxEnergy}/{@code actualMaxStamina} are the live
 * {@code getMaxEnergy}/{@code getMaxStamina} values from DragonMineZ and the other
 * installed mods. LegacyMechanics does not multiply those caps by a prestige scale.
 *
 * <p>Currents clamp to those maxima only — never to the unscaled HUD reconstruction
 * (2.4.93) and never by raising the advertised max to the overflowing current.
 */
public final class DmzResourcePoolClamp {
    /** Below this, {@code setCurrentEnergy} clears Limit Release. */
    private static final float POWER_RELEASE_FLOOR = 1.0f;
    /** {@code StatsData.getSecondaryAttributeValue} default when the client has no extras. */
    private static final double HUD_SECONDARY_DEFAULT = 20.0d;
    private static final ThreadLocal<Boolean> READING_NATIVE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    private DmzResourcePoolClamp() {}

    /** True while computing the unscaled native {@code getMax*} (HUD mixin must not scale). */
    public static boolean isReadingNativeMax() {
        return Boolean.TRUE.equals(READING_NATIVE.get());
    }

    /** Identity. Prestige scale is owned by dmzrevamp / DragonMineZ, not LM. */
    public static float applyOverhaulScale(StatsData data, float base) {
        return base;
    }

    /** Authoritative ki cap — live {@code getMaxEnergy} (HUD mixin applies scale once). */
    public static float actualMaxEnergy(StatsData data) {
        return data == null ? 0f : data.getMaxEnergy();
    }

    /** Live {@code getMaxStamina}. LM does not multiply this. */
    public static float actualMaxStamina(StatsData data) {
        return data == null ? 0f : data.getMaxStamina();
    }

    /** Alias of {@link #actualMaxEnergy(StatsData)} — one canonical ki max. */
    public static float displayMaxEnergy(StatsData data) {
        return actualMaxEnergy(data);
    }

    /** Alias of {@link #actualMaxStamina(StatsData)} — one canonical stamina max. */
    public static float displayMaxStamina(StatsData data) {
        return actualMaxStamina(data);
    }

    /**
     * Live DMZ ki/stamina cap: native {@code getMax*} unless Iron-contaminated,
     * otherwise the HUD formula. Prestige scale is applied exactly once.
     */
    private static float actualMax(StatsData data, boolean energy) {
        if (data == null) {
            return 0f;
        }
        float nativeMax = readNativeMax(data, energy);
        if (energy && nativeMax > POWER_RELEASE_FLOOR && looksLikeIronMana(data, nativeMax)) {
            nativeMax = 0f;
        }
        if (Float.isFinite(nativeMax) && nativeMax > POWER_RELEASE_FLOOR) {
            return applyOverhaulScale(data, nativeMax);
        }
        try {
            float hud = hudFormulaMax(data, energy);
            if (Float.isFinite(hud) && hud > POWER_RELEASE_FLOOR) {
                return applyOverhaulScale(data, hud);
            }
        } catch (Throwable ignored) {
        }
        float fallback = investedFallback(data, energy);
        if (fallback > POWER_RELEASE_FLOOR) {
            return applyOverhaulScale(data, fallback);
        }
        return 0f;
    }

    private static float readNativeMax(StatsData data, boolean energy) {
        READING_NATIVE.set(Boolean.TRUE);
        try {
            float live = energy ? data.getMaxEnergy() : data.getMaxStamina();
            return Float.isFinite(live) ? live : 0f;
        } catch (Throwable ignored) {
            return 0f;
        } finally {
            READING_NATIVE.set(Boolean.FALSE);
        }
    }

    private static float investedFallback(StatsData data, boolean energy) {
        try {
            Stats stats = data.getStats();
            if (stats == null) {
                return 0f;
            }
            int invested = energy ? stats.getEnergy() : stats.getResistance();
            if (invested <= 0) {
                return 0f;
            }
            double scaling = resolveHudScaling(data, energy ? "ENE" : "STM");
            float fallback = (float) (HUD_SECONDARY_DEFAULT + invested * scaling);
            return Float.isFinite(fallback) && fallback > POWER_RELEASE_FLOOR ? fallback : 0f;
        } catch (Throwable ignored) {
            return 0f;
        }
    }

    private static boolean looksLikeIronMana(StatsData data, float liveMax) {
        try {
            net.minecraft.world.entity.player.Player player = data.getPlayer();
            if (player == null) {
                return false;
            }
            net.minecraft.resources.ResourceLocation id =
                    new net.minecraft.resources.ResourceLocation("irons_spellbooks", "max_mana");
            net.minecraft.world.entity.ai.attributes.Attribute attr =
                    net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(id);
            if (attr == null) {
                return false;
            }
            net.minecraft.world.entity.ai.attributes.AttributeInstance inst = player.m_21051_(attr);
            if (inst == null) {
                return false;
            }
            double iron = inst.m_22115_();
            if (!Double.isFinite(iron) || iron < 100.0d) {
                return false;
            }
            return Math.abs(liveMax - iron) <= Math.max(25.0d, iron * 0.08d);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Clamp currents to {@link #actualMaxEnergy}/{@link #actualMaxStamina}. */
    public static boolean clamp(StatsData data) {
        return clampToCanonical(data);
    }

    public static void clampAndSync(ServerPlayer player, StatsData data) {
        if (clamp(data) && player != null) {
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

    /** HUD and Statistics use the canonical prestige-aware max. */
    public static float toHudMax(float live, StatsData data, boolean energy) {
        return energy ? actualMaxEnergy(data) : actualMaxStamina(data);
    }

    public static boolean shouldClampCurrent(float current, float max) {
        return Float.isFinite(current)
                && Float.isFinite(max)
                && max > POWER_RELEASE_FLOOR
                && current > max + 0.08f;
    }

    /** Same as {@link #clamp(StatsData)} — currents never sit above the canonical max. */
    public static boolean clampToOverhaulPool(StatsData data) {
        return clampToCanonical(data);
    }

    private static boolean clampToCanonical(StatsData data) {
        if (data == null) {
            return false;
        }
        Resources res = data.getResources();
        if (res == null) {
            return false;
        }
        boolean changed = false;
        try {
            float maxE = actualMaxEnergy(data);
            float curE = res.getCurrentEnergy();
            if (shouldClampCurrent(curE, maxE)) {
                res.setCurrentEnergy(maxE);
                changed = true;
            }
        } catch (Throwable ignored) {
        }
        try {
            float maxS = actualMaxStamina(data);
            float curS = res.getCurrentStamina();
            if (shouldClampCurrent(curS, maxS)) {
                res.setCurrentStamina(maxS);
                changed = true;
            }
        } catch (Throwable ignored) {
        }
        return changed;
    }

    /**
     * Same arithmetic as {@code StatsData.getMaxEnergy}/{@code getMaxStamina} with the
     * secondary attribute forced to 20. Does not call those getters. The scale is the
     * live {@code getStatScaling} those mods already computed.
     */
    private static float hudFormulaMax(StatsData data, boolean energy) {
        Stats stats = data.getStats();
        if (stats == null) {
            return 0f;
        }
        int invested = energy ? stats.getEnergy() : stats.getResistance();
        String key = energy ? "ENE" : "STM";
        double scaling = sanePositive(resolveHudScaling(data, key), 1.0d);
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

    /**
     * Live {@code getStatScaling}. DragonMineZ and the other installed mods already
     * summed race, class, and prestige. Replacing that here was the same extra
     * multiply that used to sit on ki.
     */
    private static double resolveHudScaling(StatsData data, String key) {
        try {
            double live = data.getStatScaling(key);
            if (Double.isFinite(live) && live > 0.0d) {
                return live;
            }
        } catch (Throwable ignored) {
        }
        return 1.0d;
    }

    private static double sanePositive(double value, double fallback) {
        return Double.isFinite(value) && value > 0.0d ? value : fallback;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0d;
    }
}
