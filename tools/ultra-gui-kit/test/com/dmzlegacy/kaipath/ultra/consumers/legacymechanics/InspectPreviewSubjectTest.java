package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

/** Layout subject tokens. No Minecraft classes. */
public final class InspectPreviewSubjectTest {
    private InspectPreviewSubjectTest() {}

    public static void main(String[] args) {
        String uuid = "11111111-1111-1111-1111-111111111111";
        String top =
                InspectPreviewSubjects.tokenFromLayout(
                        "{\"playerModel\":{\"subject\":\"uuid:"
                                + uuid
                                + "\",\"parts\":{\"subject\":\"local\"}}}");
        check("uuid:" + uuid, top, "top-level subject wins over parts.subject");

        String nested =
                InspectPreviewSubjects.tokenFromLayout(
                        "{\"playerModel\":{\"parts\":{\"subject\":\"uuid:" + uuid + "\"}}}");
        check("uuid:" + uuid, nested, "parts.subject is used when the top-level key is absent");

        String bare =
                InspectPreviewSubjects.tokenFromLayout(
                        "{\"playerModel\":{\"subject\":\"" + uuid + "\"}}");
        check(uuid, bare, "a bare player uuid is a subject");
        if (!uuid.equals(String.valueOf(InspectPreviewSubjects.playerId(bare)))) {
            throw new AssertionError("bare uuid did not parse");
        }
        if (!uuid.equals(String.valueOf(InspectPreviewSubjects.playerId("uuid:" + uuid)))) {
            throw new AssertionError("uuid: prefix did not parse");
        }
        if (InspectPreviewSubjects.playerId("local") != null) {
            throw new AssertionError("local must mean the viewing player");
        }

        String local =
                InspectPreviewSubjects.tokenFromLayout(
                        "{\"playerModel\":{\"x\":1,\"subject\":\"local\"}}");
        check("local", local, "local stays local");

        String missing = InspectPreviewSubjects.tokenFromLayout("{\"screen\":\"character\"}");
        check("local", missing, "menus without a model stay local");

        String blank = InspectPreviewSubjects.tokenFromLayout("");
        check("local", blank, "blank layout stays local");

        System.out.println("InspectPreviewSubjectTest ok");
    }

    private static void check(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected [" + expected + "] got [" + actual + "]");
        }
    }
}
