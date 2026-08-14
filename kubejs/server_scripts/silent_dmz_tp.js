// kubejs/server_scripts/silent_dmz_tp.js
// Silent Building TP (no chat, no console).
//
// Fabled Building used to run /dmzpoints add which ALWAYS prints feedback
// to the command source (player chat for OP, console for Console).
//
// This script awards the same TP on block place with zero feedback.
// Disable the Fabled Building "Command" mechanic (keep Value Set for leveling).
//
// Formula (matches Fabled Value Set base 1 / scale 1):
//   TP = 1 + (BuildingLevel - 1) = BuildingLevel   (requires level >= 1)

console.info("[SilentDMZTP] loading...");

function playerUuid(player) {
  try {
    if (player.uuid) return String(player.uuid);
  } catch (e) {}
  try {
    if (player.getUuid) return String(player.getUuid());
  } catch (e) {}
  try {
    if (player.stringUuid) return String(player.stringUuid);
  } catch (e) {}
  return null;
}

function getBukkitPlayer(player) {
  try {
    var Bukkit = Java.loadClass("org.bukkit.Bukkit");
    var id = playerUuid(player);
    if (!id) return null;
    var UUID = Java.loadClass("java.util.UUID");
    return Bukkit.getPlayer(UUID.fromString(id));
  } catch (err) {
    return null;
  }
}

function getBuildingLevel(player) {
  try {
    var bp = getBukkitPlayer(player);
    if (bp == null) return 0;
    var Fabled = Java.loadClass("studio.magemonkey.fabled.Fabled");
    if (!Fabled.isLoaded()) return 0;
    var data = Fabled.getData(bp);
    if (data == null) return 0;
    return Math.max(0, Number(data.getSkillLevel("Building")) || 0);
  } catch (err) {
    console.error("[SilentDMZTP] Fabled level read failed: " + err);
    return 0;
  }
}

function addTrainingPointsSilent(player, amount) {
  if (!player || !(amount > 0) || !isFinite(amount)) return false;
  try {
    var StatsProvider = Java.loadClass("com.dragonminez.common.stats.StatsProvider");
    var StatsCapability = Java.loadClass("com.dragonminez.common.stats.StatsCapability");
    var NetworkHandler = Java.loadClass("com.dragonminez.common.network.NetworkHandler");
    var ResourceSyncS2C = Java.loadClass("com.dragonminez.common.network.S2C.ResourceSyncS2C");

    // KubeJS player works with StatsProvider on this pack (see dmz_bridge_example.js).
    var lazy = StatsProvider.get(StatsCapability.INSTANCE, player);
    if (lazy == null) return false;
    var data = lazy.orElse(null);
    if (data == null) return false;
    var resources = data.getResources();
    if (resources == null) return false;

    var cur = Number(resources.getTrainingPoints());
    if (!isFinite(cur) || cur < 0) cur = 0;
    var next = cur + amount;
    if (!isFinite(next) || next < cur) return false;

    resources.setTrainingPoints(next);
    try {
      NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(player), player);
    } catch (syncErr) {}
    return true;
  } catch (err) {
    console.error("[SilentDMZTP] add TP failed: " + err);
    return false;
  }
}

BlockEvents.placed(function (event) {
  try {
    var player = event.player;
    if (!player) return;
    try {
      if (player.level && player.level.clientSide) return;
    } catch (e0) {}

    var level = getBuildingLevel(player);
    if (level < 1) return;

    var amount = 1 + Math.max(0, level - 1);
    addTrainingPointsSilent(player, amount);
  } catch (err) {
    console.error("[SilentDMZTP] place handler: " + err);
  }
});

console.info("[SilentDMZTP] Block place Building TP hook ready (silent).");
