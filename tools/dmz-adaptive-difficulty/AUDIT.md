## Unified Difficulty Tiers menu (2.3.155)

Buy Tier and Lower Tier are one **Tiers** menu: buy higher, lower unlocked, or reset
to None. Hub shows Tiers + Titles (no separate Lower). Legacy `/difficulty buy|lower`
aliases open the same page.

## Difficulty titles: no tier regress + presence (2.3.154)

Tier titles unlock from **DMZ level or Prestige** (not the active difficulty tier), so
lowering tier never blocks or revokes them. Free swap among unlocked titles stays.
Equipped titles gain **rarity presence** (softer Adaptive landings, small AD damage,
TP while a tier is on) plus buffed specific perks. Godslayer sense/quality no longer
requires holding T7. Ascendant needs ever-unlocked T7, not active T7.

## Unified chat style (2.3.153)

All player-facing system chat uses Meditation-style {@code §5§l[System] §r…} tags via
`LmChat` (`tagged` / `ok` / `fail` / `info` / `tip` / `card` / `tp`). Long legacy tags
(`Sparring`, `Mentor Bond`, `Rival Instinct`, …) alias to short names. `§8Open §e/cmd`
tips rewrite to `Tip · /cmd`. TP awards go through `LmChat.tp`.

## Dojo membership roster (2.3.152)

Dojo menu shows the **mentor at the top** plus every apprentice in that dojo when you
are a member. If you also mentor others, **My Dojo** / **Their Dojo** switches between
your own apprentices and your master's roster. Dojo opens for apprentices (not only mentors).

## Mentor Actions tip placeholders (2.3.151)

Leave/Release tips no longer show **End bond with ?** (gray Leave used the active tip
with empty `{name}`) or literal **`{name}`** on multi-apprentice Release (CMI page tips
skipped var substitution; on-disk lore still said End bond with `{name}`).

- Separate `leave_none` / `release_none` tip keys for inactive buttons
- CMI page buttons pass `{name}` vars (dojo roster summary)
- Jar force-refreshes mentor leave/release/dojo tip keys on `/lm admin reload`

## Mentor Actions cooldown tooltip (2.3.150)

On-disk `plugins/LegacyMechanicsGUI/gui-tooltips.json` was still showing **7-day** Leave/Release
cooldown after the real bond CD moved to **12 hours** (disk overrides jar). Reload now rewrites
stale `7-day` / `7d` mentor cooldown phrases to **12-hour** / **12h** and persists the file.

## Mentor invite 1h + dojo peer spar (2.3.149)

Mentor/apprentice **Pending invites last 1 hour** (was 2 minutes). Apprentices of the
same mentor who spar each other get **+10% TP** (`DOJO_PEER_SPAR_BONUS_PCT`); mentor↔apprentice
spar stays **+18%**. Bonuses do not stack.

## Spar TP lines player/staff (2.3.147)

Spar combat TP chat is simple for players (`+TP (style)`). Staff see bonus tags,
multiplier stack (bp/riv/rel/grav/wt/sty ×total), melee/ki/clash split, and session total.

## Mentor Actions GUI (2.3.146)

Mentor bonds are handled in-GUI like Rival Actions: Invite · Ask · Pending · Leave ·
Release · Dojo. Incoming pending opens Accept/Decline submenu; outgoing cancels.
Legacy Accept…/Decline… hub buttons removed (deep-links kept).

## Spar CNPC multiplier parity (2.3.145)

Restored/hardened CNPC spar TP multiplier stack visibility and edge cases:
BP × rival × release × gravity × weight × prestige × momentum × session × streak ×
style × perfect × global 1.50 (+ mentor pair). Prestige reads max(DMZ skill, Fabled−1).
Momentum tier-up messages + TP chat bonus tags (combo/mom/streak/pres/sess/★).

## Mentor dojo multi-apprentice (2.3.144)

Mentor/apprentice change cooldown is **12 hours** (was 7 days). Mentors may train up to
**8 apprentices** (dojo roster); each player still has at most one master. Mentor TP share
is diluted by roster size (`15%/N`) so total take maxes at the former one-apprentice rate.

## Spar active TP + ki-charge hold (2.3.143)

