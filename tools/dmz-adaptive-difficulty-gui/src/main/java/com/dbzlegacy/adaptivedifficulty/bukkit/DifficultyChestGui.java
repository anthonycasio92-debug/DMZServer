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

/** Clean Bukkit chest GUI fallback when CMI/CMILib is unavailable. */
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
        Inventory inv = Bukkit.createInventory(holder, 36, color("&8Difficulty"));
        holder.bind(inv);
        frame(inv, 36);

        String stateColor = ph.getOrDefault("state_color", "f");
        inv.setItem(13, item(Material.NETHER_STAR, "&f&lDifficulty", statusLore(ph, stateColor)));
        inv.setItem(20, pageBtn(Material.LIME_CONCRETE, "&aAdjust", "adjust",
                "&7Raise / lower active difficulty", "&8Steps +1 … +100000"));
        inv.setItem(22, pageBtn(Material.GOLD_INGOT, "&eBuy Max", "buy",
                "&7Unlock more available max", "&8Paid from inventory coins"));
        inv.setItem(24, button(Material.COMPASS, "&bTeam", "team", "0", "main",
                List.of("&7Cycle team scaling", "&8" + ph.getOrDefault("team_mode", "?"))));

        inv.setItem(27, pageBtn(Material.EXPERIENCE_BOTTLE, "&fRewards", "rewards", "&7TP multiplier details"));
        inv.setItem(28, pageBtn(Material.IRON_SWORD, "&fTiers", "tiers", "&7Enemy tier unlocks"));
        inv.setItem(29, pageBtn(Material.BOOK, "&fDetails", "stats", "&7Team, titles, full breakdown"));
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
                "&7Active / Available",
                "",
                "&7State  &" + stateColor + ph.getOrDefault("state", "?"),
                "&7Inv    &f" + ph.getOrDefault("balance", "?"),
                "",
                "&8Raising costs inventory coins"
        )));
        inv.setItem(6, button(Material.ORANGE_CONCRETE, "&6Max", "set_max", "0", "adjust",
                List.of("&7Jump to available max", "&f" + ph.getOrDefault("available", "?"),
                        "&8Cost &e" + ph.getOrDefault("cost_max", "?"),
                        "&8Paid from inventory coins")));
        inv.setItem(7, button(Material.WHITE_CONCRETE, "&fReset", "reset", "0", "adjust",
                List.of("&7Set active to &f0", "&8Purchased max kept", "&8Always free")));

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

        inv.setItem(28, button(Material.LIME_TERRACOTTA, "&a+1", "up", "1", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_1", "?"),
                        "&8Paid from inventory coins")));
        inv.setItem(29, button(Material.LIME_CONCRETE, "&a+5", "up", "5", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_5", "?"),
                        "&8Paid from inventory coins")));
        inv.setItem(30, button(Material.GREEN_TERRACOTTA, "&a+25", "up", "25", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_25", "?"),
                        "&8Paid from inventory coins")));
        inv.setItem(31, button(Material.GREEN_CONCRETE, "&a+100", "up", "100", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_100", "?"),
                        "&8Paid from inventory coins")));
        inv.setItem(32, button(Material.EMERALD, "&a+1000", "up", "1000", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_1000", "?"),
                        "&8Paid from inventory coins")));
        inv.setItem(33, button(Material.DIAMOND, "&a+10000", "up", "10000", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_10000", "?"),
                        "&8Paid from inventory coins")));
        inv.setItem(34, button(Material.NETHERITE_INGOT, "&a+100000", "up", "100000", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_100000", "?"),
                        "&8Paid from inventory coins")));

        inv.setItem(36, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(40, pageBtn(Material.GOLD_INGOT, "&eBuy Max", "buy", "&7Unlock more available max"));
        inv.setItem(44, closeBtn());
        return inv;
    }

    private Inventory buy(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("buy");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Buy Max Difficulty"));
        holder.bind(inv);
        frame(inv, 45);

        inv.setItem(4, item(Material.GOLD_INGOT, "&e&lBuy Max", List.of(
                "",
                "&7Purchased  &f" + ph.getOrDefault("purchased", "?"),
                "&7Available  &f" + ph.getOrDefault("available", "?"),
                "&7Inv coins  &f" + ph.getOrDefault("balance", "?"),
                "",
                "&8Unlock more max difficulty",
                "&8Paid from inventory coins"
        )));
        inv.setItem(19, button(Material.GOLD_NUGGET, "&e+1 max", "buy", "1", "buy",
                List.of("&7Unlock +1 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_1", "?"))));
        inv.setItem(20, button(Material.GOLD_NUGGET, "&e+5 max", "buy", "5", "buy",
                List.of("&7Unlock +5 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_5", "?"))));
        inv.setItem(21, button(Material.GOLD_INGOT, "&e+25 max", "buy", "25", "buy",
                List.of("&7Unlock +25 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_25", "?"))));
        inv.setItem(22, button(Material.GOLD_BLOCK, "&e+100 max", "buy", "100", "buy",
                List.of("&7Unlock +100 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_100", "?"))));
        inv.setItem(23, button(Material.EMERALD, "&e+1000 max", "buy", "1000", "buy",
                List.of("&7Unlock +1000 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_1000", "?"))));
        inv.setItem(24, button(Material.DIAMOND, "&e+10000 max", "buy", "10000", "buy",
                List.of("&7Unlock +10000 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_10000", "?"))));
        inv.setItem(25, button(Material.NETHERITE_INGOT, "&e+100000 max", "buy", "100000", "buy",
                List.of("&7Unlock +100000 max difficulty", "&8Cost &e" + ph.getOrDefault("cost_buy_100000", "?"))));

        inv.setItem(36, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(40, pageBtn(Material.LIME_CONCRETE, "&aAdjust", "adjust", "&7Raise / lower active"));
        inv.setItem(44, closeBtn());
        return inv;
    }

    private Inventory rewards(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("rewards");
        Inventory inv = Bukkit.createInventory(holder, 27, color("&8Rewards"));
        holder.bind(inv);
        frame(inv, 27);
        inv.setItem(13, item(Material.EXPERIENCE_BOTTLE, "&f&lRewards", List.of(
                "",
                "&7Active  &f" + ph.getOrDefault("active", "?"),
                "&7Kill TP &a~" + ph.getOrDefault("kill_tp", "?"),
                "&7Train × &a" + ph.getOrDefault("reward_mult", "?"),
                "",
                "&8Kill ≈ 400k at 8M difficulty (curve)"
        )));
        inv.setItem(18, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(26, closeBtn());
        return inv;
    }

    private Inventory tiers(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("tiers");
        Inventory inv = Bukkit.createInventory(holder, 54, color("&8Enemy Tiers"));
        holder.bind(inv);
        frame(inv, 54);
        inv.setItem(4, item(Material.IRON_SWORD, "&f&lEnemy Tiers", List.of(
                "",
                "&7Current  &f" + ph.getOrDefault("tier", "?"),
                "&7Active   &f" + ph.getOrDefault("active", "?"),
                "",
                "&8Zenith = theoretical max",
                "&8(level 100k × 10 prestiges)"
        )));

        String[][] rows = {
                {"awakened", "Awakened"}, {"enhanced", "Enhanced"}, {"elite", "Elite"},
                {"advanced", "Advanced"}, {"master", "Master"}, {"legendary", "Legendary"},
                {"god", "God"}, {"divine", "Divine"}, {"impossible", "Impossible"},
                {"transcendent", "Transcendent"}, {"eternal", "Eternal"}, {"mythic", "Mythic"},
                {"omega", "Omega"}, {"absolute", "Absolute"}, {"apex", "Apex"},
                {"zenith", "Zenith"}
        };
        long active = parseLong(ph.getOrDefault("active", "0"));
        int[] slots = {
                9, 10, 11, 12, 13, 14, 15, 16,
                18, 19, 20, 21, 22, 23, 24, 25
        };
        for (int i = 0; i < rows.length && i < slots.length; i++) {
            long thr = parseLong(ph.getOrDefault("tier_" + rows[i][0], defaultTier(rows[i][0])));
            boolean unlocked = active >= thr;
            inv.setItem(slots[i], item(
                    unlocked ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE,
                    (unlocked ? "&a" : "&8") + rows[i][1],
                    List.of("", "&7Threshold  &f" + thr, unlocked ? "&aUnlocked" : "&8Locked")
            ));
        }
        inv.setItem(45, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(53, closeBtn());
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
                "&7Available   &f" + ph.getOrDefault("available", "?"),
                "&7Theoretical &f" + ph.getOrDefault("calculated", "?"),
                "&7Purchased   &f" + ph.getOrDefault("purchased", "?"),
                "&7Personal    &f" + ph.getOrDefault("personal_max", "?"),
                "",
                "&7State  &" + stateColor + ph.getOrDefault("state", "?"),
                "&7Tier   &f" + ph.getOrDefault("tier", "?")
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
        inv.setItem(15, item(Material.GOLD_INGOT, "&f&lInventory", List.of(
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
        )));
        inv.setItem(27, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(35, closeBtn());
        return inv;
    }

    private static List<String> statusLore(Map<String, String> ph, String stateColor) {
        return List.of(
                "",
                "&f" + ph.getOrDefault("active", "?") + " &8/ &7" + ph.getOrDefault("available", "?"),
                "&7Active / Available",
                "",
                "&7State  &" + stateColor + ph.getOrDefault("state", "?"),
                "&7Tier   &f" + ph.getOrDefault("tier", "?"),
                "&7Inv    &f" + ph.getOrDefault("balance", "?"),
                "",
                "&8Theoretical " + ph.getOrDefault("calculated", "?")
                        + "  ·  Bought " + ph.getOrDefault("purchased", "?"),
                "&8Open Adjust to raise / lower"
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
