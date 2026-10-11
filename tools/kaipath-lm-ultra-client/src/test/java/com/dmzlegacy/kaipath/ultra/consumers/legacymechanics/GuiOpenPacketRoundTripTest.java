package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Offline encode/decode self-test (no Minecraft runtime). Invoked from {@code build.sh}.
 */
public final class GuiOpenPacketRoundTripTest {
    private GuiOpenPacketRoundTripTest() {}

    public static void main(String[] args) {
        GuiOpenPacket original = new GuiOpenPacket(
                "difficulty",
                "§6Difficulty",
                "tiers",
                "[{\"id\":\"@difficulty:tab:tiers\",\"label\":\"Tiers\"}]",
                "tiers",
                "[\"Line A\",\"Line B\"]",
                "[{\"id\":\"@difficulty:do:activate:1\",\"label\":\"§aT1\",\"x\":20,\"y\":120,\"w\":150,\"h\":18,"
                        + "\"args\":{\"screen\":\"difficulty\",\"page\":\"tiers\"}}]",
                350,
                220,
                "{\"center\":true,\"margin\":6,\"system\":\"difficulty\"}");

        FriendlyByteBuf buf = new FriendlyByteBuf();
        GuiOpenPacket.encode(original, buf);
        // Future field 11 — older clients must ignore
        buf.writeExtraMarker("future-field-11");

        GuiOpenPacket decoded = GuiOpenPacket.decode(buf);
        assertEq("screenId", original.screenId(), decoded.screenId());
        assertEq("title", original.title(), decoded.title());
        assertEq("pageId", original.pageId(), decoded.pageId());
        assertEq("tabsJson", original.tabsJson(), decoded.tabsJson());
        assertEq("activeTab", original.activeTab(), decoded.activeTab());
        assertEq("linesJson", original.linesJson(), decoded.linesJson());
        assertEq("buttonsJson", original.buttonsJson(), decoded.buttonsJson());
        assertEq("panelWidth", String.valueOf(original.panelWidth()), String.valueOf(decoded.panelWidth()));
        assertEq("panelHeight", String.valueOf(original.panelHeight()), String.valueOf(decoded.panelHeight()));
        assertEq("layoutJson", original.layoutJson(), decoded.layoutJson());

        // Truncated packet (only screenId) → defaults
        FriendlyByteBuf shortBuf = new FriendlyByteBuf();
        shortBuf.m_130070_("hub");
        GuiOpenPacket shortDecoded = GuiOpenPacket.decode(shortBuf);
        assertEq("short.screenId", "hub", shortDecoded.screenId());
        assertEq("short.panelW", "350", String.valueOf(shortDecoded.panelWidth()));
        assertEq("short.panelH", "220", String.valueOf(shortDecoded.panelHeight()));
        assertEq("short.buttons", "[]", shortDecoded.buttonsJson());

        // GuiActionPacket verbatim round-trip
        GuiActionPacket action = new GuiActionPacket(
                "hub",
                "@difficulty:do:activate:1",
                "{\"screen\":\"difficulty\",\"page\":\"tiers\"}");
        FriendlyByteBuf abuf = new FriendlyByteBuf();
        GuiActionPacket.encode(action, abuf);
        GuiActionPacket adecoded = GuiActionPacket.decode(abuf);
        assertEq("action.screenId", action.screenId(), adecoded.screenId());
        assertEq("action.actionId", action.actionId(), adecoded.actionId());
        assertEq("action.argsJson", action.argsJson(), adecoded.argsJson());

        // Protocol accept always true + warns on mismatch (no throw)
        if (!LmGuiNetwork.accepts("1") || !LmGuiNetwork.accepts("2") || !LmGuiNetwork.accepts("")) {
            fail("accepts() must tolerate any remote protocol");
        }

        System.out.println("GuiOpenPacketRoundTripTest OK (10 fields + future ignore + action echo)");
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
