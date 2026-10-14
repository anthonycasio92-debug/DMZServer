package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.dmzlegacy.kaipath.ultra.consumers.UltraGuiConsumerRegistry;
import com.dmzlegacy.kaipath.ultra.consumers.UltraGuiPartners;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Offline encode/decode self-test (no Minecraft runtime). Invoked from {@code build-kaipath-overlay.sh}.
 */
public final class GuiOpenPacketRoundTripTest {
    private GuiOpenPacketRoundTripTest() {}

    public static void main(String[] args) {
        String inputs =
                "[{\"id\":\"weight_value\",\"type\":\"text\",\"x\":8,\"y\":80,\"w\":200,\"h\":18,"
                        + "\"value\":\"50\",\"placeholder\":\"Weight\",\"maxLength\":8,"
                        + "\"args\":{\"bindInput\":\"weight_value\"}}]";
        GuiOpenPacket original =
                new GuiOpenPacket(
                        "difficulty",
                        "Difficulty",
                        "weights",
                        "[{\"id\":\"@difficulty:tab:weights\",\"label\":\"Weights\"}]",
                        "weights",
                        "[\"Line A\",\"Line B\"]",
                        "[{\"id\":\"@difficulty:weights:confirm\",\"label\":\"Confirm\",\"x\":20,\"y\":120,"
                                + "\"w\":150,\"h\":18,\"style\":\"confirm\","
                                + "\"args\":{\"bindInput\":\"weight_value\",\"screen\":\"difficulty\","
                                + "\"page\":\"weights\"}}]",
                        350,
                        220,
                        "{\"center\":true,\"margin\":6,\"titleAlign\":\"center\",\"system\":\"difficulty\"}",
                        inputs);

        FriendlyByteBuf buf = new FriendlyByteBuf();
        GuiOpenPacket.encode(original, buf);
        // Trailing junk after field 11 must be ignored
        buf.writeExtraMarker("future-field-12");

        GuiOpenPacket decoded = GuiOpenPacket.decode(buf);
        assertEq("screenId", original.screenId(), decoded.screenId());
        assertEq("title", original.title(), decoded.title());
        assertEq("pageId", original.pageId(), decoded.pageId());
        assertEq("tabsJson", original.tabsJson(), decoded.tabsJson());
        assertEq("activeTab", original.activeTab(), decoded.activeTab());
        assertEq("linesJson", original.linesJson(), decoded.linesJson());
        assertEq("buttonsJson", original.buttonsJson(), decoded.buttonsJson());
        assertEq(
                "panelWidth",
                String.valueOf(original.panelWidth()),
                String.valueOf(decoded.panelWidth()));
        assertEq(
                "panelHeight",
                String.valueOf(original.panelHeight()),
                String.valueOf(decoded.panelHeight()));
        assertEq("layoutJson", original.layoutJson(), decoded.layoutJson());
        assertEq("inputsJson", original.inputsJson(), decoded.inputsJson());

        // 10-field packet (no field 11) → inputsJson defaults to []
        GuiOpenPacket ten =
                new GuiOpenPacket(
                        "hub",
                        "Title",
                        "main",
                        "[]",
                        "home",
                        "[]",
                        "[]",
                        350,
                        300,
                        "{\"center\":true}");
        FriendlyByteBuf tenBuf = new FriendlyByteBuf();
        // Encode without field 11: copy encode but stop before inputs
        GuiOpenPacket.writeUtf8(tenBuf, ten.screenId());
        GuiOpenPacket.writeUtf8(tenBuf, ten.title());
        GuiOpenPacket.writeUtf8(tenBuf, ten.pageId());
        GuiOpenPacket.writeUtf8(tenBuf, ten.tabsJson());
        GuiOpenPacket.writeUtf8(tenBuf, ten.activeTab());
        GuiOpenPacket.writeUtf8(tenBuf, ten.linesJson());
        GuiOpenPacket.writeUtf8(tenBuf, ten.buttonsJson());
        tenBuf.m_130130_(ten.panelWidth());
        tenBuf.m_130130_(ten.panelHeight());
        GuiOpenPacket.writeUtf8(tenBuf, ten.layoutJson());
        GuiOpenPacket tenDecoded = GuiOpenPacket.decode(tenBuf);
        assertEq("ten.inputsJson", "[]", tenDecoded.inputsJson());
        assertEq("ten.screenId", "hub", tenDecoded.screenId());

        // Truncated packet (only screenId) → defaults
        FriendlyByteBuf shortBuf = new FriendlyByteBuf();
        GuiOpenPacket.writeUtf8(shortBuf, "hub");
        GuiOpenPacket shortDecoded = GuiOpenPacket.decode(shortBuf);
        assertEq("short.screenId", "hub", shortDecoded.screenId());
        assertEq("short.panelW", "350", String.valueOf(shortDecoded.panelWidth()));
        assertEq("short.panelH", "300", String.valueOf(shortDecoded.panelHeight()));
        assertEq("short.buttons", "[]", shortDecoded.buttonsJson());
        assertEq("short.inputs", "[]", shortDecoded.inputsJson());

        // GuiActionPacket verbatim round-trip (incl. bindInput value)
        GuiActionPacket action =
                new GuiActionPacket(
                        "difficulty",
                        "@difficulty:weights:confirm",
                        "{\"bindInput\":\"weight_value\",\"value\":\"50\"}");
        FriendlyByteBuf abuf = new FriendlyByteBuf();
        GuiActionPacket.encode(action, abuf);
        GuiActionPacket adecoded = GuiActionPacket.decode(abuf);
        assertEq("action.screenId", action.screenId(), adecoded.screenId());
        assertEq("action.actionId", action.actionId(), adecoded.actionId());
        assertEq("action.argsJson", action.argsJson(), adecoded.argsJson());

        if (!LmGuiNetwork.accepts("1") || !LmGuiNetwork.accepts("2") || !LmGuiNetwork.accepts("")) {
            fail("accepts() must tolerate any remote protocol");
        }
        if (!"legacymechanics:gui".equals(LmGuiNetwork.CHANNEL_ID)) {
            fail("CHANNEL_ID expected legacymechanics:gui got " + LmGuiNetwork.CHANNEL_ID);
        }
        if (!UltraGuiConsumerRegistry.ADVERTISED_FEATURES_JSON.contains("textInput")) {
            fail("ADVERTISED_FEATURES_JSON must include textInput");
        }
        for (String feat :
                new String[] {
                    "style",
                    "activeTab",
                    "titleAlign",
                    "playerModel",
                    "textInput",
                    "headerBottom",
                    "accent"
                }) {
            if (!UltraGuiConsumerRegistry.ADVERTISED_FEATURES_JSON.contains(feat)) {
                fail("missing advertise feature: " + feat);
            }
        }

        // Multi-mod partners: LM is one entry; list must stay open for other mods
        if (UltraGuiPartners.DEFAULT_PARTNERS == null
                || UltraGuiPartners.DEFAULT_PARTNERS.length < 1) {
            fail("UltraGuiPartners.DEFAULT_PARTNERS must list at least one partner");
        }
        boolean hasLm = false;
        for (String p : UltraGuiPartners.DEFAULT_PARTNERS) {
            if ("legacymechanics".equals(p)) {
                hasLm = true;
            }
        }
        if (!hasLm) {
            fail("DEFAULT_PARTNERS should still include legacymechanics as a partner example");
        }
        // channel-agnostic: same advertise JSON is not tied to a single mod id string
        if (UltraGuiConsumerRegistry.ADVERTISED_FEATURES_JSON.contains("legacymechanics")) {
            fail("ADVERTISED_FEATURES_JSON must not embed a mod id");
        }

        // fitLabel ASCII patcher must rewrite mojibake ellipsis constants
        try {
            java.nio.file.Path patcher =
                    java.nio.file.Path.of("tools/ultra-gui-kit/patch_fitlabel_ascii.py");
            if (!java.nio.file.Files.isRegularFile(patcher)) {
                // When cwd is the kit dir during some runners
                patcher = java.nio.file.Path.of("patch_fitlabel_ascii.py");
            }
            if (!java.nio.file.Files.isRegularFile(patcher)) {
                fail("missing patch_fitlabel_ascii.py");
            }
        } catch (Throwable t) {
            fail("fitLabel patcher check failed: " + t.getMessage());
        }

        System.out.println(
                "GuiOpenPacketRoundTripTest OK (11 fields + multi-mod partners + advertise)");
    }

    private static void assertEq(String name, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            fail(name + ": expected=[" + expected + "] actual=[" + actual + "]");
        }
    }

    private static void fail(String msg) {
        System.err.println("FAIL: " + msg);
        System.exit(1);
    }
}
