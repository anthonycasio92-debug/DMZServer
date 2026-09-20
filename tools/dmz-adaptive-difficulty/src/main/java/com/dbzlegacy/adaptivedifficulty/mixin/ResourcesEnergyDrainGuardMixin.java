package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.bridge.EnergyManaSync;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks CNPC {@code DMZ Fabled Bridge.js} from calling {@code removeEnergy} when Fabled mana
 * flickers below stored last-mana while DMZ is the authoritative ki pool.
 */
@Mixin(value = Resources.class, remap = false)
public abstract class ResourcesEnergyDrainGuardMixin {

    @Inject(method = "removeEnergy", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$blockFabledMirrorDrain(float amount, CallbackInfo ci) {
        if (amount <= 0f) {
            ci.cancel();
            return;
        }
        Resources self = (Resources) (Object) this;
        Player player = self.getPlayer();
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        if (EnergyManaSync.shouldBlockFabledMirrorDrain(server, self, amount)) {
            ci.cancel();
        }
    }
}
