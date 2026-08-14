// kubejs/server_scripts/silent_dmz_tp.js
// Silent DMZ Training Point grant for Fabled Building.
//
// Fabled Building computes TP via Value Set:
//   value = value-base + (skillLevel - 1) * value-scale
//   (base 1, scale 1 => TP equals Building skill level)
// Then attribute scaling may apply via Fabled scaleDynamic.
//
// Do NOT reimplement that math here — Fabled already puts the final
// amount in {TPB}. This command only applies it quietly.
//
// Fabled Building Command (Console):
//   silentdmztp {player} {TPB}
//
// /dmzpoints always prints to the command source (chat or console).
// This command intentionally sends no feedback.

console.info("[SilentDMZTP] loading...");

function resolveNmsFromBukkit(bukkitPlayer) {
  if (!bukkitPlayer) return null;
  try {
    if (typeof bukkitPlayer.getHandle === "function") {
      return bukkitPlayer.getHandle();
    }
  } catch (e0) {}
  try {
    // Mohist / CraftPlayer field fallbacks
    var cls = bukkitPlayer.getClass();
    var names = ["entity", "handle", "nmsEntity"];
    for (var i = 0; i < names.length; i++) {
      try {
        var f = cls.getDeclaredField(names[i]);
        f.setAccessible(true);
        var v = f.get(bukkitPlayer);
        if (v) return v;
      } catch (e1) {}
    }
  } catch (e2) {}
  return null;
}

function addTrainingPointsSilentNms(nmsPlayer, amount) {
  if (!nmsPlayer || !(amount > 0) || !isFinite(amount)) return false;
  try {
    var StatsProvider = Java.loadClass("com.dragonminez.common.stats.StatsProvider");
    var StatsCapability = Java.loadClass("com.dragonminez.common.stats.StatsCapability");
    var NetworkHandler = Java.loadClass("com.dragonminez.common.network.NetworkHandler");
    var ResourceSyncS2C = Java.loadClass("com.dragonminez.common.network.S2C.ResourceSyncS2C");

    var lazy = StatsProvider.get(StatsCapability.INSTANCE, nmsPlayer);
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
      NetworkHandler.sendToTrackingEntityAndSelf(new ResourceSyncS2C(nmsPlayer), nmsPlayer);
    } catch (syncErr) {}
    return true;
  } catch (err) {
    console.error("[SilentDMZTP] add TP failed: " + err);
    return false;
  }
}

ServerEvents.commandRegistry(function (event) {
  var Commands = event.commands;
  var Arguments = event.arguments;

  event.register(
    Commands.literal("silentdmztp")
      .requires(function (src) {
        return src.hasPermission(2);
      })
      .then(
        Commands.argument("player", Arguments.PLAYER.create(event)).then(
          Commands.argument("amount", Arguments.FLOAT.create(event)).executes(function (ctx) {
            try {
              var player = Arguments.PLAYER.getResult(ctx, "player");
              var amount = Number(Arguments.FLOAT.getResult(ctx, "amount"));
              if (!player || !(amount > 0) || !isFinite(amount)) {
                return 0;
              }

              // Arguments.PLAYER is a Bukkit Player on Mohist/Paper bridges.
              var nms = resolveNmsFromBukkit(player);
              if (!nms) {
                // Some KubeJS builds already wrap ServerPlayer
                nms = player.minecraftPlayer || player.minecraftEntity || player;
              }
              var ok = addTrainingPointsSilentNms(nms, amount);
              // No src.tell / sendSuccess — silent on purpose.
              return ok ? 1 : 0;
            } catch (err) {
              console.error("[SilentDMZTP] command error: " + err);
              return 0;
            }
          })
        )
      )
  );

  console.info("[SilentDMZTP] registered /silentdmztp <player> <amount> (silent, scales via Fabled {TPB})");
});
