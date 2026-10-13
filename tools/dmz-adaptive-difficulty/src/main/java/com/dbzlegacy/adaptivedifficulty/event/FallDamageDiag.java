package com.dbzlegacy.adaptivedifficulty.event;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.stats.StatsData;
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
 * When vanilla max health is far below DMZ max health, restores that pool before
 * DMZ's fall handler and ki negation run.
 *
 * <p>LivingHurtEvent for fall can fire without a preceding LivingFallEvent (ender
 * pearl / chorus / other teleport falls), so {@link #onHurt} also aligns the pool
 * at {@link EventPriority#HIGHEST}. This does not cancel the event or change the
 * damage amount.
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
            alignVanillaPool(player);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] FallDamageDiag.onFall failed", AdaptiveDifficultyMod.MOD_ID, t);
        }
    }

    /**
     * Sync before other HIGHEST hurt listeners (register this class before
     * {@link DifficultyEvents}). Covers fall damage with no LivingFallEvent.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onHurt(LivingHurtEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player) || player.m_9236_().f_46443_) {
                return;
            }
            if (event.getSource() == null || !event.getSource().m_269533_(DamageTypeTags.f_268549_)) {
                return;
            }
            alignVanillaPool(player);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] FallDamageDiag.onHurt failed", AdaptiveDifficultyMod.MOD_ID, t);
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
            // Stats not loaded yet - best-effort apply so a later hurt/fall can finish.
            try {
                StatsEvents.applyHealthBonus(player);
            } catch (Throwable ignored) {
            }
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
}
