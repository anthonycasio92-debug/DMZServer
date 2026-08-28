/*
============================================================
 End Dimension Strength ? DISABLED STUB
============================================================
 LegacyMechanics (Forge mod) now owns End Dimension Strength
 (EndDimensionStrength.java ? script v2.12.0 parity).

 Keeping this CNPC script enabled double-applies DEF mitigation,
 kill-TP settle, natural spawn, and ki attacks ? and fights the
 mod's single-dragon + EndDragonFight-linked spawn path.

 Behavior lives in Java:
   - Scale dragon DEF/HP from strongest End player (~200?300 hits)
   - /enddragon · /cleardragons (formerly triggers 50/51)
   - Natural respawn every 5 minutes when none alive
   - Extra DMZ ki beam/blast attacks
   - Egg reward + crystal / podium egg clear
   - Single living dragon + End ki_laser/ki_blast hygiene

 Full script backup: uploads/scripts/End Dimension Strength.js
   and uploads/live-scripts-2026-08-27/ecmascript/

 Do NOT re-enable the handlers below unless you disable
 enableEndDimensionStrength in the Forge mod.
============================================================
*/

function init(e) { /* owned by LegacyMechanics */ }
function tick(e) { /* owned by LegacyMechanics */ }
function kill(e) { /* owned by LegacyMechanics */ }
function damagedEntity(e) { /* owned by LegacyMechanics */ }
function trigger(e) { /* owned by LegacyMechanics ? use /enddragon */ }
function playerHurtEvent(e) { /* owned by LegacyMechanics */ }