Release-control drip TP no longer treats ki-charge / soft-clash holds as combat hits.
Real damage exchanges stamp `lastCombatOut*`; charge/clash only set `holdUntil` /
`chargingUntil` so the match stays open through charge→shot. Idle fighters stop
earning drip TP (and during recover grace). `KiChargeEvent` refreshes the charge hold.

## Rival list status order + underdog any status (2.3.142)

GUI/list order: Nemesis → Mutual → Declared → Silent (RP desc within group).
Underdog engage/win TP applies for any rival you declared (including Mutual/Nemesis)
when they are stronger — not one-sided Silent only.

## Mutual slot replace picker (2.3.141)

At max Mutuals (2), Accept opens a picker to choose which Mutual to replace
(demoted to Declared) instead of auto-dropping the oldest.

## Dual Silent Mutual confirm (2.3.140)

Both Silent → Declared on both lists + Pending Mutual confirm for both.
Each must Accept (Pending submenu); Mutual only when both have accepted.
Decline Mutual keeps Declared.

## Pending request submenu (2.3.139)

Actions menu: Declare · Pending · Remove · Silent.
Pending list: click an incoming head → submenu with Accept / Decline for that request.

## Pending Accept/Decline on each invite row (2.3.138)

Pending Invites shows Accept and Decline beside each incoming request
(chest + CMI). Removed separate Accept…/Decline… menus from Pending.
Actions keeps **Accept Declared…** for both-Silent Declared → Mutual only.

## Declared stays on list after decline/ignore (2.3.137)

Visible Declare sets `visibleDeclare` so the declarer keeps a **Declared** entry on
their rival list even if the target declines, ignores, or the invite expires.
Incoming declares stay on **Pending** (Accept / Decline). Accept/Decline resolve
`uuid:` args so offline declarers work.

## Nemesis = challenge deaths only (2.3.136)

Nemesis climb uses challenge knockouts only (`deathLosses`), not open-world PvP
and not damage-timer challenge losses. Wins/losses stay on the challenge score
path so KO deaths are not double-counted.

## Rival Progress UI cleanup (2.3.135)

Progress hub is 4 buttons (Stats · Season · Quests · More). More opens Records
(Title · Achs · HOF · Journal). Detail pages are a single summary item on a
3-row board — no tip spam / tile walls.

## Rival remove semantics + progress UI (2.3.134)

Rival relationship model (labels + menus):

| Status | How | Who sees it |
|--------|-----|-------------|
| **Silent** | `/rival` or Actions → Silent | Only the player who Silent'd (no notify) |
| **Pending** | Actions → Declare | Pending Invites only (target notified) |
| **Declared** | Both Silent each other | Both lists; Accept → Mutual |
| **Mutual** | Accept pending or Declared | Both |
| **Nemesis** | Mutual + **3 challenge KOs** | Both |

**Remove:**
- Remover always archives to **History**
- Mutual/Nemesis remove: other keeps a one-way **Silent** declare (no longer clears both)
- Declared remove: other may keep Silent one-way
- Silent/Pending remove: own side only

Progress hub tightened to a compact 3-row board (less tip spam).

## Live telemetry retune (2.3.148)

Pulled production hits to `uploads/live-telemetry-2026-09-02-retune/` (**40628** hit
lines). Calibration window = Aug 31 + Sep 1 + Sep 2 (**23997** hits). Soft-cap
fingerprint was **40**.

Signals after 2.3.133 (fp40):
- KP0–2 gods soft-cap-pin healthy
- **KP8+ T3→T4 still flat** (0.282→0.315; JarebearT +0.027)
- **KP8+ T5→T6 still flat** (0.462→0.487; Rogerio +0.027)

Changes (fingerprint **41**):
- T4 landFrac **0.44→0.48** (still ≤ soft/landCap 0.50)
- T6 landFrac **0.57→0.58**; T7 landFrac **0.58→0.60**
- T6 soft-cap + landCap **0.58→0.60** (still ≤ T7 0.62)
- Soft-caps T1–T5 / T7 unchanged

Expected KP10: T4 landing climbs toward ~0.34+; T6 soft/landing headroom above T5.

