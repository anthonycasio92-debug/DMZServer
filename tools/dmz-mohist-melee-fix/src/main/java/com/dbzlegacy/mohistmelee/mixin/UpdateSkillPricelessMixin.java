package com.dbzlegacy.mohistmelee.mixin;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.network.C2S.UpdateSkillC2S;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import java.util.List;
import java.util.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * DMZ {@code UpdateSkillC2S.computeTpCost} does {@code Math.max(0, configuredCost)}.
 * Configured {@code -1} ("Priceless" in the skills UI) therefore becomes {@code 0} TP and can be
 * purchased. SDU's stack-skill double-click path does the same clamp client-side for Ultimate /
 * Kaioken.
 * <p>
 * Restore negative costs after DMZ computes so purchase/upgrade handlers reject them via
 * {@code if (cost < 0) return}.
 */
@Mixin(value = UpdateSkillC2S.class, remap = false)
public abstract class UpdateSkillPricelessMixin {

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
            int raw = dbzlegacy$rawConfiguredCost(data, skillName, level);
            if (raw < 0) {
                cir.setReturnValue(-1);
            }
        } catch (Throwable ignored) {
            // Never break skill purchase flow on unexpected config shapes.
        }
    }

    private static int dbzlegacy$rawConfiguredCost(StatsData data, String skillName, int level) {
        SkillsConfig skillsConfig = ConfigManager.getSkillsConfig();
        if (skillsConfig == null) {
            return 0;
        }
        String key = skillName.toLowerCase(Locale.ROOT);
        if (skillsConfig.getFormSkills() != null && skillsConfig.getFormSkills().contains(key)) {
            Character character = data != null ? data.getCharacter() : null;
            String race = character != null ? character.getRaceName() : "";
            RaceCharacterConfig raceConfig = ConfigManager.getRaceCharacter(race);
            if (raceConfig == null) {
                return -1;
            }
            Integer[] prices = raceConfig.getFormSkillTpCosts(skillName);
            if (prices == null || level >= prices.length || prices[level] == null) {
                return -1;
            }
            return prices[level];
        }
        SkillsConfig.SkillCosts skillCosts = skillsConfig.getSkillCosts(skillName);
        if (skillCosts == null || skillCosts.getCosts() == null) {
            return -1;
        }
        List<Integer> costs = skillCosts.getCosts();
        if (level >= costs.size() || costs.get(level) == null) {
            return -1;
        }
        return costs.get(level);
    }
}
