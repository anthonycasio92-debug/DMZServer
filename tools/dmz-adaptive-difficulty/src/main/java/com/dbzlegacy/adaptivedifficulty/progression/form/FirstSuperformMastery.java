package com.dbzlegacy.adaptivedifficulty.progression.form;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.form.FirstSuperformChoice.Candidate;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.FormConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.extras.FormMasteries;
import com.dragonminez.common.stats.skills.Skills;
import com.dragonminez.common.util.TransformationsHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Quest reward for the Frieza Saga 16 branch: mastery on the player's current
 * race's first superform, or the legendary variant when that is the race's
 * first form (including Legendary Super Saiyan for a mutant).
 *
 * <p>Race resolution matches {@code TransformationReward.giveReward}:
 * {@code StatsProvider}/{@code StatsCapability} via {@code DmzProgression.stats},
 * then {@code Character.getRaceName()}. Form groups come from the live race
 * config ({@code ConfigManager.getAllFormsForRace}), the same folders
 * {@code DmzContentDiscovery.discoverRaceIds} scans.
 */
public final class FirstSuperformMastery {
    private FirstSuperformMastery() {}

    public static final class Outcome {
        public final boolean ok;
        public final String message;

        Outcome(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }

    public static Outcome grant(ServerPlayer player, double amount) {
        if (player == null) {
            return fail("Run that while logged in as a player, or name one.");
        }
        if (!(amount > 0.0) || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return fail("Mastery amount has to be above 0.");
        }
        StatsData data;
        try {
            data = DmzProgression.stats(player);
        } catch (Throwable t) {
            data = null;
        }
        if (data == null) {
            return fail("Could not read that player's race.");
        }
        Character character;
        try {
            character = data.getCharacter();
        } catch (Throwable t) {
            character = null;
        }
        if (character == null) {
            return fail("Could not read that player's race.");
        }
        String race;
        try {
            race = character.getRaceName();
        } catch (Throwable t) {
            race = null;
        }
        if (race == null || race.isBlank()) {
            return fail("Could not read that player's race.");
        }
        boolean mutant = hasMutant(data);
        String legendaryGroup = legendaryGroupName();
        List<Candidate> configured = configuredForms(race);
        Candidate dmz = fromHelper(data, race, false);
        Candidate chosen = choose(configured, dmz, mutant, legendaryGroup);
        boolean stack = false;
        if (chosen == null) {
            chosen = fromHelper(data, race, true);
            stack = chosen != null;
        }
        if (chosen == null) {
            return fail("No superform or legendary form is set for race " + race + ".");
        }
        FormMasteries masteries = stack ? character.getStackFormMasteries() : character.getFormMasteries();
        if (masteries == null) {
            return fail("Could not read form mastery for " + race + ".");
        }
        double cap = masteryCap(race, chosen, stack);
        ensureSkill(data, chosen, stack);
        double current = masteries.getMastery(chosen.group, chosen.form);
        double target = Math.min(cap, Math.max(current, amount));
        boolean raised = target > current + 1.0E-9;
        if (raised) {
            masteries.setMastery(chosen.group, chosen.form, target, cap);
        }
        DmzSkillUtil.sync(player);
        String who = player.m_6302_();
        String label = chosen.label();
        if (!raised) {
            return new Outcome(true,
                    "§e" + who + " already has §f" + format(current)
                            + "§e mastery on §f" + label + "§e.");
        }
        return new Outcome(true,
                "§aGranted mastery on §f" + label + "§a (" + race + ") for §f" + who
                        + "§a. Now §f" + format(target) + "§a.");
    }

    /**
     * Mutant legendary variant wins. Otherwise DMZ
     * {@code TransformationsHelper.getFirstAvailableForm} wins when it is a
     * real superform or legendary form. Oozaru and other ladders fall through
     * to the race config.
     */
    static Candidate choose(List<Candidate> configured, Candidate dmz, boolean mutant, String legendaryGroup) {
        Candidate fromConfig = FirstSuperformChoice.select(configured, mutant, legendaryGroup);
        if (mutant && fromConfig != null && FirstSuperformChoice.isLegendary(fromConfig)) {
            return fromConfig;
        }
        if (dmz != null && FirstSuperformChoice.isQuestForm(dmz)) {
            return dmz;
        }
        return fromConfig;
    }

