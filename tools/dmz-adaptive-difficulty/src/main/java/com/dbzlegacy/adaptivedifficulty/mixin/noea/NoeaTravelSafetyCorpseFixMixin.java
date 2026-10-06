package com.dbzlegacy.adaptivedifficulty.mixin.noea;

import net.minecraftforge.event.entity.living.LivingDropsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Noea's travel safety cancels LivingDropsEvent on death in protected
 * dimensions, which also suppresses the Corpse mod (corelib spawns corpses
 * from its own LivingDropsEvent handler, skipped for cancelled events).
 * This stops Noea's method just before setCanceled(true) — drops stay
 * cleared, the event stays alive, corelib spawns the corpse, and Noea
 * still restores the inventory on respawn. No duplication, no loss.
 */
@Mixin(targets = "com.butterjaffa.noeabosses.TravelSafetyService", remap = false)
public abstract class NoeaTravelSafetyCorpseFixMixin {

    @Inject(method = "suppressConfirmedDrops",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraftforge/event/entity/living/LivingDropsEvent;setCanceled(Z)V",
                     remap = false),
            cancellable = true,
            remap = false,
            require = 0)
    private static void lm$keepDropsEventAlive(LivingDropsEvent event, CallbackInfo ci) {
        try {
            ci.cancel();
        } catch (Throwable ignored) {
        }
    }
}
