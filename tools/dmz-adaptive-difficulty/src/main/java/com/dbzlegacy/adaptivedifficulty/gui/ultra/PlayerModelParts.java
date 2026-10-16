package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.character.CosmeticBodyPartService;
import com.dbzlegacy.adaptivedifficulty.character.CosmeticHeadBoneService;
import com.dbzlegacy.adaptivedifficulty.character.HeadPartPieces;
import com.dbzlegacy.adaptivedifficulty.gui.AdminInspectSessions;
import com.dragonminez.common.stats.character.Character;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/**
 * Builds the optional {@code layoutJson.playerModel.parts} snapshot for KaiPath / JLDK.
 *
 * <p>Schema {@code 1} - see {@code docs/KAIPATH_GUI_PROTOCOL.md}. {@code subject} is the bridge
 * field any mod may set: {@code local} for the viewing player, or {@code uuid:<id>} for someone
 * else. KaiPath draws that player's model, skin, and parts. This object also carries custom
 * body pieces (DMZ head bones, worn form identity, explicit {@code bodyParts}, and an
 * open {@code extras} array). {@code form} stays even when {@code bodyParts} is sent so
 * older clients that only know the form ids keep working.
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
        JsonArray body = bodyParts(target);
        if (body != null && !body.isEmpty()) {
            parts.add("bodyParts", body);
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
     * Explicit pieces for {@code parts.bodyParts}. Resolves the worn form (and stack form)
     * through {@link BodyPartCatalog}, which mirrors DMZUltra FormParts. Also names
     * {@code saiyan_tail} when the character still has the race tail. Empty when nothing
     * applies. Never throws — a missing catalog still reports the race tail.
     */
    private static JsonArray bodyParts(ServerPlayer player) {
        JsonArray out = new JsonArray();
        try {
            Character ch = null;
            try {
                ch = DmzProgression.character(player);
            } catch (Throwable ignored) {
            }
            String race = safe(DmzProgression.race(player));
            String group = "";
            String form = "";
            String stackGroup = "";
            String stackForm = "";
            boolean tail = false;
            if (ch != null) {
                try {
                    tail = ch.isHasSaiyanTail();
                } catch (Throwable ignored) {
                }
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
            }
            Set<String> seen = new java.util.LinkedHashSet<>();
            for (BodyPartCatalog.Part part :
                    BodyPartCatalog.resolve(race, group, form, stackGroup, stackForm, tail)) {
                if (part == null || part.id() == null || part.id().isBlank()) {
                    continue;
                }
                JsonObject entry = new JsonObject();
                entry.addProperty("id", part.id());
                entry.addProperty("source", part.source());
                entry.addProperty("slot", part.slot());
                seen.add(part.id().trim().toLowerCase(Locale.ROOT));
                out.add(entry);
            }
            // Custom Body Parts Shop pieces the player turned on. Dedup ids the worn
            // form already named; the KaiPath 1.3.29 preview attaches each known mesh.
            for (String customId : CosmeticBodyPartService.equippedParts(player)) {
                if (customId == null
                        || customId.isBlank()
                        || !seen.add(customId.trim().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                JsonObject custom = new JsonObject();
                custom.addProperty("id", customId);
                custom.addProperty("source", BodyPartCatalog.SOURCE_DMZULTRA);
                custom.addProperty("slot", BodyPartCatalog.slot(customId));
                out.add(custom);
            }
        } catch (Throwable ignored) {
            return new JsonArray();
        }
        return out;
    }

    /**
     * Worn form identity so the client can attach DMZUltra {@code FormParts} (client-only
     * catalog keyed by form). Omitted when the player is fully base / unknown. Kept even
     * when {@link #bodyParts} is present.
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
