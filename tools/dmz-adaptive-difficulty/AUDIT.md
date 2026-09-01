## Buy GUI live level for all players (2.3.124)

Some players updated immediately; others stayed stuck on a stale gate. Snapshot
preferred {@code gateLevelForEligibility} when {@code highestDmzLevel > 1} and
refused to raise it while transformed — so form users / late-attach players
kept an old high-water while base-form players showed live {@code getLevel()}.

Fix: Buy GUI paint always prefers live DMZ {@code getLevel()} via
{@code guiDisplayDmzLevel}. Tier costs still use base-form freeze separately.
Level-pull retries also continue while the painted level is still ≤1.

## Difficulty GUI shows live DMZ level (2.3.123)

Root cause after 2.3.122: Buy GUI paints {@code DifficultySnapshot.dmzLevel}, which
preferred {@code gateLevelForEligibility}. When the unlock sample was unreliable,
that fell back to persisted {@code highestDmzLevel}. Early-login pollution often
left highest at **1**, and because {@code gate > 0} the snapshot never fell through
to live {@code getLevel()} — so the menu stayed at 1 even after Character attached.

Fix: treat highest≤1 as unknown; {@code guiDisplayDmzLevel} prefers live/base-form
sample; snapshot + ForgeBridge placeholders use that for the painted level.

## Difficulty level sample before death (2.3.122)

Buy GUI / unlock-gate DMZ level stayed at placeholder 1 until the player died
because login often has StatsData before Character. `isTransformed` treated
form-multiplier peak as transformed when Character was null, freezing
`BASE_FORM_LEVEL`, and early samples wrote level-1 into the session cache.

Fix: Character-null is never transformed; unreliable until Character attaches;
never freeze placeholder 1 over a good sample; `scheduleLevelPull` on login /
GUI open / death clone with delayed retries.

## PvP corpse empty inventory (2.3.63)

Root cause: GriefPrevention **16.18.3** `AllowCombatItemDrop: false` cancels
`PlayerDropItemEvent` while `inPvpCombat()` with **no** `player.isDead()` check.
Mohist/Paper fire that event for death loot, so GP destroys the inventory and
Corpse spawns empty.

Fix in `LegacyMechanicsGUI`: {@code DeathDropGuard} (HIGH) un-cancels cancelled
drop events only when the player is dead. Live combat Q-drop still blocked by GP.

## Difficulty GUI live level pull (2.3.62)

Mohist {@code /difficulty} opens the Bukkit CMI/chest GUI without going through
Forge {@code DifficultyActions.openGui}, so the Buy menu could paint a stuck
session DMZ sample (level 1 / stale mid-level) until {@code resynclevel}.

Fix: {@code DifficultyActions.prepareGui} + {@code ForgeBridge.prepareDifficultyGui}
on every open; {@code DifficultyCache.refresh} always re-samples; GUI-open sampling
no longer writes placeholder level-1 while StatsData is missing, and heals polluted
caches when transform detection is weak.

## Tier cost anchors (2.3.61)

Absolute-level exponential curve:
- **DMZ 1 / T1** = **1× Copper**
- **DMZ 150000 / T7** = **100× Netherite** (`tierCostT7TargetCopper` = 10_000_000)
- Shared mult across tiers; progress clamps at `tierCostLevelAnchor` (150000)
- Admin: `/difficulty admin set tierCostLevelAnchor|tierCostT7TargetCopper <n>`

## Global TP boost persistence (2.3.60)

Active window saved to `config/legacymechanics/global-tp-boost.json` (wall-clock
end time). Restart mid-boost restores remaining time; joiners while active get
the effect; when the window ends online players are stripped; login strips
leftover `TP_GAIN` if the boost already ended offline.

## Skill Check NPC edit + Natural/Saga tabs (2.3.59)

- Holding CNPC editor tools (`npcscripter`, `npcwand`, cloner, NBT book, …) skips
  LM GUI opens so staff can edit tagged NPCs with the scripter wand.
- CMI Natural/Saga tabs used `lmdo skills page` (staff-only) — Skill Check sessions
  now route `lmdo skillcheck page`. Session (NPC open without donator node) can
  switch pages; slash `/skillcheck` still needs `legacymechanics.skillcheck`.

## Console TP boost (2.3.58)

Bukkit `/progression` rejected console (`Players only`), so store/Tebex purchase
commands could not start a global TP boost. Console may now run:

