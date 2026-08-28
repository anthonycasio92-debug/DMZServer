## Spar mentor pending invites (2.3.18)

- Mentor page mirrors Rival Actions: Invite / Ask / Pending(N) / Accept… / Decline… / Remove.
- Pending board shows IN+OUT heads; incoming click Accept, outgoing click Cancel.
- Invite TTL extended to 24h; `pending_invites` placeholder + SparGuiApi cards/args.
- Jars: `LegacyMechanics-2.3.18` / `LegacyMechanicsGUI-2.3.18`.

## Simplified GUI lore / Rival titles (2.3.17)

- Non-staff instructional tip lore gated via `GuiBoardHelper.tips` / `tipsList`.
- Rival inventory titles drop `Legacy Mechanics ·` prefix (Difficulty-style short titles).
- Jars: `LegacyMechanics-2.3.17` / `LegacyMechanicsGUI-2.3.17`.

## End Dimension Strength script parity (2.3.17)

- Stubbed CNPC `End Dimension Strength.js` + Forge trigger bridge (Java owns dragon).
- Spawn via EndDragonFight (`createNewDragon` + spike crystals) so perch/charge AI works.
- World-wide dragon rescale; script softcap tables; natural spawn first-boot delay.
- Jars: `LegacyMechanics-2.3.17` / `LegacyMechanicsGUI-2.3.17`.

## Potential Unlock script parity (2.3.17)

- Rewrote `PotentialProgression` to match `Potential.js`: movement tip, method-streak
  check-before-increment + switch tip, effective-weight-only mult, soft-cap/Guru messages.
- Stubbed CNPC `Potential.js` (was still live and could double-count vs the mod).

## Skill tooltips + Rival pending invites (2.3.5)

- Potential Unlock tips: taking damage (not blocking); Jump/Sprint marked Strength unlocked; Ki Control/Manipulation/Sense marked skill-saga gained.
- Rival GUI: **Pending** board shows incoming + outgoing declare invites (count on main).
- Jars: `LegacyMechanics-2.3.5` / `LegacyMechanicsGUI-2.3.5`.

## Silent Declared notify + Accept Mutual + Spar admin (2.3.4)

- Both Silent → Declared: both players get `[Rival] DECLARED` chat.
- Accept… lists Declared rivals and upgrades Declared → Mutual (also pending Declares).
- Spar Admin GUI is staff-only Save/Status/Reset Mentor CD (Help button removed).
- Jars: `LegacyMechanics-2.3.4` / `LegacyMechanicsGUI-2.3.4`.

## Rival admin staff-only board (2.3.3)

Admin GUI shows only Save / Refresh / Status (no Help tile; no Top/Stats/Challenge player pages).

## Rival list heads + actions/history (2.3.3)

- **List**: current rivals as player heads; hover shows status, tier, RP, W/L/D, presence
- **Actions**: declare / accept / decline / remove / silent in their own section
- **History**: previous (archived) rivals as heads with final stats
- Remove picker limited to current rivals (not all online players)

## Potential soft-cap + SkillCheck layout (2.3.3)

- Potential Unlock soft-stops at **10** (Guru gate); after unlocking to **11+**, training continues to **30**. Registers max 30 so DMZ allows post-Guru levels.
- SkillCheck / Skills reorganized: **Natural** first (Potential Unlock, Flight, Meditation, Jump, Sprint), then **Saga** (Defense Pen / Healing Red / Ki skills).
- Jump/Sprint show **invested STR** (total − race/class base) — matches SprintJump progression.
- Potential Unlock labeled clearly (not as a train skill); always shows `/30`.

## Meditation trial script parity (2.3.3)

`MeditationProgression` aligned to `Meditation new.js`:
- Random 15-minute trial rotation (avoid immediate repeat), persisted to
  `config/legacymechanics/meditation-trial.json`
- Announce block: ☯ MEDITATION TRIAL / Current Trial / Requirement / Changes in…
- 5-minute remaining warning
- Focus: progress during first 10s of charge, then release & re-charge
- Wrong-biome warnings independent of progress (10s delay, 60s hard CD)
- Same biomes + condition strings as the script
- Screen title on announce (extra vs chat-only script)
- CNPC `Meditation new.js` + `ChangeBiomeMED.js` stubbed (Java owns trials)

## Combat concept parity (2.3.3)

Sparring / Rival / Potential audited against CNPC scripts and critical gaps closed:

