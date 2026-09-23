package com.dbzlegacy.adaptivedifficulty.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Brigadier tab-complete helpers for LegacyMechanics command trees (Forge / Mohist). */
public final class LmCommandSuggestions {
    private LmCommandSuggestions() {}

    /** Online player names — explicit list (Mohist {@code m_5983_()} can include non-player noise). */
    public static final SuggestionProvider<CommandSourceStack> PLAYERS =
            (ctx, builder) -> SharedSuggestionProvider.m_82970_(onlinePlayerNames(ctx.getSource()), builder);

    public static List<String> onlinePlayerNames(CommandSourceStack source) {
        if (source == null) {
            return List.of();
        }
        MinecraftServer server = source.m_81377_();
        if (server == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (ServerPlayer p : server.m_6846_().m_11314_()) {
            if (p == null) {
                continue;
            }
            String name = p.m_6302_();
            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    public static List<String> filterPlayerNames(CommandSourceStack source, String partial) {
        String prefix = partial == null ? "" : partial.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String name : onlinePlayerNames(source)) {
            if (prefix.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(name);
            }
        }
        return out;
    }

    /**
     * Mohist Bukkit tab passes {@code args}; last entry is the partial token being completed.
     */
    public static boolean expectsPlayerName(String label, String[] args) {
        if (args == null || args.length == 0) {
            return false;
        }
        String cmd = label == null ? "" : label.toLowerCase(Locale.ROOT);
        int n = args.length;
        String a0 = n > 0 ? lower(args[0]) : "";
        String a1 = n > 1 ? lower(args[1]) : "";
        String a2 = n > 2 ? lower(args[2]) : "";
        String a3 = n > 3 ? lower(args[3]) : "";

        if ("lm".equals(cmd) || "legacymechanics".equals(cmd)) {
            if ("admin".equals(a0) && "clear".equals(a1) && n >= 3) {
                return true;
            }
            if ("admin".equals(a0) && "inspect".equals(a1) && n >= 3 && !"clear".equals(a2)) {
                return true;
            }
            if ("admin".equals(a0) && isCharacterAdminSub(a1) && isCooldownSub(a2) && "clear".equals(a3)
                    && n >= 5) {
                return true;
            }
            return false;
        }
        if ("difficulty".equals(cmd) || "diff".equals(cmd)) {
            if ("admin".equals(a0) && ("gui".equals(a1) || "inspect".equals(a1) || "resynclevel".equals(a1))
                    && n >= 3 && !"clear".equals(a2)) {
                return true;
            }
            if ("admin".equals(a0) && "whitelist".equals(a1) && "add".equals(a2) && n >= 4) {
                return true;
            }
            return false;
        }
        if ("padmin".equals(cmd) || "prestigeadmin".equals(cmd)) {
            if (n == 2 && (isPrestigeAdminPlayerSub(a0) || isPrestigeAdminPointsShorthand(a0))) {
                return true;
            }
            return false;
        }
        if ("prestige".equals(cmd) && "admin".equals(a0)) {
            if (n == 3 && (isPrestigeAdminPlayerSub(a1) || isPrestigeAdminPointsShorthand(a1))) {
                return true;
            }
            return false;
        }
        if ("rival".equals(cmd) || "spar".equals(cmd)) {
            if (n == 2 && ("stats".equals(a0) || "spectate".equals(a0) || "declare".equals(a0)
                    || "accept".equals(a0) || "decline".equals(a0) || "remove".equals(a0))) {
                return true;
            }
            if ("rival".equals(cmd) && "challenge".equals(a0) && "send".equals(a1) && n >= 3) {
                return true;
            }
            if ("spar".equals(cmd) && "apprentice".equals(a0) && n >= 2) {
                return true;
            }
            if ("spar".equals(cmd) && "admin".equals(a0) && "mentor".equals(a1) && "resetcd".equals(a2) && n >= 4) {
                return true;
            }
            return false;
        }
        return false;
    }

    private static boolean isPrestigeAdminPlayerSub(String sub) {
        return switch (sub) {
            case "info", "sync", "skills", "invested", "skill", "invest", "held", "completed", "points",
                    "breakthroughs", "breakthrough", "cap", "fabled", "tier", "tiers", "difficulty" -> true;
            default -> false;
        };
    }

    private static boolean isPrestigeAdminPointsShorthand(String sub) {
        return switch (sub) {
            case "addpoints", "givepoints", "grantpoints", "setpoints", "removepoints", "takepoints" -> true;
            default -> false;
        };
    }

    private static boolean isCharacterAdminSub(String raw) {
        return switch (lower(raw)) {
            case "character", "char", "charservices", "characterservices" -> true;
            default -> false;
        };
    }

    private static boolean isCooldownSub(String raw) {
        return "cooldown".equals(lower(raw)) || "cool".equals(lower(raw));
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    public static final SuggestionProvider<CommandSourceStack> LM_OPEN_SYSTEMS = literals(
            "hub", "main", "difficulty", "diff", "rival", "spar", "sparring", "prestige",
            "character", "char", "progression", "prog", "skills", "skillcheck", "android_remove"
    );

    public static final SuggestionProvider<CommandSourceStack> LM_INSPECT_SYSTEMS = literals(
            "hub", "clear", "difficulty", "rival", "spar", "skillcheck", "character",
            "prestige", "progression", "skills"
    );

    public static final SuggestionProvider<CommandSourceStack> LM_CLEAR_SCOPES = literals(
            "all", "rival", "spar", "difficulty", "progression"
    );

    public static final SuggestionProvider<CommandSourceStack> LM_CHARACTER_COOLDOWN_KINDS = literals(
            "all", "race", "class", "reskin"
    );

    public static final SuggestionProvider<CommandSourceStack> SYSLOG_MODES = literals(
            "on", "off", "status", "flush", "toggle"
    );

    public static final SuggestionProvider<CommandSourceStack> TOGGLE_MODES = literals(
            "on", "off", "toggle", "status", "enable", "disable"
    );

    public static final SuggestionProvider<CommandSourceStack> VANILLA_DIFFICULTY = literals(
            "peaceful", "easy", "normal", "hard"
    );

    public static final SuggestionProvider<CommandSourceStack> RIVAL_TOP_CATEGORIES = literals(
            "rp", "wins", "win", "streak", "beststreak", "damage", "dmg", "combo", "hit", "hits",
            "battles", "battle", "challenges"
    );

    public static final SuggestionProvider<CommandSourceStack> SPAR_TOP_CATEGORIES = literals(
            "tp", "rp", "wins", "streak", "damage", "sessions", "dojo"
    );

    public static final SuggestionProvider<CommandSourceStack> PRESTIGE_ADJUST_MODES = literals(
            "set", "add", "remove"
    );

    public static final SuggestionProvider<CommandSourceStack> PRESTIGE_FABLED_MODES = literals(
            "set", "add", "remove", "take"
    );

    public static final SuggestionProvider<CommandSourceStack> PRESTIGE_TIER_MODES = literals(
            "set", "add", "remove", "give", "clear"
    );

    public static final SuggestionProvider<CommandSourceStack> PRESTIGE_TIER_IDS = literals(
            "0", "1", "2", "3", "4", "5", "6", "7", "all"
    );

    public static final SuggestionProvider<CommandSourceStack> DIFFICULTY_ADMIN_SET_KEYS = literals(
            "enabled", "whitelistEnabled", "staffFreeAncientCoinCosts", "balanceTelemetryEnabled",
            "enableSystemTelemetry", "prestigeMultiplier", "levelMultiplier", "rewardScaling"
    );

    public static SuggestionProvider<CommandSourceStack> literals(String... values) {
        List<String> list = Arrays.asList(values);
        return (ctx, builder) -> SharedSuggestionProvider.m_82970_(list, builder);
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> word(
            String name, SuggestionProvider<CommandSourceStack> suggestions) {
        RequiredArgumentBuilder<CommandSourceStack, String> arg =
                Commands.m_82129_(name, StringArgumentType.word());
        if (suggestions != null) {
            arg.suggests(suggestions);
        }
        return arg;
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> playerWord(String name) {
        return word(name, PLAYERS);
    }

    public static RequiredArgumentBuilder<CommandSourceStack, String> playerString(String name) {
        return Commands.m_82129_(name, StringArgumentType.string()).suggests(PLAYERS);
    }
}
