package com.dbzlegacy.adaptivedifficulty.net.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraScreenIds;
import com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraScreenSelfTest;
import com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraStyle;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
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
    /**
     * Forge SimpleChannel protocol string shared with KaiPath.
     * Bumping this requires coordinated KaiPath release — mismatched clients drop the channel.
     */
    public static final String PROTOCOL_VERSION = "1";

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
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION));

    private static boolean registered;
    private static Field connectionField;

    private LmGuiNetwork() {}

    /**
     * PLAY_TO_SERVER {@link GuiActionPacket} entry on the network thread.
     * Captures the sender, then {@link NetworkEvent.Context#enqueueWork}s main-thread dispatch.
     */
    static void handleGuiAction(GuiActionPacket msg, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        // Diagnostic: first line — if clicks produce nothing here, the client is not sending.
        ServerPlayer earlySender = null;
        NetworkEvent.Context context = null;
        try {
            context = ctx == null ? null : ctx.get();
            earlySender = context == null ? null : context.getSender();
        } catch (Throwable ignored) {
            // fall through to INFO log with unknowns
        }
        String playerName = earlySender == null ? "<null>" : safeName(earlySender);
        if (msg == null) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] GuiActionPacket ← player={} screenId=<null-packet> actionId=<null-packet>",
                    AdaptiveDifficultyMod.MOD_ID,
                    playerName);
            if (context != null) {
                context.setPacketHandled(true);
            }
            return;
        }
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] GuiActionPacket ← player={} screenId={} actionId={}",
                AdaptiveDifficultyMod.MOD_ID,
                playerName,
                msg.screenId(),
                msg.actionId());
        if (msg.actionId() == null || msg.actionId().isBlank()) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] GuiActionPacket empty actionId — full packet screenId={} actionId={} argsJson={} player={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    msg.screenId(),
                    msg.actionId(),
                    msg.argsJson(),
                    playerName);
            if (context != null) {
                context.setPacketHandled(true);
            }
            return;
        }
        if (context == null) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] GuiActionPacket dropped: null NetworkEvent.Context player={} screenId={} actionId={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    playerName,
                    msg.screenId(),
                    msg.actionId());
            return;
        }
        // Must capture before enqueueWork — context sender is only reliable here.
        ServerPlayer sender = earlySender != null ? earlySender : context.getSender();
        context.enqueueWork(() -> GuiActionPacket.dispatchOnMain(sender, msg));
        context.setPacketHandled(true);
    }

    private static String safeName(ServerPlayer player) {
        if (player == null) {
            return "<null>";
        }
        try {
            return player.m_7755_().getString();
        } catch (Throwable ignored) {
            return "<name?>";
        }
    }

    static int buttonCount(GuiOpenPacket packet) {
        if (packet == null) {
            return 0;
        }
        String json = packet.buttonsJson();
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return 0;
        }
        try {
            return JsonParser.parseString(json).getAsJsonArray().size();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /**
     * Diagnostic: send a one-button Ultra menu to {@code player}. Click should produce an INFO
     * {@code GuiActionPacket ←} line; ack is handled by {@code do:guitest_ack}.
     */
    public static void sendGuiTest(ServerPlayer player) {
        if (player == null) {
            return;
        }
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] guitest → sending diagnostic GuiOpenPacket to player={} "
                        + "(KaiPath 1.3.0 ignores buttonsJson — click any hardcoded hub tile)",
                AdaptiveDifficultyMod.MOD_ID,
                safeName(player));
        // Still send a rich packet so wire/logs prove S→C delivery; KaiPath 1.3.0 only reads
        // screenId and opens its hardcoded LmHubScreen. Click Character/Difficulty/etc. — those
        // call sendAction("hub", "<bare>", "{}") and must produce GuiActionPacket ← on the server.
        com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraScreenSpec.of("hub")
                .title("Ultra GUI Test")
                .page("guitest")
                .activeTab("home")
                .tabs("home|Test")
                .line(com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraStyle.LABEL
                        + "KaiPath 1.3.0 shows its hardcoded hub (ignores this packet body).")
                .line(com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraStyle.DIM
                        + "Click Character or Difficulty — server log must show GuiActionPacket ←")
                .setRow(72)
                .button(
                        "do:guitest_ack",
                        com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraStyle.confirm("Ping server"),
                        "confirm")
                .rowGap()
                .footerBackToMain()
                .send(player);
    }

    /**
     * Login probe: whether this connection advertised KaiPath (Ultra path eligibility).
     */
    public static void logClientGuiCapability(ServerPlayer player) {
        if (player == null) {
            return;
        }
        boolean kaipath = clientHasKaiPath(player);
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] login GUI probe player={} kaipath={} channel={}:{} protocol={} guiBackend={}",
                AdaptiveDifficultyMod.MOD_ID,
                safeName(player),
                kaipath,
                CHANNEL_NAMESPACE,
                CHANNEL_PATH,
                PROTOCOL_VERSION,
                com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig.get().guiBackend);
        if (!kaipath
                && com.dbzlegacy.adaptivedifficulty.gui.GuiBackend.fromConfig()
                        == com.dbzlegacy.adaptivedifficulty.gui.GuiBackend.ULTRA) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] player={} has no KaiPath ≥ {} — Ultra GuiOpenPacket will not render; "
                            + "CNPC/chat fallbacks apply where configured",
                    AdaptiveDifficultyMod.MOD_ID,
                    safeName(player),
                    KAIPATH_MIN_VERSION);
        }
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] registering channel {}:{} protocol={} msg0=GuiOpenPacket(S→C) msg1=GuiActionPacket(C→S)",
                AdaptiveDifficultyMod.MOD_ID,
                CHANNEL_NAMESPACE,
                CHANNEL_PATH,
                PROTOCOL_VERSION);
        CHANNEL.messageBuilder(GuiOpenPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GuiOpenPacket::encode)
                .decoder(buf -> decodeOpenLogged(buf))
                .consumerMainThread(GuiOpenPacket::handle)
                .add();
        // Network-thread receive → enqueueWork (main thread) for all ServerPlayer access.
        CHANNEL.messageBuilder(GuiActionPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GuiActionPacket::encode)
                .decoder(buf -> decodeActionLogged(buf))
                .consumerNetworkThread(LmGuiNetwork::handleGuiAction)
                .add();
        validateBuilderScreenIdsAtStartup();
        UltraScreenSelfTest.runAtStartup();
        UltraScreenSelfTest.verifyTabStaysOnScreen();
        UltraScreenSelfTest.verifyHubShim();
        runPacketRoundTripSelfTest();
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] KaiPath bridge ready protocol={} — client should render what the server sends "
                        + "(no hardcoded screen lists on LM). KaiPath 1.3.0 still reads screenId only.",
                AdaptiveDifficultyMod.MOD_ID,
                PROTOCOL_VERSION);
    }

    private static GuiOpenPacket decodeOpenLogged(net.minecraft.network.FriendlyByteBuf buf) {
        try {
            return GuiOpenPacket.decode(buf);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] GuiOpenPacket decode failed protocol={} channel={}:{}",
                    AdaptiveDifficultyMod.MOD_ID,
                    PROTOCOL_VERSION,
                    CHANNEL_NAMESPACE,
                    CHANNEL_PATH,
                    t);
            throw t;
        }
    }

    private static GuiActionPacket decodeActionLogged(net.minecraft.network.FriendlyByteBuf buf) {
        try {
            GuiActionPacket pkt = GuiActionPacket.decode(buf);
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] GuiActionPacket decoded protocol={} screenId={} actionId={} argsJson={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    PROTOCOL_VERSION,
                    pkt == null ? "<null>" : pkt.screenId(),
                    pkt == null ? "<null>" : pkt.actionId(),
                    pkt == null ? "<null>" : pkt.argsJson());
            return pkt;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] GuiActionPacket decode failed protocol={} channel={}:{} — handler will not run",
                    AdaptiveDifficultyMod.MOD_ID,
                    PROTOCOL_VERSION,
                    CHANNEL_NAMESPACE,
                    CHANNEL_PATH,
                    t);
            throw t;
        }
    }

    /**
     * Encode → decode round-trip for every registered screen builder plus a verbatim
     * {@link GuiActionPacket}. Any failure aborts startup — a broken packet format must not
     * reach production.
     */
    static void runPacketRoundTripSelfTest() {
        java.util.List<String> failures = new java.util.ArrayList<>();
        int screensTested = 0;
        try {
            for (String id : UltraScreenIds.BUILDER_IDS) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                java.util.List<GuiOpenPacket> packets =
                        com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraScreenSpec.captureSends(
                                () -> UltraScreenSelfTest.probeBuilderForRoundTrip(id));
                if (packets == null || packets.isEmpty()) {
                    failures.add(id + ": no GuiOpenPacket produced for round-trip");
                    continue;
                }
                for (GuiOpenPacket open : packets) {
                    screensTested++;
                    String mismatch = roundTripOpenFields(open);
                    if (mismatch != null) {
                        failures.add(id + ": " + mismatch);
                    }
                }
            }

            String verbatimAction = "@hub:do:guitest_ack";
            GuiActionPacket action =
                    new GuiActionPacket("hub", verbatimAction, "{\"screen\":\"hub\",\"system\":\"hub\"}");
            net.minecraft.network.FriendlyByteBuf actionBuf =
                    new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            GuiActionPacket.encode(action, actionBuf);
            GuiActionPacket actionBack = GuiActionPacket.decode(actionBuf);
            if (!action.screenId().equals(actionBack.screenId())
                    || !action.actionId().equals(actionBack.actionId())
                    || !action.argsJson().equals(actionBack.argsJson())
                    || !verbatimAction.equals(actionBack.actionId())) {
                failures.add(
                        "GuiActionPacket verbatim actionId round-trip failed original=["
                                + action.screenId()
                                + ","
                                + action.actionId()
                                + ","
                                + action.argsJson()
                                + "] decoded=["
                                + actionBack.screenId()
                                + ","
                                + actionBack.actionId()
                                + ","
                                + actionBack.argsJson()
                                + "]");
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] packet round-trip self-test threw",
                    AdaptiveDifficultyMod.MOD_ID,
                    t);
            throw new IllegalStateException(
                    "LegacyMechanics Ultra GUI packet round-trip self-test failed — aborting startup",
                    t);
        }

        if (!failures.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] GuiOpen/GuiAction round-trip self-test FAILED screensTested={} failures={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    screensTested,
                    failures);
            throw new IllegalStateException(
                    "LegacyMechanics Ultra GUI packet round-trip self-test failed ("
                            + failures.size()
                            + " error(s), screensTested="
                            + screensTested
                            + "): "
                            + failures);
        }
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] packet round-trip self-test OK screensTested={} protocol={} "
                        + "(every BUILDER_IDS GuiOpenPacket 10-field + GuiActionPacket verbatim actionId)",
                AdaptiveDifficultyMod.MOD_ID,
                screensTested,
                PROTOCOL_VERSION);
    }

    /** @return null when all 10 GuiOpen fields survive encode→decode */
    private static String roundTripOpenFields(GuiOpenPacket open) {
        if (open == null) {
            return "null packet";
        }
        net.minecraft.network.FriendlyByteBuf buf =
                new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        GuiOpenPacket.encode(open, buf);
        GuiOpenPacket back = GuiOpenPacket.decode(buf);
        if (!java.util.Objects.equals(open.screenId(), back.screenId())) {
            return "screenId " + open.screenId() + " → " + back.screenId();
        }
        if (!java.util.Objects.equals(open.title(), back.title())) {
            return "title mismatch";
        }
        if (!java.util.Objects.equals(open.pageId(), back.pageId())) {
            return "pageId mismatch";
        }
        if (!java.util.Objects.equals(open.tabsJson(), back.tabsJson())) {
            return "tabsJson mismatch";
        }
        if (!java.util.Objects.equals(open.activeTab(), back.activeTab())) {
            return "activeTab mismatch";
        }
        if (!java.util.Objects.equals(open.linesJson(), back.linesJson())) {
            return "linesJson mismatch";
        }
        if (!java.util.Objects.equals(open.buttonsJson(), back.buttonsJson())) {
            return "buttonsJson mismatch";
        }
        if (open.panelWidth() != back.panelWidth()) {
            return "panelWidth " + open.panelWidth() + " → " + back.panelWidth();
        }
        if (open.panelHeight() != back.panelHeight()) {
            return "panelHeight " + open.panelHeight() + " → " + back.panelHeight();
        }
        if (!java.util.Objects.equals(open.layoutJson(), back.layoutJson())) {
            return "layoutJson mismatch";
        }
        return null;
    }

    public static void sendOpen(ServerPlayer player, GuiOpenPacket packet) {
        if (player == null || packet == null) {
            return;
        }
        GuiOpenPacket wire = applyHubShim(packet);
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] GuiOpenPacket → player={} screenId={} (logical={}) buttons={} panel={}x{}",
                AdaptiveDifficultyMod.MOD_ID,
                safeName(player),
                wire.screenId(),
                logicalSystemOf(packet, wire),
                buttonCount(wire),
                wire.panelWidth(),
                wire.panelHeight());
        validatePanelBounds(wire);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), wire);
    }

    /**
     * KaiPath compatibility: rewrite wire {@code screenId} to {@code hub} when the logical id
     * is not in {@link UltraScreenIds#KAIPATH_REGISTERED_IDS}. Title, tabs, buttons, panel, and
     * layout stay as built; logical system is kept in button {@code args} / {@code layoutJson}.
     */
    public static GuiOpenPacket applyHubShim(GuiOpenPacket packet) {
        if (packet == null) {
            return null;
        }
        String logical = packet.screenId();
        if (!UltraScreenIds.needsHubShim(logical)) {
            return packet;
        }
        String layout = withLogicalSystem(packet.layoutJson(), logical);
        return new GuiOpenPacket(
                "hub",
                packet.title(),
                packet.pageId(),
                packet.tabsJson(),
                packet.activeTab(),
                packet.linesJson(),
                packet.buttonsJson(),
                packet.panelWidth(),
                packet.panelHeight(),
                layout);
    }

    static String withLogicalSystem(String layoutJson, String logical) {
        String system = logical == null || logical.isBlank() ? "hub" : logical.trim().toLowerCase();
        try {
            JsonObject o;
            if (layoutJson == null || layoutJson.isBlank()) {
                o = new JsonObject();
            } else {
                o = JsonParser.parseString(layoutJson).getAsJsonObject();
            }
            o.addProperty(UltraScreenIds.META_SYSTEM, system);
            o.addProperty("screen", system);
            return o.toString();
        } catch (Throwable ignored) {
            return "{\"system\":\""
                    + system
                    + "\",\"screen\":\""
                    + system
                    + "\",\"center\":true,\"margin\":"
                    + UltraStyle.PANEL_MARGIN
                    + "}";
        }
    }

    private static String logicalSystemOf(GuiOpenPacket original, GuiOpenPacket wire) {
        if (original != null
                && original.screenId() != null
                && !original.screenId().isBlank()
                && UltraScreenIds.needsHubShim(original.screenId())) {
            return original.screenId();
        }
        return wire == null ? "?" : wire.screenId();
    }

    /**
     * Startup check: every Ultra builder {@code screenId} must be in the protocol list.
     * Errors abort channel registration so a mis-cataloged screen never ships live.
     */
    static void validateBuilderScreenIdsAtStartup() {
        Set<String> builderIds = new LinkedHashSet<>(UltraScreenIds.BUILDER_IDS);
        int errors = UltraScreenIds.validateAtStartup(builderIds);
        if (errors > 0) {
            throw new IllegalStateException(
                    "LegacyMechanics Ultra screen id catalog invalid ("
                            + errors
                            + " error(s)) — fix UltraScreenIds.PROTOCOL_IDS / BUILDER_IDS");
        }
    }

    /**
     * Every Ultra screen must send fields 8–10 ({@code panelWidth}, {@code panelHeight},
     * {@code layoutJson}). Log when bounds are missing so a fullscreen client is not blamed on
     * an empty server packet. Does not rewrite the packet — KaiPath must honor the values.
     */
    static void validatePanelBounds(GuiOpenPacket packet) {
        if (packet == null) {
            return;
        }
        int w = packet.panelWidth();
        int h = packet.panelHeight();
        String layout = packet.layoutJson();
        if (w <= 0 || h <= 0) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] GuiOpenPacket missing panel bounds screenId={} page panelWidth={} panelHeight={} layoutJson={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    packet.screenId(),
                    w,
                    h,
                    layout == null || layout.isBlank() ? "<empty>" : layout);
        } else if (layout == null || layout.isBlank()) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] GuiOpenPacket missing layoutJson screenId={} panelWidth={} panelHeight={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    packet.screenId(),
                    w,
                    h);
        }
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
                    // Null/empty version ⇒ mod present (Forge often omits client mod versions).
                    // Never call isVersionAtLeast with null — it returns false and would
                    // incorrectly reject KaiPath.
                    if (version == null || version.isBlank()) {
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
