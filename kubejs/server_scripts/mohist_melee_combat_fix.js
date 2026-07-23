/**
 * Mohist + DragonMineZ: melee deals no damage until the player dies once.
 *
 * On login, re-run the same attribute refresh DMZ uses after respawn
 * (LivingEntity.refreshAttributes / m_6210_), and clear join i-frames.
 *
 * Intentionally does NOT: gamemode flicker, teleport, kill, or touch keepInventory.
 *
 * See docs/MOHIST_MELEE_BUG.md
 */

const PRIME_DELAY_TICKS = 40
const TAG_PRIMED = 'dmz_mohist_melee_primed'

function pd(player) {
  return player.persistentData
}

function playerName(player) {
  return player.username || player.name.string
}

function mcEntity(player) {
  try {
    if (player.minecraftPlayer) return player.minecraftPlayer
  } catch (e0) {}
  try {
    if (player.minecraftEntity) return player.minecraftEntity
  } catch (e1) {}
  return player
}

/** Same call DMZ makes on respawn / login: LivingEntity.refreshAttributes(). */
function refreshAttributes(player) {
  const ent = mcEntity(player)
  if (typeof ent.refreshAttributes === 'function') {
    ent.refreshAttributes()
    return 'refreshAttributes'
  }
  // SRG name on Mohist (Mojang mappings: false)
  if (typeof ent.m_6210_ === 'function') {
    ent.m_6210_()
    return 'm_6210_'
  }
  return null
}

function resetAttackStrength(player) {
  const ent = mcEntity(player)
  if (typeof ent.resetAttackStrengthTicker === 'function') {
    ent.resetAttackStrengthTicker()
    return
  }
  // LivingEntity.resetAttackStrengthTicker SRG
  if (typeof ent.m_36334_ === 'function') {
    ent.m_36334_()
  }
}

function clearJoinIFrames(player) {
  try {
    player.invulnerableTime = 0
  } catch (e0) {}
  try {
    const ent = mcEntity(player)
    if (ent.invulnerableDuration !== undefined) ent.invulnerableDuration = 0
    if (ent.invulnerableTime !== undefined) ent.invulnerableTime = 0
    if (ent.hurtTime !== undefined) ent.hurtTime = 0
  } catch (e1) {}
}

function reassertHealth(player) {
  try {
    const hp = player.health
    const max = player.maxHealth
    if (max > 0) {
      player.health = Math.min(hp > 0 ? hp : max, max)
    }
  } catch (e) {}
}

function clearDmzMeleeRateLimit(player) {
  // CombatAttackRequestC2S stores dmz_last_melee_attack_time on entity persistent data.
  try {
    const ent = mcEntity(player)
    if (ent.getPersistentData && typeof ent.getPersistentData === 'function') {
      const tag = ent.getPersistentData()
      if (tag && typeof tag.remove === 'function') {
        tag.remove('dmz_last_melee_attack_time')
        tag.remove('dmz_first_hit')
        tag.remove('dmz_swing_stamina_ratio')
      } else if (tag && typeof tag.m_128473_ === 'function') {
        tag.m_128473_('dmz_last_melee_attack_time')
        tag.m_128473_('dmz_first_hit')
        tag.m_128473_('dmz_swing_stamina_ratio')
      }
    }
  } catch (e) {}
}

function primeCombat(player) {
  clearJoinIFrames(player)
  const how = refreshAttributes(player)
  resetAttackStrength(player)
  reassertHealth(player)
  clearDmzMeleeRateLimit(player)
  return how
}

PlayerEvents.loggedIn(event => {
  const player = event.player
  if (!player) return
  try {
    if (player.level.clientSide) return
  } catch (e) {}

  try {
    pd(player).putBoolean(TAG_PRIMED, false)
  } catch (e2) {}

  player.server.scheduleInTicks(PRIME_DELAY_TICKS, () => {
    try {
      if (!player || !player.isAlive()) return
      try {
        if (pd(player).getBoolean(TAG_PRIMED)) return
      } catch (e0) {}

      const how = primeCombat(player)
      try {
        pd(player).putBoolean(TAG_PRIMED, true)
      } catch (e1) {}

      console.info(
        '[mohist-melee-fix] Primed melee for ' +
          playerName(player) +
          (how ? ' via ' + how : ' (attribute refresh unavailable)')
      )
    } catch (e3) {
      console.error('[mohist-melee-fix] prime failed: ' + e3)
    }
  })
})
