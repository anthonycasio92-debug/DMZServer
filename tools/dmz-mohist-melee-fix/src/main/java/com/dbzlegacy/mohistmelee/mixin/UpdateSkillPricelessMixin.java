package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DmzMohistMeleeFix;
import com.dbzlegacy.mohistmelee.PricelessPurchaseGuard;
import com.dbzlegacy.mohistmelee.SkillTpCostHelper;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.skills.Skill;
import java.util.concurrent.CompletableFuture;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Block menu/packet purchase of priceless ({@code -1}) skills and forms.
 * <p>
 * Primary hook redirects {@code handle}'s {@code enqueueWork} (stable method name on Mohist)
 * and skips the original work when the configured cost is priceless. Also preserves negatives
 * in {@code computeTpCost} ({@code Math.max(0, cost)} otherwise turns Ultimate free).
 */
@Mixin(value = UpdateSkillC2S.class, priority = 2000, remap = false)
public abstract class UpdateSkillPricelessMixin {

    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);

    @Shadow
    private String skillName;

    @Shadow
    private UpdateSkillC2S.SkillAction action;

    /**
     * Wrap packet work so priceless PURCHASE/UPGRADE never reaches DMZ apply logic.
     * {@code handle} is a public stable name — unlike {@code lambda$handle$*}.
     */
    @Redirect(
            method = "handle(Ljava/util/function/Supplier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/network/NetworkEvent$Context;enqueueWork(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;"
            ),
            remap = false,
            require = 0
    )
    private CompletableFuture<Void> dbzlegacy$wrapEnqueueWork(
            NetworkEvent.Context ctx,
            Runnable original
    ) {
        return ctx.enqueueWork(() -> {
            StatsData data = null;
            try {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    data = StatsProvider.get(StatsCapability.INSTANCE, (Entity) player).orElse(null);
                }
            } catch (Throwable ignored) {
                // Fall through and run original if stats unavailable.
            }
            if (data != null && dbzlegacy$shouldBlockPriceless(data)) {
                return;
            }
            if (data != null) {
                PricelessPurchaseGuard.enter(data);
            }
            try {
                original.run();
            } finally {
                PricelessPurchaseGuard.exit();
            }
        });
    }

    private boolean dbzlegacy$shouldBlockPriceless(StatsData data) {
        if (this.action == null || this.skillName == null || this.skillName.isEmpty()) {
            return false;
        }
        if (this.action != UpdateSkillC2S.SkillAction.PURCHASE
                && this.action != UpdateSkillC2S.SkillAction.UPGRADE) {
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
            return false;
        }
    }

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
}
