package com.dbzlegacy.adaptivedifficulty.net.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client → server: optional Ultra GUI feature advertisement.
 *
 * <p>Wire (protocol {@link UltraGuiProtocol#PROTOCOL_VERSION}):
 *
 * <ol>
 *   <li>{@code featuresJson} — JSON array of feature ids the client renders (e.g.
 *       {@code ["style","activeTab"]})
 * </ol>
 *
 * <p>Backward compatible: clients that never send this packet are treated as the KaiPath
 * 1.3.1 baseline (dynamic layout/buttons, but no {@code style} / {@code activeTab} rendering).
 * Extra trailing fields are ignored.
 */
public final class GuiFeaturesPacket {
    private final String featuresJson;

    public GuiFeaturesPacket(String featuresJson) {
        this.featuresJson = GuiOpenPacket.trim(featuresJson == null ? "[]" : featuresJson, 512);
    }

    public String featuresJson() {
        return featuresJson;
    }

    public static void encode(GuiFeaturesPacket msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.featuresJson == null || msg.featuresJson.isBlank() ? "[]" : msg.featuresJson);
    }

    public static GuiFeaturesPacket decode(FriendlyByteBuf buf) {
        String json = "[]";
        if (buf.isReadable()) {
            try {
                json = buf.m_130277_();
            } catch (Throwable ignored) {
                json = "[]";
            }
        }
        // Ignore any future fields beyond #1.
        return new GuiFeaturesPacket(json);
    }

    public static void handle(GuiFeaturesPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> dispatchOnMain(sender, msg));
        context.setPacketHandled(true);
    }

    static void dispatchOnMain(ServerPlayer player, GuiFeaturesPacket msg) {
        if (player == null || msg == null) {
            return;
        }
        Set<String> features = parseFeatures(msg.featuresJson());
        UltraClientFeatures.recordAdvertisement(player, features, msg.featuresJson());
        UltraClientFeatures.warnMissingOptionalFeatures(player, "GuiFeaturesPacket");
        // bodyPartsSync advertisers get the initial authoritative world sync (login trigger).
        LmGuiNetwork.onClientAdvertisement(player);
    }

    static Set<String> parseFeatures(String featuresJson) {
        Set<String> out = new LinkedHashSet<>();
        if (featuresJson == null || featuresJson.isBlank()) {
            return out;
        }
        try {
            JsonElement el = JsonParser.parseString(featuresJson);
            if (!el.isJsonArray()) {
                return out;
            }
            JsonArray arr = el.getAsJsonArray();
            for (JsonElement item : arr) {
                if (item == null || !item.isJsonPrimitive()) {
                    continue;
                }
                String id = item.getAsString();
                if (id == null || id.isBlank()) {
                    continue;
                }
                out.add(id.trim().toLowerCase(Locale.ROOT));
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] GuiFeaturesPacket featuresJson parse failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    featuresJson);
        }
        return out;
    }
}
