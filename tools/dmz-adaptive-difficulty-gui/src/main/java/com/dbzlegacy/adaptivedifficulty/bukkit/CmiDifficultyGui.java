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
                case "titles", "title" -> openTitles(player);
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
        CMIGuiButton status = new CMIGuiButton(13, Material.NETHER_STAR, "&f&lDifficulty V3");
        status.lockField();
        status.addLore(statusLore(ph, stateColor));
        gui.addButton(status);

        gui.addButton(pageBtn(20, Material.GOLD_INGOT, "&eBuy Tier", "buy",
                "&7Purchase a difficulty tier",
                "&8Spend Ancient Coins from inventory"));
        gui.addButton(pageBtn(22, Material.WHITE_CONCRETE, "&fLower", "adjust",
                "&7Lower / clear active difficulty",
                "&8Free — buy tiers to raise"));
        gui.addButton(actionBtn(24, Material.COMPASS, "&bTeam", "team", "0", "main",
                List.of("&7Cycle team scaling", "&8" + ph.getOrDefault("team_mode", "?"))));

        gui.addButton(pageBtn(27, Material.EXPERIENCE_BOTTLE, "&fRewards", "rewards",
                "&7Kill rewards (no TP)"));
        gui.addButton(pageBtn(28, Material.IRON_SWORD, "&fTiers", "tiers",
                "&7Enemy tier unlocks"));
        gui.addButton(pageBtn(29, Material.NAME_TAG, "&eTitles", "titles",
                "&7Unlock & equip titles",
                "&8Equipped &f" + ph.getOrDefault("active_title", "none")));
        gui.addButton(pageBtn(30, Material.BOOK, "&fDetails", "stats",
                "&7Team, full breakdown"));
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
                "&7Active / Ceiling",
                "",
                "&7State   &" + stateColor + ph.getOrDefault("state", "?"),
                "&7Tier    &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7CR      &f" + ph.getOrDefault("combat_rating", "?"),
                "&7Ancient &f" + ph.getOrDefault("balance", "?"),
                "",
                "&8Buy tiers to raise · lower is free"
        ));
        gui.addButton(status);

        gui.addButton(actionBtn(7, Material.WHITE_CONCRETE, "&fReset", "reset", "0", "adjust",
                List.of("&7Clear active tier & level", "&8Unlocks & coins kept", "&8Always free")));

        // Lower only — raising is done by purchasing tiers.
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

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(pageBtn(40, Material.GOLD_INGOT, "&eBuy Tier", "buy",
                "&7Purchase a difficulty tier"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    /** V3 tier purchase page — spend Ancient Coins to buy a tier (sets full difficulty). */
    private static void openBuy(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Buy Difficulty Tier", 5);

        CMIGuiButton info = new CMIGuiButton(4, Material.GOLD_INGOT, "&e&lPurchase Tier");
        info.lockField();
        info.addLore(List.of(
                "",
                "&7Active     &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7Unlocked   &fT" + ph.getOrDefault("highest_unlocked", "0"),
                "&7Inventory  &f" + ph.getOrDefault("balance", "?"),
                "",
                "&8Unlock by DMZ level or Prestige",
                "&8Click a tier to buy it with Ancient Coins",
                "&8No change — exact coins from inventory",
                "&8Death clears active tier (unlocks stay)"
        ));
        gui.addButton(info);

        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT,
                Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR
        };
        int activeTier = 0;
        try {
            activeTier = Integer.parseInt(ph.getOrDefault("active_tier", "0"));
        } catch (Exception ignored) {
        }
        int highest = 0;
        try {
            highest = Integer.parseInt(ph.getOrDefault("highest_unlocked", "0"));
        } catch (Exception ignored) {
        }
        for (int t = 1; t <= 7; t++) {
            int slot = 18 + (t - 1);
            String cost = ph.getOrDefault("tier_" + t + "_cost", ph.getOrDefault("unlock_tier_" + t + "_cost", "?"));
            String max = ph.getOrDefault("unlock_tier_" + t + "_max", "?");
            boolean unlocked = highest >= t;
            boolean active = activeTier == t;
            String title = active ? "&a● T" + t + " Active"
                    : unlocked ? "&eBuy T" + t
                    : "&8Locked T" + t;
            List<String> lore = new ArrayList<>();
            lore.add("&7Difficulty tier &f" + t);
            lore.add("&7Max difficulty &f" + max);
            lore.add("&7Cost &e" + cost);
            if (active) {
                lore.add("&aCurrently active");
            } else if (unlocked) {
                lore.add("&aUnlocked &8· click to purchase");
                lore.add("&8Sets difficulty to this tier's max");
            } else {
                lore.add("&cLocked &8· need DMZ level or Prestige " + t);
            }
            lore.add("&8Current active &f" + ph.getOrDefault("active_tier", "0"));
            if (unlocked && !active) {
                gui.addButton(actionBtn(slot, mats[t - 1], title, "activate", String.valueOf(t), "buy", lore));
            } else {
                CMIGuiButton locked = new CMIGuiButton(slot, mats[t - 1], title);
                locked.lockField();
                locked.addLore(lore);
                gui.addButton(locked);
            }
        }

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(pageBtn(40, Material.WHITE_CONCRETE, "&fLower", "adjust",
                "&7Lower / clear active"));
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
                "&7Active   &f" + ph.getOrDefault("active", "?"),
                "&7Tier     &f" + ph.getOrDefault("tier", "?"),
                "&7Reward × &a" + ph.getOrDefault("reward_mult", "1.00"),
                "",
                "&8XP & drop odds scale with difficulty",
                "&8(1 + active / rewardScaling)",
                "",
                "&8Training Points / Potential",
                "&8are not touched by this mod",
                "",
                "&7Also: capsules · titles"
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
                "&7Ceiling     &f" + ph.getOrDefault("available", "?"),
                "&7Combat CR   &f" + ph.getOrDefault("combat_rating", "?"),
                "&7Personal    &f" + ph.getOrDefault("personal_max", "?"),
                "",
                "&7State  &" + stateColor + ph.getOrDefault("state", "?"),
                "&7Tier   &f" + ph.getOrDefault("active_tier_name", ph.getOrDefault("tier", "?"))
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

        CMIGuiButton account = new CMIGuiButton(15, Material.GOLD_INGOT, "&f&lAncient Coins");
        account.lockField();
        account.addLore(List.of(
                "",
                "&7Balance &f" + ph.getOrDefault("balance", "?"),
                "&7Level   &f" + ph.getOrDefault("level", "?"),
                "&7Prestige &f" + ph.getOrDefault("prestige", "?"),
                "",
                "&7Title   &e" + ph.getOrDefault("active_title", "none"),
                "&7Owned   &f" + ph.getOrDefault("titles_count", "0"),
                "",
                "&8Activation & upgrades use",
                "&8Ancient Coins only"
        ));
        gui.addButton(account);

        gui.addButton(pageBtn(27, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(pageBtn(31, Material.NAME_TAG, "&eTitles", "titles",
                "&7Unlock & equip titles"));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        gui.open();
    }

    private static void openTitles(Player player) {
        ForgeBridge.syncTitles(player);
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Titles", 6);

        CMIGuiButton header = new CMIGuiButton(4, Material.NAME_TAG, "&e&lTitles");
        header.lockField();
        header.addLore(List.of(
                "",
                "&7Equipped  &e" + ph.getOrDefault("active_title", "none"),
                "&7Owned     &f" + ph.getOrDefault("titles_count", "0"),
                "&7Active    &f" + ph.getOrDefault("active", "?"),
                "",
                "&8Tier titles unlock with difficulty",
                "&8Combat titles unlock from feats",
                "&8Click an unlocked title to equip"
        ));
        gui.addButton(header);
        gui.addButton(actionBtn(8, Material.BARRIER, "&fUnequip", "clear_title", "0", "titles",
                List.of("&7Clear equipped title")));

        String[][] tierTitles = {
                {"awakened", "Awakened"}, {"enhanced", "Enhanced"}, {"elite", "Elite"},
                {"advanced", "Advanced"}, {"master", "Master"}, {"legendary", "Legendary"},
                {"god", "God"}, {"divine", "Divine"}, {"impossible", "Impossible"},
                {"transcendent", "Transcendent"}, {"eternal", "Eternal"}, {"mythic", "Mythic"},
                {"omega", "Omega"}, {"absolute", "Absolute"}, {"apex", "Apex"},
                {"zenith", "Zenith"}
        };
        int[] tierSlots = {
                9, 10, 11, 12, 13, 14, 15, 16,
                18, 19, 20, 21, 22, 23, 24, 25
        };
        String equippedId = ph.getOrDefault("active_title_id", "");
        for (int i = 0; i < tierTitles.length && i < tierSlots.length; i++) {
            String id = tierTitles[i][0];
            String name = tierTitles[i][1];
            boolean unlocked = "1".equals(ph.getOrDefault("title_" + id, "0"));
            boolean on = id.equalsIgnoreCase(equippedId);
            long thr = parseLong(ph.getOrDefault("tier_" + id, defaultsTier(id)));
            Material mat = on ? Material.GOLD_BLOCK
                    : unlocked ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
            String label = (on ? "&e" : unlocked ? "&a" : "&8") + name;
            List<String> tip = new ArrayList<>();
            tip.add("");
            tip.add("&7Tier title");
            tip.add("&7Threshold  &f" + thr);
            if (on) {
                tip.add("&eEquipped &8· click to unequip");
            } else if (unlocked) {
                tip.add("&aUnlocked &8· click to equip");
            } else {
                tip.add("&8Locked");
            }
            if (unlocked) {
                gui.addButton(actionBtn(tierSlots[i], mat, label, "equip_title", id, "titles", tip));
            } else {
                CMIGuiButton locked = new CMIGuiButton(tierSlots[i], mat, label);
                locked.lockField();
                locked.addLore(tip);
                gui.addButton(locked);
            }
        }

        String[][] combatTitles = {
                {"boss_slayer", "Boss Slayer", "Kill a boss at Master+"},
                {"legendary_hunter", "Legendary Hunter", "Kill an elite at Legendary+"},
                {"god_challenger", "God Challenger", "Get a kill at God+"}
        };
        int[] combatSlots = {37, 39, 41};
        for (int i = 0; i < combatTitles.length; i++) {
            String id = combatTitles[i][0];
            String name = combatTitles[i][1];
            String how = combatTitles[i][2];
            boolean unlocked = "1".equals(ph.getOrDefault("title_" + id, "0"));
            boolean on = id.equalsIgnoreCase(equippedId);
            Material mat = on ? Material.NETHER_STAR
                    : unlocked ? Material.DIAMOND : Material.COAL;
            String label = (on ? "&e" : unlocked ? "&a" : "&8") + name;
            List<String> tip = new ArrayList<>();
            tip.add("");
            tip.add("&7Combat title");
            tip.add("&8" + how);
            if (on) {
                tip.add("&eEquipped &8· click to unequip");
            } else if (unlocked) {
                tip.add("&aUnlocked &8· click to equip");
            } else {
                tip.add("&8Locked");
            }
            if (unlocked) {
                gui.addButton(actionBtn(combatSlots[i], mat, label, "equip_title", id, "titles", tip));
            } else {
                CMIGuiButton locked = new CMIGuiButton(combatSlots[i], mat, label);
                locked.lockField();
                locked.addLore(tip);
                gui.addButton(locked);
            }
        }

        gui.addButton(pageBtn(45, Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        gui.addButton(closeBtn(53));
        fillEmpty(gui, 6);
        gui.open();
    }

    private static List<String> statusLore(Map<String, String> ph, String stateColor) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&f" + ph.getOrDefault("active", "?") + " &8/ &7" + ph.getOrDefault("available", "?"));
        lore.add("&7Active Difficulty / Ceiling");
        lore.add("");
        lore.add("&7State  &" + stateColor + ph.getOrDefault("state", "?"));
        lore.add("&7Tier   &f" + ph.getOrDefault("active_tier_name", ph.getOrDefault("tier", "?")));
        lore.add("&7CR     &f" + ph.getOrDefault("combat_rating", ph.getOrDefault("calculated", "?")));
        lore.add("&7Title  &e" + ph.getOrDefault("active_title", "none"));
        lore.add("&7Ancient &f" + ph.getOrDefault("balance", "?"));
        lore.add("");
        lore.add("&7DMZ &f" + ph.getOrDefault("level", "?")
                + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"));
        lore.add("&7Team bonus  &f" + ph.getOrDefault("team_bonus", "0"));
        lore.add("&7Team contrib &f" + ph.getOrDefault("team_contrib", "0"));
        lore.add("&8Buy a tier in the Tier menu to raise");
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
