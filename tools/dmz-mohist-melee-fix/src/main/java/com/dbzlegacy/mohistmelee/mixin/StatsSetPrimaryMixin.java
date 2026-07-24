package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.PrimaryStatRepair;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DMZ intentional primary writes ({@code setStrength}/{@code resetPlayerProgress}) go through
 * {@code setAttributeBaseValue}. Record those into the Mohist wipe snapshot — including 0 —
 * so tick/read-side restore cannot resurrect pre-reset stats.
 * <p>
 * Mohist dim-change wipes attributes without calling this method, so the prior snapshot remains
 * available for recovery.
 */
@Mixin(value = Stats.class, remap = false)
public abstract class StatsSetPrimaryMixin {

    @Shadow
    private Player player;

    @Inject(method = "setAttributeBaseValue", at = @At("RETURN"), remap = false)
    private void dbzlegacy$recordPrimaryWrite(Attribute attribute, int value, CallbackInfo ci) {
        PrimaryStatRepair.record(this.player, attribute, value);
    }
}
