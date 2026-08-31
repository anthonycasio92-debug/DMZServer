package com.dbzlegacy.adaptivedifficulty.bukkit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Bukkit-side Legacy Mechanics chat look — mirrors Forge {@code LmChat}
 * (Meditation-style {@code §5§l[System] §r…} tags).
 */
final class GuiChat {
    static final String DIVIDER = "§8────────────";

    private GuiChat() {}

    static String tagged(String system, String body) {
        String tag = system == null || system.isBlank() ? "Legacy Mechanics" : system.trim();
        String text = body == null ? "" : body;
        return "§5§l[" + tag + "] §r" + text;
    }

    static String normalize(String text) {
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

    /** Send to a player (multi-line OK). */
    static void send(Player player, String text) {
        if (player == null || text == null) {
            return;
        }
        String normalized = normalize(text);
        if (normalized.indexOf('\n') < 0) {
            player.sendMessage(normalized);
            return;
        }
        for (String line : normalized.split("\n", -1)) {
            player.sendMessage(line);
        }
    }

    /** Send to any command sender (console gets plain lines). */
    static void send(CommandSender sender, String text) {
        if (sender == null || text == null) {
            return;
        }
        if (sender instanceof Player player) {
            send(player, text);
            return;
        }
        String normalized = normalize(text);
        if (normalized.indexOf('\n') < 0) {
            sender.sendMessage(normalized);
            return;
        }
        for (String line : normalized.split("\n", -1)) {
            sender.sendMessage(line);
        }
    }

    /** Prefix uncolored Forge/plugin results with §a, then show in GUI or chat. */
    static void sendResult(Player player, String msg) {
        if (player == null || msg == null || msg.isBlank()) {
            return;
        }
        if (GuiFeedback.preferGui()) {
            GuiFeedback.setFromResult(player, msg);
            return;
        }
        sendChatResult(player, msg);
    }

    /**
     * Always deliver to chat (never swallow into GUI feedback).
     * Use for staff slash commands like {@code /prestige admin}.
     */
    static void sendChatResult(Player player, String msg) {
        if (player == null || msg == null || msg.isBlank()) {
            return;
        }
        for (String line : msg.split("\n")) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String colored = line.startsWith("§") ? line : "§a" + line;
            send(player, colored);
        }
    }

    private static String normalizeLine(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        if (line.startsWith("§5§l[") && line.contains("] §r")) {
            return line;
        }
        if (line.startsWith("§d§l")
                || line.startsWith(DIVIDER)
                || line.equals("§8────────────")
                || line.startsWith("§8Tip ·")
                || line.startsWith("§8Staff ·")
                || looksLikeHelpHeader(line)) {
            return line;
        }

        int i = 0;
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
        if (!tag.matches("[A-Za-z0-9][A-Za-z0-9 \\-]{0,38}")) {
            return line;
        }
        int bodyStart = close + 1;
        while (bodyStart < line.length() && line.charAt(bodyStart) == ' ') {
            bodyStart++;
        }
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
        int i = 0;
        while (i + 1 < line.length() && line.charAt(i) == '§') {
            i += 2;
        }
        return i < line.length() && line.charAt(i) == '/';
    }
}
