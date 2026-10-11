package com.dbzlegacy.adaptivedifficulty.client;

import com.dbzlegacy.adaptivedifficulty.client.gui.LmScreens;
import com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Loaded by name from the mod constructor on the physical client only. */
@OnlyIn(Dist.CLIENT)
public final class LmClientBootstrap {
    private LmClientBootstrap() {}

    public static void init() {
        LmScreens.registerDefaults();
        LmGuiNetwork.register();
    }
}
