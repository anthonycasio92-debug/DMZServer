package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.server.level.ServerPlayer;

/** One-shot messages shown inside CNPC menus (instead of chat). */
public final class CnpcMenuFeedback {
    /** Gold bold header on every flash notice. Screens do not pick their own. */
    public static final String NOTICE_HEADER = "§6§lNotice";

    /**
     * Yellow body on every flash notice. Matches the gold header.
     * Positive or negative status belongs in the words, not a second color.
     */
    public static final String NOTICE_BODY = "§e";

    /** Color and format codes a caller may have put on a notice line. */
    private static final Pattern SECTION_CODE = Pattern.compile("§[0-9A-FK-ORa-fk-or]");

    private static final ConcurrentHashMap<UUID, List<String>> PENDING = new ConcurrentHashMap<>();

    private CnpcMenuFeedback() {}

    /**
     * One yellow notice line. Existing color and format codes are removed so a
     * screen cannot keep its own notice color.
     */
    public static String noticeBody(String line) {
        if (line == null || line.isBlank()) {
            return "";
        }
        String plain = SECTION_CODE.matcher(line).replaceAll("").trim();
        if (plain.isEmpty()) {
            return "";
        }
        return NOTICE_BODY + plain;
    }

    public static void set(ServerPlayer player, String message) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }
        List<String> lines = new ArrayList<>();
        for (String line : message.split("\n")) {
            if (line != null && !line.isBlank()) {
                String body = noticeBody(CnpcGuiSupport.safeChat(CnpcGuiStyle.normalizeLine(line)));
                if (!body.isBlank()) {
                    lines.add(body);
                }
            }
        }
        if (!lines.isEmpty()) {
            PENDING.put(player.m_20148_(), lines);
        }
    }

    /** True when a notice is waiting. Does not consume it. */
    public static boolean hasPending(ServerPlayer player) {
        return pendingLineCount(player) > 0;
    }

    /** Lines waiting for the next menu, capped at the inline notice band. Does not consume them. */
    public static int pendingLineCount(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        List<String> lines = PENDING.get(player.m_20148_());
        if (lines == null || lines.isEmpty()) {
            return 0;
        }
        return Math.min(CnpcGuiStyle.INFO_INLINE_MAX, lines.size());
    }

    /** Removes and returns pending lines for this player (shown once per menu open). */
    public static List<String> take(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }
        List<String> lines = PENDING.remove(player.m_20148_());
        return lines == null ? List.of() : lines;
    }
}
