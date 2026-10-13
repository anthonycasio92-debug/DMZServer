package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.rival.RivalPlayerRecord;
import com.dbzlegacy.adaptivedifficulty.rival.RivalStore;
import com.dbzlegacy.adaptivedifficulty.rival.RivalUuid;
import com.dbzlegacy.adaptivedifficulty.sparring.DojoRankings;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player-facing labels for picker args and internal tokens (no raw UUIDs / snake_case on buttons).
 */
public final class UltraText {
    private static final Pattern PACKAGE_LIKE =
            Pattern.compile("(?:\\b[a-z]{2,}\\.){2,}[A-Za-z0-9_$]+");
    private static final Pattern UUID_LINE = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE);

    private UltraText() {}

    /** Player-facing label for pick lists ({@code uuid:…} → stored name). */
    public static String humanizePickerArg(String arg) {
        if (arg == null || arg.isBlank()) {
            return UltraStyle.DIM + "(entry)";
        }
        String s = arg.trim();
        if (s.startsWith("uuid:")) {
            return humanizeUuid(s.substring(5).trim());
        }
        if (UUID_LINE.matcher(s).matches()) {
            return humanizeUuid(s);
        }
        if (s.contains("\t")) {
            String[] p = s.split("\t", -1);
            if (p.length > 1 && p[1] != null && !p[1].isBlank()) {
                return safeChat(p[1]);
            }
            s = p[0];
        }
        if (looksLikeInternalId(s)) {
            return humanizeToken(s);
        }
        return safeChat(s);
    }

    public static String humanizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return UltraStyle.DIM + "?";
        }
        String s = raw.trim();
        if (s.startsWith("uuid:")) {
            return humanizeUuid(s.substring(5).trim());
        }
        int slash = s.lastIndexOf('/');
        if (slash >= 0 && slash < s.length() - 1) {
            s = s.substring(slash + 1);
        }
        int dot = s.lastIndexOf('.');
        if (dot >= 0 && dot < s.length() - 1 && s.contains(".")) {
            s = s.substring(dot + 1);
        }
        s = s.replace('_', ' ').replace('-', ' ');
        if (s.length() > 1 && s.chars().noneMatch(Character::isWhitespace)) {
            s = splitCamel(s);
        }
        if (s.isBlank()) {
            return UltraStyle.DIM + "?";
        }
        return UltraStyle.LABEL + capitalizeWords(s.toLowerCase(Locale.ROOT));
    }

    private static String humanizeUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return UltraStyle.LABEL + "(player)";
        }
        String u = uuid.trim();
        if (u.regionMatches(true, 0, "uuid:", 0, 5)) {
            u = u.substring(5).trim();
        }
        String resolved = resolveStoredPlayerName(u);
        if (resolved != null && !resolved.isBlank()) {
            return UltraStyle.LABEL + safeChat(resolved);
        }
        if (u.length() > 8) {
            return UltraStyle.LABEL + "Player " + UltraStyle.DIM + u.substring(0, 8) + "…";
        }
        return UltraStyle.LABEL + "Player " + UltraStyle.DIM + u;
    }

    private static String resolveStoredPlayerName(String uuidRaw) {
        if (uuidRaw == null || uuidRaw.isBlank()) {
            return null;
        }
        try {
            UUID id = UUID.fromString(uuidRaw);
            String key = RivalUuid.canonical(id.toString());
            RivalPlayerRecord rec = key == null ? null : RivalStore.get().players.get(key);
            if (rec != null && rec.name != null && !rec.name.isBlank()) {
                return rec.name;
            }
            MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer online = server.m_6846_().m_11259_(id);
                if (online != null) {
                    return online.m_7755_().getString();
                }
            }
            String dojo = DojoRankings.dojoDisplayName(id.toString().toLowerCase(Locale.ROOT));
            if (dojo != null && !dojo.isBlank() && !"?".equals(dojo) && !"Dojo".equalsIgnoreCase(dojo)) {
                return dojo;
            }
        } catch (IllegalArgumentException ignored) {
        }
        for (RivalStore.DeclareRequest dr : RivalStore.get().declareRequests.values()) {
            if (dr == null) {
                continue;
            }
            if (uuidRaw.equalsIgnoreCase(dr.fromUuid) && dr.fromName != null && !dr.fromName.isBlank()) {
                return dr.fromName;
            }
            if (uuidRaw.equalsIgnoreCase(dr.toUuid) && dr.toName != null && !dr.toName.isBlank()) {
                return dr.toName;
            }
        }
        for (RivalStore.StoredChallengeRequest cr : RivalStore.get().challengeRequests.values()) {
            if (cr == null) {
                continue;
            }
            if (uuidRaw.equalsIgnoreCase(cr.from) && cr.fromName != null && !cr.fromName.isBlank()) {
                return cr.fromName;
            }
            if (uuidRaw.equalsIgnoreCase(cr.to) && cr.toName != null && !cr.toName.isBlank()) {
                return cr.toName;
            }
        }
        return null;
    }

    private static String splitCamel(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(s.charAt(i - 1))) {
                out.append(' ');
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String capitalizeWords(String s) {
        String[] parts = s.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                out.append(p.substring(1));
            }
        }
        return out.toString();
    }

    private static boolean looksLikeInternalId(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        if (s.contains(":") && !s.startsWith("uuid:")) {
            return true;
        }
        if (PACKAGE_LIKE.matcher(s).find()) {
            return true;
        }
        return s.length() > 24 && !s.contains(" ");
    }

    /** Strip characters that break label parsing; keep color codes. */
    public static String safeChat(String text) {
        if (text == null) {
            return "";
        }
        String s = text.replace('\n', ' ').replace('\r', ' ');
        return s.replace("{", "").replace("}", "");
    }
}
