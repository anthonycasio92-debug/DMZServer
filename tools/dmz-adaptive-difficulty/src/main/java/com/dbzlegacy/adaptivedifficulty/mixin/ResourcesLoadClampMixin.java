package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** {@link Resources#load} writes {@code currentEnergy} directly — bypasses {@code setCurrentEnergy} clamp. */
@Mixin(value = Resources.class, remap = false)
public abstract class ResourcesLoadClampMixin {

    @Inject(method = "load", at = @At("RETURN"), remap = false)
    private void lm$clampAfterLoad(CompoundTag tag, CallbackInfo ci) {
        lm$reclamp();
    }

    @Inject(method = "copyFrom", at = @At("RETURN"), remap = false)
    private void lm$clampAfterCopy(Resources other, CallbackInfo ci) {
        lm$reclamp();
    }

    private void lm$reclamp() {
        Resources self = (Resources) (Object) this;
        StatsData data = self.getStatsData();
        if (data != null) {
            try {
                DmzResourcePoolClamp.clamp(data);
            } catch (Throwable t) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] Ki/stamina clamp skipped during Resources load: {}",
                        AdaptiveDifficultyMod.MOD_ID,
                        t.toString());
            }
        }
    }
}
