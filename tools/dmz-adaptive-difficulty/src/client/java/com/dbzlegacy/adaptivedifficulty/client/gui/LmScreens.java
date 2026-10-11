package com.dbzlegacy.adaptivedifficulty.client.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Native screen ids. The server packet only carries the id string. */
@OnlyIn(Dist.CLIENT)
public final class LmScreens {
    private static final Map<String, Function<Minecraft, LmScreen>> SCREENS = new HashMap<>();

    private LmScreens() {}

    public static void registerDefaults() {
        SCREENS.put("hub", minecraft -> new LmHubScreen());
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
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] native GUI {} failed: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    id,
                    t.toString());
            minecraft.m_91152_(new LmMissingScreen(id));
        }
    }
}
