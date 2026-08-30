/*
============================================================
 DBZ Legacy Reborn - Global TP Boost (STUB)
 Trigger ID: 30

 Java owns this path now (LegacyMechanics):
   /progression boost
   /progression boost start <mult> <minutes> [name]
   /progression boost start <encoded> [name]
   /progression boost end

 Do NOT run boost logic here — it would double-apply with
 ProgressionSystem / GlobalTpBoost.
============================================================
*/

var COLOR = "\u00A7";
var TRIGGER_ID = 30;

function trigger(event) {
    if (event == null) return;
    try {
        if (Number(event.id) != Number(TRIGGER_ID)) return;
    } catch (ignored) {
        return;
    }
    try {
        print("[Global TP Boost 30] Stub — use /progression boost (LegacyMechanics Java).");
    } catch (ignored2) {}
    try {
        if (event.player != null && typeof event.player.message == "function") {
            event.player.message(
                COLOR + "e[TP Boost] " + COLOR + "7Use " + COLOR + "f/progression boost"
                + COLOR + "7 (Java)."
            );
        }
    } catch (ignored3) {}
}
