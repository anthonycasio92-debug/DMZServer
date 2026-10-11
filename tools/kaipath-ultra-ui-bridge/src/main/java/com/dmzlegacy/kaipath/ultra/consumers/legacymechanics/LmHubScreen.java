package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.UltraScreen;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** LM hub built on the shared {@link UltraScreen} API. */
@OnlyIn(Dist.CLIENT)
public final class LmHubScreen extends UltraScreen {
    public LmHubScreen() {
        super(Component.m_237113_("Legacy Mechanics"));
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
        addThemedList(x, y + gap + 8, w * 2 + 8, 16, List.of("Legacy Mechanics"));
    }

    private static void act(String actionId) {
        LmGuiNetwork.sendAction("hub", actionId, "{}");
    }
}