### Sparring
- BP curve restored to script v2 anchors (100K=2× … 100T=600×)
- Ki efficiency table matches script (beam 1.30, explosive 0.75, …)
- Momentum window refreshes on every hit
- Fully-mitigated ki floor (12), absorption in HP pool
- Friendly Fist: knockdown/lethal heal victim only + latch
- Clash TP only while both fighters clashing; styleBeam ticks

### Rival
- Challenge RP only for mutual pairs
- Forfeit: −10 RP to loser, no lose TP
- Distance mid-fight warns (no force-draw)
- Long-fight live score cadence (15s / 60s)
- Proving Grounds on-grounds / reclaim RP applied

### Potential
- Duplicate window checked before method-streak (script order)

## Rival challenge duration picker (2.3.3)

GUI challenge flow: pick rival → choose **1–10 minutes** (script parity with
`/challenge <player> [minutes]`). Forge `challenge_send` accepts `uuid@minutes`.

## Spar movement AFK gate (2.3.3)

Restored script parity from Sparring Tp System.js: both fighters must
**exchange damage AND keep moving**. Hits no longer satisfy the AFK gate alone
(fixes box-farm standing still). Ki charge / beam clash still hold both gates.

## End portal lag/fly false titles (2.3.3)

Title only when **on ground** in a real portal (silent cancel while flying / mid-air).
Feet point-samples replace AABB; pulse eject stays silent. CNPC script remains stubbed.

# LegacyMechanics audit (2026-08-27)

