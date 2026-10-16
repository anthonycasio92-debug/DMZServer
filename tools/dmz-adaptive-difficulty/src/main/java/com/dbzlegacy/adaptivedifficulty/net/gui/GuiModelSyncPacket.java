package com.dbzlegacy.adaptivedifficulty.net.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.ultra.PlayerModelParts;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server → client: authoritative equipped body-parts state for the receiving player
 * ({@code <modid>:gui} message {@link UltraGuiProtocol#MSG_MODEL_SYNC}).
 *
 * <p><b>Feature-gated — do not send to clients that did not advertise
 * {@code bodyPartsSync}</b> via {@link GuiFeaturesPacket}. KaiPath 1.3.29 and older
 * registered only messages 0–2 on this channel; an unregistered message id aborts the
 * connection. {@link LmGuiNetwork#sendModelSync} enforces the gate.
 *
 * <p>Wire format (protocol {@link UltraGuiProtocol#PROTOCOL_VERSION}, append-compatible):
 *
 * <ol>
 *   <li>{@code partsJson} — the playerModel parts object, <b>same schema</b> as
 *       {@code GuiOpenPacket layoutJson.playerModel.parts} ({@code schema}, {@code subject},
 *       {@code headBones}, {@code form}, {@code bodyParts}, {@code extras}). Always includes
 *       {@code bodyParts} — an empty array clears the client's extra pieces.
 *   <li>{@code reason} — trigger token ({@code login}/{@code equip}/{@code unequip}/
 *       {@code clear}); diagnostics only, client rendering must not depend on it
 * </ol>
 *
 * <p>Client contract (JLDK): replace — never merge — the cached parts for the local player
 * with the object received. The server already deduplicates tail-slot pieces
 * ({@code saiyan_tail} is only sent when no other {@code tail}-slot piece is present).
 * Unknown trailing fields are ignored.
 */
public final class GuiModelSyncPacket {
    /** Max {@code partsJson} chars on the wire. Oversized payloads degrade, never truncate. */
    public static final int PARTS_JSON_MAX = 8192;

    /** {@code reason} tokens (diagnostics only — not part of the render contract). */
    public static final String REASON_LOGIN = "login";

    public static final String REASON_EQUIP = "equip";
    public static final String REASON_UNEQUIP = "unequip";
    public static final String REASON_CLEAR = "clear";

    private final String partsJson;
    private final String reason;

    public GuiModelSyncPacket(String partsJson, String reason) {
        this.partsJson = sanitizePartsJson(partsJson);
        this.reason = GuiOpenPacket.trim(reason == null ? "" : reason, 32);
    }

    /** PlayerModel parts object JSON ({@code PlayerModelParts.SCHEMA} layout). */
    public String partsJson() {
        return partsJson;
    }

    /** Trigger token — diagnostics only. */
    public String reason() {
        return reason;
    }

    /** Serialize 2 fields — no channel / mod-id bytes. */
    public static void encode(GuiModelSyncPacket msg, FriendlyByteBuf buf) {
        GuiOpenPacket.writeUtf8(buf, msg.partsJson);
        GuiOpenPacket.writeUtf8(buf, msg.reason);
    }

    /** Decode fields 1–2; missing trailing fields default. Leftover bytes are ignored. */
    public static GuiModelSyncPacket decode(FriendlyByteBuf buf) {
        String parts = GuiOpenPacket.readUtf8(buf);
        String reason = buf.isReadable() ? GuiOpenPacket.readUtf8(buf) : "";
        return new GuiModelSyncPacket(parts, reason);
    }

    /** Server never renders — KaiPath owns the client handler. */
    public static void handle(GuiModelSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // No player/world access on the network thread — mark handled only.
        context.setPacketHandled(true);
    }

    /**
     * Guarantees valid JSON within {@link #PARTS_JSON_MAX}. Oversized payloads drop
     * {@code headBones}, then {@code form}, then fall back to a minimal valid object
     * (clears client state) — a hard cut mid-JSON would break the whole message.
     */
    static String sanitizePartsJson(String json) {
        String fallback = "{\"schema\":" + PlayerModelParts.SCHEMA
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
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] GuiModelSync partsJson oversized — dropped headBones len={}",
                        AdaptiveDifficultyMod.MOD_ID,
                        out.length());
                return out;
            }
            o.remove("form");
            out = o.toString();
            if (out.length() <= PARTS_JSON_MAX) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] GuiModelSync partsJson oversized — dropped headBones+form len={}",
                        AdaptiveDifficultyMod.MOD_ID,
                        out.length());
                return out;
            }
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] GuiModelSync partsJson oversized after drops — sending empty parts",
                    AdaptiveDifficultyMod.MOD_ID);
            return fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
