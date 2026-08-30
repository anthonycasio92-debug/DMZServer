# End Dragon — Adaptive Difficulty summon (2.3.68)

## Player path (Difficulty GUI only)
- Button: **Summon End Dragon** on `/difficulty` main page
- Requirements:
  - Personal Adaptive Difficulty **ON**
  - Active Unlock Tier **T4–T7** (Prestige 4 unlocks T4 eligibility — still must buy/activate)
  - Standing in **The End**
  - Cost: **3 Ancient Netherite** coins (pay-up OK, change returned)
- Dragon scales to the summoner's **AD profile** (stats × tier %)
- **Only the summoner** can damage it (anti-farm)
- Same ki beam / blast attacks as before; prefer targeting the summoner

## Staff
- `/enddragon` remains a free staff override (no summoner lock)
- `/cleardragons` unchanged

## Config
- `enableEndPlayerDragonSummon` (default true)
- `endDragonSummonNetheriteCost` (default 3)
- `enableEndNaturalDragonSpawn` (default **false** — GUI is the player path)

## Live summoner AD scale & despawn (2.3.68)
- Dragon tracks the **summoner's** Adaptive Difficulty profile live (forms in/out), like nearby AD mobs.
- **Other players cannot** change its scale or damage it.
- Despawn when the summoner: turns personal difficulty **OFF**, **dies**, goes offline, or stops participating in AD.


## No natural spawn (2.3.68)
- Auto / vanilla End Dragon spawning is **removed**.
- Only Difficulty GUI summons and staff `/enddragon` can create a dragon.
- Config `enableEndNaturalDragonSpawn` is forced **false** on load.
