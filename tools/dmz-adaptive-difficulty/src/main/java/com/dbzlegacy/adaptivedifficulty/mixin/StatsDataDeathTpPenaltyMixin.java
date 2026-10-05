package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.tp.DeathTpPenalty;
import com.dragonminez.common.config.TpSource;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * After a death, cut the training points DragonMineZ is about to grant in half.
 * {@code calculateTPGain} is the single path that turns a raw award into the
 * number actually added, including the global TP boost. The one-argument
 * overload delegates here, so this runs once.
 */
@Mixin(value = StatsData.class, remap = false, priority = 6200)
public abstract class StatsDataDeathTpPenaltyMixin {

    @Inject(
            method = "calculateTPGain(ILcom/dragonminez/common/config/TpSource;)I",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void lm$halveTpAfterDeath(int amount, TpSource source, CallbackInfoReturnable<Integer> cir) {
        int gain = cir.getReturnValue();
        if (gain <= 0) {
            return;
        }
        Player player;
        try {
            player = ((StatsData) (Object) this).getPlayer();
        } catch (Throwable ignored) {
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        // addTrainingPoints already halved this grant. Cutting calculateTPGain
        // again would turn the penalty into a quarter.
        if (DeathTpPenalty.inGrant()) {
            return;
        }
        int next = DeathTpPenalty.applyToGain(sp, gain);
        if (next != gain) {
            cir.setReturnValue(next);
        }
    }
}
