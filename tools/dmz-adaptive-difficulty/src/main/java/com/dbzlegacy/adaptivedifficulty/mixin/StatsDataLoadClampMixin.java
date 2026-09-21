package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.StatsDataLoadContext;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks DMZ stats NBT load — no ki clamp here (clamp on login via {@code ProgressionSystem}).
 * Running {@code DmzResourcePoolClamp} during load caused Mohist join failures
 * (Invalid player data).
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataLoadClampMixin {

    @Inject(method = "load", at = @At("HEAD"), remap = false)
    private void lm$loadEnter(CompoundTag tag, CallbackInfo ci) {
        StatsDataLoadContext.enter();
    }

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private void lm$loadExit(CompoundTag tag, CallbackInfo ci) {
        StatsDataLoadContext.exit();
    }
}
