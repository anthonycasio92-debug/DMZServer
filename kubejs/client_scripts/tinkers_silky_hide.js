/*
 * DBZ Legacy Reborn - Hide Tinkers Silky Cloth from JEI (client).
 * Pure ASCII.
 *
 * Clients need this file; reconnect or /jei reload after update.
 */

JEIEvents.hideItems(function (event) {
    try {
        event.hide("tconstruct:silky_cloth");
        console.info("[Tinkers Silky] Hid tconstruct:silky_cloth from JEI.");
    } catch (err) {
        console.error("[Tinkers Silky] JEI hide failed: " + err);
    }
});
