# AdaptiveDifficulty (v1.0.9)

**Server-side only** Forge mod for Mohist/Forge 1.20.1.  
Clients do **not** need this jar to join.

Java package stays `com.dbzlegacy.adaptivedifficulty` (GUI reflection ABI). Player NBT root `dmz_adaptive_difficulty` and PlaceholderAPI `%dmzdiff_*%` are unchanged.

## Model

| System | Behavior |
|--------|----------|
| Unlock tiers (1–7) | Live gate: current DMZ level **or** Prestige ≥ tier id. Prestige-up / level reset revokes tiers you no longer qualify for (coins alone cannot keep them). |
| Buy tier | Spend Ancient Coins (pay-up OK with lower/higher mix; overpay returned as change) |
| Active difficulty | Set only by tier purchase — no +difficulty upgrades |
| Personal toggle | GUI on/off for that player only (default ON). OFF freezes scaling, kill coins, AI pressure, and tier buy/lower until turned back on |
| Coin drop chat | GUI mute for "Dropped X Ancient Coins" (default OFF) |
| Death | Clears active tier + level whenever the system allows the player (personal OFF cannot skip). Unlocks / prestige / coins kept. Disconnect / logout keeps the purchased tier |
| Nearby scaling | Hostiles scale to **STR/SKP** × tier % for damage and soft **VIT** for HP (max 5/player). **PWR/ENE never scaled against.** Mob damage is VIT-capped so higher tiers pressure **ki protection** without one-punch bag dumps; mild durability floor keeps mega forms from vaporizing packs without becoming HP walls. Form soft-curve on STR/SKP only. Tuned from a full sim of `config/dragonminez/races/*` (10 races / 106 forms). Leave range / personal off / logout reverts stats **and** strips elite/mutation/boss nameplates. |
| Combat Rating | Rewards / display / area readouts (not mob HP) |
| Teams | WIP stub — personal difficulty only |
| Titles | Restored; equip from GUI |
| Kill rewards | Personal ON: pre-T1 drops 1× Copper; with tier, normal ladder drops on the ground |
| Upgrade drop | 2% chance for +1 next-higher coin (e.g. Copper + rare Iron) |
| Feature gates | T1+ AI/evo kits by unlock band · T4 elite · T5 mutation · T6 boss · T7 Zenith ceiling |

## Install

1. `mods/AdaptiveDifficulty-1.0.9.jar` (remove older AD jars)
2. `plugins/AdaptiveDifficultyGUI-1.0.9.jar`
3. Restart — config at `config/adaptivedifficulty.json` (auto-migrates from `dmz_adaptive_difficulty.json`)
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

## Adding a new race (future-proof)

AD is **race-agnostic** — it never hardcodes race ids. Drop a new folder under `config/dragonminez/races/<id>/` with `character.json`, `stats.json`, and `forms/*.json` (DMZ loads it; AD reads live STR/SKP/VIT/RES + form⊕stack).

Checklist so new races stay balanced:
1. Set `character.json` → `raceName` to the **same** folder id (form mult lookup uses it).
2. Put combat power in `strMultiplier` / `skpMultiplier` / `defMultiplier` / `vitMultiplier` (and mastery `maxStatsMultiplier` if desired). **PWR/ENE are ignored by AD.**
3. Prefer DMZ form multipliers over baking power only into BonusStats/attributes (AD still has baselines + form-key polling as fallback).
4. Re-run the sim after adding forms:  
   `python3 tools/dmz-adaptive-difficulty/sim/simulate_race_forms.py`  
   CI-style: `python3 tools/dmz-adaptive-difficulty/sim/simulate_race_forms.py --check`
5. Optional clamps (already safe by default): `maxFormBoost` (100), `maxLiveCombatChannel` (5e7).

Runtime guards for unknown races: NaN/absurd form&stat clamps, race-tagged baselines (cleared on race swap), active form-key polling when FormChangeEvent is missing, fail-soft profile build.

## Notes

- The End scales hostiles like other dimensions. The **Ender Dragon** is hard-exempt (End Strength script). To disable the whole End again, add `minecraft:the_end` to `disabledDimensions`.
- Ancient Coins are real Lightman's `coin_ancient` items. Tier buys spend from **inventory first, then the equipped wallet** — never from Lightman's bank. Pay-up allowed; overpay is returned as Ancient Coin change.
- Coin ladder values: Copper 1 → Iron 10 → Gold 100 → Emerald 1k → Diamond 10k → Netherite 100k (9 letter variants equal). Lapis / Ender Pearl unused.
- Example: a 1× Iron cost can be paid with 10+ Copper, or 1× Gold (overpay returned as change).
- Tier costs cap at 128 of one coin type, then promote to the next denomination (rounded up; top = 128× Netherite).
- Stock tier costs (copper-value at unlock): T1 **1× Copper** at DMZ level 1 · T2 5 · T3 15 · T4 50 · T5 150 · T6 500 · T7 1500. Level scaling uses levels above each tier’s unlock requirement (buying at unlock ≈ base). Higher tiers are always ≥ ~25% more than the previous tier’s cost for the same player. Buy prices / unlock gates use a **base-form** DMZ level — transforming must not change the cost.
- Saga/quest entities are exempt by class (`DBSagasEntity`) and by tags (`dmz_quest_*` / `dmz_saga_id`). Vanilla cage spawners, SDD Advanced Spawner mobs (`sdd_spawner` / `sdd_boss`), the Ender Dragon, and **slime/magma cube split children** are also never AD-converted. Transform forms keep quest HP — AD never rolls them back to the entity-default 300 max health.
- Mob damage uses soft-blended STR/SKP × tier%, then a VIT-relative hit cap (ki-protection friendly). Counters (class + top STR/SKP/RES/VIT) ramp with tier%.
- Tier scale vs player fight stats: T1 21% · T2 42% · T3 65% · T4 90% · T5 135% · T6 160% · T7 200%. Form soft curve stock: `transformScaleWeight` 0.55 · `transformScaleExponent` 0.75.
- Side channels (creeper boom, gravity, early kiblasts) also follow tier%/unlock — not absolute offense proxy. Creeper explosions bake from painted attack damage (like kiblasts) so T1–T2 blasts are not cancelled to 0 by DMZ DEF.
- Mob ki blasts/lasers/beams use the mob’s scaled attack damage (not vanilla 3–72), so DMZ DEF mitigation no longer cancels them to 0. Early unlock kiblasts are soft-ratio’d (~60% at T1–T2).
- **Counters (light):** class + single top combat stat only; intensity scales with tier%. No race / weak / top-3 / specialization stacks. Toggles: `enableClassCounters`, `enableStrongStatCounters`.
- Scaled mob HP defaults to 65% of the previous match (`mobHealthScale`; damage unchanged). Elite rarity mult stock 1.50×.
- Old NBT wallet balances migrate into Copper Ancient coins once per login.
- AI + Enemy Evolution kits deepen with Buy Tier but stay **silent** (no kit nameplate/glow). Only true rarity rolls get cosmetics: Elite (`✦ Elite …`), Mutation (`§d…`), Boss (`☠ Boss …`). Kit bands: T1 Awakened · T2 Awakened→Enhanced · T3 Enhanced→Elite · T4 Advanced→Master · T5 Master→Divine · T6 Legendary→Mythic · T7 God→Zenith. Stock rarity chances: elite **2.25%**, mutation **3.75%** (bosses are natural T6+ hostiles — no % roll).