- `progression boost start <mult> <minutes> [purchaser]`
- `progression boost start <encoded> [purchaser]`
- `progression boost end` / `status`

Forge `ProgressionGuiApi.boost` accepts null actor; brigadier minutes path also
takes optional purchaser name.

## Live telemetry retune (2.3.57)

Pulled `hits-2026-08-29.jsonl` + `hits-2026-08-30.jsonl` from production SFTP
(`uploads/live-telemetry-2026-08-30/`, 2132 hits).

Signals:
- T2 god forms soft-cap pinned at **43%** bag (71% of hits) — too hot for early buy
- T4/T5 wouldCancel 90–96% is expected: soft-cap &lt; mit/thr so pierce cannot clear cancel;
  real pressure is landing + soft-cap (T4 ~50%, T5 ~46%)

Changes:
- Soft-caps **T1 0.34 · T2 0.36 · T3 0.44** (T4–T7 unchanged)
- Ease T1–T2 landFrac/landCap + form bump; T5 landFrac → 0.50
- Trim T1–T2 threat floors / liveShare
- Formula fingerprint **38**

## End exit podium repair (2.3.56)

Older egg-clear used `Blocks.f_50259_` believing it was `end_portal` — that SRG
id is **`end_stone`**, so it hollowed the fountain skirt. Later dragon kills
re-placed `EndPodiumFeature` → stacked portals / ruined underside.

Fix:
- Egg clear uses real `dragon_egg` (`f_50260_`) + registry id only
- `repairEndExitPodium` collapses ghost portal layers and rebuilds one fountain
- Runs on dragon kill, GUI player summon, hygiene when duplicates detected
- Staff: `/enddragon repair` / `/cleardragons` (staff spawn disabled)
- `EndPortalGuard` fast-path constants corrected (`end_portal=f_50257_`,
  `end_gateway=f_50446_`)

## Dragon ki launch (2.3.55)

End dragon DMZ ki blasts were hovering: `setupKiBlastPlayer` already spawns the
entity, then `spawnAndFireKi` aborted when a second `addFreshEntity` failed —
so `fireHability` never ran (firing=false / zero velocity). Fix: mob setup path
(`setupKiLargeBlast` / `setupKiLaser` cast 0), always fire after setup, explicit
`launchDragonKiToward` velocity (same pattern as `KiAttackHelper`).

## End ki purge console spam (2.3.54)

End hygiene was running `kill @e[type=dragonminez:ki_laser|ki_blast]` every ~1.5s
when no dragon was present, printing **"No entity was found"** twice per pulse.
Purge now discards entities in Java and no-ops when empty (same for crystal clear).

## Player command whitelist (2.3.54)

Players may slash only:
`/lm` · `/difficulty` · `/rival` · `/spar` · `/progression meditation` · `/skillcheck` (donator).

Prestige, Remove Android, Skills, and other progression actions are **GUI-only**
via `/lm` (CMI clicks still use internal `/lmdo`). Staff keep full slash trees.

## Script parity deep check (2.3.54)

Re-verified Java vs live/backup scripts (`uploads/live-scripts-2026-08-27/`,
`script-backups-full`). Core Rival/Spar/Prestige/End/Android/SkillCheck/Meditation/
Flight/SprintJump/Farming/Building/combat/Fabled OK.

Gaps fixed in this release:
- Yardrat starters → real skill IDs `kimanipulation` / `kicontrol`
- RaceLock clears stuck saga `difficultyChosen` after reset + while create incomplete
- ShadowDummy dual-reads legacy CD NBT key
- Global TP Boost 5s end debounce

Conversation-era changes preserved — see `CONVERSATION-CHECKLIST.md`
(GUI hub hops, StaffAccess enddragon, player prestige/android remove, CNPC-free,
mentor 2 min, spar 3-card reports, Skill Check no soft rival match).

## GUI + command audit (2.3.51)

Full GUI/command/lmdo/inspect audit: `GUI-COMMAND-AUDIT.md`.
Fixes: bare `/lm` hub hops → `/lmdo lm open hub`; enddragon + difficulty staff
via `StaffAccess`; progression player root; skillcheck no op2 grant; android_remove
inspect + chat-backend inventory; prestige/player hub buttons.

## CNPC-free (2.3.50)

LegacyMechanics replaces all CNPC scripts. See `CNPC-FREE.md`.
NPC GUI opens cancel on tag **or** strict name. No scripts required.

