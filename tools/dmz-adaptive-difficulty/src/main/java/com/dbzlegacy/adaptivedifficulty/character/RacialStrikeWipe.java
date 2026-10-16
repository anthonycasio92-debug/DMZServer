package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.skills.Skills;
import com.dragonminez.common.stats.techniques.PredefinedTechniques;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.TechniqueType;
import com.dragonminez.common.stats.techniques.Techniques;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Removes racial strike attacks on the same race-change path as {@link RacialBonusWipeHelper}.
 * Player-bought strikes, quest ki, and custom techniques stay.
 */
public final class RacialStrikeWipe {
    private static final Map<Techniques, WeakReference<StatsData>> OWNERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Boolean> RAW_READ = new ThreadLocal<>();

    private RacialStrikeWipe() {}

    /**
     * Delete strike attacks and their skills that {@code newRace} does not grant.
     * Reads the technique bag directly so a read-time gate cannot hide the entries
     * this method is deleting.
     */
    public static void wipe(StatsData data, String oldRace, String newRace) {
        if (data == null) {
            return;
        }
        runRaw(() -> wipeRaw(data, oldRace, newRace));
    }

    /** True when skill restore must not put this strike skill back. */
    public static boolean dropSkill(String id, String newRace) {
        return dropId(id, false, newRace);
    }

    /**
     * Link a technique bag the caller already holds. Do not call {@code getTechniques}
     * from here: the owner hook sits on that getter.
     */
    public static void remember(StatsData data, Techniques techniques) {
        if (data == null || techniques == null) {
            return;
        }
        WeakReference<StatsData> existing = OWNERS.get(techniques);
        if (existing != null && existing.get() == data) {
            return;
        }
        OWNERS.put(techniques, new WeakReference<>(data));
    }

    public static boolean rawRead() {
        return Boolean.TRUE.equals(RAW_READ.get());
    }

    public static void runRaw(Runnable action) {
        if (action == null) {
            return;
        }
        boolean previous = Boolean.TRUE.equals(RAW_READ.get());
        RAW_READ.set(true);
        try {
            action.run();
        } finally {
            if (previous) {
                RAW_READ.set(true);
            } else {
                RAW_READ.remove();
            }
        }
    }

    /**
     * @return the same map when every entry still belongs, otherwise a copy without
     *     strikes the current race does not grant. An unknown owner keeps the map.
     */
    public static Map<String, TechniqueData> filterUnlocked(
            Techniques techniques, Map<String, TechniqueData> unlocked
    ) {
        if (rawRead() || techniques == null || unlocked == null || unlocked.isEmpty()) {
            return unlocked;
        }
        String race = raceOf(techniques);
        if (race == null) {
            return unlocked;
        }
        boolean hiding = false;
        Map<String, TechniqueData> kept = null;
        for (Map.Entry<String, TechniqueData> entry : unlocked.entrySet()) {
            if (entry == null) {
                continue;
            }
            if (absent(entry.getKey(), entry.getValue(), race)) {
                if (!hiding) {
                    hiding = true;
                    kept = new LinkedHashMap<>();
                    for (Map.Entry<String, TechniqueData> prior : unlocked.entrySet()) {
                        if (prior == null) {
                            continue;
                        }
                        if (sameKey(prior.getKey(), entry.getKey())) {
                            break;
                        }
                        kept.put(prior.getKey(), prior.getValue());
                    }
                }
                continue;
            }
            if (hiding) {
                kept.put(entry.getKey(), entry.getValue());
            }
        }
        return hiding ? kept : unlocked;
    }

    /** True when the selected technique should read as missing. */
    public static boolean hideSelected(Techniques techniques, TechniqueData selected) {
        if (rawRead() || techniques == null || selected == null) {
            return false;
        }
        String race = raceOf(techniques);
        if (race == null) {
            return false;
        }
        return absent(selected.getId(), selected, race);
    }

    private static void wipeRaw(StatsData data, String oldRace, String newRace) {
        int removed = 0;
        try {
            Skills skills = data.getSkills();
            if (skills != null) {
                removed += removeStrikeSkills(skills, newRace);
            }
        } catch (Throwable ignored) {
        }
        try {
            Techniques techniques = data.getTechniques();
            if (techniques != null) {
                removed += removeStrikeTechniques(techniques, newRace);
            }
        } catch (Throwable ignored) {
        }
        if (removed <= 0) {
            return;
        }
        AdaptiveDifficultyMod.LOGGER.debug(
                "[{}] RacialStrikeWipe cleared {} strike entr{} {}->{}",
                AdaptiveDifficultyMod.MOD_ID,
                removed,
                removed == 1 ? "y" : "ies",
                RacialStrikeRules.normalizeRace(oldRace),
                RacialStrikeRules.normalizeRace(newRace));
    }

