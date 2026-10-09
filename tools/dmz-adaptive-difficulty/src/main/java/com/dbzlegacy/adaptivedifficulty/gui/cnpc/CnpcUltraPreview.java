package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff audit of the DMZUltra CNPC colors.
 * {@code /lm admin testgui} turns it on. {@code /lm} and the other menus turn it off.
 */
public final class CnpcUltraPreview {
    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<UUID> PAINTING = new ThreadLocal<>();

    private CnpcUltraPreview() {}

    public static void enter(ServerPlayer player) {
        if (player != null) {
            ACTIVE.add(player.m_20148_());
        }
    }

    public static void leave(ServerPlayer player) {
        if (player != null) {
            ACTIVE.remove(player.m_20148_());
        }
    }

    public static boolean active(ServerPlayer player) {
        return player != null && ACTIVE.contains(player.m_20148_());
    }

    /** Set while {@link CnpcGuiSupport#showSized} is painting this player. */
    static void bindPaint(ServerPlayer player) {
        if (player == null) {
            PAINTING.remove();
        } else {
            PAINTING.set(player.m_20148_());
        }
    }

    static void clearPaint() {
        PAINTING.remove();
    }

    /** True only while painting a player who opened the staff test GUI. */
    static boolean paintingUltra() {
        UUID id = PAINTING.get();
        return id != null && ACTIVE.contains(id);
    }
}
