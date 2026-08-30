/*
============================================================
 DBZ Legacy Reborn - TP Boost End (STUB)
 Trigger ID: 31

 Java owns this path now (LegacyMechanics):
   /progression boost end

 Do NOT run end logic here — GlobalTpBoost.endBoost owns it.
============================================================
*/

var COLOR = "\u00A7";
var TRIGGER_ID = 31;

function trigger(event) {
    if (event == null) return;
    try {
        if (Number(event.id) != Number(TRIGGER_ID)) return;
    } catch (ignored) {
        return;
    }
    try {
        print("[TP Boost End 31] Stub — use /progression boost end (LegacyMechanics Java).");
    } catch (ignored2) {}
    try {
        if (event.player != null && typeof event.player.message == "function") {
            event.player.message(
                COLOR + "e[TP Boost] " + COLOR + "7Use " + COLOR + "f/progression boost end"
                + COLOR + "7 (Java)."
            );
        }
    } catch (ignored3) {}
}
