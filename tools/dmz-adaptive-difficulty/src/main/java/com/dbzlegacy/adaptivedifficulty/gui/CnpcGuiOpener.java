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
 * Match scoreboard tags ({@code lm_rival}, {@code lm_spar}, …) or display-name needles.
 * Skill Check has its own opener ({@link SkillCheckService#tryOpenFromNpc}).
 */
public final class CnpcGuiOpener {
    private CnpcGuiOpener() {}

    /**
     * @return true if a GUI was opened (caller should cancel the interact)
     */
    public static boolean tryOpenFromNpc(ServerPlayer player, Entity npc) {
        if (player == null || npc == null) {
            return false;
        }
        if (!(npc instanceof LivingEntity)) {
            return false;
        }
        if (!SkillCheckService.looksLikeCustomNpc(npc)) {
            return false;
        }
        String system = resolveSystem(npc);
        if (system == null) {
            return false;
        }
        return openSystem(player, system);
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

    /** Prefer tags, then display-name needles. */
    private static String resolveSystem(Entity npc) {
        String fromTag = fromTags(npc);
        if (fromTag != null) {
            return fromTag;
        }
        return fromName(npc);
    }

    private static String fromTags(Entity npc) {
        for (String tag : collectTags(npc)) {
            String t = tag.toLowerCase(Locale.ROOT);
            if (t.contains("lm_rival") || t.equals("rival")) {
                return "rival";
            }
            if (t.contains("lm_spar") || t.contains("lm_sparring") || t.equals("spar")) {
                return "spar";
            }
            if (t.contains("lm_hub") || t.contains("lm_legacy") || t.equals("lm")) {
                return "hub";
            }
            if (t.contains("lm_diff") || t.contains("lm_difficulty") || t.equals("difficulty")) {
                return "difficulty";
            }
            if (t.contains("lm_prestige") || t.equals("prestige")) {
                return "prestige";
            }
        }
        return null;
    }

    private static String fromName(Entity npc) {
        String name = displayName(npc);
        if (name == null || name.isBlank()) {
            return null;
        }
        String hay = stripFormatting(name).toLowerCase(Locale.ROOT);
        // More specific first.
        if (containsAny(hay, "sparring", "spar npc", "sparring npc") || hay.equals("spar")) {
            return "spar";
        }
        if (containsAny(hay, "rival system", "rivalry", "rival npc") || hay.equals("rival")) {
            return "rival";
        }
        if (containsAny(hay, "adaptive difficulty", "difficulty npc") || hay.equals("difficulty")) {
            return "difficulty";
        }
        if (containsAny(hay, "prestige npc") || hay.equals("prestige")) {
            return "prestige";
        }
        if (containsAny(hay, "legacy mechanics", "mechanics hub", "lm hub") || hay.equals("lm")) {
            return "hub";
        }
        // Soft name matches (substring).
        if (hay.contains("spar")) {
            return "spar";
        }
        if (hay.contains("rival")) {
            return "rival";
        }
        if (hay.contains("difficulty")) {
            return "difficulty";
        }
        if (hay.contains("prestige")) {
            return "prestige";
        }
        if (hay.contains("legacy mechanics") || hay.contains("mechanics hub")) {
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
