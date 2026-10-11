package net.minecraft.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

/** Compile stub. The Forge client jar supplies the real class. */
public class Screen {
    public Minecraft f_96541_;
    public int f_96543_;
    public int f_96544_;
    public Font f_96547_;

    protected Screen(Component title) {}

    protected void m_7856_() {}

    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    public void m_280273_(GuiGraphics graphics) {}

    protected <T extends GuiEventListener> T m_142416_(T widget) {
        return widget;
    }
}
