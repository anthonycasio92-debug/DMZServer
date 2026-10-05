package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Bukkit {@code /progression} / {@code /prog} command tree.
 * <p>
 * <b>Players:</b> {@code /progression meditation} only — other actions via {@code /lm} GUI.
 * <b>Staff:</b> full tree (GUI · boost · android · flags · do).
 * <b>Console:</b> {@code /progression boost …} (store / Tebex) and
 * {@code /progression android <player>} / {@code /androidify <player>} (Saga).
 * <p>
 * Mohist: this plugin owns the command name, so every leaf must be handled here and
 * call into LegacyMechanics via {@link ForgeBridge} — never Forge brigadier
 * {@code forwardCommand}.
 */
public final class ProgressionCommandTree implements TabCompleter {
    private static final List<String> ROOT = List.of(
            "gui", "help", "status", "flags", "admin", "do",
            "meditation", "android", "boost",
            "skills", "tp", "race", "combat", "end", "utility"
    );
    private static final List<String> PAGES = List.of(
            "main", "skills", "tp", "race", "combat", "end", "utility",
            "admin", "flags", "help", "status", "boost_panel",
            "android_panel", "android_convert", "android_remove", "flags_fabled"
    );
    private static final List<String> FLAGS = List.of(
            "master", "potential",
            "boost", "bio",
            "racelock", "yardrat", "spiritualist", "android",
            "kiweapons", "piercing", "dot", "apothic",
            "end", "endportal", "endnatural",
            "shadow", "statchecker",
            "skills", "prestige"
    );
    private static final List<String> ON_OFF = List.of("on", "off");
    private static final List<String> MEDITATION = List.of("status", "help", "next", "advance", "cycle");
    private static final List<String> MEDITATION_PLAYER = List.of("status", "help");
    private static final List<String> BOOST = List.of(
            "status", "help", "end", "stop", "start", "1.25", "1.5", "2", "3"
    );
    private static final List<String> DO_ACTIONS = List.of(
            "page", "refresh", "flag", "android", "android_remove", "boost"
    );

    private final AdaptiveDifficultyGuiPlugin plugin;

