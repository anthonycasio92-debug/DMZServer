package com.dbzlegacy.adaptivedifficulty.net.gui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Server half of {@code legacymechanics:gui} / protocol {@code "1"}.
 * KaiPath owns the client consumer. No client screen classes are referenced here.
 */
public final class LmGuiNetwork {
    public static final String CHANNEL_NAMESPACE = "legacymechanics";
    public static final String CHANNEL_PATH = "gui";
    public static final String PROTOCOL = "1";

    /** Forge mod id for KaiPath. */
    public static final String KAIPATH_MOD_ID = "kaipath";
    /** Minimum KaiPath version that speaks {@code legacymechanics:gui}. */
    public static final String KAIPATH_MIN_VERSION = "1.2.5";

    /**
     * Optional channel: clients without KaiPath omit it. Accept missing so those players
     * still join; UltraGui then falls back to CustomNPCs for them.
     */
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CHANNEL_NAMESPACE, CHANNEL_PATH),
            () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL));

    private static boolean registered;
    private static Field connectionField;

    private LmGuiNetwork() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.messageBuilder(GuiOpenPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GuiOpenPacket::encode)
                .decoder(GuiOpenPacket::decode)
                .consumerMainThread(GuiOpenPacket::handle)
                .add();
        CHANNEL.messageBuilder(GuiActionPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GuiActionPacket::encode)
                .decoder(GuiActionPacket::decode)
                .consumerMainThread(GuiActionPacket::handle)
                .add();
    }

    public static void sendOpen(ServerPlayer player, GuiOpenPacket packet) {
        if (player == null || packet == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /**
     * True when the player's connection lists KaiPath at {@link #KAIPATH_MIN_VERSION} or newer.
     * Forge's client handshake may omit version strings ({@code Pair("", "")}); presence alone
     * is accepted in that case. When a version is present it must be {@code >= 1.2.5}.
     */
    public static boolean clientHasKaiPath(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        try {
            Connection connection = connectionOf(player);
            if (connection == null) {
                return false;
            }
            Class<?> hooks = Class.forName("net.minecraftforge.network.NetworkHooks");
            Object data = hooks.getMethod("getConnectionData", Connection.class).invoke(null, connection);
            if (data == null) {
                return false;
            }
            Object modData = data.getClass().getMethod("getModData").invoke(data);
            if (modData instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() == null
                            || !KAIPATH_MOD_ID.equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                        continue;
                    }
                    String version = pairVersion(entry.getValue());
                    if (version == null || version.isBlank()) {
                        // Forge server handshake stores empty versions for client mods.
                        return true;
                    }
                    return isVersionAtLeast(version, KAIPATH_MIN_VERSION);
                }
            }
            // Older ConnectionData path: mod id list only.
            Object modList = data.getClass().getMethod("getModList").invoke(data);
            if (modList instanceof Iterable<?> mods) {
                for (Object mod : mods) {
                    if (mod != null && KAIPATH_MOD_ID.equalsIgnoreCase(String.valueOf(mod))) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Compare dotted numeric versions ({@code 1.2.5}, {@code 1.2.6}, {@code 1.3.0}).
     * Non-numeric suffixes on a segment are ignored after the leading integer.
     * Missing trailing segments are treated as {@code 0}.
     */
    public static boolean isVersionAtLeast(String actual, String minimum) {
        if (actual == null || actual.isBlank() || minimum == null || minimum.isBlank()) {
            return false;
        }
        int[] a = parseVersionParts(actual);
        int[] m = parseVersionParts(minimum);
        int len = Math.max(a.length, m.length);
        for (int i = 0; i < len; i++) {
            int av = i < a.length ? a[i] : 0;
            int mv = i < m.length ? m[i] : 0;
            if (av > mv) {
                return true;
            }
            if (av < mv) {
                return false;
            }
        }
        return true;
    }

    private static int[] parseVersionParts(String version) {
        String cleaned = version.trim();
        int plus = cleaned.indexOf('+');
        if (plus >= 0) {
            cleaned = cleaned.substring(0, plus);
        }
        int dash = cleaned.indexOf('-');
        if (dash >= 0) {
            cleaned = cleaned.substring(0, dash);
        }
        String[] bits = cleaned.split("\\.");
        int[] parts = new int[bits.length];
        for (int i = 0; i < bits.length; i++) {
            parts[i] = leadingInt(bits[i]);
        }
        return parts;
    }

    private static int leadingInt(String segment) {
        if (segment == null || segment.isEmpty()) {
            return 0;
        }
        int n = 0;
        boolean any = false;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (c < '0' || c > '9') {
                break;
            }
            any = true;
            n = n * 10 + (c - '0');
        }
        return any ? n : 0;
    }

    /** {@code Pair<String,String>} is (modName, modVersion); prefer right, then left. */
    private static String pairVersion(Object pair) {
        if (pair == null) {
            return null;
        }
        try {
            Method getRight = pair.getClass().getMethod("getRight");
            Object right = getRight.invoke(pair);
            if (right != null && !String.valueOf(right).isBlank()) {
                return String.valueOf(right);
            }
            Method getLeft = pair.getClass().getMethod("getLeft");
            Object left = getLeft.invoke(pair);
            if (left != null && !String.valueOf(left).isBlank()) {
                // Only treat left as version when it looks like one (starts with a digit).
                String s = String.valueOf(left);
                if (!s.isEmpty() && s.charAt(0) >= '0' && s.charAt(0) <= '9') {
                    return s;
                }
            }
            return right != null ? String.valueOf(right) : null;
        } catch (Throwable ignored) {
            return String.valueOf(pair);
        }
    }

    private static Connection connectionOf(ServerPlayer player) {
        try {
            ServerGamePacketListenerImpl listener = player.f_8906_;
            if (listener == null) {
                return null;
            }
            if (connectionField == null) {
                for (Field field : ServerGamePacketListenerImpl.class.getDeclaredFields()) {
                    if (Connection.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        connectionField = field;
                        break;
                    }
                }
            }
            if (connectionField == null) {
                return null;
            }
            return (Connection) connectionField.get(listener);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
