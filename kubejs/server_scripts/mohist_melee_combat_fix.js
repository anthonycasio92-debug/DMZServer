/**
 * Mohist + DragonMineZ: some players deal no melee damage until they die once.
 *
 * Does NOT kill, teleport, gamemode-flicker, or touch keepInventory.
 * (Login kills are unsafe — DMZ Otherworld treats death as story state.)
 *
 * 1) Login: clear DMZ combat locks, refreshAttributes, re-sync Stats + weapons
 * 2) Attack fallback: if Player.attack starts but the target takes no damage,
 *    call hurt() with DMZ melee (bypasses broken Mohist Player.attack bridge)
 *
 * See docs/MOHIST_MELEE_BUG.md
 */

const Gson = Java.loadClass('com.google.gson.Gson')
const HashMap = Java.loadClass('java.util.HashMap')
const AttackEntityEvent = Java.loadClass(
  'net.minecraftforge.event.entity.player.AttackEntityEvent'
)
const LivingHurtEvent = Java.loadClass(
  'net.minecraftforge.event.entity.living.LivingHurtEvent'
)

const StatsProvider = Java.loadClass('com.dragonminez.common.stats.StatsProvider')
const StatsCapability = Java.loadClass('com.dragonminez.common.stats.StatsCapability')
const StatsSyncS2C = Java.loadClass('com.dragonminez.common.network.S2C.StatsSyncS2C')
const NetworkHandler = Java.loadClass('com.dragonminez.common.network.NetworkHandler')
const WeaponRegistry = Java.loadClass(
  'com.dragonminez.common.combat.logic.weapon.WeaponRegistry'
)
const SyncWeaponRegistryS2C = Java.loadClass(
  'com.dragonminez.common.network.S2C.SyncWeaponRegistryS2C'
)

const SYNC_DELAY_TICKS = 25
const FALLBACK_CHECK_TICKS = 2

/** attackerUUID -> pending attack info */
const pendingAttacks = {}

function playerName(entity) {
  try {
    if (entity.username) return entity.username
  } catch (e0) {}
  try {
    if (entity.getGameProfile) return entity.getGameProfile().getName()
  } catch (e1) {}
  try {
    return entity.name.string
  } catch (e2) {
    return '?'
  }
}

function mcEntity(entity) {
  try {
    if (entity.minecraftPlayer) return entity.minecraftPlayer
  } catch (e0) {}
  try {
    if (entity.minecraftEntity) return entity.minecraftEntity
  } catch (e1) {}
  return entity
}

function getServer(entity) {
  try {
    if (Utils && Utils.server) return Utils.server
  } catch (e0) {}
  try {
    if (entity.server) return entity.server
  } catch (e1) {}
  try {
    const e = mcEntity(entity)
    if (e.getServer) return e.getServer()
  } catch (e2) {}
  try {
    const lvl = entity.level || entity.getCommandSenderWorld() || entity.getLevel()
    if (lvl && (lvl.server || lvl.getServer)) return lvl.server || lvl.getServer()
  } catch (e3) {}
  return null
}

function schedule(entity, ticks, fn) {
  const server = getServer(entity)
  if (server && server.scheduleInTicks) {
    server.scheduleInTicks(ticks, fn)
    return true
  }
  // Raw MinecraftServer — use tick-delay via Utils if available
  try {
    if (Utils && Utils.server && Utils.server.scheduleInTicks) {
      Utils.server.scheduleInTicks(ticks, fn)
      return true
    }
  } catch (e) {}
  return false
}

function getDmzData(entity) {
  try {
    const lazy = StatsProvider.get(StatsCapability.INSTANCE, mcEntity(entity))
    if (!lazy) return null
    return lazy.orElse(null)
  } catch (e) {
    return null
  }
}

function syncDmzStats(entity) {
  try {
    const mc = mcEntity(entity)
    NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(mc), mc)
  } catch (e) {
    console.error('[mohist-melee-fix] StatsSync failed: ' + e)
  }
}

function syncWeaponRegistry(entity) {
  try {
    const mc = mcEntity(entity)
    const out = new HashMap()
    const entries = WeaponRegistry.registrations.entrySet().iterator()
    while (entries.hasNext()) {
      const entry = entries.next()
      out.put(entry.getKey().toString(), entry.getValue())
    }
    const json = new Gson().toJson(out)
    NetworkHandler.sendToPlayer(new SyncWeaponRegistryS2C(json), mc)
  } catch (e) {
    console.error('[mohist-melee-fix] WeaponRegistry sync failed: ' + e)
  }
}

function clearDmzCombatLocks(entity) {
  const data = getDmzData(entity)
  if (!data) return false
  try {
    const status = data.getStatus()
    status.setStrikeLocked(false)
    status.setStunEffect(false)
    status.setKnockedDown(false)
    try {
      status.setBlocking(false)
    } catch (e0) {}
    try {
      data.getCooldowns().removeCooldown('KnockdownDuration')
    } catch (e1) {}
    syncDmzStats(entity)
    return true
  } catch (e) {
    return false
  }
}

function refreshAttributes(entity) {
  const ent = mcEntity(entity)
  try {
    if (typeof ent.refreshAttributes === 'function') ent.refreshAttributes()
    else if (typeof ent.m_6210_ === 'function') ent.m_6210_()
  } catch (e) {}
}

function loginPrime(player) {
  const hasCaps = clearDmzCombatLocks(player)
  refreshAttributes(player)
  syncDmzStats(player)
  syncWeaponRegistry(player)
  console.info(
    '[mohist-melee-fix] Login sync for ' +
      playerName(player) +
      (hasCaps ? ' (caps ok)' : ' (caps missing)')
  )
}

