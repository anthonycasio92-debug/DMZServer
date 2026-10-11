package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Boots the LM Ultra GUI client consumer. Always registers on the physical client —
 * LM is server-only and does not own this channel on the client.
 */
public final class LmUltraConsumer {
    private static boolean bootstrapped;

    private LmUltraConsumer() {}

    public static void bootstrap() {
        if (bootstrapped || !FMLEnvironment.dist.isClient()) {
            return;
        }
        bootstrapped = true;
        // Do NOT skip when legacymechanics is loaded — LM registers no client channel.
        LmScreens.registerDefaults();
        LmGuiNetwork.registerClient();
        UltraUiBridge.LOGGER.info("[kaipath] Optional LegacyMechanics Ultra GUI consumer ready (dynamic)");
    }
}
