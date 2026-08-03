package com.dbzlegacy.adaptivedifficulty.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bukkit-side entrypoint so {@code /difficulty} works for all players on Mohist.
 * Prefers CMILib (CMI) inventory GUIs, then a plain chest GUI.
 * <p>
 * On Mohist, this plugin owns {@code /difficulty} — admin switches (on/off/whitelist)
 * must be handled here and forwarded into the Forge mod config.
 */
public final class AdaptiveDifficultyGuiPlugin extends JavaPlugin {
    private DifficultyChestGui chestGui;

    @Override
    public void onEnable() {
        chestGui = new DifficultyChestGui(this);
        getServer().getPluginManager().registerEvents(chestGui, this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DmzDiffExpansion(this).register();
            getLogger().info("Registered PlaceholderAPI expansion: dmzdiff");
        }

        boolean forge = ForgeBridge.forgeAvailable();
        getLogger().info("GUI backend: CMILib=" + CmiDifficultyGui.available()
                + " forgeMod=" + forge);
        if (!forge) {
            String err = ForgeBridge.lastError();
            getLogger().severe("Adaptive Difficulty Forge mod NOT reachable — GUI actions will fail."
                    + (err == null || err.isBlank() ? "" : " (" + err + ")"));
            getLogger().severe("Install mods/dmz_adaptive_difficulty-*.jar and restart.");
        } else {
            String modVer = ForgeBridge.modVersion();
            String pluginVer = getDescription().getVersion();
            if (modVer != null && pluginVer != null && !modVer.equals(pluginVer)) {
                getLogger().severe("VERSION SKEW: Forge mod=" + modVer + " GUI plugin=" + pluginVer
                        + " — install matching dmz_adaptive_difficulty jars or GUI reopen/actions may break.");
            } else {
                getLogger().info("Version handshake OK: " + pluginVer);
            }
        }
        getLogger().info("Registered Bukkit /difficulty (CMI GUI preferred).");
    }

