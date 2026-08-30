// Sync prestige-locked races to clients for SDU's RaceSelectionScreen padlock UI.
// SDU already has RaceLockClient + padlock overlay; nothing was feeding it.
//
// Channel: dmz_race_locks
// Payload: { locked: string[], required: { [raceId]: number } }
//
// Unlock gate (permanent): Fabled race-unlock skill level >= 1.
// Players spend prestige tokens (from prestiginging) to buy that skill once.
// After purchase they stay unlocked forever — current/active prestige is NOT checked.
//
// required = tooltip only while locked ("Requires Prestige N").
//   Ancient Saiyan ? Prestige 10 · Sento Saiyan ? Prestige 1

var CHANNEL = "dmz_race_locks";
var SYNC_INTERVAL_TICKS = 40;

var RESTRICTED = [
  { id: "ancient_saiyan", skill: "Ancient Saiyan", prestigeLevel: 10 },
  { id: "sento_saiyan", skill: "Sento Saiyan", prestigeLevel: 1 },
];

function playerUuid(player) {
  try {
    if (player.uuid) return String(player.uuid);
  } catch (e0) {}
  try {
    if (player.getUuid) return String(player.getUuid());
  } catch (e1) {}
  try {
    if (player.stringUuid) return String(player.stringUuid);
  } catch (e2) {}
  return null;
}

function getBukkitPlayer(player) {
  try {
    var Bukkit = Java.loadClass("org.bukkit.Bukkit");
    var UUID = Java.loadClass("java.util.UUID");
    var id = playerUuid(player);
    if (!id) return null;
    return Bukkit.getPlayer(UUID.fromString(id));
  } catch (err) {
    return null;
  }
}

function getFabledSkillLevel(bukkitPlayer, skillName) {
  try {
    var Fabled = Java.loadClass("studio.magemonkey.fabled.Fabled");
    if (!Fabled.isLoaded()) return 0;
    var data = Fabled.getData(bukkitPlayer);
    if (data == null) return 0;
    var level = Number(data.getSkillLevel(skillName));
    if (!isFinite(level) || level < 0) return 0;
    return level;
  } catch (err) {
    return 0;
  }
}

function buildPayload(player) {
  var locked = [];
  var required = {};
  var bp = getBukkitPlayer(player);

  for (var i = 0; i < RESTRICTED.length; i++) {
    var entry = RESTRICTED[i];
    var level = bp == null ? 0 : getFabledSkillLevel(bp, entry.skill);
    // Skill owned = permanent unlock. Never gate on current prestige class level.
    if (level >= 1) {
      continue;
    }
    locked.push(entry.id);
    required[entry.id] = entry.prestigeLevel;
  }

  return { locked: locked, required: required };
}

function syncPlayer(player) {
  if (!player) return;
  try {
    player.sendData(CHANNEL, buildPayload(player));
  } catch (err) {
    console.error("[RaceLockGUI] sync failed: " + err);
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

console.info("[RaceLockGUI] server sync ready (" + CHANNEL + ")");