package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.character.CosmeticHeadBoneService;
import com.dbzlegacy.adaptivedifficulty.character.HeadPartPieces;
import com.dragonminez.common.stats.character.Character;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/**
 * Builds the optional {@code layoutJson.playerModel.parts} snapshot for KaiPath / JLDK.
 *
 * <p>Schema {@code 1} — see {@code docs/KAIPATH_GUI_PROTOCOL.md}. The client draws the local
 * player in the declared bounds and uses this object for custom body pieces (DMZ head bones,
 * worn form identity for DMZUltra FormParts, and an open {@code extras} array).
 */
public final class PlayerModelParts {
    /** Current documented schema version inside {@code parts.schema}. */
    public static final int SCHEMA = 1;

    private PlayerModelParts() {}

    /**
     * Snapshot equipped cosmetics for {@code player}. Returns {@code null} when nothing useful
     * can be serialized (caller should omit the {@code parts} field).
     */
    public static JsonObject snapshot(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        JsonObject parts = new JsonObject();
        parts.addProperty("schema", SCHEMA);
        // Character Services always previews the viewing player.
        parts.addProperty("subject", "local");

        boolean any = false;
        JsonObject head = headBones(player);
        if (head != null) {
            parts.add("headBones", head);
            any = true;
        }
        JsonObject form = formIdentity(player);
        if (form != null) {
            parts.add("form", form);
            any = true;
        }
        // Open extension slot — always present so clients can iterate without null checks.
        parts.add("extras", new JsonArray());
        return any ? parts : null;
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