    /**
     * Called by the Forge mod via reflection when {@code guiBackend} is cmi/chest/auto.
     * Always opens an inventory GUI (does not re-read chat preference — Forge already decided).
     */
    public void openMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        openInventory(player, page);
    }

    /** Chest-only open for Forge {@code guiBackend=chest}. */
    public void openChestMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        chestGui.open(player, page);
    }

    /** Player-facing open that honors Forge {@code guiBackend}. */
    public void openMenuRespectingConfig(Player player, String page) {
        if (player == null) {
            return;
        }
        String backend = ForgeBridge.guiBackend();
        if ("chat".equals(backend)) {
            if (!ForgeBridge.openChatMenu(player, page)) {
                player.sendMessage("§cChat difficulty menu unavailable (is the Forge mod loaded?).");
            }
            return;
        }
        if ("chest".equals(backend)) {
            chestGui.open(player, page);
            return;
        }
        // cmi / auto / unknown → CMI then chest
        openInventory(player, page);
    }

    private void openInventory(Player player, String page) {
        if (CmiDifficultyGui.available() && CmiDifficultyGui.open(player, page)) {
            return;
        }
        if (CmiDifficultyGui.available()) {
            getLogger().warning("CMI GUI open failed for " + player.getName()
                    + " — falling back to chest GUI. Check CMILib version.");
        } else {
            getLogger().warning("CMILib/CMI not available — using chest GUI for "
                    + player.getName() + ". Install CMILib + CMI for the CMI inventory UI.");
        }
        chestGui.open(player, page);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        if ("dmzdiffgui".equals(name)) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                player.sendMessage("§cNo permission: dmzdiff.gui");
                return true;
            }
            String page = args.length > 0 ? args[0] : "main";
            if (("settings".equalsIgnoreCase(page) || "stats".equalsIgnoreCase(page)
                    || "details".equalsIgnoreCase(page) || "statistics".equalsIgnoreCase(page))
                    && !ForgeBridge.isStaff(player)) {
                player.sendMessage("§cStaff only.");
                page = "main";
            }
            // Force inventory — ignore guiBackend=chat (debug / recovery).
            openInventory(player, page);
            return true;
        }
        if (!"difficulty".equals(name)) {
            return false;
        }
        return handleDifficulty(sender, args);
    }

    private boolean handleDifficulty(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only. Use /difficulty admin … from console.");
                return true;
            }
            if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                player.sendMessage("§cNo permission: dmzdiff.gui");
                return true;
            }
            openMenuRespectingConfig(player, "main");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "do" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                    player.sendMessage("§cNo permission: dmzdiff.gui");
                    return true;
                }
                String action = args.length > 1 ? args[1] : "";
                String arg = args.length > 2 ? args[2] : null;
                String returnPage = args.length > 3 ? args[3] : null;
                String reopen = ForgeBridge.resolveReturnPage(action, arg, returnPage);
                ForgeBridge.ActionResult result = ForgeBridge.handleActionResult(player, action, arg, reopen);
                if (result.message() != null && !result.message().isBlank()) {
                    String msg = result.message();
                    if (!msg.startsWith("§")) {
                        msg = (result.ok() ? "§a" : "§c") + msg;
                    }
                    player.sendMessage(msg);
                }
                // Bukkit owns reopen: inventory for cmi/chest/auto, chat menu for chat backend.
                if (reopen != null && !reopen.isBlank()) {
                    if ("chat".equals(ForgeBridge.guiBackend())) {
                        ForgeBridge.openChatMenu(player, reopen);
                    } else {
                        openInventory(player, reopen);
                    }
                }
                return true;
            }
            case "admin" -> {
                return handleAdmin(sender, args);
            }
            case "reset", "zero", "clear" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                    player.sendMessage("§cNo permission: dmzdiff.gui");
                    return true;
                }
                ForgeBridge.ActionResult result = ForgeBridge.handleActionResult(player, "reset", "0", "main");
                if (result.message() != null && !result.message().isBlank()) {
                    String msg = result.message();
                    if (!msg.startsWith("§")) {
                        msg = (result.ok() ? "§a" : "§c") + msg;
                    }
                    player.sendMessage(msg);
                }
                if ("chat".equals(ForgeBridge.guiBackend())) {
                    ForgeBridge.openChatMenu(player, "main");
                } else {
                    openInventory(player, "main");
                }
                return true;
            }
            case "buy", "purchase", "unlock", "lower", "adjust", "titles", "title",
                 "team", "teams", "stats", "details" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                    player.sendMessage("§cNo permission: dmzdiff.gui");
                    return true;
                }
                String page = switch (sub) {
                    case "purchase", "unlock" -> "buy";
                    case "adjust" -> "lower";
                    case "title" -> "titles";
                    case "teams" -> "team";
                    case "details" -> "stats";
                    default -> sub;
                };
                if (("stats".equals(page) || "details".equals(sub)) && !ForgeBridge.isStaff(player)) {
                    openMenuRespectingConfig(player, "main");
                    return true;
                }
                openMenuRespectingConfig(player, page);
                return true;
            }
            case "hard", "normal", "easy", "peaceful" -> {
                boolean staff = sender instanceof Player p
                        ? ForgeBridge.isStaff(p)
                        : sender.isOp() || sender.hasPermission(ForgeBridge.adminPermission());
                if (!staff) {
                    sender.sendMessage("§cOps only.");
                    return true;
                }
                boolean ok = ForgeBridge.setVanillaDifficulty(sub);
                sender.sendMessage(ok
                        ? "§aVanilla difficulty set to §f" + sub
                        : "§cFailed to set vanilla difficulty.");
                if (ok && "peaceful".equals(sub)) {
                    sender.sendMessage("§cHostile mobs will not spawn — adaptive scaling will not run.");
                }
                return true;
            }
            case "help", "?" -> {
                if (sender instanceof Player player && !ForgeBridge.isStaff(player)) {
                    // Normal players: open the clean GUI — no command dump.
                    openMenuRespectingConfig(player, "main");
                    return true;
                }
                sendStaffHelp(sender);
                return true;
            }
            default -> {
                // Unknown args: players just get the menu; staff get command help.
                if (sender instanceof Player player) {
                    if (ForgeBridge.isStaff(player)) {
                        sendStaffHelp(sender);
                    } else {
                        openMenuRespectingConfig(player, "main");
                    }
                    return true;
                }
                sendStaffHelp(sender);
                return true;
            }
        }
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        boolean console = !(sender instanceof Player);
        Player player = console ? null : (Player) sender;

        if (!console && !ForgeBridge.isStaff(player)) {
            // No command-syntax dump for normal players.
            sender.sendMessage("§cStaff only.");
            return true;
        }

        // Bare /difficulty admin shows staff help.
        if (args.length == 1) {
            sendAdminHelp(sender);
            return true;
        }

        String sub = args[1].toLowerCase();

        // Master switch + whitelist.
        if (isDirectStaffSubcommand(sub)) {
            return handleDirectStaff(sender, args, sub);
        }

        switch (sub) {
            case "help" -> {
                sendAdminHelp(sender);
                return true;
            }
            case "resetpurchased" -> {
                if (player == null) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                sender.sendMessage(ForgeBridge.resetPurchased(player));
                return true;
            }
            case "characterreset" -> {
                if (player == null) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                ForgeBridge.ActionResult result =
                        ForgeBridge.handleActionResult(player, "character_reset", "0", "");
                String msg = result.message();
                if (msg == null || msg.isBlank()) {
                    msg = result.ok() ? "Character difficulty reset applied." : "Character reset failed.";
                }
                if (!msg.startsWith("§")) {
                    msg = (result.ok() ? "§a" : "§c") + msg;
                }
                sender.sendMessage(msg);
                return true;
            }
            case "reload" -> {
                if (ForgeBridge.reloadConfig()) {
                    sender.sendMessage("§aAdaptive difficulty config reloaded.");
                } else {
                    String err = ForgeBridge.lastError();
                    sender.sendMessage("§cConfig reload failed"
                            + (err == null || err.isBlank() ? "." : ": " + err));
                }
                return true;
            }
            case "settings" -> {
                if (player == null) {
                    sender.sendMessage("Players only for settings page.");
                    return true;
                }
                if (!ForgeBridge.openChatMenu(player, "settings")) {
                    openMenuRespectingConfig(player, "main");
                }
                sender.sendMessage("§7Use §f/difficulty admin set <key> <value>");
                return true;
            }
            case "area" -> {
                if (player == null) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                sender.sendMessage(ForgeBridge.areaDifficultyText(player));
                return true;
            }
            case "gamedifficulty" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /difficulty admin gamedifficulty <peaceful|easy|normal|hard>");
                    return true;
                }
                boolean ok = ForgeBridge.setVanillaDifficulty(args[2]);
                sender.sendMessage(ok
                        ? "§aVanilla difficulty set to §f" + args[2]
                        : "§cFailed to set vanilla difficulty.");
                return true;
            }
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /difficulty admin set <key> <value>");
                    return true;
                }
                String key = args[2];
                StringBuilder sb = new StringBuilder(args[3]);
                for (int i = 4; i < args.length; i++) {
                    sb.append(' ').append(args[i]);
                }
                sender.sendMessage(ForgeBridge.adminSet(key, sb.toString()));
                return true;
            }
            default -> {
                sender.sendMessage("§cUnknown admin subcommand. Try §f/difficulty admin help");
                return true;
            }
        }
    }

    private static boolean isDirectStaffSubcommand(String sub) {
        return switch (sub) {
            case "off", "disable", "on", "enable", "toggle", "status",
                 "whitelist", "wl" -> true;
            default -> false;
        };
    }

    private boolean handleDirectStaff(CommandSender sender, String[] args, String sub) {
        switch (sub) {
            case "off", "disable" -> {
                sender.sendMessage(ForgeBridge.setSystemEnabled(false));
                return true;
            }
            case "on", "enable" -> {
                sender.sendMessage(ForgeBridge.setSystemEnabled(true));
                return true;
            }
            case "toggle" -> {
                sender.sendMessage(ForgeBridge.setSystemEnabled(!ForgeBridge.systemEnabled()));
                return true;
            }
            case "status" -> {
                sender.sendMessage(ForgeBridge.systemStatusText());
                return true;
            }
            case "whitelist", "wl" -> {
                return handleWhitelist(sender, args);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean handleWhitelist(CommandSender sender, String[] args) {
        // /difficulty admin whitelist
        if (args.length == 2) {
            sender.sendMessage(ForgeBridge.whitelistStatusText());
            return true;
        }
        String op = args[2].toLowerCase();
        switch (op) {
            case "on", "enable" -> {
                sender.sendMessage(ForgeBridge.setWhitelistEnabled(true));
                return true;
            }
            case "off", "disable" -> {
                sender.sendMessage(ForgeBridge.setWhitelistEnabled(false));
                return true;
            }
            case "toggle" -> {
                sender.sendMessage(ForgeBridge.setWhitelistEnabled(!ForgeBridge.whitelistEnabled()));
                return true;
            }
            case "status" -> {
                sender.sendMessage(ForgeBridge.whitelistStatusText());
                return true;
            }
            case "list" -> {
                sender.sendMessage(ForgeBridge.whitelistListText());
                return true;
            }
            case "add" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /difficulty admin whitelist add <player>");
                    return true;
                }
                sender.sendMessage(ForgeBridge.whitelistAdd(args[3]));
                return true;
            }
            case "remove", "rm", "del" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /difficulty admin whitelist remove <player|uuid>");
                    return true;
                }
                StringBuilder name = new StringBuilder(args[3]);
                for (int i = 4; i < args.length; i++) {
                    name.append(' ').append(args[i]);
                }
                sender.sendMessage(ForgeBridge.whitelistRemove(name.toString()));
                return true;
            }
            case "clear" -> {
                sender.sendMessage(ForgeBridge.whitelistClear());
                return true;
            }
            default -> {
                sender.sendMessage("§cUsage: /difficulty admin whitelist on|off|add|remove|list|clear");
                return true;
            }
        }
    }

    private static void sendAdminHelp(CommandSender sender) {
        sendStaffHelp(sender);
    }

    /** Full command reference — staff / console only. */
    private static void sendStaffHelp(CommandSender sender) {
        sender.sendMessage("§6Adaptive Difficulty — staff");
        sender.sendMessage("§e/difficulty §7— open GUI");
        sender.sendMessage("§e/difficulty buy|lower|titles|details §7— open those pages");
        sender.sendMessage("§e/difficulty reset §7— clear active tier");
        sender.sendMessage("§e/difficulty admin off|on|toggle|status §7— master system switch");
        sender.sendMessage("§e/difficulty admin whitelist on|off|add|remove|list|clear §7— testing whitelist");
        sender.sendMessage("§e/difficulty admin reload|settings|area|set §7— config tools");
        sender.sendMessage("§e/difficulty hard|normal|easy|peaceful §7— vanilla difficulty");
        sender.sendMessage("§8Master keys: enabled · whitelistEnabled");
    }
}
