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

/** Native Bukkit chest GUI fallback when DeluxeMenus is unavailable. */
public final class DifficultyChestGui implements Listener {
    private final AdaptiveDifficultyGuiPlugin plugin;

    public DifficultyChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        Inventory inv = switch (p) {
            case "rewards" -> rewards(player);
            case "tiers", "enemies" -> tiers(player);
            case "stats", "statistics" -> stats(player);
            default -> main(player);
        };
        player.openInventory(inv);
    }

    private Inventory main(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Adaptive Difficulty"));
        holder.bind(inv);

        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        String stateColor = ph.getOrDefault("state_color", "f");
        inv.setItem(4, item(Material.NETHER_STAR, "&6&lAdaptive Difficulty", List.of(
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
                        + " online via " + ph.getOrDefault("team_source", "?") + ")",
                "&eTeam Mode: &f" + ph.getOrDefault("team_mode", "?")
                        + " &8| &eBalance: &f" + ph.getOrDefault("balance", "?"),
                "&eEnemy Tier: &f" + ph.getOrDefault("tier", "?")
        )));

        inv.setItem(19, button(Material.RED_DYE, "&c▼ -100", "down", "100", "&7Lower active difficulty (free)"));
        inv.setItem(21, button(Material.ORANGE_DYE, "&6Max", "set_max", "0", "&7Set active to available max"));
        inv.setItem(23, button(Material.LIME_DYE, "&a▲ +100", "up", "100", "&7Raise active difficulty"));
        inv.setItem(25, button(Material.COMPASS, "&bTeam Mode", "team", "0", "&7Cycle team scaling mode"));

        inv.setItem(29, button(Material.GOLD_NUGGET, "&6Buy +100", "buy", "100",
                "&7Cost: &f" + ph.getOrDefault("cost_100", "?")));
        inv.setItem(31, button(Material.GOLD_INGOT, "&6Buy +1,000", "buy", "1000",
                "&7Cost: &f" + ph.getOrDefault("cost_1000", "?")));
        inv.setItem(33, button(Material.GOLD_BLOCK, "&6Buy +10,000", "buy", "10000",
                "&7Cost: &f" + ph.getOrDefault("cost_10000", "?")));

        inv.setItem(37, pageBtn(Material.BOOK, "&eRewards", "rewards"));
        inv.setItem(38, pageBtn(Material.IRON_SWORD, "&eTiers", "tiers"));
        inv.setItem(39, pageBtn(Material.PAPER, "&eStats", "stats"));
        inv.setItem(40, button(Material.SUNFLOWER, "&aRefresh", "refresh", "0", "&7Reload this menu"));
        inv.setItem(44, closeBtn());
        return inv;
    }

    private Inventory rewards(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("rewards");
        Inventory inv = Bukkit.createInventory(holder, 27, color("&8Rewards"));
        holder.bind(inv);
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(13, item(Material.EXPERIENCE_BOTTLE, "&6&lRewards", List.of(
                "&eActive: &f" + ph.getOrDefault("active", "?"),
                "&eTP Multiplier: &f×" + ph.getOrDefault("reward_mult", "?"),
                "&7Formula: 1 + Difficulty / RewardScaling"
        )));
        inv.setItem(18, pageBtn(Material.ARROW, "&a« Back", "main"));
        inv.setItem(26, closeBtn());
        return inv;
    }

    private Inventory tiers(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("tiers");
        Inventory inv = Bukkit.createInventory(holder, 36, color("&8Enemy Tiers"));
        holder.bind(inv);
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(4, item(Material.DIAMOND_SWORD, "&6&lEnemy Tiers", List.of(
                "&7Current: &f" + ph.getOrDefault("tier", "?"),
                "&7Active: &f" + ph.getOrDefault("active", "?")
        )));
        String[][] rows = {
                {"awakened", "Awakened"}, {"enhanced", "Enhanced"}, {"elite", "Elite"}, {"advanced", "Advanced"},
                {"master", "Master"}, {"legendary", "Legendary"}, {"god", "God"},
                {"divine", "Divine"}, {"impossible", "Impossible"}
        };
        int slot = 11;
        long active;
        try {
            active = Long.parseLong(ph.getOrDefault("active", "0"));
        } catch (NumberFormatException e) {
            active = 0L;
        }
        for (String[] row : rows) {
            long thr;
            try {
                thr = Long.parseLong(ph.getOrDefault("tier_" + row[0], defaultTier(row[0])));
            } catch (NumberFormatException e) {
                thr = Long.parseLong(defaultTier(row[0]));
            }
            boolean unlocked = active >= thr;
            inv.setItem(slot, item(
                    unlocked ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE,
                    (unlocked ? "&a✓ " : "&8· ") + "&e" + thr + " &f" + row[1],
                    List.of(unlocked ? "&7Unlocked" : "&7Locked")
            ));
            slot++;
            if (slot == 16) {
                slot = 20;
            }
        }
        inv.setItem(27, pageBtn(Material.ARROW, "&a« Back", "main"));
        inv.setItem(35, closeBtn());
        return inv;
    }

    private Inventory stats(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("stats");
        Inventory inv = Bukkit.createInventory(holder, 27, color("&8Statistics"));
        holder.bind(inv);
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        String stateColor = ph.getOrDefault("state_color", "f");
        inv.setItem(13, item(Material.BOOK, "&6&lStatistics", List.of(
                "&7State &" + stateColor + ph.getOrDefault("state", "?"),
                "&7Balance &f" + ph.getOrDefault("balance", "?")
                        + " &8| &7Teams &f" + ph.getOrDefault("team_source", "?"),
                "&7Active &f" + ph.getOrDefault("active", "?")
                        + " &8| &7Available &f" + ph.getOrDefault("available", "?"),
                "&7Purchased &f" + ph.getOrDefault("purchased", "?")
                        + " &8| &7Calculated &f" + ph.getOrDefault("calculated", "?"),
                "&7Titles &f" + ph.getOrDefault("titles", "none")
        )));
        inv.setItem(18, pageBtn(Material.ARROW, "&a« Back", "main"));
        inv.setItem(26, closeBtn());
        return inv;
    }

    private static String defaultTier(String key) {
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

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
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
        for (String line : clicked.getItemMeta().getLore()) {
            String plain = strip(line);
            if (plain.startsWith("ACTION:")) {
                action = plain.substring("ACTION:".length());
            } else if (plain.startsWith("ARG:")) {
                arg = plain.substring("ARG:".length());
            } else if (plain.startsWith("PAGE:")) {
                page = plain.substring("PAGE:".length());
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
        final String cmd = "difficulty do " + action + (arg == null || arg.isBlank() ? "" : " " + arg);
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

    private static ItemStack button(Material mat, String name, String action, String arg, String tip) {
        List<String> lore = new ArrayList<>();
        lore.add(tip);
        lore.add("&8ACTION:" + action);
        lore.add("&8ARG:" + arg);
        return item(mat, name, lore);
    }

    private static ItemStack pageBtn(Material mat, String name, String page) {
        return item(mat, name, List.of("&7Open page", "&8PAGE:" + page));
    }

    private static ItemStack closeBtn() {
        return item(Material.BARRIER, "&cClose", List.of("&8CLOSE"));
    }

    private static void fill(Inventory inv, Material mat) {
        ItemStack pane = item(mat, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
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
