package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Set;

/** Offline checks for {@link BodyPartWire}. No Minecraft runtime. */
public final class BodyPartWireTest {
    private BodyPartWireTest() {}

    public static void main(String[] args) {
        JsonArray wire =
                JsonParser.parseString(
                                "["
                                        + "{\"id\":\"saiyan_tail\",\"source\":\"dmzultra\",\"slot\":\"tail\"},"
                                        + "{\"id\":\"wings_feather\",\"source\":\"dmzultra\",\"slot\":\"wings\"},"
                                        + "{\"id\":\"not_a_part\",\"source\":\"dmzultra\",\"slot\":\"tail\"},"
                                        + "{\"id\":\"shop_wing\",\"source\":\"myshop\",\"slot\":\"wings\"},"
                                        + "{\"slot\":\"tail\"},"
                                        + "\"nope\""
                                        + "]")
                        .getAsJsonArray();
        List<BodyPartWire.Entry> entries = BodyPartWire.parse(wire);
        if (entries.size() != 4) {
            fail("bad elements must be skipped, got " + entries.size());
        }
        List<BodyPartWire.Model> models =
                BodyPartWire.resolve(
                        entries,
                        source -> {
                            if ("dmzultra".equals(source)) {
                                return BodyPartWire.DMZULTRA_PIECES;
                            }
                            if ("myshop".equals(source)) {
                                return Set.of("shop_wing");
                            }
                            return Set.of();
                        });
        boolean tail = false;
        boolean wings = false;
        boolean shop = false;
        for (BodyPartWire.Model model : models) {
            if ("saiyan_tail".equals(model.id())
                    && "tail_fur".equals(model.mesh())
                    && model.alias()
                    && "tail".equals(model.slot())) {
                tail = true;
            }
            if ("wings_feather".equals(model.mesh()) && "wings".equals(model.slot()) && !model.alias()) {
                wings = true;
            }
            if ("not_a_part".equals(model.id())) {
                fail("unknown id must be skipped");
            }
            if ("shop_wing".equals(model.id()) && "myshop".equals(model.source())) {
                shop = true;
            }
        }
        if (!tail || !wings || !shop) {
            fail("expected tail alias, wings, and myshop piece, got " + models);
        }
        List<BodyPartWire.Model> absent =
                BodyPartWire.resolve(entries, source -> Set.of());
        if (!absent.isEmpty()) {
            fail("missing catalogs must attach nothing, got " + absent);
        }
        if (!BodyPartWire.resolve(BodyPartWire.parse(null), source -> BodyPartWire.DMZULTRA_PIECES)
                .isEmpty()) {
            fail("null array must parse empty");
        }
        if (!BodyPartWire.parse(JsonParser.parseString("[]").getAsJsonArray()).isEmpty()) {
            fail("empty array stays empty");
        }
        System.out.println("BodyPartWireTest OK");
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }
}
