# LegacyMechanics ↔ CNPC script audit (2.3.48)

Audit date: 2026-08-30. Compares Forge mod behavior to live scripts under
`uploads/scripts/`, `uploads/live-scripts-2026-08-27/`, and former
`customnpcs/scripts/` fulls (now stubbed).

## Verdict

**Java owns the gameplay systems that were in your CNPC pack.** Remaining risk
was **double-apply**: `player_scripts.json` still loaded full Rival / Spar /
Farming / Flight / … while Java flags default ON.

**2.3.48 actions**
1. Stubbed all Java-owned full scripts in `customnpcs/scripts/` (full backups in
   `uploads/scripts/` + `uploads/script-backups-full/`).
2. Trigger stubs **forward to Java** (30/31/41/45/50/51/21) so shops/CMI that
   still fire `noppes script trigger N` keep working — **no CMI End aliases**.
3. Fixed parity bugs: mentor invite **2 min** (was 24h); manual meditation trial
   **30 min** (ChangeBiomeMED); auto rotate stays **15 min**.

Keep `LM-SkillCheck-NPC.js` on the Skill Check NPC. Prefer Forge `/enddragon`
(delete any CMI enddragon aliases).

---

## Status matrix

| Script | customnpcs | Java owner | Flag | Parity |
|--------|------------|------------|------|--------|
| End Dimension Strength.js | STUB | `EndDimensionStrength` | `enableEndDimensionStrength` | OK vs live 2.12.0 |
| EndDragon-Forge-Trigger.js | STUB→Java 50/51 | same + `/enddragon` | same | OK — use Forge cmds |
| Disable End Portals.js | STUB | `EndPortalGuard` | `enableEndPortalGuard` | OK (title feedback) |
| Global TP Boost.js | STUB→Java 30 | `GlobalTpBoost` | `enableGlobalTpBoost` | OK |
| TP boost end.js | STUB→Java 31 | same | same | OK |
| Meditation new.js | STUB | `MeditationProgression` | `enableMeditation` | OK |
| ChangeBiomeMED.js | STUB→Java 41 | same | same | OK — manual 30m |
| Potential.js | STUB | `PotentialProgression` | `enablePotential` | OK |
| AndrioidConversion.js | STUB→Java 45 | `AndroidConversion` | `enableAndroidConversion` | OK |
| Rival System.js | STUB | `RivalSystem` | `enableRivalSystem` | OK — migrate data |
| Rival Command Handler.js | STUB | `RivalCommands` | same | OK — `/rival` |
| Sparring Tp System.js | STUB | `SparringSystem` | `enableSparringSystem` | OK — invite 2m fixed |
| Sparring Command Handler.js | STUB | `SparCommands` | same | OK — `/spar` |
| Prestige NPC.js | STUB | `PrestigeSystem` | `enablePrestigeSystem` | OK — `/prestige` |
| SkillCheckCommand.js | STUB→Java 21 | `SkillCheckService` | `enableSkillCheck` | OK |
| SkillUnlockNPC.js | STUB | `SkillUnlockService` | `enableSkillUnlockService` | OK |
| LM-SkillCheck-NPC.js | KEEP (helper) | `SkillCheckService` | `enableSkillCheck` | OK — re-paste 2.3.47+ |
| Farming TP Skill.js | STUB | `FarmingTp` | `enableFarmingTp` | OK |
| PlayerStatChecker.js | STUB | `PlayerStatChecker` | `enablePlayerStatChecker` | OK (+1.5s CD) |
| Flight.js / Fly / Viltrumite / suppression | STUB | `FlightProgression` | `enableFlightProgression` | OK |
| SprintJump / Jump / Sprint / JumpSprint | STUB | `SprintJumpProgression` | `enableSprintJump` | OK |
| KiWeapons / Piercing / DoT / Apothic | STUB | `combat.*` | matching enable* | OK |
| BioAndroid / Race Lock / Yardrat / Spiritualist | STUB | `race.*` / `BioAndroidAbsorb` | matching | OK |
| ShadowDummy* | STUB | `ShadowDummyLimiter` | `enableShadowDummyLimiter` | OK |
| DMZ Fabled Bridge / Energy / Stat / Class / Sync / Attr / Races / TP↔SP | STUB | `bridge.*` | `enableFabledBridge` + subflags | OK |
| Building TP (KubeJS only) | n/a | `BuildingTp` | `enableBuildingTp` | OK |

**Pure LM (no CNPC twin):** Adaptive Difficulty core, titles/tiers, Ancient Coins,
GUIs/hub, telemetry, CNPC data migrator.

---

## Admin checklist after installing 2.3.48

1. Drop `LegacyMechanics-2.3.48.jar` (+ matching GUI jar). Remove older product jars.
2. Reload CustomNPCs scripts (or restart) so stubs replace fulls on disk.
3. Delete CMI CustomAlias for `enddragon` / `cleardragons` / `spawndragon` / `killdragons`.
4. Optionally delete Rival/Spar CMI aliases — Forge `/rival` `/spar` already work.
5. Run `/lm admin migrate-cnpc` once if rivalry/spar data still lives in CNPC worlddata.
6. Confirm `/enddragon`, `/progression boost`, `/progression meditation next`,
   `/progression android` from op/staff.

---

## Gaps fixed in this audit pass

| Issue | Fix |
|-------|-----|
| Mentor invite 24h vs script 2 min | `MENTOR_INVITE_MS = 120_000` |
| Manual meditation always 15m vs ChangeBiomeMED 30m | `MANUAL_TRIAL_DURATION_MS` |
| Full CNPC + Java double TP | Stubbed owned scripts in `customnpcs/scripts/` |
| Triggers 30/31/41/45/50/51/21 no-op stubs | Forward to Java |

## Remaining intentional differences

- End portals stay blocked (title, not chat) — reach End via teleport, then `/enddragon`.
- `/skillcheck` slash still needs `legacymechanics.skillcheck`; NPC/trigger 21 does not.
- Global TP Boost is in-memory for the active window (lost on mid-boost restart).
- PlayerStatChecker has a 1.5s cooldown and cancels the interact (script had neither).

## Do not re-enable

Do not paste full Rival/Spar/End/Farming/… scripts back into Global Player tabs while
the matching `enable*` flags are true. Backups: `uploads/scripts/` and
`uploads/script-backups-full/`.
