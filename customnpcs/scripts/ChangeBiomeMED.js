/*
============================================================
 Meditation Trial Cycler (STUB → Java)
 Trigger ID: 41

 Forwards to LegacyMechanics MeditationProgression.advanceTrial.
 Prefer: /progression meditation next
 Manual cycle uses 30-minute window (ChangeBiomeMED parity).
============================================================
*/

var TRIGGER_ID = 41;

function init(e) { /* owned by LegacyMechanics */ }

function trigger(e) {
    try {
        if (e == null || Number(e.id) != Number(TRIGGER_ID)) {
            return;
        }
        var Meditation = Java.type(
            "com.dbzlegacy.adaptivedifficulty.progression.skills.MeditationProgression"
        );
        var actor = null;
        try {
            if (e.player != null) actor = e.player.getMCEntity();
        } catch (ignoredMc) {}
        var msg = Meditation.advanceTrial(actor);
        if (msg && e.player != null && typeof e.player.message === "function") {
            e.player.message(String(msg));
        }
    } catch (err) {
        try {
            print("[Meditation 41] Java forward failed: " + err);
        } catch (ignoredPrint) {}
        try {
            if (e.player != null) {
                e.player.message(
                    "\u00A75[Meditation] \u00A77Use \u00A7e/progression meditation next\u00A77 (staff)."
                );
            }
        } catch (ignoredMsg) {}
    }
}

function interact(e) { /* owned by LegacyMechanics */ }
