# LegacyMechanics (v2.3.54)

**Server-side only** Forge mod for Mohist/Forge 1.20.1.  
Clients do **not** need this jar to join.

**CNPC scripts are not required.** This mod replaces the old CustomNPC pack —
see `CNPC-FREE.md` (tags + Forge commands). Repo script folders are backups only.

Java package stays `com.dbzlegacy.adaptivedifficulty` (GUI reflection ABI). Player NBT root `dmz_adaptive_difficulty` and PlaceholderAPI `%dmzdiff_*%` are unchanged.

**Audits** (run after build / before ship):

```bash
python3 tools/dmz-adaptive-difficulty/sim/audit_gui_abi.py   # Forge↔GUI reflection + version handshake
python3 tools/dmz-adaptive-difficulty/sim/audit_features.py  # intended product features vs source
python3 tools/dmz-adaptive-difficulty/sim/validate_scaling.py # PWR/ENE + class/top-2 combat sim
python3 tools/dmz-adaptive-difficulty/sim/simulate_race_forms.py --check  # full race/form pack
python3 tools/dmz-adaptive-difficulty/sim/validate_tier_costs.py  # buy-cost ladder at unlock / 100k
```

Keep Forge `AdaptiveDifficultyMod.VERSION` = GUI `plugin.yml` version (both jars same).  
Do not rename the package, `DifficultyCache` / `DifficultyActions` / snapshot fields, or Bukkit `openMenu*` / `openChestMenu*` entrypoints.

## Model

| System | Behavior |
|--------|----------|
| Unlock tiers (1–7) | Live gate: current DMZ level **or** Prestige ≥ tier id. Prestige-up / level reset revokes tiers you no longer qualify for (coins alone cannot keep them). |
| Buy tier | Spend Ancient Coins (pay-up OK with lower/higher mix; overpay returned as change) |
| Active difficulty | Set only by tier purchase — no +difficulty upgrades |
| Personal toggle | GUI on/off for that player only (default OFF). Every server restart forces personal OFF on first login that boot; mid-session reconnect keeps the toggle. OFF freezes scaling, kill coins, AI pressure, and tier buy/lower until turned back on |
| Coin drop chat | GUI mute for "Dropped X Ancient Coins" (default OFF) |
| Death | Clears active tier + level whenever the system allows the player (personal OFF cannot skip). Unlocks / prestige / coins kept. Disconnect / logout keeps the purchased tier |
| Nearby scaling | Hostiles scale to soft-blended **STR/SKP/PWR** (+ mild **ENE** pool) × tier % for damage and soft **VIT** for HP (max 5/player). **VIT/RES damage floors** press tank dumps; **Ki Infusion / Potential Unlock** raise pack sponge; **class counters** + **top-2 combat stats** (STR/SKP/RES/VIT/PWR/ENE) ramp with tier%. Mob damage is VIT-capped (raised budgets) so higher tiers pressure **ki protection** without one-punch bag dumps; stronger durability sponge keeps STR/PWR dumps from vaporizing packs. Form soft-curve on STR/SKP/PWR/ENE. Leave range / personal off / logout reverts stats **and** strips elite/mutation/boss nameplates. |
| Combat Rating | Rewards / display / area readouts (not mob HP) |
| Teams | WIP stub — personal difficulty only |
| Titles | Restored; equip from GUI |
| Kill rewards | Personal ON: **5%** chance per eligible kill — pre-T1 Copper; with tier, normal ladder |
| Upgrade drop | **0.5%** dual drop: original coin + next-higher (e.g. Copper + Iron) |
| Feature gates | T1+ AI/evo kits by unlock band · T4 elite · T5 mutation · T6 boss · T7 Zenith ceiling |

## Install

1. `mods/LegacyMechanics-2.3.54.jar` (remove older AD jars)
2. `plugins/LegacyMechanicsGUI-2.3.54.jar` (or matching GUI if available)
3. **Do not load CNPC Global Player / NPC scripts** for systems this mod owns (`CNPC-FREE.md`)
4. Tag GUI NPCs (`lm_rival`, `lm_spar`, `lm_skillcheck`, …) or use slash commands
5. Restart — config at `config/legacymechanics.json` (auto-migrates from `dmz_adaptive_difficulty.json`)
6. `/difficulty` → hub: Buy / Lower / Titles + personal & coin-chat toggles (Details is ops-only)

## Commands

