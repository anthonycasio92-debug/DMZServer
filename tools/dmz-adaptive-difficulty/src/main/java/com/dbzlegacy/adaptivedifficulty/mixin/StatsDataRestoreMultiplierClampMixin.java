package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataRestoreMultiplierClampMixin {

    @Inject(method = "restoreMultiplierGains", at = @At("RETURN"), remap = false)
    private void lm$clampAfterRestore(ServerPlayer player, float[] snapshot, CallbackInfo ci) {
        if (!LmOverhaulPrestigeIntegration.overhaulPrestigeEnabled()) {
            return;
        }
        DmzResourcePoolClamp.clamp((StatsData) (Object) this);
    }
}
