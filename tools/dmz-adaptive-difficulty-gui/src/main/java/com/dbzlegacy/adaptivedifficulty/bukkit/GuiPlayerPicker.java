package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/** Shared online-player head helpers for Rival / Spar inventory pickers. */
final class GuiPlayerPicker {
    /** Interior slots of a 5-row chest (skip edges used by frame). */
    static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private GuiPlayerPicker() {}

    static List<Player> onlineExcept(Player self) {
        List<Player> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p == null) {
                continue;
            }
            if (self != null && p.getUniqueId().equals(self.getUniqueId())) {
                continue;
            }
            out.add(p);
        }
        out.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    static ItemStack head(Player target, String title, List<String> tip) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        if (meta != null) {
            try {
                meta.setOwningPlayer(target);
            } catch (Throwable ignored) {
                // Fall back to unnamed head if skull owner fails.
            }
            meta.setDisplayName(color(title));
            List<String> lore = new ArrayList<>();
            lore.add("");
            for (String line : tip) {
                lore.add(color(line));
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    static ItemStack headByName(String name, String title, List<String> tip) {
        ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        if (meta != null) {
            try {
                OfflinePlayer off = Bukkit.getOfflinePlayer(name);
                meta.setOwningPlayer(off);
            } catch (Throwable ignored) {
            }
            meta.setDisplayName(color(title));
            List<String> lore = new ArrayList<>();
            lore.add("");
            for (String line : tip) {
                lore.add(color(line));
            }
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }
}
