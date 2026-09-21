package com.dbzlegacy.mohistmelee.mixin;

import com.dmzrevamp.item.HandwearCombatEvents;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DMZ Revamp applies Apotheosis affixes from curio handwear inside {@link LivingHurtEvent}.
 * Affixes such as Thunderstruck deal follow-up damage, which re-enters the same hook and can
 * overflow the stack (seen on live with wristbands + Simply More weapons, Sep 2026).
 */
@Mixin(value = HandwearCombatEvents.class, remap = false)
public abstract class HandwearApotheosisReentrancyMixin {
    private static final ThreadLocal<Boolean> IN_HANDWEAR_APOTH_HOOK =
            ThreadLocal.withInitial(() -> false);

    @Inject(method = "runApotheosisHandwearAttackHooks", at = @At("HEAD"), cancellable = true, remap = false)
    private static void dbzlegacy$blockReentrantHandwearApoth(LivingHurtEvent event, CallbackInfo ci) {
        if (Boolean.TRUE.equals(IN_HANDWEAR_APOTH_HOOK.get())) {
            ci.cancel();
            return;
        }
        IN_HANDWEAR_APOTH_HOOK.set(true);
    }

    @Inject(method = "runApotheosisHandwearAttackHooks", at = @At("RETURN"), remap = false)
    private static void dbzlegacy$clearHandwearApothHook(LivingHurtEvent event, CallbackInfo ci) {
        IN_HANDWEAR_APOTH_HOOK.set(false);
    }
}
