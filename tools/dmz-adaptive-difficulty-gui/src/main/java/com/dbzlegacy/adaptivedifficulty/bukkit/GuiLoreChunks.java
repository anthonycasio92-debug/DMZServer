package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Split long lore lists across multiple inventory items so CMI/Bukkit do not
 * truncate a single book item to one visible line.
 */
final class GuiLoreChunks {
    static final int LINES_PER_ITEM = 11;

    private GuiLoreChunks() {}

    /** Chunk {@code lines} into groups of at most {@link #LINES_PER_ITEM}. */
    static List<List<String>> chunk(List<String> lines, int perItem) {
        int max = perItem <= 0 ? LINES_PER_ITEM : perItem;
        List<List<String>> out = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            out.add(List.of("&7Nothing here yet."));
            return out;
        }
        List<String> cleaned = new ArrayList<>();
        for (String line : lines) {
            cleaned.add(line == null ? "" : line);
        }
        for (int i = 0; i < cleaned.size(); i += max) {
            out.add(new ArrayList<>(cleaned.subList(i, Math.min(i + max, cleaned.size()))));
        }
        return out;
    }

    static List<List<String>> chunk(List<String> lines) {
        return chunk(lines, LINES_PER_ITEM);
    }

    /**
     * Build paper/book stacks for each chunk, titled {@code Part N} when more than one.
     */
    static List<ItemStack> chunkItems(Material mat, String baseTitle, List<String> lines) {
        List<List<String>> parts = chunk(lines);
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            String title = parts.size() == 1
                    ? baseTitle
                    : baseTitle + " &8(" + (i + 1) + "/" + parts.size() + ")";
            items.add(named(mat == null ? Material.PAPER : mat, title, parts.get(i)));
        }
        return items;
    }

    /**
     * Split skillsLines-style output: header lines before the separator, then group
     * each skill (+ optional continuation) into its own lore block.
     */
    static SkillPage splitSkillsPage(List<String> lines) {
        List<String> header = new ArrayList<>();
        List<List<String>> skills = new ArrayList<>();
        if (lines == null || lines.isEmpty()) {
            header.add("&7Nothing here yet.");
            return new SkillPage(header, skills);
        }
        boolean pastHeader = false;
        List<String> current = null;
        for (String raw : lines) {
            String line = raw == null ? "" : raw;
            String plain = stripColor(line).trim();
            if (!pastHeader) {
                if (plain.startsWith("---") || plain.equalsIgnoreCase("Core Skills")
                        || plain.equalsIgnoreCase("DragonMineZ 2.1 Skills")
                        || plain.equalsIgnoreCase("Saga Skills")) {
                    pastHeader = true;
                    // Skip section title — tabs already label the category.
                    continue;
                }
                header.add(line);
                continue;
            }
            if (plain.isEmpty()) {
                continue;
            }
            boolean continuation = plain.startsWith("-") || line.contains("§8  -") || line.contains("&8  -");
            if (continuation && current != null) {
                current.add(line);
            } else {
                current = new ArrayList<>();
                current.add(line);
                skills.add(current);
            }
        }
        if (header.isEmpty()) {
            header.add("&7Pick a skill below");
        }
        return new SkillPage(header, skills);
    }

    /** Display name for a skill lore block (first line, trimmed of level suffix). */
    static String skillDisplayName(List<String> skillLore) {
        if (skillLore == null || skillLore.isEmpty()) {
            return "&fSkill";
        }
        String first = skillLore.get(0);
        int colon = indexOfLevelSep(first);
        if (colon > 0) {
            return first.substring(0, colon).trim();
        }
        return first;
    }

    static Material skillIcon(String displayName) {
        String n = stripColor(displayName == null ? "" : displayName).toLowerCase(Locale.ROOT);
        if (n.contains("flight") || n.contains("fly")) {
            return Material.FEATHER;
        }
        if (n.contains("meditation")) {
            return Material.ENDER_EYE;
        }
        if (n.contains("potential")) {
            return Material.NETHER_STAR;
        }
        if (n.contains("jump")) {
            return Material.RABBIT_FOOT;
        }
        if (n.contains("sprint")) {
            return Material.SUGAR;
        }
        if (n.contains("sense")) {
            return Material.COMPASS;
        }
        if (n.contains("control") || n.contains("manipulation") || n.contains("ki ")) {
            return Material.LAPIS_LAZULI;
        }
        if (n.contains("defense") || n.contains("penetration")) {
            return Material.IRON_SWORD;
        }
        if (n.contains("healing")) {
            return Material.GHAST_TEAR;
        }
        if (n.contains("transmission") || n.contains("instant")) {
            return Material.ENDER_PEARL;
        }
        if (n.contains("infusion") || n.contains("boost") || n.contains("protection")) {
            return Material.DIAMOND;
        }
        if (n.contains("kaioken")) {
            return Material.REDSTONE;
        }
        if (n.contains("fusion")) {
            return Material.AMETHYST_SHARD;
        }
        return Material.ENCHANTED_BOOK;
    }

    private static int indexOfLevelSep(String line) {
        if (line == null) {
            return -1;
        }
        int a = line.indexOf("§7:");
        int b = line.indexOf("&7:");
        if (a < 0) {
            return b;
        }
        if (b < 0) {
            return a;
        }
        return Math.min(a, b);
    }

    private static String stripColor(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c == '§' || c == '&') && i + 1 < input.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static ItemStack named(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> colored = new ArrayList<>();
            colored.add("");
            for (String line : lore) {
                colored.add(color(line == null ? "" : line.replace('§', '&')));
            }
            meta.setLore(colored);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    static final class SkillPage {
        final List<String> header;
        final List<List<String>> skills;

        SkillPage(List<String> header, List<List<String>> skills) {
            this.header = header;
            this.skills = skills;
        }
    }
}
