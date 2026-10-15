package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import com.dmzlegacy.kaipath.ultra.consumers.UltraGuiPartners;
import java.util.List;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Client bootstrap entry (historical name kept for KaiPath binary/mixin compatibility).
 *
 * <p>Registers <b>all</b> {@link UltraGuiPartners} — not only LegacyMechanics. Wire codecs and
 * the dynamic screen are shared across every {@code <modid>:gui} channel. Server mods never
 * own the client channel; KaiPath does.
 *
 * <p>Partners are discovered from {@link UltraGuiPartners#MARKER_ENTRY} inside mod jars and
 * from {@code config/kaipath-ultra-partners.txt}. A new server mod does not need a KaiPath
 * release. {@link UltraGuiPartners#register(String...)} still works for tests.
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
        List<String> partners = UltraGuiPartners.discoverAndRegister();
        UltraUiBridge.LOGGER.info(
                "[kaipath] Ultra GUI consumers ready partners={} (discovered, multi-mod registry)",
                String.join(",", partners));
    }
}
