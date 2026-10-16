package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import java.util.List;

/**
 * Offline check for {@link BodyPartCatalog}. No Minecraft types.
 */
public final class BodyPartCatalogSelfTest {
    private BodyPartCatalogSelfTest() {}

    public static void main(String[] args) {
        if (BodyPartCatalog.formCount() < 80) {
            fail("catalog should load DMZUltra form keys, got " + BodyPartCatalog.formCount());
        }
        List<BodyPartCatalog.Part> tail =
                BodyPartCatalog.resolve("saiyan", "", "", "", "", true);
        if (tail.size() != 1) {
            fail("saiyan tail should be the only part, got " + tail);
        }
        BodyPartCatalog.Part saiyan = tail.get(0);
        if (!BodyPartCatalog.SAIYAN_TAIL.equals(saiyan.id())
                || !BodyPartCatalog.SOURCE_DMZULTRA.equals(saiyan.source())
                || !BodyPartCatalog.SLOT_TAIL.equals(saiyan.slot())) {
            fail("expected saiyan_tail/dmzultra/tail, got " + saiyan);
        }
        List<BodyPartCatalog.Part> wings =
                BodyPartCatalog.resolve("human", "sageforms", "craneascendant", "", "", false);
        boolean feather = false;
        for (BodyPartCatalog.Part part : wings) {
            if ("wings_feather".equals(part.id()) && BodyPartCatalog.SLOT_WINGS.equals(part.slot())) {
                feather = true;
            }
            if (BodyPartCatalog.SAIYAN_TAIL.equals(part.id())) {
                fail("crane form must not invent a saiyan tail");
            }
        }
        if (!feather) {
            fail("craneascendant should name wings_feather, got " + wings);
        }
        List<BodyPartCatalog.Part> demon =
                BodyPartCatalog.resolve("demon", "abyssforms", "demongod", "", "", false);
        boolean horn = false;
        boolean wing = false;
        boolean demonTail = false;
        for (BodyPartCatalog.Part part : demon) {
            if ("horns_swept".equals(part.id()) && BodyPartCatalog.SLOT_HORNS.equals(part.slot())) {
                horn = true;
            }
            if ("wings_bat_small".equals(part.id()) && BodyPartCatalog.SLOT_WINGS.equals(part.slot())) {
                wing = true;
            }
            if ("tail_spade".equals(part.id()) && BodyPartCatalog.SLOT_TAIL.equals(part.slot())) {
                demonTail = true;
            }
        }
        if (!horn || !wing || !demonTail) {
            fail("demongod should name horns, wings, and a tail, got " + demon);
        }
        List<BodyPartCatalog.Part> stacked =
                BodyPartCatalog.resolve("saiyan", "", "", "sageforms", "craneascendant", true);
        // Stack group is not a saiyan form key; craneascendant is human. The lookup
        // uses the player's race, so this misses. A real stack uses the same race.
        List<BodyPartCatalog.Part> sameRaceStack =
                BodyPartCatalog.resolve("human", "", "", "sageforms", "craneascendant", true);
        boolean stackWing = false;
        boolean stackTail = false;
        for (BodyPartCatalog.Part part : sameRaceStack) {
            if ("wings_feather".equals(part.id())) {
                stackWing = true;
            }
            if (BodyPartCatalog.SAIYAN_TAIL.equals(part.id())) {
                stackTail = true;
            }
        }
        if (!stackWing || !stackTail) {
            fail("stack form wings plus race tail, got " + sameRaceStack + " / " + stacked);
        }
        List<BodyPartCatalog.Part> none =
                BodyPartCatalog.resolve("nope", "missing", "form", null, null, false);
        if (!none.isEmpty()) {
            fail("unknown form should send nothing, got " + none);
        }
        if (!BodyPartCatalog.resolve(null, null, null, null, null, false).isEmpty()) {
            fail("null identity should send nothing");
        }
        System.out.println("BodyPartCatalogSelfTest OK forms=" + BodyPartCatalog.formCount());
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }
}
