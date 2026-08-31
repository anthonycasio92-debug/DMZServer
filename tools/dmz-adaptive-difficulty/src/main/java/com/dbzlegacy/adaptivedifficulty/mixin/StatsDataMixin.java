package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Per-player DMZ level cap from prestige breakthroughs.
 * Server {@code maxValue} stays 100k; breakthrough buyers get a higher personal
 * {@link StatsData#getConfiguredMaxValue()} so they can level into 110k…150k normally.
 */
@Mixin(StatsData.class)
public abstract class StatsDataMixin {
    @Shadow
    public abstract Player getPlayer();

    @Inject(method = "getConfiguredMaxValue", at = @At("RETURN"), cancellable = true)
    private void lm$personalBreakthroughCap(CallbackInfoReturnable<Integer> cir) {
        Integer serverMax = cir.getReturnValue();
        int base = serverMax == null || serverMax <= 0 ? PrestigePointsSystem.BASE_LEVEL_CAP : serverMax;
        Player p = getPlayer();
        if (!(p instanceof ServerPlayer sp)) {
            return;
        }
        try {
            int personal = PrestigePointsSystem.effectiveMaxLevel(sp);
            if (personal > base) {
                cir.setReturnValue(personal);
            }
        } catch (Throwable ignored) {
            // Prestige system / NBT unavailable — keep server default.
        }
    }
}
