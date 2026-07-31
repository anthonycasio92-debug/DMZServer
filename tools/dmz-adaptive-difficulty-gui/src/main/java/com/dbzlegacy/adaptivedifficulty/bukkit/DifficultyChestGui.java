package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Clean Bukkit chest GUI fallback when CMI/CMILib is unavailable.
 * V3 pages: Hub · Buy Tier · Lower/Reset · Details.
 */
public final class DifficultyChestGui implements Listener {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private final AdaptiveDifficultyGuiPlugin plugin;

    public DifficultyChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        Inventory inv = switch (p) {
            case "adjust", "change", "set" -> adjust(player);
            case "buy", "purchase", "unlock" -> buy(player);
            case "stats", "statistics", "details" -> stats(player);
            default -> main(player);
        };
        player.openInventory(inv);
    }

    private Inventory main(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 36, color("&8Difficulty"));
        holder.bind(inv);
        frame(inv, 36);

        boolean systemOn = !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "true"));
        if (!systemOn) {
            inv.setItem(13, item(Material.NETHER_STAR, "&c&lSYSTEM DISABLED", List.of(
                    "",
                    "&cAdaptive Difficulty is off",
                    "&7An admin disabled the system",
                    "&8No scaling, coins, or purchases",
                    "&8Re-enable: &f/difficulty admin on"
            )));
            inv.setItem(31, button(Material.SUNFLOWER, "&7Refresh", "refresh", "0", "main",
                    List.of("&7Reload this menu")));
            inv.setItem(35, closeBtn());
            return inv;
        }

        String stateColor = ph.getOrDefault("state_color", "f");
        inv.setItem(13, item(Material.NETHER_STAR, "&f&lDifficulty V3", statusLore(ph, stateColor)));
        inv.setItem(20, pageBtn(Material.GOLD_INGOT, "&eBuy Tier", "buy",
                "&7Purchase a difficulty tier", "&8Spend Ancient Coins from inventory"));
        inv.setItem(22, pageBtn(Material.WHITE_CONCRETE, "&fLower", "adjust",
                "&7Lower / clear active difficulty", "&8Free — buy tiers to raise"));
        inv.setItem(24, button(Material.COMPASS, "&bTeam", "team", "0", "main",
                List.of("&7Cycle team scaling", "&8" + ph.getOrDefault("team_mode", "?"))));

        inv.setItem(30, pageBtn(Material.BOOK, "&fDetails", "stats", "&7Team, full breakdown"));
        inv.setItem(31, button(Material.SUNFLOWER, "&7Refresh", "refresh", "0", "main",
                List.of("&7Reload this menu")));
        inv.setItem(35, closeBtn());
        return inv;
    }

    private Inventory adjust(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("adjust");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Adjust Difficulty"));
        holder.bind(inv);
        frame(inv, 45);

        String stateColor = ph.getOrDefault("state_color", "f");
        inv.setItem(4, item(Material.NETHER_STAR, "&f&lActive", List.of(
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
        )));
        inv.setItem(7, button(Material.WHITE_CONCRETE, "&fReset", "reset", "0", "adjust",
                List.of("&7Clear active tier & level", "&8Unlocks & coins kept", "&8Always free")));

        inv.setItem(19, button(Material.RED_CONCRETE, "&c−100000", "down", "100000", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        inv.setItem(20, button(Material.RED_WOOL, "&c−10000", "down", "10000", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        inv.setItem(21, button(Material.RED_TERRACOTTA, "&c−1000", "down", "1000", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        inv.setItem(22, button(Material.PINK_CONCRETE, "&c−100", "down", "100", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        inv.setItem(23, button(Material.PINK_TERRACOTTA, "&c−25", "down", "25", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        inv.setItem(24, button(Material.MAGENTA_CONCRETE, "&c−5", "down", "5", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));
        inv.setItem(25, button(Material.MAGENTA_TERRACOTTA, "&c−1", "down", "1", "adjust",
                List.of("&7Lower active difficulty", "&8Always free")));

        inv.setItem(36, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(40, pageBtn(Material.GOLD_INGOT, "&eBuy Tier", "buy", "&7Purchase a difficulty tier"));
        inv.setItem(44, closeBtn());
        return inv;
    }

    private Inventory buy(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("buy");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Buy Difficulty Tier"));
        holder.bind(inv);
        frame(inv, 45);

        inv.setItem(4, item(Material.GOLD_INGOT, "&e&lPurchase Tier", List.of(
                "",
                "&7Active     &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7Unlocked   &fT" + ph.getOrDefault("highest_unlocked", "0"),
                "&7Inventory  &f" + ph.getOrDefault("balance", "?"),
                "",
                "&8Unlock by DMZ level or Prestige",
                "&8Click a tier to buy it with Ancient Coins",
                "&8Sets full tier difficulty · no change",
                "&8Death clears active tier (unlocks stay)"
        )));
        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT,
                Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR
        };
        int activeTier = (int) parseLong(ph.getOrDefault("active_tier", "0"));
        int highest = (int) parseLong(ph.getOrDefault("highest_unlocked", "0"));
        for (int t = 1; t <= 7; t++) {
            String cost = ph.getOrDefault("tier_" + t + "_cost",
                    ph.getOrDefault("unlock_tier_" + t + "_cost", "?"));
            String max = ph.getOrDefault("unlock_tier_" + t + "_max", "?");
            boolean unlocked = highest >= t;
            boolean active = activeTier == t;
            String title = active ? "&a● T" + t + " Active"
                    : unlocked ? "&eBuy T" + t
                    : "&8Locked T" + t;
            List<String> tip = new ArrayList<>();
            tip.add("&7Difficulty tier &f" + t);
            tip.add("&7Max difficulty &f" + max);
            tip.add("&7Cost &e" + cost);
            if (active) {
                tip.add("&aCurrently active");
            } else if (unlocked) {
                tip.add("&aUnlocked &8· click to purchase");
                tip.add("&8Sets difficulty to this tier's max");
            } else {
                tip.add("&cLocked &8· need DMZ level or Prestige " + t);
            }
            tip.add("&8Current active &f" + ph.getOrDefault("active_tier", "0"));
            if (unlocked && !active) {
                inv.setItem(18 + (t - 1), button(mats[t - 1], title, "activate", String.valueOf(t), "buy", tip));
            } else {
                inv.setItem(18 + (t - 1), item(mats[t - 1], title, prependBlank(tip)));
            }
        }

        inv.setItem(36, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(40, pageBtn(Material.WHITE_CONCRETE, "&fLower", "adjust", "&7Lower / clear active"));
        inv.setItem(44, closeBtn());
        return inv;
    }

    private Inventory stats(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("stats");
        Inventory inv = Bukkit.createInventory(holder, 36, color("&8Details"));
        holder.bind(inv);
        frame(inv, 36);
        String stateColor = ph.getOrDefault("state_color", "f");

        inv.setItem(11, item(Material.NETHER_STAR, "&f&lProgression", List.of(
                "",
                "&7Active      &f" + ph.getOrDefault("active", "?"),
                "&7Ceiling     &f" + ph.getOrDefault("available", "?"),
                "&7Combat CR   &f" + ph.getOrDefault("combat_rating", "?"),
                "&7Personal    &f" + ph.getOrDefault("personal_max", "?"),
                "",
                "&7State  &" + stateColor + ph.getOrDefault("state", "?"),
                "&7Tier   &f" + ph.getOrDefault("active_tier_name", ph.getOrDefault("tier", "?"))
        )));
        inv.setItem(13, item(Material.COMPASS, "&f&lTeam", List.of(
                "",
                "&7Name    &f" + ph.getOrDefault("team_name", "?"),
                "&7Mode    &f" + ph.getOrDefault("team_mode", "?"),
                "&7Online  &f" + ph.getOrDefault("team_size", "0"),
                "&7Source  &f" + ph.getOrDefault("team_source", "?"),
                "",
                "&7Bonus         &f" + ph.getOrDefault("team_bonus", "?"),
                "&7Contribution  &f" + ph.getOrDefault("team_contrib", "?")
        )));
        inv.setItem(15, item(Material.GOLD_INGOT, "&f&lAncient Coins", List.of(
                "",
                "&7Balance &f" + ph.getOrDefault("balance", "?"),
                "&7Level   &f" + ph.getOrDefault("level", "?"),
                "&7Prestige &f" + ph.getOrDefault("prestige", "?"),
                "&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"),
                "",
                "&8Tier purchases use Ancient Coins only"
        )));
        inv.setItem(27, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(35, closeBtn());
        return inv;
    }

    private static List<String> prependBlank(List<String> tip) {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(tip);
        return out;
    }

    private static List<String> statusLore(Map<String, String> ph, String stateColor) {
        return List.of(
                "",
                "&f" + ph.getOrDefault("active", "?") + " &8/ &7" + ph.getOrDefault("available", "?"),
                "&7Active Difficulty / Ceiling",
                "",
                "&7State  &" + stateColor + ph.getOrDefault("state", "?"),
                "&7Tier   &f" + ph.getOrDefault("active_tier_name", ph.getOrDefault("tier", "?")),
                "&7CR     &f" + ph.getOrDefault("combat_rating", ph.getOrDefault("calculated", "?")),
                "&7Ancient &f" + ph.getOrDefault("balance", "?"),
                "",
                "&7DMZ &f" + ph.getOrDefault("level", "?")
                        + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"),
                "&7Team &f" + ph.getOrDefault("team_mode", "?")
                        + "  &7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"),
                "&8Buy a tier in the Tier menu to raise"
        );
    }

    private static void frame(Inventory inv, int size) {
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            inv.setItem(i, item(edge ? ACCENT : FILL, " ", List.of()));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta().getLore() == null) {
            return;
        }
        String action = null;
        String arg = null;
        String page = null;
        String returnPage = null;
        for (String line : clicked.getItemMeta().getLore()) {
            String plain = strip(line);
            if (plain.startsWith("ACTION:")) {
                action = plain.substring("ACTION:".length());
            } else if (plain.startsWith("ARG:")) {
                arg = plain.substring("ARG:".length());
            } else if (plain.startsWith("PAGE:")) {
                page = plain.substring("PAGE:".length());
            } else if (plain.startsWith("RETURN:")) {
                returnPage = plain.substring("RETURN:".length());
            } else if (plain.equals("CLOSE")) {
                player.closeInventory();
                return;
            }
        }
        if (page != null) {
            final String targetPage = page;
            Bukkit.getScheduler().runTask(plugin, () -> open(player, targetPage));
            return;
        }
        if (action == null) {
            return;
        }
        final String ret = returnPage == null || returnPage.isBlank() ? "main" : returnPage;
        final String cmd = "difficulty do " + action
                + (arg == null || arg.isBlank() ? " 0" : " " + arg)
                + " " + ret;
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            player.performCommand(cmd);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private static ItemStack button(
            Material mat, String name, String action, String arg, String returnPage, List<String> tip) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(tip);
        lore.add("&8ACTION:" + action);
        lore.add("&8ARG:" + arg);
        lore.add("&8RETURN:" + returnPage);
        return item(mat, name, lore);
    }

    private static ItemStack pageBtn(Material mat, String name, String page, String... tips) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        lore.add("&8PAGE:" + page);
        return item(mat, name, lore);
    }

    private static ItemStack closeBtn() {
        return item(Material.BARRIER, "&cClose", List.of("&8CLOSE"));
    }

    private static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(color(line));
        }
        meta.setLore(colored);
        stack.setItemMeta(meta);
        return stack;
    }

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    private static String strip(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("§[0-9A-FK-ORa-fk-or]", "").replaceAll("&[0-9A-FK-ORa-fk-or]", "");
    }

    static final class Holder implements InventoryHolder {
        final String page;
        Inventory inventory;

        Holder(String page) {
            this.page = page;
        }

        void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
