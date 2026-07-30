package net.minecraft.client.gui.layouts;
import net.minecraft.client.gui.components.events.GuiEventListener;
public interface LayoutElement {
    void m_264042_(int x, int y);
    int m_252754_();
    int m_252907_();
    int m_93694_();
    int m_93695_();
    default void visitWidgets(java.util.function.Consumer<? super net.minecraft.client.gui.components.AbstractWidget> consumer) {}
}
