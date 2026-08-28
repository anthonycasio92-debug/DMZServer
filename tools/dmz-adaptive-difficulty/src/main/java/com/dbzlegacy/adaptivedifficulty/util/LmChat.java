package com.dbzlegacy.adaptivedifficulty.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Legacy Mechanics chat look — matches Meditation:
 * {@code §5§l[System] §r…} tags, card titles, and {@code §8────────────} dividers.
 */
public final class LmChat {
    /** Horizontal rule used under card titles (Meditation Trial style). */
    public static final String DIVIDER = "§8────────────";

    private LmChat() {}

    /** {@code §5§l[system] §r} + body (Meditation tag style). */
    public static String tagged(String system, String body) {
        String tag = system == null || system.isBlank() ? "Legacy Mechanics" : system.trim();
        String text = body == null ? "" : body;
        return "§5§l[" + tag + "] §r" + text;
    }

    /** Bold purple card title (like {@code §d§lMeditation Trial}). */
    public static String title(String name) {
        String n = name == null || name.isBlank() ? "Legacy Mechanics" : name.trim();
        return "§d§l" + n;
    }

    /** Tip footer line: {@code §8Tip · §e/cmd §7rest}. */
    public static String tip(String command, String rest) {
        String cmd = command == null || command.isBlank() ? "/lm" : command.trim();
        String r = rest == null ? "" : rest;
        if (r.isBlank()) {
            return "§8Tip · §e" + cmd;
        }
        return "§8Tip · §e" + cmd + " §7" + r;
    }

    /**
     * Multi-line card: title, divider, body lines, optional tip.
     * Blank body lines are kept; nulls skipped.
     */
    public static String card(String titleName, String tipCommand, String tipRest, String... bodyLines) {
        StringBuilder sb = new StringBuilder();
        sb.append(title(titleName)).append('\n');
        sb.append(DIVIDER);
        if (bodyLines != null) {
            for (String line : bodyLines) {
                if (line == null) {
                    continue;
                }
                sb.append('\n').append(line);
            }
        }
        if (tipCommand != null && !tipCommand.isBlank()) {
            sb.append('\n').append(tip(tipCommand, tipRest));
        }
        return sb.toString();
    }

    /**
     * Rewrite leading {@code [Tag]} prefixes to Meditation style.
     * Leaves card titles, dividers, labeled rows, and help walls alone.
     */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (text.indexOf('\n') < 0) {
            return normalizeLine(text);
        }
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(normalizeLine(lines[i]));
        }
        return sb.toString();
    }

    /** Send one chat line (or multi-line) with Meditation-style tags applied. */
    public static void send(ServerPlayer player, String text) {
        if (player == null || text == null) {
            return;
        }
        String normalized = normalize(text);
        if (normalized.indexOf('\n') < 0) {
            sendRaw(player, normalized);
            return;
        }
        for (String line : normalized.split("\n", -1)) {
            sendRaw(player, line);
        }
    }

    private static void sendRaw(ServerPlayer player, String line) {
        try {
            player.m_213846_(Component.m_237113_(line == null ? "" : line));
        } catch (Throwable ignored) {
        }
    }

    private static String normalizeLine(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        // Already Meditation-style tag.
        if (line.startsWith("§5§l[") && line.contains("] §r")) {
            return line;
        }
        // Card chrome / help — do not wrap.
        if (line.startsWith("§d§l")
                || line.startsWith(DIVIDER)
                || line.equals("§8────────────")
                || line.startsWith("§8Tip ·")
                || line.startsWith("§8Staff ·")
                || looksLikeHelpHeader(line)) {
            return line;
        }

        int i = 0;
        // Skip leading formatting codes (§ + char).
        while (i + 1 < line.length() && line.charAt(i) == '§') {
            i += 2;
        }
        if (i >= line.length() || line.charAt(i) != '[') {
            return line;
        }
        int close = line.indexOf(']', i + 1);
        if (close < 0) {
            return line;
        }
        String tag = line.substring(i + 1, close).trim();
        if (tag.isEmpty() || tag.length() > 40) {
            return line;
        }
        // Avoid rewriting inspect prefixes like [PlayerName] that are not systems —
        // system tags are short words / phrases without spaces mostly, but "Potential Unlock"
        // and "Proving Grounds" have spaces. Allow letters/digits/spaces/-.
        if (!tag.matches("[A-Za-z0-9][A-Za-z0-9 \\-]{0,38}")) {
            return line;
        }
        int bodyStart = close + 1;
        while (bodyStart < line.length() && line.charAt(bodyStart) == ' ') {
            bodyStart++;
        }
        // Drop a leading §r after the tag if present.
        if (bodyStart + 1 < line.length()
                && line.charAt(bodyStart) == '§'
                && line.charAt(bodyStart + 1) == 'r') {
            bodyStart += 2;
            while (bodyStart < line.length() && line.charAt(bodyStart) == ' ') {
                bodyStart++;
            }
        }
        String body = bodyStart < line.length() ? line.substring(bodyStart) : "";
        return tagged(tag, body);
    }

    private static boolean looksLikeHelpHeader(String line) {
        // §6§l/lm admin  or  §e/rival …
        int i = 0;
        while (i + 1 < line.length() && line.charAt(i) == '§') {
            i += 2;
        }
        return i < line.length() && line.charAt(i) == '/';
    }
}
