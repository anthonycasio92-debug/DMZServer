package com.dbzlegacy.adaptivedifficulty.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff inspect: viewer (admin) UUID → subject UUID. CNPC panels paint and apply actions on the
 * subject while the admin keeps the GUI open.
 */
public final class AdminInspectSessions {
    private static final Map<UUID, UUID> VIEWER_TO_SUBJECT = new ConcurrentHashMap<>();

    private AdminInspectSessions() {}

    public static void set(UUID viewerId, UUID subjectId) {
        if (viewerId == null || subjectId == null) {
            return;
        }
        if (viewerId.equals(subjectId)) {
            VIEWER_TO_SUBJECT.remove(viewerId);
            return;
        }
        VIEWER_TO_SUBJECT.put(viewerId, subjectId);
    }

    public static void clear(UUID viewerId) {
        if (viewerId != null) {
            VIEWER_TO_SUBJECT.remove(viewerId);
        }
    }

    public static void clearAllInvolving(UUID playerId) {
        if (playerId == null) {
            return;
        }
        VIEWER_TO_SUBJECT.remove(playerId);
        VIEWER_TO_SUBJECT.entrySet().removeIf(e -> playerId.equals(e.getValue()));
    }

    public static UUID subjectId(UUID viewerId) {
        return viewerId == null ? null : VIEWER_TO_SUBJECT.get(viewerId);
    }

    public static boolean isInspecting(UUID viewerId) {
        UUID subject = subjectId(viewerId);
        return subject != null && !subject.equals(viewerId);
    }

    /** Subject for GUI data/actions, or {@code viewer} when not inspecting. */
    public static ServerPlayer resolveSubject(ServerPlayer viewer) {
        if (viewer == null) {
            return null;
        }
        UUID sid = VIEWER_TO_SUBJECT.get(viewer.m_20148_());
        if (sid == null || sid.equals(viewer.m_20148_())) {
            return viewer;
        }
        MinecraftServer server = viewer.m_20194_();
        if (server == null) {
            VIEWER_TO_SUBJECT.remove(viewer.m_20148_());
            return viewer;
        }
        ServerPlayer subject = server.m_6846_().m_11259_(sid);
        if (subject == null) {
            VIEWER_TO_SUBJECT.remove(viewer.m_20148_());
            return viewer;
        }
        return subject;
    }

    public static String inspectBannerLine(ServerPlayer viewer) {
        ServerPlayer subject = resolveSubject(viewer);
        if (subject == null || subject.m_20148_().equals(viewer.m_20148_())) {
            return null;
        }
        return "§eInspecting §f" + subject.m_7755_().getString() + "§e — edits apply to them";
    }
}
