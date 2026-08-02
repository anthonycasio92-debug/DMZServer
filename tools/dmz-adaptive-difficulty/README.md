# DMZ Adaptive Difficulty (v3.3.20)

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
| Nearby scaling | Hostiles scale to player post-transform stats × tier % (max 5/player); leave range / personal off / logout reverts. Elite/mutation/boss rolls happen on claim using the claim owner's unlock tier |
| Combat Rating | Rewards / display / area readouts (not mob HP) |
| Teams | WIP stub — personal difficulty only |
| Titles | Restored; equip from GUI |
| Kill rewards | Personal ON: pre-T1 drops 1× Copper; with tier, normal ladder drops on the ground |
| Upgrade drop | 2% chance for +1 next-higher coin (e.g. Copper + rare Iron) |
| Feature gates | T1+ AI/evo kits by unlock band · T4 elite · T5 mutation · T6 boss · T7 Zenith ceiling |

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

1. `mods/dmz_adaptive_difficulty-3.3.20.jar` (remove older AD jars)
2. `plugins/dmz_adaptive_difficulty_gui-3.3.20.jar`
3. Restart — config regenerates at `config/dmz_adaptive_difficulty.json`
4. `/difficulty` → hub: Buy / Lower / Titles + personal & coin-chat toggles (Details is ops-only)

## Commands

- `/difficulty` — GUI
- `/difficulty admin off|on|toggle|status` — master system switch (ops / staff)
- `/difficulty admin reload|settings|set <key> <value>`
- Actions: `activate <1-7>`, `reset`, `character_reset`, title equip, `toggle_personal`, `toggle_coin_chat`

When disabled: no mob scaling, kill coins, AI, death reset, or tier purchases.
Config key: `enabled` (also `admin set enabled false`).

### Testing whitelist
- `/difficulty admin whitelist on|off|toggle`
- `/difficulty admin whitelist add <player>` / `remove <player>` / `list` / `clear`
- Alias: `wl`
- When on, only listed players use AD (scaling, coins, purchases). Persists in config.

## Security (staff settings / admin set)

Two issues that used to look like “auth gaps” — what they were, and what 3.3.16+ does:

### Unauthenticated settings page
- **Risk:** The settings/details chat page is status text for staff, but the Bukkit path `/dmzdiffgui settings` only required `dmzdiff.gui` (default true for all players). `openChatMenu` did not re-check staff, so a non-admin could open the settings/details view. That page itself cannot mutate config, but it leaked staff-facing status and was a footgun next to `admin set`.
- **Also:** Bukkit `isStaff` treated `permission("*")` as staff. Some permission plugins grant a wildcard-looking node broadly, which could widen settings/admin access beyond ops / `difficulty.admin`.
- **Fix:** Staff checks at `/dmzdiffgui`, inside `openChatMenu` (settings/stats/details → main for non-staff), Forge `StaffAccess` on chat settings, and Bukkit `isStaff` = op or `adminPermission` only (no `*`).

### Unsanitized Bukkit `admin set`
- **Risk:** Bukkit `ForgeBridge.adminSet` used reflection over almost every public config field (deny-list only). A compromised or careless staff account could set `eliteChancePercent=100`, uncapped damage multipliers, or previously `adminPermission` to a common node (privilege escalation for the next player who holds that node). Clamps were thin; Forge’s typed `adminSet` also skipped `sanitizeLive()`.
- **Fix:** Deny-by-default **allowlist** of known-safe scalar keys; `adminPermission` blocked from live set (edit JSON + reload); expanded clamps (chances 0–100, rarity mults, ki/damage percents, unlock mins 0–7, gui/vanilla enums); `sanitizeLive()` after both Bukkit and Forge sets; `adminPermission="*"` normalized away on load.

Staff gate remains: op level ≥2 / Bukkit op, or `difficulty.admin` (config `adminPermission`).

## Notes

- The End scales hostiles like other dimensions. The **Ender Dragon** is hard-exempt (End Strength script). To disable the whole End again, add `minecraft:the_end` to `disabledDimensions`.
- Ancient Coins are real Lightman's `coin_ancient` items. Tier buys spend from **inventory first, then the equipped wallet** — never from Lightman's bank. Pay-up allowed (no change returned).
- Coin ladder values: Copper 1 → Iron 10 → Gold 100 → Emerald 1k → Diamond 10k → Netherite 100k (9 letter variants equal). Lapis / Ender Pearl unused.
- Example: a 1× Iron cost can be paid with 10+ Copper, or 1× Gold (overpay, no change).
- Tier costs cap at 128 of one coin type, then promote to the next denomination (rounded up; top = 128× Netherite).
- Level scaling uses levels above each tier’s unlock requirement (buying T7 at unlock ≈ base cost, not 100×). Higher tiers are always ≥ ~25% more than the previous tier’s cost for the same player.
- Saga/quest entities are exempt by class (`DBSagasEntity`) and by tags (`dmz_quest_*` / `dmz_saga_id`). Vanilla cage spawners, SDD Advanced Spawner mobs (`sdd_spawner` / `sdd_boss`), the Ender Dragon, and **slime/magma cube split children** are also never AD-converted. Transform forms keep quest HP — AD never rolls them back to the entity-default 300 max health.
- Mob damage uses blended offense + a DEF/HP tank floor + specialization tax so 1–3 stat dumps cannot shrug tiered hits vs even builds.
- Scaled mob HP defaults to 50% of the previous match (`mobHealthScale` in config; damage unchanged).
- Old NBT wallet balances migrate into Copper Ancient coins once per login.
- AI + Enemy Evolution kits deepen with Buy Tier (nameplates show the kit, e.g. `§6Elite §fZombie`). Soft floors: T1 Awakened · T2 Enhanced · T3 Elite · T4 Advanced · T5 Master · T6 Legendary · T7 God (ceiling up to Zenith).
