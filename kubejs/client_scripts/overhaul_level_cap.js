// Overhaul stock Prestige.initialLevelCap is 50000 and LevelingRevamp.json
// is server-only. Native levelCap = initial + prestige × step, which with a
// 150k maxLevel becomes 100k / 115k / 130k / 145k and ignores breakthroughs.
//
// Pin BOTH initialLevelCap and maxLevel to the player's personal cap
// (100k + 10k×breakthroughs, max 150k) so the native formula stays flat.
// Cap arrives on channel lm_personal_level_cap from the server script.

var CHANNEL = "lm_personal_level_cap";
var OVERHAUL_BASE_CAP = 100000;
var OVERHAUL_ABSOLUTE_CAP = 150000;
var lastCap = OVERHAUL_BASE_CAP;
var pinnedOnce = false;

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

function pinOverhaulCaps(cap) {
  cap = clampCap(cap);
  lastCap = cap;
  try {
    var Cfg = Java.loadClass("com.dmzrevamp.config.LevelingRevampConfig");
    var cfg = Cfg.get();
    if (!cfg) return false;
    var levels = cfg.levelsAndAttributes;
    var prestige = cfg.Prestige;
    levels.getClass().getField("maxLevel").setInt(levels, cap);
    prestige.getClass().getField("initialLevelCap").setInt(prestige, cap);
    if (!pinnedOnce) {
      pinnedOnce = true;
      console.info("[LM] Overhaul client level cap pinned to " + cap);
    }
    return true;
  } catch (e) {
    console.error("[LM] Overhaul client level cap pin failed: " + e);
    return false;
  }
}

pinOverhaulCaps(OVERHAUL_BASE_CAP);

NetworkEvents.dataReceived(CHANNEL, function (event) {
  try {
    var data = event.data;
    var raw = data.cap !== undefined ? data.cap : data.get && data.get("cap");
    pinOverhaulCaps(Number(raw));
  } catch (e) {
    console.error("[LM] personal cap packet failed: " + e);
  }
});

ClientEvents.loggedIn(function () {
  pinOverhaulCaps(lastCap);
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
    pinOverhaulCaps(lastCap);
  } catch (e) {}
});
