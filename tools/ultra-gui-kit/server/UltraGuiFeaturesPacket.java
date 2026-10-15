package com.example.ultragui;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: optional feature advertisement. Message index {@code 2} on
 * {@code <modid>:gui} (any partner mod — same wire shape).
 *
 * <p>Copy into your KaiPath / client mod. Backward compatible: if you never send this,
 * the server treats you as the baseline (no optional features).
 *
 * <p>After implementing the full renderer, advertise once per channel:
 *
 * <pre>
 * channel.sendToServer(new UltraGuiFeaturesPacket(
 *     "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\","
 *     + "\"headerBottom\",\"accent\",\"palette\",\"sound\",\"buttonSkin\",\"frame\","
 *     + "\"pagedBook\",\"portraitFrame\",\"fixedFooter\",\"combined.forms.v1\","
 *     + "\"bodyParts\",\"icon\"]"));
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
        UltraGuiOpenPacket.writeUtf8(buf, msg.featuresJson);
    }

    public static UltraGuiFeaturesPacket decode(FriendlyByteBuf buf) {
        String json = "[]";
        if (buf.isReadable()) {
            try {
                json = UltraGuiOpenPacket.readUtf8(buf);
            } catch (Throwable ignored) {
                json = "[]";
            }
        }
        return new UltraGuiFeaturesPacket(json);
    }
}
