# DMZ Adaptive Difficulty (v3.3.0)

**Server-side only** Forge mod for Mohist/Forge 1.20.1.  
Clients do **not** need this jar to join.

## Model

| System | Behavior |
|--------|----------|
| Unlock tiers (1–7) | Unlocked by DMZ level **or** Prestige ≥ tier id |
| Buy tier | Spend inventory Ancient Coins (exact payment, sets tier difficulty) |
| Active difficulty | Set only by tier purchase — no +difficulty upgrades |
| Death | Clears active tier + level (unlocks / prestige / coins kept) |
| Nearby scaling | Hostiles scale to player post-transform stats × tier % (max 5/player) |
| Combat Rating | Rewards / display / area readouts (not mob HP) |
| Teams | WIP stub — personal difficulty only |
| Titles | Restored; equip from GUI |
| Kill rewards | Ancient Coins **drop on the ground** at the mob (+ modest XP) |
| Feature gates | T2 evo → T3 AI → T4 elite → T5 mutation → T6 boss → T7 full |

## Removed / cleaned

- Training Point grants / TP multipliers
- Per-level +difficulty coin upgrades / set-max
- Iron-coin / Lightman's main-chain difficulty payments
- Free bootstrap coin grants
- Capsule / emerald / diamond kill inventory dumps
- Zenith ladder UI for players
- Admin-mode session toggle (staff use op / `difficulty.admin` directly)
- Empty mixin plumbing and dead CR spawn-bake path

## Install

1. `mods/dmz_adaptive_difficulty-3.3.0.jar` (remove older AD jars)
2. `plugins/dmz_adaptive_difficulty_gui-3.3.0.jar`
3. Restart — config regenerates at `config/dmz_adaptive_difficulty.json`
4. `/difficulty` → **Buy Tier**

## Commands

- `/difficulty` — GUI
- `/difficulty admin off|on|toggle|status` — master system switch (ops / staff)
- `/difficulty admin reload|settings|set <key> <value>`
- Actions: `activate <1-7>`, `reset`, `character_reset`, title equip

When disabled: no mob scaling, kill coins, AI, death reset, or tier purchases.
Config key: `enabled` (also `admin set enabled false`).

### Testing whitelist
- `/difficulty admin whitelist on|off|toggle`
- `/difficulty admin whitelist add <player>` / `remove <player>` / `list` / `clear`
- Alias: `wl`
- When on, only listed players use AD (scaling, coins, purchases). Persists in config.

## Notes

- The End stays in `disabledDimensions` by default (End Strength script owns it).
- Ancient Coins are real Lightman's `coin_ancient` items (exact type/count, no change).
- Coin ladder: Copper → Iron → Gold → Emerald → Diamond → Netherite (9 letter variants, equal value). Lapis / Ender Pearl unused.
- Tier costs cap at 128 of one coin type, then promote to the next denomination (rounded up).
- Saga/quest-spawned mobs (`dmz_quest_*` / `dmz_saga_id`) and cage-spawner mobs are never AD-converted.
- Mob damage uses blended offense + a DEF/HP tank floor + specialization tax so 1–3 stat dumps cannot shrug tiered hits vs even builds.
- Old NBT wallet balances migrate into Copper Ancient coins once per login.
