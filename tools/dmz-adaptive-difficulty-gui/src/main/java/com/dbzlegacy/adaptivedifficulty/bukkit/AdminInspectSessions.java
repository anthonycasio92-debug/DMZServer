package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Tracks staff "inspect" sessions: viewer UUID → subject UUID.
 * The chest GUI opens for the viewer but paints/actions use the subject.
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

    /** Online subject for this viewer, or the viewer themselves when not inspecting. */
    public static Player resolveSubject(Player viewer) {
        if (viewer == null) {
            return null;
        }
        UUID subjectId = VIEWER_TO_SUBJECT.get(viewer.getUniqueId());
        if (subjectId == null || subjectId.equals(viewer.getUniqueId())) {
            return viewer;
        }
        Player subject = Bukkit.getPlayer(subjectId);
        if (subject == null || !subject.isOnline()) {
            VIEWER_TO_SUBJECT.remove(viewer.getUniqueId());
            return viewer;
        }
        return subject;
    }
}
