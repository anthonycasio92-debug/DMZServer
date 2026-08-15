// ============================================================
// DMZ Training Points -> Fabled Skill Points
// CNPC Global Player Tick Script
//
// DMZ TP is the authoritative currency.
// Fabled SP mirrors available TP (capped at Integer.MAX_VALUE).
//
// Spending Fabled SP (skill purchase) removes the same amount
// from DMZ TP. False "spend" detection MUST NOT wipe TP when
// Fabled briefly reports 0 points (reload / data race).
//
// Jobs skills (Building / Farming / Fishing) cost 10000 SP =
// 10000 TP. If SP does not mirror TP, those purchases fail
// silently in the Fabled GUI.
// ============================================================

var TICK_INTERVAL = 5; // ~4 checks/sec
var DEBUG = false;

var MAX_FABLED_SP = 2147483647;

// Hard cap on SP→TP debit per sync tick. Real skill buys are
// far below this; a jump of billions means Fabled data glitched.
var MAX_SPEND_PER_TICK = 50000000;

// Stored-data keys
var KEY_INITIALIZED = "dmz_fabled_sp_initialized";
var KEY_LAST_DISPLAYED_SP = "dmz_fabled_sp_last_displayed";
var KEY_LAST_TP = "dmz_fabled_tp_last";

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

        var uuidStr = "" + player.getUUID();
        var bukkitPlayer = Bukkit.getPlayer(
            UUID.fromString(uuidStr)
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

        var stored = player.getStoreddata();

        var targetDisplayedSp = Math.floor(currentTp);
        if (targetDisplayedSp > MAX_FABLED_SP) {
            targetDisplayedSp = MAX_FABLED_SP;
        }
        if (targetDisplayedSp < 0) targetDisplayedSp = 0;

        // ----------------------------------------------------
        // First sync
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

            if (DEBUG) {
                player.message(
                    "\u00A7a[TP/SP Sync] Initialized: \u00A7e" +
                    formatNumber(currentTp) +
                    " TP \u00A77| \u00A7e" +
                    formatNumber(targetDisplayedSp) +
                    " SP"
                );
            }
            return;
        }

        var lastDisplayedSp = targetDisplayedSp;
        if (stored.has(KEY_LAST_DISPLAYED_SP)) {
            lastDisplayedSp = parseInt(
                "" + stored.get(KEY_LAST_DISPLAYED_SP),
                10
            );
            if (isNaN(lastDisplayedSp) || lastDisplayedSp < 0) {
                lastDisplayedSp = targetDisplayedSp;
            }
            if (lastDisplayedSp > MAX_FABLED_SP) {
                lastDisplayedSp = MAX_FABLED_SP;
            }
        }

        var lastTp = currentTp;
        if (stored.has(KEY_LAST_TP)) {
            lastTp = Number(stored.get(KEY_LAST_TP));
            if (isNaN(lastTp) || lastTp < 0) lastTp = currentTp;
        }

        // ----------------------------------------------------
        // Detect real SP spending (skill purchase).
        // Guard against Fabled briefly reporting 0 / garbage.
        // ----------------------------------------------------
        var spentSp = 0;
        if (currentSp < lastDisplayedSp) {
            var rawDrop = lastDisplayedSp - currentSp;

            // Glitch: SP collapsed to 0 while DMZ TP is still high.
            // Re-mirror SP from TP — do NOT debit TP.
            var looksLikeDesync =
                currentSp === 0 &&
                currentTp >= 1000 &&
                lastDisplayedSp >= 1000 &&
                rawDrop >= 1000;

            // Glitch: drop larger than any plausible skill cost burst.
            var looksLikeOverflow =
                rawDrop > MAX_SPEND_PER_TICK;

            // Glitch: TP did not stay stable while SP cratered
            // (real spends debit TP on the *next* sync after SP drops;
            //  a simultaneous huge TP loss is unrelated).
            if (looksLikeDesync || looksLikeOverflow) {
                if (DEBUG) {
                    player.message(
                        "\u00A7c[TP/SP Sync] Ignored fake spend drop=" +
                        formatNumber(rawDrop) +
                        " (desync/overflow guard)"
                    );
                }
                spentSp = 0;
            } else {
                spentSp = rawDrop;
            }
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
                    "\u00A7e[TP/SP Sync] Spent \u00A7c" +
                    formatNumber(spentSp) +
                    " SP\u00A7e. Remaining TP: \u00A7a" +
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
