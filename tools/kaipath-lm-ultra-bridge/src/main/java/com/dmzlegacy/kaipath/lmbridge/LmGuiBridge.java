package com.dmzlegacy.kaipath.lmbridge;

import com.dmzlegacy.kaipath.lmbridge.gui.LmScreens;
import com.dmzlegacy.kaipath.lmbridge.net.LmGuiNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Drop-in LegacyMechanics Ultra GUI bridge for Kai Path.
 *
 * <p>Call {@link #bootstrap()} once from Kai Path client init (for example from
 * {@code KaiClient} / a {@code Dist.CLIENT} setup). Registers the
 * {@code legacymechanics:gui} channel so server-only LegacyMechanics can open
 * Ultra-themed screens. Widgets/textures are pulled from {@code dmzultra} when
 * present; otherwise plain vanilla buttons are used.
 *
 * <p>Does not add a second Forge mod id — lives inside {@code kaipath}.
 */
public final class LmGuiBridge {
    public static final Logger LOGGER = LogManager.getLogger("KaiPath/LmGuiBridge");
    private static boolean bootstrapped;

    private LmGuiBridge() {}

    /** Safe to call multiple times; no-ops on dedicated server. */
    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        bootstrapped = true;
        try {
            DistOnly.init();
            LOGGER.info("[kaipath] LM Ultra GUI bridge ready (channel legacymechanics:gui)");
        } catch (Throwable t) {
            LOGGER.warn("[kaipath] LM Ultra GUI bridge init failed: {}", t.toString());
        }
    }

    /** Isolated so the dedicated server never resolves client screen classes. */
    private static final class DistOnly {
        private DistOnly() {}

        private static void init() {
            if (FMLEnvironment.dist != Dist.CLIENT) {
                return;
            }
            LmScreens.registerDefaults();
            LmGuiNetwork.registerClient();
        }
    }
}
