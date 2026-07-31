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

/**
 * Clean CMILib inventory GUI for adaptive difficulty.
 * <p>
 * Important: CMILib {@code addButton} does <b>not</b> overwrite an occupied slot —
 * it silently skips (or searches forward). Always place real buttons first, then
 * fill remaining empty slots with glass.
 */
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
                case "adjust", "change", "set" -> openAdjust(player);
                case "buy", "purchase", "unlock" -> openBuy(player);
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

    /** Hub: status + navigation into Adjust / Buy / info pages. */
    private static void openMain(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Difficulty", 4);

        String stateColor = ph.getOrDefault("state_color", "f");
        CMIGuiButton status = new CMIGuiButton(13, Material.NETHER_STAR, "&f&lDifficulty");
        status.lockField();
        status.addLore(statusLore(ph, stateColor));
        gui.addButton(status);

        gui.addButton(pageBtn(20, Material.LIME_CONCRETE, "&aAdjust", "adjust",
                "&7Raise / lower active difficulty",
                "&8Steps +1 … +100000"));
        gui.addButton(pageBtn(22, Material.GOLD_INGOT, "&eBuy Max", "buy",
                "&7Unlock more available max",
                "&8Paid from inventory coins"));
        gui.addButton(actionBtn(24, Material.COMPASS, "&bTeam", "team", "0", "main",
                List.of("&7Cycle team scaling", "&8" + ph.getOrDefault("team_mode", "?"))));

        gui.addButton(pageBtn(27, Material.EXPERIENCE_BOTTLE, "&fRewards", "rewards",
                "&7TP multiplier details"));
        gui.addButton(pageBtn(28, Material.IRON_SWORD, "&fTiers", "tiers",
                "&7Enemy tier unlocks"));
        gui.addButton(pageBtn(29, Material.BOOK, "&fDetails", "stats",
                "&7Team, titles, full breakdown"));
        gui.addButton(actionBtn(31, Material.SUNFLOWER, "&7Refresh", "refresh", "0", "main",
                List.of("&7Reload this menu")));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        gui.open();
    }

    /** Dedicated raise / lower page. */
    private static void openAdjust(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Adjust Difficulty", 5);

        String stateColor = ph.getOrDefault("state_color", "f");
        CMIGuiButton status = new CMIGuiButton(4, Material.NETHER_STAR, "&f&lActive");
        status.lockField();
        status.addLore(List.of(
                "",
                "&f" + ph.getOrDefault("active", "?") + " &8/ &7" + ph.getOrDefault("available", "?"),
                "&7Active / Available",
                "",
                "&7State  &" + stateColor + ph.getOrDefault("state", "?"),
                "&7Inv    &f" + ph.getOrDefault("balance", "?"),
                "",
                "&8Raising costs inventory coins"
        ));
        gui.addButton(status);

        gui.addButton(actionBtn(6, Material.ORANGE_CONCRETE, "&6Max", "set_max", "0", "adjust",
                List.of("&7Jump to available max", "&f" + ph.getOrDefault("available", "?"),
                        "&8Cost &e" + ph.getOrDefault("cost_max", "?"),
                        "&8Paid from inventory coins")));
        gui.addButton(actionBtn(7, Material.WHITE_CONCRETE, "&fReset", "reset", "0", "adjust",
                List.of("&7Set active to &f0", "&8Purchased max kept", "&8Always free")));

        // Lower row (large → small)
        gui.addButton(actionBtn(19, Material.RED_CONCRETE, "&c−100000", "down", "100000", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        gui.addButton(actionBtn(20, Material.RED_WOOL, "&c−10000", "down", "10000", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        gui.addButton(actionBtn(21, Material.RED_TERRACOTTA, "&c−1000", "down", "1000", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        gui.addButton(actionBtn(22, Material.PINK_CONCRETE, "&c−100", "down", "100", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        gui.addButton(actionBtn(23, Material.PINK_TERRACOTTA, "&c−25", "down", "25", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        gui.addButton(actionBtn(24, Material.MAGENTA_CONCRETE, "&c−5", "down", "5", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        gui.addButton(actionBtn(25, Material.MAGENTA_TERRACOTTA, "&c−1", "down", "1", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));

        // Raise row (small → large)
        gui.addButton(actionBtn(28, Material.LIME_TERRACOTTA, "&a+1", "up", "1", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_1", "?"),
                        "&8Paid from inventory coins")));
        gui.addButton(actionBtn(29, Material.LIME_CONCRETE, "&a+5", "up", "5", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_5", "?"),
                        "&8Paid from inventory coins")));
        gui.addButton(actionBtn(30, Material.GREEN_TERRACOTTA, "&a+25", "up", "25", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_25", "?"),
                        "&8Paid from inventory coins")));
        gui.addButton(actionBtn(31, Material.GREEN_CONCRETE, "&a+100", "up", "100", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_100", "?"),
                        "&8Paid from inventory coins")));
        gui.addButton(actionBtn(32, Material.EMERALD, "&a+1000", "up", "1000", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_1000", "?"),
                        "&8Paid from inventory coins")));
        gui.addButton(actionBtn(33, Material.DIAMOND, "&a+10000", "up", "10000", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_10000", "?"),
                        "&8Paid from inventory coins")));
        gui.addButton(actionBtn(34, Material.NETHERITE_INGOT, "&a+100000", "up", "100000", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_100000", "?"),
                        "&8Paid from inventory coins")));

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(pageBtn(40, Material.GOLD_INGOT, "&eBuy Max", "buy", "&7Unlock more available max"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    /** Dedicated buy-max unlock page. */
    private static void openBuy(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Buy Max Difficulty", 5);

        CMIGuiButton info = new CMIGuiButton(4, Material.GOLD_INGOT, "&e&lBuy Max");
        info.lockField();
        info.addLore(List.of(
                "",
                "&7Purchased  &f" + ph.getOrDefault("purchased", "?"),
                "&7Available  &f" + ph.getOrDefault("available", "?"),
                "&7Inv coins  &f" + ph.getOrDefault("balance", "?"),
                "",
                "&8Unlock more max difficulty",
                "&8Paid from inventory coins"
        ));
        gui.addButton(info);

        gui.addButton(actionBtn(19, Material.GOLD_NUGGET, "&e+1 max", "buy", "1", "buy",
                List.of("&7Unlock +1 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_1", "?"))));
        gui.addButton(actionBtn(20, Material.GOLD_NUGGET, "&e+5 max", "buy", "5", "buy",
                List.of("&7Unlock +5 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_5", "?"))));
        gui.addButton(actionBtn(21, Material.GOLD_INGOT, "&e+25 max", "buy", "25", "buy",
                List.of("&7Unlock +25 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_25", "?"))));
        gui.addButton(actionBtn(22, Material.GOLD_BLOCK, "&e+100 max", "buy", "100", "buy",
                List.of("&7Unlock +100 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_100", "?"))));
        gui.addButton(actionBtn(23, Material.EMERALD, "&e+1000 max", "buy", "1000", "buy",
                List.of("&7Unlock +1000 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_1000", "?"))));
        gui.addButton(actionBtn(24, Material.DIAMOND, "&e+10000 max", "buy", "10000", "buy",
                List.of("&7Unlock +10000 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_10000", "?"))));
        gui.addButton(actionBtn(25, Material.NETHERITE_INGOT, "&e+100000 max", "buy", "100000", "buy",
                List.of("&7Unlock +100000 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_100000", "?"))));

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(pageBtn(40, Material.LIME_CONCRETE, "&aAdjust", "adjust", "&7Raise / lower active"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openRewards(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Rewards", 3);

        CMIGuiButton info = new CMIGuiButton(13, Material.EXPERIENCE_BOTTLE, "&f&lRewards");
        info.lockField();
        info.addLore(List.of(
                "",
                "&7Active  &f" + ph.getOrDefault("active", "?"),
                "&7TP mult &a×" + ph.getOrDefault("reward_mult", "?"),
                "",
                "&81 + gain × (Difficulty / Scale)^exp  &8(uncapped)"
        ));
        gui.addButton(info);
        gui.addButton(pageBtn(18, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(closeBtn(26));
        fillEmpty(gui, 3);
        gui.open();
    }

    private static void openTiers(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Enemy Tiers", 6);

        CMIGuiButton header = new CMIGuiButton(4, Material.IRON_SWORD, "&f&lEnemy Tiers");
        header.lockField();
        header.addLore(List.of(
                "",
                "&7Current  &f" + ph.getOrDefault("tier", "?"),
                "&7Active   &f" + ph.getOrDefault("active", "?"),
                "",
                "&8Zenith = theoretical max",
                "&8(level 100k × 10 prestiges)"
        ));
        gui.addButton(header);

        String[][] rows = {
                {"awakened", "Awakened"}, {"enhanced", "Enhanced"}, {"elite", "Elite"},
                {"advanced", "Advanced"}, {"master", "Master"}, {"legendary", "Legendary"},
                {"god", "God"}, {"divine", "Divine"}, {"impossible", "Impossible"},
                {"transcendent", "Transcendent"}, {"eternal", "Eternal"}, {"mythic", "Mythic"},
                {"omega", "Omega"}, {"absolute", "Absolute"}, {"apex", "Apex"},
                {"zenith", "Zenith"}
        };
        long active = parseLong(ph.getOrDefault("active", "0"));
        // Two full rows + partial third
        int[] slots = {
                9, 10, 11, 12, 13, 14, 15, 16,
                18, 19, 20, 21, 22, 23, 24, 25
        };
        for (int i = 0; i < rows.length && i < slots.length; i++) {
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
        gui.addButton(pageBtn(45, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(closeBtn(53));
        fillEmpty(gui, 6);
        gui.open();
    }

    private static void openStats(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Details", 4);
        String stateColor = ph.getOrDefault("state_color", "f");

        CMIGuiButton core = new CMIGuiButton(11, Material.NETHER_STAR, "&f&lProgression");
        core.lockField();
        core.addLore(List.of(
                "",
                "&7Active      &f" + ph.getOrDefault("active", "?"),
                "&7Available   &f" + ph.getOrDefault("available", "?"),
                "&7Theoretical &f" + ph.getOrDefault("calculated", "?"),
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

        CMIGuiButton account = new CMIGuiButton(15, Material.GOLD_INGOT, "&f&lInventory");
        account.lockField();
        account.addLore(List.of(
                "",
                "&7Coins   &f" + ph.getOrDefault("balance", "?"),
                "&7Level   &f" + ph.getOrDefault("level", "?"),
                "&7Prestige &f" + ph.getOrDefault("prestige", "?"),
                "",
                "&7Titles",
                "&f" + ph.getOrDefault("titles", "none"),
                "",
                "&8Difficulty purchases use",
                "&8inventory coins only"
        ));
        gui.addButton(account);

        gui.addButton(pageBtn(27, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
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
        lore.add("&7Inv    &f" + ph.getOrDefault("balance", "?"));
        lore.add("");
        lore.add("&8Theoretical " + ph.getOrDefault("calculated", "?")
                + "  ·  Bought " + ph.getOrDefault("purchased", "?"));
        lore.add("&8Open Adjust to raise / lower");
        return lore;
    }

    private static void fillEmpty(CMIGui gui, int rows) {
        int size = rows * 9;
        Map<Integer, CMIGuiButton> existing = gui.getButtons();
        for (int i = 0; i < size; i++) {
            if (existing != null && existing.containsKey(i)) {
                continue;
            }
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

    private static CMIGuiButton actionBtn(
            int slot, Material mat, String name, String action, String arg, String returnPage, List<String> tip) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(tip);
        btn.addLore(lore);
        btn.addCommand("difficulty do " + action + " " + arg + " " + returnPage);
        return btn;
    }

    private static CMIGuiButton pageBtn(int slot, Material mat, String name, String page, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        btn.addLore(lore);
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
            case "transcendent" -> "250000";
            case "eternal" -> "500000";
            case "mythic" -> "1000000";
            case "omega" -> "2500000";
            case "absolute" -> "5000000";
            case "apex" -> "7500000";
            case "zenith" -> "10000000";
            default -> "0";
        };
    }
}
