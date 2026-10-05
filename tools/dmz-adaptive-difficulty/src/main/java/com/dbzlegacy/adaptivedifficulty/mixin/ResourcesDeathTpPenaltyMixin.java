package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.tp.DeathTpPenalty;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When the resources player is null, DragonMineZ skips {@code calculateTPGain}
 * and adds the raw amount. That path is halved here.
 * <p>
 * When the player is set, DragonMineZ posts {@code TPGainEvent} and replaces
 * the gain with {@code calculateTPGain}. Halving the raw input hides the cut
 * inside that boost. The finished grant is halved once, after that math, and
 * {@link StatsDataDeathTpPenaltyMixin} does not cut again on the same call.
 */
@Mixin(value = Resources.class, remap = false, priority = 6200)
public abstract class ResourcesDeathTpPenaltyMixin {
    @Shadow
    private Player player;

    @Shadow
    private StatsData statsData;

    @ModifyVariable(
            method = "addTrainingPoints(FZ)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0,
            remap = false
    )
    private float lm$halveDeathTp(float amount) {
        DeathTpPenalty.enterGrant();
        if (!(amount > 0f)) {
            return amount;
        }
        Player resolved = player;
        if (!(resolved instanceof ServerPlayer)) {
            try {
                if (statsData != null) {
                    resolved = statsData.getPlayer();
                }
            } catch (Throwable ignored) {
                resolved = player;
            }
        }
        if (player instanceof ServerPlayer) {
            return amount;
        }
        if (!(resolved instanceof ServerPlayer sp)) {
            return amount;
        }
        float next = DeathTpPenalty.applyToAmount(sp, amount);
        if (next != amount) {
            DeathTpPenalty.explainGrant(sp, Math.max(0, (int) amount), Math.max(0, (int) next));
        }
        return next;
    }

    @Inject(method = "addTrainingPoints(FZ)V", at = @At("RETURN"), remap = false)
    private void lm$endDeathTpGrant(CallbackInfo ci) {
        DeathTpPenalty.exitGrant();
    }
}
