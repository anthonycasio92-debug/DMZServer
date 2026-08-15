// ============================================================
// DMZ Training Points -> Fabled Skill Points
// CNPC Global Player Tick Script
//
// DMZ TP is authoritative. Fabled SP mirrors available TP
// (capped at Integer.MAX_VALUE for display only).
//
// Spend detection uses getInvestedSkillPoints() — that only
// rises when a skill is actually purchased. Do NOT debit TP
// from raw SP drops (reload/race can zero SP briefly).
//
// Prestige costs go up to 2,000,000,000 SP; Jobs Building /
// Farming / Fishing cost 10,000 SP each.
// ============================================================

var TICK_INTERVAL = 5;
var DEBUG = false;
var MAX_FABLED_SP = 2147483647;

var KEY_INITIALIZED = "dmz_fabled_sp_initialized";
var KEY_LAST_DISPLAYED_SP = "dmz_fabled_sp_last_displayed";
var KEY_LAST_TP = "dmz_fabled_tp_last";
var KEY_LAST_INVESTED = "dmz_fabled_sp_last_invested";

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

        var currentSp = parseInt("" + fabledData.getPoints(), 10);
        if (isNaN(currentSp) || currentSp < 0) currentSp = 0;

        var invested = 0;
        try {
            invested = parseInt(
                "" + fabledData.getInvestedSkillPoints(),
                10
            );
        } catch (invErr) {
            invested = 0;
        }
        if (isNaN(invested) || invested < 0) invested = 0;

        var stored = player.getStoreddata();

        var targetDisplayedSp = Math.floor(currentTp);
        if (targetDisplayedSp > MAX_FABLED_SP) {
            targetDisplayedSp = MAX_FABLED_SP;
        }
        if (targetDisplayedSp < 0) targetDisplayedSp = 0;

        // ----------------------------------------------------
        // First sync — mirror SP from TP, record invested
        // ----------------------------------------------------
        if (
            !stored.has(KEY_INITIALIZED) ||
            "" + stored.get(KEY_INITIALIZED) != "true"
        ) {
            if (currentSp != targetDisplayedSp) {
                fabledData.setPoints(targetDisplayedSp);
                currentSp = targetDisplayedSp;
                try {
                    fabledData.updateScoreboard();
                } catch (ignoreSb) {}
            }

            stored.put(KEY_INITIALIZED, "true");
            stored.put(KEY_LAST_DISPLAYED_SP, "" + targetDisplayedSp);
            stored.put(KEY_LAST_TP, "" + currentTp);
            stored.put(KEY_LAST_INVESTED, "" + invested);

            if (DEBUG) {
                player.message(
                    "\u00A7a[TP/SP Sync] Init TP=" +
                    formatNumber(currentTp) +
                    " SP=" +
                    formatNumber(targetDisplayedSp) +
                    " invested=" +
                    formatNumber(invested)
                );
            }
            return;
        }

        var lastInvested = invested;
        if (stored.has(KEY_LAST_INVESTED)) {
            lastInvested = parseInt(
                "" + stored.get(KEY_LAST_INVESTED),
                10
            );
            if (isNaN(lastInvested) || lastInvested < 0) {
                lastInvested = invested;
            }
        }

        // ----------------------------------------------------
        // Real spend = invested skill points went UP (purchase).
        // Raw SP drops without invested change = reload glitch —
        // remirror only, never wipe TP.
        // ----------------------------------------------------
        var spentSp = 0;
        if (invested > lastInvested) {
            spentSp = invested - lastInvested;
        }

        var tpChanged = false;
        var spChanged = false;

        if (spentSp > 0) {
            var newTp = currentTp - spentSp;
            if (newTp < 0) newTp = 0;
            resources.setTrainingPoints(newTp);
            currentTp = newTp;
            tpChanged = true;

            if (DEBUG) {
                player.message(
                    "\u00A7e[TP/SP Sync] Purchase cost \u00A7c" +
                    formatNumber(spentSp) +
                    "\u00A7e. TP left: \u00A7a" +
                    formatNumber(currentTp)
                );
            }
        }

        targetDisplayedSp = Math.floor(currentTp);
        if (targetDisplayedSp > MAX_FABLED_SP) {
            targetDisplayedSp = MAX_FABLED_SP;
        }
        if (targetDisplayedSp < 0) targetDisplayedSp = 0;

        if (currentSp != targetDisplayedSp) {
            fabledData.setPoints(targetDisplayedSp);
            currentSp = targetDisplayedSp;
            spChanged = true;
        }

        if (tpChanged) {
            try {
                NetworkHandler.sendToTrackingEntityAndSelf(
                    new StatsSyncS2C(mcEntity),
                    mcEntity
                );
            } catch (dmzSyncError) {}
        }

        if (spChanged) {
            try {
                fabledData.updateScoreboard();
            } catch (scoreboardError) {}
        }

        stored.put(KEY_LAST_DISPLAYED_SP, "" + targetDisplayedSp);
        stored.put(KEY_LAST_TP, "" + currentTp);
        stored.put(KEY_LAST_INVESTED, "" + invested);
    } catch (error) {
        if (event.player != null && DEBUG) {
            event.player.message(
                "\u00A7c[TP/SP Sync Error] \u00A7f" + error
            );
        }
    }
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
