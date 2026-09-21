// Overhaul stock Prestige.initialLevelCap is 50000. LevelingRevamp.json is
// server-only (not synced), so Statistics at prestige 0 shows 50k on clients.
// Pin both Overhaul caps to the 150k server ceiling.

var OVERHAUL_LEVEL_CAP = 150000;

function pinOverhaulCaps() {
  try {
    var Cfg = Java.loadClass("com.dmzrevamp.config.LevelingRevampConfig");
    var cfg = Cfg.get();
    if (!cfg) return false;
    var levels = cfg.levelsAndAttributes;
    var prestige = cfg.Prestige;
    levels.getClass().getField("maxLevel").setInt(levels, OVERHAUL_LEVEL_CAP);
    prestige.getClass().getField("initialLevelCap").setInt(prestige, OVERHAUL_LEVEL_CAP);
    return true;
  } catch (e) {
    return false;
  }
}

pinOverhaulCaps();

ClientEvents.loggedIn(function () {
  pinOverhaulCaps();
});
