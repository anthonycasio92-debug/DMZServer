/*
============================================================
 Potential — DISABLED STUB
============================================================
 LegacyMechanics (Forge mod) now owns Potential Unlock
 leveling (PotentialProgression).

 Keeping this CNPC script enabled double-counts PvP progress
 and fights the mod's movement / method-streak / soft-cap
 gates (stored in player LM NBT / CNPC migrator keys).

 Behavior lives in Java with script parity:
   - PvP hit 3pts · ki hit 3pts · getting hit 1 · blocking 2
   - Move ≥2 blocks (5s window) or progress pauses (with tip)
   - Same-method streak max 5 then switch methods
   - Gravity / effective weight / Prestige multipliers (cap 30)
   - Soft-cap at 10 (Guru) · hard max 30
   - Mentor TP for higher-level partners

 Do NOT re-enable the handlers below unless you disable
 enablePotential in the Forge mod.
============================================================
*/

function init(e) { /* owned by LegacyMechanics */ }
function damaged(e) { /* owned by LegacyMechanics */ }
function damagedEntity(e) { /* owned by LegacyMechanics */ }
function playerHurtEvent(e) { /* owned by LegacyMechanics */ }
function livingHurtEvent(e) { /* owned by LegacyMechanics */ }
