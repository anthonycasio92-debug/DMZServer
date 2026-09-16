package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.skills.Skills;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Keeps DMZ ki / saga skills and shared form ladders when changing race via Character Services. */
public final class RaceChangeSkillPreserve {
    public record Entry(String id, int level, boolean active) {}

    private RaceChangeSkillPreserve() {}

    public static List<Entry> capture(Skills skills) {
        List<Entry> out = new ArrayList<>();
        if (skills == null) {
            return out;
        }
        try {
            Map<String, Skill> all = skills.getAllSkills();
            if (all == null) {
                return out;
            }
            for (Map.Entry<String, Skill> e : all.entrySet()) {
                Skill skill = e.getValue();
                if (skill == null) {
                    continue;
                }
                String id = skill.getName();
                if (id == null || id.isBlank()) {
                    id = e.getKey();
                }
                if (id == null || id.isBlank()) {
                    continue;
                }
                int level = Math.max(0, skill.getLevel());
                boolean active = skill.isActive();
                if (level <= 0 && !active) {
                    continue;
                }
                out.add(new Entry(id, level, active));
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    /**
     * Restore progress after {@code setRace}: all non-form skills + stack skills; form groups only
     * when both old and new race define that ladder (e.g. superforms on Human → Saiyan).
     */
    public static void restore(StatsData data, String oldRace, String newRace, List<Entry> captured) {
        if (data == null || captured == null || captured.isEmpty()) {
            return;
        }
        Skills skills = data.getSkills();
        if (skills == null) {
            return;
        }
        String oldId = normalizeRace(oldRace);
        String newId = normalizeRace(newRace);
        Set<String> sharedForms = sharedFormGroups(oldId, newId);

        for (Entry entry : captured) {
            applyEntry(skills, entry, sharedForms);
        }
        try {
            data.updateTransformationSkillLimits(newId);
        } catch (Throwable ignored) {
        }
        DmzSkillUtil.refreshMaxes(skills);
        for (Entry entry : captured) {
            clampEntry(skills, entry, sharedForms);
        }
    }

    private static void applyEntry(Skills skills, Entry entry, Set<String> sharedForms) {
        if (entry == null || entry.id == null || entry.id.isBlank()) {
            return;
        }
        String id = entry.id;
        String key = id.toLowerCase(Locale.ROOT);
        if (DmzSkillUtil.isFormSkill(key) && !sharedForms.contains(key)) {
            DmzSkillUtil.setLevel(skills, id, 0);
            try {
                skills.setSkillActive(id, false);
            } catch (Throwable ignored) {
            }
            return;
        }
        if (entry.level <= 0 && !entry.active) {
            return;
        }
        DmzSkillUtil.ensureRegistered(skills, id, Math.max(1, entry.level));
        int max = DmzSkillUtil.maxLevel(skills, id, entry.level);
        int level = entry.level <= 0 ? 0 : Math.min(entry.level, max);
        DmzSkillUtil.setLevel(skills, id, level);
        if (entry.active && level > 0) {
            try {
                skills.setSkillActive(id, true);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void clampEntry(Skills skills, Entry entry, Set<String> sharedForms) {
        if (entry == null || entry.id == null || entry.id.isBlank() || entry.level <= 0) {
            return;
        }
        String key = entry.id.toLowerCase(Locale.ROOT);
        if (DmzSkillUtil.isFormSkill(key) && !sharedForms.contains(key)) {
            return;
        }
        try {
            int max = skills.getMaxSkillLevel(entry.id);
            if (max > 0) {
                int live = DmzSkillUtil.level(skills, entry.id);
                if (live > max) {
                    DmzSkillUtil.setLevel(skills, entry.id, max);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Set<String> sharedFormGroups(String oldRace, String newRace) {
        Set<String> oldForms = formGroups(oldRace);
        Set<String> newForms = formGroups(newRace);
        Set<String> shared = new HashSet<>(oldForms);
        shared.retainAll(newForms);
        return shared;
    }

    private static Set<String> formGroups(String raceId) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (raceId == null || raceId.isBlank()) {
            return out;
        }
        try {
            RaceCharacterConfig cfg = ConfigManager.getRaceCharacter(raceId);
            if (cfg == null) {
                return out;
            }
            var forms = cfg.getFormSkills();
            if (forms == null) {
                return out;
            }
            for (String form : forms) {
                if (form != null && !form.isBlank()) {
                    out.add(form.toLowerCase(Locale.ROOT));
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static String normalizeRace(String raw) {
        if (raw == null) {
            return "";
        }
        String r = raw.trim().toLowerCase(Locale.ROOT);
        int colon = r.lastIndexOf(':');
        if (colon >= 0 && colon < r.length() - 1) {
            r = r.substring(colon + 1).trim();
        }
        return r;
    }
}