## Spar individual reports (2.3.49)

`/spar` → **Stats** shows the last **3 spars as separate report cards**
(TP, time, melee/ki, combo, reason, when) — not one combined line.

## Script audit + parity (2.3.48)

Full matrix: `SCRIPT-AUDIT.md`. Stubbed Java-owned CNPC scripts (no double-apply).
Trigger stubs 21/30/31/41/45/50/51 forward to Java. Mentor invite 2 min;
manual meditation trial 30 min.

## End dragon parity + Skill Check CNPC (2.3.47)

- **Skill Check CNPC**: Forge no longer soft-matches `contains("rival")` (stole
  scripted Skill Check clicks / false-positives like "Arrival"). Tag opens cancel;
  name opens also cancel (CNPC-free). Prefer tag `lm_skillcheck` — no NPC script.
- **End hit cap**: damage is set to the capped mitigated value (no longer
  re-floored with `raw * minFrac`, which undid the ~250-hit fight length).
- **Egg clear**: only `minecraft:dragon_egg` — older builds cleared `end_portal`
  near 0,0 and broke the exit portal.
- **CMI aliases**: do **not** use CMI for `/enddragon`. Delete any CMI
  CustomAlias for `enddragon` / `spawndragon` / `cleardragons` / `killdragons`
  (old ones fired stub CNPC triggers 50/51). Forge owns the commands.

## CNPC GUI open without permission (2.3.46)

Right-click CustomNPCs open player GUIs with **no permission**:
Rival / Spar / Hub / Difficulty / Prestige (tags `lm_rival`… or name match).
Skill Check CNPC / trigger 21 also skips `legacymechanics.skillcheck`
(slash `/skillcheck` still gated).

## Rival title bonuses (2.3.45)

Rival Title GUI shows perk + TP Gain % (CNPC parity). RP-rank `tpMult` also
applies to rival TP awards via `RivalTpCurve`.

## GUI action feedback (2.3.44)

