package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.skills.Skills;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/** Shared DMZ skill sync / level helpers for progression modules. */
public final class DmzSkillUtil {
    private DmzSkillUtil() {}

    public static Skills skills(ServerPlayer player) {
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return null;
        }
        try {
            return data.getSkills();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] progression skill sync failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
        }
    }

    /**
     * Repair aliases + refresh max levels from skills.json before Skill Check reads.
     */
    public static void prepareForRead(Skills skills) {
        if (skills == null) {
            return;
        }
        try {
            skills.repairSkillNames();
        } catch (Throwable ignored) {
        }
        refreshMaxes(skills);
    }

    public static int level(Skills skills, String id) {
        if (skills == null || id == null || id.isBlank()) {
            return 0;
        }
        prepareForRead(skills);
        int direct = safeGetSkillLevel(skills, id);
        if (direct > 0) {
            return direct;
        }
        try {
            Skill skill = skills.getSkill(id);
            if (skill != null) {
                int lv = Math.max(0, skill.getLevel());
                if (lv > 0) {
                    return lv;
                }
            }
        } catch (Throwable ignored) {
        }
        String want = normalizeSkillKey(id);
        try {
            var all = skills.getAllSkills();
            if (all != null) {
                for (var entry : all.entrySet()) {
                    if (entry == null || entry.getKey() == null) {
                        continue;
                    }
                    if (!normalizeSkillKey(entry.getKey()).equals(want)) {
                        continue;
                    }
                    Skill skill = entry.getValue();
                    if (skill != null && skill.getLevel() > 0) {
                        return skill.getLevel();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static int safeGetSkillLevel(Skills skills, String id) {
        try {
            return Math.max(0, skills.getSkillLevel(id));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String normalizeSkillKey(String id) {
        if (id == null) {
            return "";
        }
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Authoritative max from {@code config/dragonminez/skills.json} cost ladder length
     * (same source DMZ {@code Skills#calculateMaxLevel} uses).
     */
    public static int configuredMaxLevel(String id) {
        if (id == null || id.isBlank()) {
            return 0;
        }
        String key = id.toLowerCase(Locale.ROOT);
        try {
            SkillsConfig cfg = ConfigManager.getSkillsConfig();
            if (cfg == null) {
                return 0;
            }
            var costs = cfg.getSkillCosts(key);
            if (costs == null || costs.getCosts() == null || costs.getCosts().isEmpty()) {
                return 0;
            }
            int max = costs.getCosts().size();
            // Match DMZ calculateMaxLevel: potentialunlock hard-capped at 30.
            if ("potentialunlock".equals(key)) {
                return Math.min(max, 30);
            }
            return Math.min(max, 50);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** True when {@code id} is a DMZ form-skill ladder (not prestige-purchasable). */
    public static boolean isFormSkill(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String key = id.toLowerCase(Locale.ROOT);
        try {
            SkillsConfig cfg = ConfigManager.getSkillsConfig();
            if (cfg == null || cfg.getFormSkills() == null) {
                return false;
            }
            for (String form : cfg.getFormSkills()) {
                if (form != null && key.equals(form.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Skill Check Natural page — Potential Unlock, Flight, Meditation, Jump, Sprint.
     * Prestige skill shop is limited to these + {@link #SKILL_CHECK_SAGA}.
     */
    public static final String[] SKILL_CHECK_NATURAL = {
            "potentialunlock", "fly", "meditation", "jump", "sprint"
    };

    /**
     * Skill Check Saga page — same ids {@code SkillUnlockService} shows under Saga.
     * Excludes ultimates / ki attacks / strike skills.
     */
    public static final String[] SKILL_CHECK_SAGA = {
            "kicontrol", "kimanipulation", "kisense",
            "defense_penetration", "healing_reduction",
            "instant_transmission", "ki_infusion", "kiboost", "kiprotection",
            "kaioken", "fusion"
    };

    /** True when {@code id} appears on Skill Check (Natural or Saga). */
    public static boolean isSkillCheckSkill(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String key = id.toLowerCase(Locale.ROOT);
        for (String s : SKILL_CHECK_NATURAL) {
            if (key.equals(s)) {
                return true;
            }
        }
        for (String s : SKILL_CHECK_SAGA) {
            if (key.equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Skill Check catalog (Natural then Saga). Prestige permanent skill shop uses this —
     * not every non-form entry in skills.json (no ultimate / ki attacks).
     */
    public static List<String> skillCheckSkillIds() {
        List<String> out = new ArrayList<>(SKILL_CHECK_NATURAL.length + SKILL_CHECK_SAGA.length);
        for (String id : SKILL_CHECK_NATURAL) {
            out.add(id);
        }
        for (String id : SKILL_CHECK_SAGA) {
            out.add(id);
        }
        return List.copyOf(out);
    }

    /**
     * All non-form skill ids from DMZ skills config (stable sorted order).
     * Used for max-level resolution / reapply of legacy purchases — not the prestige shop catalog.
     */
    public static List<String> allNonFormSkillIds() {
        LinkedHashSet<String> out = new LinkedHashSet<>(skillCheckSkillIds());
        try {
            SkillsConfig cfg = ConfigManager.getSkillsConfig();
            if (cfg != null && cfg.getSkills() != null) {
                List<String> keys = new ArrayList<>(cfg.getSkills().keySet());
                Collections.sort(keys);
                for (String raw : keys) {
                    if (raw == null || raw.isBlank()) {
                        continue;
                    }
                    String id = raw.toLowerCase(Locale.ROOT);
                    if (isFormSkill(id)) {
                        continue;
                    }
                    if (configuredMaxLevel(id) > 0) {
                        out.add(id);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return List.copyOf(out);
    }

    public static String prettySkillLabel(String id) {
        if (id == null || id.isBlank()) {
            return "Skill";
        }
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "potentialunlock" -> "Potential Unlock";
            case "fly" -> "Fly";
            case "meditation" -> "Meditation";
            case "sprint" -> "Sprint";
            case "jump" -> "Jump";
            case "kicontrol" -> "Ki Control";
            case "kimanipulation" -> "Ki Manipulation";
            case "kisense" -> "Ki Sense";
            case "defense_penetration" -> "Defense Penetration";
            case "healing_reduction" -> "Healing Reduction";
            case "instant_transmission" -> "Instant Transmission";
            case "ki_infusion" -> "Ki Infusion";
            case "kiboost" -> "Ki Boost";
            case "kiprotection" -> "Ki Protection";
            case "kaioken" -> "Kaioken";
            case "fusion" -> "Fusion";
            case "ultimate" -> "Ultimate";
            default -> {
                String[] parts = id.toLowerCase(Locale.ROOT).split("[_\\-]+");
                StringBuilder sb = new StringBuilder();
                for (String p : parts) {
                    if (p.isEmpty()) {
                        continue;
                    }
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(Character.toUpperCase(p.charAt(0)));
                    if (p.length() > 1) {
                        sb.append(p.substring(1));
                    }
                }
                yield sb.length() == 0 ? id : sb.toString();
            }
        };
    }

    /**
     * Best-known max: live skill map after refresh, else skills.json ladder, else fallback.
     * Does <b>not</b> clamp a live/config max down to {@code fallbackCap}.
     */
    public static int maxLevel(Skills skills, String id, int fallbackCap) {
        if (id == null) {
            return Math.max(0, fallbackCap);
        }
        prepareForRead(skills);
        try {
            if (skills != null) {
                int live = skills.getMaxSkillLevel(id);
                if (live > 0) {
                    return live;
                }
                int dmz = dmzCalculateMaxLevel(skills, id);
                if (dmz > 0) {
                    return dmz;
                }
            }
        } catch (Throwable ignored) {
        }
        int cfg = configuredMaxLevel(id);
        if (cfg > 0) {
            return cfg;
        }
        return Math.max(0, fallbackCap);
    }

    /** Same path DMZ {@code Skills#calculateMaxLevel} uses (private on DMZ jar). */
    private static int dmzCalculateMaxLevel(Skills skills, String id) {
        if (skills == null || id == null || id.isBlank()) {
            return 0;
        }
        try {
            var method = skills.getClass().getDeclaredMethod("calculateMaxLevel", String.class);
            method.setAccessible(true);
            Object v = method.invoke(skills, id);
            if (v instanceof Number n) {
                return Math.max(0, n.intValue());
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public static void refreshMaxes(Skills skills) {
        if (skills == null) {
            return;
        }
        try {
            skills.refreshNonFormSkillMaxLevels();
        } catch (Throwable ignored) {
        }
    }

    public static boolean setLevel(Skills skills, String id, int level) {
        if (skills == null || id == null) {
            return false;
        }
        try {
            skills.setSkillLevel(id, Math.max(0, level));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void ensureRegistered(Skills skills, String id, int max) {
        if (skills == null || id == null) {
            return;
        }
        try {
            int want = max > 0 ? max : configuredMaxLevel(id);
            if (want <= 0) {
                want = 10;
            }
            skills.registerDefaultSkill(id, want);
            skills.refreshNonFormSkillMaxLevels();
        } catch (Throwable ignored) {
        }
    }

    public static boolean isActive(Skills skills, String id) {
        if (skills == null || id == null) {
            return false;
        }
        try {
            return skills.isSkillActive(id);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
