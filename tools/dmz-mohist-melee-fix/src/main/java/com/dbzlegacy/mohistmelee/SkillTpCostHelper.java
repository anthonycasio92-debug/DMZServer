package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import java.util.List;
import java.util.Locale;

/**
 * Reads the raw configured TP cost for a skill level (no {@code Math.max(0, …)} clamp).
 * Configured {@code -1} is DMZ “Priceless” and must stay unbuyable.
 */
public final class SkillTpCostHelper {
    private SkillTpCostHelper() {}

    /**
     * @param levelIndex 0-based index into the skill’s costs/prices array (same as DMZ
     *                   {@code computeTpCost} / form initial buy)
     * @return configured cost, or {@code -1} if missing / unknown
     */
    public static int rawConfiguredCost(StatsData data, String skillName, int levelIndex) {
        if (skillName == null || skillName.isEmpty() || levelIndex < 0) {
            return -1;
        }
        SkillsConfig skillsConfig = ConfigManager.getSkillsConfig();
        if (skillsConfig == null) {
            return -1;
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
            if (prices == null || levelIndex >= prices.length || prices[levelIndex] == null) {
                return -1;
            }
            return prices[levelIndex];
        }
        SkillsConfig.SkillCosts skillCosts = skillsConfig.getSkillCosts(skillName);
        if (skillCosts == null || skillCosts.getCosts() == null) {
            return -1;
        }
        List<Integer> costs = skillCosts.getCosts();
        if (levelIndex >= costs.size() || costs.get(levelIndex) == null) {
            return -1;
        }
        return costs.get(levelIndex);
    }

    public static boolean isPriceless(StatsData data, String skillName, int levelIndex) {
        return rawConfiguredCost(data, skillName, levelIndex) < 0;
    }
}
