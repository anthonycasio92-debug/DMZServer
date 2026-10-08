package com.dbzlegacy.adaptivedifficulty.event;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Temporary console line for the fall-death reports. Does not change damage.
 *
 * <p>StatsData stores a max health and does not store a current health. The
 * number fall damage subtracts is the entity health. The log prints both so a
 * tall jump shows whether those pools have split.
 */
public final class FallDamageDiag {
    public FallDamageDiag() {}

    /** Distance is still set here. Hurt may run after the field is cleared. */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onFall(LivingFallEvent event) {
        try {
            if (!(event.getEntity() instanceof ServerPlayer player) || player.m_9236_().f_46443_) {
                return;
            }
            if (event.getDistance() < 3.0f) {
                return;
            }
            print(player, "fall", event.getDistance(), -1.0f, event.isCanceled());
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
            print(player, "hurt", player.f_19789_, event.getAmount(), event.isCanceled());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void print(ServerPlayer player, String phase, float fallDistance, float damage, boolean canceled) {
        double vanillaHp = player.m_21223_();
        double vanillaMax = player.m_21233_();
        double dmzMax = Double.NaN;
        boolean initialized = false;
        StatsData data = DmzProgression.stats(player);
        if (data != null) {
            dmzMax = data.getMaxHealth();
            initialized = data.hasInitializedHealth();
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
                + " dmz=not-stored/" + dmzMax
                + " attr=" + attrBase + "/" + attrValue
                + " fallDistance=" + fallDistance
                + " damage=" + damage
                + " canceled=" + canceled
                + " initialized=" + initialized);
    }
}
