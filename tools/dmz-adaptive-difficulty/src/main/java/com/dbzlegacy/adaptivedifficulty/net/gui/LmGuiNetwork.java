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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Server half of this mod's Ultra GUI channel ({@code <modid>:gui}).
 *
 * <p>Channel namespace comes from {@link AdaptiveDifficultyMod#MOD_ID} via
 * {@link UltraGuiProtocol#channel(String)} — other mods should register the same packet
 * shape on <em>their</em> {@code modid:gui}. KaiPath keys client consumers by channel.
 * No client screen classes are referenced here.
 */
public final class LmGuiNetwork {
    /** This mod's channel namespace (= Forge mod id). Not part of the packet payload. */
    public static final String CHANNEL_NAMESPACE = AdaptiveDifficultyMod.MOD_ID;
    /** @see UltraGuiProtocol#CHANNEL_PATH */
    public static final String CHANNEL_PATH = UltraGuiProtocol.CHANNEL_PATH;
    /**
     * Forge SimpleChannel protocol string shared with KaiPath.
     * Bumping this requires coordinated KaiPath release — mismatched clients drop the channel.
     * Must stay equal to {@link UltraGuiProtocol#PROTOCOL_VERSION}.
     */
    public static final String PROTOCOL_VERSION = "1";

    /** Forge mod id for KaiPath. */
    public static final String KAIPATH_MOD_ID = "kaipath";
    /**
     * Minimum KaiPath version that speaks the Ultra GUI protocol on {@code <modid>:gui}.
     * Full dynamic menus (10-field decode + buttonsJson) require <b>1.3.1+</b>; channel
     * handshake still accepts {@code >= 1.2.5}.
     */
    public static final String KAIPATH_MIN_VERSION = "1.2.5";

    /** First KaiPath release with dynamic Ultra GUI (10 fields + multi-mod registry). */
    public static final String KAIPATH_DYNAMIC_UI_VERSION = "1.3.1";

    /**
     * Current recommended/released KaiPath for Ultra (live). Ships style, activeTab,
     * titleAlign, playerModel, textInput, headerBottom, accent, fixedFooter (+ palette/sound/
     * buttonSkin/frame/pagedBook/portraitFrame). LM {@code footer:true} matches 1.3.17.
     */
    public static final String KAIPATH_RECOMMENDED_VERSION = "1.3.17";

    /**
     * Optional channel: clients without KaiPath omit it. Accept missing so those players
     * still join; UltraGui then falls back to CustomNPCs for them.
     */
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            UltraGuiProtocol.channel(CHANNEL_NAMESPACE),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION));

    private static boolean registered;
    private static Field connectionField;
    /** Players already warned this session for missing style/activeTab on GuiOpen. */
    private static final java.util.Set<java.util.UUID> FEATURE_GAP_WARNED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

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
        String featureSummary = UltraClientFeatures.summarizeForStaff(player);
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] guitest → player={} {} — sending GuiOpenPacket with style=confirm + activeTab=home",
                AdaptiveDifficultyMod.MOD_ID,
                safeName(player),
                featureSummary);
        UltraClientFeatures.warnMissingOptionalFeatures(player, "guitest");
        com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraScreenSpec.of("hub")
                .title("Ultra GUI Test")
                .page("guitest")
                .activeTab("home")
                .tabs("home|Test", "other|Other")
                .line(com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraStyle.LABEL
                        + "Features: "
                        + featureSummary)
                .line(com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraStyle.DIM
                        + "Ping=confirm (green). Active tab should highlight home.")
                .setRow(80)
                .button(
                        "do:guitest_ack",
                        com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraStyle.confirm("Ping server"),
                        "confirm")
                .button(
                        "do:guitest_ack",
                        com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraStyle.danger("Danger look"),
                        "danger")
                .button(
                        "do:guitest_ack",
                        com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraStyle.dim("Dim look"),
                        "dim")
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
                    "[{}] player={} has no KaiPath ≥ {} — Ultra menus require KaiPath "
                            + "(no CNPC fallback)",
                    AdaptiveDifficultyMod.MOD_ID,
                    safeName(player),
                    KAIPATH_MIN_VERSION);
        } else if (kaipath) {
            // Baseline until GuiFeaturesPacket arrives — surface style/activeTab gaps early.
            UltraClientFeatures.warnMissingOptionalFeatures(player, "login");
        }
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        if (!PROTOCOL_VERSION.equals(UltraGuiProtocol.PROTOCOL_VERSION)) {
            throw new IllegalStateException(
                    "LmGuiNetwork.PROTOCOL_VERSION ("
                            + PROTOCOL_VERSION
                            + ") != UltraGuiProtocol.PROTOCOL_VERSION ("
                            + UltraGuiProtocol.PROTOCOL_VERSION
                            + ")");
        }
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] registering channel {}:{} protocol={} "
                        + "msg0=GuiOpenPacket(S→C) msg1=GuiActionPacket(C→S) msg2=GuiFeaturesPacket(C→S)",
                AdaptiveDifficultyMod.MOD_ID,
                CHANNEL_NAMESPACE,
                CHANNEL_PATH,
                PROTOCOL_VERSION);
        CHANNEL.messageBuilder(GuiOpenPacket.class, UltraGuiProtocol.MSG_OPEN, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GuiOpenPacket::encode)
                .decoder(buf -> decodeOpenLogged(buf))
                .consumerMainThread(GuiOpenPacket::handle)
                .add();
        // Network-thread receive → enqueueWork (main thread) for all ServerPlayer access.
        CHANNEL.messageBuilder(
                        GuiActionPacket.class, UltraGuiProtocol.MSG_ACTION, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GuiActionPacket::encode)
                .decoder(buf -> decodeActionLogged(buf))
                .consumerNetworkThread(LmGuiNetwork::handleGuiAction)
                .add();
        CHANNEL.messageBuilder(
                        GuiFeaturesPacket.class, UltraGuiProtocol.MSG_FEATURES, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GuiFeaturesPacket::encode)
                .decoder(GuiFeaturesPacket::decode)
                .consumerNetworkThread(GuiFeaturesPacket::handle)
                .add();
        validateBuilderScreenIdsAtStartup();
        UltraScreenSelfTest.runAtStartup();
        UltraScreenSelfTest.verifyTabStaysOnScreen();
        UltraScreenSelfTest.verifyHubShim();
        UltraScreenSelfTest.verifyMenuLinks();
        UltraScreenSelfTest.verifyStaffGate();
        runPacketRoundTripSelfTest();
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] Ultra GUI bridge ready channel={} protocol={} — KaiPath {}+ dynamic UI "
                        + "(any screenId; no hub shim). Recommended KaiPath {}; "
                        + "optional features via GuiFeaturesPacket "
                        + "(style/activeTab/titleAlign/playerModel; textInput still pending).",
                AdaptiveDifficultyMod.MOD_ID,
                UltraGuiProtocol.channelId(CHANNEL_NAMESPACE),
                PROTOCOL_VERSION,
                KAIPATH_DYNAMIC_UI_VERSION,
                KAIPATH_RECOMMENDED_VERSION);
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

            // style + activeTab must survive encode/decode (client conformance contract).
            String styleFail = roundTripStyleAndActiveTab();
            if (styleFail != null) {
                failures.add(styleFail);
            }

            GuiFeaturesPacket features =
                    new GuiFeaturesPacket("[\"style\",\"activeTab\"]");
            net.minecraft.network.FriendlyByteBuf featBuf =
                    new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            GuiFeaturesPacket.encode(features, featBuf);
            GuiFeaturesPacket featuresBack = GuiFeaturesPacket.decode(featBuf);
            java.util.Set<String> parsed = GuiFeaturesPacket.parseFeatures(featuresBack.featuresJson());
            if (!parsed.contains(UltraClientFeatures.FEATURE_STYLE)
                    || !parsed.contains(UltraClientFeatures.FEATURE_ACTIVE_TAB)) {
                failures.add(
                        "GuiFeaturesPacket round-trip failed featuresJson="
                                + featuresBack.featuresJson()
                                + " parsed="
                                + parsed);
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
                        + "(BUILDER_IDS 10-field + style/activeTab + GuiActionPacket + GuiFeaturesPacket)",
                AdaptiveDifficultyMod.MOD_ID,
                screensTested,
                PROTOCOL_VERSION);
    }

    /**
     * Assert button {@code style} values and {@code activeTab} survive encode→decode.
     *
     * @return null on success, failure message otherwise
     */
    private static String roundTripStyleAndActiveTab() {
        GuiOpenPacket open =
                com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraScreenSpec.of("difficulty")
                        .title("Style probe")
                        .page("tiers")
                        .activeTab("tiers")
                        .tabs("tiers|Tiers", "info|Info")
                        .line("style-activeTab probe")
                        .button("do:ok", "Confirm", UltraStyle.STYLE_CONFIRM)
                        .button("do:bad", "Danger", UltraStyle.STYLE_DANGER)
                        .button("do:off", "Dim", UltraStyle.STYLE_DIM)
                        .button("do:plain", "Normal", UltraStyle.STYLE_NORMAL)
                        .textField("input:probe", "hello", "type…", 16, 220, 200, 18, 32)
                        .toPacket();
        if (!"tiers".equals(open.activeTab())) {
            return "style/activeTab probe: activeTab not set on builder packet";
        }
        String buttons = open.buttonsJson() == null ? "" : open.buttonsJson();
        for (String style : new String[] {"confirm", "danger", "dim", "normal"}) {
            if (!buttons.contains("\"style\":\"" + style + "\"")) {
                return "style/activeTab probe: buttonsJson missing style=" + style;
            }
        }
        String inputs = open.inputsJson() == null ? "" : open.inputsJson();
        if (!inputs.contains("\"type\":\"text\"") || !inputs.contains("input:probe")) {
            return "style/activeTab probe: inputsJson missing text field";
        }
        String mismatch = roundTripOpenFields(open);
        if (mismatch != null) {
            return "style/activeTab probe: " + mismatch;
        }
        net.minecraft.network.FriendlyByteBuf buf =
                new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        GuiOpenPacket.encode(open, buf);
        GuiOpenPacket back = GuiOpenPacket.decode(buf);
        if (!"tiers".equals(back.activeTab())) {
            return "style/activeTab probe: activeTab lost in decode → " + back.activeTab();
        }
        String backButtons = back.buttonsJson() == null ? "" : back.buttonsJson();
        for (String style : new String[] {"confirm", "danger", "dim", "normal"}) {
            if (!backButtons.contains("\"style\":\"" + style + "\"")) {
                return "style/activeTab probe: style=" + style + " lost after decode";
            }
        }
        return null;
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
        if (!java.util.Objects.equals(open.inputsJson(), back.inputsJson())) {
            return "inputsJson mismatch";
        }
        return null;
    }

    public static void sendOpen(ServerPlayer player, GuiOpenPacket packet) {
        if (player == null || packet == null) {
            return;
        }
        GuiOpenPacket wire = applyHubShim(packet);
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] GuiOpenPacket → player={} screenId={} (logical={}) buttons={} panel={}x{} "
                        + "activeTab={} {}",
                AdaptiveDifficultyMod.MOD_ID,
                safeName(player),
                wire.screenId(),
                logicalSystemOf(packet, wire),
                buttonCount(wire),
                wire.panelWidth(),
                wire.panelHeight(),
                wire.activeTab(),
                UltraClientFeatures.summarizeForStaff(player));
        validatePanelBounds(wire);
        if (FEATURE_GAP_WARNED.add(player.m_20148_())) {
            UltraClientFeatures.warnMissingOptionalFeatures(player, "GuiOpenPacket");
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), wire);
    }

    /** Clear per-session feature advertisement (call on logout). */
    public static void clearClientSession(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UltraClientFeatures.clear(player);
        FEATURE_GAP_WARNED.remove(player.m_20148_());
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
                layout,
                packet.inputsJson());
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
            // Stock KaiPath defaults titleAlign to left when the key is missing — always force.
            o.addProperty("titleAlign", UltraStyle.TITLE_ALIGN);
            return o.toString();
        } catch (Throwable ignored) {
            return "{\"system\":\""
                    + system
                    + "\",\"screen\":\""
                    + system
                    + "\",\"center\":true,\"margin\":"
                    + UltraGuiProtocol.DEFAULT_MARGIN
                    + ",\"titleAlign\":\"center\"}";
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
        String version = probeKaiPathVersion(player);
        if (version == null) {
            // null = mod absent; blank = present without version string
            return false;
        }
        if (version.isBlank()) {
            return true;
        }
        return isVersionAtLeast(version, KAIPATH_MIN_VERSION);
    }

    /**
     * KaiPath version from the Forge client mod list, or {@code ""} when present without a
     * version string, or {@code null} when KaiPath is not listed.
     */
    public static String probeKaiPathVersion(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        try {
            Connection connection = connectionOf(player);
            if (connection == null) {
                return null;
            }
            Class<?> hooks = Class.forName("net.minecraftforge.network.NetworkHooks");
            Object data = hooks.getMethod("getConnectionData", Connection.class).invoke(null, connection);
            if (data == null) {
                return null;
            }
            Object modData = data.getClass().getMethod("getModData").invoke(data);
            if (modData instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() == null
                            || !KAIPATH_MOD_ID.equalsIgnoreCase(String.valueOf(entry.getKey()))) {
                        continue;
                    }
                    String version = pairVersion(entry.getValue());
                    return version == null ? "" : version;
                }
            }
            Object modList = data.getClass().getMethod("getModList").invoke(data);
            if (modList instanceof Iterable<?> mods) {
                for (Object mod : mods) {
                    if (mod != null && KAIPATH_MOD_ID.equalsIgnoreCase(String.valueOf(mod))) {
                        return "";
                    }
                }
            }
            return null;
        } catch (Throwable ignored) {
            return null;
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
