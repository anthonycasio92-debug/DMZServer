// Overhaul stock Prestige.initialLevelCap is 50000 and LevelingRevamp.json
// is server-only. Statistics at prestige 0 then shows 50k, so players cannot
// reach LM Need 60k/80k/100k. Pin the floor to 100k.
//
// Native levelCap grows with prestige count (initial + count×maxLevel/10).
// At held 1 that becomes 115k while 0-breakthrough personal cap is 100k.
// Pin BOTH fields to the player's personal cap so every held prestige shows
// 100k + 10k×breakthroughs (max 150k).

var OVERHAUL_BASE_CAP = 100000;
var OVERHAUL_ABSOLUTE_CAP = 150000;

function clampCap(n) {
  if (typeof n !== "number" || !isFinite(n)) {
    return OVERHAUL_BASE_CAP;
  }
  n = Math.floor(n);
  if (n < OVERHAUL_BASE_CAP) {
    return OVERHAUL_BASE_CAP;
  }
  if (n > OVERHAUL_ABSOLUTE_CAP) {
    return OVERHAUL_ABSOLUTE_CAP;
  }
  return n;
}

function readPersonalCap(player) {
  try {
    if (!player || !player.persistentData) {
      return OVERHAUL_BASE_CAP;
    }
    var data = player.persistentData;
    if (data.contains("lm_personal_level_cap")) {
      return clampCap(data.getInt("lm_personal_level_cap"));
    }
  } catch (e) {}
  return OVERHAUL_BASE_CAP;
}

function pinOverhaulCaps(cap) {
  cap = clampCap(cap);
  try {
    var Cfg = Java.loadClass("com.dmzrevamp.config.LevelingRevampConfig");
    var cfg = Cfg.get();
    if (!cfg) return false;
    var levels = cfg.levelsAndAttributes;
    var prestige = cfg.Prestige;
    // Equal max + initial → native levelCap stays flat across prestige count.
    levels.getClass().getField("maxLevel").setInt(levels, cap);
    prestige.getClass().getField("initialLevelCap").setInt(prestige, cap);
    return true;
  } catch (e) {
    return false;
  }
}

pinOverhaulCaps(OVERHAUL_BASE_CAP);

ClientEvents.loggedIn(function (event) {
  pinOverhaulCaps(readPersonalCap(event.player));
});

ClientEvents.tick(function () {
  try {
    var player = Client.player;
    if (!player) {
      return;
    }
    var ticks = player.tickCount || player.age || 0;
    if (ticks % 40 !== 0) {
      return;
    }
    pinOverhaulCaps(readPersonalCap(player));
  } catch (e) {}
});
