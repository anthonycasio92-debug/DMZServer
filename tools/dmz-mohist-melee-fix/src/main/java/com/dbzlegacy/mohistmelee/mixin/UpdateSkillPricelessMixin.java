package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DmzMohistMeleeFix;
import com.dbzlegacy.mohistmelee.PricelessPurchaseGuard;
import com.dbzlegacy.mohistmelee.SkillTpCostHelper;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skill;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Block menu/packet purchase of priceless ({@code -1}) skills and stack forms.
 * <p>
 * SDU double-click / spam-click sends {@code PURCHASE} with {@code Math.max(0, cost)},
 * so {@code -1} arrives as free {@code 0}. Vanilla DMZ {@code computeTpCost} also clamps
 * with {@code Math.max(0, …)}, which would make the server accept that free buy.
 * <p>
 * Primary hook: cancel {@code lambda$handle$0} (same pattern as NPCAction disables) before
 * any TP spend / level grant. Also force {@code computeTpCost} to keep negatives so DMZ's
 * own {@code if (cost < 0) skip} checks fire.
 */
@Mixin(value = UpdateSkillC2S.class, priority = 2000, remap = false)
public abstract class UpdateSkillPricelessMixin {

    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);

    @Shadow
    @Final
    private String skillName;

    @Shadow
    @Final
    private UpdateSkillC2S.SkillAction action;

    /**
     * Hard cancel on the actual apply lambda (runs on server thread after enqueueWork).
     * Covers stack-form spam buys even if Math.max redirects miss.
     */
    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true, remap = false)
    private void dbzlegacy$blockPricelessApply(ServerPlayer player, StatsData data, CallbackInfo ci) {
        if (data == null || !dbzlegacy$isBuyAction()) {
            return;
        }
        if (dbzlegacy$shouldBlockPriceless(data)) {
            ci.cancel();
            return;
        }
        PricelessPurchaseGuard.enter(data);
    }

    /**
     * Always clear the packet-scoped guard after apply (success or early return inside DMZ).
     */
    @Inject(method = "lambda$handle$0", at = @At("RETURN"), remap = false)
    private void dbzlegacy$clearPricelessGuard(ServerPlayer player, StatsData data, CallbackInfo ci) {
        PricelessPurchaseGuard.exit();
    }

    private boolean dbzlegacy$isBuyAction() {
        return this.action == UpdateSkillC2S.SkillAction.PURCHASE
                || this.action == UpdateSkillC2S.SkillAction.UPGRADE;
    }

    private boolean dbzlegacy$shouldBlockPriceless(StatsData data) {
        if (this.action == null || this.skillName == null || this.skillName.isEmpty()) {
            return false;
        }
        if (!dbzlegacy$isBuyAction()) {
            return false;
        }
        try {
            int levelIndex = 0;
            if (this.action == UpdateSkillC2S.SkillAction.UPGRADE) {
                Skill skill = data.getSkills() != null ? data.getSkills().getSkill(this.skillName) : null;
                levelIndex = skill != null ? skill.getLevel() : 0;
            }
            int raw = SkillTpCostHelper.rawConfiguredCost(data, this.skillName, levelIndex);
            LOGGER.info(
                    "[{}] skill packet {} '{}' levelIndex={} rawCost={}",
                    DmzMohistMeleeFix.MOD_ID,
                    this.action.name().toLowerCase(),
                    this.skillName,
                    levelIndex,
                    raw
            );
            if (raw < 0) {
                LOGGER.info(
                        "[{}] blocked priceless {} of skill '{}' (levelIndex={}, rawCost={})",
                        DmzMohistMeleeFix.MOD_ID,
                        this.action.name().toLowerCase(),
                        this.skillName,
                        levelIndex,
                        raw
                );
                return true;
            }
            return false;
        } catch (Throwable t) {
            LOGGER.warn(
                    "[{}] priceless purchase guard failed for '{}': {}",
                    DmzMohistMeleeFix.MOD_ID,
                    this.skillName,
                    t.toString()
            );
            // Fail closed for menu buys: never let a broken cost lookup become a free unlock.
            return true;
        }
    }

    @Redirect(
            method = "computeTpCost(Lcom/dragonminez/common/stats/StatsData;Ljava/lang/String;I)I",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 0),
            remap = false
    )
    private static int dbzlegacy$preservePricelessMathMax0(int a, int b) {
        return dbzlegacy$preservePricelessMathMax(a, b);
    }

    @Redirect(
            method = "computeTpCost(Lcom/dragonminez/common/stats/StatsData;Ljava/lang/String;I)I",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I", ordinal = 1),
            remap = false
    )
    private static int dbzlegacy$preservePricelessMathMax1(int a, int b) {
        return dbzlegacy$preservePricelessMathMax(a, b);
    }

    private static int dbzlegacy$preservePricelessMathMax(int a, int b) {
        if (b < 0) {
            return b;
        }
        return Math.max(a, b);
    }

    @Inject(
            method = "computeTpCost(Lcom/dragonminez/common/stats/StatsData;Ljava/lang/String;I)I",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void dbzlegacy$preservePricelessCost(
            StatsData data,
            String skillName,
            int level,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (skillName == null || skillName.isEmpty() || level < 0) {
            return;
        }
        try {
            int raw = SkillTpCostHelper.rawConfiguredCost(data, skillName, level);
            if (raw < 0) {
                cir.setReturnValue(-1);
            }
        } catch (Throwable ignored) {
            // Never break skill purchase flow on unexpected config shapes.
        }
    }
}
