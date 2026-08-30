/*
============================================================
 Disable End Portals — DISABLED STUB
============================================================
 LegacyMechanics (Forge mod) now owns End portal blocking.

 Feedback is a SCREEN TITLE (not chat). Keeping this CNPC
 script enabled causes duplicate chat spam and false "End
 portals are disabled" popups while lagging / flying.

 Do NOT re-enable BLOCK_* below unless you remove
 LegacyMechanics EndPortalGuard.
============================================================
*/

var BLOCK_END_PORTAL_TRAVEL = false;
var BLOCK_END_GATEWAY_TRAVEL = false;
var BLOCK_ENDER_EYE_ON_FRAMES = false;
var EJECT_FROM_PORTAL_BLOCKS = false;
var SHOW_MESSAGES = false;

function init(e) {
    try {
        if (e != null && e.npc != null) {
            /* no-op */
        }
    } catch (ignored) {}
}

function entityTravelToDimensionEvent(e) { /* owned by LegacyMechanics */ }
function playerInteractEventRightClickBlock(e) { /* owned by LegacyMechanics */ }
function tickEventPlayerTickEvent(e) { /* owned by LegacyMechanics */ }
function tick(e) { /* owned by LegacyMechanics */ }
