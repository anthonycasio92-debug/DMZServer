package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import com.dmzlegacy.kaipath.ultra.consumers.UltraGuiPartners;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Client bootstrap entry (historical name kept for KaiPath binary/mixin compatibility).
 *
 * <p>Registers <b>all</b> {@link UltraGuiPartners} — not only LegacyMechanics. Wire codecs and
 * the dynamic screen are shared across every {@code <modid>:gui} channel. Server mods never
 * own the client channel; KaiPath does.
 *
 * <p>To add another mod: put its Forge mod id in {@link UltraGuiPartners#DEFAULT_PARTNERS}
 * (or call {@link UltraGuiPartners#register(String...)} before/after bootstrap).
 */
public final class LmUltraConsumer {
    private static boolean bootstrapped;

    private LmUltraConsumer() {}

    public static void bootstrap() {
        if (bootstrapped || !FMLEnvironment.dist.isClient()) {
            return;
        }
        bootstrapped = true;
        // Do NOT skip when a partner mod jar is also on the client — they register no client channel.
        LmScreens.registerDefaults();
        UltraGuiPartners.registerDefaults();
        UltraUiBridge.LOGGER.info(
                "[kaipath] Ultra GUI consumers ready partners={} (dynamic, multi-mod registry)",
                String.join(",", UltraGuiPartners.DEFAULT_PARTNERS));
    }
}
