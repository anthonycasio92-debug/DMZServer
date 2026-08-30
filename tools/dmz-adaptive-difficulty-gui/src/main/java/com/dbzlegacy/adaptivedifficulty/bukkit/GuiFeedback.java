package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.Zrips.CMILib.GUI.CMIGui;
import net.Zrips.CMILib.GUI.CMIGuiButton;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Shows action results on the inventory header (slot 4) instead of chat when
 * {@code guiBackend} is chest/CMI. Chat backend keeps {@link GuiChat#sendResult}.
 */
final class GuiFeedback {
    private static final long TTL_MS = 45_000L;
    private static final int HEADER_SLOT = 4;
    private static final int MAX_LINES = 8;

    private static final Map<UUID, Entry> PENDING = new ConcurrentHashMap<>();

    private GuiFeedback() {}

    record Entry(boolean ok, List<String> linesAmp, long expiresAt) {}

    /** True when inventory GUIs are preferred over chat menus. */
    static boolean preferGui() {
        String backend = ForgeBridge.guiBackend();
        return backend != null && !"chat".equalsIgnoreCase(backend);
    }

    /** Stash a Forge/plugin result for the next inventory paint. */
    static void setFromResult(Player player, String msg) {
        if (player == null || msg == null || msg.isBlank()) {
            return;
        }
        List<String> lines = new ArrayList<>();
        boolean ok = true;
        for (String raw : msg.split("\n")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String line = raw.startsWith("§") ? raw : "§a" + raw;
            String plain = GuiBoardHelper.strip(line).trim();
            if (plain.isEmpty()
                    || plain.startsWith("---")
                    || plain.startsWith("──")
                    || plain.startsWith("----")) {
                continue;
            }
            if (line.startsWith("§c") || line.contains("§c")) {
                ok = false;
            }
            lines.add(line.replace('§', '&'));
            if (lines.size() >= MAX_LINES) {
                break;
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        PENDING.put(
                player.getUniqueId(),
                new Entry(ok, List.copyOf(lines), System.currentTimeMillis() + TTL_MS));
    }

    static void clear(Player player) {
        if (player != null) {
            PENDING.remove(player.getUniqueId());
        }
    }

    static Entry consume(Player player) {
        if (player == null) {
            return null;
        }
        Entry e = PENDING.remove(player.getUniqueId());
        if (e == null) {
            return null;
        }
        if (e.expiresAt < System.currentTimeMillis()) {
            return null;
        }
        return e;
    }

    /** Paint pending feedback onto slot 4, then open the chest. */
    static void openChest(Player player, Inventory inv) {
        if (player == null || inv == null) {
            return;
        }
        paintChest(player, inv, HEADER_SLOT);
        player.openInventory(inv);
    }

    static void paintChest(Player player, Inventory inv, int slot) {
        Entry entry = consume(player);
        if (entry == null || inv == null) {
            return;
        }
        ItemStack stack = inv.getItem(slot);
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        stack = stack.clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> oldLore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        String first = entry.linesAmp.get(0);
        meta.setDisplayName(color(first));
        List<String> lore = new ArrayList<>();
        for (int i = 1; i < entry.linesAmp.size(); i++) {
            lore.add(color(entry.linesAmp.get(i)));
        }
        lore.add(color(entry.ok ? "&8────────" : "&c────────"));
        lore.addAll(oldLore);
        meta.setLore(lore);
        stack.setItemMeta(meta);
        inv.setItem(slot, stack);
    }

    /** Apply pending feedback to CMI header slot 4, then open. */
    static void openCmi(CMIGui gui) {
        if (gui == null) {
            return;
        }
        applyCmi(gui);
        gui.open();
    }

    static void applyCmi(CMIGui gui) {
        if (gui == null) {
            return;
        }
        Player player = gui.getPlayer();
        Entry entry = consume(player);
        if (entry == null) {
            return;
        }
        CMIGuiButton btn = gui.getButton(HEADER_SLOT);
        if (btn == null) {
            return;
        }
        List<String> oldLore = new ArrayList<>();
        try {
            ItemStack item = btn.getItem(player);
            if (item != null && item.hasItemMeta() && item.getItemMeta() != null
                    && item.getItemMeta().hasLore()
                    && item.getItemMeta().getLore() != null) {
                for (String line : item.getItemMeta().getLore()) {
                    oldLore.add(line == null ? "" : line.replace('§', '&'));
                }
            }
        } catch (Throwable ignored) {
        }
        btn.setName(entry.linesAmp.get(0));
        btn.clearLore();
        List<String> lore = new ArrayList<>();
        for (int i = 1; i < entry.linesAmp.size(); i++) {
            lore.add(entry.linesAmp.get(i));
        }
        lore.add(entry.ok ? "&8────────" : "&c────────");
        lore.addAll(oldLore);
        btn.addLore(lore);
    }

    private static String color(String amp) {
        return ChatColor.translateAlternateColorCodes('&', amp == null ? "" : amp);
    }
}
