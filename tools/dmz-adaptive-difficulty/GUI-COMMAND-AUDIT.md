# GUI + command-tree audit (2.3.51)

Complete pass over LegacyMechanics Forge + LegacyMechanicsGUI: every GUI surface,
command tree, `/lmdo` bridge, inspect routing, and former CNPC script parity.

## Verdict

**Ship 2.3.51 (+ script parity 2.3.53).** Critical/HIGH navigation and permission bugs found in the audit
are fixed. Script-owned systems remain Java-owned (CNPC-free). Remaining notes
are MEDIUM/intentional. See `SCRIPT-AUDIT.md` for 2.3.53 Yardrat/RaceLock/ShadowDummy/TP-boost fixes.

---

## Systems matrix

| System | Commands | GUIs (chest / CMI / chat) | NPC open | Player? | Staff extras |
|--------|----------|---------------------------|----------|---------|--------------|
| Hub | `/lm`, `/legacymechanics` | HubChest / CmiHub / MechanicsChat | `lm_hub` | yes | logs, admin, progression |
| Difficulty | `/difficulty`, `/diff` | DifficultyChest / CmiDiff / DiffChat | `lm_difficulty` | yes | admin, details |
| Rival | `/rival` | RivalChest / CmiRival / RivalChat | `lm_rival` | yes | admin |
| Spar | `/spar` | SparChest / CmiSpar / SparChat | `lm_spar` | yes | admin |
| Prestige | `/prestige` | PrestigeChest / CmiPrestige / inventory fallback | `lm_prestige` | yes | — |
| Skill Check | `/skillcheck` | SkillsChest / CmiSkills / SkillsMenu | `lm_skillcheck` | donator | — |
| Skills admin | `/skills` | same Skills GUIs | hub | staff | unlock admin |
| Progression | `/progression`, `/prog` | ProgressionChest / CmiProg / ProgChat | hub staff | meditation + android remove | flags / boost / convert |
| End dragon | `/enddragon`, `/spawndragon`, `/cleardragons`, `/killdragons` | — | — | staff (`StaffAccess`) | spawn/clear |
| Bridge | `/lmdo …` | — | CMI clicks | internal | — |

Hub hops and CMI buttons must use **`/lmdo lm open <system>`** (not bare `/lm`)
so Mohist routing and staff inspect sessions stay coherent.

---

## Fixes in 2.3.51

### CRITICAL / HIGH

| Issue | Fix |
|-------|-----|
| Prestige chest used nonexistent `SlotAction.open` | `cmd("lmdo lm open progression")` |
| Bare `lm` hub buttons on nearly every GUI | → `lmdo lm open hub` (chest + CMI + chat) |
| `/enddragon*` Forge level-2 only (Bukkit OP fail) | `StaffAccess` / `ProgressionCommands::staff` |
| Forge `/progression` root staff-gated (blocked meditation/android) | Root open; staff on gui/do/boost/admin |
| `/skillcheck` `.requires` auto-granted Forge op2 | Requires `SkillCheckService.canUse` only |
| Chat hub hid Prestige + Remove Android | Player-facing prestige + android_remove |
| Prestige → bare `/progression` for players | Staff-only Progression; hub via lmdo |
| `lmdo` rival/spar ignored `guiBackend=chest` + broke inspect | RespectingConfig + inspect reopen |
| `openMenuRespectingConfig` cleared inspect | Keep inspect + `openAs` |
| `lmdo` progression/skills/skillcheck reopen ungated | Staff / skillcheck gates on reopen |
| Difficulty Forge `isStaff` missed Bukkit `isOp` | Align with `StaffAccess` |
| Vanilla `/difficulty hard|…` level-2 only | Same `isStaff` |
| Inspect hub → Remove Android unknown system | `android_remove` in `openInspectSystem` + `isKnownSystem` |
| Chat-backend Remove Android → staff ProgressionChat | Force inventory for `android_remove` page |
| Forge `lm open android_remove` instant-removed | Opens confirm GUI (matches Bukkit) |
| Bukkit bare `/progression` opened staff main GUI | Align with Forge `helpOrGui` |
| Hub reopen during inspect could paint CMI-as-self | `openHubRespectingConfig` keeps chest inspect |

### Script parity (queued follow-up)

Cross-checked against `SCRIPT-AUDIT.md` / `CNPC-FREE.md` / uploads backups:

| Former script | Status |
|---------------|--------|
| Rival / Spar / Prestige / SkillCheck | OK |
| Meditation / Android convert+remove / End dragon / Portal guard | OK |
| Flight / SprintJump / Potential / Farming / Building / Race / Fabled / ShadowDummy / StatChecker | OK |
| Global TP Boost | OK (in-memory window intentional) |

No script re-enable required. Ops checklist remains: NPC tags + delete CMI
`enddragon` / `noppes script trigger` aliases.

---

## Command ownership (Mohist)

Bukkit plugin owns: `/lm`, `/rival`, `/spar`, `/difficulty`, `/progression`,
`/prestige`, `/skills`, `/skillcheck`, `/lmdo`.

Forge still registers brigadier trees (fallback / non-Mohist) and **owns**
`/enddragon*` (no Bukkit duplicate).

CMI inventory clicks → `/lmdo <system> <action> …` → `ForgeBridge.*HandleDo` + reopen.

---

## Known remaining (MEDIUM / intentional)

- Difficulty `team` page is WIP placeholder.
- Hub **Admin** tile opens chat help (not a full admin chest).
- CMI GUIs are not inspect-aware by design — inspect forces chest.
- Chat menus still use `/rival do` / `/spar do` / `/difficulty do` in places
  (Bukkit owns those names on Mohist; CMI path prefers lmdo).
- `RivalCommands` private dead helpers (`tpmsgToggle`, `instinctToggle`, `help`) unused.
- CMI `rival_title` usermeta sync not ported (documented).

---

## Ops verify checklist

1. Install matching `LegacyMechanics-2.3.51.jar` + `LegacyMechanicsGUI-2.3.51.jar`.
2. `/lm` → Difficulty / Rival / Spar / Prestige / Remove Android all open.
3. Hub ← from each system returns to hub (inspect session preserved for staff).
4. Non-staff: `/progression` help text; `/progression meditation`; `/progression android remove`.
5. Staff: `/progression gui`; `/enddragon` with Bukkit OP (no Forge level-2 required).
6. Donator: `/skillcheck` / NPC `lm_skillcheck`; staff without node sees Skills not Skill Check.
7. `guiBackend=chat|chest|cmi`: Remove Android still opens confirm inventory.
