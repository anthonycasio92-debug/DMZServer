package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatRepair;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intentional DMZ primary writes (setStrength / resetPlayerProgress) go through
 * setAttributeBaseValue. Record them into the Mohist wipe snapshot — including 0 —
 * so tick/read-side restore cannot resurrect pre-reset stats.
 */
@Mixin(value={Stats.class}, remap=false)
public abstract class StatsSetPrimaryMixin {
    @Shadow
    private Player player;

    @Inject(method={"setAttributeBaseValue"}, at={@At(value="RETURN")}, remap=false)
    private void dmzmmf$recordPrimaryWrite(Attribute attribute, int value, CallbackInfo ci) {
        CombatRepair.recordPrimaryWrite(this.player, attribute, value);
    }
}
