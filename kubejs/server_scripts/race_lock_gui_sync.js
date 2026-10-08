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

PlayerEvents.tick(function (event) {
  try {
    var player = event.player;
    if (!player) return;
    var age = 0;
    try {
      age = Number(player.age);
    } catch (e0) {
      try {
        age = Number(player.tickCount);
      } catch (e1) {
        return;
      }
    }
    if (!isFinite(age) || age <= 0 || age % SYNC_INTERVAL_TICKS !== 0) return;
    clearPlayer(player);
  } catch (err) {}
});

console.info("[RaceLockGUI] race lock removed; padlocks cleared (" + CHANNEL + ")");
