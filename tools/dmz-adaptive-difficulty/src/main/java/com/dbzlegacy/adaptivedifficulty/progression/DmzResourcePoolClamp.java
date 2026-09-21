package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.character.DmzContentDiscovery;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.BonusStats;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.server.level.ServerPlayer;

/**
 * HUD-formula helpers for display / Fabled sync. Current-pool clamp is off (2.4.93).
 *
 * <p>XenoverseHUD / AlternativeHUD call {@code getMaxEnergy()}/{@code getMaxStamina()} on the
 * <em>client</em>. {@code ResourceSyncS2C} only sends current energy/stamina. Server
 * {@code getMax*} adds Mohist / Apotheosis / Potentialist extras that never reach the client
 * bar, so a full server pool reads as over-max (HUD 159/145 vs overlay 159/190).
 *
 * <p>2.4.87 used {@code data.getStatScaling(class)} and returned 0 on any throw.
 * Live {@code config/dragonminez/races/ancient_saiyan/stats.json} (and saiyan) only has
 * {@code classes.race}. JLDK is {@code warrior} — {@code getStatScaling} NPEs on the missing
 * class (or on null {@code Double} defaults from an auto-created empty ClassStats).
 * {@code displayMax} then returned 0, {@code shouldClampCurrent} refused to clamp (power-release
 * floor), and current stayed at Iron {@code max_mana} 67474 / 171345.5 after the 2.4.87 boot.
 *
 * <p>Cap = DMZ formula with the vanilla secondary default the HUD actually uses:
 * {@code 20 + (invested + bonusMult) × scaling × totalMult + bonusAdd × scaling}.
 * Scaling is the fighting-class row when that id exists in the race JSON, otherwise
 * {@code classes.race} (live 0.6 ancient / 0.8 saiyan). Never call {@code getMax*}.
 *
 * <p>Never clamp current energy to {@code ≤ 1} — {@code Resources#setCurrentEnergy} zeros
 * power release at that threshold.
 */
public final class DmzResourcePoolClamp {
    /** Below this, {@code setCurrentEnergy} clears Limit Release. */
    private static final float POWER_RELEASE_FLOOR = 1.0f;
    /** {@code StatsData.getSecondaryAttributeValue} default when the client has no extras. */
    private static final double HUD_SECONDARY_DEFAULT = 20.0d;

    private DmzResourcePoolClamp() {}

    /** HUD-matching ki cap (Statistics Max Ki). */
    public static float displayMaxEnergy(StatsData data) {
        return displayMax(data, true);
    }

