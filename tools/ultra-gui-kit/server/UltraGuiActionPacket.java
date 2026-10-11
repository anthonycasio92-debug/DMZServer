package com.example.ultragui;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Client → server: button or tab press. <b>Mod-agnostic</b> — encode/decode never inspects or
 * rewrites action ids. The owning mod's server decides validity.
 *
 * <p>Wire fields (not a protocol version — that lives on the Forge channel):
 *
 * <ol>
 *   <li>screenId — echo {@link UltraGuiOpenPacket#screenId()} verbatim
 *   <li>actionId — echo button/tab {@code id} verbatim
 *   <li>argsJson — echo button {@code args} JSON verbatim
 * </ol>
 *
 * <p>Copy into your mod and rename the package. Depends only on Minecraft {@link FriendlyByteBuf}.
 */
public final class UltraGuiActionPacket {
    public static final int ACTION_ID_MAX = 192;

    private final String screenId;
    private final String actionId;
    private final String argsJson;

    public UltraGuiActionPacket(String screenId, String actionId, String argsJson) {
        this.screenId = trim(screenId, 64);
        this.actionId = trim(actionId, ACTION_ID_MAX);
        this.argsJson = trim(argsJson == null ? "{}" : argsJson, 512);
    }

    public String screenId() {
        return screenId;
    }

    public String actionId() {
        return actionId;
    }

    public String argsJson() {
        return argsJson;
    }

    public static void encode(UltraGuiActionPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.screenId);
        buf.writeUtf(msg.actionId);
        buf.writeUtf(msg.argsJson);
    }

    public static UltraGuiActionPacket decode(FriendlyByteBuf buf) {
        return new UltraGuiActionPacket(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String text = value.trim();
        return text.length() <= max ? text : text.substring(0, max);
    }
}
