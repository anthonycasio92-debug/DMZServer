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

    /** Staff see full tip lines; players get no instructional lore. */
    static List<String> tips(Player player, String... staffLines) {
        if (player != null && ForgeBridge.isStaff(player)) {
            List<String> out = new ArrayList<>();
            for (String line : staffLines) {
                if (line != null) {
                    out.add(line);
                }
            }
            return out;
        }
        return List.of();
    }

    static List<String> tipsList(Player player, List<String> staffLines) {
        if (player != null && ForgeBridge.isStaff(player) && staffLines != null) {
            return new ArrayList<>(staffLines);
        }
        return List.of();
    }

    /**
     * Pending invite rows: each invite uses 3 interior slots — head, Accept, Decline.
     * Packed left-to-right across rows 10–16, 19–25, 28–34 (max 7 invites).
     * Outgoing invites still reserve the Accept/Decline slots (caller leaves them empty).
     */
    static int[][] pendingInviteActionSlots(int count) {
        int n = Math.max(0, Math.min(count, (ROW_STARTS.length * ROW_WIDTH) / 3));
        int[] flat = new int[ROW_STARTS.length * ROW_WIDTH];
        int fi = 0;
        for (int start : ROW_STARTS) {
            for (int c = 0; c < ROW_WIDTH; c++) {
                flat[fi++] = start + c;
            }
        }
        int[][] rows = new int[n][3];
        for (int i = 0; i < n; i++) {
            rows[i][0] = flat[i * 3];
            rows[i][1] = flat[i * 3 + 1];
            rows[i][2] = flat[i * 3 + 2];
        }
        return rows;
    }

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

    /**
     * Mutual rival heads on Rival Teams — top interior row (10–16) so heads do not cover
     * mode buttons on row 19–25 (slots 20/22/24).
     */
    static int[] teamMutualRivalSlots(int count) {
        int n = Math.max(0, count);
        if (n == 0) {
            return new int[0];
        }
        if (n <= ROW_WIDTH) {
            int start = 10 + (ROW_WIDTH - n) / 2;
            int[] out = new int[n];
            for (int i = 0; i < n; i++) {
                out[i] = start + i;
            }
            return out;
        }
        int[] pool = {
                10, 11, 12, 13, 14, 15, 16,
                19, 21, 23, 25, 26,
                28, 29, 30, 32, 33, 34
        };
        n = Math.min(n, pool.length);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = pool[i];
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
     * Encoded rival card from Forge:
     * uuid, name, status, tier, rp, wins, losses, draws, deathLosses, deathWins,
     * online, past, presenceMs, lastBattleAt (tab-separated).
     */
    static final class RivalCard {
        final String uuid;
        final String name;
        final String status;
        final String tier;
        final int rp;
        final int wins;
        final int losses;
        final int draws;
        final int deathLosses;
        final int deathWins;
        final boolean online;
        final boolean past;
        final long presenceMs;
        final long lastBattleAt;

        RivalCard(
                String uuid, String name, String status, String tier,
                int rp, int wins, int losses, int draws,
                int deathLosses, int deathWins, boolean online, boolean past,
                long presenceMs, long lastBattleAt
        ) {
            this.uuid = uuid == null ? "" : uuid;
            this.name = name == null || name.isBlank() ? "?" : name;
            this.status = status == null || status.isBlank() ? "?" : status;
            this.tier = tier == null || tier.isBlank() ? "?" : tier;
            this.rp = rp;
            this.wins = wins;
            this.losses = losses;
            this.draws = draws;
            this.deathLosses = deathLosses;
            this.deathWins = deathWins;
            this.online = online;
            this.past = past;
            this.presenceMs = Math.max(0L, presenceMs);
            this.lastBattleAt = Math.max(0L, lastBattleAt);
        }

        String pickerArg() {
            if (!uuid.isBlank()) {
                return "uuid:" + uuid;
            }
            return name;
        }
    }

    static List<RivalCard> parseRivalCards(List<String> encoded) {
        List<RivalCard> out = new ArrayList<>();
        if (encoded == null) {
            return out;
        }
        for (String raw : encoded) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String[] p = raw.split("\t", -1);
            if (p.length < 6) {
                continue;
            }
            out.add(new RivalCard(
                    p[0],
                    p[1],
                    p.length > 2 ? p[2] : "?",
                    p.length > 3 ? p[3] : "?",
                    parseIntSafe(p.length > 4 ? p[4] : "0"),
                    parseIntSafe(p.length > 5 ? p[5] : "0"),
                    parseIntSafe(p.length > 6 ? p[6] : "0"),
                    parseIntSafe(p.length > 7 ? p[7] : "0"),
                    parseIntSafe(p.length > 8 ? p[8] : "0"),
                    parseIntSafe(p.length > 9 ? p[9] : "0"),
                    "1".equals(p.length > 10 ? p[10] : "0"),
                    "1".equals(p.length > 11 ? p[11] : "0"),
                    parseLongSafe(p.length > 12 ? p[12] : "0"),
                    parseLongSafe(p.length > 13 ? p[13] : "0")
            ));
        }
        return out;
    }

    /** uuid, name, status, online, optedIn, near, spare (tab-separated). */
    static final class TeamRivalCard {
        final String uuid;
        final String name;
        final String status;
        final boolean online;
        final boolean optedIn;
        final boolean near;
        final long spare;

        TeamRivalCard(String uuid, String name, String status,
                boolean online, boolean optedIn, boolean near, long spare) {
            this.uuid = uuid == null ? "" : uuid;
            this.name = name == null || name.isBlank() ? "?" : name;
            this.status = status == null || status.isBlank() ? "?" : status;
            this.online = online;
            this.optedIn = optedIn;
            this.near = near;
            this.spare = Math.max(0L, spare);
        }
    }

    static List<TeamRivalCard> parseTeamRivalCards(List<String> encoded) {
        List<TeamRivalCard> out = new ArrayList<>();
        if (encoded == null) {
            return out;
        }
        for (String raw : encoded) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String[] p = raw.split("\t", -1);
            if (p.length < 3) {
                continue;
            }
            out.add(new TeamRivalCard(
                    p[0],
                    p[1],
                    p.length > 2 ? p[2] : "?",
                    "1".equals(p.length > 3 ? p[3] : "0"),
                    "1".equals(p.length > 4 ? p[4] : "0"),
                    "1".equals(p.length > 5 ? p[5] : "0"),
                    parseLongSafe(p.length > 6 ? p[6] : "0")
            ));
        }
        return out;
    }

    static ItemStack teamRivalHead(TeamRivalCard card) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Status &f" + card.status);
        lore.add(card.online ? "&aOnline" : "&8Offline");
        lore.add(card.optedIn ? "&aUsing team mode" : "&8Personal only");
        if (card.online && card.optedIn) {
            lore.add(card.near ? "&aNearby — shares spare room" : "&8Too far to share spare room");
            if (card.spare > 0) {
                lore.add("&7Spare tier room &f" + card.spare);
            }
        }
        java.util.UUID id = null;
        try {
            if (!card.uuid.isBlank()) {
                id = java.util.UUID.fromString(card.uuid);
            }
        } catch (IllegalArgumentException ignored) {
        }
        if (id != null) {
            return GuiPlayerPicker.headByUuid(id, card.name, "&f" + card.name, lore);
        }
        return GuiPlayerPicker.headByName(card.name, "&f" + card.name, lore);
    }

    static ItemStack rivalHead(RivalCard card) {
        List<String> lore = new ArrayList<>();
        if (card.past) {
            lore.add("&8Previous rivalry");
        } else {
            lore.add("&7Status &f" + card.status);
        }
        lore.add("&7Tier &f" + card.tier + " &8· &7RP &f" + card.rp);
        lore.add("&7Record &a" + card.wins + "&7/&c" + card.losses + "&7/&e" + card.draws);
        if (card.deathWins > 0 || card.deathLosses > 0) {
            lore.add("&7Deaths &a" + card.deathWins + "W &c" + card.deathLosses + "L");
        }
        if (card.presenceMs > 0L) {
            lore.add("&7Presence &f" + formatDuration(card.presenceMs));
        }
        if (card.lastBattleAt > 0L) {
            long ago = System.currentTimeMillis() - card.lastBattleAt;
            if (ago >= 0L) {
                lore.add("&7Last battle &f" + formatDuration(ago) + " &7ago");
            }
        }
        lore.add("");
        lore.add(card.online ? "&aOnline" : "&8Offline");
        String title = (card.past ? "&8" : "&f") + card.name;
        if (!card.past) {
            title = "&f" + card.name;
        }
        if (!card.uuid.isBlank()) {
            try {
                java.util.UUID id = java.util.UUID.fromString(card.uuid);
                Player online = org.bukkit.Bukkit.getPlayer(id);
                if (online != null) {
                    return GuiPlayerPicker.head(online, title, lore);
                }
                return GuiPlayerPicker.headByUuid(id, card.name, title, lore);
            } catch (IllegalArgumentException ignored) {
                // fall through to name
            }
        }
        Player online = org.bukkit.Bukkit.getPlayerExact(card.name);
        if (online != null) {
            return GuiPlayerPicker.head(online, title, lore);
        }
        return GuiPlayerPicker.headByName(card.name, title, lore);
    }

    /**
     * Encoded pending invite: uuid, name, direction(IN|OUT), expiresAtMs, online(0/1)[, kind].
     * Kind is optional — rival declares omit it; mentor invites use {@code mentor}/{@code apprentice}.
     */
    static final class PendingInvite {
        final String uuid;
        final String name;
        final boolean incoming;
        final long expiresAt;
        final boolean online;
        final String kind;

        PendingInvite(String uuid, String name, boolean incoming, long expiresAt, boolean online) {
            this(uuid, name, incoming, expiresAt, online, "");
        }

        PendingInvite(String uuid, String name, boolean incoming, long expiresAt, boolean online, String kind) {
            this.uuid = uuid == null ? "" : uuid;
            this.name = name == null || name.isBlank() ? "?" : name;
            this.incoming = incoming;
            this.expiresAt = Math.max(0L, expiresAt);
            this.online = online;
            this.kind = kind == null ? "" : kind.trim();
        }

        boolean isMentorBond() {
            return "mentor".equalsIgnoreCase(kind) || "apprentice".equalsIgnoreCase(kind);
        }

        boolean isMutualConfirm() {
            return "mutual".equalsIgnoreCase(kind);
        }

        String pickerArg() {
            if (!uuid.isBlank()) {
                return "uuid:" + uuid;
            }
            return name;
        }
    }

    static List<PendingInvite> parsePendingInvites(List<String> encoded) {
        List<PendingInvite> out = new ArrayList<>();
        if (encoded == null) {
            return out;
        }
        for (String raw : encoded) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String[] p = raw.split("\t", -1);
            if (p.length < 3) {
                continue;
            }
            out.add(new PendingInvite(
                    p[0],
                    p[1],
                    "IN".equalsIgnoreCase(p[2]),
                    parseLongSafe(p.length > 3 ? p[3] : "0"),
                    "1".equals(p.length > 4 ? p[4] : "0"),
                    p.length > 5 ? p[5] : ""
            ));
        }
        return out;
    }

    static ItemStack pendingInviteHead(Player player, PendingInvite inv) {
        List<String> lore = new ArrayList<>();
        if (inv.isMentorBond()) {
            String role = "mentor".equalsIgnoreCase(inv.kind) ? "Mentor" : "Apprentice";
            if (inv.incoming) {
                lore.add("&aIncoming mentor invite");
                lore.add("&7They want you as their &f" + role);
                lore.addAll(tips(player, "&eClick to Accept / Decline"));
            } else {
                lore.add("&6Outgoing mentor invite");
                lore.add("&7Waiting — they would be your &f" + role);
                lore.addAll(tips(player, "&eClick to Cancel · or wait for them"));
            }
        } else if (inv.isMutualConfirm() && inv.incoming) {
            lore.add("&eDeclared — Mutual confirm");
            lore.add("&7You both Silent'd each other");
            lore.add("&7Both must Accept for Mutual");
            lore.add("&eClick to Accept or Decline");
        } else if (inv.incoming) {
            lore.add("&aIncoming declare");
            lore.add("&7They Declared you");
            lore.add("&eClick to Accept or Decline");
        } else {
            lore.add("&6Outgoing declare");
            lore.add("&7On your list as Declared");
            lore.add("&7Waiting for them to Accept");
            lore.addAll(tips(player, "&8You cannot Accept/Decline your own declare"));
        }
        if (inv.expiresAt > 0L) {
            long left = inv.expiresAt - System.currentTimeMillis();
            if (left > 0L) {
                lore.add("&7Expires in &f" + formatDuration(left));
            }
        }
        lore.add("");
        lore.add(inv.online ? "&aOnline" : "&8Offline");
        String title = (inv.incoming ? "&a◀ " : "&6▶ ") + "&f" + inv.name;
        if (!inv.uuid.isBlank()) {
            try {
                java.util.UUID id = java.util.UUID.fromString(inv.uuid);
                Player online = org.bukkit.Bukkit.getPlayer(id);
                if (online != null) {
                    return GuiPlayerPicker.head(online, title, lore);
                }
                return GuiPlayerPicker.headByUuid(id, inv.name, title, lore);
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        Player online = org.bukkit.Bukkit.getPlayerExact(inv.name);
        if (online != null) {
            return GuiPlayerPicker.head(online, title, lore);
        }
        return GuiPlayerPicker.headByName(inv.name, title, lore);
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s == null ? "0" : s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseLongSafe(String s) {
        try {
            return Long.parseLong(s == null ? "0" : s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String formatDuration(long ms) {
        long sec = Math.max(0L, ms / 1000L);
        long days = sec / 86400L;
        long hours = (sec % 86400L) / 3600L;
        long mins = (sec % 3600L) / 60L;
        if (days > 0L) {
            return days + "d " + hours + "h";
        }
        if (hours > 0L) {
            return hours + "h " + mins + "m";
        }
        if (mins > 0L) {
            return mins + "m";
        }
        return sec + "s";
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
        if (n.contains("spar report") || n.contains("report #") || n.matches(".*#\\d+.*vs.*")) {
            return Material.WRITTEN_BOOK;
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
