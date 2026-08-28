package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Shared inventory layout helpers — centered rows, top boards with heads, detail tiles. */
final class GuiBoardHelper {
    /** Centered row starts for a framed 5-row (45) chest: rows 2–4 interior. */
    static final int[] ROW_STARTS = {10, 19, 28};
    static final int ROW_WIDTH = 7;

    private static final Pattern TOP_LINE = Pattern.compile(
            "^#\\s*(\\d+)\\s+(.+?)\\s{2,}(.+)$|^#\\s*(\\d+)\\s+(\\S+)\\s+(.+)$");

    private GuiBoardHelper() {}

    /**
     * Place up to {@code count} items centered across interior rows (10–16, 19–25, 28–34).
     * ≤7 items → single centered middle row. More → fill top→bottom, each row centered.
     */
    static int[] centeredSlots(int count) {
        int n = Math.max(0, Math.min(count, ROW_STARTS.length * ROW_WIDTH));
        if (n == 0) {
            return new int[0];
        }
        List<Integer> slots = new ArrayList<>();
        if (n <= ROW_WIDTH) {
            int start = 19 + (ROW_WIDTH - n) / 2;
            for (int i = 0; i < n; i++) {
                slots.add(start + i);
            }
        } else {
            int remaining = n;
            for (int row = 0; row < ROW_STARTS.length && remaining > 0; row++) {
                int take = Math.min(ROW_WIDTH, remaining);
                int start = ROW_STARTS[row] + (ROW_WIDTH - take) / 2;
                for (int i = 0; i < take; i++) {
                    slots.add(start + i);
                }
                remaining -= take;
            }
        }
        int[] out = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            out[i] = slots.get(i);
        }
        return out;
    }

    /** Single centered row (middle) for small button groups. */
    static int[] centeredRow(int count) {
        int n = Math.max(0, Math.min(count, ROW_WIDTH));
        int start = 19 + (ROW_WIDTH - n) / 2;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = start + i;
        }
        return out;
    }

    static final class TopEntry {
        final int rank;
        final String name;
        final String value;

        TopEntry(int rank, String name, String value) {
            this.rank = rank;
            this.name = name == null ? "?" : name;
            this.value = value == null ? "" : value;
        }
    }

    /**
     * Parse Forge top lines like {@code §e#1 §fName §7RP §f1234} into entries.
     * Skips headers / empty / "No … data" lines.
     */
    static List<TopEntry> parseTopEntries(List<String> lines) {
        List<TopEntry> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (String raw : lines) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String plain = strip(raw).trim();
            if (plain.isEmpty()
                    || plain.toLowerCase(Locale.ROOT).contains("top")
                    || plain.toLowerCase(Locale.ROOT).startsWith("no ")
                    || plain.startsWith("---")) {
                continue;
            }
            Matcher m = Pattern.compile("^#\\s*(\\d+)\\s+(\\S+)\\s*(.*)$").matcher(plain);
            if (m.find()) {
                int rank = Integer.parseInt(m.group(1));
                String name = m.group(2);
                String value = m.group(3) == null ? "" : m.group(3).trim();
                out.add(new TopEntry(rank, name, value));
            }
        }
        return out;
    }

    /** Build a player-head stack for a leaderboard entry. */
    static ItemStack topHead(TopEntry entry) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Rank &e#" + entry.rank);
        if (entry.value != null && !entry.value.isBlank()) {
            lore.add("&f" + entry.value);
        }
        lore.add("");
        lore.add("&8Leaderboard");
        Player online = org.bukkit.Bukkit.getPlayerExact(entry.name);
        if (online != null) {
            return GuiPlayerPicker.head(online, "&e#" + entry.rank + " &f" + entry.name, lore);
        }
        return GuiPlayerPicker.headByName(entry.name, "&e#" + entry.rank + " &f" + entry.name, lore);
    }

    /**
     * Split detail lines into per-item lore blocks (skip blank / pure separators).
     * First line may be a section header — kept as its own tile title source.
     */
    static List<DetailTile> detailTiles(List<String> lines) {
        List<DetailTile> tiles = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            tiles.add(new DetailTile("&7Empty", Material.BARRIER, List.of("&7Nothing here yet.")));
            return tiles;
        }
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String plain = strip(line).trim();
            if (plain.isEmpty() || plain.startsWith("---") || plain.startsWith("──")) {
                continue;
            }
            cleaned.add(line);
        }
        if (cleaned.isEmpty()) {
            tiles.add(new DetailTile("&7Empty", Material.BARRIER, List.of("&7Nothing here yet.")));
            return tiles;
        }
        // Group: header-looking lines start a tile; indented "- " lines append to current.
        DetailTile current = null;
        for (String line : cleaned) {
            String plain = strip(line).trim();
            boolean cont = plain.startsWith("-") || line.contains("§8  -") || line.contains("&8  -");
            if (cont && current != null) {
                current.lore.add(amp(line));
                continue;
            }
            String title = amp(plain.length() > 40 ? plain.substring(0, 40) + "…" : plain);
            // Prefer short title from before colon
            int colon = plain.indexOf(':');
            if (colon > 0 && colon < 28) {
                title = amp(plain.substring(0, colon).trim());
            }
            current = new DetailTile(title.startsWith("&") ? title : "&f" + title,
                    iconFor(plain), new ArrayList<>());
            current.lore.add(amp(line));
            tiles.add(current);
        }
        return tiles;
    }

    static final class DetailTile {
        final String title;
        final Material icon;
        final List<String> lore;

        DetailTile(String title, Material icon, List<String> lore) {
            this.title = title;
            this.icon = icon == null ? Material.PAPER : icon;
            this.lore = lore;
        }
    }

    static Material iconFor(String plain) {
        String n = plain.toLowerCase(Locale.ROOT);
        if (n.contains("rp") || n.contains("season")) {
            return Material.GOLD_INGOT;
        }
        if (n.contains("win") || n.contains("record") || n.contains("ko")) {
            return Material.IRON_SWORD;
        }
        if (n.contains("streak") || n.contains("combo")) {
            return Material.BLAZE_POWDER;
        }
        if (n.contains("quest")) {
            return Material.WRITABLE_BOOK;
        }
        if (n.contains("achiev") || n.contains("✔")) {
            return Material.DIAMOND;
        }
        if (n.contains("hall") || n.contains("fame") || n.contains("hof")) {
            return Material.GOLD_BLOCK;
        }
        if (n.contains("journal") || n.contains("vs ")) {
            return Material.MAP;
        }
        if (n.contains("title") || n.contains("tier")) {
            return Material.NAME_TAG;
        }
        if (n.contains("mentor") || n.contains("apprentice")) {
            return Material.EMERALD;
        }
        if (n.contains("session") || n.contains("spar")) {
            return Material.CLOCK;
        }
        if (n.contains("tp") || n.contains("training")) {
            return Material.EXPERIENCE_BOTTLE;
        }
        if (n.contains("mutual") || n.contains("rival")) {
            return Material.PLAYER_HEAD;
        }
        if (n.contains("challenge")) {
            return Material.GOLDEN_SWORD;
        }
        return Material.BOOK;
    }

    static String strip(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("§.", "").replaceAll("&[0-9a-fk-or]", "");
    }

    static String amp(String s) {
        return s == null ? "" : s.replace('§', '&');
    }
}
