package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Same mirror-drain protection for stamina while below scaled max. */
@Mixin(value = Resources.class, remap = false)
public abstract class ResourcesStaminaDrainGuardMixin {

    private static final float SMALL_DRAIN_CAP = 64f;

    @Inject(method = "removeStamina", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$blockSmallDrainBelowMax(float amount, CallbackInfo ci) {
        if (amount <= 0f || !DifficultyConfig.get().enableEnergyManaSync) {
            return;
        }
        Resources self = (Resources) (Object) this;
        Player player = self.getPlayer();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        StatsData data = self.getStatsData();
        if (data == null) {
            return;
        }
        float cur = self.getCurrentStamina();
        float max = data.getMaxStamina();
        if (max > 0.5f && cur < max - 0.15f && amount <= SMALL_DRAIN_CAP) {
            ci.cancel();
        }
    }

    @Inject(method = "setCurrentStamina", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$blockDownwardSetBelowMax(float value, CallbackInfo ci) {
        if (!DifficultyConfig.get().enableEnergyManaSync) {
            return;
        }
        Resources self = (Resources) (Object) this;
        float cur = self.getCurrentStamina();
        if (value >= cur - 0.01f) {
            return;
        }
        float drain = cur - value;
        if (drain <= 0f || drain > SMALL_DRAIN_CAP) {
            return;
        }
        StatsData data = self.getStatsData();
        if (data == null) {
            return;
        }
        float max = data.getMaxStamina();
        if (max > 0.5f && cur < max - 0.15f) {
            ci.cancel();
        }
    }
}
