package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
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

/** Character Services — race / class / reskin (Ancient Coins). */
public final class CharacterServicesChestGui implements Listener {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private final AdaptiveDifficultyGuiPlugin plugin;

    public CharacterServicesChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String page) {
        Player viewer = player;
        Player subject = AdminInspectSessions.resolveSubject(viewer);
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        Inventory inv;
        if (p.startsWith("race_confirm:")) {
            inv = raceConfirm(viewer, subject, p.substring("race_confirm:".length()));
        } else if (p.startsWith("race_pct:")) {
            inv = racePct(viewer, subject, p.substring("race_pct:".length()));
        } else if (p.startsWith("class_confirm:")) {
            inv = classConfirm(viewer, subject, p.substring("class_confirm:".length()));
        } else {
            inv = switch (p) {
                case "race" -> raceList(viewer, subject);
                case "class" -> classList(viewer, subject);
                case "reskin" -> reskin(viewer, subject);
                case "reskin_confirm" -> reskinConfirm(viewer, subject);
                case "bones" -> boneShopOrReskin(viewer, subject, 0);
                default -> {
                    if (p.startsWith("bones:")) {
                        yield boneShopOrReskin(viewer, subject, parseBonePage(p));
                    }
                    yield main(viewer, subject);
                }
            };
        }
        GuiFeedback.openChest(viewer, inv);
    }

    private static boolean inspecting(Player viewer, Player subject) {
        return viewer != null && subject != null
                && !viewer.getUniqueId().equals(subject.getUniqueId());
    }

    private static String invTitle(Player viewer, Player subject, String base) {
        return GuiNav.inventoryTitle(viewer, subject, base);
    }

    private Inventory main(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.charPlaceholders(subject);
        Map<String, String> vars = charCooldownVars(ph);
        vars.put("race", ph.getOrDefault("current_race", "?"));
        vars.put("class", ph.getOrDefault("current_class", "?"));
        vars.put("reskin_cost", ph.getOrDefault("reskin_cost", "?"));
        vars.put("class_cost", ph.getOrDefault("class_cost", "?"));

        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Character Services"));
        holder.bind(inv);
        frame(inv, 45);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean enabled = bridgeOk && "true".equalsIgnoreCase(ph.getOrDefault("enabled", "false"));
        if (!bridgeOk || !enabled) {
            putProfile(holder, inv, "&c&lUNAVAILABLE",
                    List.of("", bridgeOk ? "&cCharacter Services are off right now" : "&cCan't reach Legacy Mechanics on the server"));
            footer45(holder, inv, "character.main.back_hub", SlotAction.cmd("lmdo lm open hub"));
            return inv;
        }

        putProfile(holder, inv, GuiTooltips.name("character.main.wallet", "&d&lCharacter Services", vars),
                profileLore(subject, vars));

        putServiceEntry(holder, inv, 20, "character.main.race", Material.NETHER_STAR, "&eChange Race",
                List.of("&7Choose a race and how much progress to keep",
                        "&8{race_cooldown}", "&eContinue"),
                vars, ph, "race_enabled", "can_race_change", "race");
        putServiceEntry(holder, inv, 22, "character.main.class", Material.ENCHANTED_BOOK, "&bChange Class",
                List.of("&7Change fighting class — your core stats stay",
                        "&7Cost &f{class_cost}",
                        "&8{class_cooldown}", "&eContinue"),
                vars, ph, "class_enabled", "can_class_change", "class");
        putServiceEntry(holder, inv, 24, "character.main.reskin", Material.AMETHYST_CLUSTER, "&dReskin",
                List.of("&7Change how you look and your head parts",
                        "&7Cost &f{reskin_cost}",
                        "&8{reskin_cooldown}", "&eContinue"),
                vars, ph, "reskin_enabled", "can_reskin", "reskin");

        footer45(holder, inv, "character.main.back_hub", SlotAction.cmd("lmdo lm open hub"));
        return inv;
    }

    private List<String> profileLore(Player subject, Map<String, String> vars) {
        Map<String, String> ph = ForgeBridge.charPlaceholders(subject);
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(coinLore(ph));
        lore.add("&7Race &f" + vars.getOrDefault("race", "?")
                + " &8· &7Class &f" + vars.getOrDefault("class", "?"));
        lore.add("&8Cancel from the editor and you get a refund");
        lore.addAll(toAmp(ForgeBridge.charLines(subject, "main")));
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
                "&6Total &f" + ph.getOrDefault("ancient_coins", "0") + " &7AC copper-value"
        );
    }

    private Inventory raceList(Player viewer, Player subject) {
        Holder holder = new Holder("race");
        Inventory inv = Bukkit.createInventory(holder, 54, invTitle(viewer, subject, "&8Change Race"));
        holder.bind(inv);
        frame(inv, 54);
        put(holder, inv, 4, item(Material.NETHER_STAR,
                GuiTooltips.name("character.race.header", "&e&lChoose a Race"),
                prependBlank(toAmp(ForgeBridge.charLines(subject, "race")))));
        Map<String, String> ph = ForgeBridge.charPlaceholders(subject);
        if (!serviceAllowed(ph, "race_enabled", "can_race_change")) {
            put(holder, inv, 22, item(Material.BARRIER, "&cRace change unavailable",
                    List.of("", "&7Not available right now", "&7Use Back to return")));
            footer54(holder, inv, "character.race.back", SlotAction.page("main"));
            return inv;
        }
        List<String> cards = ForgeBridge.charRaceCards(subject);
        int[] slots = centered(Math.min(cards.size(), 28));
        for (int i = 0; i < slots.length && i < cards.size(); i++) {
            String[] p = cards.get(i).split("\t", -1);
            String id = p.length > 0 ? p[0] : "";
            String name = p.length > 1 ? p[1] : id;
            boolean current = "1".equals(p.length > 2 ? p[2] : "0");
            Material mat = current ? Material.LIME_CONCRETE : Material.WHITE_CONCRETE;
            String cardKey = current ? "character.race.card_current" : "character.race.card_pick";
            put(holder, inv, slots[i], item(mat, (current ? "&a" : "&f") + name,
                    GuiTooltips.buttonLore(cardKey, List.of(), null, null)),
                    SlotAction.page("race_pct:" + id + ":0"));
        }
        footer54(holder, inv, "character.race.back", SlotAction.page("main"));
        return inv;
    }

    private Inventory racePct(Player viewer, Player subject, String raceAndPct) {
        String race = raceAndPct;
        int defaultPct = 0;
        int colon = raceAndPct.lastIndexOf(':');
        if (colon > 0) {
            race = raceAndPct.substring(0, colon);
            try {
                defaultPct = Integer.parseInt(raceAndPct.substring(colon + 1));
            } catch (NumberFormatException ignored) {
                defaultPct = 0;
            }
        }
        Holder holder = new Holder("race_pct");
        Inventory inv = Bukkit.createInventory(holder, 54, invTitle(viewer, subject, "&8Stat Preservation"));
        holder.bind(inv);
        frame(inv, 54);
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7Becoming &f" + prettyId(race));
        lore.addAll(toAmp(ForgeBridge.charLines(subject, "race_pct:" + race + ":" + defaultPct)));
        put(holder, inv, 4, item(Material.EXPERIENCE_BOTTLE,
                GuiTooltips.name("character.race_pct.header", "&e&lHow Much to Keep?"), lore));
        int[] pcts = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        int[] slots = {10, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30};
        for (int i = 0; i < pcts.length && i < slots.length; i++) {
            int pct = pcts[i];
            boolean selected = pct == defaultPct;
            Map<String, String> pctVars = Map.of("pct", String.valueOf(pct));
            Material mat = selected ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
            String title = pct == 0 ? (selected ? "&a&l0% — Free" : "&e&l0% — Free") : (selected ? "&a" : "&f") + pct + "%";
            List<String> btnLore = pct == 0
                    ? List.of("&7Full wipe, then pick class and look", "&aFree for everyone", "&eReview next")
                    : List.of("&7Keep " + pct + "% of your core stats", "&eReview next");
            put(holder, inv, slots[i], item(mat, title,
                    GuiTooltips.buttonLore("character.race_pct.pct", btnLore, pctVars, null)),
                    SlotAction.page("race_confirm:" + race + ":" + pct));
        }
        footer54(holder, inv, "character.race_pct.back", SlotAction.page("race"));
        return inv;
    }

    private Inventory raceConfirm(Player viewer, Player subject, String raceAndPct) {
        String[] bits = raceAndPct.split(":", 2);
        String race = bits.length > 0 ? bits[0] : "";
        String pct = bits.length > 1 ? bits[1] : "0";
        Holder holder = new Holder("race_confirm");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Confirm Race"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> summary = prependBlank(toAmp(ForgeBridge.charLines(subject, "race_confirm:" + race + ":" + pct)));
        put(holder, inv, 4, item(Material.ORANGE_CONCRETE,
                GuiTooltips.name("character.race_confirm.header", "&c&lLast Chance"), summary));
        boolean freeWipe = "0".equals(pct.trim());
        List<String> confirmLore = freeWipe
                ? List.of("&7Free full wipe, then pick class and look", "&eContinue")
                : List.of("&7Pay the listed cost to change race", "&eConfirm when you're ready");
        SlotAction confirmAction = freeWipe
                ? SlotAction.actNoReopen("race_confirm", race + ":" + pct)
                : SlotAction.act("race_confirm", race + ":" + pct, "main");
        put(holder, inv, 20, tipBtn("character.race_confirm.confirm", Material.LIME_CONCRETE, "&a&lConfirm Race Change",
                confirmLore, null), confirmAction);
        put(holder, inv, 24, tipBtn("character.race_confirm.cancel", Material.RED_CONCRETE, "&cCancel",
                List.of("&7Go back without paying"), null),
                SlotAction.page("race_pct:" + race + ":" + pct));
        footer45(holder, inv, "character.race_confirm.back", SlotAction.page("race_pct:" + race + ":" + pct));
        return inv;
    }

    private Inventory classList(Player viewer, Player subject) {
        Holder holder = new Holder("class");
        Inventory inv = Bukkit.createInventory(holder, 54, invTitle(viewer, subject, "&8Change Class"));
        holder.bind(inv);
        frame(inv, 54);
        put(holder, inv, 4, item(Material.ENCHANTED_BOOK,
                GuiTooltips.name("character.class.header", "&b&lChoose a Class"),
                prependBlank(toAmp(ForgeBridge.charLines(subject, "class")))));
        Map<String, String> ph = ForgeBridge.charPlaceholders(subject);
        if (!serviceAllowed(ph, "class_enabled", "can_class_change")) {
            put(holder, inv, 22, item(Material.BARRIER, "&cClass change unavailable",
                    List.of("", "&7Turned off or no permission", "&7Return with Back")));
            footer54(holder, inv, "character.class.back", SlotAction.page("main"));
            return inv;
        }
        List<String> cards = ForgeBridge.charClassCards(subject);
        int[] slots = centered(Math.min(cards.size(), 28));
        for (int i = 0; i < slots.length && i < cards.size(); i++) {
            String[] p = cards.get(i).split("\t", -1);
            String id = p.length > 0 ? p[0] : "";
            String name = p.length > 1 ? p[1] : id;
            boolean current = "1".equals(p.length > 2 ? p[2] : "0");
            Material mat = current ? Material.LIME_CONCRETE : Material.LIGHT_BLUE_CONCRETE;
            String cardKey = current ? "character.class.card_current" : "character.class.card_pick";
            put(holder, inv, slots[i], item(mat, (current ? "&a" : "&f") + name,
                    GuiTooltips.buttonLore(cardKey, List.of(), null, null)),
                    SlotAction.page("class_confirm:" + id));
        }
        footer54(holder, inv, "character.class.back", SlotAction.page("main"));
        return inv;
    }

    private Inventory classConfirm(Player viewer, Player subject, String classId) {
        Holder holder = new Holder("class_confirm");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Confirm Class"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.ORANGE_CONCRETE,
                GuiTooltips.name("character.class_confirm.header", "&c&lConfirm Class Change"),
                prependBlank(toAmp(ForgeBridge.charLines(subject, "class_confirm:" + classId)))));
        put(holder, inv, 20, tipBtn("character.class_confirm.confirm", Material.LIME_CONCRETE, "&a&lConfirm",
                List.of("&7Pay the listed cost to change class", "&eConfirm when you're ready"), null),
                SlotAction.act("class_confirm", classId, "main"));
        put(holder, inv, 24, tipBtn("character.class_confirm.cancel", Material.RED_CONCRETE, "&cCancel",
                List.of("&7Go back without paying"), null),
                SlotAction.page("class"));
        footer45(holder, inv, "character.class_confirm.back", SlotAction.page("class"));
        return inv;
    }

    private Inventory boneShopOrReskin(Player viewer, Player subject, int page) {
        Map<String, String> ph = ForgeBridge.charPlaceholders(subject);
        if (!headBoneShopAllowed(ph)) {
            return reskin(viewer, subject);
        }
        return boneShop(viewer, subject, page, "reskin");
    }

    private Inventory boneShop(Player viewer, Player subject, int page, String returnPage) {
        Holder holder = new Holder("bones");
        Inventory inv = Bukkit.createInventory(holder, 54, invTitle(viewer, subject, "&8Head Parts"));
        holder.bind(inv);
        frame(inv, 54);
        String pageKey = "bones:" + page;
        put(holder, inv, 4, item(Material.PLAYER_HEAD,
                GuiTooltips.name("character.bones.header", "&6&lHead Parts Shop"),
                prependBlank(toAmp(ForgeBridge.charLines(subject, pageKey)))));
        List<String> cards = ForgeBridge.charHeadBoneCards(subject, page);
        int[] slots = centered(Math.min(cards.size(), 28));
        for (int i = 0; i < slots.length && i < cards.size(); i++) {
            String[] p = cards.get(i).split("\t", -1);
            String id = p.length > 0 ? p[0] : "";
            String name = p.length > 1 ? p[1] : id;
            String state = p.length > 2 ? p[2] : "L";
            String cost = p.length > 3 ? p[3] : "";
            Material mat;
            String title;
            List<String> lore = new ArrayList<>();
            SlotAction action;
            switch (state) {
                case "E" -> {
                    mat = Material.LIME_CONCRETE;
                    title = "&a&l" + name + " &8(equipped)";
                    lore.add("&7Currently on your character");
                    action = SlotAction.actNoReopen("bone_equip", id);
                }
                case "U", "N" -> {
                    mat = Material.LIGHT_BLUE_CONCRETE;
                    title = "&f" + name;
                    lore.add(state.equals("N") ? "&7Included with your race" : "&7Unlocked");
                    lore.add("&eEquip");
                    action = SlotAction.act("bone_equip", id, pageKey);
                }
                default -> {
                    mat = Material.GOLD_INGOT;
                    title = "&e" + name;
                    lore.add("&7Unlock for &f" + (cost.isBlank() ? "?" : cost));
                    lore.add("&eUnlock and equip");
                    action = SlotAction.act("bone_unlock", id, pageKey);
                }
            }
            put(holder, inv, slots[i], item(mat, title, lore), action);
        }
        int pages = 1;
        for (int p = 0; p < 64; p++) {
            List<String> slice = ForgeBridge.charHeadBoneCards(subject, p);
            if (slice == null || slice.isEmpty()) {
                pages = Math.max(1, p);
                break;
            }
            pages = p + 1;
        }
        put(holder, inv, 48, tipBtn("character.bones.race_default", Material.TOTEM_OF_UNDYING, "&eRace default",
                List.of("&7Equip this race's default head part", "&8From DMZ character.json", "&eClick"), null),
                SlotAction.act("bone_race_default", "0", pageKey));
        put(holder, inv, 49, tipBtn("character.bones.unequip", Material.BARRIER, "&7Unequip / clear",
                List.of("&7Remove cross-race parts", "&7Uses &fhair &7when your race has it", "&eClick"), null),
                SlotAction.act("bone_unequip", "0", pageKey));
        if (page > 0) {
            put(holder, inv, 45, pageBtn("character.bones.prev", Material.ARROW, "&7« Page " + page,
                    null), SlotAction.page("bones:" + (page - 1)));
        }
        if (page + 1 < pages) {
            put(holder, inv, 53, pageBtn("character.bones.next", Material.ARROW, "&7Page " + (page + 2) + " »",
                    null), SlotAction.page("bones:" + (page + 1)));
        }
        String back = returnPage == null || returnPage.isBlank() ? "reskin" : returnPage;
        footer54(holder, inv, "character.bones.back", SlotAction.page(back));
        return inv;
    }

    private static int parseBonePage(String page) {
        if (page == null || page.isBlank() || "bones".equals(page)) {
            return 0;
        }
        if (page.startsWith("bones:")) {
            try {
                return Integer.parseInt(page.substring("bones:".length()).trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private Inventory reskin(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.charPlaceholders(subject);
        Map<String, String> vars = charCooldownVars(ph);
        vars.put("reskin_cost", ph.getOrDefault("reskin_cost", "?"));
        vars.put("active_head_bone", ph.getOrDefault("active_head_bone", "none"));

        Holder holder = new Holder("reskin");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Reskin"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.PAINTING,
                GuiTooltips.name("character.reskin.header", "&d&lReskin"),
                prependBlank(toAmp(ForgeBridge.charLines(subject, "reskin")))));
        if (serviceAllowed(ph, "reskin_enabled", "can_reskin")) {
            put(holder, inv, 20, tipBtn("character.reskin.review", Material.LIME_CONCRETE, "&a&lReview cost & continue",
                    List.of("&7See exact price on the next screen", "&7Then pay and open the look editor"), vars),
                    SlotAction.page("reskin_confirm"));
        } else {
            put(holder, inv, 20, item(Material.GRAY_CONCRETE, "&7Reskin unavailable",
                    List.of("", "&7Turned off or no permission")));
        }
        if (headBoneShopAllowed(ph)) {
            put(holder, inv, 24, tipBtn("character.reskin.bones", Material.PLAYER_HEAD, "&6&lHead Parts Shop",
                    List.of("&7Unlock & equip cross-race ears, horns, etc.",
                            "&7Equipped &f{active_head_bone}",
                            "&eBrowse head parts"), vars),
                    SlotAction.page("bones"));
        }
        footer45(holder, inv, "character.reskin.back", SlotAction.page("main"));
        return inv;
    }

    private Inventory reskinConfirm(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.charPlaceholders(subject);
        Map<String, String> vars = charCooldownVars(ph);
        vars.put("reskin_cost", ph.getOrDefault("reskin_cost", "?"));

        Holder holder = new Holder("reskin_confirm");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Confirm reskin"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.PAINTING,
                GuiTooltips.name("character.reskin.confirm.header", "&d&lConfirm reskin"),
                prependBlank(toAmp(ForgeBridge.charLines(subject, "reskin_confirm")))));
        if (serviceAllowed(ph, "reskin_enabled", "can_reskin")) {
            put(holder, inv, 20, tipBtn("character.reskin.confirm.pay", Material.LIME_CONCRETE, "&a&lConfirm & pay",
                    List.of("&7Cost &f{reskin_cost}", "&7Opens the in-game look editor",
                            "&8Closing the editor early does not refund", "&ePay, then open the editor"), vars),
                    SlotAction.actNoReopen("reskin_confirm", "0"));
        } else {
            put(holder, inv, 20, item(Material.GRAY_CONCRETE, "&7Reskin unavailable",
                    List.of("", "&7Turned off or no permission")));
        }
        footer45(holder, inv, "character.reskin.confirm.back", SlotAction.page("reskin"));
        return inv;
    }

    private static boolean serviceAllowed(Map<String, String> ph, String configKey, String permKey) {
        if (ph == null) {
            return false;
        }
        return "true".equalsIgnoreCase(ph.getOrDefault(configKey, "false"))
                && "true".equalsIgnoreCase(ph.getOrDefault(permKey, "false"));
    }

    private static boolean headBoneShopAllowed(Map<String, String> ph) {
        return serviceAllowed(ph, "head_bone_shop_enabled", "can_head_bones");
    }

    private static void putServiceEntry(
            Holder holder,
            Inventory inv,
            int slot,
            String key,
            Material mat,
            String name,
            List<String> lore,
            Map<String, String> vars,
            Map<String, String> ph,
            String configKey,
            String permKey,
            String page
    ) {
        if (serviceAllowed(ph, configKey, permKey)) {
            put(holder, inv, slot, tipBtn(key, mat, name, lore, vars), SlotAction.page(page));
            return;
        }
        boolean configOff = !"true".equalsIgnoreCase(ph.getOrDefault(configKey, "false"));
        List<String> blocked = new ArrayList<>(lore);
        blocked.add("");
        blocked.add(configOff ? "&cTurned off on this server" : "&cNo permission");
        put(holder, inv, slot, item(Material.GRAY_CONCRETE, "&8" + stripColor(name), blocked), null);
    }

    private static String stripColor(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("(?i)&[0-9a-fk-or]", "").replaceAll("§[0-9a-fk-or]", "");
    }

    private static void putProfile(Holder holder, Inventory inv, String title, List<String> lore) {
        put(holder, inv, 4, item(Material.GOLD_INGOT, title, lore));
    }

    private void footer45(Holder holder, Inventory inv, String backKey, SlotAction backAction) {
        put(holder, inv, 36, pageBtn("common.back", Material.ARROW, "&7Back", null), backAction);
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn("character.main.close"), SlotAction.dismiss());
    }

    private void footer54(Holder holder, Inventory inv, String backKey, SlotAction backAction) {
        put(holder, inv, 45, pageBtn("common.back", Material.ARROW, "&7Back", null), backAction);
        put(holder, inv, 49, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 53, closeBtn("character.main.close"), SlotAction.dismiss());
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
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        SlotAction slot = holder.actionAt(event.getSlot());
        if (slot == null) {
            return;
        }
        if (slot.shouldClose) {
            player.closeInventory();
            return;
        }
        if (slot.page != null) {
            Bukkit.getScheduler().runTask(plugin, () -> open(player, slot.page));
            return;
        }
        if (slot.rawCommand != null && !slot.rawCommand.isBlank()) {
            String cmd = slot.rawCommand;
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                player.performCommand(cmd);
            });
            return;
        }
        if (slot.action == null) {
            return;
        }
        String ret = slot.returnPage == null ? "main" : slot.returnPage;
        String action = slot.action;
        String arg = slot.arg == null ? "0" : slot.arg;
        boolean reopen = slot.reopenAfterAct;
        boolean closeFirst = slot.closeBeforeAct;
        Player subject = AdminInspectSessions.resolveSubject(player);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (closeFirst) {
                player.closeInventory();
            }
            Runnable work = () -> {
                String msg = ForgeBridge.charHandleDo(subject, action, arg, ret);
                if (msg != null && !msg.isBlank()) {
                    GuiChat.sendResult(player, msg);
                }
                if (reopen) {
                    open(player, ret);
                }
            };
            if (closeFirst) {
                Bukkit.getScheduler().runTaskLater(plugin, work, 2L);
            } else {
                work.run();
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private static List<String> prependBlank(List<String> lines) {
        List<String> out = new ArrayList<>();
        out.add("");
        if (lines != null) {
            out.addAll(lines);
        }
        return out;
    }

    private static List<String> toAmp(List<String> lines) {
        if (lines == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            out.add(line == null ? "" : line.replace('§', '&'));
        }
        return out;
    }

    private static int[] centered(int count) {
        return GuiBoardHelper.centeredSlots(count);
    }

    private static void frame(Inventory inv, int size) {
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            inv.setItem(i, item(edge ? ACCENT : FILL, " ", List.of()));
        }
    }

    private static void put(Holder holder, Inventory inv, int slot, ItemStack stack) {
        put(holder, inv, slot, stack, null);
    }

    private static void put(Holder holder, Inventory inv, int slot, ItemStack stack, SlotAction action) {
        inv.setItem(slot, stack);
        if (action != null) {
            holder.bindAction(slot, action);
        }
    }

    private static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null) {
                List<String> colored = new ArrayList<>();
                for (String line : lore) {
                    colored.add(color(line));
                }
                meta.setLore(colored);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static Map<String, String> charCooldownVars(Map<String, String> ph) {
        Map<String, String> vars = new HashMap<>();
        vars.put("race_cooldown", cooldownAmp(ph == null ? null : ph.get("race_cooldown")));
        vars.put("class_cooldown", cooldownAmp(ph == null ? null : ph.get("class_cooldown")));
        vars.put("reskin_cooldown", cooldownAmp(ph == null ? null : ph.get("reskin_cooldown")));
        return vars;
    }

    private static String cooldownAmp(String line) {
        if (line == null || line.isBlank()) {
            return "&aReady to use";
        }
        return line.replace('§', '&');
    }

    private static String prettyId(String id) {
        if (id == null || id.isBlank()) {
            return "?";
        }
        String[] parts = id.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    private static ItemStack tipBtn(
            String key, Material mat, String name, List<String> defaults, Map<String, String> vars
    ) {
        return item(mat, GuiTooltips.name(key, name, vars),
                GuiTooltips.buttonLore(key, defaults, vars, null));
    }

    private static ItemStack pageBtn(String key, Material mat, String name, String sub) {
        List<String> defaults = new ArrayList<>();
        if (sub != null) {
            defaults.add(sub);
        }
        return item(mat, GuiTooltips.name(key, name), GuiTooltips.buttonLore(key, defaults, null, null));
    }

    private static ItemStack hubBtn() {
        return GuiNav.hubItem();
    }

    private static ItemStack closeBtn(String key) {
        return item(Material.BARRIER, GuiTooltips.name(key, "&cClose"),
                GuiTooltips.buttonLore(key, List.of("&7Close this menu"), null, null));
    }

    private static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    private static final class Holder implements InventoryHolder {
        private final String pageId;
        private final Map<Integer, SlotAction> actions = new java.util.HashMap<>();
        private Inventory inv;

        Holder(String pageId) {
            this.pageId = pageId;
        }

        void bind(Inventory inventory) {
            this.inv = inventory;
        }

        void bindAction(int slot, SlotAction action) {
            actions.put(slot, action);
        }

        SlotAction actionAt(int slot) {
            return actions.get(slot);
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private static final class SlotAction {
        final String action;
        final String arg;
        final String returnPage;
        final String page;
        final String rawCommand;
        final boolean shouldClose;
        final boolean reopenAfterAct;
        final boolean closeBeforeAct;

        SlotAction(
                String action,
                String arg,
                String returnPage,
                String page,
                String rawCommand,
                boolean shouldClose,
                boolean reopenAfterAct,
                boolean closeBeforeAct
        ) {
            this.action = action;
            this.arg = arg;
            this.returnPage = returnPage;
            this.page = page;
            this.rawCommand = rawCommand;
            this.shouldClose = shouldClose;
            this.reopenAfterAct = reopenAfterAct;
            this.closeBeforeAct = closeBeforeAct;
        }

        static SlotAction page(String page) {
            return new SlotAction(null, null, null, page, null, false, false, false);
        }

        static SlotAction act(String action, String arg, String returnPage) {
            return new SlotAction(action, arg, returnPage, null, null, false, true, false);
        }

        static SlotAction actNoReopen(String action, String arg) {
            return new SlotAction(action, arg, null, null, null, false, false, true);
        }

        static SlotAction cmd(String command) {
            return new SlotAction(null, null, null, null, command, false, false, false);
        }

        static SlotAction dismiss() {
            return new SlotAction(null, null, null, null, null, true, false, false);
        }
    }
}
