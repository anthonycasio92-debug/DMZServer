# DMZ Adaptive Difficulty (v3.3.33)

**Server-side only** Forge mod for Mohist/Forge 1.20.1.  
Clients do **not** need this jar to join.

## Model

| System | Behavior |
|--------|----------|
| Unlock tiers (1–7) | Unlocked by DMZ level **or** Prestige ≥ tier id |
| Buy tier | Spend Ancient Coins (pay-up OK with lower/higher mix, no change) |
| Active difficulty | Set only by tier purchase — no +difficulty upgrades |
| Personal toggle | GUI on/off for that player only (default ON). OFF freezes scaling, kill coins, AI pressure, and tier buy/lower until turned back on |
| Coin drop chat | GUI mute for "Dropped X Ancient Coins" (default OFF) |
| Death | Clears active tier + level when personal ON (unlocks / prestige / coins kept). Disconnect / logout keeps the purchased tier |
| Nearby scaling | Hostiles scale to player post-transform stats × tier % (max 5/player); leave range / personal off / logout reverts stats **and** strips elite/mutation/boss nameplates so they are normal mobs again. Rarity rolls once per mob (no leave/re-enter farming) |
| Combat Rating | Rewards / display / area readouts (not mob HP) |
| Teams | WIP stub — personal difficulty only |
| Titles | Restored; equip from GUI |
| Kill rewards | Personal ON: pre-T1 drops 1× Copper; with tier, normal ladder drops on the ground |
| Upgrade drop | 2% chance for +1 next-higher coin (e.g. Copper + rare Iron) |
| Feature gates | T1+ AI/evo kits by unlock band · T4 elite · T5 mutation · T6 boss · T7 Zenith ceiling |

## Install

1. `mods/dmz_adaptive_difficulty-3.3.33.jar` (remove older AD jars)
2. `plugins/dmz_adaptive_difficulty_gui-3.3.31.jar`
3. Restart — config regenerates at `config/dmz_adaptive_difficulty.json`
4. `/difficulty` → hub: Buy / Lower / Titles + personal & coin-chat toggles (Details is ops-only)

## Commands

- `/difficulty` — GUI
- `/difficulty admin off|on|toggle|status` — master system switch (ops / staff)
- `/difficulty admin reload|settings|set <key> <value>`
- Actions: `activate <1-7>`, `reset`, `character_reset`, title equip, `toggle_personal`, `toggle_coin_chat`

When disabled: no mob scaling, kill coins, AI, death reset, or tier purchases.
Config key: `enabled` (also `admin set enabled false`).

Staff gate: op level ≥2 / Bukkit op, or `difficulty.admin` (config `adminPermission`).
Live `admin set` is allowlisted + clamped; change `adminPermission` only in JSON + reload.

### Testing whitelist
- `/difficulty admin whitelist on|off|toggle`
- `/difficulty admin whitelist add <player>` / `remove <player>` / `list` / `clear`
- Alias: `wl`
- When on, only listed players use AD (scaling, coins, purchases). Persists in config.

## Notes

- The End scales hostiles like other dimensions. The **Ender Dragon** is hard-exempt (End Strength script). To disable the whole End again, add `minecraft:the_end` to `disabledDimensions`.
- Ancient Coins are real Lightman's `coin_ancient` items. Tier buys spend from **inventory first, then the equipped wallet** — never from Lightman's bank. Pay-up allowed (no change returned).
- Coin ladder values: Copper 1 → Iron 10 → Gold 100 → Emerald 1k → Diamond 10k → Netherite 100k (9 letter variants equal). Lapis / Ender Pearl unused.
- Example: a 1× Iron cost can be paid with 10+ Copper, or 1× Gold (overpay, no change).
- Tier costs cap at 128 of one coin type, then promote to the next denomination (rounded up; top = 128× Netherite).
- Level scaling uses levels above each tier’s unlock requirement (buying T7 at unlock ≈ base cost, not 100×). Higher tiers are always ≥ ~25% more than the previous tier’s cost for the same player.
- Saga/quest entities are exempt by class (`DBSagasEntity`) and by tags (`dmz_quest_*` / `dmz_saga_id`). Vanilla cage spawners, SDD Advanced Spawner mobs (`sdd_spawner` / `sdd_boss`), the Ender Dragon, and **slime/magma cube split children** are also never AD-converted. Transform forms keep quest HP — AD never rolls them back to the entity-default 300 max health.
- Mob damage uses blended offense + a DEF/HP tank floor + specialization tax so 1–3 stat dumps cannot shrug tiered hits vs even builds.
- Tier scale vs player fight stats: T1 13% · T2 28% · T3 42% · T4 58% · T5 76% · T6 90% · T7 116%. High-DEF / tank builds always get a pierce floor so DMZ mitigation cannot zero mob hits.
- Mob ki blasts/lasers/beams use the mob’s scaled attack damage (not vanilla 3–72), so DMZ DEF mitigation no longer cancels them to 0.
- **Class/race counters:** mobs also press DMZ fighting classes (warrior/berserker melee, martial artist strike, spiritualist/cleric ki, tank/paladin) and mild race overlays (saiyan/majin/namekian/…). Toggle: `enableClassCounters`.
- Scaled mob HP defaults to 50% of the previous match (`mobHealthScale` in config; damage unchanged).
- Old NBT wallet balances migrate into Copper Ancient coins once per login.
- AI + Enemy Evolution kits deepen with Buy Tier but stay **silent** (no kit nameplate/glow). Only true rarity rolls get cosmetics: Elite (`✦ Elite …`), Mutation (`§d…`), Boss (`☠ Boss …`). Soft floors: T1 Awakened · T2 Enhanced · T3 Elite kit · T4 Advanced · T5 Master · T6 Legendary · T7 God (ceiling up to Zenith). Stock rarity chances: elite 0.75%, mutation 1.25%.
