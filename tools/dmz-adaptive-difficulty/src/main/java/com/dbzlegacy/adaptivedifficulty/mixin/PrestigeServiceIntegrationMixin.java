package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep LM held/completed NBT aligned when players rebirth through Overhaul Statistics UI. */
@Mixin(targets = "com.dmzrevamp.revamp.prestige.PrestigeService", remap = false)
public abstract class PrestigeServiceIntegrationMixin {

    @Inject(method = "tryPrestige", at = @At("RETURN"), remap = false)
    private static void lm$syncLmAfterOverhaulPrestige(ServerPlayer player, CallbackInfo ci) {
        try {
            LmOverhaulPrestigeIntegration.syncLmWalletFromOverhaulCount(player);
        } catch (Throwable ignored) {
        }
    }
}
