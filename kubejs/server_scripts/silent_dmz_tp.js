// kubejs/server_scripts/silent_dmz_tp.js
// Silent Building TP — NO /dmzpoints (that command always spam chat/console).
//
// Award paths (same-tick dedupe so they never double):
//  1) Fabled Building Command → /silentdmztp {TPB}  (proven Block Place trigger)
//  2) Block place hook backup (YAML/Fabled level → same award helper)
//
// Keep Fabled Building "Value Set" (counts:true, key:TPB) so the skill still levels.
// Building.yml Command must be: silentdmztp {TPB}  type: OP

console.info("[SilentDMZTP] loading...");

var VALUE_BASE = 1.0;
var VALUE_SCALE = 1.0;
var SKILL_NAME = "Building";
var MAX_AWARD = 10000;
var DEBUG = false;

// uuid -> gameTime of last successful award (prevents Command + place double-hit)
var lastAwardTick = {};

function asServerPlayer(player) {
  if (!player) return null;
  var ServerPlayer = Java.loadClass("net.minecraft.server.level.ServerPlayer");
  var p = player;
  try {
    if (p.minecraftPlayer) p = p.minecraftPlayer;
  } catch (e0) {}
  try {
    if (p.getMinecraftPlayer) p = p.getMinecraftPlayer();
  } catch (e1) {}
  try {
    if (p.self && p.self !== player) p = p.self;
  } catch (e2) {}
  try {
    // CraftBukkit / Mohist: Bukkit Player → NMS
    if (p.getHandle) p = p.getHandle();
  } catch (e3) {}
  try {
    if (ServerPlayer.class.isInstance(p)) return p;
  } catch (e4) {}
  try {
    if (p instanceof ServerPlayer) return p;
  } catch (e5) {}
  return null;
}

function playerUuidString(player) {
  var sp = asServerPlayer(player);
  if (sp != null) {
    try {
      return String(sp.getUUID());
    } catch (e0) {}
    try {
      return String(sp.getStringUUID());
    } catch (e1) {}
  }
  try {
    if (player.uuid) return String(player.uuid);
  } catch (e2) {}
  try {
    if (player.getUuid) return String(player.getUuid());
  } catch (e3) {}
  try {
    if (player.stringUuid) return String(player.stringUuid);
  } catch (e4) {}
  return null;
}

function getBukkitPlayer(player) {
  try {
    var Bukkit = Java.loadClass("org.bukkit.Bukkit");
    var UUID = Java.loadClass("java.util.UUID");
    var id = playerUuidString(player);
    if (!id) return null;
    return Bukkit.getPlayer(UUID.fromString(id));
  } catch (err) {
    return null;
  }
}

function readBuildingLevelFromYaml(uuid) {
  if (!uuid) return 0;
  try {
    var Files = Java.loadClass("java.nio.file.Files");
    var Paths = Java.loadClass("java.nio.file.Paths");
    var path = Paths.get("plugins/Fabled/players/" + String(uuid).toLowerCase() + ".yml");
    if (!Files.exists(path)) return 0;
    var lines = Files.readAllLines(path);
    var found = false;
    for (var i = 0; i < lines.size(); i++) {
      var trimmed = String(lines.get(i)).trim();
      if (trimmed === SKILL_NAME + ":") {
        found = true;
        continue;
      }
      if (found && trimmed.indexOf("level:") === 0) {
        var level = parseInt(trimmed.replace("level:", "").trim(), 10);
        return isNaN(level) ? 0 : Math.max(0, level);
      }
      if (found && trimmed.length > 0 && trimmed.indexOf(" ") !== 0 && trimmed.indexOf("level:") !== 0) {
        // left the Building block without finding level
        break;
      }
    }
  } catch (err) {
    if (DEBUG) console.error("[SilentDMZTP] yaml read failed: " + err);
  }
  return 0;
}

function buildingLevelFor(player) {
  var bp = getBukkitPlayer(player);
  if (bp != null) {
    try {
      var Fabled = Java.loadClass("studio.magemonkey.fabled.Fabled");
      if (Fabled.isLoaded()) {
        var data = Fabled.getData(bp);
        if (data != null) {
          var level = Math.max(0, Number(data.getSkillLevel(SKILL_NAME)) || 0);
          if (level >= 1) return level;
        }
      }
    } catch (err) {
      if (DEBUG) console.error("[SilentDMZTP] Fabled level failed: " + err);
    }
  }
  return readBuildingLevelFromYaml(playerUuidString(player));
}

