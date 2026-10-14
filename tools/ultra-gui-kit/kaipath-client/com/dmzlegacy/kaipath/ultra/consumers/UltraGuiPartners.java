package com.dmzlegacy.kaipath.ultra.consumers;

/**
 * Partner <em>server</em> mod ids that own an Ultra GUI channel ({@code <modid>:gui}).
 *
 * <p>Wire codecs and {@code LmDynamicScreen} are <b>shared</b> — they do not embed a mod id.
 * KaiPath registers one client consumer per partner so clicks return on the same channel
 * that opened the screen. Add every mod that ships Ultra menus, not only LegacyMechanics.
 *
 * <p>JLDK: extend {@link #DEFAULT_PARTNERS} (or call {@link #register(String...)} at boot)
 * whenever a new server mod adopts the protocol.
 */
public final class UltraGuiPartners {
    /**
     * Default partner namespaces shipped in this reference kit. LM is one entry — not the
     * only supported mod. Keep lowercase Forge mod ids (channel namespace).
     */
    public static final String[] DEFAULT_PARTNERS = {
        "legacymechanics",
        // Examples — uncomment / replace when those mods ship Ultra GUI:
        // "myshop",
        // "townyplus",
    };

    private UltraGuiPartners() {}

    /** Register every {@link #DEFAULT_PARTNERS} id (idempotent). */
    public static void registerDefaults() {
        register(DEFAULT_PARTNERS);
    }

    /** Register one or more partner mod ids. Blank entries are skipped. */
    public static void register(String... modIds) {
        if (modIds == null) {
            return;
        }
        for (String modId : modIds) {
            if (modId == null || modId.isBlank()) {
                continue;
            }
            UltraGuiConsumerRegistry.registerClientChannel(modId.trim().toLowerCase());
        }
    }
}
