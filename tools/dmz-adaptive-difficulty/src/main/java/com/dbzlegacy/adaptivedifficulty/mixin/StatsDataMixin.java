package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.FusionBonusOwner;
import com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulCapMath;
import com.dbzlegacy.adaptivedifficulty.progression.PersonalLevelCapMirror;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides dmzrevamp {@code StatsDataLevelingRevampMixin} (default priority 1000).
 * Priority 2000 merges after Overhaul so our HEAD/RETURN injects run last and
 * {@code setReturnValue} wins: personal 100k + 10k×breakthroughs only — never
 * stock 50k at prestige 0 or +5k/held from Overhaul's native ladder.
 */
@Mixin(value = StatsData.class, remap = false, priority = 5000)
public abstract class StatsDataMixin {
    @Shadow(remap = false)
    public abstract Player getPlayer();

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void lm$rememberFusionOwner(Player player, CallbackInfo ci) {
        FusionBonusOwner.remember((StatsData) (Object) this);
    }

    @Inject(method = "getBonusStats", at = @At("RETURN"), remap = false)
    private void lm$rememberFusionOwnerOnRead(CallbackInfoReturnable<com.dragonminez.common.stats.character.BonusStats> cir) {
        FusionBonusOwner.remember((StatsData) (Object) this);
    }

    @Inject(method = "getConfiguredMaxValue", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$personalMaxValueHead(CallbackInfoReturnable<Integer> cir) {
        applyPersonalMaxValue(cir);
    }

    @Inject(method = "getConfiguredMaxValue", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$personalMaxValue(CallbackInfoReturnable<Integer> cir) {
        applyPersonalMaxValue(cir);
    }

    @Inject(method = "getConfiguredMaxTotalStats", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$personalMaxTotalHead(CallbackInfoReturnable<Integer> cir) {
        applyPersonalMaxTotal(cir);
    }

    @Inject(method = "getConfiguredMaxTotalStats", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$personalMaxTotal(CallbackInfoReturnable<Integer> cir) {
        applyPersonalMaxTotal(cir);
    }

    @Inject(method = "getMaxAllowedIncreaseForStat", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$clampStatBuyHead(String stat, int amount, CallbackInfoReturnable<Integer> cir) {
        applyStatBuyClamp(stat, amount, cir);
    }

    @Inject(method = "getMaxAllowedIncreaseForStat", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$clampStatBuy(String stat, int amount, CallbackInfoReturnable<Integer> cir) {
        applyStatBuyClamp(stat, amount, cir);
    }

    private void applyPersonalMaxValue(CallbackInfoReturnable<Integer> cir) {
        if (!lmCapsActive()) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        int personal = Math.max(
                PrestigePointsSystem.BASE_LEVEL_CAP, LmOverhaulCapMath.personalLevelCap(self));
        PersonalLevelCapMirror.bind(self, personal);
        cir.setReturnValue(personal);
    }

    private void applyPersonalMaxTotal(CallbackInfoReturnable<Integer> cir) {
        if (!lmCapsActive()) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        cir.setReturnValue(LmOverhaulCapMath.maxAssignableTotal(self));
    }

    private void applyStatBuyClamp(String stat, int amount, CallbackInfoReturnable<Integer> cir) {
        if (!lmCapsActive()) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        int personal = LmOverhaulCapMath.personalLevelCap(self);
        int maxTotal = LmOverhaulCapMath.maxAssignableTotal(self, personal);
        int total = 0;
        try {
            if (self.getStats() != null) {
                total = Math.max(0, self.getStats().getTotalStats());
            }
        } catch (Throwable ignored) {
        }
        int room = Math.max(0, maxTotal - total);
        int requested = Math.max(0, amount);
        int previous = cir.getReturnValue() != null ? Math.max(0, cir.getReturnValue()) : requested;
        cir.setReturnValue(Math.min(previous, Math.min(requested, room)));
    }

    /** Mods do not load or unload while the server is up. */
    private static volatile Boolean CAPS_ACTIVE;

    /** Always enforce on production (dmzrevamp present); config toggles must not re-enable stock 50k ladder. */
    private static boolean lmCapsActive() {
        Boolean known = CAPS_ACTIVE;
        if (known != null) {
            return known;
        }
        boolean loaded = true;
        try {
            loaded = ModList.get().isLoaded("dmzrevamp");
        } catch (Throwable ignored) {
            loaded = true;
        }
        CAPS_ACTIVE = loaded;
        return loaded;
    }
}
