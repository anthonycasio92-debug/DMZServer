package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraScreen;
import com.dmzlegacy.kaipath.ultra.UltraUiBridge;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** LM screen id registry. Other mods should not use this — use UltraUiAccess instead. */
@OnlyIn(Dist.CLIENT)
public final class LmScreens {
    private static final Map<String, Function<Minecraft, UltraScreen>> SCREENS = new HashMap<>();

    private LmScreens() {}

    public static void registerDefaults() {
        SCREENS.put("hub", minecraft -> new LmHubScreen());
    }

    public static void register(String screenId, Function<Minecraft, UltraScreen> factory) {
        if (screenId == null || screenId.isBlank() || factory == null) {
            return;
        }
        SCREENS.put(screenId.trim(), factory);
    }

    public static void open(String screenId) {
        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft == null) {
            return;
        }
        String id = screenId == null || screenId.isBlank() ? "hub" : screenId.trim();
        Function<Minecraft, UltraScreen> factory = SCREENS.get(id);
        if (factory == null) {
            minecraft.m_91152_(new MissingScreen(id));
            return;
        }
        try {
            minecraft.m_91152_(factory.apply(minecraft));
        } catch (Throwable t) {
            UltraUiBridge.LOGGER.warn("[kaipath] LM GUI {} failed: {}", id, t.toString());
            minecraft.m_91152_(new MissingScreen(id));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class MissingScreen extends UltraScreen {
        private final String screenId;

        MissingScreen(String screenId) {
            super(Component.m_237113_("Unknown screen"));
            this.screenId = screenId == null ? "" : screenId;
        }

        @Override
        protected void m_7856_() {
            addThemedList(20, 40, 240, 20, java.util.List.of("Unknown screen: " + screenId));
        }
    }
}
