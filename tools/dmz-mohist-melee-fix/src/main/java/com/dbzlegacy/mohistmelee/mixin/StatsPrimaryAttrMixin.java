package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatRepair;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * After a cross-world teleport Mohist can zero primary bases. Empty-hand melee reads those via
 * getMeleeDamage → getStrength. If live base is 0/missing but a spawn-world snapshot exists, use it.
 * Skipped while an intentional reset suppress window is active.
 */
@Mixin(value = Stats.class, remap = false)
public abstract class StatsPrimaryAttrMixin {
    @Shadow
    private Player player;

    @Inject(method = "getAttributeBaseValue", at = @At("RETURN"), cancellable = true, remap = false)
    private void dmzmmf$primaryFallback(Attribute attribute, int fallback, CallbackInfoReturnable<Integer> cir) {
        if (CombatRepair.isSuppressed(this.player)) {
            return;
        }
        if (cir.getReturnValueI() > 0) {
            return;
        }
        int saved = CombatRepair.savedFor(this.player, attribute);
        if (saved <= 0) {
            return;
        }
        cir.setReturnValue(saved);
        CombatRepair.restoreOnly(this.player, "stats-read");
    }
}
