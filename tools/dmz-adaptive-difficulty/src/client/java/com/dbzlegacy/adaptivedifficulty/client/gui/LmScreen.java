package com.dbzlegacy.adaptivedifficulty.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Base native screen. Widgets come from {@link UltraWidgetAdapter}, not from DMZUltra types. */
@OnlyIn(Dist.CLIENT)
public abstract class LmScreen extends Screen {
    private final Object theme;
    private final List<FallbackList> fallbackLists = new ArrayList<>();

    protected LmScreen(Object theme) {
        super(Component.m_237113_("Legacy Mechanics"));
        this.theme = theme;
    }

    protected final Object theme() {
        return theme;
    }

    protected final void addThemedButton(int x, int y, int w, int h, String label, Runnable onPress) {
        GuiEventListener widget = UltraWidgetAdapter.button(x, y, w, h, label, onPress, theme);
        if (widget != null) {
            m_142416_(widget);
        }
    }

    protected final void addThemedList(int x, int y, int w, int h, List<String> rows) {
        GuiEventListener widget = UltraWidgetAdapter.list(x, y, w, h, rows, theme);
        if (widget != null) {
            m_142416_(widget);
            return;
        }
        fallbackLists.add(new FallbackList(x, y, rows == null ? List.of() : List.copyOf(rows)));
    }

    @Override
    public void m_280273_(GuiGraphics graphics) {
        graphics.m_280509_(0, 0, this.f_96543_, this.f_96544_, UltraWidgetAdapter.backgroundColor(theme));
    }

    @Override
    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.m_88315_(graphics, mouseX, mouseY, partialTick);
        for (FallbackList list : fallbackLists) {
            int rowY = list.y();
            for (String row : list.rows()) {
                graphics.m_280653_(this.f_96547_, Component.m_237113_(row), list.x(), rowY, 0xFFE8E8E8);
                rowY += 12;
            }
        }
        if (!UltraWidgetAdapter.ultraAvailable()) {
            graphics.m_280653_(
                    this.f_96547_,
                    Component.m_237113_("DMZUltra UI is not installed. Using plain buttons."),
                    16,
                    this.f_96544_ - 22,
                    0xFFFFCC66);
        }
    }

    private record FallbackList(int x, int y, List<String> rows) {}
}
