package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatRepair;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intentional DMZ primary writes go through public setters and private
 * {@code setAttributeBaseValue}. Record them into the Mohist wipe snapshot — including 0 —
 * so tick/read-side restore cannot resurrect pre-reset stats.
 * <p>
 * Mohist dim-change wipes attributes without calling these methods, so the prior snapshot
 * remains available for recovery.
 */
@Mixin(value = Stats.class, remap = false)
public abstract class StatsSetPrimaryMixin {
    @Shadow
    private Player player;

    @Inject(method = "setAttributeBaseValue", at = @At("RETURN"), remap = false)
    private void dmzmmf$recordPrimaryWrite(Attribute attribute, int value, CallbackInfo ci) {
        CombatRepair.recordPrimaryWrite(this.player, attribute, value);
    }

    @Inject(method = "setStrength", at = @At("RETURN"), remap = false)
    private void dmzmmf$recordStrength(int value, CallbackInfo ci) {
        recordLive(MainAttributes.STRENGTH.get(), value);
    }

    @Inject(method = "setStrikePower", at = @At("RETURN"), remap = false)
    private void dmzmmf$recordStrikePower(int value, CallbackInfo ci) {
        recordLive(MainAttributes.STRIKE_POWER.get(), value);
    }

    @Inject(method = "setResistance", at = @At("RETURN"), remap = false)
    private void dmzmmf$recordResistance(int value, CallbackInfo ci) {
        recordLive(MainAttributes.RESISTANCE.get(), value);
    }

    @Inject(method = "setVitality", at = @At("RETURN"), remap = false)
    private void dmzmmf$recordVitality(int value, CallbackInfo ci) {
        recordLive(MainAttributes.VITALITY.get(), value);
    }

    @Inject(method = "setKiPower", at = @At("RETURN"), remap = false)
    private void dmzmmf$recordKiPower(int value, CallbackInfo ci) {
        recordLive(MainAttributes.KI_POWER.get(), value);
    }

    @Inject(method = "setEnergy", at = @At("RETURN"), remap = false)
    private void dmzmmf$recordEnergy(int value, CallbackInfo ci) {
        recordLive(MainAttributes.ENERGY.get(), value);
    }

    private void recordLive(Attribute attribute, int fallback) {
        if (this.player == null || attribute == null) {
            return;
        }
        AttributeInstance inst = this.player.m_21051_(attribute);
        int recorded = fallback;
        if (inst != null) {
            double base = inst.m_22115_();
            if (Double.isFinite(base)) {
                recorded = (int) Math.round(base);
            }
        }
        CombatRepair.recordPrimaryWrite(this.player, attribute, Math.max(0, recorded));
    }
}
