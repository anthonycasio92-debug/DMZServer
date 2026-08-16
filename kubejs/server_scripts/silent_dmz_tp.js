// kubejs/server_scripts/silent_dmz_tp.js
// DISABLED — Building TP is awarded by CNPC Forge script:
//   AdventureWorld/customnpcs/scripts/ecmascript/Building TP Skill.js
//   (blockEventEntityPlaceEvent + StatsProvider, same pattern as Farming TP Skill.js)
//
// Fabled Building.yml must NOT call /silentdmztp or /dmzpoints (Command removed).
// Keep Value Set (counts:true, key:TPB) so the skill still levels on place.
//
// This file stays so old reloads do not resurrect a broken handler.
// KubeJS Rhino cannot call StatsProvider.get(...) reliably (overload error).

console.info(
    "[SilentDMZTP] disabled — Building TP handled by CNPC Building TP Skill.js"
);
