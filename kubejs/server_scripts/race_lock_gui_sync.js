// Race lock is retired. This only clears padlocks the old sync left on clients.
// Channel: dmz_race_locks
// Payload: { locked: [], required: {} }

var CHANNEL = "dmz_race_locks";
var SYNC_INTERVAL_TICKS = 200;

function clearPlayer(player) {
  if (!player) return;
  try {
    player.sendData(CHANNEL, { locked: [], required: {} });
  } catch (err) {
    console.error("[RaceLockGUI] clear failed: " + err);
  }
}

PlayerEvents.loggedIn(function (event) {
  clearPlayer(event.player);
});

// PlayerEvents.tick moved to player_tick_consolidated.js.
global.dmzRaceLockTick = {
  SYNC_INTERVAL_TICKS: SYNC_INTERVAL_TICKS,
  clearPlayer: clearPlayer
};

console.info("[RaceLockGUI] race lock removed; padlocks cleared (" + CHANNEL + ")");
