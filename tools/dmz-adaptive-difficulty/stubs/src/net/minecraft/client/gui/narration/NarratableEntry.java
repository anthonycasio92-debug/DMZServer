package net.minecraft.client.gui.narration;
public interface NarratableEntry {
    default NarrationPriority narrationPriority() { return NarrationPriority.NONE; }
    default void m_142260_(NarrationElementOutput output) {}
    enum NarrationPriority { NONE, HOVERED, FOCUSED }
}
