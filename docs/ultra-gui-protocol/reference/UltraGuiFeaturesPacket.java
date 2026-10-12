package com.example.ultragui;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: optional feature advertisement. Message index {@code 2} on
 * {@code <modid>:gui}.
 *
 * <p>Copy into your KaiPath / client mod. Backward compatible: if you never send this,
 * the server treats you as the 1.3.1 baseline (no {@code style} / {@code activeTab}).
 *
 * <p>After implementing button styles and active-tab highlight, send once:
 *
 * <pre>
 * channel.sendToServer(new UltraGuiFeaturesPacket("[\"style\",\"activeTab\"]"));
 * </pre>
 */
public final class UltraGuiFeaturesPacket {
    private final String featuresJson;

    public UltraGuiFeaturesPacket(String featuresJson) {
        this.featuresJson = featuresJson == null || featuresJson.isBlank() ? "[]" : featuresJson.trim();
    }

    public String featuresJson() {
        return featuresJson;
    }

    public static void encode(UltraGuiFeaturesPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.featuresJson);
    }

    public static UltraGuiFeaturesPacket decode(FriendlyByteBuf buf) {
        String json = "[]";
        if (buf.isReadable()) {
            try {
                json = buf.readUtf();
            } catch (Throwable ignored) {
                json = "[]";
            }
        }
        return new UltraGuiFeaturesPacket(json);
    }
}