function amountFromBuildingLevel(level) {
  if (!(level >= 1)) return 0;
  var amount = VALUE_BASE + (level - 1) * VALUE_SCALE;
  if (!isFinite(amount) || amount < 1) return 0;
  return Math.min(MAX_AWARD, Math.floor(amount));
}

function addTrainingPointsSilent(player, amount) {
  if (!(amount > 0) || !isFinite(amount)) return false;
  amount = Math.min(MAX_AWARD, Math.floor(Number(amount)));
  if (amount < 1) return false;

  var sp = asServerPlayer(player);
  if (sp == null) {
    if (DEBUG) console.warn("[SilentDMZTP] no ServerPlayer for award");
    return false;
  }

  var tick = 0;
  try {
    tick = Number(sp.level.getGameTime());
  } catch (e0) {
    try {
      tick = Number(sp.getLevel().getGameTime());
    } catch (e1) {}
  }
  var id = playerUuidString(sp);
  if (id && lastAwardTick[id] === tick) {
    return false; // already awarded this tick (Command + place)
  }

  try {
    var StatsProvider = Java.loadClass("com.dragonminez.common.stats.StatsProvider");
    var StatsCapability = Java.loadClass("com.dragonminez.common.stats.StatsCapability");
    var NetworkHandler = Java.loadClass("com.dragonminez.common.network.NetworkHandler");
    var ResourceSyncS2C = Java.loadClass("com.dragonminez.common.network.S2C.ResourceSyncS2C");
    var StatsSyncS2C = Java.loadClass("com.dragonminez.common.network.S2C.StatsSyncS2C");

    var lazy = StatsProvider.get(StatsCapability.INSTANCE, sp);
    if (lazy == null) {
      if (DEBUG) console.warn("[SilentDMZTP] StatsProvider null");
      return false;
    }
    var data = lazy.orElse(null);
    if (data == null) {
      if (DEBUG) console.warn("[SilentDMZTP] StatsData missing");
      return false;
    }
    var resources = data.getResources();
    if (resources == null) return false;

    // Prefer event-aware add; fall back to direct set.
    try {
      resources.addTrainingPoints(amount * 1.0, false);
    } catch (addErr) {
      var cur = Number(resources.getTrainingPoints());
      if (!isFinite(cur) || cur < 0) cur = 0;
      resources.setTrainingPoints(cur + amount);
    }

    try {
      NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(sp), sp);
    } catch (syncErr) {}
    try {
      NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(sp), sp);
    } catch (syncErr2) {}

    if (id) lastAwardTick[id] = tick;
    if (DEBUG) console.info("[SilentDMZTP] +" + amount + " TP -> " + id);
    return true;
  } catch (err) {
    console.error("[SilentDMZTP] add TP failed: " + err);
    return false;
  }
}

// /silentdmztp <amount>           — player/OP source (Fabled Building)
// /silentdmztp <player> <amount>  — console / admin
ServerEvents.commandRegistry(function (event) {
  var Commands = event.commands;
  var Arguments = event.arguments;

  event.register(
    Commands.literal("silentdmztp")
      .requires(function (src) {
        return src.hasPermission(2);
      })
      .then(
        Commands.argument("amount", Arguments.FLOAT.create(event)).executes(function (ctx) {
          try {
            var amount = Number(Arguments.FLOAT.getResult(ctx, "amount"));
            var player = ctx.getSource().getPlayerOrException();
            addTrainingPointsSilent(player, amount);
          } catch (err) {
            console.error("[SilentDMZTP] cmd amount: " + err);
          }
          return 1;
        })
      )
      .then(
        Commands.argument("player", Arguments.PLAYER.create(event)).then(
          Commands.argument("amount", Arguments.FLOAT.create(event)).executes(function (ctx) {
            try {
              var target = Arguments.PLAYER.getResult(ctx, "player");
              var amount = Number(Arguments.FLOAT.getResult(ctx, "amount"));
              addTrainingPointsSilent(target, amount);
            } catch (err) {
              console.error("[SilentDMZTP] cmd player+amount: " + err);
            }
            return 1;
          })
        )
      )
  );

  console.info("[SilentDMZTP] registered /silentdmztp (silent, no chat/console spam)");
});

BlockEvents.placed(function (event) {
  try {
    var player = event.player;
    if (!player) return;
    try {
      if (player.level && player.level.clientSide) return;
    } catch (e0) {}

    var level = buildingLevelFor(player);
    var amount = amountFromBuildingLevel(level);
    if (amount < 1) return;

    addTrainingPointsSilent(player, amount);
  } catch (err) {
    console.error("[SilentDMZTP] place handler: " + err);
  }
});

console.info("[SilentDMZTP] ready — Building TP via silent command + place backup");
