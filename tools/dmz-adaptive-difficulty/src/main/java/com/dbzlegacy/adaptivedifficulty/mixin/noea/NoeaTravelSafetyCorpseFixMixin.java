package com.dbzlegacy.adaptivedifficulty.mixin.noea;

import com.butterjaffa.noeabosses.TravelSafetyService;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Noea's off-world protection confirms a saved inventory, clears the drop
 * list, and cancels LivingDropsEvent. Corpse stores items from that event,
 * so a cancelled event leaves an empty corpse and the saved copy comes back
 * on respawn.
 *
 * <p>{@code suppressConfirmedDrops} is static, so this injector is static.
 * Cancelling at the start skips the confirm flag, the drop clear, and the
 * event cancel. The items stay on the drop list. Respawn restore runs only
 * after that confirm flag, so it does not put them back.
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
