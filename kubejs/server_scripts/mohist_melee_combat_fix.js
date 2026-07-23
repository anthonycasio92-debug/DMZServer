/**
 * Mohist + DragonMineZ: melee deals no damage until the player dies once.
 *
 * Soft prime (default): refresh HP attributes / clear join invuln / brief
 * gamemode flicker shortly after login.
 *
 * Hard prime: one keep-inventory kill→respawn per login (matches the manual
 * fix). Enable only if soft prime is not enough on your Mohist build.
 *
 * See docs/MOHIST_MELEE_BUG.md
 */

// Set true only if soft prime fails in testing.
const HARD_PRIME = false

// Ticks after login before priming (let DMZ login sync finish).
const PRIME_DELAY_TICKS = 40

const TAG_PRIMED = 'dmz_mohist_melee_primed'
const TAG_HARD_PENDING = 'dmz_mohist_melee_hard_pending'
const TAG_KEEPINV_RESTORE = 'dmz_mohist_melee_keepinv_restore'

function pd(player) {
  return player.persistentData
}

function alreadyPrimed(player) {
  try {
    return !!pd(player).getBoolean(TAG_PRIMED)
  } catch (e) {
    return false
  }
}

function markPrimed(player) {
  try {
    pd(player).putBoolean(TAG_PRIMED, true)
  } catch (e) {}
}

function playerName(player) {
  return player.username || player.name.string
}

function softPrime(player) {
  try {
    // Clear vanilla join / teleport i-frames that can linger oddly on hybrids.
    try {
      player.invulnerableTime = 0
    } catch (e0) {}
    try {
      player.hurtTime = 0
    } catch (e1) {}

    try {
      const hp = player.health
      const max = player.maxHealth
      if (max > 0) {
        player.health = Math.min(hp > 0 ? hp : max, max)
      }
    } catch (e2) {}

    // Brief gamemode flicker can rebuild Mohist's CraftPlayer attack bridge.
    // Prefer commands — more reliable across KubeJS builds than GameMode enums.
    const name = playerName(player)
    const server = player.server
    let mode = 'survival'
    try {
      mode = String(player.gameMode && player.gameMode.id ? player.gameMode.id : 'survival')
    } catch (e3) {
      mode = 'survival'
    }

    if (mode === 'survival' || mode === 'SURVIVAL') {
      server.runCommandSilent('gamemode adventure ' + name)
      server.runCommandSilent('gamemode survival ' + name)
    } else if (mode === 'adventure' || mode === 'ADVENTURE') {
      server.runCommandSilent('gamemode survival ' + name)
      server.runCommandSilent('gamemode adventure ' + name)
    }

    // Micro-teleport: force tracker / position refresh without changing world.
    try {
      player.teleportTo(player.x, player.y + 0.001, player.z)
    } catch (e4) {}
  } catch (e) {
    console.error('[mohist-melee-fix] softPrime failed: ' + e)
  }
}

function hardPrime(player) {
  try {
    // WARNING: forces a real death. Enable keepInventory on the server first,
    // or players will drop items. Soft prime is preferred.
    pd(player).putBoolean(TAG_HARD_PENDING, true)
    pd(player).putBoolean(TAG_KEEPINV_RESTORE, false)
    player.server.runCommandSilent('gamerule keepInventory true')
    player.kill()
  } catch (e) {
    console.error('[mohist-melee-fix] hardPrime failed: ' + e)
    try {
      pd(player).putBoolean(TAG_HARD_PENDING, false)
    } catch (e2) {}
  }
}

PlayerEvents.loggedIn(event => {
  const player = event.player
  if (!player) return
  try {
    if (player.level.clientSide) return
  } catch (e) {}

  // New connection each login — allow priming again this session.
  try {
    pd(player).putBoolean(TAG_PRIMED, false)
    pd(player).putBoolean(TAG_HARD_PENDING, false)
  } catch (e2) {}

  player.server.scheduleInTicks(PRIME_DELAY_TICKS, () => {
    try {
      if (!player || !player.isAlive()) return
      if (alreadyPrimed(player)) return

      if (HARD_PRIME) {
        hardPrime(player)
        return
      }

      softPrime(player)
      markPrimed(player)
      console.info('[mohist-melee-fix] Soft-primed melee combat for ' + playerName(player))
    } catch (e3) {
      console.error('[mohist-melee-fix] scheduled prime failed: ' + e3)
    }
  })
})

PlayerEvents.respawned(event => {
  const player = event.player
  if (!player) return
  try {
    if (player.level.clientSide) return
  } catch (e) {}

  let pending = false
  try {
    pending = !!pd(player).getBoolean(TAG_HARD_PENDING)
  } catch (e2) {}
  if (!pending) return

  try {
    pd(player).putBoolean(TAG_HARD_PENDING, false)
  } catch (e3) {}
  markPrimed(player)

  try {
    if (pd(player).getBoolean(TAG_KEEPINV_RESTORE)) {
      pd(player).putBoolean(TAG_KEEPINV_RESTORE, false)
      // Do NOT force keepInventory false if the server normally runs with it true.
      // Only restore when we flipped it for the prime — operators should set
      // HARD_PRIME carefully on production. Leave gamerule alone if unsure.
    }
  } catch (e4) {}

  softPrime(player)
  console.info('[mohist-melee-fix] Hard-primed melee combat for ' + playerName(player))
})
