package com.dbzlegacy.mohistmelee.mixin;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.skills.Skills;
import java.util.List;
import java.util.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DMZ {@code Skills.setSkillLevel} only recalculates {@code maxLevel} when creating a new skill
 * entry. Existing skills keep the NBT-saved max (e.g. Ultimate stayed at 1 after {@code skills.json}
 * gained a second cost slot), so {@code /dmzskill set ultimate 2} silently clamped to 1 and Beast
 * never unlocked.
 * <p>
 * Refresh max from the current costs array length before applying the new level.
 */
@Mixin(value = Skills.class, remap = false)
public abstract class SkillsSetSkillLevelMixin {

    @Shadow
    public abstract Skill getSkill(String name);

    @Inject(
            method = "setSkillLevel(Ljava/lang/String;I)V",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private void dbzlegacy$refreshMaxLevelFromConfig(String skillName, int level, CallbackInfo ci) {
        if (skillName == null || skillName.isEmpty()) {
            return;
        }
        try {
            Skill skill = this.getSkill(skillName);
            if (skill == null) {
                return;
            }
            int max = dbzlegacy$configuredMaxLevel(skillName);
            if (max > 0) {
                skill.setMaxLevel(max);
            }
        } catch (Throwable ignored) {
            // Never break admin skill commands on unexpected config shapes.
        }
    }

    private static int dbzlegacy$configuredMaxLevel(String skillName) {
        SkillsConfig skillsConfig = ConfigManager.getSkillsConfig();
        if (skillsConfig == null) {
            return 0;
        }
        SkillsConfig.SkillCosts skillCosts = skillsConfig.getSkillCosts(skillName);
        if (skillCosts == null || skillCosts.getCosts() == null) {
            return 0;
        }
        List<Integer> costs = skillCosts.getCosts();
        int size = costs.size();
        if ("potentialunlock".equalsIgnoreCase(skillName)) {
            return Math.min(size, 30);
        }
        return Math.min(size, 50);
    }
}
