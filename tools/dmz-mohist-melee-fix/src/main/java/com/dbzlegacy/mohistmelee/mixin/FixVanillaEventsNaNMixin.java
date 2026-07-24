package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.RateLog;
import com.dragonminez.server.events.FixVanillaEvents;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={FixVanillaEvents.class}, remap=false)
public abstract class FixVanillaEventsNaNMixin {
    @Inject(method={"onAttackEntity"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void dmzmmf$allowNaNAttack(LivingAttackEvent event, CallbackInfo ci) {
        float amt = event.getAmount();
        if (Float.isNaN(amt) || Float.isInfinite(amt)) {
            RateLog.info("nan", 40, "prevented NaN cancel where=LivingAttack", new Object[0]);
            ci.cancel();
        }
    }

    @Inject(method={"onLivingHurt"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void dmzmmf$sanitizeHurt(LivingHurtEvent event, CallbackInfo ci) {
        float amt = event.getAmount();
        if (Float.isNaN(amt) || Float.isInfinite(amt)) {
            RateLog.info("nan", 40, "prevented NaN cancel where=LivingHurt", new Object[0]);
            event.setAmount(1.0f);
            ci.cancel();
        }
    }

    @Inject(method={"onLivingDamage"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void dmzmmf$sanitizeDamage(LivingDamageEvent event, CallbackInfo ci) {
        float amt = event.getAmount();
        if (Float.isNaN(amt) || Float.isInfinite(amt)) {
            RateLog.info("nan", 40, "prevented NaN cancel where=LivingDamage", new Object[0]);
            event.setAmount(1.0f);
            ci.cancel();
        }
    }
}

