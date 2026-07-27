package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.PrimaryStatRepair;
import com.dragonminez.common.stats.character.Stats;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * After a cross-world teleport Mohist can zero {@code dragonminez:strength} (etc.) bases.
 * Empty-hand melee reads those via {@code getMeleeDamage()} → {@code getStrength()}.
 * If the live attribute base is 0/missing but we still have a spawn-world snapshot, use it
 * (and try to write the attribute back). Does not touch {@code ki_damage}.
 * <p>
 * Intentional resets clear/update the snapshot (and suppress restore), so this must not
 * resurrect pre-reset stats.
 */
@Mixin(value = Stats.class, remap = false)
public abstract class StatsPrimaryAttrMixin {

    @Shadow
    private Player player;

    @Inject(method = "getAttributeBaseValue", at = @At("RETURN"), cancellable = true, remap = false)
    private void dbzlegacy$primaryFallback(
            Attribute attribute,
            int fallback,
            CallbackInfoReturnable<Integer> cir
    ) {
        int live = cir.getReturnValueI();
        if (live > 0) {
            return;
        }
        int saved = PrimaryStatRepair.savedFor(this.player, attribute);
        if (saved <= 0) {
            return;
        }
        // Always allow read-side fallback (needed for percentage reset math when live is 0).
        cir.setReturnValue(saved);
        // Write-back restore stays blocked during intentional reset suppress windows.
        if (!PrimaryStatRepair.isSuppressed(this.player)) {
            PrimaryStatRepair.restore(this.player, "stats-read");
        }
    }
}