function uuidOf(entity) {
  try {
    if (entity.uuid) return String(entity.uuid)
  } catch (e0) {}
  try {
    return String(entity.getUUID())
  } catch (e1) {
    return playerName(entity)
  }
}

function entityId(entity) {
  try {
    if (entity.getId) return entity.getId()
  } catch (e0) {}
  try {
    return entity.id
  } catch (e1) {
    return -1
  }
}

function getHealth(entity) {
  try {
    if (typeof entity.health === 'number') return Number(entity.health)
  } catch (e0) {}
  const e = mcEntity(entity)
  try {
    if (typeof e.getHealth === 'function') return Number(e.getHealth())
  } catch (e1) {}
  try {
    if (typeof e.m_21223_ === 'function') return Number(e.m_21223_())
  } catch (e2) {}
  return NaN
}

function isLiving(entity) {
  try {
    if (entity.isLiving && entity.isLiving()) return true
  } catch (e0) {}
  try {
    return isFinite(getHealth(entity))
  } catch (e1) {
    return false
  }
}

function isAlive(entity) {
  try {
    if (typeof entity.isAlive === 'function') return !!entity.isAlive()
  } catch (e0) {}
  try {
    if (typeof entity.isAlive === 'boolean') return entity.isAlive
  } catch (e1) {}
  return getHealth(entity) > 0
}

function clearIFrames(entity) {
  const e = mcEntity(entity)
  try {
    e.invulnerableTime = 0
  } catch (e0) {}
  try {
    e.hurtTime = 0
  } catch (e1) {}
  try {
    if (typeof e.m_6703_ === 'function') {
      /* setLastHurtByMob noop */
    }
  } catch (e2) {}
}

function playerAttackSource(attacker) {
  const mc = mcEntity(attacker)
  try {
    if (attacker.damageSources) return attacker.damageSources().playerAttack(attacker)
  } catch (e0) {}
  try {
    return mc.damageSources().playerAttack(mc)
  } catch (e1) {
    return null
  }
}

function forceMeleeHit(attacker, target) {
  const data = getDmzData(attacker)
  if (!data) return false
  try {
    if (!data.getStatus().isHasCreatedCharacter()) return false
  } catch (e0) {
    return false
  }

  let dmg = 0.1
  try {
    dmg = Number(data.getMeleeDamage())
  } catch (e1) {}
  if (!isFinite(dmg) || dmg <= 0) dmg = 0.1

  const src = playerAttackSource(attacker)
  if (!src) return false

  clearIFrames(target)
  const living = mcEntity(target)

  try {
    if (typeof target.attack === 'function') {
      target.attack(src, dmg)
      return true
    }
  } catch (e2) {}
  try {
    if (typeof living.hurt === 'function') {
      living.hurt(src, dmg)
      return true
    }
  } catch (e3) {}
  try {
    if (typeof living.m_6469_ === 'function') {
      living.m_6469_(src, dmg)
      return true
    }
  } catch (e4) {
    console.error('[mohist-melee-fix] forceMeleeHit failed: ' + e4)
  }
  return false
}

function finishFallback(attacker, target, id) {
  try {
    const pending = pendingAttacks[id]
    delete pendingAttacks[id]
    if (!pending) return
    if (!target || !isAlive(target)) return

    // Normal DMZ/vanilla hurt path worked.
    if (pending.hurtSeen && pending.hurtAmount > 0.01) return

    const hpNow = getHealth(target)
    if (isFinite(pending.hpBefore) && isFinite(hpNow) && hpNow < pending.hpBefore - 0.01) {
      return
    }

    if (forceMeleeHit(attacker, target)) {
      console.info(
        '[mohist-melee-fix] Forced melee for ' +
          playerName(attacker) +
          ' → #' +
          pending.targetId
      )
    }
  } catch (e) {
    console.error('[mohist-melee-fix] finishFallback failed: ' + e)
  }
}

PlayerEvents.loggedIn(event => {
  const player = event.player
  if (!player) return
  try {
    if (player.level.clientSide) return
  } catch (e) {}

  schedule(player, SYNC_DELAY_TICKS, () => {
    try {
      if (!player || !isAlive(player)) return
      loginPrime(player)
    } catch (e2) {
      console.error('[mohist-melee-fix] loginPrime failed: ' + e2)
    }
  })
})

NativeEvents.onEvent(LivingHurtEvent, event => {
  try {
    const srcEnt = event.getSource().getEntity()
    if (!srcEnt) return

    const id = uuidOf(srcEnt)
    const pending = pendingAttacks[id]
    if (!pending) return

    const tid = entityId(event.getEntity())
    if (pending.targetId !== tid) return

    pending.hurtSeen = true
    pending.hurtAmount = event.getAmount()
  } catch (e) {}
})

NativeEvents.onEvent(AttackEntityEvent, event => {
  try {
    const attacker = event.getEntity()
    const target = event.getTarget()
    if (!attacker || !target || !isLiving(target)) return

    const data = getDmzData(attacker)
    if (!data) return
    try {
      if (!data.getStatus().isHasCreatedCharacter()) return
    } catch (e0) {
      return
    }

    const id = uuidOf(attacker)
    pendingAttacks[id] = {
      targetId: entityId(target),
      hurtSeen: false,
      hurtAmount: 0,
      hpBefore: getHealth(target)
    }

    if (
      !schedule(attacker, FALLBACK_CHECK_TICKS, () => {
        finishFallback(attacker, target, id)
      })
    ) {
      // Could not schedule — attempt immediate deferred check via setTimeout-like fail: skip
      delete pendingAttacks[id]
    }
  } catch (e) {
    console.error('[mohist-melee-fix] AttackEntityEvent failed: ' + e)
  }
})
