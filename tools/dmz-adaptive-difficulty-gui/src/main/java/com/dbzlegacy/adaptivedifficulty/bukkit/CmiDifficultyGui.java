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
 * CMILib inventory GUI — tier-centric Adaptive Difficulty.
 * Pages: Hub · Buy Tier · Lower Tier · Titles · Details.
 * Teams shown as WIP only (no actions).
 */
public final class CmiDifficultyGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;
    /** Centered tier row slots in a 5-row chest (columns 1–7 of row 3). */
    private static final int[] TIER_SLOTS = {19, 20, 21, 22, 23, 24, 25};

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
                case "adjust", "change", "set", "lower" -> openLower(player);
                case "buy", "purchase", "unlock" -> openBuy(player);
                case "titles", "title" -> openTitles(player);
                case "team", "teams" -> openTeamsWip(player);
                case "stats", "statistics", "details" -> openStats(player);
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
        CMIGui gui = base(player, "&8Adaptive Difficulty", 4);

        boolean systemOn = !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "true"));
        boolean allowed = !"false".equalsIgnoreCase(ph.getOrDefault("player_allowed", "true"));
        String stateColor = ph.getOrDefault("state_color", "f");
        CMIGuiButton status = new CMIGuiButton(13, Material.NETHER_STAR,
                !systemOn ? "&c&lSYSTEM DISABLED"
                        : !allowed ? "&e&lWHITELIST ONLY"
                        : "&f&lAdaptive Difficulty");
        status.lockField();
        if (!systemOn || !allowed) {
            status.addLore(unavailableLore(player, systemOn));
            gui.addButton(status);
            gui.addButton(actionBtn(31, Material.SUNFLOWER, "&7Refresh", "refresh", "0", "main",
                    List.of("&7Reload this menu")));
            gui.addButton(closeBtn(35));
            fillEmpty(gui, 4);
            gui.open();
            return;
        }
        status.addLore(statusLore(ph, stateColor));
        gui.addButton(status);

        gui.addButton(pageBtn(19, Material.GOLD_INGOT, "&eBuy Tier", "buy",
                "&7Purchase a higher Unlock Tier",
                "&8Exact Ancient Coins · scales with DMZ level"));
        gui.addButton(pageBtn(21, Material.WHITE_CONCRETE, "&fLower Tier", "lower",
                "&7Select a lower unlocked tier",
                "&8Or reset to None · always free"));
        gui.addButton(pageBtn(23, Material.NAME_TAG, "&dTitles", "titles",
                "&7Equip difficulty titles",
                "&8Higher CR / kill requirements"));
        gui.addButton(pageBtn(25, Material.COMPASS, "&8Teams &7(WIP)", "team",
                "&7Not available yet",
                "&ePersonal difficulty only"));

        gui.addButton(pageBtn(30, Material.BOOK, "&fDetails", "stats",
                "&7Coins, progression, title"));
        gui.addButton(actionBtn(31, Material.SUNFLOWER, "&7Refresh", "refresh", "0", "main",
                List.of("&7Reload this menu")));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        gui.open();
    }

    private static void openBuy(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Buy Higher Tier", 5);

        CMIGuiButton info = new CMIGuiButton(4, Material.GOLD_INGOT, "&e&lBuy Higher Tier");
        info.lockField();
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"));
        infoLore.add("&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
        infoLore.add("&7DMZ Level &f" + ph.getOrDefault("level", "?")
                + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"));
        infoLore.add("");
        infoLore.addAll(coinLore(ph));
        infoLore.add("");
        infoLore.add("&8Costs scale with your DMZ level");
        infoLore.add("&8Exact coins only — no overpay / change");
        info.addLore(infoLore);
        gui.addButton(info);

        placeTierButtons(gui, ph, true);
        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(pageBtn(40, Material.WHITE_CONCRETE, "&fLower Tier", "lower",
                "&7Select a lower unlocked tier"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openLower(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Lower Difficulty Tier", 5);

        CMIGuiButton status = new CMIGuiButton(4, Material.NETHER_STAR, "&f&lLower Tier");
        status.lockField();
        status.addLore(List.of(
                "",
                "&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7Combat Rating &f" + ph.getOrDefault("combat_rating", "?"),
                "",
                "&8Select a lower unlocked tier",
                "&8Or reset to None — always free"
        ));
        gui.addButton(status);

        gui.addButton(actionBtn(8, Material.BARRIER, "&cReset to None", "lower_tier", "0", "lower",
                List.of("&7Clear active tier", "&8Unlocks & coins kept", "&8Always free")));

        placeTierButtons(gui, ph, false);
        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(pageBtn(40, Material.GOLD_INGOT, "&eBuy Tier", "buy",
                "&7Purchase a higher Unlock Tier"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openTitles(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Difficulty Titles", 5);

        CMIGuiButton info = new CMIGuiButton(4, Material.NAME_TAG, "&d&lTitles");
        info.lockField();
        info.addLore(List.of(
                "",
                "&7Equipped &e" + blankAsNone(ph.getOrDefault("active_title", "")),
                "&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7CR &f" + ph.getOrDefault("combat_rating", "?"),
                "",
                "&8Tier titles need active tier + higher DMZ/Prestige",
                "&8Combat titles need harder kill feats"
        ));
        gui.addButton(info);
        gui.addButton(actionBtn(8, Material.BARRIER, "&cClear Title", "clear_title", "0", "titles",
                List.of("&7Unequip your title")));

        String[] ids = {
                "t1_awakened", "t2_enhanced", "t3_elite", "t4_advanced",
                "t5_master", "t6_legendary", "t7_god",
                "boss_slayer", "elite_hunter", "ascendant"
        };
        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND,
                Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR,
                Material.WITHER_SKELETON_SKULL, Material.DRAGON_HEAD, Material.ENCHANTED_GOLDEN_APPLE
        };
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 29, 31, 33};
        String equipped = ph.getOrDefault("active_title_id", "");
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            boolean earned = "true".equalsIgnoreCase(ph.getOrDefault("title_" + id + "_earned", "false"));
            String name = ph.getOrDefault("title_" + id + "_name", id);
            String tip = ph.getOrDefault("title_" + id + "_req", "");
            boolean isEquipped = id.equalsIgnoreCase(equipped);
            String title = isEquipped ? "&a● " + name
                    : earned ? "&e" + name
                    : "&8" + name;
            List<String> lore = new ArrayList<>();
            lore.add("&7" + tip);
            if (isEquipped) {
                lore.add("&aCurrently equipped &8· click to unequip");
            } else if (earned) {
                lore.add("&aUnlocked &8· click to equip");
            } else {
                lore.add("&cLocked");
            }
            if (earned) {
                gui.addButton(actionBtn(slots[i], mats[i], title, "equip_title", id, "titles", lore));
            } else {
                CMIGuiButton locked = new CMIGuiButton(slots[i], mats[i], title);
                locked.lockField();
                locked.addLore(lore);
                gui.addButton(locked);
            }
        }

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openTeamsWip(Player player) {
        CMIGui gui = base(player, "&8Teams (WIP)", 3);
        CMIGuiButton info = new CMIGuiButton(13, Material.COMPASS, "&8&lTeams — Work in Progress");
        info.lockField();
        info.addLore(List.of(
                "",
                "&7Team difficulty is not available yet.",
                "&eDifficulty is personal / individual only.",
                "",
                "&8No team actions can be taken from this menu."
        ));
        gui.addButton(info);
        gui.addButton(pageBtn(18, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(26));
        fillEmpty(gui, 3);
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
                "&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7Combat CR   &f" + ph.getOrDefault("combat_rating", "?"),
                "&7State       &" + stateColor + ph.getOrDefault("state", "?"),
                "",
                "&7DMZ &f" + ph.getOrDefault("level", "?")
                        + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"),
                "&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"),
                "&7Title &e" + blankAsNone(ph.getOrDefault("active_title", ""))
        ));
        gui.addButton(core);

        CMIGuiButton team = new CMIGuiButton(13, Material.COMPASS, "&8&lTeams (WIP)");
        team.lockField();
        team.addLore(List.of(
                "",
                "&7Not available yet.",
                "&ePersonal difficulty only."
        ));
        gui.addButton(team);

        CMIGuiButton account = new CMIGuiButton(15, Material.GOLD_INGOT, "&f&lAncient Coins");
        account.lockField();
        List<String> coin = new ArrayList<>();
        coin.add("");
        coin.addAll(coinLore(ph));
        coin.add("");
        coin.add("&8Tier purchases use exact Ancient Coins");
        account.addLore(coin);
        gui.addButton(account);

        gui.addButton(pageBtn(27, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        gui.open();
    }

    private static void placeTierButtons(CMIGui gui, Map<String, String> ph, boolean buyMode) {
        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT,
                Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR
        };
        int activeTier = parseInt(ph.getOrDefault("active_tier", "0"));
        int highest = parseInt(ph.getOrDefault("highest_unlocked", "0"));
        for (int t = 1; t <= 7; t++) {
            int slot = TIER_SLOTS[t - 1];
            String cost = ph.getOrDefault("tier_" + t + "_cost", "?");
            String name = ph.getOrDefault("tier_" + t + "_name", "T" + t);
            boolean unlocked = highest >= t;
            boolean active = activeTier == t;
            List<String> lore = new ArrayList<>();
            lore.add("&7" + name);
            if (buyMode) {
                lore.add("&7Cost &e" + cost);
                lore.add("&8Scaled for your DMZ level");
            }
            if (active) {
                lore.add("&aCurrently active");
            } else if (buyMode && unlocked) {
                lore.add("&aUnlocked &8· click to purchase");
                lore.add("&8Exact Ancient Coins only");
            } else if (!buyMode && unlocked && t < activeTier) {
                lore.add("&aOwned &8· click to lower here");
            } else if (!unlocked) {
                lore.add("&cLocked &8· need DMZ level or Prestige " + t);
            } else if (!buyMode) {
                lore.add("&8Higher than current — use Buy");
            }

            String title;
            if (active) {
                title = "&a● T" + t + " Active";
            } else if (buyMode) {
                title = unlocked ? "&eBuy T" + t : "&8Locked T" + t;
            } else {
                title = (unlocked && t < activeTier) ? "&fLower to T" + t
                        : unlocked ? "&8T" + t : "&8Locked T" + t;
            }

            boolean clickable = buyMode
                    ? (unlocked && !active)
                    : (unlocked && t < activeTier);
            if (clickable) {
                String action = buyMode ? "activate" : "lower_tier";
                String page = buyMode ? "buy" : "lower";
                gui.addButton(actionBtn(slot, mats[t - 1], title, action, String.valueOf(t), page, lore));
            } else {
                CMIGuiButton locked = new CMIGuiButton(slot, mats[t - 1], title);
                locked.lockField();
                locked.addLore(lore);
                gui.addButton(locked);
            }
        }
    }

    /** Clean copy for players; command tips only for staff. */
    private static List<String> unavailableLore(Player player, boolean systemOn) {
        boolean staff = ForgeBridge.isStaff(player);
        if (!systemOn) {
            if (staff) {
                return List.of("", "&cAdaptive Difficulty is off",
                        "&7No scaling, coins, or purchases",
                        "&8Re-enable: &f/difficulty admin on");
            }
            return List.of("", "&cAdaptive Difficulty is off", "&7Please try again later");
        }
        if (staff) {
            return List.of("", "&eTesting whitelist is on", "&7You are not on the whitelist",
                    "&8Add: &f/difficulty admin whitelist add <you>");
        }
        return List.of("", "&eNot available right now", "&7Ask an admin if you need access");
    }

    private static List<String> statusLore(Map<String, String> ph, String stateColor) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"));
        lore.add("&7State  &" + stateColor + ph.getOrDefault("state", "?"));
        lore.add("&7CR     &f" + ph.getOrDefault("combat_rating", "?"));
        lore.add("&7Title  &e" + blankAsNone(ph.getOrDefault("active_title", "")));
        lore.add("");
        lore.addAll(coinLore(ph));
        lore.add("");
        lore.add("&7DMZ &f" + ph.getOrDefault("level", "?")
                + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"));
        lore.add("&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
        lore.add("&8Buy a higher tier · Lower to step down");
        return lore;
    }

    private static List<String> coinLore(Map<String, String> ph) {
        return List.of(
                "&6Ancient Coins",
                "&eCopper &f" + ph.getOrDefault("coins_copper", "0")
                        + "  &eIron &f" + ph.getOrDefault("coins_iron", "0")
                        + "  &eGold &f" + ph.getOrDefault("coins_gold", "0"),
                "&eEmerald &f" + ph.getOrDefault("coins_emerald", "0")
                        + "  &eDiamond &f" + ph.getOrDefault("coins_diamond", "0")
                        + "  &eNetherite &f" + ph.getOrDefault("coins_netherite", "0"),
                "&6Total &f" + ph.getOrDefault("ancient_coins", "0") + " AC"
        );
    }

    private static String blankAsNone(String value) {
        return value == null || value.isBlank() ? "None" : value;
    }

    private static int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return 0;
        }
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
}
