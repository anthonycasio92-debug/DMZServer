package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.Locale;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Bukkit-side Legacy Mechanics chat look — mirrors Forge {@code LmChat}
 * (Meditation-style {@code §5§l[System] §r…} tags).
 */
final class GuiChat {
    static final String DIVIDER = "§8────────────";

    private static final Map<String, String> TAG_ALIASES = Map.ofEntries(
            Map.entry("sparring", "Spar"),
            Map.entry("mentor bond", "Mentor"),
            Map.entry("mentor", "Mentor"),
            Map.entry("rival challenge", "Challenge"),
            Map.entry("rival instinct", "Instinct"),
            Map.entry("rival quest", "Rival"),
            Map.entry("proving grounds", "Proving"),
            Map.entry("potential unlock", "Potential"),
            Map.entry("potential", "Potential"),
            Map.entry("race lock", "Race"),
            Map.entry("shadow dummy", "Dummy"),
            Map.entry("global tp", "TP"),
            Map.entry("tp", "TP"),
            Map.entry("legacy mechanics", "LM"),
            Map.entry("stats", "Stats"),
            Map.entry("spiritualist", "Spirit"),
            Map.entry("android", "Android"),
            Map.entry("absorb", "Absorb"),
            Map.entry("flight", "Flight"),
            Map.entry("meditation", "Meditation"),
            Map.entry("prestige", "Prestige"),
            Map.entry("difficulty", "Difficulty"),
            Map.entry("end", "End"),
            Map.entry("spec", "Spec")
    );

    private GuiChat() {}

    static String systemName(String system) {
        if (system == null || system.isBlank()) {
            return "LM";
        }
        String raw = system.trim();
        String alias = TAG_ALIASES.get(raw.toLowerCase(Locale.ROOT));
        return alias != null ? alias : raw;
    }

    static String tagged(String system, String body) {
        String text = body == null ? "" : body;
        return "§5§l[" + systemName(system) + "] §r" + text;
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
            return realiasCanonical(line);
        }
        if (line.startsWith("§d§l")
                || line.startsWith(DIVIDER)
                || line.equals("§8────────────")
                || line.startsWith("§8Tip ·")
                || line.startsWith("§8Staff ·")
                || looksLikeHelpHeader(line)) {
            return line;
        }
        String openTip = normalizeOpenTip(line);
        if (openTip != null) {
            return openTip;
        }
        if (line.matches("§8-{6,}")) {
            return DIVIDER;
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

    private static String realiasCanonical(String line) {
        if (!line.startsWith("§5§l[")) {
            return line;
        }
        int close = line.indexOf(']', 5);
        if (close < 0) {
            return line;
        }
        String tag = line.substring(5, close).trim();
        String canonical = systemName(tag);
        if (canonical.equals(tag)) {
            return line;
        }
        String body = "";
        if (close + 4 <= line.length() && line.startsWith("] §r", close)) {
            body = line.substring(close + 4);
        } else if (close + 1 < line.length()) {
            body = line.substring(close + 1).trim();
        }
        return tagged(canonical, body);
    }

    private static String normalizeOpenTip(String line) {
        if (line == null || !line.contains("Open") || !line.contains("/")) {
            return null;
        }
        int i = 0;
        while (i + 1 < line.length() && line.charAt(i) == '§') {
            i += 2;
        }
        if (i >= line.length() || !line.regionMatches(i, "Open", 0, 4)) {
            return null;
        }
        int slash = line.indexOf('/', i);
        if (slash < 0) {
            return null;
        }
        int endCmd = slash + 1;
        while (endCmd < line.length()) {
            char c = line.charAt(endCmd);
            if (c == ' ' || c == '§' || c == '→') {
                break;
            }
            endCmd++;
        }
        String cmd = line.substring(slash, endCmd).trim();
        if (cmd.length() < 2) {
            return null;
        }
        String rest = line.substring(endCmd).trim();
        while (!rest.isEmpty()) {
            if (rest.startsWith("§") && rest.length() >= 2) {
                rest = rest.substring(2).trim();
                continue;
            }
            if (rest.startsWith("→") || rest.startsWith("->")) {
                rest = rest.replaceFirst("^(→|->)\\s*", "").trim();
                continue;
            }
            if (rest.regionMatches(true, 0, "to ", 0, 3)) {
                rest = rest.substring(3).trim();
                continue;
            }
            break;
        }
        return tip(cmd, rest.isBlank() ? null : rest);
    }

    private static String tip(String command, String rest) {
        String cmd = command == null || command.isBlank() ? "/lm" : command.trim();
        String r = rest == null ? "" : rest;
        if (r.isBlank()) {
            return "§8Tip · §e" + cmd;
        }
        return "§8Tip · §e" + cmd + " §7" + r;
    }

    private static boolean looksLikeHelpHeader(String line) {
        int i = 0;
        while (i + 1 < line.length() && line.charAt(i) == '§') {
            i += 2;
        }
        return i < line.length() && line.charAt(i) == '/';
    }
}
