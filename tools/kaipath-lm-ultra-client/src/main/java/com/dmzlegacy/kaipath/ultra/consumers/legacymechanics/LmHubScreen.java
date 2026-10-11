package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

/**
 * Legacy entry kept for binary/API compatibility. Prefer {@link LmDynamicScreen} via
 * {@link LmScreens#open(GuiOpenPacket)}.
 */
public final class LmHubScreen extends LmDynamicScreen {
    public LmHubScreen() {
        super(GuiOpenPacket.emptyHub());
    }
}
