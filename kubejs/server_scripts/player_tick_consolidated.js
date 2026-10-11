// Consolidated player tick. One handler for every former per-player tick:
// weapon bonus, lifesteal cap, pothala, race-lock clear, apotheosis spawner
// vanillaize, capsule purge, heartstop strip, silk touch strip, tinkers silky.
//
// Each original file keeps its helpers and publishes them on global. KubeJS
// gives every server script its own scope, so the bodies below call those
// globals. Each section keeps its own interval. A return inside one section
// must not skip the others, so the old early-returns are if-guards here.
//
// Weapon: a missing age still runs wrap + cleanup (do not bail on null age).
// Pothala: uses tickCount; if that read throws, the scan still runs.
// Race lock: falls back to tickCount only when reading age throws.
// Spawner scan: slower than the old 5s pass, and each player is offset by
// their id so the chunk walks do not land on the same tick.
//
// LegacyMechanics LmHotPath owns this listener once the class is loaded.
// On an older jar the class is missing and this listener keeps running.

function dmzLmOwnsHotPath() {
    try {
        return Java.loadClass("com.dbzlegacy.adaptivedifficulty.kube.LmHotPath").ENABLED === true;
    } catch (e) {
        return false;
    }
}

if (dmzLmOwnsHotPath()) {
    console.info("[DMZ] Player tick hot path is native LegacyMechanics; KubeJS listener idle.");
} else {
PlayerEvents.tick(function (event) {
    try {
        var player = event.player;
        if (player == null) return;

        // --- dmz_weapon_bonus_scale (was: every 20 ticks; null age still runs) ---
        try {
            var weapon = global.dmzWeaponBonusTick;
            if (weapon) {
                var raw = player;
                if (!(raw.age != null && raw.age % 20 !== 0)) {
                    var wrapped = weapon.wrapPlayer(raw);
                    weapon.cleanupApothicFlats(wrapped);
                    if ((wrapped.getAge() % weapon.UPDATE_INTERVAL) == 0) {
                        weapon.updateWeaponMultipliers(wrapped, false);
                    }
                }
            }
        } catch (err) {
            console.info("[DMZ Weapon V11 TICK ERROR] " + err);
        }

        // --- lifesteal_cap (was: every TICK_INTERVAL) ---
        try {
            var life = global.dmzLifestealCapTick;
            if (life && player.age % life.TICK_INTERVAL === 0) {
                life.clampAll(player);
            }
        } catch (err) {}

        // --- pathalafix1 (was: every CHECK_INTERVAL, tickCount) ---
        try {
            var pothala = global.dmzPothalaTick;
            if (pothala) {
                var runScan = true;
                try {
                    if ((player.tickCount % pothala.CHECK_INTERVAL) != 0) {
                        runScan = false;
                    }
                } catch (ignored) {
                    // If tickCount access fails, allow the scan instead.
                }
                if (runScan) {
                    pothala.cleanAllPothalas(player);
                }
            }
        } catch (err) {
            console.error("[DMZ Pothala Protection] tick error: " + err);
        }

        // --- race_lock_gui_sync (was: every SYNC_INTERVAL_TICKS) ---
        try {
            var race = global.dmzRaceLockTick;
            if (race) {
                var age = 0;
                try {
                    age = Number(player.age);
                } catch (e0) {
                    try {
                        age = Number(player.tickCount);
                    } catch (e1) {
                        age = NaN;
                    }
                }
                if (isFinite(age) && age > 0 && age % race.SYNC_INTERVAL_TICKS === 0) {
                    race.clearPlayer(player);
                }
            }
        } catch (err) {}

        // --- apotheosis_spawner_disable (chunk walk; staggered, not every player at once) ---
        try {
            var apoth = global.dmzApothSpawnerTick;
            if (apoth) {
                var apothAge = NaN;
                try {
                    apothAge = Number(player.age);
                } catch (eApothAge) {
                    apothAge = NaN;
                }
                if (isFinite(apothAge) && apothAge >= 0) {
                    var span = apoth.SCAN_INTERVAL;
                    var slot = 0;
                    try {
                        var key = "";
                        try {
                            key = String(player.uuid);
                        } catch (eUuid) {
                            key = "";
                        }
                        if (!key || key === "undefined") {
                            key = String(player.username);
                        }
                        var hash = 0;
                        for (var hi = 0; hi < key.length; hi++) {
                            hash = (hash * 31 + key.charCodeAt(hi)) | 0;
                        }
                        slot = ((hash % span) + span) % span;
                    } catch (eSlot) {
                        slot = 0;
                    }
                    if (apothAge % span === slot) {
                        apoth.scan(player);
                    }
                }
            }
        } catch (err) {}

        // --- capsule_disable (was: every PLAYER_PURGE_INTERVAL) ---
        try {
            var capsules = global.dmzCapsuleTick;
            if (capsules && player.age % capsules.PLAYER_PURGE_INTERVAL === 0) {
                capsules.purge(player, true);
            }
        } catch (err) {
            console.error("[Capsule Disable] tick error: " + err);
        }

        // --- heartstop_disable (was: every 40 ticks; a failed age read skips only this) ---
        try {
            var heart = global.dmzHeartstopTick;
            if (heart) {
                var heartAge = 0;
                var heartOk = true;
                try {
                    heartAge = player.age;
                } catch (eHeart) {
                    heartOk = false;
                }
                if (heartOk && heartAge % heart.INTERVAL === 0) {
                    heart.strip(player);
                }
            }
        } catch (err) {}

        // --- remove_silk_touch (was: every PLAYER_SCAN_INTERVAL) ---
        try {
            var silk = global.dmzSilkTouchTick;
            if (silk && player.age % silk.PLAYER_SCAN_INTERVAL === 0) {
                silk.purge(player, true);
            }
        } catch (err) {
            console.error("[Silk Touch Remove] tick error: " + err);
        }

        // --- tinkers_silky_disable (cloth every 1s; tools on the slower cadence) ---
        try {
            var silky = global.dmzTinkersSilkyTick;
            if (silky) {
                var silkyAge = 0;
                var silkyOk = true;
                try {
                    silkyAge = Number(player.age);
                } catch (eSilky) {
                    silkyOk = false;
                }
                if (silkyOk && silkyAge % silky.CLOTH_SCAN_INTERVAL === 0) {
                    silky.purge(player, true, silkyAge % silky.TOOL_SCAN_INTERVAL === 0);
                }
            }
        } catch (err) {
            console.error("[Tinkers Silky] tick error: " + err);
        }
    } catch (err) {}
});
}

console.info(
    "[DMZ] Consolidated player tick loaded (weapon, lifesteal, pothala, race-lock, spawner, capsule, heartstop, silk, silky)."
);
