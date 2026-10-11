package com.dmzlegacy.kaipath.lmbridge.gui;

import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Shown when a screen id has no factory. */
@OnlyIn(Dist.CLIENT)
final class LmMissingScreen extends LmScreen {
    private final String screenId;

    LmMissingScreen(String screenId) {
        super(UltraWidgetAdapter.forgeTheme());
        this.screenId = screenId == null ? "" : screenId;
    }

    @Override
    protected void m_7856_() {
        addThemedList(20, 40, 240, 20, List.of("Unknown screen: " + screenId));
    }
}
