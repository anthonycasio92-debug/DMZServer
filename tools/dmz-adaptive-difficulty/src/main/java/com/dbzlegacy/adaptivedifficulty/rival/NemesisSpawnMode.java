package com.dbzlegacy.adaptivedifficulty.rival;

import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import net.minecraft.server.level.ServerPlayer;

/**
 * Per-player policy for dmzultra nemesis spawns (the rival-journal mob that
 * visits and fights players on its own). Three states:
 *
 * <ul>
 *   <li>{@link #AUTO} — current dmzultra behavior: tick visits can spawn a nemesis
 *       and manual challenges spawn one (the default; nothing changes for players
 *       who never touch the toggle).
 *   <li>{@link #MANUAL} — only spawns the player starts (journal {@code challenge})
 *       come; the tick-driven auto-visits never spawn.
 *   <li>{@link #OFF} — no nemesis ever spawns; existing triggers are suppressed.
 * </ul>
 *
 * Persisted per player in entity custom data. The dmzultra side is gated by
 * {@code DmzultraNemesisSpawnGateMixin} (bytecode call graph: {@code Rival.meet}
 * is the spawn choke — {@code onTick} calls it with "tired"/"found", the manual
 * journal challenge calls it with "appear").
 */
public final class NemesisSpawnMode {

    /** Tick visits and manual challenges may spawn — dmzultra's shipped behavior. */
    public static final int AUTO = 0;
    /** Only player-started challenges may spawn; tick visits never do. */
    public static final int MANUAL = 1;
    /** No nemesis spawns at all. */
    public static final int OFF = 2;

    private static final String KEY = "lmNemesisSpawnMode";

    private NemesisSpawnMode() {}

    /** Stored policy, clamped to a known state. Missing key = {@link #AUTO}. */
    public static int mode(ServerPlayer player) {
        if (player == null) {
            return AUTO;
        }
        int stored = PersistentDataAccess.getInt(player, KEY, AUTO);
        return stored == MANUAL || stored == OFF ? stored : AUTO;
    }

    /** Manual journal challenges may spawn a nemesis (everything except {@link #OFF}). */
    public static boolean allowManual(ServerPlayer player) {
        return mode(player) != OFF;
    }

    /** Tick-driven auto-visits may spawn a nemesis (only {@link #AUTO}). */
    public static boolean allowTickSpawn(ServerPlayer player) {
        return mode(player) == AUTO;
    }

    /** Advance AUTO → MANUAL → OFF → AUTO, persist, and return menu-inline feedback. */
    public static String cycle(ServerPlayer player) {
        if (player == null) {
            return "§cPlayers only.";
        }
        int next = switch (mode(player)) {
            case AUTO -> MANUAL;
            case MANUAL -> OFF;
            default -> AUTO;
        };
        PersistentDataAccess.putInt(player, KEY, next);
        return switch (next) {
            case MANUAL -> "§eNemesis spawn - manual. Only challenges you start will bring one.";
            case OFF -> "§8Nemesis spawn - OFF. No nemesis fights will start for you.";
            default -> "§aNemesis spawn - ON. Your nemesis can come for you anytime.";
        };
    }

    /** Short state word for labels: {@code ON}, {@code manual}, or {@code OFF}. */
    public static String stateName(int mode) {
        return switch (mode) {
            case MANUAL -> "manual";
            case OFF -> "OFF";
            default -> "ON";
        };
    }
}
