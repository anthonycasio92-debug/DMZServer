package com.dmzlegacy.kaipath.lmbridge.gui;

import com.dmzlegacy.kaipath.lmbridge.LmGuiBridge;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Screen id registry. Server open packets only carry the id string. */
@OnlyIn(Dist.CLIENT)
public final class LmScreens {
    private static final Map<String, Function<Minecraft, LmScreen>> SCREENS = new HashMap<>();

    private LmScreens() {}

    public static void registerDefaults() {
        SCREENS.put("hub", minecraft -> new LmHubScreen());
    }

    /** Optional: Kai Path can register more Ultra screens later. */
    public static void register(String screenId, Function<Minecraft, LmScreen> factory) {
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
        Function<Minecraft, LmScreen> factory = SCREENS.get(id);
        if (factory == null) {
            minecraft.m_91152_(new LmMissingScreen(id));
            return;
        }
        try {
            minecraft.m_91152_(factory.apply(minecraft));
        } catch (Throwable t) {
            LmGuiBridge.LOGGER.warn("[kaipath] LM GUI {} failed: {}", id, t.toString());
            minecraft.m_91152_(new LmMissingScreen(id));
        }
    }
}
