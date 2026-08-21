// Sync race locks to clients for SDU's RaceSelectionScreen padlock UI.
// SDU already has RaceLockClient + padlock overlay; nothing was feeding it.
//
// Channel: dmz_race_locks
// Payload: { locked: string[], required: { [raceId]: number } }
//
// Unlock gates:
//   Ancient Saiyan → LuckPerms fabled.skill.ancient-saiyan (NOT prestige level)
//   Sento Saiyan   → Fabled skill level >= 1 (bought with prestige tokens)
//
// required = SDU padlock tooltip number only ("Requires Prestige N" is hardcoded
// in SDU). Ancient still sends a placeholder; real unlock is LP permission.

var CHANNEL = "dmz_race_locks";
var SYNC_INTERVAL_TICKS = 40;

var RESTRICTED = [
  {
    id: "ancient_saiyan",
    unlock: "permission",
    permission: "fabled.skill.ancient-saiyan",
    // SDU tooltip only (hardcoded "Requires Prestige N") — not the unlock check
    prestigeLevel: 10,
  },
  {
    id: "sento_saiyan",
    unlock: "skill",
    skill: "Sento Saiyan",
    prestigeLevel: 1,
  },
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

function hasPermission(bukkitPlayer, node) {
  if (bukkitPlayer == null || !node) return false;
  try {
    return !!bukkitPlayer.hasPermission(String(node));
  } catch (err) {
    return false;
  }
}

function isUnlocked(bukkitPlayer, entry) {
  if (entry.unlock === "permission") {
    return hasPermission(bukkitPlayer, entry.permission);
  }
  if (entry.unlock === "skill") {
    if (bukkitPlayer == null) return false;
    return getFabledSkillLevel(bukkitPlayer, entry.skill) >= 1;
  }
  return false;
}

function buildPayload(player) {
  var locked = [];
  var required = {};
  var bp = getBukkitPlayer(player);

  for (var i = 0; i < RESTRICTED.length; i++) {
    var entry = RESTRICTED[i];
    if (isUnlocked(bp, entry)) {
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

console.info("[RaceLockGUI] server sync ready (" + CHANNEL + ") — Ancient=LP, Sento=skill");
