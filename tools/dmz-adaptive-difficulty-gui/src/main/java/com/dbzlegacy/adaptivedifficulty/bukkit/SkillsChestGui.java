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

/** Bukkit chest GUI fallback — Legacy Mechanics Skills. */
public final class SkillsChestGui implements Listener {
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private final AdaptiveDifficultyGuiPlugin plugin;

    public SkillsChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
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

    public void open(Player player, String page) {
        Player viewer = player;
        Player subject = AdminInspectSessions.resolveSubject(viewer);
        String p = page == null || page.isBlank() ? "core" : page.toLowerCase(Locale.ROOT);
        Inventory inv = switch (p) {
            // Advanced folded into Saga — alias keeps old links working.
            case "advanced", "dmz", "saga" ->
                    pageInv(viewer, subject, "saga", "&dSaga", Material.AMETHYST_SHARD);
            case "help" -> pageInv(viewer, subject, "core", "&aNatural", Material.FEATHER);
            case "natural" -> pageInv(viewer, subject, "core", "&aNatural", Material.FEATHER);
            default -> pageInv(viewer, subject, "core", "&aNatural", Material.FEATHER);
        };
        GuiFeedback.openChest(viewer, inv);
    }

    private Inventory pageInv(Player viewer, Player subject, String page, String title, Material mat) {
        Map<String, String> ph = ForgeBridge.skillsPlaceholders(subject);
        // SkillCheck session stays on viewer; staff admin browser gated on viewer staff.
        boolean skillCheckUi = ForgeBridge.inSkillCheckSession(viewer);
        boolean staffAdmin = ForgeBridge.isStaff(viewer) && !skillCheckUi;
        Holder holder = new Holder(page);
        String baseTitle = skillCheckUi
                ? "&8Skill Check"
                : staffAdmin ? "&8Skills (Admin)" : "&8Skills";
        Inventory inv = Bukkit.createInventory(holder, 54, invTitle(viewer, subject, baseTitle));
        holder.bind(inv);
        frameOnly(inv, 54);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        if (!bridgeOk || !systemOn) {
            put(holder, inv, 4, item(mat,
                    !bridgeOk ? "&c&lUNAVAILABLE" : "&c&lSKILLS DISABLED",
                    unavailableLore(bridgeOk)));
            put(holder, inv, 49, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
            put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        List<String> raw = toAmp(ForgeBridge.skillsLines(subject, page));
        if ("help".equals(page)) {
            put(holder, inv, 4, item(mat, title, prependBlank(raw.isEmpty()
                    ? List.of("&7Use Natural · Saga tabs.") : raw)));
        } else {
            GuiLoreChunks.SkillPage split = GuiLoreChunks.splitSkillsPage(raw);
            List<String> headerLore = new ArrayList<>();
            headerLore.add("");
            headerLore.addAll(split.header.isEmpty()
                    ? List.of("&7DMZ stats unavailable") : split.header);
            headerLore.add("");
            headerLore.addAll(GuiTooltips.lore("skills.main.header", GuiBoardHelper.tips(viewer,
                    skillCheckUi ? "&eSkill Check · one item per skill" : "&8One item per skill below")));
            // Skill Check: EXPERIENCE_BOTTLE header. Staff Skills: BOOK.
            Material headerMat = skillCheckUi ? Material.EXPERIENCE_BOTTLE : Material.BOOK;
            put(holder, inv, 4, item(headerMat,
                    skillCheckUi ? title + " Skill Check"
                            : staffAdmin ? title + " (Admin)" : title + " Skills",
                    headerLore));

            int placed = 0;
            for (List<String> skill : split.skills) {
                if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                    break;
                }
                int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
                String name = GuiLoreChunks.skillDisplayName(skill);
                Material icon = GuiLoreChunks.skillIcon(name);
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.addAll(skill);
                lore.add("");
                lore.add(skillUnlocked(skill) ? "&aUnlocked" : "&cLocked / in progress");
                put(holder, inv, slot, item(icon, name, lore));
            }
            if (placed == 0) {
                put(holder, inv, 22, tipBtn(viewer, "skills.empty", Material.BARRIER, "&cNo skills listed",
                        List.of("&7Bridge returned no skill rows")));
            }
        }

        put(holder, inv, 45, pageBtn(viewer, "skills.main.natural", Material.FEATHER, "&aNatural",
                        "&7Potential · Flight · Meditation · Jump · Sprint"),
                SlotAction.page("core"));
        put(holder, inv, 46, pageBtn(viewer, "skills.main.saga", Material.AMETHYST_SHARD, "&dSaga",
                        "&7Unlock and level up via the skills saga."),
                SlotAction.page("saga"));
        put(holder, inv, 49, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        if (staffAdmin) {
            put(holder, inv, 51, tipBtn(viewer, "skills.main.progression", Material.BREWING_STAND, "&dProgression",
                    List.of("&7Skills · TP · Race · Combat flags", "&eClick to open")),
                    SlotAction.cmd("lmdo lm open progression"));
        }
        put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private static boolean skillUnlocked(List<String> skillLore) {
        if (skillLore == null || skillLore.isEmpty()) {
            return false;
        }
        String first = skillLore.get(0);
        if (first.contains("MAX")) {
            return true;
        }
        String plain = first.replace('§', '&');
        int slash = plain.lastIndexOf('/');
        if (slash > 0) {
            try {
                String before = plain.substring(Math.max(0, slash - 4), slash).replaceAll("[^0-9]", "");
                if (!before.isEmpty() && Integer.parseInt(before) > 0) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static List<String> unavailableLore(boolean bridgeOk) {
        if (!bridgeOk) {
            return List.of("", "&cForge LegacyMechanics mod unreachable");
        }
        return List.of("", "&cSkill unlock service is disabled");
    }

    private static List<String> toAmp(List<String> lines) {
        List<String> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            out.add(line == null ? "" : line.replace('§', '&'));
        }
        return out;
    }

    private static List<String> prependBlank(List<String> tip) {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(tip);
        return out;
    }

    private static void frameOnly(Inventory inv, int size) {
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            if (edge) {
                inv.setItem(i, item(ACCENT, " ", List.of()));
            }
        }
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
        SlotAction slotAction = holder.actionAt(event.getSlot());
        if (slotAction == null) {
            return;
        }
        if (slotAction.shouldClose) {
            player.closeInventory();
            return;
        }
        if (slotAction.page != null) {
            final String targetPage = slotAction.page;
            Bukkit.getScheduler().runTask(plugin, () -> open(player, targetPage));
            return;
        }
        if (slotAction.rawCommand != null && !slotAction.rawCommand.isBlank()) {
            final String cmd = slotAction.rawCommand;
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                player.performCommand(cmd);
            });
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private static void put(Holder holder, Inventory inv, int slot, ItemStack stack) {
        put(holder, inv, slot, stack, null);
    }

    private static void put(Holder holder, Inventory inv, int slot, ItemStack stack, SlotAction action) {
        inv.setItem(slot, stack);
        if (holder != null && action != null) {
            holder.bindAction(slot, action);
        }
    }

    private static ItemStack tipBtn(Player player, Material mat, String name, List<String> tip) {
        return tipBtn(player, null, mat, name, tip);
    }

    private static ItemStack tipBtn(
            Player player, String key, Material mat, String name, List<String> tip
    ) {
        if (key == null || key.isBlank()) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (tip != null) {
                lore.addAll(tip);
            }
            return item(mat, name, lore);
        }
        return item(mat, GuiTooltips.name(key, name), GuiTooltips.buttonLore(key, tip));
    }

    private static ItemStack pageBtn(Player player, Material mat, String name, String... tips) {
        return pageBtn(player, null, mat, name, tips);
    }

    private static ItemStack pageBtn(Player player, String key, Material mat, String name, String... tips) {
        List<String> defaults = new ArrayList<>();
        if (tips != null) {
            for (String tip : tips) {
                if (tip != null) {
                    defaults.add(tip);
                }
            }
        }
        if (key == null || key.isBlank()) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(defaults);
            return item(mat, name, lore);
        }
        return item(mat, GuiTooltips.name(key, name), GuiTooltips.buttonLore(key, defaults));
    }

    private static ItemStack hubBtn() {
        return item(Material.COMPASS, "&7« Hub", List.of());
    }

    private static ItemStack closeBtn() {
        return item(Material.BARRIER, "&cClose", List.of());
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

    private static final class SlotAction {
        final String page;
        final String rawCommand;
        final boolean shouldClose;

        private SlotAction(String page, String rawCommand, boolean shouldClose) {
            this.page = page;
            this.rawCommand = rawCommand;
            this.shouldClose = shouldClose;
        }

        static SlotAction page(String page) {
            return new SlotAction(page, null, false);
        }

        static SlotAction cmd(String command) {
            return new SlotAction(null, command, false);
        }

        static SlotAction dismiss() {
            return new SlotAction(null, null, true);
        }
    }

    static final class Holder implements InventoryHolder {
        final String page;
        final Map<Integer, SlotAction> actions = new HashMap<>();
        Inventory inventory;

        Holder(String page) {
            this.page = page;
        }

        void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        void bindAction(int slot, SlotAction action) {
            if (action != null) {
                actions.put(slot, action);
            }
        }

        SlotAction actionAt(int slot) {
            return actions.get(slot);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