    private static int removeStrikeSkills(Skills skills, String newRace) {
        Map<String, Skill> all;
        try {
            all = skills.getAllSkills();
        } catch (Throwable t) {
            return 0;
        }
        if (all == null || all.isEmpty()) {
            return 0;
        }
        List<String> drop = new ArrayList<>();
        for (Map.Entry<String, Skill> entry : all.entrySet()) {
            if (entry == null) {
                continue;
            }
            Skill skill = entry.getValue();
            String id = skill == null ? null : skill.getName();
            if (id == null || id.isBlank()) {
                id = entry.getKey();
            }
            if (dropId(id, false, newRace)) {
                drop.add(id);
            }
        }
        int removed = 0;
        for (int i = 0; i < drop.size(); i++) {
            try {
                skills.removeSkill(drop.get(i));
                removed++;
            } catch (Throwable ignored) {
            }
        }
        return removed;
    }

    private static int removeStrikeTechniques(Techniques techniques, String newRace) {
        Map<String, TechniqueData> unlocked;
        try {
            unlocked = techniques.getUnlockedTechniques();
        } catch (Throwable t) {
            return 0;
        }
        if (unlocked == null || unlocked.isEmpty()) {
            return 0;
        }
        List<String> drop = new ArrayList<>();
        for (Map.Entry<String, TechniqueData> entry : unlocked.entrySet()) {
            if (entry == null) {
                continue;
            }
            TechniqueData technique = entry.getValue();
            String id = technique == null ? null : technique.getId();
            if (id == null || id.isBlank()) {
                id = entry.getKey();
            }
            boolean strike = technique != null && technique.getType() == TechniqueType.STRIKE_ATTACK;
            if (dropId(id, strike, newRace)) {
                drop.add(id);
            }
        }
        int removed = 0;
        for (int i = 0; i < drop.size(); i++) {
            try {
                techniques.removeTechnique(drop.get(i));
                removed++;
            } catch (Throwable ignored) {
            }
        }
        return removed;
    }

    private static boolean sameKey(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private static boolean absent(String id, TechniqueData technique, String race) {
        String resolved = id;
        if ((resolved == null || resolved.isBlank()) && technique != null) {
            resolved = technique.getId();
        }
        boolean strike = technique != null && technique.getType() == TechniqueType.STRIKE_ATTACK;
        boolean known = strike || knownStrikeId(resolved);
        if (!known) {
            return false;
        }
        return RacialStrikeRules.absentOnRead(true, allowedRaces(resolved), race);
    }

    private static boolean dropId(String id, boolean forceStrike, String newRace) {
        boolean known = forceStrike || knownStrikeId(id);
        if (!known) {
            return false;
        }
        return RacialStrikeRules.dropOnRaceChange(true, allowedRaces(id), costs(id), newRace);
    }

    private static List<String> allowedRaces(String id) {
        SkillsConfig.SkillCosts costs = skillCosts(id);
        if (costs == null || costs.getAllowedRaces() == null) {
            return List.of();
        }
        return costs.getAllowedRaces();
    }

    private static List<Integer> costs(String id) {
        SkillsConfig.SkillCosts costs = skillCosts(id);
        if (costs == null || costs.getCosts() == null) {
            return List.of();
        }
        return costs.getCosts();
    }

    private static SkillsConfig.SkillCosts skillCosts(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            SkillsConfig cfg = ConfigManager.getSkillsConfig();
            if (cfg == null) {
                return null;
            }
            return cfg.getSkillCosts(id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean knownStrikeId(String id) {
        String key = strikeKey(id);
        if (key.isEmpty()) {
            return false;
        }
        try {
            Map<String, ?> registry = PredefinedTechniques.STRIKE_REGISTRY;
            if (registry != null && (registry.containsKey(key) || registry.containsKey(id))) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (listed(PredefinedTechniques.STRIKE_IDS, key)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            SkillsConfig cfg = ConfigManager.getSkillsConfig();
            if (cfg != null && listed(cfg.getStrikeSkills(), key)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean listed(List<String> ids, String key) {
        if (ids == null || key == null || key.isEmpty()) {
            return false;
        }
        for (int i = 0; i < ids.size(); i++) {
            if (key.equals(strikeKey(ids.get(i)))) {
                return true;
            }
        }
        return false;
    }

    private static String strikeKey(String id) {
        if (id == null) {
            return "";
        }
        String key = id.trim().toLowerCase(Locale.ROOT);
        if (key.startsWith("skp.")) {
            key = key.substring(4);
        }
        return key;
    }

    /** Null when this bag has no recorded owner. Callers then leave the entry in place. */
    private static String raceOf(Techniques techniques) {
        WeakReference<StatsData> ref = OWNERS.get(techniques);
        StatsData data = ref == null ? null : ref.get();
        if (data == null) {
            return null;
        }
        try {
            Character character = data.getCharacter();
            if (character == null) {
                return "";
            }
            String race = character.getRace();
            if (race == null || race.isBlank()) {
                race = character.getRaceName();
            }
            return race == null ? "" : race;
        } catch (Throwable ignored) {
            return "";
        }
    }
}