## Live telemetry retune (2.3.133)

Pulled production hits to `uploads/live-telemetry-2026-09-02/` (**40302** hit lines;
Aug 9, 29–31, Sep 1–2). Post-2.3.129 window = **Aug 31 + Sep 1 + Sep 2** (**23671** hits;
Sep 2 still thin at 352). Soft-caps unchanged (fingerprint was 39).

God-form Sep 1 same-player ladders (wouldCancel-dominated / landing path):

| player | ladder |
|--------|--------|
| Ch4osDoom64 (KP~1) | T3 0.426 → T4 0.482 → T5 0.515 |
| JarebearT (KP10) | T3 0.284 → T4 0.311 → T5 0.420 |
| RogerioTorio (KP10) | T5 0.471 → T6 0.500 |

Signals after 2.3.129:
- KP0–2 gods soft-cap-pin as designed; soft-cap ladder healthy
- **KP8+ T3→T4 still flat** (landFrac 0.40 → ~0.31 after KP/bag)
- **KP8+ T5→T6 still flat** (+0.029 only; T6 landFrac 0.54 under KP10)

Changes (fingerprint **40**):
- T4 landFrac **0.40→0.44** (still ≤ soft 0.50 / landCap 0.50)
- T6 landFrac **0.54→0.57** (still ≤ soft 0.58 / landCap 0.58)
- Soft-caps / liveShare / form nudges unchanged

Expected KP10 god landing climb: T4 ~0.31→~0.34+, T6 ~0.50→~0.53+; KP0 path stays soft-cap bound.

## Prestige turn-in ladder (2.3.132)

Turn-in packs are **1 / 2 / 3 / 6 / 9** only (removed 5 and Turn In All).
Points: {@code N + T(N/3)} with triangular {@code T(k)=k(k+1)/2}:

| Amount | Points |
|-------:|-------:|
| 1 | 1 |
| 2 | 2 |
| 3 | 4 |
| 6 | 9 |
| 9 | 15 |

## Full mod audit (2.3.131) — 2026-09-01

Fail-closed suite all green on `cursor/telemetry-scaling-cal-c766`:

| Audit | Result |
|-------|:------:|
| audit_features (555) | PASS |
| audit_concept (64) | PASS |
| validate_tier_costs (1–150k) | PASS |
| validate_scaling (73) | PASS |
| simulate_build_matrix --check (35) | PASS |
| simulate_race_forms --check (10 races) | PASS |
| audit_tier_level_matrix (72; 1554 form cells) | PASS |
| audit_gui_abi | PASS |

Report: `sim/out/full-mod-audit.md`. Jars on disk: LegacyMechanics / GUI **2.3.131**.

## Android upgrade all races (2.3.131)

Android is a Gero **upgrade flag** on races that ship `androidforms` TP costs — not a
race swap. Stock configs: **Human, Saiyan, Frost Demon, Viltrumite**. Bio-Android is
blocked (already an android lineage). Namekian / Majin / Monkey / Sento / Ancient have
no `androidforms` group.

Convert gate matches DMZ Gero (`getFormSkillTpCosts("androidforms").length > 0`).
Combat scaling stays race-agnostic (live stats + form mults); 2.3.130 landing fix
applies to every upgraded race the same way. Sim T5 peak androidforms: all four
eligible races hitFrac ≥ 0.48 with landing ≤ soft-cap.

Also: deny/GUI copy no longer says "humans only"; hit telemetry logs `"android":true|false`.

## Android T5 zero damage (2.3.130)

Live report: Android-upgraded players on T5 took no real HP damage. Telemetry still
showed ~42–52% bag because AD logged `event.getAmount()` after restoring landing,
then DMZ `CombatEvent.overrideVanillaArmorReduction` (also `LOWEST`) rewrote from
`dmz_raw_damage` and `applyFullNegation` → 0. Androids / high-DEF gods hit
wouldCancel ~90%+ so they lived almost entirely on the safety-net path.

Fix: when AD finalizes landing / soft-cap on a painted mob hit, clear
`dmz_raw_damage` / `dmz_defense_pen` / `dmz_block_multiplier` so DMZ cannot
overwrite the bite. Soft-caps / landFrac ladder unchanged (fingerprint 39).

