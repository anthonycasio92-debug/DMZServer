package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Pending action results for the next Ultra {@link UltraScreenSpec#send}. Mirrors Bukkit
 * {@code GuiFeedback}: menu actions stash a short outcome here instead of chatting, and the
 * reopened (or handler-opened) screen paints it as Notice / danger lines.
 *
 * <p>Chat backend ({@code guiBackend=chat}) still prefers chat; Ultra is the default.
 */
public final class UltraGuiFeedback {
    private static final long TTL_MS = 45_000L;
    private static final int MAX_LINES = 6;

    private static final Map<UUID, Entry> PENDING = new ConcurrentHashMap<>();

    private UltraGuiFeedback() {}

    record Entry(boolean ok, List<String> linesPlain, long expiresAt) {}

    /** True when Ultra/inventory GUIs should show results instead of chat. */
    public static boolean preferGui() {
        try {
            String backend = DifficultyConfig.get().guiBackend;
            if (backend == null || backend.isBlank()) {
                return true;
            }
            return !"chat".equalsIgnoreCase(backend.trim());
        } catch (Throwable ignored) {
            return true;
        }
    }

    /**
     * Stash a handler return string for the next Ultra open. Blank / divider-only messages are
     * ignored. Lines with danger red mark the entry as a failure (danger ink).
     */
    public static void setFromResult(ServerPlayer player, String msg) {
        if (player == null || msg == null || msg.isBlank()) {
            return;
        }
        List<String> lines = new ArrayList<>();
        boolean ok = true;
        for (String raw : msg.split("\n")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String plain = UltraStyle.plain(raw).trim();
            if (plain.isEmpty()
                    || plain.startsWith("---")
                    || plain.chars().allMatch(ch -> ch == '-' || Character.isWhitespace(ch))) {
                continue;
            }
            // Drop LmChat system tags if a handler still returned a tagged line.
            plain = stripSystemTag(plain);
            if (plain.isEmpty()) {
                continue;
            }
            // Heuristic: Ultra danger ink or common failure openers.
            String plainLower = plain.toLowerCase(Locale.ROOT);
            if (raw.contains(UltraStyle.DANGER)
                    || plainLower.startsWith("not ")
                    || plainLower.startsWith("could not")
                    || plainLower.startsWith("you do not")
                    || plainLower.contains("failed")
                    || plainLower.contains("cannot")
                    || plainLower.contains("can't ")
                    || plainLower.contains("need ")
                    || plainLower.contains("missing")) {
                ok = false;
            }
            lines.add(plain);
            if (lines.size() >= MAX_LINES) {
                break;
            }
        }
        if (lines.isEmpty()) {
            return;
        }
        PENDING.put(
                player.m_20148_(),
                new Entry(ok, List.copyOf(lines), System.currentTimeMillis() + TTL_MS));
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            PENDING.remove(player.m_20148_());
        }
    }

    /** Consume pending feedback and prepend styled lines onto {@code spec}. */
    public static void applyPending(UltraScreenSpec spec, ServerPlayer viewer) {
        if (spec == null) {
            return;
        }
        Entry entry = consume(viewer);
        if (entry == null || entry.linesPlain().isEmpty()) {
            return;
        }
        List<String> painted = new ArrayList<>(entry.linesPlain().size());
        for (String plain : entry.linesPlain()) {
            if (entry.ok()) {
                painted.add(UltraStyle.notice(plain));
            } else {
                painted.add(UltraStyle.danger(plain));
            }
        }
        spec.prependBodyLines(painted);
    }

    static Entry consume(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        Entry e = PENDING.remove(player.m_20148_());
        if (e == null) {
            return null;
        }
        if (e.expiresAt() < System.currentTimeMillis()) {
            return null;
        }
        return e;
    }

    /** Deliver menu-action feedback: GUI stash when preferred, else chat. */
    public static void deliver(ServerPlayer player, String msg) {
        if (player == null || msg == null || msg.isBlank()) {
            return;
        }
        if (preferGui()) {
            setFromResult(player, msg);
            return;
        }
        com.dbzlegacy.adaptivedifficulty.util.DmzRewards.msg(
                player, com.dbzlegacy.adaptivedifficulty.gui.GuiNotice.noticeBody(msg));
    }

    private static String stripSystemTag(String plain) {
        if (plain == null) {
            return "";
        }
        String s = plain.trim();
        if (s.startsWith("[") && s.contains("]")) {
            int close = s.indexOf(']');
            if (close >= 0 && close + 1 < s.length()) {
                s = s.substring(close + 1).trim();
            }
        }
        return s;
    }
}
