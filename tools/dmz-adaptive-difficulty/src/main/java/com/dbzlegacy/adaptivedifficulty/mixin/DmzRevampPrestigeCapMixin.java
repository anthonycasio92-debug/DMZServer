package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulCapMath;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration;
import com.dbzlegacy.adaptivedifficulty.progression.LmStatsDataAccess;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * LM pins Overhaul playable max at 150k (no breakthrough shop). Prestige eligibility
 * uses Legacy Mechanics Need (20k ladder → held-based gates), not “must fill cap”.
 * {@code hexStatReference} follows the same cap. Playable totals stay
 * {@link com.dbzlegacy.adaptivedifficulty.mixin.StatsDataMixin} + this
 * {@code maxAssignableTotal} + TP/stat soft-locks.
 */
@Mixin(targets = "com.dmzrevamp.revamp.prestige.PrestigeSystem", remap = false, priority = 5000)
public abstract class DmzRevampPrestigeCapMixin {

    @Inject(method = "levelCap", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$personalOverhaulLevelCap(StatsData data, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(LmOverhaulCapMath.overhaulLevelCap(data));
    }

    @Inject(method = "maxAssignableTotal", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$maxAssignableTotal(StatsData data, CallbackInfoReturnable<Integer> cir) {
        if (data == null || !lmCapsEnforced()) {
            return;
        }
        cir.setReturnValue(LmOverhaulCapMath.maxAssignableTotal(data));
    }

    @Inject(method = "hexStatReference", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$hexReferenceUsesPersonalCap(StatsData data, CallbackInfoReturnable<Double> cir) {
        if (data == null || !lmCapsEnforced()) {
            return;
        }
        int cap = LmOverhaulCapMath.overhaulLevelCap(data);
        cir.setReturnValue((double) Math.max(1, cap));
    }

    @Inject(method = "canPrestige", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lm$blockOverhaulRebirth(StatsData data, CallbackInfoReturnable<Boolean> cir) {
        int count = 0;
        try {
            count = com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge
                    .overhaulCount(data);
        } catch (Throwable ignored) {
        }
        if (count >= LmOverhaulPrestigeIntegration.OVERHAUL_MAX_PRESTIGE) {
            cir.setReturnValue(false);
            return;
        }
        if (lmOwnsPrestige() && !LmOverhaulPrestigeIntegration.integrationActive()) {
            cir.setReturnValue(false);
            return;
        }
        if (data == null) {
            return;
        }
        try {
            ServerPlayer sp = LmStatsDataAccess.serverPlayer(data);
            int need = sp != null ? PrestigeSystem.requiredLevel(sp) : LmOverhaulCapMath.overhaulLevelCap(data);
            cir.setReturnValue(data.getLevel() >= need);
        } catch (Throwable ignored) {
        }
    }

    @ModifyVariable(method = "setCount", at = @At("HEAD"), argsOnly = true, remap = false)
    private static int lm$capOverhaulPrestigeCount(int count) {
        return Math.max(0, Math.min(LmOverhaulPrestigeIntegration.OVERHAUL_MAX_PRESTIGE, count));
    }

    private static boolean lmOwnsPrestige() {
        try {
            return DifficultyConfig.get().enablePrestigeSystem;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean lmCapsEnforced() {
        try {
            return ModList.get().isLoaded("dmzrevamp");
        } catch (Throwable t) {
            return true;
        }
    }
}
