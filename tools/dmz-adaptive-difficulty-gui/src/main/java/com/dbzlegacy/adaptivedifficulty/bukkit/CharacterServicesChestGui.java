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
                default -> main(viewer, subject);
            };
        }
        GuiFeedback.openChest(viewer, inv);
    }

    private static boolean inspecting(Player viewer, Player subject) {
        return viewer != null && subject != null
                && !viewer.getUniqueId().equals(subject.getUniqueId());
    }

    private static String invTitle(Player viewer, Player subject, String base) {
        if (inspecting(viewer, subject)) {
            return color(base + " · &c" + subject.getName());
        }
        return color(base);
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
                    List.of("", bridgeOk ? "&cCharacter Services are turned off" : "&cLegacyMechanics mod unreachable"));
            footer45(holder, inv, "character.main.back_hub", SlotAction.cmd("lmdo lm open hub"));
            return inv;
        }

        putProfile(holder, inv, GuiTooltips.name("character.main.wallet", "&d&lCharacter Services", vars),
                profileLore(subject, vars));

        put(holder, inv, 20, tipBtn("character.main.race", Material.NETHER_STAR, "&eChange Race",
                List.of("&7Pick a new race and how much progress to keep",
                        "&8{race_cooldown}", "&eClick to continue"), vars),
                SlotAction.page("race"));
        put(holder, inv, 22, tipBtn("character.main.class", Material.ENCHANTED_BOOK, "&bChange Class",
                List.of("&7Swap fighting class — base stats stay",
                        "&7Cost &f{class_cost}",
                        "&8{class_cooldown}", "&eClick to continue"), vars),
                SlotAction.page("class"));
        put(holder, inv, 24, tipBtn("character.main.reskin", Material.AMETHYST_CLUSTER, "&dReskin",
                List.of("&7Cosmetic look only",
                        "&7Cost &f{reskin_cost}",
                        "&8{reskin_cooldown}", "&eClick to continue"), vars),
                SlotAction.page("reskin"));

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
        lore.add("&8Pay-up OK · change returned");
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
                    SlotAction.page("race_pct:" + id + ":100"));
        }
        footer54(holder, inv, "character.race.back", SlotAction.page("main"));
        return inv;
    }

    private Inventory racePct(Player viewer, Player subject, String raceAndPct) {
        String race = raceAndPct;
        int defaultPct = 100;
        int colon = raceAndPct.lastIndexOf(':');
        if (colon > 0) {
            race = raceAndPct.substring(0, colon);
            try {
                defaultPct = Integer.parseInt(raceAndPct.substring(colon + 1));
            } catch (NumberFormatException ignored) {
                defaultPct = 100;
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
        int[] pcts = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30};
        for (int i = 0; i < pcts.length && i < slots.length; i++) {
            int pct = pcts[i];
            boolean selected = pct == defaultPct;
            Map<String, String> pctVars = Map.of("pct", String.valueOf(pct));
            put(holder, inv, slots[i], item(
                    selected ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE,
                    (selected ? "&a" : "&f") + pct + "%",
                    GuiTooltips.buttonLore("character.race_pct.pct",
                            List.of("&7Keep " + pct + "% of core stats", "&eClick to review"), pctVars, null)),
                    SlotAction.page("race_confirm:" + race + ":" + pct));
        }
        footer54(holder, inv, "character.race_pct.back", SlotAction.page("race"));
        return inv;
    }

    private Inventory raceConfirm(Player viewer, Player subject, String raceAndPct) {
        String[] bits = raceAndPct.split(":", 2);
        String race = bits.length > 0 ? bits[0] : "";
        String pct = bits.length > 1 ? bits[1] : "100";
        Holder holder = new Holder("race_confirm");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Confirm Race"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> summary = prependBlank(toAmp(ForgeBridge.charLines(subject, "race_confirm:" + race + ":" + pct)));
        put(holder, inv, 4, item(Material.ORANGE_CONCRETE,
                GuiTooltips.name("character.race_confirm.header", "&c&lLast Chance"), summary));
        put(holder, inv, 20, tipBtn("character.race_confirm.confirm", Material.LIME_CONCRETE, "&a&lConfirm Race Change",
                List.of("&7Pay and switch races", "&eClick to confirm"), null),
                SlotAction.act("race_confirm", race + ":" + pct, "main"));
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
                List.of("&7Pay and switch class", "&eClick to confirm"), null),
                SlotAction.act("class_confirm", classId, "main"));
        put(holder, inv, 24, tipBtn("character.class_confirm.cancel", Material.RED_CONCRETE, "&cCancel",
                List.of("&7Go back without paying"), null),
                SlotAction.page("class"));
        footer45(holder, inv, "character.class_confirm.back", SlotAction.page("class"));
        return inv;
    }

    private Inventory reskin(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.charPlaceholders(subject);
        Map<String, String> vars = charCooldownVars(ph);
        vars.put("reskin_cost", ph.getOrDefault("reskin_cost", "?"));

        Holder holder = new Holder("reskin");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Reskin"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.PAINTING,
                GuiTooltips.name("character.reskin.header", "&d&lReskin"),
                prependBlank(toAmp(ForgeBridge.charLines(subject, "reskin")))));
        put(holder, inv, 22, tipBtn("character.reskin.open", Material.LIME_CONCRETE, "&a&lPay & Open Appearance",
                List.of("&7Cost &f{reskin_cost}", "&7Opens the in-game look editor",
                        "&8Pay-up OK · change returned", "&eClick to pay and open"), vars),
                SlotAction.actNoReopen("reskin_confirm", "0"));
        footer45(holder, inv, "character.reskin.back", SlotAction.page("main"));
        return inv;
    }

    private static void putProfile(Holder holder, Inventory inv, String title, List<String> lore) {
        put(holder, inv, 4, item(Material.GOLD_INGOT, title, lore));
    }

    private void footer45(Holder holder, Inventory inv, String backKey, SlotAction backAction) {
        put(holder, inv, 36, pageBtn(backKey, Material.ARROW, "&7« Back", null), backAction);
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn("character.main.close"), SlotAction.dismiss());
    }

    private void footer54(Holder holder, Inventory inv, String backKey, SlotAction backAction) {
        put(holder, inv, 45, pageBtn(backKey, Material.ARROW, "&7« Back", null), backAction);
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
        return item(Material.COMPASS, "&7« Hub", List.of());
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
