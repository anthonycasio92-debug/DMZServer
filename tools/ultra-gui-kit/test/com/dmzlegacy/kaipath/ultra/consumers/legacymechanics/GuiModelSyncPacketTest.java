package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Offline checks for message 3 ({@link GuiModelSyncPacket}) and the world-sync cache in
 * {@link BodyPartPreview}. Uses the SRG stubs — no game runtime. Invoked from
 * {@code build-kaipath-overlay.sh} via {@link GuiOpenPacketRoundTripTest}.
 */
public final class GuiModelSyncPacketTest {
    private GuiModelSyncPacketTest() {}

    public static void main(String[] args) {
        roundTrip();
        caps();
        stateFromPartsRules();
        applySyncEndToEnd();
        System.out.println("GuiModelSyncPacketTest OK (message 3 + world sync)");
    }

    private static void roundTrip() {
        String parts =
                "{\"schema\":1,\"subject\":\"local\",\"bodyParts\":["
                        + "{\"id\":\"saiyan_tail\",\"source\":\"dmzultra\",\"slot\":\"tail\"}],\"extras\":[]}";
        GuiModelSyncPacket original =
                new GuiModelSyncPacket(parts, GuiModelSyncPacket.REASON_LOGIN);
        FriendlyByteBuf buf = new FriendlyByteBuf();
        GuiModelSyncPacket.encode(original, buf);
        buf.writeExtraMarker("future-field-3"); // trailing junk after field 2 is ignored
        GuiModelSyncPacket decoded = GuiModelSyncPacket.decode(buf);
        assertEq("partsJson", parts, decoded.partsJson());
        assertEq("reason", "login", decoded.reason());

        // A 1-field payload (no reason) decodes with an empty reason — diagnostics only.
        FriendlyByteBuf shortBuf = new FriendlyByteBuf();
        GuiOpenPacket.writeUtf8(shortBuf, parts);
        GuiModelSyncPacket shortDecoded = GuiModelSyncPacket.decode(shortBuf);
        assertEq("short.partsJson", parts, shortDecoded.partsJson());
        assertEq("short.reason", "", shortDecoded.reason());
    }

    private static void caps() {
        GuiModelSyncPacket capped =
                new GuiModelSyncPacket("x".repeat(9000), "y".repeat(100));
        if (capped.partsJson().length() != GuiModelSyncPacket.PARTS_JSON_MAX) {
            fail("partsJson cap expected " + GuiModelSyncPacket.PARTS_JSON_MAX
                    + " got " + capped.partsJson().length());
        }
        if (capped.reason().length() != 32) {
            fail("reason cap expected 32 got " + capped.reason().length());
        }
    }

    private static void stateFromPartsRules() {
        // The saiyan_tail wire id aliases to the tail_fur mesh (DMZUltra fallback catalog
        // is used offline — no DMZUltra runtime needed).
        BodyPartPreview.WorldState alias =
                BodyPartPreview.stateFromParts(
                        "{\"schema\":1,\"subject\":\"local\",\"bodyParts\":["
                                + "{\"id\":\"saiyan_tail\",\"source\":\"dmzultra\",\"slot\":\"tail\"}],"
                                + "\"extras\":[]}",
                        BodyPartPreview::piecesFor);
        assertTrue(alias.meshes().contains("tail_fur"), "saiyan_tail must alias to tail_fur");
        assertTrue(alias.aliasTailOnly(), "an aliased-only tail must be flagged");
        assertTrue(alias.noTail().isEmpty(), "noTail must drop the aliased tail");

        // Defensive single-tail rule: never two tail-slot entries — first wins.
        BodyPartPreview.WorldState twoTails =
                BodyPartPreview.stateFromParts(
                        "{\"bodyParts\":["
                                + "{\"id\":\"saiyan_tail\",\"source\":\"dmzultra\",\"slot\":\"tail\"},"
                                + "{\"id\":\"tail_lion\",\"source\":\"dmzultra\",\"slot\":\"tail\"}]}",
                        BodyPartPreview::piecesFor);
        assertTrue(twoTails.meshes().contains("tail_fur"), "first tail-slot entry must win");
        assertTrue(!twoTails.meshes().contains("tail_lion"),
                "second tail-slot entry must be dropped");

        // A direct (non-aliased) tail is not flagged for the race-tail guard.
        BodyPartPreview.WorldState direct =
                BodyPartPreview.stateFromParts(
                        "{\"bodyParts\":[{\"id\":\"tail_lion\",\"source\":\"dmzultra\",\"slot\":\"tail\"}]}",
                        BodyPartPreview::piecesFor);
        assertTrue(!direct.aliasTailOnly(), "a direct tail mesh must not set aliasTailOnly");

        // Empty bodyParts clears the pieces but keeps the raw payload.
        String emptyJson = "{\"schema\":1,\"subject\":\"local\",\"bodyParts\":[],\"extras\":[]}";
        BodyPartPreview.WorldState cleared =
                BodyPartPreview.stateFromParts(emptyJson, BodyPartPreview::piecesFor);
        assertTrue(cleared.meshes().isEmpty(), "empty bodyParts must clear pieces");
        assertEq("raw payload kept", emptyJson, cleared.partsJson());

        // Missing bodyParts keeps the raw payload and no pieces.
        BodyPartPreview.WorldState noArray =
                BodyPartPreview.stateFromParts(
                        "{\"schema\":1,\"subject\":\"local\",\"extras\":[]}",
                        BodyPartPreview::piecesFor);
        assertTrue(noArray.meshes().isEmpty(), "a payload without bodyParts must add no pieces");

        // Garbage never throws.
        assertTrue(BodyPartPreview.stateFromParts("{not-json", BodyPartPreview::piecesFor)
                        .meshes().isEmpty(),
                "garbage partsJson must degrade to empty");
        assertTrue(BodyPartPreview.stateFromParts(null, BodyPartPreview::piecesFor)
                        .meshes().isEmpty(),
                "null partsJson must degrade to empty");
    }

