package net.minecraft.client.gui.components;

import net.minecraft.network.chat.Component;

/** Compile stub. The Forge client jar supplies the real class. */
public class Button extends AbstractWidget {
    public static Builder m_253074_(Component message, OnPress onPress) {
        return new Builder();
    }

    public interface OnPress {
        void m_93750_(Button button);
    }

    public static class Builder {
        public Builder m_252987_(int x, int y, int width, int height) {
            return this;
        }

        public Button m_253136_() {
            return new Button();
        }
    }
}
