package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulCapMath;
import com.dbzlegacy.adaptivedifficulty.progression.LmStatsDataAccess;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Legacy Mechanics owns level caps and rebirth — not dmzrevamp Overhaul prestige ladder.
 * Overhaul still shows synced prestige count ({@link com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge})
 * but {@code levelCap} must follow LM breakthrough caps (100k–150k), not 50k / +40k per overhaul prestige.
 */
@Mixin(targets = "com.dmzrevamp.revamp.prestige.PrestigeSystem", remap = false)
public abstract class DmzRevampPrestigeCapMixin {

    @Inject(method = "levelCap", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$levelCap(StatsData data, CallbackInfoReturnable<Integer> cir) {
        if (!lmOwnsPrestige() || data == null) {
            return;
        }
        ServerPlayer sp = LmStatsDataAccess.serverPlayer(data);
        if (sp == null) {
            return;
        }
        cir.setReturnValue(LmOverhaulCapMath.personalLevelCap(sp));
    }

    @Inject(method = "maxAssignableTotal", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$maxAssignableTotal(StatsData data, CallbackInfoReturnable<Integer> cir) {
        if (!lmOwnsPrestige() || data == null) {
            return;
        }
        ServerPlayer sp = LmStatsDataAccess.serverPlayer(data);
        if (sp == null) {
            return;
        }
        cir.setReturnValue(LmOverhaulCapMath.maxAssignableTotal(data, sp));
    }

    @Inject(method = "canPrestige", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$blockOverhaulRebirth(StatsData data, CallbackInfoReturnable<Boolean> cir) {
        if (lmOwnsPrestige()) {
            cir.setReturnValue(false);
        }
    }

    private static boolean lmOwnsPrestige() {
        try {
            return DifficultyConfig.get().enablePrestigeSystem;
        } catch (Throwable t) {
            return false;
        }
    }
}
