package com.dbzlegacy.adaptivedifficulty.compat.sdu;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.network.DifficultyNet;
import com.dbzlegacy.adaptivedifficulty.network.RequestOpenDifficultyPacket;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import net.shurui.dev.sdu.api.SduHubExtensions;

/** Optional SDU hub entry — mirrors tournaments / raid bosses. */
public final class SduHubCompat {
    private SduHubCompat() {}

    public static void register() {
        if (ModList.get().isLoaded("sdu")) {
            Sdu.register();
        }
    }

    private static final class Sdu {
        private Sdu() {}

        static void register() {
            SduHubExtensions.section("dmz_adaptive_difficulty", Component.m_237113_("Difficulty"))
                    .entry(
                            "dmz_adaptive_difficulty:main",
                            Component.m_237113_("Adaptive Difficulty"),
                            Component.m_237113_("Open personal difficulty controls"),
                            () -> DifficultyNet.sendToServer(new RequestOpenDifficultyPacket("main"))
                    )
                    .register();
            AdaptiveDifficultyMod.LOGGER.info("[{}] registered SDU hub entry", AdaptiveDifficultyMod.MOD_ID);
        }
    }
}
