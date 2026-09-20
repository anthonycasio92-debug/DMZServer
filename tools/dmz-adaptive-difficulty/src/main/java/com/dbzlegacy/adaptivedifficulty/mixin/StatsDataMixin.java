package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
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
 * Server {@code maxValue} is 150k so clients can display/buy past 100k; this mixin
 * clamps each player's {@link StatsData#getConfiguredMaxValue()} to their personal
 * breakthrough ceiling (100k…150k) on the server.
 *
 * <p>{@code remap = false} is required — DMZ methods are not obfuscated (same pattern as
 * {@code dmz_mohist_melee_fix} StatsData mixins). With remap left on, this inject never
 * applied and the personal cap stayed stuck at the server default.
 *
 * <p>{@code priority = 1100} runs after {@code dmzrevamp}'s {@code StatsDataLevelingRevampMixin}
 * (default 1000), which otherwise leaves prestige-0 players at Overhaul's stock
 * {@code initialLevelCap} of 50k instead of the normal 100k.
 */
@Mixin(value = StatsData.class, remap = false, priority = 1500)
public abstract class StatsDataMixin {
    @Shadow(remap = false)
    public abstract Player getPlayer();

    @Inject(method = "getConfiguredMaxValue", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$personalBreakthroughCap(CallbackInfoReturnable<Integer> cir) {
        try {
            if (!DifficultyConfig.get().enablePrestigeSystem) {
                return;
            }
        } catch (Throwable t) {
            return;
        }
        Player p;
        try {
            p = getPlayer();
        } catch (Throwable t) {
            return;
        }
        if (!(p instanceof ServerPlayer sp)) {
            return;
        }
        try {
            int personal = PrestigePointsSystem.effectiveMaxLevel(sp);
            if (personal <= 0) {
                return;
            }
            int overhaul = 0;
            Integer returned = cir.getReturnValue();
            if (returned != null) {
                overhaul = Math.max(0, returned);
            }
            int cap = personal;
            if (overhaul > personal && overhaul <= PrestigePointsSystem.ABSOLUTE_LEVEL_CAP) {
                cap = overhaul;
            } else if (overhaul < personal) {
                cap = personal;
            }
            cir.setReturnValue(cap);
        } catch (Throwable ignored) {
            // Prestige system / NBT unavailable — keep server default.
        }
    }
}
