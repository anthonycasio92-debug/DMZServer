#!/usr/bin/env python3
"""Generate minimal compile stubs for Minecraft/Forge client APIs (SRG names)."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent

FILES = {
    "net/minecraft/network/chat/Component.java": """
package net.minecraft.network.chat;
public interface Component {
  static MutableComponent m_237113_(String s) { return new MutableComponent(s); }
  static MutableComponent m_237119_() { return new MutableComponent(""); }
}
""",
    "net/minecraft/network/chat/MutableComponent.java": """
package net.minecraft.network.chat;
public class MutableComponent implements Component {
  public MutableComponent(String s) {}
}
""",
    "net/minecraft/util/FormattedCharSequence.java": """
package net.minecraft.util;
public interface FormattedCharSequence {}
""",
    "net/minecraft/client/gui/Font.java": """
package net.minecraft.client.gui;
import net.minecraft.util.FormattedCharSequence;
public class Font {
  public FormattedCharSequence m_92834_(String text, int width) { return new FormattedCharSequence() {}; }
}
""",
    "net/minecraft/client/gui/GuiGraphics.java": """
package net.minecraft.client.gui;
import net.minecraft.util.FormattedCharSequence;
public class GuiGraphics {
  public void m_280509_(int x1, int y1, int x2, int y2, int color) {}
  public void m_280488_(Font font, FormattedCharSequence text, int x, int y, int color) {}
  public void m_280588_(int x1, int y1, int x2, int y2) {}
  public void m_280618_() {}
}
""",
    "net/minecraft/client/gui/components/events/GuiEventListener.java": """
package net.minecraft.client.gui.components.events;
public interface GuiEventListener {}
""",
    "net/minecraft/client/gui/components/AbstractWidget.java": """
package net.minecraft.client.gui.components;
import net.minecraft.client.gui.components.events.GuiEventListener;
public abstract class AbstractWidget implements GuiEventListener {}
""",
    "net/minecraft/client/gui/screens/Screen.java": """
package net.minecraft.client.gui.screens;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
public abstract class Screen {
  public int f_96543_;
  public int f_96544_;
  public Font f_96547_ = new Font();
  protected Screen(Component title) {}
  protected void m_7856_() {}
  protected void m_169413_() {}
  public void m_7379_() {}
  public <T extends GuiEventListener> T m_142416_(T w) { return w; }
  public void m_280273_(GuiGraphics g) {}
  public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float tick) {}
  public boolean m_7043_() { return false; }
}
""",
    "net/minecraft/client/multiplayer/ClientPacketListener.java": """
package net.minecraft.client.multiplayer;
import net.minecraft.network.Connection;
public class ClientPacketListener {
  public Connection m_104910_() { return new Connection(); }
}
""",
    "net/minecraft/network/Connection.java": """
package net.minecraft.network;
public class Connection {}
""",
    "net/minecraft/client/Minecraft.java": """
package net.minecraft.client;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
public class Minecraft {
  public static Minecraft m_91087_() { return new Minecraft(); }
  public ClientPacketListener m_91403_() { return new ClientPacketListener(); }
  public void m_91152_(Screen screen) {}
}
""",
    "net/minecraft/network/FriendlyByteBuf.java": """
package net.minecraft.network;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
/** Minimal in-memory buffer for compile stubs + encode/decode self-tests. */
public class FriendlyByteBuf {
  private final List<Object> values = new ArrayList<>();
  private int idx = 0;
  public FriendlyByteBuf m_130070_(String s) { values.add(s == null ? "" : s); return this; }
  public FriendlyByteBuf m_130072_(String s, int max) { return m_130070_(s); }
  public String m_130277_() {
    if (idx >= values.size()) throw new IndexOutOfBoundsException("utf");
    Object v = values.get(idx++);
    return v == null ? "" : String.valueOf(v);
  }
  public String m_130136_(int max) { return m_130277_(); }
  public FriendlyByteBuf m_130130_(int v) { values.add(v); return this; }
  public int m_130242_() {
    if (idx >= values.size()) throw new IndexOutOfBoundsException("varint");
    return ((Number) values.get(idx++)).intValue();
  }
  public boolean isReadable() { return idx < values.size(); }
  public int readableBytes() { return Math.max(0, values.size() - idx); }
  public void writeExtraMarker(String s) { values.add(s); }
}
""",
    "net/minecraft/resources/ResourceLocation.java": """