- `/difficulty` — GUI
- `/lm admin clear <player> [all|rival|spar|difficulty|progression]` — staff wipe of one player's LM data
- `/lm admin migrate-cnpc` / `force` — import CNPC Rival/Spar into LM stores
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
2. Put combat power in `strMultiplier` / `skpMultiplier` / `pwrMultiplier` / `eneMultiplier` / `defMultiplier` / `vitMultiplier` (and mastery `maxStatsMultiplier` if desired). AD scales against STR/SKP/PWR (+ mild ENE) and counters top-2 of all six stats.
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
- Stock tier costs (copper-value at unlock): T1 **1× Copper** at DMZ level 1 · T2 5 · T3 15 · T4 50 · T5 150 · T6 500 · T7 1500. Level scaling uses levels above each tier’s unlock requirement (buying at unlock ≈ base). Stock `tierCostLevelDivisor` is **50000** (was 1000) so a DMZ 100k player pays ~15× Gold for T7 instead of a ladder-inflated Emerald pile. Higher tiers are always ≥ ~25% more than the previous tier’s cost for the same player. Buy prices / unlock gates use a **base-form** DMZ level — transforming must not change the cost.
- Saga/quest entities are exempt by class (`DBSagasEntity`) and by tags (`dmz_quest_*` / `dmz_saga_id`). Vanilla cage spawners, SDD Advanced Spawner mobs (`sdd_spawner` / `sdd_boss`), the Ender Dragon, and **slime/magma cube split children** are also never AD-converted. Transform forms keep quest HP — AD never rolls them back to the entity-default 300 max health.
- **God forms:** hit-cap blends soft↔live HP and pulls live-offense pressure so transforms cannot out-tank packs after DMZ DEF. T4+ also pierces DMZ’s hard cancel (`flatMit ≥ dmg×2.5`) using live `getFlatMitigation()` (DEF form), with a LivingDamageEvent safety net that restores tier-scaled landing damage if a hit is still zeroed (SSJB knockback-with-0-damage fix).
### Economy (2.3.49)
Kill coin drops are chance-gated (was always-on):
- `ancientCoinDropChance` **0.05** (5% any coin)
- `ancientCoinUpgradeChance` **0.005** (0.5% original + next-higher dual)
Existing configs auto-migrate stock 2% upgrade → 0.5%.

### Live balance + fixes (1.0.35)
From `hits-2026-08-06..08.jsonl` (14770 hits) + reported bugs:
- **DMZ level clamp:** `getLevel()` is a stat-progress estimate; AD now clamps to
  `min(DMZ maxValue, referenceMaxLevel)` and repairs inflated `highestDmzLevel` NBT
  so a 100k ceiling never displays as ~987k.
- **Skeleton mounts:** cancel rider↔mount damage; kits refuse mount targets (spider
  jockey / skeleton-horse self-kills).
- **Ladder:** T3 landFrac 0.30 · T4 0.37 (ease cliff) · T5 0.48 · T6 0.50 · T7 0.58;
  T7 nudge/liveShare up so T6≉T7. Soft-caps unchanged. Formula fingerprint **37**.

### Live balance (1.0.34)
T4/T5 landing pressure from Aug 6 hits (superseded landFrac by 1.0.35).

### Soft-cap ladder (1.0.33)
T3 soft-cap was **0.55 > T4 0.48**, so god forms got *easier* after buying T4.
Monotonic ceilings: **T1 0.40 · T2 0.43 · T3 0.46 · T4 0.50 · T5 0.52 · T6 0.58 · T7 0.62**.

### Ghast aim (1.0.31)
Large / laser / beam kits no longer use a multi-tick cast that lets Ghast flight
AI overwrite look before `fireHability`. Shots fire instantly and use
`launchToward` (same as small blasts). `ghastTick` also re-faces the player every
tick so vanilla charge + kits stay aimed.

### Economy (1.0.30)
Stock `mobHealthScale` **→ 0.75** (was 1.15, briefly 0.92). Mob damage,
defense, and other attrs are unchanged — only max HP / TP-from-kill pace.

### Economy (1.0.29)
Stock `mobHealthScale` 1.15 → 0.92 (superseded by 1.0.30 → 0.75).

### Live balance (1.0.28)
Retuned from claimed hits in `hits-2026-08-05.jsonl` (8821 hits):
- Raise T1–T2 landing / hit-cap bite (T1 high-form packs were ~11% bag)
- Space soft-caps T5/T6/T7 → **0.52 / 0.58 / 0.62** (T6 was under T5 glue)
- Push T6 form nudge + live-offense share so the buy ladder climbs past T5

### UX (1.0.27)
Buy Tier GUI / chat now shows each locked tier’s real gate:
**DMZ level or Prestige N** (either one qualifies), plus your current DMZ/Prestige.
Example: T7 → `Need DMZ 100000 or Prestige 7`. Prestige is an alternate path, not required.

### Bugfix (1.0.26)
Tier buy costs at DMZ ~100k: stock `tierCostLevelDivisor` 1000 → 50000 so excess
scaling no longer makes T6 ≫ T7 at God unlock (monotonic floor was dragging T7 to
~33× Emerald). Buying T7 at unlock is ~15× Gold again.

### Bugfix (1.0.25)
Post-pierce mob ATK clamped to the live incoming soft-cap so high-DEF god forms
no longer paint hitFrac ≫ 0.75 (race/form audit). Soft-cap event path unchanged.

