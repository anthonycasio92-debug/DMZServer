package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** After full {@link StatsData#load}, {@code Resources} is attached — clamp NBT current pools. */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataLoadClampMixin {

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private void lm$clampAfterStatsLoad(CompoundTag tag, CallbackInfo ci) {
        DmzResourcePoolClamp.clamp((StatsData) (Object) this);
    }
}
