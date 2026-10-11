package com.dbzlegacy.adaptivedifficulty.client;

import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Legacy fallback client companion. Prefer the Kai Path drop-in bridge instead:
 * {@code tools/kaipath-lm-ultra-bridge/} — do not ship both (same channel).
 *
 * <p>Pulls DMZUltra widgets/textures; talks to server LegacyMechanics. Players do not
 * install the server LegacyMechanics jar.
 */
@Mod(LegacyMechanicsUltraMod.MOD_ID)
public final class LegacyMechanicsUltraMod {
    public static final String MOD_ID = "legacymechanicsultra";
    public static final String VERSION = "4.6.76";
    public static final Logger LOGGER = LogManager.getLogger("LegacyMechanicsUltra");

    public LegacyMechanicsUltraMod() {
        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> NetworkConstants.IGNORESERVERONLY,
                        (remote, isFromServer) -> true));
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        try {
            LmClientBootstrap.init();
            LOGGER.info("[{}] v{} ready — Ultra textures from dmzultra", MOD_ID, VERSION);
        } catch (Throwable t) {
            LOGGER.warn("[{}] client init failed: {}", MOD_ID, t.toString());
        }
    }
}
