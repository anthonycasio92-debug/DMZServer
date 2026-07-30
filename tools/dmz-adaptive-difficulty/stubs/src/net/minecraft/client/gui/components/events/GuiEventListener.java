package net.minecraft.client.gui.components.events;
public interface GuiEventListener {
    default void m_93692_(boolean focused) {}
    default boolean m_93696_() { return false; }
    default boolean m_6375_(double x, double y, int button) { return false; }
    default boolean m_6348_(double x, double y, int button) { return false; }
    default boolean m_7979_(double x, double y, int button, double dx, double dy) { return false; }
    default boolean m_6050_(double x, double y, double delta) { return false; }
    default boolean m_7933_(int key, int scan, int modifiers) { return false; }
    default boolean m_7920_(int key, int scan, int modifiers) { return false; }
    default boolean m_5534_(char code, int modifiers) { return false; }
    default boolean m_5953_(double x, double y) { return false; }
}
