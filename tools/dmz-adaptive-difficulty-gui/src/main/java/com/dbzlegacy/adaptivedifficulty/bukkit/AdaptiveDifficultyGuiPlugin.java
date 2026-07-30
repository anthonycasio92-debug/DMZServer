package com.dbzlegacy.adaptivedifficulty.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Companion Bukkit plugin:
 * <ul>
 *   <li>PlaceholderAPI expansion {@code %dmzdiff_*%} for DeluxeMenus</li>
 *   <li>Chest inventory GUI fallback opened by the Forge mod</li>
 * </ul>
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
        } else {
            getLogger().warning("PlaceholderAPI not found — DeluxeMenus lore placeholders will be empty.");
        }
        getLogger().info("DMZ Adaptive Difficulty GUI ready (chest + PAPI bridge).");
    }

    /** Called by the Forge mod via reflection. */
    public void openMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        chestGui.open(player, page);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        String page = args.length > 0 ? args[0] : "main";
        if (args.length > 1 && "open".equalsIgnoreCase(args[0])) {
            page = args[1];
        }
        openMenu(player, page);
        return true;
    }
}
