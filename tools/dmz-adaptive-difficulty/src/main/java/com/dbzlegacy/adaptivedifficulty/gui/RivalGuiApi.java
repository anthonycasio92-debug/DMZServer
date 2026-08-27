package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.rival.RivalChallengeManager;
import com.dbzlegacy.adaptivedifficulty.rival.RivalConstants;
import com.dbzlegacy.adaptivedifficulty.rival.RivalInstinct;
import com.dbzlegacy.adaptivedifficulty.rival.RivalPlayerRecord;
import com.dbzlegacy.adaptivedifficulty.rival.RivalProgression;
import com.dbzlegacy.adaptivedifficulty.rival.RivalStore;
import com.dbzlegacy.adaptivedifficulty.rival.RivalSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Public static API for Bukkit companion reflection ({@code LegacyMechanicsGUI}).
 * Status maps, lore lines, and {@code /rival do} action dispatch without reopening menus —
 * the companion plugin owns inventory reopen.
 */
public final class RivalGuiApi {
    private RivalGuiApi() {}

    public static Map<String, String> placeholders(ServerPlayer player) {
        Map<String, String> out = new HashMap<>();
        out.put("bridge_ok", "false");
        out.put("system_enabled", "false");
        if (player == null) {
            return out;
        }
        boolean enabled = DifficultyConfig.get().enableRivalSystem;
        out.put("bridge_ok", "true");
        out.put("system_enabled", enabled ? "true" : "false");
        if (!enabled) {
            return out;
        }
        RivalPlayerRecord me = RivalStore.get().ensurePlayer(player);
        RivalConstants.RpTier tier = RivalConstants.tierFor(me.totalRp);
        out.put("rp", String.valueOf((int) me.totalRp));
        out.put("tier", tier.name());
        out.put("tier_color", String.valueOf(tier.color()));
        out.put("mutual", String.valueOf(me.countMutual()));
        out.put("mutual_max", String.valueOf(RivalConstants.MAX_MUTUAL_RIVALS));
        out.put("wins", String.valueOf(me.officialWins));
        out.put("losses", String.valueOf(me.officialLosses));
        out.put("draws", String.valueOf(me.officialDraws));
        out.put("tpMsg", me.tpMessages ? "true" : "false");
        out.put("tp_msg", me.tpMessages ? "true" : "false");
        out.put("instinct", RivalInstinct.isEnabled(player) ? "true" : "false");
        out.put("instinct_feature", DifficultyConfig.get().rivalInstinct ? "true" : "false");
        out.put("challengeActive",
                RivalChallengeManager.get().isInChallenge(player.m_20148_()) ? "true" : "false");
        out.put("challenge_active", out.get("challengeActive"));
        out.put("name", me.name == null ? "" : me.name);
        return out;
    }

