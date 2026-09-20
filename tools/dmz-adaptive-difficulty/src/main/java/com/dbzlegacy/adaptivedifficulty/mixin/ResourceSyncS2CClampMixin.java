package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.dragonminez.common.network.S2C.ResourceSyncS2C", remap = false)
public abstract class ResourceSyncS2CClampMixin {

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"), remap = false)
    private void lm$clampBeforeResourcePacket(ServerPlayer player, CallbackInfo ci) {
        if (player == null) {
            return;
        }
        StatsData data = DmzProgression.stats(player);
        if (data != null) {
            DmzResourcePoolClamp.clamp(data);
        }
    }
}
