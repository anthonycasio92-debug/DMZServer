package com.example.ultragui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server → client: authoritative equipped body-parts state for the receiving player.
 * Message index {@code 3} on {@code <modid>:gui} (any partner mod — same wire shape).
 *
 * <p><b>Feature-gated — do not send to clients that did not advertise {@code bodyPartsSync}</b>
 * via {@link UltraGuiFeaturesPacket}. KaiPath ≤ 1.3.29 registered only messages 0–2 on this
 * channel; an unregistered message id aborts the connection. Track the advertisement per
 * connection and gate every send on it (LM's {@code LmGuiNetwork.supportsModelSync} is the
 * production example).
 *
 * <p>Wire format (protocol {@code "1"}, append-compatible):
 *
 * <ol>
 *   <li>{@code partsJson} — the playerModel parts object, <b>same schema</b> as
 *       {@code layoutJson.playerModel.parts} ({@code schema}, {@code subject},
 *       {@code headBones}, {@code form}, {@code bodyParts}, {@code extras}). Always includes
 *       {@code bodyParts} — an empty array clears the client's extra pieces.
 *   <li>{@code reason} — trigger token ({@code login}/{@code equip}/{@code unequip}/
 *       {@code clear}); diagnostics only, client rendering must not depend on it
 * </ol>
 *
 * <p>Client contract: replace — never merge — the cached parts for the local player with the
 * object received. Deduplicate tail-slot pieces server-side before sending (never two
 * {@code slot:"tail"} entries in one payload). Unknown trailing fields are ignored.
 *
 * <p>Protocol: {@code docs/KAIPATH_GUI_PROTOCOL.md} § GuiModelSyncPacket.
 */
public final class UltraGuiModelSyncPacket {
    /** Max {@code partsJson} chars on the wire. Oversized payloads degrade, never truncate. */
    public static final int PARTS_JSON_MAX = 8192;

    /** {@code reason} tokens (diagnostics only — not part of the render contract). */
    public static final String REASON_LOGIN = "login";

    public static final String REASON_EQUIP = "equip";

    public static final String REASON_UNEQUIP = "unequip";

    public static final String REASON_CLEAR = "clear";

    /** Parts schema version ({@code schema:1}). */
    public static final int SCHEMA = 1;

    private final String partsJson;
    private final String reason;

    public UltraGuiModelSyncPacket(String partsJson, String reason) {
        this.partsJson = sanitizePartsJson(partsJson);
        this.reason = trim(reason == null ? "" : reason, 32);
    }

    /** PlayerModel parts object JSON ({@code schema:1} layout). */
    public String partsJson() {
        return partsJson;
    }

    /** Trigger token — diagnostics only. */
    public String reason() {
        return reason;
    }

    /** Serialize 2 fields — no channel / mod-id bytes. */
    public static void encode(UltraGuiModelSyncPacket msg, FriendlyByteBuf buf) {
        UltraGuiOpenPacket.writeUtf8(buf, msg.partsJson);
        UltraGuiOpenPacket.writeUtf8(buf, msg.reason);
    }

    /** Decode fields 1–2; missing trailing fields default. Leftover bytes are ignored. */
    public static UltraGuiModelSyncPacket decode(FriendlyByteBuf buf) {
        String parts = UltraGuiOpenPacket.readUtf8(buf);
        String reason = buf.isReadable() ? UltraGuiOpenPacket.readUtf8(buf) : "";
        return new UltraGuiModelSyncPacket(parts, reason);
    }

    /**
     * Guarantees valid JSON within {@link #PARTS_JSON_MAX}. Oversized payloads drop
     * {@code headBones}, then {@code form}, then fall back to a minimal valid object
     * (clears client state) — a hard cut mid-JSON would break the whole message.
     */
    static String sanitizePartsJson(String json) {
        String fallback =
                "{\"schema\":" + SCHEMA
                        + ",\"subject\":\"local\",\"bodyParts\":[],\"extras\":[]}";
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            String out = o.toString();
            if (out.length() <= PARTS_JSON_MAX) {
                return out;
            }
            o.remove("headBones");
            out = o.toString();
            if (out.length() <= PARTS_JSON_MAX) {
                return out;
            }
            o.remove("form");
            out = o.toString();
            if (out.length() <= PARTS_JSON_MAX) {
                return out;
            }
            return fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        return text.length() <= max ? text : text.substring(0, max);
    }
}
