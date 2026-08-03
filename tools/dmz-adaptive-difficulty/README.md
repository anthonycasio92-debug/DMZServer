# DMZ Adaptive Difficulty (v3.3.46)

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
| Nearby scaling | Hostiles scale to player combat stats × tier % (max 5/player). Form boost uses a soft curve (`transformScaleWeight` 0.25 + `transformScaleExponent` 0.50, further dampened at high tiers) so transforming does not linearly explode mob damage. Leave range / personal off / logout reverts stats **and** strips elite/mutation/boss nameplates so they are normal mobs again. Rarity rolls once per mob (no leave/re-enter farming) |
| Combat Rating | Rewards / display / area readouts (not mob HP) |
| Teams | WIP stub — personal difficulty only |
| Titles | Restored; equip from GUI |
| Kill rewards | Personal ON: pre-T1 drops 1× Copper; with tier, normal ladder drops on the ground |
| Upgrade drop | 2% chance for +1 next-higher coin (e.g. Copper + rare Iron) |
| Feature gates | T1+ AI/evo kits by unlock band · T4 elite · T5 mutation · T6 boss · T7 Zenith ceiling |

## Install

1. `mods/dmz_adaptive_difficulty-3.3.46.jar` (remove older AD jars)
2. `plugins/dmz_adaptive_difficulty_gui-3.3.46.jar`
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
- Mob damage uses blended offense + a DEF/HP tank pierce floor so high mitigation cannot zero hits. Pierce / counters ramp with tier% (full by ~50%), so T2 20% stays near 20%.
- Tier scale vs player fight stats: T1 10% · T2 20% · T3 30% · T4 40% · T5 50% · T6 65% · T7 90%.
- Side channels (creeper boom, gravity, early kiblasts) also follow tier%/unlock — not absolute offense proxy. Creeper explosions bake from painted attack damage (like kiblasts) so T1–T2 blasts are not cancelled to 0 by DMZ DEF.
- Mob ki blasts/lasers/beams use the mob’s scaled attack damage (not vanilla 3–72), so DMZ DEF mitigation no longer cancels them to 0. Early unlock kiblasts are soft-ratio’d (~60% at T1–T2).
- **Counters (light):** class + single top combat stat only; intensity scales with tier%. No race / weak / top-3 / specialization stacks. Toggles: `enableClassCounters`, `enableStrongStatCounters`.
- Scaled mob HP defaults to 65% of the previous match (`mobHealthScale`; damage unchanged). Elite rarity mult stock 1.50×.
- Old NBT wallet balances migrate into Copper Ancient coins once per login.
- AI + Enemy Evolution kits deepen with Buy Tier but stay **silent** (no kit nameplate/glow). Only true rarity rolls get cosmetics: Elite (`✦ Elite …`), Mutation (`§d…`), Boss (`☠ Boss …`). Soft floors: T1 Awakened · T2 Awakened→Enhanced · T3 Enhanced→Elite · T4 Advanced · T5 Master · T6 Legendary · T7 God (ceiling up to Zenith). Stock rarity chances: elite **2.25%**, mutation **3.75%** (bosses are natural T6+ hostiles — no % roll).
