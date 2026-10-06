// Client: apply dmz_race_locks. The server now sends an empty list so no race is padlocked.

var CHANNEL = "dmz_race_locks";

function asObject(tag) {
  if (!tag) return {};
  // Already a plain object
  if (typeof tag === "object" && !tag.get && !tag.keySet) return tag;
  var out = {};
  try {
    if (tag.keySet) {
      var it = tag.keySet().iterator();
      while (it.hasNext()) {
        var k = String(it.next());
        out[k] = tag.get(k);
      }
      return out;
    }
  } catch (e0) {}
  try {
    if (tag.getAllKeys) {
      var keys = tag.getAllKeys();
      var kit = keys.iterator();
      while (kit.hasNext()) {
        var k2 = String(kit.next());
        out[k2] = tag.get(k2);
      }
      return out;
    }
  } catch (e1) {}
  return out;
}

function asStringList(value) {
  var out = [];
  if (!value) return out;
  if (Array.isArray(value)) {
    for (var i = 0; i < value.length; i++) out.push(String(value[i]));
    return out;
  }
  try {
    if (value.size) {
      for (var n = 0; n < value.size(); n++) out.push(String(value.get(n)));
      return out;
    }
  } catch (e2) {}
  try {
    if (value.length !== undefined) {
      for (var j = 0; j < value.length; j++) out.push(String(value[j]));
    }
  } catch (e3) {}
  return out;
}

NetworkEvents.dataReceived(CHANNEL, function (event) {
  try {
    var RaceLockClient = Java.loadClass(
      "net.shurui.dev.sdu.race.RaceLockClient"
    );
    var HashMap = Java.loadClass("java.util.HashMap");
    var HashSet = Java.loadClass("java.util.HashSet");
    var Integer = Java.loadClass("java.lang.Integer");

    var data = event.data;
    var required = new HashMap();
    var locked = new HashSet();

    var reqObj = asObject(data.required !== undefined ? data.required : data.get && data.get("required"));
    var reqKeys = Object.keys(reqObj);
    for (var i = 0; i < reqKeys.length; i++) {
      var raceId = String(reqKeys[i]).toLowerCase();
      var lvl = Number(reqObj[reqKeys[i]]);
      if (!isFinite(lvl) || lvl < 1) lvl = 1;
      required.put(raceId, Integer.valueOf(Math.floor(lvl)));
    }

    var lockedRaw =
      data.locked !== undefined
        ? data.locked
        : data.get && data.get("locked");
    var lockedList = asStringList(lockedRaw);
    for (var j = 0; j < lockedList.length; j++) {
      locked.add(String(lockedList[j]).toLowerCase());
    }

    RaceLockClient.apply(required, locked);
  } catch (err) {
    console.error("[RaceLockGUI] client apply failed: " + err);
  }
});

console.info("[RaceLockGUI] client listener ready (" + CHANNEL + ")");
