package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Boots the LM Ultra GUI client consumer (one entry in the multi-mod registry).
 * Always registers on the physical client — server mods do not own the client channel.
 *
 * <p>Other mods: {@code UltraGuiConsumerRegistry.registerClientChannel("mymod")}.
 */
public final class LmUltraConsumer {
    private static boolean bootstrapped;

    private LmUltraConsumer() {}

    public static void bootstrap() {
        if (bootstrapped || !FMLEnvironment.dist.isClient()) {
            return;
        }
        bootstrapped = true;
        // Do NOT skip when the partner mod jar is also on the client — they register no client channel.
        LmScreens.registerDefaults();
        LmGuiNetwork.registerClient();
        UltraUiBridge.LOGGER.info(
                "[kaipath] Ultra GUI consumer ready channel={} (dynamic, multi-mod registry)",
                LmGuiNetwork.CHANNEL_ID);
    }
}
