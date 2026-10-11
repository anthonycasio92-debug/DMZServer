package com.dbzlegacy.adaptivedifficulty.client.gui;

import com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork;
import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Staff proof screen. The six hub actions match the CNPC hub buttons. */
@OnlyIn(Dist.CLIENT)
public final class LmHubScreen extends LmScreen {
    public LmHubScreen() {
        super(UltraWidgetAdapter.forgeTheme());
    }

    @Override
    protected void m_7856_() {
        int x = 20;
        int y = 36;
        int w = 150;
        int h = 20;
        int gap = 24;
        addThemedButton(x, y, w, h, "Character", () -> act("character"));
        addThemedButton(x + w + 8, y, w, h, "Difficulty", () -> act("difficulty"));
        y += gap;
        addThemedButton(x, y, w, h, "Progression", () -> act("progression"));
        addThemedButton(x + w + 8, y, w, h, "Prestige", () -> act("prestige"));
        y += gap;
        addThemedButton(x, y, w, h, "Spar", () -> act("spar"));
        addThemedButton(x + w + 8, y, w, h, "Rival", () -> act("rival"));
        addThemedList(x, y + gap + 8, w * 2 + 8, 16, List.of("Native hub"));
    }

    private static void act(String actionId) {
        LmGuiNetwork.sendAction("hub", actionId, "{}");
    }
}
