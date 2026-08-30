# End Dragon — Adaptive Difficulty summon (2.3.76)

## Player path (Difficulty GUI only)
- Button: **Summon End Dragon** on `/difficulty` main page
- Requirements:
  - Personal Adaptive Difficulty **ON**
  - Active Unlock Tier **T4–T7** (Prestige 4 unlocks T4 eligibility — still must buy/activate)
  - Standing in **The End**
  - Cost: **3 Ancient Netherite** coins (pay-up OK, change returned)
- Dragon is painted with the summoner's **Adaptive Difficulty boss profile**:
  - HP / ATK / Armor from `PlayerCombatProfile.targetMob*` × `bossStatMultiplier` × 1.25 dragon pad
  - Live retarget on form/stat changes (same signature path as nearby AD mobs)
  - **No** legacy End Strength HP log-curve or DEF sponge / hit-cap
- **Only the summoner** can damage it (anti-farm)
- Ki beam / blast use painted AD attack
- **Multi-player:** each eligible player may summon their own dragon. Spawn is **near the summoner** (player position + ~16 Y), including off the main island — not forced to `0,0`.
- Hygiene keeps **at most one living dragon per summoner** and never culls another player's fight.

## Staff
- **No staff spawn** — `/enddragon`, `/enddragon spawn`, and `/spawndragon` are denied.
- Clear: `/cleardragons`, `/killdragons`, `/enddragon clear|cleanup`
- Repair exit podium: `/enddragon repair`
- Bare `/enddragon` prints staff usage help

## Config
- `enableEndPlayerDragonSummon` (default true)
- `endDragonSummonNetheriteCost` (default 3)
- `enableEndNaturalDragonSpawn` (default **false** — GUI is the player path)
- `endEnforceSingleDragon` (default true) — per-summoner cap + strip unauthorized vanilla dragons
- `bossStatMultiplier` (default 1.5) — shared with AD bosses

## Fight AI + AD isolation
- Player-summoned dragons focus the summoner: faster ki cadence, strafe/charge phase steering, optional beam+blast combo.
- While your summoned dragon is alive, **nearby Adaptive Difficulty mob scaling is suspended** for you (reverts claimed mobs).

## Despawn
- Despawn when the summoner: turns personal difficulty **OFF**, **dies**, goes offline, or stops participating in AD.
- Auto / vanilla End Dragon spawning is **removed**.

## Loot (Simply Swords / Simply More) + XP
- Unique weapon injections on `minecraft:entities/ender_dragon` are **preserved**.
- LM never clears `LivingDropsEvent` for End dragons.
- AD zero-HP force-kill is skipped for Ender Dragons (`EnderDragon.kill()` would strip loot/XP).
- On player-summon kills, vanilla dragon **XP** (12 000 first End kill / 500 after) is awarded at the **death position** (including off-island). The dying animation is then stopped so XP is not double-dropped at the main island.
- If the loot table somehow does not roll on death, LM falls back once at the death position (no double-drop when vanilla already rolled).