Scope: CNPC scripts in-repo, remote feature branches, and a live-server script pull.
**Canonical branch:** `cursor/legacymechanics-progression-c766` (PR #28) — consolidates
Rival/Spar, progression, Title/Admin inspect, Building TP / End portal guard,
GhostPartyHeal (melee 2.12.20), inventory GUIs, plus KubeJS packs from
`server-fixes-consolidated` and `dmz-dino-food-balance`.
Jars: `LegacyMechanics-2.3.3` / `LegacyMechanicsGUI-2.3.3` / `dmz_mohist_melee_fix-2.12.20`.

## Screen title deny feedback (2.3.3)

Blocked in-world actions (End portals, race lock) show a **screen title/subtitle**, not chat.
CNPC `Disable End Portals.js` is a no-op stub so it cannot double-fire chat or false positives.

## CNPC → mod data migration (2.3.3)

On first boot with CustomNPCs present, LegacyMechanics imports Rival/Spar/progression
from overworld CNPC `storeddata`, writes `config/legacymechanics/*.json`, backups raw
blobs to `cnpc-import-backup/`, then **clears** those CNPC world keys.

On each player's first login after install, spar bonds/streaks + flight/meditation/potential
progress keys copy into LM NBT / `sparring.json`, then those CNPC player keys are cleared.

| Guard | Path / key |
|-------|------------|
| World done | `config/legacymechanics/cnpc-world-migration.done` |
| Player done | NBT `lm_cnpc_player_migrated` |
| Config | `enableCnpcDataMigration` (default true) |
| Staff | `/lm admin migrate-cnpc` · `/lm admin migrate-cnpc force` |

Disable matching Rival/Spar CNPC tabs before or right after cutover to avoid dual writes.

## Script ↔ mod parity pass (2.3.3)

Full audit of live dump `uploads/live-scripts-2026-08-27/ecmascript/` (32 files)
against Java. All 30 enabled CNPC tabs remain mapped; gaps closed in 2.3.3:

| Gap | Fix |
|-----|-----|
| RaceLock missing `sento_saiyan` | `RaceLock` locks Ancient + Sento Saiyan |
| End dragon extra ki attacks | `EndDimensionStrength` DMZ beam/blast tick |
| End crystals / egg podium on kill | clear crystals + egg blocks (+ retry window) |
| Skill Check progress thin | flight/meditation/potential detail + Jump/Sprint STR gates |
| Spar STYLE_BONUS drift | aligned to live 1.08/1.08/1.12/1.10/1.06/1.05 |
| Prestige held/tokens | dual-write CNPC faction 4 + quest/dialog reset |
| Rival tops / stats / archive | `/rival top <cat>`, `/rival stats [player]`, `pastRivals` |
| Rival achievements / HoF | first_blood, perfect_victory, legend_killer, etc. + streak/rivalry/season HoF |
| Spar stats other player | `/spar stats [player]` |

### Still intentional / out of scope

- Prestige `/prestige` remains **staff-only** (live used Prestige NPC interact for players).
- End TP softcap tables remain simplified linear curve (documented).
- CMI `rival_title` usermeta sync not ported (GUI titles cover display).
- KubeJS balance pack stays KubeJS (weapons, Apotheosis, food, etc.).
- Disable live Building TP + shadow-dummy KubeJS when LM ships.

## Live server pull

**Done 2026-08-27** (read-only SFTP). Snapshot: `uploads/live-scripts-2026-08-27/`.

| Path on live | Contents |
|--------------|----------|
| `AdventureWorld/customnpcs/scripts/ecmascript/` | 32 active CNPC scripts |
| `…/player_scripts.json` | 30 Global Player tabs enabled |
| `…/forge_scripts.json` | **disabled** (`ScriptEnabled: 0`) |
| `kubejs/` | balance pack + Building TP + weapon scale + Pothala protect |

### Live vs LegacyMechanics

All 30 enabled CNPC tabs have Java ports. Live versions refreshed into `customnpcs/scripts/`.

Notable live bumps synced + ported:

| Script | Live | Action |
|--------|------|--------|
| End Dimension Strength | **2.12.0** (was 2.10.4 in repo) | Java: single-dragon + End ki purge |
| Rival System | **4.7.10** | Already in LM; script dump updated |
| Sparring TP | **3.2.11** | Already in LM |
| SprintJump | **1.1.0** invested STR | Already `InvestedStrength` |

### Live-only KubeJS added to repo

- `kubejs/server_scripts/dmz_pothala_protection.js` (live `pathalafix1.js`)
- `kubejs/startup_scripts/dmzweaponscale.js` (actual base weapon damage)

Building TP / shadow-dummy remain active on **live** KubeJS; LM has Java ports — disable those KubeJS files when LM ships to avoid doubles.

## CNPC scripts → LegacyMechanics

| Result | Count | Notes |
|--------|-------|-------|
| Ported OK | ~48 unique features | Rival, Sparring, natural progression, Fabled bridges, PlayerStatChecker |
| Intentionally not | 1 | `Attr Fabled bonus stats.js` — marked disabled duplicate of Multi bonus |
| Gaps closed this audit | 2 | `Disable End Portals.js` → `EndPortalGuard`; Building TP (KubeJS) → `BuildingTp` |

See [PROGRESSION.md](PROGRESSION.md) for the full disable list and config flags.

### Intentionally superseded splits (still covered by Java)

`Flight.js` covers Fly / Viltrumite / suppression; `Yardrat.js` covers race+skills; `Fabled Sync` / bridges cover prestige trio; `SprintJumpProgression` covers Jump/Sprint (invested STR).

## Other branches

| Branch | Unique vs LM | Status after audit |
|--------|--------------|--------------------|
| `main` | none material | Ancestor; LM ahead |
| `rival-sparring-adaptive` | Rival/Spar | Already in LM |
| `sprintjump-invested-str` | SprintJump 1.1.0 | Already `SprintJumpProgression` + `InvestedStrength` |
| `dmz-dino-food-balance` | `kubejs/.../dmz_food_balance.js` | **OUT_OF_SCOPE** (KubeJS item food) — keep on that PR |
| `server-fixes-consolidated` | Title 1.0.39, Admin inspect, unlock-gate, Building TP, GhostPartyHeal, weapon/Apotheosis KubeJS | **Ported into LM/melee** except KubeJS balance pack (below) |

### Ported from `server-fixes` this audit

- Title mastery / challenge / score (`TitleProgress`, `TitleEffects`, `TitleRarity`, `TitleScoreRewards`, `TitleSense`)
- Admin inspect: `/difficulty admin gui|inspect <player>` + `AdminInspectSessions`
- Unlock gate: `UnlockSystem.gateLevelForEligibility` + Buy GUI level text
- Building TP on place (`BuildingTp`)
- End portal guard (`EndPortalGuard`)
- Ghost party heal → `dmz_mohist_melee_fix` 2.12.20

### Still OUT_OF_SCOPE (stay KubeJS / other packs)

- `dmz_food_balance.js` (dino meat)
- `dmz_weapon_bonus_scale.js` / `dmz_actual_attack_damage.js`
- Apotheosis / lifesteal / heartstop / silk / necrotic / silky / capsule disables
- Dungeon clone ki / Shurui bridges / race_lock_gui_sync client companion

Ship those via `server-fixes-consolidated` / `dmz-dino-food-balance` PRs, not LegacyMechanics.

## Remaining risks

1. On test: disable matching CNPC + KubeJS Building TP / shadow-dummy when LM flags are on.
2. Do not deploy LM to live until test pass.
3. PrestigeFactionSync (class level → faction 4) can overwrite spent held tokens — same tension as live CNPC; monitor race-unlock shops.
