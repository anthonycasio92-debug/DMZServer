package com.dbzlegacy.adaptivedifficulty.mixin.noea;

import com.butterjaffa.noeabosses.TravelSafetyService;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In a protected dimension Noea's {@code suppressConfirmedDrops} clears the
 * death drop list and cancels the event. It does not snapshot the inventory.
 * That copy is written earlier by {@code captureInventory}, which this mixin
 * does not touch. After the list is cleared, corelib
 * {@code Death.processDrops} deletes every inventory item that is not on the
 * drop list, so the corpse spawns empty and the items exist only in Noea's
 * record until respawn.
 *
 * <p>{@code suppressConfirmedDrops} is static, so this injector is static.
 * Cancelling at the start skips the confirm flag, the drop clear, and the
 * event cancel. The drop list stays full, corelib leaves those items alone,
 * and the corpse receives them. Respawn restore runs only after that confirm
 * flag, so it does not put them back as well.
 */
@Mixin(value = TravelSafetyService.class, remap = false)
public abstract class NoeaTravelSafetyCorpseFixMixin {

    @Inject(
            method = "suppressConfirmedDrops",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0)
    private static void lm$disableDropSuppression(LivingDropsEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
