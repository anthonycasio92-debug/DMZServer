package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prestige / form scaling can raise {@link StatsData#getMaxEnergy()} after current was filled,
 * or lower max while current stays high — clamp on every write.
 */
@Mixin(value = Resources.class, remap = false)
public abstract class ResourcesPoolClampMixin {

    @ModifyVariable(method = "setCurrentEnergy", at = @At("HEAD"), argsOnly = true, remap = false)
    private float lm$clampSetEnergy(float value) {
        return clampEnergy(value);
    }

    @ModifyVariable(method = "setCurrentStamina", at = @At("HEAD"), argsOnly = true, remap = false)
    private float lm$clampSetStamina(float value) {
        return clampStamina(value);
    }

    @Inject(method = "addEnergy", at = @At("RETURN"), remap = false)
    private void lm$clampAfterAddEnergy(CallbackInfo ci) {
        clampBoth();
    }

    @Inject(method = "addStamina", at = @At("RETURN"), remap = false)
    private void lm$clampAfterAddStamina(CallbackInfo ci) {
        clampBoth();
    }

    private void clampBoth() {
        Resources self = (Resources) (Object) this;
        StatsData data = self.getStatsData();
        if (data != null) {
            DmzResourcePoolClamp.clamp(data);
        }
    }

    private float clampEnergy(float value) {
        if (!Float.isFinite(value)) {
            return value;
        }
        Resources self = (Resources) (Object) this;
        StatsData data = self.getStatsData();
        if (data == null) {
            return value;
        }
        try {
            float max = DmzResourcePoolClamp.displayMaxEnergy(data);
            if (DmzResourcePoolClamp.shouldClampCurrent(value, max)) {
                return max;
            }
        } catch (Throwable ignored) {
        }
        return value;
    }

    private float clampStamina(float value) {
        if (!Float.isFinite(value)) {
            return value;
        }
        Resources self = (Resources) (Object) this;
        StatsData data = self.getStatsData();
        if (data == null) {
            return value;
        }
        try {
            float max = DmzResourcePoolClamp.displayMaxStamina(data);
            if (DmzResourcePoolClamp.shouldClampCurrent(value, max)) {
                return max;
            }
        } catch (Throwable ignored) {
        }
        return value;
    }
}
