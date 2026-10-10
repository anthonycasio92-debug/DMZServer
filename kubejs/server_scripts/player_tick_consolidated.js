// Consolidated player tick - replaces 4 separate PlayerEvents.tick handlers
// (dmz_weapon_bonus_scale, lifesteal_cap, pathalafix1 / dmz_pothala_protection,
// race_lock_gui_sync).
// One handler = one Rhino proxy invocation per player per tick instead of four.
//
// Each original file keeps its helpers and publishes them on global. KubeJS
// gives every server script its own scope, so the bodies below call those
// globals. Each section keeps its own interval. A return inside one section
// must not skip the others, so the old early-returns are if-guards here.
//
// Weapon: a missing age still runs wrap + cleanup (do not bail on null age).
// Pothala: uses tickCount; if that read throws, the scan still runs.
// Race lock: falls back to tickCount only when reading age throws.

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
    } catch (err) {}
});

console.info(
    "[DMZ] Consolidated player tick loaded (weapon bonus, lifesteal cap, pothala, race-lock clear)."
);
