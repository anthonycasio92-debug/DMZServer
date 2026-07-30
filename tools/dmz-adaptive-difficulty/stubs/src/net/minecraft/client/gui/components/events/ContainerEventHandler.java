package net.minecraft.client.gui.components.events;
import java.util.List;
public interface ContainerEventHandler extends GuiEventListener {
    List<? extends GuiEventListener> m_6702_();
    boolean m_7282_();
    void m_7897_(boolean dragging);
    GuiEventListener m_7222_();
    void m_7522_(GuiEventListener listener);
}
