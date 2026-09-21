// Server: push personal level cap (100k + 10k×breakthroughs) to clients.
// Overhaul LevelingRevamp.json is server-only; clients default to 50k and
// native levelCap grows with prestige (100k/115k/130k/145k). Client pin
// must use THIS packet — Forge persistentData is not synced.

var CHANNEL = "lm_personal_level_cap";
var SYNC_INTERVAL_TICKS = 40;
var BASE_CAP = 100000;
var ABSOLUTE_CAP = 150000;

var PrestigePointsSystem = null;
try {
  PrestigePointsSystem = Java.loadClass(
    "com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem"
  );
} catch (e) {
  console.warn("[LM] PrestigePointsSystem unavailable for cap sync: " + e);
}

function clampCap(n) {
  if (typeof n !== "number" || !isFinite(n)) {
    return BASE_CAP;
  }
  n = Math.floor(n);
  if (n < BASE_CAP) return BASE_CAP;
  if (n > ABSOLUTE_CAP) return ABSOLUTE_CAP;
  return n;
}

function minecraftPlayer(player) {
  try {
    if (player && player.minecraftPlayer) return player.minecraftPlayer;
  } catch (e0) {}
  return player;
}

function readCap(player) {
  try {
    if (PrestigePointsSystem) {
      var cap = Number(PrestigePointsSystem.effectiveMaxLevel(minecraftPlayer(player)));
      if (isFinite(cap) && cap >= BASE_CAP) {
        return clampCap(cap);
      }
    }
  } catch (e1) {}
  try {
    var data = player.persistentData;
    if (data && data.contains("lm_personal_level_cap")) {
      return clampCap(data.getInt("lm_personal_level_cap"));
    }
  } catch (e2) {}
  return BASE_CAP;
}

function syncPlayer(player) {
  if (!player) return;
  try {
    player.sendData(CHANNEL, { cap: readCap(player) });
  } catch (err) {
    console.error("[LM] personal cap sync failed: " + err);
  }
}

PlayerEvents.loggedIn(function (event) {
  syncPlayer(event.player);
});

PlayerEvents.tick(function (event) {
  try {
    var player = event.player;
    if (!player) return;
    var age = 0;
    try {
      age = Number(player.age);
    } catch (e0) {
      try {
        age = Number(player.tickCount);
      } catch (e1) {
        return;
      }
    }
    if (!isFinite(age) || age % SYNC_INTERVAL_TICKS !== 0) return;
    syncPlayer(player);
  } catch (err) {}
});

console.info("[LM] personal level-cap server sync ready (" + CHANNEL + ")");