### Live balance (1.0.24)
Retuned from claimed hits in `hits-2026-08-04.jsonl` + `hits-2026-08-05.jsonl`:
- Fix T4 tank ladder dip (fill-to-landing + T4 hitFrac/nudge)
- Cap T1–T2 god landing (was ~38% bag on spiritualist)
- Progressive soft-caps T4→T7 (0.48→0.58)
- KP10 saves ~15% on landing path (was ~10%)

### Bugfix (1.0.23)
Ghost difficulty from unclaimed / personal-OFF fights:
- Landing safety net requires personal ON (`participates`)
- `isAdPainted` only true for claimed/profile-scaled mobs (not spawn-init shells)

### Behavior (1.0.22)
Personal difficulty starts **OFF** for everyone after each server restart (first login that boot). Mid-session reconnect keeps the toggle. New players default OFF.

### Bugfix (1.0.21)
Enderman/Warden combat gravity no longer sticks after the mob dies.
- Clear contributions on death (`CombatGravity.removeSource`)
- Hysteresis no longer skips unregister when gravity decays to ~0

### Live balance (1.0.20)
Second telemetry pass: high-DEF god forms lived on the **safety-net landing** path.
- Live-bag landing ladder (~12%→48% across T1–T7)
- T3 god-form DEF-cancel pierce (was T4+ only)
- Soft-caps: T5 50% · T6 55% · T7 52% live bag

### Live balance (1.0.19)
Tuned from combat telemetry (`hits-2026-08-04.jsonl`):
- Raise T1–T3 god-form pressure (soft floors + live-share + hitFrac)
- Stretch T4–T6 for tanks (`tankDamageHealthRatio` 0.22→0.28, nudges, live-share)
- Soft-cap crushing T5–T7 landings at 62/58/48% live bag (event clamp)

- **Balance telemetry (Bukkit owns `/difficulty` on Mohist):** `/difficulty admin telemetry on|off|status|flush|test` — when ON, logs AD hits for **all players** using the difficulty system to `config/legacymechanics/telemetry/hits-YYYY-MM-DD.jsonl` (rate-limited; not whitelist-gated). Use `test` to write a probe line and confirm the folder. Summarize with `sim/summarize_telemetry.py --dir <that folder>`.
- **Skills:** Ki Protection leaves mid/high-tier hits load-bearing (DMZ 1%/lvl mitigation); Ki Infusion / Potential Unlock raise pack HP sponge. Melee kits chase earlier and deal painted shock/slam damage so zombies are not toothless vs skeleton ki.
- Mob damage uses soft-blended STR/SKP/PWR (+ mild ENE) × tier%, then **VIT/RES floors** (`tankDamageHealthRatio` **0.28** / `tankDamageDefenseRatio` 0.45) so VIT dumps and tank class track the same ladder as even builds, then a raised VIT-relative hit cap (ki-protection friendly). Counters (class + top-2 of STR/SKP/RES/VIT/PWR/ENE) ramp with tier%.
- Tier scale vs player fight stats: T1 21% · T2 42% · T3 65% · T4 90% · T5 135% · T6 160% · T7 200%. Form soft curve stock: `transformScaleWeight` 0.65 · `transformScaleExponent` 0.75.
- Side channels (creeper boom, gravity, early kiblasts) also follow tier%/unlock — not absolute offense proxy. Creeper explosions bake from painted attack damage (like kiblasts) so T1–T2 blasts are not cancelled to 0 by DMZ DEF.
- Mob ki blasts/lasers/beams use the mob’s scaled attack damage (not vanilla 3–72), so DMZ DEF mitigation no longer cancels them to 0. Early unlock kiblasts are soft-ratio’d (~60% at T1–T2).
- **Counters:** fighting class + top-2 combat stats; intensity scales with tier%. No race / weak / specialization stacks. Toggles: `enableClassCounters`, `enableStrongStatCounters`.
- Scaled mob HP sponge stock **75%** (`mobHealthScale`, was 115%) with a stronger offense durability floor so glass STR/PWR dumps trade hits. Elite rarity mult stock 1.50×.
- Old NBT wallet balances migrate into Copper Ancient coins once per login.
- AI + Enemy Evolution kits deepen with Buy Tier but stay **silent** (no kit nameplate/glow). Only true rarity rolls get cosmetics: Elite (`✦ Elite …`), Mutation (`§d…`), Boss (`☠ Boss …`). Kit bands: T1 Awakened · T2 Awakened→Enhanced · T3 Enhanced→Elite · T4 Advanced→Master · T5 Master→Divine · T6 Legendary→Mythic · T7 God→Zenith. Stock rarity chances: elite **2.25%**, mutation **3.75%** (bosses are natural T6+ hostiles — no % roll).


See also [MECHANICS.md](MECHANICS.md) for Rival / Sparring / Difficulty hub docs.