    private static void applySyncEndToEnd() {
        BodyPartPreview.clearSync();
        Minecraft mc = Minecraft.m_91087_();
        LocalPlayer local = new LocalPlayer();
        mc.f_91074_ = local;

        String tailJson =
                "{\"schema\":1,\"subject\":\"local\",\"bodyParts\":["
                        + "{\"id\":\"saiyan_tail\",\"source\":\"dmzultra\",\"slot\":\"tail\"}],"
                        + "\"extras\":[]}";
        BodyPartPreview.applySync(tailJson, GuiModelSyncPacket.REASON_LOGIN);
        assertEq("cached raw parts", tailJson, BodyPartPreview.localPartsJson());
        assertTrue(BodyPartPreview.meshesFor(local).contains("tail_fur"),
                "subject:local sync must feed meshesFor for the local player");

        // A sync for another player keys by UUID and never touches the local state.
        LocalPlayer other = new LocalPlayer();
        UUID otherId = other.m_20148_();
        String otherJson =
                "{\"schema\":1,\"subject\":\"uuid:" + otherId + "\",\"bodyParts\":["
                        + "{\"id\":\"wings_feather\",\"source\":\"dmzultra\",\"slot\":\"wings\"}],"
                        + "\"extras\":[]}";
        BodyPartPreview.applySync(otherJson, "equip");
        assertTrue(BodyPartPreview.meshesFor(other).contains("wings_feather"),
                "uuid subject sync must key by player uuid");
        assertTrue(BodyPartPreview.meshesFor(local).contains("tail_fur"),
                "a remote sync must not clear the local state");
        assertTrue(BodyPartPreview.meshesFor(new LocalPlayer()).isEmpty(),
                "an unsynced player must get no extra pieces");

        // Server-authoritative replace: an empty bodyParts array clears the cache.
        BodyPartPreview.applySync(
                "{\"schema\":1,\"subject\":\"local\",\"bodyParts\":[],\"extras\":[]}",
                GuiModelSyncPacket.REASON_CLEAR);
        assertTrue(BodyPartPreview.meshesFor(local).isEmpty(),
                "empty bodyParts must clear the world pieces");

        // Disconnect drops everything (contract: state is per-session).
        BodyPartPreview.applySync(tailJson, GuiModelSyncPacket.REASON_LOGIN);
        BodyPartPreview.clearSync();
        assertTrue(BodyPartPreview.meshesFor(local).isEmpty(),
                "clearSync must drop the local state");
        assertTrue(BodyPartPreview.meshesFor(other).isEmpty(),
                "clearSync must drop remote state");
        assertEq("local raw cleared", "", BodyPartPreview.localPartsJson());
    }

    private static void assertTrue(boolean cond, String message) {
        if (!cond) {
            fail(message);
        }
    }

    private static void assertEq(String name, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            fail(name + ": expected=[" + expected + "] actual=[" + actual + "]");
        }
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }
}
