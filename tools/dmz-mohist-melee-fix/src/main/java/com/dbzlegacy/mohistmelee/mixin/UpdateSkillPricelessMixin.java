package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DmzMohistMeleeFix;
import com.dbzlegacy.mohistmelee.SkillTpCostHelper;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.skills.Skill;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stop priceless ({@code -1}) skills/forms from being purchased.
 * <ul>
 *   <li>{@code computeTpCost} uses {@code Math.max(0, cost)} — redirect keeps negatives</li>
 *   <li>Hard-cancel in {@code lambda$handle$1} (before stats apply) for PURCHASE/UPGRADE</li>
 * </ul>
 * Stack skills like Ultimate are bought via PURCHASE + {@code computeTpCost(0)}; without the
 * redirect that becomes 0 TP and succeeds.
 */
@Mixin(value = UpdateSkillC2S.class, priority = 2000, remap = false)
public abstract class UpdateSkillPricelessMixin {

    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);

    @Shadow
    private String skillName;

    @Shadow
    private UpdateSkillC2S.SkillAction action;

    /**
     * DMZ always invokes {@code Math.max(0, configuredCost)}. Preserve priceless negatives.
     */
    @Redirect(
            method = "computeTpCost(Lcom/dragonminez/common/stats/StatsData;Ljava/lang/String;I)I",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"),
            remap = false,
            require = 0
    )
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
            remap = false,
            require = 0
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

    /**
     * Runs on the server thread before {@code lambda$handle$0}. More reliable than injecting the
     * inner consumer lambda (name/descriptor can differ across loaders).
     */
    @Inject(
            method = "lambda$handle$1(Ljava/util/function/Supplier;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void dbzlegacy$blockPricelessBeforeApply(Supplier<?> contextSupplier, CallbackInfo ci) {
        if (this.action == null || this.skillName == null || this.skillName.isEmpty()) {
            return;
        }
        if (this.action != UpdateSkillC2S.SkillAction.PURCHASE
                && this.action != UpdateSkillC2S.SkillAction.UPGRADE) {
            return;
        }
        try {
            Object rawCtx = contextSupplier.get();
            if (!(rawCtx instanceof NetworkEvent.Context ctx)) {
                return;
            }
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            StatsData data = StatsProvider.get(StatsCapability.INSTANCE, (Entity) player).orElse(null);
            if (data == null) {
                return;
            }
            int levelIndex = 0;
            if (this.action == UpdateSkillC2S.SkillAction.UPGRADE) {
                Skill skill = data.getSkills() != null ? data.getSkills().getSkill(this.skillName) : null;
                levelIndex = skill != null ? skill.getLevel() : 0;
            }
            if (SkillTpCostHelper.isPriceless(data, this.skillName, levelIndex)) {
                LOGGER.info(
                        "[{}] blocked priceless {} of skill '{}' (levelIndex={})",
                        DmzMohistMeleeFix.MOD_ID,
                        this.action.name().toLowerCase(),
                        this.skillName,
                        levelIndex
                );
                ci.cancel();
            }
        } catch (Throwable t) {
            LOGGER.warn(
                    "[{}] priceless purchase guard failed for '{}': {}",
                    DmzMohistMeleeFix.MOD_ID,
                    this.skillName,
                    t.toString()
            );
        }
    }
}
