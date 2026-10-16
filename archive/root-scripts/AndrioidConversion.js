/*
============================================================
 DBZ Legacy Reborn - Android Conversion (STUB)
 Trigger ID: 45

 Java owns this path now (LegacyMechanics):
   /progression android
   /progression android <player>

 Do NOT run conversion logic here — it would double-apply
 with ProgressionSystem.androidConvert / AndroidConversion.
============================================================
*/

var COLOR = "\u00A7";
var TRIGGER_ID = 45;

function trigger(event) {
    if (event == null) return;
    try {
        if (Number(event.id) != Number(TRIGGER_ID)) return;
    } catch (ignored) {
        return;
    }
    try {
        print("[Android Trigger 45] Stub — use /progression android <player> (LegacyMechanics Java).");
    } catch (ignored2) {}
    try {
        var name = "";
        if (event.arguments != null && event.arguments.length > 0) {
            name = String(event.arguments[0]);
        }
        if (event.player != null && typeof event.player.message == "function") {
            event.player.message(
                COLOR + "e[Android] " + COLOR + "7Use " + COLOR + "f/progression android"
                + (name ? (" " + name) : "") + COLOR + "7 (Java)."
            );
        }
    } catch (ignored3) {}
}
