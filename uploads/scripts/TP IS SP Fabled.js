// ============================================================
// DMZ Training Points <-> Fabled Skill Points
// CNPC Global Player Tick Script
//
// DMZ TP is authoritative. Fabled SP only *displays* available TP
// (capped at Integer.MAX_VALUE).
//
// DO NOT use getInvestedSkillPoints() for economy math — it is an
// int sum and overflows once Prestige costs exceed ~2.1B total
// (Ancient 2B + Mutant 1B, etc.). That overflow caused:
//   - purchases not debiting TP
//   - refunds looking like huge "spends" that wiped TP
//
// Instead track each skill's level and apply getCost(level) with
// JS numbers (safe well past Prestige costs).
// ============================================================

var TICK_INTERVAL = 5;
var DEBUG = false;
var MAX_FABLED_SP = 2147483647;

var KEY_INITIALIZED = "dmz_fabled_sp_initialized";
var KEY_LAST_LEVELS = "dmz_fabled_sp_skill_levels";

function tick(event) {
    try {
        var player = event.player;
        if (player == null) return;

        var temp = player.getTempdata();
        var tickKey = "dmz_fabled_sp_sync_tick";
        var tickCount = temp.get(tickKey);
        if (tickCount == null) tickCount = 0;
        tickCount = parseInt("" + tickCount, 10) + 1;
        if (isNaN(tickCount)) tickCount = 1;

        if (tickCount < TICK_INTERVAL) {
            temp.put(tickKey, "" + tickCount);
            return;
        }
        temp.put(tickKey, "0");

        var Bukkit = Java.type("org.bukkit.Bukkit");
        var UUID = Java.type("java.util.UUID");
        var StatsProvider = Java.type(
            "com.dragonminez.common.stats.StatsProvider"
        );
        var StatsCapability = Java.type(
            "com.dragonminez.common.stats.StatsCapability"
        );
        var StatsSyncS2C = Java.type(
            "com.dragonminez.common.network.S2C.StatsSyncS2C"
        );
        var NetworkHandler = Java.type(
            "com.dragonminez.common.network.NetworkHandler"
        );

        var bukkitPlayer = Bukkit.getPlayer(
            UUID.fromString("" + player.getUUID())
        );
        if (bukkitPlayer == null) return;

        var plugin = Bukkit.getPluginManager().getPlugin("Fabled");
        if (plugin == null || !plugin.isEnabled()) return;

        var loader = plugin.getClass().getClassLoader();
        var fabledClass = loader.loadClass(
            "studio.magemonkey.fabled.Fabled"
        );

        var getDataMethod = null;
        var methods = fabledClass.getMethods();
        for (var i = 0; i < methods.length; i++) {
            if (
                "" + methods[i].getName() == "getData" &&
                methods[i].getParameterTypes().length == 1
            ) {
                getDataMethod = methods[i];
                break;
            }
        }
        if (getDataMethod == null) return;

        var fabledData = getDataMethod.invoke(null, bukkitPlayer);
        if (fabledData == null) return;

        var mcEntity = player.getMCEntity
            ? player.getMCEntity()
            : null;
        if (mcEntity == null) return;

        var lazy = StatsProvider.get(
            StatsCapability.INSTANCE,
            mcEntity
        );
        if (lazy == null) return;

        var dmzData = lazy.orElse(null);
        if (dmzData == null) return;

        var resources = dmzData.getResources();
        if (resources == null) return;

        var currentTp = Number(resources.getTrainingPoints());
        if (isNaN(currentTp) || currentTp < 0) currentTp = 0;

        // Negative SP = Java int overflow after refund at MAX — ignore.
        var currentSpRaw = Number(fabledData.getPoints());
        var currentSp = currentSpRaw;
        if (isNaN(currentSp) || currentSp < 0) currentSp = 0;
        if (currentSp > MAX_FABLED_SP) currentSp = MAX_FABLED_SP;

        var stored = player.getStoreddata();
        var levelsNow = readSkillLevels(fabledData);

        // ----------------------------------------------------
        // First sync / migration: snapshot levels, mirror SP.
        // Never debit on snapshot — that would wipe TP for
        // everyone who already owns Prestige/Jobs skills.
        // ----------------------------------------------------
        var needsSnapshot =
            !stored.has(KEY_INITIALIZED) ||
            "" + stored.get(KEY_INITIALIZED) != "true" ||
            !stored.has(KEY_LAST_LEVELS) ||
            "" + stored.get(KEY_LAST_LEVELS) === "";

        if (needsSnapshot) {
            mirrorSp(fabledData, currentTp);
            stored.put(KEY_INITIALIZED, "true");
            stored.put(KEY_LAST_LEVELS, serializeLevels(levelsNow));
            // Drop obsolete invested key from prior broken versions
            try {
                if (stored.has("dmz_fabled_sp_last_invested")) {
                    stored.remove("dmz_fabled_sp_last_invested");
                }
            } catch (rmErr) {}
            return;
        }

        var levelsPrev = deserializeLevels(
            "" + stored.get(KEY_LAST_LEVELS)
        );

        // positive = remove TP (purchase), negative = add TP (refund)
        var tpDelta = netTpDeltaFromLevelChanges(
            fabledData,
            levelsPrev,
            levelsNow
        );

        var tpChanged = false;
        if (tpDelta !== 0) {
            var newTp = currentTp - tpDelta;
            if (newTp < 0) newTp = 0;
            resources.setTrainingPoints(newTp);
            currentTp = newTp;
            tpChanged = true;

            if (DEBUG) {
                if (tpDelta > 0) {
                    player.message(
                        "\u00A7e[TP/SP] Purchased \u00A7c-" +
                        formatNumber(tpDelta) +
                        "\u00A7e TP \u00A77→ \u00A7a" +
                        formatNumber(currentTp)
                    );
                } else {
                    player.message(
                        "\u00A7a[TP/SP] Refunded \u00A7a+" +
                        formatNumber(-tpDelta) +
                        "\u00A7a TP \u00A77→ \u00A7a" +
                        formatNumber(currentTp)
                    );
                }
            }
        }

        var spChanged = mirrorSp(fabledData, currentTp);

        if (tpChanged) {
            try {
                NetworkHandler.sendToTrackingEntityAndSelf(
                    new StatsSyncS2C(mcEntity),
                    mcEntity
                );
            } catch (syncErr) {}
        }

        if (spChanged) {
            try {
                fabledData.updateScoreboard();
            } catch (sbErr) {}
        }

        stored.put(KEY_LAST_LEVELS, serializeLevels(levelsNow));
    } catch (error) {
        if (event.player != null && DEBUG) {
            event.player.message(
                "\u00A7c[TP/SP Sync Error] \u00A7f" + error
            );
        }
    }
}

