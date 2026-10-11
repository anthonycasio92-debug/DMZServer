package com.dbzlegacy.adaptivedifficulty.client.gui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Shown when a screen id has no factory, instead of crashing the client. */
@OnlyIn(Dist.CLIENT)
final class LmMissingScreen extends LmScreen {
    LmMissingScreen(String screenId) {
        super(UltraWidgetAdapter.forgeTheme());
    }

    @Override
    protected void m_7856_() {
        addThemedList(20, 40, 240, 20, java.util.List.of("This menu is unavailable."));
    }
}
