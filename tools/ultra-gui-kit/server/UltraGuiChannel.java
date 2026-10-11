package com.example.ultragui;

/**
 * Shared channel convention for the Ultra GUI protocol (KaiPath client renderer).
 *
 * <p>Copy this file into your mod and change the package. There are <b>zero</b> LegacyMechanics
 * imports — only the naming rule that every mod owns {@code <modid>:gui}.
 *
 * <p>Convention:
 *
 * <ul>
 *   <li>Channel id = {@code new ResourceLocation(modId, "gui")} → wire name {@code modid:gui}
 *   <li>Forge SimpleChannel protocol string = {@link #PROTOCOL_VERSION} ({@code "1"})
 *   <li>Message 0 = GuiOpenPacket (S→C), message 1 = GuiActionPacket (C→S)
 * </ul>
 *
 * <p>Examples: {@code legacymechanics:gui}, {@code mymod:gui}, {@code shopmod:gui}.
 *
 * <p>KaiPath keys client consumers by this channel id — never assume a single mod.
 */
public final class UltraGuiChannel {
    /** Path segment of every Ultra GUI channel. */
    public static final String PATH = "gui";

    /**
     * Forge SimpleChannel protocol. Bumping requires a coordinated KaiPath release.
     * Clients should accept mismatched remotes with a warning (do not refuse to open).
     */
    public static final String PROTOCOL_VERSION = "1";

    public static final int DEFAULT_PANEL_WIDTH = 350;
    public static final int DEFAULT_PANEL_HEIGHT = 220;
    public static final int DEFAULT_MARGIN = 6;

    private UltraGuiChannel() {}

    /** Wire channel id string {@code <modId>:gui}. */
    public static String channelId(String modId) {
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId required");
        }
        return modId.trim().toLowerCase() + ":" + PATH;
    }

    /**
     * Forge helper (uncomment when copying into a Forge mod):
     *
     * <pre>
     * import net.minecraft.resources.ResourceLocation;
     * public static ResourceLocation location(String modId) {
     *     return new ResourceLocation(modId.trim().toLowerCase(), PATH);
     * }
     * </pre>
     */
    public static String namespaceOf(String channelId) {
        if (channelId == null) {
            return "";
        }
        int colon = channelId.indexOf(':');
        return colon < 0 ? channelId : channelId.substring(0, colon);
    }
}
