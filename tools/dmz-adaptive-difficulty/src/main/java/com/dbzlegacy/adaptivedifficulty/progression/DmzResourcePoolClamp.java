package com.dbzlegacy.adaptivedifficulty.progression;

import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;

/**
 * Keep live ki/stamina at or below the Statistics / HUD cap.
 *
 * {@link StatsData#getMaxEnergy()} adds Forge {@code MAX_ENERGY} (default 20). Mohist often
 * applies Potentialist / Overhaul attribute modifiers on the server that never reach the
 * client, so server max is ~4/3 of the HUD number and current looks "over max".
 */
public final class DmzResourcePoolClamp {
    private static final float EPS = 0.08f;
    /** Vanilla DMZ secondary-attribute fallback when {@code StatsData.player} is null. */
    private static final double ATTR_DEFAULT = 20.0d;

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
            if (Float.isFinite(maxE) && maxE > 0f) {
                float curE = res.getCurrentEnergy();
                if (Float.isFinite(curE) && curE > maxE + EPS) {
                    res.setCurrentEnergy(maxE);
                    changed = true;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            float maxS = displayMaxStamina(data);
            if (Float.isFinite(maxS) && maxS > 0f) {
                float curS = res.getCurrentStamina();
                if (Float.isFinite(curS) && curS > maxS + EPS) {
                    res.setCurrentStamina(maxS);
                    changed = true;
                }
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

    private static float displayMax(StatsData data, boolean energy) {
        if (data == null) {
            return 0f;
        }
        float live = energy ? data.getMaxEnergy() : data.getMaxStamina();
        if (!Float.isFinite(live) || live <= 0f) {
            return 0f;
        }
        double attr = readSecondary(data, energy ? MainAttributes.MAX_ENERGY : MainAttributes.MAX_STAMINA);
        double extra = attr - ATTR_DEFAULT;
        if (extra > 1.0d) {
            live = (float) Math.max(ATTR_DEFAULT, live - extra);
        }
        return live;
    }

    private static double readSecondary(StatsData data, RegistryObject<Attribute> attr) {
        if (data == null || attr == null) {
            return ATTR_DEFAULT;
        }
        try {
            Attribute resolved = attr.get();
            Player player = data.getPlayer();
            if (player == null || resolved == null) {
                return ATTR_DEFAULT;
            }
            AttributeInstance inst = player.m_21051_(resolved);
            if (inst == null) {
                return ATTR_DEFAULT;
            }
            double v = inst.m_22135_();
            return Double.isFinite(v) ? v : ATTR_DEFAULT;
        } catch (Throwable ignored) {
            return ATTR_DEFAULT;
        }
    }
}
