package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.RateLog;
import com.dragonminez.server.events.players.combat.CombatEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={CombatEvent.class}, remap=false)
public abstract class CombatEventHurtSanitizeMixin {
    @Inject(method={"onLivingHurt"}, at={@At(value="RETURN")}, remap=false)
    private static void dmzmmf$finiteMeleeAmount(LivingHurtEvent event, CallbackInfo ci) {
        if (event.isCanceled()) {
            return;
        }
        float amt = event.getAmount();
        if (Float.isFinite(amt) && amt >= 0.0f) {
            return;
        }
        event.setAmount(1.0f);
        RateLog.info("combat-nan", 40, "sanitized non-finite CombatEvent hurt amount -> 1.0", new Object[0]);
    }
}

