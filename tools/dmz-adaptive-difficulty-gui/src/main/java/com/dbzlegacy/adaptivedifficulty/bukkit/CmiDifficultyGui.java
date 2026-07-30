package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.Zrips.CMILib.GUI.CMIGui;
import net.Zrips.CMILib.GUI.CMIGuiButton;
import net.Zrips.CMILib.GUI.GUIManager.InvType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Clean CMILib inventory GUI for adaptive difficulty. */
public final class CmiDifficultyGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

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
        // 6 rows: status / spacer / adjust / buy / spacer / nav
        CMIGui gui = base(player, "&8Difficulty", 6);
        frame(gui, 6);

        String stateColor = ph.getOrDefault("state_color", "f");
        CMIGuiButton status = new CMIGuiButton(13, Material.NETHER_STAR, "&f&lDifficulty");
        status.lockField();
        status.addLore(statusLore(ph, stateColor));
        gui.addButton(status);

        // Adjust row (row index 2 → slots 18–26)
        gui.addButton(actionBtn(20, Material.RED_CONCRETE, "&c− 100", "down", "100",
                List.of("&7Lower active difficulty", "&8Always free")));
        gui.addButton(actionBtn(21, Material.WHITE_CONCRETE, "&fReset", "reset", "0",
                List.of("&7Set active to &f0", "&8Purchased max kept")));
        gui.addButton(actionBtn(22, Material.LIME_CONCRETE, "&a+ 100", "up", "100",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_100", "?"))));
        gui.addButton(actionBtn(23, Material.ORANGE_CONCRETE, "&6Max", "set_max", "0",
                List.of("&7Jump to available max", "&f" + ph.getOrDefault("available", "?"),
                        "&8Cost &e" + ph.getOrDefault("cost_max", "?"))));
        gui.addButton(actionBtn(24, Material.COMPASS, "&bTeam", "team", "0",
                List.of("&7Cycle team scaling", "&8" + ph.getOrDefault("team_mode", "?"))));

        // Unlock more max (also Lightman's iron coins)
        gui.addButton(actionBtn(29, Material.GOLD_NUGGET, "&eBuy +100 max", "buy", "100",
                List.of("&7Unlock more max difficulty", "&8Cost &e" + ph.getOrDefault("cost_100", "?"))));
        gui.addButton(actionBtn(31, Material.GOLD_INGOT, "&eBuy +1,000 max", "buy", "1000",
                List.of("&7Unlock more max difficulty", "&8Cost &e" + ph.getOrDefault("cost_1000", "?"))));
        gui.addButton(actionBtn(33, Material.GOLD_BLOCK, "&eBuy +10,000 max", "buy", "10000",
                List.of("&7Unlock more max difficulty", "&8Cost &e" + ph.getOrDefault("cost_10000", "?"))));

        // Footer (row index 5 → slots 45–53)
        gui.addButton(pageBtn(45, Material.EXPERIENCE_BOTTLE, "&fRewards", "rewards",
                "&7TP multiplier details"));
        gui.addButton(pageBtn(46, Material.IRON_SWORD, "&fTiers", "tiers",
                "&7Enemy tier unlocks"));
        gui.addButton(pageBtn(47, Material.BOOK, "&fDetails", "stats",
                "&7Team, titles, full breakdown"));
        gui.addButton(actionBtn(49, Material.SUNFLOWER, "&7Refresh", "refresh", "0",
                List.of("&7Reload this menu")));
        gui.addButton(closeBtn(53));
        gui.open();
    }

    private static void openRewards(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Rewards", 3);
        frame(gui, 3);

        CMIGuiButton info = new CMIGuiButton(13, Material.EXPERIENCE_BOTTLE, "&f&lRewards");
        info.lockField();
        info.addLore(List.of(
                "",
                "&7Active  &f" + ph.getOrDefault("active", "?"),
                "&7TP mult &a×" + ph.getOrDefault("reward_mult", "?"),
                "",
                "&81 + Difficulty / RewardScaling"
        ));
        gui.addButton(info);
        gui.addButton(pageBtn(18, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(closeBtn(26));
        gui.open();
    }

    private static void openTiers(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Enemy Tiers", 4);
        frame(gui, 4);

        CMIGuiButton header = new CMIGuiButton(4, Material.IRON_SWORD, "&f&lEnemy Tiers");
        header.lockField();
        header.addLore(List.of(
                "",
                "&7Current  &f" + ph.getOrDefault("tier", "?"),
                "&7Active   &f" + ph.getOrDefault("active", "?")
        ));
        gui.addButton(header);

        String[][] rows = {
                {"awakened", "Awakened"}, {"enhanced", "Enhanced"}, {"elite", "Elite"},
                {"advanced", "Advanced"}, {"master", "Master"}, {"legendary", "Legendary"},
                {"god", "God"}, {"divine", "Divine"}, {"impossible", "Impossible"}
        };
        long active = parseLong(ph.getOrDefault("active", "0"));
        int[] slots = {18, 19, 20, 21, 22, 23, 24, 25, 26};
        for (int i = 0; i < rows.length; i++) {
            long thr = parseLong(ph.getOrDefault("tier_" + rows[i][0], defaultsTier(rows[i][0])));
            boolean unlocked = active >= thr;
            CMIGuiButton btn = new CMIGuiButton(
                    slots[i],
                    unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE,
                    (unlocked ? "&a" : "&8") + rows[i][1]
            );
            btn.lockField();
            btn.addLore(List.of(
                    "",
                    "&7Threshold  &f" + thr,
                    unlocked ? "&aUnlocked" : "&8Locked"
            ));
            gui.addButton(btn);
        }
        gui.addButton(pageBtn(27, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(closeBtn(35));
        gui.open();
    }

    private static void openStats(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Details", 4);
        frame(gui, 4);
        String stateColor = ph.getOrDefault("state_color", "f");

        CMIGuiButton core = new CMIGuiButton(11, Material.NETHER_STAR, "&f&lProgression");
        core.lockField();
        core.addLore(List.of(
                "",
                "&7Active      &f" + ph.getOrDefault("active", "?"),
                "&7Available   &f" + ph.getOrDefault("available", "?"),
                "&7Calculated  &f" + ph.getOrDefault("calculated", "?"),
                "&7Purchased   &f" + ph.getOrDefault("purchased", "?"),
                "&7Personal    &f" + ph.getOrDefault("personal_max", "?"),
                "",
                "&7State  &" + stateColor + ph.getOrDefault("state", "?"),
                "&7Tier   &f" + ph.getOrDefault("tier", "?")
        ));
        gui.addButton(core);

        CMIGuiButton team = new CMIGuiButton(13, Material.COMPASS, "&f&lTeam");
        team.lockField();
        team.addLore(List.of(
                "",
                "&7Name    &f" + ph.getOrDefault("team_name", "?"),
                "&7Mode    &f" + ph.getOrDefault("team_mode", "?"),
                "&7Online  &f" + ph.getOrDefault("team_size", "0"),
                "&7Source  &f" + ph.getOrDefault("team_source", "?"),
                "",
                "&7Bonus         &f" + ph.getOrDefault("team_bonus", "?"),
                "&7Contribution  &f" + ph.getOrDefault("team_contrib", "?")
        ));
        gui.addButton(team);

        CMIGuiButton account = new CMIGuiButton(15, Material.GOLD_INGOT, "&f&lAccount");
        account.lockField();
        account.addLore(List.of(
                "",
                "&7Balance  &f" + ph.getOrDefault("balance", "?"),
                "&7Level    &f" + ph.getOrDefault("level", "?"),
                "&7Prestige &f" + ph.getOrDefault("prestige", "?"),
                "",
                "&7Titles",
                "&f" + ph.getOrDefault("titles", "none")
        ));
        gui.addButton(account);

        gui.addButton(pageBtn(27, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(closeBtn(35));
        gui.open();
    }

    private static List<String> statusLore(Map<String, String> ph, String stateColor) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&f" + ph.getOrDefault("active", "?") + " &8/ &7" + ph.getOrDefault("available", "?"));
        lore.add("&7Active / Available");
        lore.add("");
        lore.add("&7State  &" + stateColor + ph.getOrDefault("state", "?"));
        lore.add("&7Tier   &f" + ph.getOrDefault("tier", "?"));
        lore.add("&7Wallet &f" + ph.getOrDefault("balance", "?"));
        lore.add("");
        lore.add("&8Calc " + ph.getOrDefault("calculated", "?")
                + "  ·  Bought " + ph.getOrDefault("purchased", "?"));
        return lore;
    }

    private static void frame(CMIGui gui, int rows) {
        int size = rows * 9;
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            CMIGuiButton pane = new CMIGuiButton(i, edge ? ACCENT : FILL, " ");
            pane.lockField();
            gui.addButton(pane);
        }
    }

    private static CMIGui base(Player player, String title, int rows) {
        CMIGui gui = new CMIGui(player);
        gui.setTitle(title);
        gui.setInvSize(rows);
        gui.addLock(InvType.Gui);
        return gui;
    }

    private static CMIGuiButton actionBtn(int slot, Material mat, String name, String action, String arg, List<String> tip) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(tip);
        btn.addLore(lore);
        btn.addCommand("difficulty do " + action + " " + arg);
        return btn;
    }

    private static CMIGuiButton pageBtn(int slot, Material mat, String name, String page, String tip) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        btn.addLore(List.of("", tip));
        btn.addCommand("difficulty do page " + page);
        return btn;
    }

    private static CMIGuiButton closeBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BARRIER, "&cClose");
        btn.lockField();
        btn.setCloseInv(true);
        return btn;
    }

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception e) {
            return 0L;
        }
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
}
