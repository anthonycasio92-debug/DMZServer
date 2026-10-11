package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Optional LegacyMechanics consumer of the shared Ultra UI bridge.
 *
 * <p>Registers channel {@code legacymechanics:gui} so a <em>server-only</em>
 * LegacyMechanics jar can open Ultra-themed screens. Other mods should not copy
 * this — they use {@link com.dmzlegacy.kaipath.ultra.UltraUiAccess} / {@link
 * com.dmzlegacy.kaipath.ultra.UltraScreen} for their own UIs.
 */
public final class LmUltraConsumer {
    private static boolean bootstrapped;

    private LmUltraConsumer() {}

    public static void bootstrap() {
        if (bootstrapped || !FMLEnvironment.dist.isClient()) {
            return;
        }
        bootstrapped = true;
        try {
            LmScreens.registerDefaults();
            LmGuiNetwork.registerClient();
            UltraUiBridge.LOGGER.info("[kaipath] LegacyMechanics Ultra consumer ready");
        } catch (Throwable t) {
            UltraUiBridge.LOGGER.warn("[kaipath] LM Ultra consumer failed: {}", t.toString());
        }
    }

    /** Dist marker so dedicated server never loads screen classes via this entry. */
    @SuppressWarnings("unused")
    private static final Dist CLIENT = Dist.CLIENT;
}