    public static List<String> listLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalSystem.listLines(player);
    }

    public static List<String> statsLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalSystem.statsLines(player);
    }

    public static List<String> topLines(ServerPlayer player) {
        if (!DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalSystem.topLines(10);
    }

    public static List<String> challengeLines(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add("§8── §cChallenge §8──");
        lines.add("§7Send: §e/rival challenge send <player> [min]");
        if (player != null && RivalChallengeManager.get().isInChallenge(player.m_20148_())) {
            lines.add("§eChallenge active");
        } else {
            lines.add("§7No active challenge.");
        }
        lines.add("§8Use Accept / Decline / Cancel below");
        return lines;
    }

    public static List<String> seasonLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().seasonLines(player);
    }

    public static List<String> questLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().questLines(player);
    }

    public static List<String> achievementLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().achievementLines(player);
    }

    public static List<String> hofLines(ServerPlayer player) {
        if (!DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().hofLines();
    }

    public static List<String> journalLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().journalLines(player);
    }

    public static List<String> titleLines(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRivalSystem) {
            return List.of("§cRival system is disabled.");
        }
        return RivalProgression.get().titleLines(player);
    }

    /** Lore lines for a GUI page (main status is placeholders — use page-specific lists). */
    public static List<String> linesForPage(ServerPlayer player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        return switch (p) {
            case "list" -> listLines(player);
            case "stats", "statistics" -> statsLines(player);
            case "challenge", "challenges" -> challengeLines(player);
            case "top", "leaderboard" -> topLines(player);
            case "season" -> seasonLines(player);
            case "quests", "quest" -> questLines(player);
            case "achievements", "achs", "ach" -> achievementLines(player);
            case "hof", "hall" -> hofLines(player);
            case "journal" -> journalLines(player);
            case "title", "titles" -> titleLines(player);
            case "help" -> List.of(
                    "§6§l/rival §8— Rival System",
                    "§e/rival <player> §7silent rival",
                    "§e/rival declare|accept|decline|remove <player>",
                    "§e/rival challenge send <player> [minutes]",
                    "§e/rival spectate [player]|stop",
                    "§e/rival season|quests|achievements|hof|journal|title"
            );
            default -> {
                Map<String, String> ph = placeholders(player);
                List<String> lore = new ArrayList<>();
                lore.add("§7RP §f" + ph.getOrDefault("rp", "0")
                        + " §8(§" + ph.getOrDefault("tier_color", "7")
                        + ph.getOrDefault("tier", "?") + "§8)");
                lore.add("§7Mutual §f" + ph.getOrDefault("mutual", "0")
                        + "§8/§f" + ph.getOrDefault("mutual_max",
                        String.valueOf(RivalConstants.MAX_MUTUAL_RIVALS)));
                lore.add("§7Record §a" + ph.getOrDefault("wins", "0")
                        + "§7/§c" + ph.getOrDefault("losses", "0")
                        + "§7/§e" + ph.getOrDefault("draws", "0"));
                lore.add("§7TP msg §f"
                        + ("true".equalsIgnoreCase(ph.get("tpMsg")) ? "ON" : "OFF"));
                if ("true".equalsIgnoreCase(ph.get("challengeActive"))) {
                    lore.add("§eChallenge active");
                }
                yield lore;
            }
        };
    }

    /**
     * Dispatch {@code /rival do} actions. Does not reopen GUI — caller reopens.
     *
     * @param action e.g. {@code page}, {@code tpmsg}, {@code instinct}, {@code challenge}
     * @param arg    e.g. page name, {@code toggle}, {@code accept}/{@code decline}/{@code cancel}
     * @param page   return page (unused for page action; used by caller to reopen)
     */
    public static String handleDo(ServerPlayer player, String action, String arg, String page) {
        if (player == null) {
            return "§cPlayers only.";
        }
        if (!DifficultyConfig.get().enableRivalSystem) {
            return "§cRival system is disabled.";
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        String a = arg == null ? "" : arg.trim();
        if ("page".equals(act) || "refresh".equals(act)) {
            return "";
        }
        if ("tpmsg".equals(act) || "tp_msg".equals(act)) {
            if ("toggle".equalsIgnoreCase(a) || a.isBlank()) {
                RivalPlayerRecord me = RivalStore.get().ensurePlayer(player);
                boolean next = me == null || !me.tpMessages;
                return RivalSystem.setTpMsg(player, next);
            }
            if ("on".equalsIgnoreCase(a) || "true".equalsIgnoreCase(a)) {
                return RivalSystem.setTpMsg(player, true);
            }
            if ("off".equalsIgnoreCase(a) || "false".equalsIgnoreCase(a)) {
                return RivalSystem.setTpMsg(player, false);
            }
            return "§cUsage: rival do tpmsg toggle";
        }
        if ("instinct".equals(act)) {
            if (!DifficultyConfig.get().rivalInstinct) {
                return "§cRival Instinct is disabled.";
            }
            boolean on = RivalInstinct.toggle(player);
            return "§aRival Instinct §f" + (on ? "ON" : "OFF");
        }
        if ("challenge".equals(act)) {
            String sub = a.toLowerCase(Locale.ROOT);
            return switch (sub) {
                case "accept" -> RivalChallengeManager.get().acceptChallenge(player);
                case "decline", "deny" -> RivalChallengeManager.get().declineChallenge(player);
                case "cancel" -> RivalChallengeManager.get().cancelChallenge(player);
                default -> "§cUsage: rival do challenge accept|decline|cancel";
            };
        }
        return "§cUnknown rival action: " + act;
    }
}