When `guiBackend` is chest/CMI, action results (can't buy, prestige not ready, etc.)
paint the **menu header** instead of chat. Chat backend unchanged.

## Global TP boost stacking (2.3.43)

When a boost is already active, buying another **adds its duration** and keeps the
**highest multiplier** (a lower pack never downgrades an active boost).

## Spar TP message toggle (2.3.42)

Players toggle live spar TP chat from the **Spar GUI** only (`/spar` → **TP Msg**).
No `/spar tpmsg` command. Default ON. Pending +TP batches flush on a timer during the fight.

## Skill Check real max levels (2.3.40)

Saga skill lines (Ki Boost, etc.) use `skills.getMaxSkillLevel(id)` instead of a
hardcoded `/10`, matching live SkillCheckCommand.js `getSkillMaxSafe`.

## Player Prestige menu (2.3.39)

`/prestige` and `/lm` → Prestige are available to all players (not staff-only).
Confirm still uses the two-click PrestigeSystem flow.

## Android convert + remove (2.3.37)

Race → **Android Tools** panel (chest + CMI): Convert or Remove Android upgrade.

- Remove mirrors the CNPC Android Upgrade Removal script: two-click confirm (10s),
  `setAndroidUpgraded(false)`, revert active form, restore `superforms`/`legendaryforms` at 0,
  remove `androidforms`, recalculate transformation limits, sync.
- Commands: `/progression android [player]` · `/progression android remove [player]`
- Forge: `AndroidConversion.remove` · Bukkit: `ForgeBridge.androidRemove`

## Player self-remove (2.3.38)

Players can de-androidify without staff:

- `/lm` hub → **Remove Android** (everyone)
- `/progression android remove` (self only; staff can still target others)
- Convert remains staff-only

## GUI coherence pass (2.3.36)

One icon language across Hub / Difficulty / Rival / Spar / Progression / Prestige / Skills:

| System | Brand icon |
|--------|------------|
| Hub | NETHER_STAR |
| Difficulty | BEACON |
| Rival | NAME_TAG |
| Spar | GOLDEN_SWORD |
| Skill Check | EXPERIENCE_BOTTLE |
| Skills admin | BOOK |
| Prestige | GOLDEN_APPLE |
| Progression | BREWING_STAND |
| Nav Hub / Close / Back | COMPASS / BARRIER / ARROW |

Fixes: Difficulty unavailable close slot 35; Clear Title / Reset None no longer BARRIER;
Rival Progress tiles match detail headers; Spar Stats BOOK; Prestige GOLDEN_APPLE;
Progression shortcuts BREWING_STAND; hub on Rival/Spar 45-slot boards.

## Skill Check icons (2.3.35)

- Skill Check header is **EXPERIENCE_BOTTLE** (not PAPER) — one status item only.
- Skill tiles never default to PAPER (ELYTRA for Flight, EMERALD fallback).
- Hub Skill Check button uses EXPERIENCE_BOTTLE; staff Skills uses BOOK.
- Staff `/skills` admin still uses a single PAPER header.

## Skill Check hub perm gate (2.3.34)

`legacymechanics.skillcheck` is required to **see** Skill Check on `/lm` hub (chest + CMI + chat).
Staff are no longer auto-granted that node — without it they see **Skills** (admin) instead.
`/skillcheck` Bukkit command declares `permission: legacymechanics.skillcheck`.
Staff can still `/lm open skillcheck` / inspect for testing.

## Spar Stats — last 3 sessions (2.3.33)

Stats board no longer shows live in-fight TP (you can't usefully read that mid-spar).

- On session end (≥30s counted), push a summary onto `sparring.json` → `recentSessions[uuid]` (newest first, cap 3).
- `/spar` → Stats (chat + chest + CMI) lists last 3: partner, TP, duration, combo, perfect mark.
- Main hub still shows active session + End button.
- Cleared by `/lm admin clear <player> spar` and CNPC migrate force.

## /lm admin clear player (2.3.32)

Staff wipe of one player's LegacyMechanics data without a full CNPC force-migrate.

```
/lm admin clear <player>              # all LM systems
/lm admin clear <player> rival
/lm admin clear <player> spar
/lm admin clear <player> difficulty
/lm admin clear <player> progression
```

- Resolves online name, RivalStore offline name, or UUID.
- Rival: drops rivalry-v4 record + reverse links + declare requests + season/achievements/journal.
- Spar: drops bond + invites + leaderboard (+ reverse mentor/apprentice pointers).
- Difficulty / progression NBT require the player **online** (offline clears rival/spar/prog-temp only).
- Progression wipe also clears `lm_cnpc_player_migrated` so per-player CNPC re-migrate can run again.
- Wired on Forge `MechanicsCommands` and Bukkit `handleLmAdmin` via `ForgeBridge.clearPlayerData` (Mohist owns `/lm`).

## CNPC richest-source pick (2.3.31)

Audit follow-up: live/disk stubs no longer beat `cnpc-import-backup`.
- `pickRichestBlob` scores all candidates (compound/live/disk/backup)
- Prefers `.backup` CNPC keys when main is empty/stub
- `force` aborts without clearing LM if no usable source
- Clears only the key groups that were actually imported

## CNPC world_data capture (2.3.30)

**Bug:** Migrator never reliably read `<world>/customnpcs/scripts/world_data.json`.
- Relied on `getIWorld(...).getStoreddata()` (Mohist-fragile)
- File fallback used Gson, but real CNPC files are **NBT-JSON** (`1b`, `123L`) → parse fail
- Wrong `MinecraftServer` path reflection (no `LevelResource("customnpcs")`)

**Fix:**
1. `ScriptController.loadStoredData()` then read `ScriptController.compound` directly
2. Resolve `<level>/customnpcs/scripts/world_data.json` via `getWorldPath(LevelResource)` / `CustomNpcs.getLevelSaveDirectory`
3. Load with `NBTJsonUtil.LoadFile` (Gson only as fallback for plain dumps)

## CNPC migrate recovery (2.3.29)

**Bug:** Migrator skipped Rival import when `rivalry-v4.json` already had stub players, then
**still cleared CNPC keys** and wrote a done marker (`rivalPlayers=0`). Later
`/lm admin migrate-cnpc` reported success with nothing left to grab.

**Fix:**
- Clear CNPC keys only after a real import (`rivalPlayers > 0` / prog / spar LB).
- Prefer CNPC when it has more (or richer) players than LM; `force` still wipes LM first.
- Source chain: live storeddata → `cnpc-import-backup/` → CNPC `world_data.json` on disk.
- Honest chat: reports counts + source; never §a on zero imports.

**Recover a wiped server:**
1. Put a pre-wipe `world_data.json` (or `rivalry-database.json`) in
   `config/legacymechanics/cnpc-import-backup/`
2. Delete `config/legacymechanics/cnpc-world-migration.done` if present
3. Run `/lm admin migrate-cnpc force`
4. Confirm chat shows Rival players &gt; 0

## Mohist CMI / admin audit (2.3.28)

- Remaining CMI GUIs (Difficulty/Hub/Progression/Prestige/Skills) now use Bukkit-only `/lmdo …`
  (same pattern as Rival/Spar in 2.3.26) — Mohist was routing `* do` to incomplete Forge trees.
- `DifficultyChestGui` always uses `ForgeBridge.handleActionResult` (no `performCommand difficulty do`).
- Hub chat menu clickables use `/lmdo lm …`; `lmdo lm open <system>` switches menus without reopening hub.
- Bukkit `/difficulty admin syslog` + `/difficulty admin resynclevel` wired (were Forge-only).
- Prestige/Skills chat-backend no longer `forwardCommand` to Forge.

## /lm admin migrate-cnpc on Bukkit (2.3.27)

- Mohist owns `/lm` via LegacyMechanicsGUI — `migrate-cnpc` was Forge-only and unreachable.
- Wired through Bukkit `handleLmAdmin` + `ForgeBridge.migrateCnpc` (+ MechanicsGuiApi handleDo).

## Rival/Spar CMI clicks (2.3.26)

- CMI Rival/Spar buttons use Bukkit-only `/lmdo` (Mohist was routing `/rival do`/`/spar do` to Forge's incomplete tree → silent no-op).
- Forge `/rival do` and `/spar do` expanded to full `handleDo` + greedy args as belt-and-suspenders.

## Player GUIs open to all (2.3.25)

- `/difficulty` (and `/lm` `/rival` `/spar` / meditation) no longer gate on `dmzdiff.gui` — Mohist was denying non-ops.
- Alias `/diff` for difficulty. Admin remains staff-only.

## Access + rival bridge + Fabled ki/AP + meditation XP (2.3.24)

- Non-op `/lm` / player GUIs: allow unless `dmzdiff.gui` is explicitly denied (LuckPerms/Mohist-safe).
- `/progression meditation` uses the same access helper.
- Typed `/rival` actions route through `rivalHandleDo` (no Forge brigadier forward).
- Meditation progress chat every 5s (matches live script).
- Fabled: energy sync after `updatePlayerStat` + Bukkit follow-up; race class sync hardened; AP→DMZ bonuses require characterCreated + attribute fallbacks.

## Short GUI titles + drop /rivals (2.3.23)

- Removed `/rivals` alias (use `/rival`).
- Inventory titles no longer prefix `Legacy Mechanics ·` — only the main `/lm` hub keeps that brand title.
- Sparring / Progression / Prestige / Skills / Logs use short names (`Sparring`, `Progression`, …).

## Spar mentor leave/release choice (2.3.22)

- Mentor GUI: separate **Leave mentor** and **Release apprentice** (no auto-pick Remove bond).
- Fixed bond detection (`has_mentor` / `has_apprentice`) — was wrongly requiring both on one record.
- Commands: `/spar mentor leave` · `/spar apprentice release|remove`.

## Meditation-style chat (2.3.21)

- Shared `LmChat` / Bukkit `GuiChat`: tags normalize to `§5§l[System] §r…` like Meditation.
- Card helpers (title + divider + tip) for major Rival/Spar/Meditation announcements.
- All `DmzRewards.msg` and GUI action results route through the normalizer.

## Trim duplicate commands (2.3.20)

- Removed `/lmgui`, `/dmzdiffgui`, `/adiffgui`, `/legacymechanicsgui` and legacy difficulty aliases.
- Kept primaries: `/lm`, `/difficulty`, `/rival` (+`/rivals`), `/spar` (+`/sparring`), `/progression` (+`/prog`).
- Fallback tips now say `/lm` instead of `/dmzdiffgui`.

## Admin LM inspect-all (2.3.19)

- Staff: `/lm admin inspect <player> [hub|difficulty|rival|spar|skillcheck|…]` opens chest GUIs as that player.
- Hub tiles keep the inspect session; edits apply to the subject. Clear with `/lm admin inspect clear`.
- Forge: `CmiGuiBridge.openLmInspect` / `clearLmInspect`; jars `LegacyMechanics-2.3.19` / `LegacyMechanicsGUI-2.3.19`.

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
- Clearer player-facing goal text (Ki %, height, stay-still) — same biome gates as the script
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