    /**
     * Live DMZ ki cap ({@code StatsData.getMaxEnergy}), not the invested ENE stat
     * and not the HUD reconstruction from that stat. Rejects Iron {@code max_mana}
     * contamination (2.4.87).
     */
    public static float actualMaxEnergy(StatsData data) {
        if (data == null) {
            return 0f;
        }
        float live = 0f;
        try {
            live = data.getMaxEnergy();
        } catch (Throwable ignored) {
        }
        if (Float.isFinite(live) && live > POWER_RELEASE_FLOOR && !looksLikeIronMana(data, live)) {
            return live;
        }
        try {
            float hud = displayMaxEnergy(data);
            if (Float.isFinite(hud) && hud > POWER_RELEASE_FLOOR) {
                return hud;
            }
        } catch (Throwable ignored) {
        }
        return Float.isFinite(live) && live > POWER_RELEASE_FLOOR ? live : 0f;
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

    /** HUD-matching stamina cap (Statistics Stamina). */
    public static float displayMaxStamina(StatsData data) {
        return displayMax(data, false);
    }

    /**
     * Disabled. Live HUD-formula clamp was shrinking ki/stamina currents to the
     * client bar (20 + invested × race scaling) and fighting native {@code getMax*}.
     * Call sites stay so a later opt-in can restore writes without a mixin hunt.
     */
    public static boolean clamp(StatsData data) {
        return false;
    }

    public static void clampAndSync(ServerPlayer player, StatsData data) {
        // no-op — do not write current energy/stamina down to the HUD formula
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

    /** Disabled — never pull current ki/stamina down to the HUD formula. */
    public static boolean shouldClampCurrent(float current, float hudMax) {
        return false;
    }

    private static float displayMax(StatsData data, boolean energy) {
        if (data == null) {
            return 0f;
        }
        float base = 0f;
        try {
            float hud = hudFormulaMax(data, energy);
            if (Float.isFinite(hud) && hud > POWER_RELEASE_FLOOR) {
                base = hud;
            }
        } catch (Throwable ignored) {
        }
        if (base <= POWER_RELEASE_FLOOR) {
            try {
                Stats stats = data.getStats();
                if (stats != null) {
                    int invested = energy ? stats.getEnergy() : stats.getResistance();
                    if (invested > 0) {
                        double scaling = resolveHudScaling(data, energy ? "ENE" : "STM");
                        float fallback = (float) (HUD_SECONDARY_DEFAULT + invested * scaling);
                        if (Float.isFinite(fallback) && fallback > POWER_RELEASE_FLOOR) {
                            base = fallback;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (base <= POWER_RELEASE_FLOOR) {
            return 0f;
        }
        // Fabled / Statistics follow Overhaul scaleMultiplier (ENE/STM stay out of getTotalMultiplier).
        try {
            double scale = LmOverhaulPrestigeIntegration.combatScaleMultiplier(data);
            if (Double.isFinite(scale) && scale > 1.000_001d) {
                float scaled = (float) (base * scale);
                if (Float.isFinite(scaled) && scaled > POWER_RELEASE_FLOOR) {
                    return scaled;
                }
            }
        } catch (Throwable ignored) {
        }
        return base;
    }

    /**
     * Pull current ki/stamina down only when they exceed the Overhaul-scaled pool
     * (not the unscaled HUD bar). Used by the Fabled bridge.
     */
    public static boolean clampToOverhaulPool(StatsData data) {
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
            if (Float.isFinite(maxE) && maxE > POWER_RELEASE_FLOOR
                    && Float.isFinite(curE) && curE > maxE + 0.08f) {
                res.setCurrentEnergy(maxE);
                changed = true;
            }
        } catch (Throwable ignored) {
        }
        try {
            float maxS = displayMaxStamina(data);
            float curS = res.getCurrentStamina();
            if (Float.isFinite(maxS) && maxS > POWER_RELEASE_FLOOR
                    && Float.isFinite(curS) && curS > maxS + 0.08f) {
                res.setCurrentStamina(maxS);
                changed = true;
            }
        } catch (Throwable ignored) {
        }
        return changed;
    }

    /**
     * Same arithmetic as {@code StatsData.getMaxEnergy}/{@code getMaxStamina} with the
     * secondary attribute forced to 20. Does not call those getters. Does not call
     * {@code getStatScaling} first — that creates an empty warrior/tank row (all 1.0)
     * when live race JSON only has {@code classes.race}.
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
     * Fighting-class scaling when that id is in the race JSON; otherwise {@code classes.race}.
     * Never calls {@code RaceStatsConfig#getClassStats} for a missing id — that inserts an
     * empty ClassStats with 1.0 defaults and poisons later {@code getStatScaling} reads.
     */
    private static double resolveHudScaling(StatsData data, String key) {
        String race = raceId(data);
        String cls = classId(data);
        if (race != null && classConfigured(race, cls)) {
            Double fromClass = scalingFromConfig(race, cls, key);
            if (fromClass != null) {
                return fromClass;
            }
        }
        if (race != null && classConfigured(race, "race")) {
            Double fromRace = scalingFromConfig(race, "race", key);
            if (fromRace != null) {
                return fromRace;
            }
        }
        try {
            double live = data.getStatScaling(key);
            if (Double.isFinite(live) && live > 0.0d && live != 1.0d) {
                return live;
            }
        } catch (Throwable ignored) {
        }
        return 1.0d;
    }

    private static String raceId(StatsData data) {
        try {
            Character ch = data.getCharacter();
            if (ch == null) {
                return null;
            }
            String race = ch.getRaceName();
            if (race == null || race.isBlank()) {
                race = ch.getRace();
            }
            if (race == null || race.isBlank()) {
                return null;
            }
            return race.trim().toLowerCase();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String classId(StatsData data) {
        try {
            Character ch = data.getCharacter();
            if (ch == null) {
                return null;
            }
            String cls = ch.getCharacterClass();
            if (cls == null || cls.isBlank()) {
                return null;
            }
            return cls.trim().toLowerCase();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean classConfigured(String race, String classId) {
        if (race == null || race.isBlank() || classId == null || classId.isBlank()) {
            return false;
        }
        try {
            for (String id : DmzContentDiscovery.classIdsForRace(race)) {
                if (id != null && classId.equalsIgnoreCase(id.trim())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static Double scalingFromConfig(String race, String classId, String key) {
        if (race == null || classId == null || key == null) {
            return null;
        }
        try {
            RaceStatsConfig cfg = ConfigManager.getRaceStats(race);
            if (cfg == null) {
                return null;
            }
            String canonical = null;
            try {
                for (String id : cfg.getAllClasses()) {
                    if (id != null && classId.equalsIgnoreCase(id.trim())) {
                        canonical = id.trim();
                        break;
                    }
                }
            } catch (Throwable ignored) {
            }
            if (canonical == null) {
                canonical = classId;
            }
            RaceStatsConfig.ClassStats classStats = cfg.getClassStats(canonical);
            if (classStats == null) {
                return null;
            }
            RaceStatsConfig.StatScaling scaling = classStats.getStatScaling();
            if (scaling == null) {
                return null;
            }
            Double value = "ENE".equalsIgnoreCase(key)
                    ? scaling.getEnergyScaling()
                    : scaling.getStaminaScaling();
            if (value == null || !Double.isFinite(value) || value <= 0.0d) {
                return null;
            }
            return value;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static double sanePositive(double value, double fallback) {
        return Double.isFinite(value) && value > 0.0d ? value : fallback;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0d;
    }
}