package net.minecraft.resources;
public class ResourceLocation {
  public ResourceLocation(String ns, String path) {}
}
""",
    "net/minecraftforge/api/distmarker/Dist.java": """
package net.minecraftforge.api.distmarker;
public enum Dist {
  CLIENT, DEDICATED_SERVER;
  public boolean isClient() { return this == CLIENT; }
}
""",
    "net/minecraftforge/fml/loading/FMLEnvironment.java": """
package net.minecraftforge.fml.loading;
import net.minecraftforge.api.distmarker.Dist;
public class FMLEnvironment {
  public static Dist dist = Dist.CLIENT;
}
""",
    "net/minecraftforge/network/NetworkDirection.java": """
package net.minecraftforge.network;
public enum NetworkDirection { PLAY_TO_CLIENT, PLAY_TO_SERVER }
""",
    "net/minecraftforge/network/NetworkEvent.java": """
package net.minecraftforge.network;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
public class NetworkEvent {
  public static class Context {
    public void setPacketHandled(boolean v) {}
    public CompletableFuture<Void> enqueueWork(Runnable r) { r.run(); return CompletableFuture.completedFuture(null); }
  }
}
""",
    "net/minecraftforge/network/NetworkRegistry.java": """
package net.minecraftforge.network;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.simple.SimpleChannel;
public class NetworkRegistry {
  public static Predicate<String> acceptMissingOr(String protocol) { return r -> true; }
  public static SimpleChannel newSimpleChannel(ResourceLocation name, Supplier<String> protocol,
      Predicate<String> clientAcceptedVersions, Predicate<String> serverAcceptedVersions) {
    return new SimpleChannel();
  }
}
""",
    "net/minecraftforge/network/simple/SimpleChannel.java": """
package net.minecraftforge.network.simple;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
public class SimpleChannel {
  public <MSG> MessageBuilder<MSG> messageBuilder(Class<MSG> type, int id, NetworkDirection dir) {
    return new MessageBuilder<>();
  }
  public boolean isRemotePresent(Connection c) { return true; }
  public void sendToServer(Object msg) {}
  public static class MessageBuilder<MSG> {
    public MessageBuilder<MSG> encoder(BiConsumer<MSG, FriendlyByteBuf> enc) { return this; }
    public MessageBuilder<MSG> decoder(Function<FriendlyByteBuf, MSG> dec) { return this; }
    public MessageBuilder<MSG> consumerMainThread(BiConsumer<MSG, Supplier<NetworkEvent.Context>> c) { return this; }
    public void add() {}
  }
}
""",
    "org/apache/logging/log4j/Logger.java": """
package org.apache.logging.log4j;
public interface Logger {
  void info(String msg, Object... args);
  void warn(String msg, Object... args);
  void error(String msg, Object... args);
  void error(String msg, Throwable t);
  void error(String msg, Object arg, Throwable t);
}
""",
    "org/apache/logging/log4j/LogManager.java": """
package org.apache.logging.log4j;
public class LogManager {
  public static Logger getLogger(String name) {
    return new Logger() {
      public void info(String msg, Object... args) {}
      public void warn(String msg, Object... args) {}
      public void error(String msg, Object... args) {}
      public void error(String msg, Throwable t) {}
      public void error(String msg, Object arg, Throwable t) {}
    };
  }
}
""",
}

def main() -> None:
    for rel, body in FILES.items():
        path = ROOT / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(body.lstrip(), encoding="utf-8")
        print("wrote", rel)

if __name__ == "__main__":
    main()
