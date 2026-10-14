package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.consumers.UltraGuiConsumerRegistry;

/**
 * Convenience constants for the LegacyMechanics channel only. Packet codecs stay
 * channel-agnostic — other mods must <b>not</b> import this class; register via
 * {@link com.dmzlegacy.kaipath.ultra.consumers.UltraGuiPartners} /
 * {@link UltraGuiConsumerRegistry#registerClientChannel(String)} instead.
 */
public final class LmGuiNetwork {
    /** LM Forge mod id — one of many possible Ultra GUI channel namespaces. */
    public static final String CHANNEL_NAMESPACE = "legacymechanics";
    public static final String CHANNEL_PATH = UltraGuiConsumerRegistry.CHANNEL_PATH;
    public static final String PROTOCOL_VERSION = UltraGuiConsumerRegistry.PROTOCOL_VERSION;

    /** Wire id {@code legacymechanics:gui}. */
    public static final String CHANNEL_ID = CHANNEL_NAMESPACE + ":" + CHANNEL_PATH;

    /** @deprecated use {@link #PROTOCOL_VERSION} */
    @Deprecated
    public static final String PROTOCOL = PROTOCOL_VERSION;

    private LmGuiNetwork() {}

    public static boolean accepts(String remote) {
        if (remote == null || remote.isBlank()) {
            return true;
        }
        return true; // mismatch warning lives in UltraGuiConsumerRegistry
    }

    public static void registerClient() {
        UltraGuiConsumerRegistry.registerClientChannel(CHANNEL_NAMESPACE, PROTOCOL_VERSION);
    }

    /** Send on the LM channel. Prefer {@link UltraGuiConsumerRegistry#sendAction} with an explicit channel. */
    public static void sendAction(String screenId, String actionId, String argsJson) {
        UltraGuiConsumerRegistry.sendAction(CHANNEL_ID, screenId, actionId, argsJson);
    }
}
