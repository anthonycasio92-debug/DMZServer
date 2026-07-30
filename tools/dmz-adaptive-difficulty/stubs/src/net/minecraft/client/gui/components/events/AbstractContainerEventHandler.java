package net.minecraft.client.gui.components.events;
import java.util.List;
public abstract class AbstractContainerEventHandler implements ContainerEventHandler {
    private GuiEventListener focused;
    private boolean isDragging;
    public abstract List<? extends GuiEventListener> m_6702_();
    public final boolean m_7282_() { return isDragging; }
    public final void m_7897_(boolean dragging) { isDragging = dragging; }
    public GuiEventListener m_7222_() { return focused; }
    public void m_7522_(GuiEventListener listener) { focused = listener; }
}
