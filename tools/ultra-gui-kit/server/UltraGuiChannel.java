package com.example.ultragui;

/**
 * Shared channel convention for the Ultra GUI protocol (KaiPath client renderer).
 *
 * <p>From {@code tools/ultra-gui-kit/server/}. Copy into your mod and change the package.
 * Zero LegacyMechanics imports — every mod owns {@code <modid>:gui}.
 *
 * <p>Convention:
 *
 * <ul>
 *   <li>Channel id = {@code new ResourceLocation(modId, "gui")} → wire name {@code modid:gui}
 *   <li>Forge SimpleChannel protocol string = {@link #PROTOCOL_VERSION} ({@code "1"})
 *   <li>Message 0 = GuiOpenPacket (S→C)
 *   <li>Message 1 = GuiActionPacket (C→S)
 *   <li>Message 2 = GuiFeaturesPacket (C→S, optional)
 * </ul>
 *
 * <p>Examples: {@code mymod:gui}, {@code shopmod:gui}, {@code legacymechanics:gui}.
 *
 * <p>KaiPath keys client consumers by this channel id via {@code UltraGuiPartners} — never
 * assume a single mod. Any Forge mod can pull this {@code server/} kit.
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
    public static final int DEFAULT_PANEL_HEIGHT = 300;
    public static final int DEFAULT_MARGIN = 10;

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
