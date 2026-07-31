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
            case "titles", "title" -> titles(player);
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
        inv.setItem(20, pageBtn(Material.LIME_CONCRETE, "&aUpgrade", "adjust",
                "&7Raise / lower active difficulty", "&8Paid with Ancient Coins"));
        inv.setItem(22, pageBtn(Material.GOLD_INGOT, "&eTiers", "buy",
                "&7Activate unlocked difficulty tiers", "&8Ancient Coin activation"));
        inv.setItem(24, button(Material.COMPASS, "&bTeam", "team", "0", "main",
                List.of("&7Cycle team scaling", "&8" + ph.getOrDefault("team_mode", "?"))));

        inv.setItem(27, pageBtn(Material.EXPERIENCE_BOTTLE, "&fRewards", "rewards", "&7Kill rewards (no TP)"));
        inv.setItem(28, pageBtn(Material.IRON_SWORD, "&fTiers", "tiers", "&7Enemy tier unlocks"));
        inv.setItem(29, pageBtn(Material.NAME_TAG, "&eTitles", "titles",
                "&7Unlock & equip titles",
                "&8Equipped &f" + ph.getOrDefault("active_title", "none")));
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
                "&8Raising costs Ancient Coins"
        )));
        inv.setItem(6, button(Material.ORANGE_CONCRETE, "&6Max", "set_max", "0", "adjust",
                List.of("&7Jump to available max", "&f" + ph.getOrDefault("available", "?"),
                        "&8Cost &e" + ph.getOrDefault("cost_max", "?"),
                        "&8Paid with Ancient Coins")));
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

        inv.setItem(28, button(Material.LIME_TERRACOTTA, "&a+1", "up", "1", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_1", "?"),
                        "&8Paid with Ancient Coins")));
        inv.setItem(29, button(Material.LIME_CONCRETE, "&a+5", "up", "5", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_5", "?"),
                        "&8Paid with Ancient Coins")));
        inv.setItem(30, button(Material.GREEN_TERRACOTTA, "&a+25", "up", "25", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_25", "?"),
                        "&8Paid with Ancient Coins")));
        inv.setItem(31, button(Material.GREEN_CONCRETE, "&a+100", "up", "100", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_100", "?"),
                        "&8Paid with Ancient Coins")));
        inv.setItem(32, button(Material.EMERALD, "&a+1000", "up", "1000", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_1000", "?"),
                        "&8Paid with Ancient Coins")));
        inv.setItem(33, button(Material.DIAMOND, "&a+10000", "up", "10000", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_10000", "?"),
                        "&8Paid with Ancient Coins")));
        inv.setItem(34, button(Material.NETHERITE_INGOT, "&a+100000", "up", "100000", "adjust",
                List.of("&7Raise active difficulty", "&8Cost &e" + ph.getOrDefault("cost_up_100000", "?"),
                        "&8Paid with Ancient Coins")));

        inv.setItem(36, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(40, pageBtn(Material.GOLD_INGOT, "&eTiers", "buy", "&7Activate unlocked tiers"));
        inv.setItem(44, closeBtn());
        return inv;
    }

    private Inventory buy(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("buy");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Activate Tier"));
        holder.bind(inv);
        frame(inv, 45);

        inv.setItem(4, item(Material.GOLD_INGOT, "&e&lDifficulty Tiers", List.of(
                "",
                "&7Active     &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7Unlocked   &fT" + ph.getOrDefault("highest_unlocked", "0"),
                "&7Ancient    &f" + ph.getOrDefault("balance", "?"),
                "",
                "&8Unlock by DMZ level or Prestige",
                "&8Activate with Ancient Coins",
                "&8Death clears active tier (unlocks stay)"
        )));
        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT,
                Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR
        };
        for (int t = 1; t <= 7; t++) {
            inv.setItem(18 + (t - 1), button(mats[t - 1], "&eActivate T" + t, "activate", String.valueOf(t), "buy",
                    List.of(
                            "&7Activate unlock tier &f" + t,
                            "&8Requires unlock + Ancient Coins",
                            "&8Current active &f" + ph.getOrDefault("active_tier", "0")
                    )));
        }

        inv.setItem(36, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(40, pageBtn(Material.LIME_CONCRETE, "&aUpgrade", "adjust", "&7Raise / lower active"));
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
                "",
                "&7Title   &e" + ph.getOrDefault("active_title", "none"),
                "&7Owned   &f" + ph.getOrDefault("titles_count", "0"),
                "",
                "&8Activation & upgrades use",
                "&8Ancient Coins only"
        )));
        inv.setItem(27, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(31, pageBtn(Material.NAME_TAG, "&eTitles", "titles", "&7Unlock & equip titles"));
        inv.setItem(35, closeBtn());
        return inv;
    }

    private Inventory titles(Player player) {
        ForgeBridge.syncTitles(player);
        Map<String, String> ph = ForgeBridge.placeholders(player);
        Holder holder = new Holder("titles");
        Inventory inv = Bukkit.createInventory(holder, 54, color("&8Titles"));
        holder.bind(inv);
        frame(inv, 54);

        inv.setItem(4, item(Material.NAME_TAG, "&e&lTitles", List.of(
                "",
                "&7Equipped  &e" + ph.getOrDefault("active_title", "none"),
                "&7Owned     &f" + ph.getOrDefault("titles_count", "0"),
                "&7Active    &f" + ph.getOrDefault("active", "?"),
                "",
                "&8Tier titles unlock with difficulty",
                "&8Combat titles unlock from feats",
                "&8Click an unlocked title to equip"
        )));
        inv.setItem(8, button(Material.BARRIER, "&fUnequip", "clear_title", "0", "titles",
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
            long thr = parseLong(ph.getOrDefault("tier_" + id, defaultTier(id)));
            Material mat = on ? Material.GOLD_BLOCK
                    : unlocked ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
            String label = (on ? "&e" : unlocked ? "&a" : "&8") + name;
            List<String> tip = new ArrayList<>();
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
                inv.setItem(tierSlots[i], button(mat, label, "equip_title", id, "titles", tip));
            } else {
                inv.setItem(tierSlots[i], item(mat, label, prependBlank(tip)));
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
                inv.setItem(combatSlots[i], button(mat, label, "equip_title", id, "titles", tip));
            } else {
                inv.setItem(combatSlots[i], item(mat, label, prependBlank(tip)));
            }
        }

        inv.setItem(45, pageBtn(Material.ARROW, "&7Back", "main", "&7Return to difficulty"));
        inv.setItem(53, closeBtn());
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
                "&7Title  &e" + ph.getOrDefault("active_title", "none"),
                "&7Ancient &f" + ph.getOrDefault("balance", "?"),
                "",
                "&7DMZ &f" + ph.getOrDefault("level", "?")
                        + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"),
                "&7Team bonus  &f" + ph.getOrDefault("team_bonus", "0"),
                "&7Team contrib &f" + ph.getOrDefault("team_contrib", "0"),
                "&8Activate a tier, then upgrade"
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
