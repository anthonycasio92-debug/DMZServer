package com.dmzlegacy.kaipath.ultra;

import com.dmzlegacy.kaipath.ultra.consumers.legacymechanics.LmUltraConsumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Kai Path Ultra UI bridge bootstrap.
 *
 * <p>Hosts a <strong>shared</strong> soft dependency on {@code dmzultra} so any mod
 * (Kai Path itself, LegacyMechanics clients, future addons) can pull Ultra themes /
 * widgets through {@link UltraUiAccess} without compiling against {@code com.dmzultra}.
 *
 * <p>Call {@link #bootstrap()} once from Kai Path client init. Optional known
 * consumers (e.g. LegacyMechanics channel) register here; other mods only need
 * {@link UltraUiAccess} at runtime.
 */
public final class UltraUiBridge {
    public static final Logger LOGGER = LogManager.getLogger("KaiPath/UltraUi");
    private static boolean bootstrapped;

    private UltraUiBridge() {}

    /** Safe to call multiple times; no-ops on the dedicated server. */
    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        bootstrapped = true;
        try {
            UltraUiAccess.probe();
            LOGGER.info(
                    "[kaipath] Ultra UI bridge ready (dmzultra widgets available={})",
                    UltraUiAccess.isAvailable());
            // Optional first-party consumers. Third-party mods do not register here —
            // they just call UltraUiAccess from their own client code / depend on kaipath.
            LmUltraConsumer.bootstrap();
        } catch (Throwable t) {
            LOGGER.warn("[kaipath] Ultra UI bridge init failed: {}", t.toString());
        }
    }

    /** Dist guard helper for callers that already know they are on CLIENT. */
    public static void bootstrapClientOnly() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            bootstrap();
        }
    }
}
