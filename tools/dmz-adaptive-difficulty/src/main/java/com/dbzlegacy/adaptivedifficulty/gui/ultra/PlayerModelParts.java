package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.character.CosmeticHeadBoneService;
import com.dbzlegacy.adaptivedifficulty.character.HeadPartPieces;
import com.dbzlegacy.adaptivedifficulty.gui.AdminInspectSessions;
import com.dragonminez.common.stats.character.Character;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/**
 * Builds the optional {@code layoutJson.playerModel.parts} snapshot for KaiPath / JLDK.
 *
 * <p>Schema {@code 1} - see {@code docs/KAIPATH_GUI_PROTOCOL.md}. {@code subject} is {@code local}
 * for the viewer, or {@code uuid:<id>} for an inspect subject. The client uses that plus this
 * object for custom body pieces (DMZ head bones, worn form identity, and an open {@code extras} array).
 */
public final class PlayerModelParts {
    /** Current documented schema version inside {@code parts.schema}. */
    public static final int SCHEMA = 1;

    private PlayerModelParts() {}

    /**
     * Snapshot equipped cosmetics for the model render target. Returns {@code null} when the
     * target is the viewer and nothing useful can be serialized (caller omits {@code parts}).
     * While staff inspect is active the target is {@link AdminInspectSessions#resolveSubject},
     * and {@code parts} is always sent so the client does not fall back to the local player.
     */
    public static JsonObject snapshot(ServerPlayer player) {
        ServerPlayer target = renderTarget(player);
        if (target == null) {
            return null;
        }
        boolean other = previewsOtherPlayer(target);
        JsonObject parts = new JsonObject();
        parts.addProperty("schema", SCHEMA);
        parts.addProperty("subject", other ? "uuid:" + target.m_20148_() : "local");

        boolean any = false;
        JsonObject head = headBones(target);
        if (head != null) {
            parts.add("headBones", head);
            any = true;
        }
        JsonObject form = formIdentity(target);
        if (form != null) {
            parts.add("form", form);
            any = true;
        }
        // Open extension slot - always present so clients can iterate without null checks.
        parts.add("extras", new JsonArray());
        if (!any && !other) {
            return null;
        }
        return parts;
    }

    /**
     * Who the client should draw. Inspect sessions use the subject, not the staff viewer.
     */
    static ServerPlayer renderTarget(ServerPlayer passed) {
        ServerPlayer viewer = UltraScreenSpec.inspectViewer();
        if (viewer != null) {
            ServerPlayer subject = AdminInspectSessions.resolveSubject(viewer);
            if (subject != null) {
                return subject;
            }
        }
        return passed;
    }

    private static boolean previewsOtherPlayer(ServerPlayer target) {
        ServerPlayer viewer = UltraScreenSpec.inspectViewer();
        return viewer != null
                && target != null
                && !viewer.m_20148_().equals(target.m_20148_());
    }

    private static JsonObject headBones(ServerPlayer player) {
        String token = "";
        try {
            token = CosmeticHeadBoneService.activeBone(player);
        } catch (Throwable ignored) {
        }
        if (token == null) {
            token = "";
        }
        token = token.trim().toLowerCase(Locale.ROOT);
        List<String> pieces = HeadPartPieces.fragments(token);
        if (token.isEmpty() && pieces.isEmpty()) {
            return null;
        }
        // Prefer the canonical join when fragments are known atomics.
        String canonical = token;
        try {
            if (!pieces.isEmpty()) {
                String joined = HeadPartPieces.join(pieces);
                if (joined != null && !joined.isBlank()) {
                    canonical = joined;
                }
            }
        } catch (Throwable ignored) {
        }
        JsonObject head = new JsonObject();
        head.addProperty("token", canonical);
        JsonArray arr = new JsonArray();
        for (String piece : pieces.isEmpty() ? HeadPartPieces.fragments(canonical) : pieces) {
            if (piece != null && !piece.isBlank()) {
                arr.add(piece);
            }
        }
        head.add("pieces", arr);
        return head;
    }

    /**
     * Worn form identity so the client can attach DMZUltra {@code FormParts} (client-only
     * catalog keyed by form). Omitted when the player is fully base / unknown.
     */
    private static JsonObject formIdentity(ServerPlayer player) {
        Character ch = null;
        try {
            ch = DmzProgression.character(player);
        } catch (Throwable ignored) {
        }
        if (ch == null) {
            return null;
        }
        boolean hasForm = false;
        boolean hasStack = false;
        try {
            hasForm = ch.hasActiveForm();
        } catch (Throwable ignored) {
        }
        try {
            hasStack = ch.hasActiveStackForm();
        } catch (Throwable ignored) {
        }
        if (!hasForm && !hasStack) {
            return null;
        }
        String race = safe(DmzProgression.race(player));
        String group = "";
        String form = "";
        String stackGroup = "";
        String stackForm = "";
        try {
            group = safe(ch.getActiveFormGroup());
            form = safe(ch.getActiveForm());
        } catch (Throwable ignored) {
        }
        try {
            stackGroup = safe(ch.getActiveStackFormGroup());
            stackForm = safe(ch.getActiveStackForm());
        } catch (Throwable ignored) {
        }
        if (race.isEmpty() && group.isEmpty() && form.isEmpty() && stackGroup.isEmpty() && stackForm.isEmpty()) {
            return null;
        }
        JsonObject o = new JsonObject();
        o.addProperty("race", race);
        o.addProperty("group", group);
        o.addProperty("form", form);
        o.addProperty("stackGroup", stackGroup);
        o.addProperty("stackForm", stackForm);
        return o;
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim().toLowerCase(Locale.ROOT);
    }
}
