// Example KubeJS bridge for DragonMineZ 2.1.3 on Forge 1.20.1.
// Your existing gameplay scripts are CustomNPCs (see customnpcs/scripts/).
// Enable this file when you want KubeJS-side hooks alongside CNPC.
//
// Tip: rename to start with ! or move aside to disable, depending on your KubeJS config.

const StatsProvider = Java.loadClass('com.dragonminez.common.stats.StatsProvider')
const StatsCapability = Java.loadClass('com.dragonminez.common.stats.StatsCapability')
const StatsSyncS2C = Java.loadClass('com.dragonminez.common.network.S2C.StatsSyncS2C')
const NetworkHandler = Java.loadClass('com.dragonminez.common.network.NetworkHandler')
const DMZEvent = Java.loadClass('com.dragonminez.common.events.DMZEvent')

/**
 * @returns {any|null} StatsData
 */
function getDmzData(player) {
  if (!player) return null
  const lazy = StatsProvider.get(StatsCapability.INSTANCE, player)
  if (!lazy) return null
  return lazy.orElse(null)
}

function syncDmz(player) {
  NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player)
}

// Mirror of your CNPC "once per second" tick style
PlayerEvents.tick(event => {
  const player = event.player
  if (!player || player.level.clientSide) return
  if (player.age % 20 !== 0) return

  const data = getDmzData(player)
  if (!data) return

  // Example read-only probe (remove or replace with real logic):
  // const energy = data.getResources().getCurrentEnergy()
  // const fly = data.getSkills().getSkillLevel('fly')
})

// Forge DMZ events via KubeJS NativeEvents
NativeEvents.onEvent(DMZEvent.TPGainEvent, event => {
  // Example: leave TP unchanged
  // event.setTpGain(event.getTpGain())
})

NativeEvents.onEvent(DMZEvent.DamageModifyEvent, event => {
  // Example cancelable damage adjust:
  // event.setAmount(event.getAmount() * 1.0)
})
