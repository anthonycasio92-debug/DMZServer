// Sync prestige-locked races to clients for SDU's RaceSelectionScreen padlock UI.
//
// Source of truth: config/legacymechanics/race-lock.json (LegacyMechanics RaceLockConfig).
// Edit that file to add races — no mod rebuild. Fallback defaults match Ancient / Sento.
//
// Channel: dmz_race_locks
// Payload: { locked: string[], required: { [raceId]: number } }
//
// Unlock gate (permanent): Fabled race-unlock skill level >= 1.
// required = tooltip only while locked ("Requires Prestige N").

var CHANNEL = "dmz_race_locks";
var SYNC_INTERVAL_TICKS = 40;
var CONFIG_PATH = "config/legacymechanics/race-lock.json";
var CONFIG_RELOAD_MS = 15000;

var DEFAULT_RESTRICTED = [
  { id: "ancient_saiyan", skill: "Ancient Saiyan", prestigeLevel: 10 },
  { id: "sento_saiyan", skill: "Sento Saiyan", prestigeLevel: 1 },
];

var cachedRestricted = DEFAULT_RESTRICTED.slice();
var cachedAt = 0;

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

function loadRestrictedFromConfig() {
  var now = Date.now();
  if (cachedAt > 0 && now - cachedAt < CONFIG_RELOAD_MS) {
    return cachedRestricted;
  }
  cachedAt = now;
  try {
    var Files = Java.loadClass("java.nio.file.Files");
    var Paths = Java.loadClass("java.nio.file.Paths");
    var path = Paths.get(CONFIG_PATH);
    if (!Files.exists(path)) {
      cachedRestricted = DEFAULT_RESTRICTED.slice();
      return cachedRestricted;
    }
    var raw = String(Files.readString(path));
    var parsed = JSON.parse(raw);
    var list = parsed && parsed.restricted ? parsed.restricted : null;
    if (!list || !list.length) {
      cachedRestricted = DEFAULT_RESTRICTED.slice();
      return cachedRestricted;
    }
    var out = [];
    for (var i = 0; i < list.length; i++) {
      var e = list[i];
      if (!e || !e.id) continue;
      var skill = e.fabledSkill || e.skill || e.displayName || String(e.id);
      var tip = Number(e.prestigeTooltip != null ? e.prestigeTooltip : e.prestigeLevel);
      if (!isFinite(tip) || tip < 0) tip = 1;
      out.push({
        id: String(e.id).toLowerCase(),
        skill: String(skill),
        prestigeLevel: tip,
      });
    }
    cachedRestricted = out.length ? out : DEFAULT_RESTRICTED.slice();
  } catch (err) {
    console.warn("[RaceLockGUI] config read failed, using defaults: " + err);
    cachedRestricted = DEFAULT_RESTRICTED.slice();
  }
  return cachedRestricted;
}

function buildPayload(player) {
  var locked = [];
  var required = {};
  var bp = getBukkitPlayer(player);
  var restricted = loadRestrictedFromConfig();

  for (var i = 0; i < restricted.length; i++) {
    var entry = restricted[i];
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

console.info("[RaceLockGUI] server sync ready (" + CHANNEL + ") — config " + CONFIG_PATH);
