package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.Zrips.CMILib.GUI.CMIGui;
import net.Zrips.CMILib.GUI.CMIGuiButton;
import net.Zrips.CMILib.GUI.GUIManager.InvType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Inventory GUI hosted by CMILib (CMI's GUI engine). */
public final class CmiDifficultyGui {
    private CmiDifficultyGui() {}

    public static boolean available() {
        try {
            Class.forName("net.Zrips.CMILib.GUI.CMIGui");
            Class.forName("net.Zrips.CMILib.CMILib");
            return org.bukkit.Bukkit.getPluginManager().getPlugin("CMILib") != null
                    || org.bukkit.Bukkit.getPluginManager().getPlugin("CMI") != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean open(Player player, String page) {
        if (player == null || !available()) {
            return false;
        }
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        try {
            switch (p) {
                case "rewards" -> openRewards(player);
                case "tiers", "enemies" -> openTiers(player);
                case "stats", "statistics" -> openStats(player);
                default -> openMain(player);
            }
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cCMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    private static void openMain(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Adaptive Difficulty", 5);

        String stateColor = ph.getOrDefault("state_color", "f");
        CMIGuiButton info = new CMIGuiButton(4, Material.NETHER_STAR, "&6&lAdaptive Difficulty");
        info.lockField();
        info.addLore(List.of(
                "&eActive: &f" + ph.getOrDefault("active", "?") + " &7/ max &f" + ph.getOrDefault("available", "?"),
                "&eState: &" + stateColor + ph.getOrDefault("state", "?"),
                "&eCalculated: &f" + ph.getOrDefault("calculated", "?")
                        + " &7(Lv " + ph.getOrDefault("level", "?")
                        + " · Prestige " + ph.getOrDefault("prestige", "?") + ")",
                "&ePurchased: &f" + ph.getOrDefault("purchased", "?")
                        + " &8| &ePersonal Max: &f" + ph.getOrDefault("personal_max", "?"),
                "&eTeam Bonus: &f" + ph.getOrDefault("team_bonus", "?")
                        + " &8| &eContribution: &f" + ph.getOrDefault("team_contrib", "?"),
                "&eTeam: &f" + ph.getOrDefault("team_name", "?")
                        + " &8(" + ph.getOrDefault("team_size", "0")
                        + " via " + ph.getOrDefault("team_source", "?") + ")",
                "&eTeam Mode: &f" + ph.getOrDefault("team_mode", "?")
                        + " &8| &eBalance: &f" + ph.getOrDefault("balance", "?"),
                "&eEnemy Tier: &f" + ph.getOrDefault("tier", "?")
        ));
        gui.addButton(info);

        gui.addButton(actionBtn(19, Material.RED_DYE, "&c▼ -100", "down", "100", "&7Lower active difficulty (free)"));
        gui.addButton(actionBtn(21, Material.ORANGE_DYE, "&6Max", "set_max", "0", "&7Set active to available max"));
        gui.addButton(actionBtn(23, Material.LIME_DYE, "&a▲ +100", "up", "100", "&7Raise active difficulty"));
        gui.addButton(actionBtn(25, Material.COMPASS, "&bTeam Mode", "team", "0",
                "&7Current: &f" + ph.getOrDefault("team_mode", "?")));

        gui.addButton(actionBtn(29, Material.GOLD_NUGGET, "&6Buy +100", "buy", "100",
                "&7Cost: &f" + ph.getOrDefault("cost_100", "?")));
        gui.addButton(actionBtn(31, Material.GOLD_INGOT, "&6Buy +1,000", "buy", "1000",
                "&7Cost: &f" + ph.getOrDefault("cost_1000", "?")));
        gui.addButton(actionBtn(33, Material.GOLD_BLOCK, "&6Buy +10,000", "buy", "10000",
                "&7Cost: &f" + ph.getOrDefault("cost_10000", "?")));

        gui.addButton(pageBtn(37, Material.BOOK, "&eRewards", "rewards"));
        gui.addButton(pageBtn(38, Material.IRON_SWORD, "&eTiers", "tiers"));
        gui.addButton(pageBtn(39, Material.PAPER, "&eStats", "stats"));
        gui.addButton(actionBtn(40, Material.SUNFLOWER, "&aRefresh", "refresh", "0", "&7Reload this menu"));
        gui.addButton(closeBtn(44));
        gui.open();
    }

    private static void openRewards(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Rewards", 3);
        CMIGuiButton info = new CMIGuiButton(13, Material.EXPERIENCE_BOTTLE, "&6&lRewards");
        info.lockField();
        info.addLore(List.of(
                "&eActive: &f" + ph.getOrDefault("active", "?"),
                "&eTP Multiplier: &f×" + ph.getOrDefault("reward_mult", "?"),
                "&7Formula: 1 + Difficulty / RewardScaling"
        ));
        gui.addButton(info);
        gui.addButton(pageBtn(18, Material.ARROW, "&a« Back", "main"));
        gui.addButton(closeBtn(26));
        gui.open();
    }

    private static void openTiers(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Enemy Tiers", 4);
        CMIGuiButton info = new CMIGuiButton(4, Material.DIAMOND_SWORD, "&6&lEnemy Tiers");
        info.lockField();
        info.addLore(List.of(
                "&7Current: &f" + ph.getOrDefault("tier", "?"),
                "&7Active: &f" + ph.getOrDefault("active", "?")
        ));
        gui.addButton(info);

        String[][] rows = {
                {"awakened", "Awakened"}, {"enhanced", "Enhanced"}, {"elite", "Elite"}, {"advanced", "Advanced"},
                {"master", "Master"}, {"legendary", "Legendary"}, {"god", "God"},
                {"divine", "Divine"}, {"impossible", "Impossible"}
        };
        long active;
        try {
            active = Long.parseLong(ph.getOrDefault("active", "0"));
        } catch (NumberFormatException e) {
            active = 0L;
        }
        int slot = 11;
        for (String[] row : rows) {
            long thr;
            try {
                thr = Long.parseLong(ph.getOrDefault("tier_" + row[0], defaultsTier(row[0])));
            } catch (NumberFormatException e) {
                thr = Long.parseLong(defaultsTier(row[0]));
            }
            boolean unlocked = active >= thr;
            CMIGuiButton btn = new CMIGuiButton(
                    slot,
                    unlocked ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE,
                    (unlocked ? "&a✓ " : "&8· ") + "&e" + thr + " &f" + row[1]
            );
            btn.lockField();
            btn.addLore(unlocked ? "&7Unlocked" : "&7Locked");
            gui.addButton(btn);
            slot++;
            if (slot == 16) {
                slot = 20;
            }
        }
        gui.addButton(pageBtn(27, Material.ARROW, "&a« Back", "main"));
        gui.addButton(closeBtn(35));
        gui.open();
    }

    private static void openStats(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Statistics", 3);
        String stateColor = ph.getOrDefault("state_color", "f");
        CMIGuiButton info = new CMIGuiButton(13, Material.BOOK, "&6&lStatistics");
        info.lockField();
        info.addLore(List.of(
                "&7State &" + stateColor + ph.getOrDefault("state", "?"),
                "&7Balance &f" + ph.getOrDefault("balance", "?")
                        + " &8| &7Teams &f" + ph.getOrDefault("team_source", "?"),
                "&7Active &f" + ph.getOrDefault("active", "?")
                        + " &8| &7Available &f" + ph.getOrDefault("available", "?"),
                "&7Purchased &f" + ph.getOrDefault("purchased", "?")
                        + " &8| &7Calculated &f" + ph.getOrDefault("calculated", "?"),
                "&7Titles &f" + ph.getOrDefault("titles", "none")
        ));
        gui.addButton(info);
        gui.addButton(pageBtn(18, Material.ARROW, "&a« Back", "main"));
        gui.addButton(closeBtn(26));
        gui.open();
    }

    private static String defaultsTier(String key) {
        return switch (key) {
            case "awakened" -> "10";
            case "enhanced" -> "50";
            case "elite" -> "100";
            case "advanced" -> "500";
            case "master" -> "1000";
            case "legendary" -> "5000";
            case "god" -> "10000";
            case "divine" -> "50000";
            case "impossible" -> "100000";
            default -> "0";
        };
    }

    private static CMIGui base(Player player, String title, int rows) {
        CMIGui gui = new CMIGui(player);
        gui.setTitle(title);
        gui.setInvSize(rows);
        gui.addLock(InvType.Gui);
        gui.fillEmptyButtons();
        return gui;
    }

    private static CMIGuiButton actionBtn(int slot, Material mat, String name, String action, String arg, String tip) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        btn.addLore(tip);
        btn.addCommand("difficulty do " + action + " " + arg);
        return btn;
    }

    private static CMIGuiButton pageBtn(int slot, Material mat, String name, String page) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        btn.addLore("&7Open page");
        btn.addCommand("difficulty do page " + page);
        return btn;
    }

    private static CMIGuiButton closeBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BARRIER, "&cClose");
        btn.lockField();
        btn.setCloseInv(true);
        return btn;
    }
}
