// Client-pack copy of the Overhaul Statistics / Information cap pin.
// startup_scripts ship with the player pack (same folder as dmzweaponscale.js).
// Dedicated-server kubejs/client_scripts are NOT synced to clients.

var CHANNEL = "lm_personal_level_cap";
var OVERHAUL_BASE_CAP = 100000;
var OVERHAUL_ABSOLUTE_CAP = 150000;
var lastCap = OVERHAUL_BASE_CAP;
var pinnedOnce = false;
var isClient = false;
try {
  isClient = !!(Platform && Platform.isClientEnvironment && Platform.isClientEnvironment());
} catch (e0) {
  isClient = false;
}

function clampCap(n) {
  if (typeof n !== "number" || !isFinite(n)) {
    return OVERHAUL_BASE_CAP;
  }
  n = Math.floor(n);
  if (n < OVERHAUL_BASE_CAP) return OVERHAUL_BASE_CAP;
  if (n > OVERHAUL_ABSOLUTE_CAP) return OVERHAUL_ABSOLUTE_CAP;
  return n;
}

function readCapFromPacket(data) {
  if (!data) return 0;
  var n = 0;
  try {
    if (typeof data.getInt === "function") {
      n = Number(data.getInt("cap"));
      if (isFinite(n) && n >= OVERHAUL_BASE_CAP) return n;
    }
  } catch (e0) {}
  try {
    if (typeof data.getString === "function") {
      n = Number(data.getString("s") || data.getString("cap"));
      if (isFinite(n) && n >= OVERHAUL_BASE_CAP) return n;
    }
  } catch (eStr) {}
  try {
    var v = typeof data.get === "function" ? data.get("cap") : data.cap;
    if (v && typeof v.getAsInt === "function") n = Number(v.getAsInt());
    else if (v && typeof v.asInt === "function") n = Number(v.asInt());
    else n = Number(v);
  } catch (e2) {}
  return n;
}

function pinOverhaulCaps(cap) {
  if (!isClient) return false;
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

if (isClient) {
  pinOverhaulCaps(OVERHAUL_BASE_CAP);

  NetworkEvents.dataReceived(CHANNEL, function (event) {
    try {
      pinOverhaulCaps(readCapFromPacket(event.data));
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
      if (!player) return;
      var ticks = player.tickCount || player.age || 0;
      if (ticks % 40 !== 0) return;
      pinOverhaulCaps(lastCap);
    } catch (e) {}
  });
}