function mirrorSp(fabledData, currentTp) {
    var target = Math.floor(Number(currentTp));
    if (isNaN(target) || target < 0) target = 0;
    if (target > MAX_FABLED_SP) target = MAX_FABLED_SP;

    var cur = Number(fabledData.getPoints());
    if (isNaN(cur) || cur < 0) cur = -1; // force fix overflow
    if (cur !== target) {
        fabledData.setPoints(target);
        return true;
    }
    return false;
}

function readSkillLevels(fabledData) {
    var map = {};
    try {
        var skills = fabledData.getSkills();
        if (skills == null) return map;
        var it = skills.iterator();
        while (it.hasNext()) {
            var ps = it.next();
            if (ps == null) continue;
            var level = 0;
            try {
                level = parseInt("" + ps.getLevel(), 10);
            } catch (e0) {
                level = 0;
            }
            if (isNaN(level) || level < 0) level = 0;
            if (level <= 0) continue;

            var name = null;
            try {
                name = "" + ps.getData().getName();
            } catch (e1) {
                try {
                    name = "" + ps.getData().getKey();
                } catch (e2) {
                    name = null;
                }
            }
            if (name == null || name === "" || name === "null") continue;
            map[name] = level;
        }
    } catch (err) {}
    return map;
}

function skillCostAtLevel(skillData, fromLevel) {
    // Skill.getCost(fromLevel) = cost to go from fromLevel -> fromLevel+1
    try {
        var c = Number(skillData.getCost(fromLevel));
        if (isNaN(c) || c < 0) return 0;
        return c;
    } catch (err) {
        return 0;
    }
}

function findSkillData(fabledData, skillName) {
    try {
        var ps = fabledData.getSkill(skillName);
        if (ps != null) return ps.getData();
    } catch (e0) {}
    try {
        var skills = fabledData.getSkills();
        var it = skills.iterator();
        while (it.hasNext()) {
            var ps2 = it.next();
            var n = "" + ps2.getData().getName();
            if (n === skillName) return ps2.getData();
        }
    } catch (e1) {}
    return null;
}

function netTpDeltaFromLevelChanges(fabledData, prev, now) {
    var debit = 0; // >0 remove TP, <0 add TP
    var names = {};
    var k;
    for (k in prev) names[k] = true;
    for (k in now) names[k] = true;

    for (k in names) {
        if (!names.hasOwnProperty(k)) continue;
        var oldL = prev[k] ? parseInt("" + prev[k], 10) : 0;
        var newL = now[k] ? parseInt("" + now[k], 10) : 0;
        if (isNaN(oldL) || oldL < 0) oldL = 0;
        if (isNaN(newL) || newL < 0) newL = 0;
        if (oldL === newL) continue;

        var data = findSkillData(fabledData, k);
        if (data == null) continue;

        var L;
        if (newL > oldL) {
            for (L = oldL; L < newL; L++) {
                debit += skillCostAtLevel(data, L);
            }
        } else {
            for (L = newL; L < oldL; L++) {
                debit -= skillCostAtLevel(data, L);
            }
        }
    }
    return debit;
}

function serializeLevels(map) {
    // name:level|name:level
    var parts = [];
    for (var k in map) {
        if (!map.hasOwnProperty(k)) continue;
        parts.push(
            encodeURIComponent(k) + ":" + map[k]
        );
    }
    return parts.join("|");
}

function deserializeLevels(text) {
    var map = {};
    if (text == null || text === "") return map;
    var parts = ("" + text).split("|");
    for (var i = 0; i < parts.length; i++) {
        var p = parts[i];
        if (!p) continue;
        var idx = p.lastIndexOf(":");
        if (idx <= 0) continue;
        var name = decodeURIComponent(p.substring(0, idx));
        var level = parseInt(p.substring(idx + 1), 10);
        if (!name || isNaN(level) || level <= 0) continue;
        map[name] = level;
    }
    return map;
}

function formatNumber(value) {
    value = Number(value);
    if (isNaN(value)) return "0";
    var text = "" + Math.floor(value);
    var output = "";
    var count = 0;
    for (var i = text.length - 1; i >= 0; i--) {
        output = text.charAt(i) + output;
        count++;
        if (count == 3 && i > 0) {
            output = "," + output;
            count = 0;
        }
    }
    return output;
}