## Live telemetry calibration (2.3.129)

Pulled production hits from SFTP `config/legacymechanics/telemetry/`:
`uploads/live-telemetry-2026-09-01/` — **33469** hits (Aug 29–Sep 1).
Calibration window = post-2.3.57 days **Aug 31 + Sep 1** (**22442** hits).

God-form avg hitFracPost (soft-caps unchanged):

| tier | n | avgHF | softCap | pin% | path |
|-----:|--:|------:|--------:|-----:|------|
| T1 | 46 | 0.122 | 0.34 | 0% | landing |
| T2 | 95 | 0.145 | 0.36 | 0% | landing |
| T3 | 3526 | 0.293 | 0.44 | 8% | landing |
| T4 | 1705 | 0.359 | 0.50 | 23% | mostly landing |
| T5 | 11045 | 0.501 | 0.52 | 73% | soft-cap |
| T6 | 2748 | 0.515 | 0.58 | 48% | 50/50 |
| T7 | 691 | 0.612 | 0.62 | 93% | soft-cap |

Signals:
- T1–T5 / T7 pressure on-target; soft-cap ladder healthy
- **T5→T6 flat** on landing path (both landFrac 0.50; god +0.014 only)
- **T4→T5 cliff** (god 0.359→0.501)
- High wouldCancel at T4+ still expected (soft-cap &lt; mit/thr)

Changes (fingerprint **39**):
- T4 landFrac **0.37→0.40**, landCap **0.48→0.50**
- T6 landFrac **0.50→0.54**
- T6 form nudge **1.82→1.90**; T6/T7 liveShare **0.74→0.80**

Concept compliance (audit_concept + ladder sim):
- Soft-caps monotonic T1→T7; landFrac strictly progressive T1→T7
- God hitFrac rises every buy T3→T7; landing T4&lt;T5&lt;T6&lt;T7 (T5→T6 buy matters)
- Even T1→T5 ≥1.4× / T5→T7 ≥1.10×; dumps still ≥28% bag at T5
- Landing stays ≤ soft-cap; god forms cannot out-tank (SSJG/SSJB landing ≥35% at T7)
- Soft-caps unchanged so early-tier feel (T1–T3) stays the 2.3.57 concept retune

## Form-aware CR vs mob scaling (2.3.128)

Verified: nearby mob HP/damage paint uses {@code PlayerCombatProfile} (liveOffense +
formBoost), not Combat Rating. CR uses the same live form-included damage channels
× power release so display rises/falls with transforms. Form change clears profile,
refreshes CR snapshot, clears area CR cache, and retargets claimed mobs.
Rival Instinct form-surge alerts now key off form⊕stack mult (not BP/CR).

## Android-safe CR from released stats (2.3.127)

Androids (and similar) can report Inf / absurd DMZ battle power, so Buy GUI CR
painted {@code Long.MAX_VALUE}. Combat Rating transform term now uses an own
rating from live combat channels (melee/strike/ki/energy/def/hp) × power release
— sparring-style released statistics — with {@code CombatSanity} clamps. Spar /
Rival BP helpers fall back to the same proxy when DMZ BP is unsafe.

## Combat Rating Long.MAX_VALUE overflow (2.3.126)

Buy GUI showed CR {@code 9223372036854775807} ({@code Long.MAX_VALUE}) for high-form
players. {@code transformationPower} is {@code battlePower/1000}; absurd/Inf BP made
{@code Math.round} overflow. {@code hardCapDifficulty=0} applied no ceiling.

Fix: sanitize non-finite BP, cap transform contribution, and clamp CR to a 1e15
display absolute cap (or configured hardCap when set).

## High-CR players stuck at level 1 (2.3.125)

Combat Rating includes form battle power (`BP/1000`), so high-form players show
high CR even when DMZ `getLevel()` is still placeholder 1 (config/maxValue race).
Buy GUI looked "stuck" for those players while low-CR / base-form players updated.

Fix: when `getLevel()` ≤ 1, recompute level from base stat totals; GUI also falls
back to persisted high-water if live is still a placeholder.

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
