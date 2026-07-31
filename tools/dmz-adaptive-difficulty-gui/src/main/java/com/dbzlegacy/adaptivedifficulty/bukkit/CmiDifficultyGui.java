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
 * Clean CMILib inventory GUI for adaptive difficulty V3.
 * <p>
 * Pages: Hub · Buy Tier · Lower/Reset · Details. No titles, no +difficulty upgrades,
 * no Zenith/DifficultyTier ladder.
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
                case "stats", "statistics", "details" -> openStats(player);
                default -> openMain(player);
            }
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cCMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    /** Hub: status + Buy Tier / Lower / Team / Details. */
    private static void openMain(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Difficulty", 4);

        boolean systemOn = !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "true"));
        boolean allowed = !"false".equalsIgnoreCase(ph.getOrDefault("player_allowed", "true"));
        String stateColor = ph.getOrDefault("state_color", "f");
        CMIGuiButton status = new CMIGuiButton(13, Material.NETHER_STAR,
                !systemOn ? "&c&lSYSTEM DISABLED"
                        : !allowed ? "&e&lWHITELIST ONLY"
                        : "&f&lDifficulty V3");
        status.lockField();
        if (!systemOn || !allowed) {
            status.addLore(!systemOn
                    ? List.of(
                    "",
                    "&cAdaptive Difficulty is off",
                    "&7An admin disabled the system",
                    "&8No scaling, coins, or purchases",
                    "&8Re-enable: &f/difficulty admin on"
            )
                    : List.of(
                    "",
                    "&eTesting whitelist is on",
                    "&7You are not on the whitelist",
                    "&8Ask an admin:",
                    "&f/difficulty admin whitelist add <you>"
            ));
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

        gui.addButton(pageBtn(20, Material.GOLD_INGOT, "&eBuy Tier", "buy",
                "&7Purchase a difficulty tier",
                "&8Spend Ancient Coins from inventory"));
        gui.addButton(pageBtn(22, Material.WHITE_CONCRETE, "&fLower", "adjust",
                "&7Lower / clear active difficulty",
                "&8Free — buy tiers to raise"));
        gui.addButton(actionBtn(24, Material.COMPASS, "&bTeam", "team", "0", "main",
                List.of("&7Cycle team scaling", "&8" + ph.getOrDefault("team_mode", "?"))));

        gui.addButton(pageBtn(30, Material.BOOK, "&fDetails", "stats",
                "&7Team, full breakdown"));
        gui.addButton(actionBtn(31, Material.SUNFLOWER, "&7Refresh", "refresh", "0", "main",
                List.of("&7Reload this menu")));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        gui.open();
    }

    /** Lower / reset page — raising is done by purchasing tiers. */
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

    /** V3 UnlockTier purchase page — Ancient Coins set full tier difficulty. */
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
                "&8Sets full tier difficulty · no change",
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
                "&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"),
                "",
                "&8Tier purchases use Ancient Coins only"
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
        lore.add("&7Active Difficulty / Ceiling");
        lore.add("");
        lore.add("&7State  &" + stateColor + ph.getOrDefault("state", "?"));
        lore.add("&7Tier   &f" + ph.getOrDefault("active_tier_name", ph.getOrDefault("tier", "?")));
        lore.add("&7CR     &f" + ph.getOrDefault("combat_rating", ph.getOrDefault("calculated", "?")));
        lore.add("&7Ancient &f" + ph.getOrDefault("balance", "?"));
        lore.add("");
        lore.add("&7DMZ &f" + ph.getOrDefault("level", "?")
                + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"));
        lore.add("&7Team &f" + ph.getOrDefault("team_mode", "?")
                + "  &7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
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
}
