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

/** Character Services — race / class / reskin (Ancient Coins). */
public final class CharacterServicesChestGui implements Listener {
    private final AdaptiveDifficultyGuiPlugin plugin;

    public CharacterServicesChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String page) {
        Player subject = AdminInspectSessions.resolveSubject(player);
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        Inventory inv;
        if (p.startsWith("race_confirm:")) {
            inv = raceConfirm(player, subject, p.substring("race_confirm:".length()));
        } else if (p.startsWith("race_pct:")) {
            inv = racePct(player, subject, p.substring("race_pct:".length()));
        } else if (p.startsWith("class_confirm:")) {
            inv = classConfirm(player, subject, p.substring("class_confirm:".length()));
        } else {
            inv = switch (p) {
                case "race" -> raceList(player, subject);
                case "class" -> classList(player, subject);
                case "reskin" -> reskin(player, subject);
                default -> main(player, subject);
            };
        }
        GuiFeedback.openChest(player, inv);
    }

    private Inventory main(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.charPlaceholders(subject);
        Map<String, String> vars = charCooldownVars(ph);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Character Services"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> header = new ArrayList<>();
        header.add("");
        header.addAll(toAmp(ForgeBridge.charLines(subject, "main")));
        header.add("");
        header.add("&7Race &f" + ph.getOrDefault("current_race", "?")
                + " &8· &7Class &f" + ph.getOrDefault("current_class", "?"));
        header.add("&7Ancient Coins &f" + ph.getOrDefault("ancient_coins", "0"));
        put(holder, inv, 4, item(Material.PLAYER_HEAD,
                GuiTooltips.name("character.main.header", "&f&lCharacter Services"), header));

        put(holder, inv, 20, tipBtn("character.main.race", Material.NETHER_STAR, "&eChange Race",
                List.of("&7Pick a new race and how much progress to keep",
                        "&8{race_cooldown}", "&eClick to continue"), vars),
                SlotAction.page("race"));
        put(holder, inv, 22, tipBtn("character.main.class", Material.ENCHANTED_BOOK, "&bChange Class",
                List.of("&7Swap fighting class — base stats stay",
                        "&8{class_cooldown}", "&eClick to continue"), vars),
                SlotAction.page("class"));
        put(holder, inv, 24, tipBtn("character.main.reskin", Material.PAINTING, "&dReskin",
                List.of("&7Cosmetic look only",
                        "&8{reskin_cooldown}", "&eClick to continue"), vars),
                SlotAction.page("reskin"));

        put(holder, inv, 36, pageBtn("character.main.back_hub", Material.ARROW, "&7Back", "&7Return to hub"),
                SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn("character.main.close"), SlotAction.dismiss());
        return inv;
    }

    private Inventory raceList(Player viewer, Player subject) {
        Holder holder = new Holder("race");
        Inventory inv = Bukkit.createInventory(holder, 54, color("&8Change Race"));
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
            Material mat = current ? Material.LIME_DYE : Material.PAPER;
            String cardKey = current ? "character.race.card_current" : "character.race.card_pick";
            put(holder, inv, slots[i], item(mat, (current ? "&a" : "&f") + name,
                    GuiTooltips.buttonLore(cardKey, List.of(), null, null)),
                    SlotAction.page("race_pct:" + id + ":100"));
        }
        put(holder, inv, 49, pageBtn("character.race.back", Material.ARROW, "&7Back", "&7Character Services"),
                SlotAction.page("main"));
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
        Inventory inv = Bukkit.createInventory(holder, 54, color("&8Preservation %"));
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
            Map<String, String> pctVars = Map.of("pct", String.valueOf(pct));
            put(holder, inv, slots[i], item(
                    pct == defaultPct ? Material.LIME_DYE : Material.GRAY_DYE,
                    "&f" + pct + "%",
                    GuiTooltips.buttonLore("character.race_pct.pct",
                            List.of("&7Keep " + pct + "% of core stats", "&eClick to review"), pctVars, null)),
                    SlotAction.page("race_confirm:" + race + ":" + pct));
        }
        put(holder, inv, 49, pageBtn("character.race_pct.back", Material.ARROW, "&7Back", "&7Race list"),
                SlotAction.page("race"));
        return inv;
    }

    private Inventory raceConfirm(Player viewer, Player subject, String raceAndPct) {
        String[] bits = raceAndPct.split(":", 2);
        String race = bits.length > 0 ? bits[0] : "";
        String pct = bits.length > 1 ? bits[1] : "100";
        Holder holder = new Holder("race_confirm");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Confirm Race Change"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.ORANGE_STAINED_GLASS,
                GuiTooltips.name("character.race_confirm.header", "&c&lLast Chance"),
                prependBlank(toAmp(ForgeBridge.charLines(subject, "race_confirm:" + race + ":" + pct)))));
        put(holder, inv, 20, tipBtn("character.race_confirm.confirm", Material.LIME_DYE, "&a&lConfirm Race Change",
                List.of("&7Pay and switch races", "&eClick to confirm"), null),
                SlotAction.act("race_confirm", race + ":" + pct, "main"));
        put(holder, inv, 24, tipBtn("character.race_confirm.cancel", Material.RED_DYE, "&cCancel",
                List.of("&7Go back without paying"), null),
                SlotAction.page("race_pct:" + race + ":" + pct));
        put(holder, inv, 36, pageBtn("character.race_confirm.back", Material.ARROW, "&7Back", "&7Preservation"),
                SlotAction.page("race_pct:" + race + ":" + pct));
        return inv;
    }

    private Inventory classList(Player viewer, Player subject) {
        Holder holder = new Holder("class");
        Inventory inv = Bukkit.createInventory(holder, 54, color("&8Change Class"));
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
            String cardKey = current ? "character.class.card_current" : "character.class.card_pick";
            put(holder, inv, slots[i], item(current ? Material.LIME_DYE : Material.BOOK, "&f" + name,
                    GuiTooltips.buttonLore(cardKey, List.of(), null, null)),
                    SlotAction.page("class_confirm:" + id));
        }
        put(holder, inv, 49, pageBtn("character.class.back", Material.ARROW, "&7Back", "&7Character Services"),
                SlotAction.page("main"));
        return inv;
    }

    private Inventory classConfirm(Player viewer, Player subject, String classId) {
        Holder holder = new Holder("class_confirm");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Confirm Class"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.ORANGE_STAINED_GLASS,
                GuiTooltips.name("character.class_confirm.header", "&c&lConfirm Class Change"),
                prependBlank(toAmp(ForgeBridge.charLines(subject, "class_confirm:" + classId)))));
        put(holder, inv, 20, tipBtn("character.class_confirm.confirm", Material.LIME_DYE, "&a&lConfirm",
                List.of("&7Pay and switch class", "&eClick to confirm"), null),
                SlotAction.act("class_confirm", classId, "main"));
        put(holder, inv, 24, tipBtn("character.class_confirm.cancel", Material.RED_DYE, "&cCancel",
                List.of("&7Go back without paying"), null),
                SlotAction.page("class"));
        put(holder, inv, 36, pageBtn("character.class_confirm.back", Material.ARROW, "&7Back", "&7Class list"),
                SlotAction.page("class"));
        return inv;
    }

    private Inventory reskin(Player viewer, Player subject) {
        Holder holder = new Holder("reskin");
        Inventory inv = Bukkit.createInventory(holder, 45, color("&8Reskin"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.PAINTING,
                GuiTooltips.name("character.reskin.header", "&d&lReskin"),
                prependBlank(toAmp(ForgeBridge.charLines(subject, "reskin")))));
        put(holder, inv, 22, tipBtn("character.reskin.open", Material.LIME_DYE, "&a&lPay & Open Appearance",
                List.of("&7Opens the in-game look editor", "&eClick to pay and open"), null),
                SlotAction.actNoReopen("reskin_confirm", "0"));
        put(holder, inv, 36, pageBtn("character.reskin.back", Material.ARROW, "&7Back", "&7Character Services"),
                SlotAction.page("main"));
        return inv;
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
                // Let the client drop the chest GUI before DMZ opens recustomize.
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

    private static String strip(String s) {
        return s == null ? "" : s.replace('§', '&');
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
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(" "));
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, pane);
            }
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
        var meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null) {
                meta.setLore(lore.stream().map(CharacterServicesChestGui::color).toList());
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
        return item(Material.NETHER_STAR, "&fHub", List.of("", "&7Back to &f/lm"));
    }

    private static ItemStack closeBtn(String key) {
        return item(Material.BARRIER, GuiTooltips.name(key, "&cClose"),
                GuiTooltips.buttonLore(key, List.of("&7Close this menu"), null, null));
    }

    private static String color(String input) {
        if (input == null) {
            return "";
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', input);
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
