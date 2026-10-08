package com.dbzlegacy.adaptivedifficulty.event;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.server.events.players.StatsEvents;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Logs a tall fall, and when vanilla max health is far below DMZ max health,
 * restores that pool before DMZ's fall handler and ki negation run.
 *
 * <p>StatsData does not store a current health. The number fall damage
 * subtracts is the entity health. This does not cancel the event or change
 * the damage amount.
 */
public final class FallDamageDiag {
    public FallDamageDiag() {}

    /** Runs before DMZ's normal-priority fall handler. */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onFall(LivingFallEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player) || player.m_9236_().f_46443_) {
                return;
            }
            if (event.getDistance() < 3.0f) {
                return;
            }
            boolean synced = alignVanillaPool(player);
            print(player, "fall", event.getDistance(), -1.0f, event.isCanceled(), "pending", synced);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onHurt(LivingHurtEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player) || player.m_9236_().f_46443_) {
                return;
            }
            if (event.getSource() == null || !event.getSource().m_269533_(DamageTypeTags.f_268549_)) {
                return;
            }
            boolean kiNegated = event.isCanceled();
            print(player, "hurt", player.f_19789_, event.getAmount(), event.isCanceled(),
                    Boolean.toString(kiNegated), false);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Vanilla max is far below the DMZ max. 20 versus thousands qualifies.
     * A small gap does not.
     */
    static boolean vanillaPoolDesynced(float vanillaMax, float dmzMax) {
        return Float.isFinite(vanillaMax)
                && Float.isFinite(dmzMax)
                && dmzMax - vanillaMax > 10.0f
                && vanillaMax < dmzMax * 0.5f;
    }

    /** Keep the old pool's fraction when that pool was the whole vanilla bar. */
    static float scaledFromOldPool(float vanillaHp, float vanillaMax, float newMax) {
        if (!(newMax > vanillaMax + 1.0f) || vanillaHp > vanillaMax + 0.5f) {
            return vanillaHp;
        }
        float ratio = vanillaMax <= 0.01f ? 1.0f : vanillaHp / vanillaMax;
        if (!Float.isFinite(ratio) || ratio < 0.0f) {
            ratio = 1.0f;
        }
        if (ratio > 1.0f) {
            ratio = 1.0f;
        }
        return Math.max(1.0f, Math.min(newMax, ratio * newMax));
    }

    private static boolean alignVanillaPool(ServerPlayer player) {
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return false;
        }
        float dmzMax = data.getMaxHealth();
        float vanillaMax = player.m_21233_();
        float vanillaHp = player.m_21223_();
        if (!vanillaPoolDesynced(vanillaMax, dmzMax)) {
            return false;
        }
        try {
            StatsEvents.applyHealthBonus(player);
        } catch (Throwable ignored) {
        }
        if (vanillaPoolDesynced(player.m_21233_(), dmzMax)) {
            raiseVanillaMax(player, dmzMax);
        }
        float newMax = player.m_21233_();
        float scaled = scaledFromOldPool(vanillaHp, vanillaMax, newMax);
        if (scaled > vanillaHp + 0.01f) {
            player.m_21153_(scaled);
        }
        return player.m_21233_() > vanillaMax + 1.0f;
    }

    /** Same UUID DMZ uses, so the bonus replaces the missing modifier instead of stacking. */
    private static void raiseVanillaMax(ServerPlayer player, float dmzMax) {
        AttributeInstance attr = player.m_21051_(Attributes.f_22276_);
        if (attr == null) {
            return;
        }
        double base = attr.m_22115_();
        double addition = dmzMax - base;
        if (!Double.isFinite(addition) || addition <= 0.0d) {
            return;
        }
        UUID id = StatsEvents.DMZ_HEALTH_MODIFIER_UUID;
        attr.m_22120_(id);
        attr.m_22125_(new AttributeModifier(
                id,
                "DMZ Health Bonus",
                addition,
                AttributeModifier.Operation.ADDITION));
    }

    private static void print(
            ServerPlayer player,
            String phase,
            float fallDistance,
            float damage,
            boolean canceled,
            String kiNegated,
            boolean synced) {
        double vanillaHp = player.m_21223_();
        double vanillaMax = player.m_21233_();
        double dmzMax = Double.NaN;
        double energy = Double.NaN;
        boolean initialized = false;
        StatsData data = DmzProgression.stats(player);
        if (data != null) {
            dmzMax = data.getMaxHealth();
            initialized = data.hasInitializedHealth();
            Resources resources = data.getResources();
            if (resources != null) {
                energy = resources.getCurrentEnergy();
            }
        }
        double attrBase = Double.NaN;
        double attrValue = Double.NaN;
        AttributeInstance attr = player.m_21051_(Attributes.f_22276_);
        if (attr != null) {
            attrBase = attr.m_22115_();
            attrValue = attr.m_22135_();
        }
        String name = player.m_7755_() == null ? "?" : player.m_7755_().getString();
        System.out.println("[LM] Fall damage: phase=" + phase
                + " player=" + name
                + " vanilla=" + vanillaHp + "/" + vanillaMax
                + " dmzCurrent=not-stored dmzMax=" + dmzMax
                + " attr=" + attrBase + "/" + attrValue
                + " fallDistance=" + fallDistance
                + " damage=" + damage
                + " canceled=" + canceled
                + " kiNegated=" + kiNegated
                + " synced=" + synced
                + " energy=" + energy
                + " initialized=" + initialized);
    }
}