    private static Candidate fromHelper(StatsData data, String race, boolean stack) {
        try {
            String group = stack
                    ? TransformationsHelper.getGroupWithFirstAvailableStackForm(data)
                    : TransformationsHelper.getGroupWithFirstAvailableForm(data);
            String form = stack
                    ? TransformationsHelper.getFirstAvailableStackForm(data)
                    : TransformationsHelper.getFirstAvailableForm(data);
            if (group == null || group.isBlank() || form == null || form.isBlank()) {
                return null;
            }
            FormConfig config = stack
                    ? ConfigManager.getStackFormGroup(group)
                    : ConfigManager.getFormGroup(race, group);
            String type = config == null ? "" : config.getFormType();
            int level = 0;
            if (config != null) {
                FormConfig.FormData formData = config.getForm(form);
                if (formData != null && formData.getUnlockOnSkillLevel() != null) {
                    level = formData.getUnlockOnSkillLevel();
                }
            }
            return new Candidate(group, form, type == null ? "" : type, level, stack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<Candidate> configuredForms(String race) {
        List<Candidate> out = new ArrayList<>();
        Map<String, FormConfig> groups = formsForRace(race);
        if (groups == null) {
            return out;
        }
        for (Map.Entry<String, FormConfig> entry : groups.entrySet()) {
            FormConfig group = entry.getValue();
            if (group == null || group.getForms() == null) {
                continue;
            }
            String groupName = group.getGroupName();
            if (groupName == null || groupName.isBlank()) {
                groupName = entry.getKey();
            }
            String type = group.getFormType() == null ? "" : group.getFormType();
            for (Map.Entry<String, FormConfig.FormData> formEntry : group.getForms().entrySet()) {
                FormConfig.FormData form = formEntry.getValue();
                String formName = form != null && form.getName() != null && !form.getName().isBlank()
                        ? form.getName()
                        : formEntry.getKey();
                if (formName == null || formName.isBlank() || groupName == null || groupName.isBlank()) {
                    continue;
                }
                int level = 0;
                if (form != null && form.getUnlockOnSkillLevel() != null) {
                    level = form.getUnlockOnSkillLevel();
                }
                out.add(new Candidate(groupName, formName, type, level, false));
            }
        }
        return out;
    }

    private static Map<String, FormConfig> formsForRace(String race) {
        try {
            Map<String, FormConfig> groups = ConfigManager.getAllFormsForRace(race);
            if (groups != null && !groups.isEmpty()) {
                return groups;
            }
            String lower = race.toLowerCase(Locale.ROOT);
            if (!lower.equals(race)) {
                return ConfigManager.getAllFormsForRace(lower);
            }
            return groups;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void ensureSkill(StatsData data, Candidate chosen, boolean stack) {
        try {
            String skill = skillName(chosen, stack);
            if (skill == null || skill.isBlank()) {
                return;
            }
            Skills skills = data.getSkills();
            if (skills == null) {
                return;
            }
            int need = Math.max(0, chosen.unlockLevel);
            boolean missing = !skills.hasSkill(skill) || skills.getSkillLevel(skill) < need;
            if (missing) {
                skills.setSkillLevel(skill, need);
            }
        } catch (Throwable ignored) {
        }
    }

    /** Same skill id {@code TransformationReward.grantFormSkill} writes. */
    private static String skillName(Candidate chosen, boolean stack) {
        if (stack) {
            return chosen.formType == null ? "" : chosen.formType.toLowerCase(Locale.ROOT);
        }
        try {
            return TransformationsHelper.getSkillNameForType(chosen.formType);
        } catch (Throwable ignored) {
            return chosen.formType;
        }
    }

    private static double masteryCap(String race, Candidate chosen, boolean stack) {
        try {
            FormConfig.FormData form = stack
                    ? ConfigManager.getStackForm(chosen.group, chosen.form)
                    : ConfigManager.getForm(race, chosen.group, chosen.form);
            if (form != null && form.getMaxMastery() != null && form.getMaxMastery() > 0.0) {
                return form.getMaxMastery();
            }
        } catch (Throwable ignored) {
        }
        return Double.MAX_VALUE;
    }

    private static boolean hasMutant(StatsData data) {
        try {
            return data.getEffects() != null && data.getEffects().hasEffect("mutant");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String legendaryGroupName() {
        try {
            if (ConfigManager.getServerConfig() != null
                    && ConfigManager.getServerConfig().getMutant() != null) {
                String name = ConfigManager.getServerConfig().getMutant().getLegendaryGroupName();
                if (name != null && !name.isBlank()) {
                    return name;
                }
            }
        } catch (Throwable ignored) {
        }
        return "legendaryforms";
    }

    private static Outcome fail(String line) {
        return new Outcome(false, "§c" + line);
    }

    private static String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-6 && !Double.isInfinite(value)) {
            return Long.toString((long) Math.rint(value));
        }
        return Double.toString(value);
    }
}
