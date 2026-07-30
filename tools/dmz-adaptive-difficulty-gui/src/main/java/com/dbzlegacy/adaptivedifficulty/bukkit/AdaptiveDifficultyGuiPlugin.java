package com.dbzlegacy.adaptivedifficulty.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bukkit-side entrypoint so {@code /difficulty} works for all players on Mohist.
 * Prefers CMILib (CMI) inventory GUIs, then a plain chest GUI.
 */
public final class AdaptiveDifficultyGuiPlugin extends JavaPlugin implements Listener {
    private DifficultyChestGui chestGui;

    @Override
    public void onEnable() {
        chestGui = new DifficultyChestGui(this);
        getServer().getPluginManager().registerEvents(chestGui, this);
        getServer().getPluginManager().registerEvents(this, this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DmzDiffExpansion(this).register();
            getLogger().info("Registered PlaceholderAPI expansion: dmzdiff");
        }

        getLogger().info("GUI backend: CMILib=" + CmiDifficultyGui.available()
                + " forgeMod=" + ForgeBridge.forgeAvailable());
        getLogger().info("Registered Bukkit /difficulty (CMI GUI preferred).");
    }

    /** Called by the Forge mod via reflection. */
    public void openMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        if (CmiDifficultyGui.open(player, page)) {
            return;
        }
        chestGui.open(player, page);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ForgeBridge.clearAdmin(event.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        if ("dmzdiffgui".equals(name)) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            openMenu(player, args.length > 0 ? args[0] : "main");
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
            openMenu(player, "main");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "do" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                String action = args.length > 1 ? args[1] : "";
                String arg = args.length > 2 ? args[2] : null;
                String msg = ForgeBridge.handleAction(player, action, arg);
                if (msg != null && !msg.isBlank()) {
                    player.sendMessage(msg.startsWith("§") ? msg : "§e" + msg);
                }
                // Actions already reopen the Forge GUI path; reopen CMI/chest here too.
                getServer().getScheduler().runTask(this, () -> openMenu(player, "page".equalsIgnoreCase(action)
                        ? (arg == null ? "main" : arg)
                        : "main"));
                return true;
            }
            case "admin" -> {
                return handleAdmin(sender, args);
            }
            case "hard", "normal", "easy", "peaceful" -> {
                if (!sender.isOp() && !sender.hasPermission("difficulty.admin")) {
                    sender.sendMessage("§cOps only: /difficulty " + sub);
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
            default -> {
                sender.sendMessage("§e/difficulty §7— open GUI");
                sender.sendMessage("§e/difficulty admin §7— staff tools");
                return true;
            }
        }
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        boolean console = !(sender instanceof Player);
        Player player = console ? null : (Player) sender;

        if (!console && !ForgeBridge.isStaff(player)) {
            sender.sendMessage("§cNo permission for /difficulty admin (need op or difficulty.admin).");
            return true;
        }

        // Bare /difficulty admin toggles admin mode for players; console gets help.
        if (args.length == 1) {
            if (console) {
                sender.sendMessage("§6/difficulty admin help|reload|settings|gamedifficulty|set");
                return true;
            }
            boolean enabled = ForgeBridge.toggleAdmin(player);
            if (enabled) {
                sender.sendMessage("§aAdmin commands ENABLED.");
                sender.sendMessage("§7/difficulty admin help|reload|settings");
                sender.sendMessage("§7/difficulty admin gamedifficulty <peaceful|easy|normal|hard>");
                sender.sendMessage("§7/difficulty admin set <key> <value>");
                sender.sendMessage("§8Run §f/difficulty admin §8again to disable.");
            } else {
                sender.sendMessage("§cAdmin commands DISABLED.");
            }
            return true;
        }

        if (!console && !ForgeBridge.hasAdmin(player)) {
            sender.sendMessage("§cEnable admin mode first: §f/difficulty admin");
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "help" -> {
                sender.sendMessage("§6Adaptive Difficulty — admin");
                sender.sendMessage("§e/difficulty §7— open CMI/chest GUI");
                sender.sendMessage("§e/difficulty hard|normal|easy|peaceful §7— vanilla difficulty");
                sender.sendMessage("§e/difficulty admin reload|settings|gamedifficulty|set");
                return true;
            }
            case "reload" -> {
                ForgeBridge.reloadConfig();
                sender.sendMessage("§aAdaptive difficulty config reloaded.");
                return true;
            }
            case "settings" -> {
                if (player == null) {
                    sender.sendMessage("Players only for settings page.");
                    return true;
                }
                openMenu(player, "main");
                sender.sendMessage("§7Use §f/difficulty admin set <key> <value>");
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
                sender.sendMessage("§a" + ForgeBridge.adminSet(key, sb.toString()));
                return true;
            }
            default -> {
                sender.sendMessage("§cUnknown admin subcommand. Try §f/difficulty admin help");
                return true;
            }
        }
    }
}
