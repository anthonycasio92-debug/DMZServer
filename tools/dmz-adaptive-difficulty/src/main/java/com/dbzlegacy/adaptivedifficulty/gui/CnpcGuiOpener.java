package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import java.util.Locale;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Open player-facing LegacyMechanics GUIs from CustomNPC right-click.
 * <p>
 * No permission nodes required — the NPC itself is the access gate for normal players.
 * Match scoreboard tags ({@code lm_rival}, {@code lm_spar}, …) or <b>strict</b> display-name
 * needles. Soft substrings like {@code contains("rival")} are intentionally avoided — they
 * steal clicks from scripted Skill Check NPCs (and false-positives like "Arrival").
 * <p>
 * Tag matches should cancel the interact. Name matches also cancel when CNPC
 * scripts are not used (LegacyMechanics replaces those scripts entirely).
 * Skill Check has its own opener ({@link SkillCheckService#tryOpenFromNpc}).
 */
public final class CnpcGuiOpener {
    private CnpcGuiOpener() {}

    /**
     * Tag-based open — cancel the interact.
     *
     * @return true if a GUI was opened from a scoreboard tag
     */
    public static boolean tryOpenFromTags(ServerPlayer player, Entity npc) {
        if (!eligible(player, npc)) {
            return false;
        }
        String system = fromTags(npc);
        if (system == null) {
            return false;
        }
        return openSystem(player, system);
    }

    /**
     * Strict name-based open — cancel the interact (no CNPC scripts required).
     *
     * @return true if a GUI was opened from the display name
     */
    public static boolean tryOpenFromName(ServerPlayer player, Entity npc) {
        if (!eligible(player, npc)) {
            return false;
        }
        // Tags already handled — avoid double-open.
        if (fromTags(npc) != null) {
            return false;
        }
        String system = fromName(npc);
        if (system == null) {
            return false;
        }
        return openSystem(player, system);
    }

    /**
     * @return true if a GUI was opened (caller should cancel the interact).
     *         Tries tags first, then strict display-name needles.
     */
    public static boolean tryOpenFromNpc(ServerPlayer player, Entity npc) {
        return tryOpenFromTags(player, npc) || tryOpenFromName(player, npc);
    }

    private static boolean eligible(ServerPlayer player, Entity npc) {
        if (player == null || npc == null) {
            return false;
        }
        if (!(npc instanceof LivingEntity)) {
            return false;
        }
        if (!SkillCheckService.looksLikeCustomNpc(npc)) {
            return false;
        }
        // Never steal Skill Check NPCs (tag / name / scripted Skill Check markers).
        return !SkillCheckService.isSkillCheckNpc(npc);
    }

    private static boolean openSystem(ServerPlayer player, String system) {
        return switch (system) {
            case "hub" -> {
                MechanicsMenu.open(player, "main");
                yield true;
            }
            case "rival" -> {
                RivalMenu.open(player, "main");
                yield true;
            }
            case "spar" -> {
                SparMenu.open(player, "main");
                yield true;
            }
            case "difficulty" -> {
                DifficultyMenu.open(player, "main");
                yield true;
            }
            case "prestige" -> {
                if (!DifficultyConfig.get().enablePrestigeSystem) {
                    yield false;
                }
                PrestigeMenu.open(player, "main");
                yield true;
            }
            default -> false;
        };
    }

    private static String fromTags(Entity npc) {
        for (String tag : collectTags(npc)) {
            String t = tag.toLowerCase(Locale.ROOT).trim();
            // Skill Check tags are handled elsewhere — never map them to Rival/etc.
            if (t.contains("lm_skillcheck") || t.equals("skillcheck") || t.equals("skill_check")) {
                continue;
            }
            if (tagEquals(t, "lm_rival") || t.equals("rival")) {
                return "rival";
            }
            if (tagEquals(t, "lm_spar") || tagEquals(t, "lm_sparring") || t.equals("spar")) {
                return "spar";
            }
            if (tagEquals(t, "lm_hub") || tagEquals(t, "lm_legacy") || t.equals("lm")) {
                return "hub";
            }
            if (tagEquals(t, "lm_diff") || tagEquals(t, "lm_difficulty") || t.equals("difficulty")) {
                return "difficulty";
            }
            if (tagEquals(t, "lm_prestige") || t.equals("prestige")) {
                return "prestige";
            }
        }
        return null;
    }

    /** Exact tag or tag with a suffix separator ({@code lm_rival_1}). */
    private static boolean tagEquals(String tag, String key) {
        if (tag == null || key == null) {
            return false;
        }
        return tag.equals(key) || tag.startsWith(key + "_") || tag.startsWith(key + "-");
    }

    private static String fromName(Entity npc) {
        String name = displayName(npc);
        if (name == null || name.isBlank()) {
            return null;
        }
        String hay = stripFormatting(name).toLowerCase(Locale.ROOT).trim();
        // Skill Check names — leave for SkillCheckService.
        if (containsAny(hay, "skill check", "skillcheck", "skill progress")) {
            return null;
        }
        // Strict phrases / exact names only — no bare substring "rival"/"spar".
        if (hay.equals("spar") || containsAny(hay, "sparring", "spar npc", "sparring npc")) {
            return "spar";
        }
        if (hay.equals("rival") || containsAny(hay, "rival system", "rivalry", "rival npc")) {
            return "rival";
        }
        if (hay.equals("difficulty") || containsAny(hay, "adaptive difficulty", "difficulty npc")) {
            return "difficulty";
        }
        if (hay.equals("prestige") || containsAny(hay, "prestige npc")) {
            return "prestige";
        }
        if (hay.equals("lm") || containsAny(hay, "legacy mechanics", "mechanics hub", "lm hub")) {
            return "hub";
        }
        return null;
    }

    private static boolean containsAny(String hay, String... needles) {
        for (String n : needles) {
            if (hay.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> collectTags(Entity entity) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        try {
            Set<String> tags = entity.m_19880_();
            if (tags != null) {
                out.addAll(tags);
            }
        } catch (Throwable ignored) {
        }
        try {
            Object bukkit = entity.getClass().getMethod("getBukkitEntity").invoke(entity);
            if (bukkit != null) {
                Object tags = bukkit.getClass().getMethod("getScoreboardTags").invoke(bukkit);
                if (tags instanceof Iterable<?> it) {
                    for (Object tag : it) {
                        if (tag != null) {
                            out.add(String.valueOf(tag));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static String displayName(Entity entity) {
        try {
            if (entity.m_8077_()) {
                Component custom = entity.m_7770_();
                if (custom != null) {
                    String s = custom.getString();
                    if (s != null && !s.isBlank()) {
                        return s;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            return entity.m_7755_().getString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String stripFormatting(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("§.", "").replaceAll("&[0-9a-fk-or]", "");
    }
}
