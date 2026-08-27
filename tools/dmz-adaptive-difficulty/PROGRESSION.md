# Natural Progression & Fabled Bridges (1.0.47)

LegacyMechanics ports CNPC “natural progression” scripts into Forge, plus soft-dependency
**Fabled** bridges (Bukkit / LuckPerms via reflection — never hard-crash if missing).

## Install (test)

1. Build / install `LegacyMechanics-1.0.47.jar` (+ matching `AdaptiveDifficultyGUI-1.0.47.jar`).
2. Keep `enableProgression` / `enableFabledBridge` true (defaults).
3. **Disable** the CNPC Global Player scripts listed below to avoid double-sync / double TP.
4. Do **not** deploy to live until tested.

## Config flags (`config/legacymechanics.json`)

| Flag | Default | Module |
|------|---------|--------|
| `enableProgression` | true | Master for CNPC natural ports |
| `enableFlightProgression` | true | Flight / Viltrumite / Yardrat fly |
| `enableSprintJump` | true | Sprint + Jump invested STR |
| `enableMeditation` | true | Meditation |
| `enablePotential` | true | Potential Unlock |
| `enableFarmingTp` | true | Farming TP Skill |
| `enableGlobalTpBoost` | true | Global TP Boost / End TP boost |
| `enableBioAndroid` | true | Bio-Android absorb |
| `enableRaceLock` | true | DMZ Race Lock |
| `enableYardrat` | true | Yardrat race / skills |
| `enableSpiritualistKi` | true | Spiritualist Ki Control |
| `enableAndroidConversion` | true | Android conversion |
| `enableFabledBridge` | true | Master for Fabled soft bridges |
| `enableEnergyManaSync` | true | Energy ↔ Mana |
| `enableStatScreenSync` | true | Stat Screen → persistent values |
| `enableTpSpMirror` | true | TP ↔ SP |
| `enableAttrMultiBonus` | true | Attr multiplicative bonuses |
| `enablePrestigeSkillSync` | true | Prestige class → DMZ skill |
| `enablePrestigeFactionSync` | true | Prestige → CNPC faction 4 |
| `enableValueCleaner` | true | Persistent value cleaner |
| `enableRaceClassSync` | true | Race → Fabled class |
| `enableClassPermissionSync` | true | Class → LuckPerms `fabled.skill.*` |

## Java packages

| Package | Role |
|---------|------|
| `progression/` | `ProgressionSystem`, `ProgressionConfig`, `ProgressionData`, helpers |
| `progression/skills/` | Flight, SprintJump, Meditation, Potential |
| `progression/tp/` | FarmingTp, GlobalTpBoost, BioAndroidAbsorb |
| `progression/race/` | RaceLock, Yardrat, SpiritualistKi, AndroidConversion |
| `progression/combat/` | KiWeapons, Piercing, DOT, Apothic elemental |
| `progression/end/` | End Dimension Strength |
| `progression/shop/` | Prestige / Skill unlock |
| `progression/dummy/` | Shadow dummy limiter |
| `progression/bridge/` | **Fabled soft-dependency bridges** |

### Fabled bridge classes

| Class | CNPC source |
|-------|-------------|
| `FabledBridge` | Facade (availability, pulse, Bukkit/Fabled resolve) |
| `EnergyManaSync` | `DMZ Energy.js` |
| `StatScreenSync` | `DMZ Stat Screen.js` |
| `TpSpMirror` | `TP IS SP Fabled.js` |
| `AttrMultiBonus` | `Attr Fabled Multi bonus.js` |
| `PrestigeSkillSync` | `Prestige Sync Fabled.js` |
| `PrestigeFactionSync` | `Fabled Prestige Faction Sync.js` |
| `ValueCleaner` | `Universal Fabled Value Cleaner.js` |
| `RaceClassSync` | `Races.js` |
| `ClassPermissionSync` | `DMZ Class Permission.js` |

## CNPC scripts to DISABLE on test

### Fabled bridges (when `enableFabledBridge` is on)

- `DMZ Energy`
- `DMZ Stat Screen`
- `TP IS SP Fabled`
- `Attr Fabled Multi bonus`
- `Prestige Sync Fabled`
- `Fabled Prestige Faction Sync`
- `Universal Fabled Value Cleaner`
- `Races`
- `DMZ Class Permission`

### Natural progression (when `enableProgression` is on)

- `Fly` / `ViltrumiteFly` / `flight suppression` / `Flight` / `JumpSprint` / `Jump` / `Sprint`
- `Meditation new`
- `Potential`
- `Farming TP Skill`
- `Global TP Boost` / `TP boost end`
- `BioAndroid`
- `DMZ RACE LOCK`
- `YardratRace` / `YardratSkills`
- `Spirtualist Ki Control`
- `AndrioidConversion`
- `KiWeapons` / `Piercing` / `damageovertime` / `Apothicfireandcolddamage`
- `End Dimension Strength`
- `ShadowDummyLimiter` / related forge protect scripts
- Prestige NPC / SkillUnlockNPC (when shop ports are active)

### Already in Java (rival / spar)

- `Rival System`, `Sparring Tp System`, `Rival Command Handler`, `Sparring Command Handler`

## Chat / telemetry

- `/lm` hub → **[Progression]** → `/lm do page progression` · `fabled` · `disable`
- System telemetry (`enableSystemTelemetry`): rate-limited `system=fabled` events
  (`login_sync`, `energy_spend`, `tp_sp_spend`, `prestige_skill`, `prestige_faction`,
  `race_class`, `class_perm_grant`, `value_clean`)

## API notes

Fabled calls use `studio.magemonkey.fabled.Fabled` / `PlayerData` from the live jar
(`getData`, `getPoints`/`setPoints`, `getMana`/`setMana`, `getAttribute`,
`setPersistentData`, `setClass`, `getSkillLevel`, etc.).

**Guessed / reflection-only (not public API):**

- Writing `maxMana` via private field (no public `setMaxMana` on this Fabled build)
- LuckPerms node mutate via reflected `Node.builder` + `user.data().add/remove`
- CNPC faction points via `NpcAPI` / `PlayerData.get` + `PlayerFactionData`
