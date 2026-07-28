package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DmzMohistMeleeFix;
import com.dbzlegacy.mohistmelee.SkillTpCostHelper;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skill;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hard-cancel {@code PURCHASE}/{@code UPGRADE} when the configured cost for that level is
 * priceless ({@code < 0}). Covers the form-skill level-0→1 upgrade path even if
 * {@link UpdateSkillPricelessMixin} fails to rewrite {@code computeTpCost}.
 */
@Mixin(value = UpdateSkillC2S.class, priority = 2000, remap = false)
public abstract class UpdateSkillBlockPricelessPurchaseMixin {

    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);

    @Shadow
    private String skillName;

    @Shadow
    private UpdateSkillC2S.SkillAction action;

    @Inject(
            method = "lambda$handle$0(Lnet/minecraft/server/level/ServerPlayer;Lcom/dragonminez/common/stats/StatsData;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void dbzlegacy$blockPricelessPurchase(
            ServerPlayer player,
            StatsData data,
            CallbackInfo ci
    ) {
        if (this.action == null || this.skillName == null || this.skillName.isEmpty() || data == null) {
            return;
        }
        if (this.action != UpdateSkillC2S.SkillAction.PURCHASE
                && this.action != UpdateSkillC2S.SkillAction.UPGRADE) {
            return;
        }
        try {
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
        } catch (Throwable ignored) {
            // Never break skill packet handling.
        }
    }
}
