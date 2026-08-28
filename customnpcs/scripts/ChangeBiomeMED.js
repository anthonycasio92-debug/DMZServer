/*
============================================================
 Meditation Trial Cycler — DISABLED STUB
============================================================
 LegacyMechanics owns global meditation trial rotation.

 Use: /progression meditation next
 (or wait for the automatic 15-minute random rotate)

 Do NOT re-enable this trigger while enableMeditation is on
 in the Forge mod — it wrote different storage keys / duration
 than the Java rotator (config/legacymechanics/meditation-trial.json).
============================================================
*/

var TRIGGER_ID = 41;

function init(e) { /* owned by LegacyMechanics */ }
function trigger(e) {
    try {
        if (e == null || e.id != TRIGGER_ID) {
            return;
        }
        var player = e.player;
        if (player != null) {
            player.message(
                "\u00A75[Meditation] \u00A77Trial cycling is owned by LegacyMechanics."
            );
            player.message(
                "\u00A77Use \u00A7e/progression meditation next\u00A77 (staff)."
            );
        }
    } catch (ignored) {}
}
function interact(e) { /* owned by LegacyMechanics */ }
