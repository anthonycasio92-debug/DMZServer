// kubejs/server_scripts/silent_dmz_tp.js
// Silent Building TP — NO commands (commands always spam console via /dmzpoints).
//
// Fabled Building Value Set formula:
//   TP = value-base + (skillLevel - 1) * value-scale
//   base=1, scale=1  =>  TP = Building skill level
// Then Fabled may apply attribute scaling via scaleDynamic("value", ...).
//
// Keep Fabled Building "Value Set" (counts:true) so the skill still levels.
// REMOVE any Building "Command" mechanic that calls dmzpoints/silentdmztp.

console.info("[SilentDMZTP] loading (block-place hook, no commands)...");

var VALUE_BASE = 1.0;
var VALUE_SCALE = 1.0;

function playerUuid(player) {
  try { if (player.uuid) return String(player.uuid); } catch (e) {}
  try { if (player.getUuid) return String(player.getUuid()); } catch (e) {}
  try { if (player.stringUuid) return String(player.stringUuid); } catch (e) {}
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

function buildingTpAmount(bukkitPlayer) {
  try {
    var Fabled = Java.loadClass("studio.magemonkey.fabled.Fabled");
    if (!Fabled.isLoaded()) return 0;
    var data = Fabled.getData(bukkitPlayer);
    if (data == null) return 0;
    var level = Math.max(0, Number(data.getSkillLevel("Building")) || 0);
    if (level < 1) return 0;

    // Exact Fabled parseValues math: base + (level - 1) * scale
    var amount = VALUE_BASE + (level - 1) * VALUE_SCALE;

    // Match attribute scaling when enabled (same key as Value Set: "value")
    try {
      if (Fabled.getSettings().isAttributesEnabled()) {
        amount = Number(data.scaleDynamic(null, "value", amount));
        if (!isFinite(amount) || amount < 0) {
          amount = VALUE_BASE + (level - 1) * VALUE_SCALE;
        }
      }
    } catch (attrErr) {
      // scaleDynamic may require a real EffectComponent — fall back to raw formula
      amount = VALUE_BASE + (level - 1) * VALUE_SCALE;
    }

    return Math.floor(amount);
  } catch (err) {
    console.error("[SilentDMZTP] Building amount failed: " + err);
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

    var bp = getBukkitPlayer(player);
    if (bp == null) return;

    var amount = buildingTpAmount(bp);
    if (amount < 1) return;

    addTrainingPointsSilent(player, amount);
  } catch (err) {
    console.error("[SilentDMZTP] place handler: " + err);
  }
});

console.info("[SilentDMZTP] ready — TP = BuildingLevel (upgradable), zero console/chat.");
