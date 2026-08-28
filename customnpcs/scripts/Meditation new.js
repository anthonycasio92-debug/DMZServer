/*
============================================================
 Meditation new — DISABLED STUB
============================================================
 LegacyMechanics (Forge mod) now owns Meditation trials +
 leveling (MeditationProgression).

 Keeping this CNPC script enabled double-counts progress and
 fights the mod's global trial rotator
 (config/legacymechanics/meditation-trial.json).

 Behavior lives in Java with script parity:
   - 15-minute random trial rotation (avoid immediate repeat)
   - Focus: first 10s of charge, then release & re-charge
   - Wrong-biome warn after 10s (60s hard CD)
   - Same biomes / condition text as the old script

 Staff: /progression meditation next
 Status: /progression meditation

 Do NOT re-enable the tick handlers below unless you disable
 enableMeditation in the Forge mod.
============================================================
*/

function init(e) { /* owned by LegacyMechanics */ }
function tick(e) { /* owned by LegacyMechanics */ }
function damaged(e) { /* owned by LegacyMechanics */ }
function damagedEntity(e) { /* owned by LegacyMechanics */ }
function playerHurtEvent(e) { /* owned by LegacyMechanics */ }
function tickEventPlayerTickEvent(e) { /* owned by LegacyMechanics */ }
function livingHurtEvent(e) { /* owned by LegacyMechanics */ }