    public ProgressionCommandTree(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    /** @return true (always handled) */
    public boolean execute(CommandSender sender, String[] args) {
        // Console / command blocks: boost + androidify (Saga / Fabled).
        if (!(sender instanceof Player player)) {
            return executeConsole(sender, args);
        }
        if (!AdaptiveDifficultyGuiPlugin.canUsePlayerGui(player)) {
            player.sendMessage("§cNo permission: dmzdiff.gui");
            return true;
        }

        // Player-facing: meditation status/help (no staff gate).
        if (args.length > 0 && "meditation".equalsIgnoreCase(args[0])) {
            return meditation(player, args);
        }
        // Android / other leaves: players use /lm GUI (staff keep slash).
        if (args.length > 0 && "android".equalsIgnoreCase(args[0])) {
            if (!ForgeBridge.isStaff(player)) {
                player.sendMessage("§7Open §f/lm §7→ §cRemove Android §7(GUI).");
                return true;
            }
            if (args.length > 1 && ("remove".equalsIgnoreCase(args[1])
                    || "unandroid".equalsIgnoreCase(args[1]))) {
                String target = args.length > 2 ? args[2] : "";
                sendMultiline(player, ForgeBridge.androidRemove(player, target));
                return true;
            }
        }
        // Player-facing help (meditation only).
        if (args.length > 0 && ("help".equalsIgnoreCase(args[0]) || "?".equals(args[0]))) {
            GuiChat.sendChatResult(player, ForgeBridge.progressionHelp(player));
            return true;
        }

        if (!ForgeBridge.isStaff(player)) {
            player.sendMessage("§6§lPlayer commands");
            player.sendMessage("§e/lm §7· §e/difficulty §7· §e/rival §7· §e/spar");
            player.sendMessage("§7Potential Unlock stays. Flight, sprint, and meditation do not.");
            player.sendMessage("§e/skillcheck §7— donator Skill Check");
            player.sendMessage("§8Everything else: open §f/lm §8and use the GUI.");
            return true;
        }

        if (args.length == 0) {
            // Staff bare → help text (Forge helpOrGui), not auto-main GUI.
            GuiChat.sendChatResult(player, ForgeBridge.progressionHelp(player));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "gui", "menu", "open" -> {
                String page = args.length > 1 ? args[1] : "main";
                plugin.openProgressionRespectingConfig(player, page);
                yield true;
            }
            case "status", "summary", "info" -> {
                GuiChat.sendChatResult(player, ForgeBridge.progressionStatus());
                yield true;
            }
            case "flags", "flagboard" -> {
                plugin.openProgressionRespectingConfig(player, "admin");
                yield true;
            }
            case "admin" -> admin(player, args);
            case "do" -> doAction(player, args);
            case "android" -> {
                if (args.length > 1 && ("remove".equalsIgnoreCase(args[1])
                        || "unandroid".equalsIgnoreCase(args[1]))) {
                    String target = args.length > 2 ? args[2] : "";
                    sendMultiline(player, ForgeBridge.androidRemove(player, target));
                } else {
                    String target = args.length > 1 ? args[1] : "";
                    sendMultiline(player, ForgeBridge.androidConvert(player, target));
                }
                yield true;
            }
            case "boost", "tpboost" -> {
                sendMultiline(player, ForgeBridge.boost(player, joinFrom(args, 1)));
                yield true;
            }
            // Shorthand: /progression skills|tp|race|… opens that GUI page
            case "skills", "tp", "race", "combat", "end", "utility",
                 "boost_panel", "android_convert", "android_remove", "android_panel", "flags_fabled" -> {
                plugin.openProgressionRespectingConfig(player, sub);
                yield true;
            }
            default -> {
                player.sendMessage("§cUnknown: §f/progression " + sub);
                sendMultiline(player, ForgeBridge.progressionHelp());
                yield true;
            }
        };
    }

    /**
     * Console-safe commands for store / Saga / Fabled.
     * Examples:
     * <pre>
     *   progression boost start 2 30 PlayerName
     *   progression android PlayerName
     *   androidify PlayerName
     * </pre>
     */
    private boolean executeConsole(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cConsole: §fprogression boost … §8| §fprogression android <player>");
            sender.sendMessage("§8Also: §fandroidify <player>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("boost".equals(sub) || "tpboost".equals(sub)) {
            String joined = joinFrom(args, 1);
            sendMultiline(sender, ForgeBridge.boost(null, joined));
            return true;
        }
        if ("android".equals(sub) || "androidify".equals(sub) || "androidification".equals(sub)) {
            if (args.length < 2 || args[1] == null || args[1].isBlank()) {
                sender.sendMessage("§cUsage: §fprogression android <player>");
                return true;
            }
            if ("remove".equalsIgnoreCase(args[1]) || "unandroid".equalsIgnoreCase(args[1])) {
                sender.sendMessage("§cAndroid remove is player/staff GUI only (not console).");
                return true;
            }
            sendMultiline(sender, ForgeBridge.androidConvertConsole(args[1].trim()));
            return true;
        }
        sender.sendMessage("§cConsole may run §f/progression boost … §7or §f/progression android <player>");
        sender.sendMessage("§8Saga alias: §fandroidify <player>");
        return true;
    }

    /** Dedicated {@code /androidify <player>} console/Saga entry (also works as staff in-game). */
    public boolean executeAndroidify(CommandSender sender, String[] args) {
        if (args.length < 1 || args[0] == null || args[0].isBlank()) {
            sender.sendMessage("§cUsage: §fandroidify <player>");
            return true;
        }
        String target = args[0].trim();
        if (!(sender instanceof Player player)) {
            sendMultiline(sender, ForgeBridge.androidConvertConsole(target));
            return true;
        }
        if (!ForgeBridge.isStaff(player)) {
            player.sendMessage("§cStaff / console only.");
            return true;
        }
        sendMultiline(player, ForgeBridge.androidConvert(player, target));
        return true;
    }

    private boolean meditation(Player player, String[] args) {
        String medSub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "status";
        if ("next".equals(medSub) || "advance".equals(medSub) || "cycle".equals(medSub)) {
            if (!ForgeBridge.isStaff(player)) {
                // Do not acknowledge staff commands to non-ops — show the trial card only.
                GuiChat.sendChatResult(player, ForgeBridge.meditationExplain(player));
                return true;
            }
            GuiChat.sendChatResult(player, ForgeBridge.meditationAdvance(player));
            GuiChat.sendChatResult(player, ForgeBridge.meditationExplain(player));
            return true;
        }
        // Always chat — never stash into GuiFeedback (player has no inventory open).
        GuiChat.sendChatResult(player, ForgeBridge.meditationExplain(player));
        return true;
    }

    private boolean admin(Player player, String[] args) {
        // /progression admin                     → flags GUI
        // /progression admin <flag> <on|off>     → toggle
        if (args.length == 1) {
            plugin.openProgressionRespectingConfig(player, "admin");
            return true;
        }
        if (args.length == 2) {
            player.sendMessage("§cUsage: §f/progression admin <flag> <on|off>");
            player.sendMessage("§8Or: §f/progression admin §7— open flags GUI");
            return true;
        }
        String flag = args[1];
        String value = args[2];
        sendMultiline(player, ForgeBridge.progressionAdminFlag(player, flag, value));
        return true;
    }

    private boolean doAction(Player player, String[] args) {
        // /progression do <action> [arg] [returnPage]
        String action = args.length > 1 ? args[1] : "";
        String arg = args.length > 2 ? args[2] : "";
        String returnPage = args.length > 3 ? args[3] : null;
        String reopen;
        if ("page".equalsIgnoreCase(action) || "refresh".equalsIgnoreCase(action)) {
            reopen = arg == null || arg.isBlank() ? "main" : arg;
        } else {
            reopen = returnPage == null || returnPage.isBlank() ? "main" : returnPage;
            String msg = ForgeBridge.progressionHandleDo(player, action, arg, reopen);
            sendMultiline(player, msg);
        }
        if ("chat".equals(ForgeBridge.guiBackend())) {
            ForgeBridge.openProgressionChatMenu(player, reopen);
        } else {
            plugin.openProgressionInventory(player, reopen);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        boolean staff = ForgeBridge.isStaff(player);
        if (args.length == 1) {
            List<String> root = new ArrayList<>();
            root.add("meditation");
            root.add("help");
            if (staff) {
                root.add("android");
                root.addAll(ROOT);
            }
            return filter(root, args[0]);
        }
        if (!staff && !(args.length >= 1 && "meditation".equalsIgnoreCase(args[0]))) {
            return Collections.emptyList();
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            return switch (sub) {
                case "gui", "menu", "open" -> staff ? filter(PAGES, args[1]) : Collections.emptyList();
                case "admin" -> staff ? filter(FLAGS, args[1]) : Collections.emptyList();
                case "do" -> staff ? filter(DO_ACTIONS, args[1]) : Collections.emptyList();
                case "meditation" -> filter(
                        ForgeBridge.isStaff(player) ? MEDITATION : MEDITATION_PLAYER, args[1]);
                case "android" -> {
                    if (!staff) {
                        yield Collections.emptyList();
                    }
                    List<String> androidTab = new ArrayList<>();
                    androidTab.add("remove");
                    androidTab.addAll(onlineNames(player));
                    yield filter(androidTab, args[1]);
                }
                case "boost", "tpboost" -> staff ? filter(BOOST, args[1]) : Collections.emptyList();
                default -> Collections.emptyList();
            };
        }
        if (args.length == 3) {
            if ("admin".equals(sub)) {
                return filter(ON_OFF, args[2]);
            }
            if ("do".equals(sub)) {
                String action = args[1].toLowerCase(Locale.ROOT);
                if ("page".equals(action) || "refresh".equals(action)) {
                    return filter(PAGES, args[2]);
                }
                if ("flag".equals(action) || "toggle".equals(action)) {
                    return filter(FLAGS, args[2]);
                }
                if ("android".equals(action) || "android_remove".equals(action)
                        || "androidremove".equals(action) || "remove_android".equals(action)) {
                    return filter(onlineNames(player), args[2]);
                }
                if ("boost".equals(action)) {
                    return filter(List.of("end", "1.25:30", "1.5:30", "2:30", "2:60", "3:30"), args[2]);
                }
            }
            if ("android".equals(sub)
                    && ("remove".equalsIgnoreCase(args[1]) || "unandroid".equalsIgnoreCase(args[1]))) {
                return filter(onlineNames(player), args[2]);
            }
            if ("boost".equals(sub) || "tpboost".equals(sub)) {
                if ("start".equalsIgnoreCase(args[1])) {
                    return filter(List.of("1.25", "1.5", "2", "2.0", "3", "1250030"), args[2]);
                }
            }
            return Collections.emptyList();
        }
        if (args.length == 4) {
            if ("do".equals(sub)) {
                return filter(PAGES, args[3]);
            }
            if (("boost".equals(sub) || "tpboost".equals(sub)) && "start".equalsIgnoreCase(args[1])) {
                // minutes after multiplier
                return filter(List.of("15", "30", "45", "60", "120"), args[3]);
            }
            if ("admin".equals(sub)) {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    private static List<String> onlineNames(Player self) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    private static List<String> filter(List<String> options, String token) {
        if (token == null || token.isBlank()) {
            return options;
        }
        String t = token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(t)) {
                out.add(o);
            }
        }
        return out;
    }

    private static String joinFrom(String[] args, int start) {
        if (args == null || start >= args.length) {
            return "";
        }
        return Arrays.stream(args, start, args.length).collect(Collectors.joining(" "));
    }

    private static void sendMultiline(CommandSender sender, String msg) {
        if (sender instanceof Player player) {
            GuiChat.sendResult(player, msg);
            return;
        }
        if (msg == null || msg.isBlank()) {
            return;
        }
        for (String line : msg.split("\n")) {
            if (!line.isBlank()) {
                sender.sendMessage(line);
            }
        }
    }

    private static void sendMultiline(Player player, String msg) {
        GuiChat.sendResult(player, msg);
    }
}
