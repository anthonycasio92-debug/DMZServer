# Race / class apply audit

## Flow matrix (intended)

| Flow | Snapshot timing | Class apply | Sync |
|------|-----------------|-------------|------|
| Paid class change | Before commit | `/dmzclass` via `onPaidClassChange` | Stats + Fabled perms |
| Race auto-remap | Before race+class commit | `onServicesRaceChangeApplied` | dmzclass + transform limits + appearance |
| Race + class picker | At race change + UpdateCharacter HEAD | On recustomize complete | same |
| 0% race wipe | CreateCharacter HEAD | `onCharacterCreated` → race hook | same |

## DMZ reference (`/dmzclass`)

- `isValidClass` → `getAllClasses().contains(lowercase)`
- `snapshotMultiplierResources` → `setCharacterClass` → `restoreMultiplierGains` → `StatsSyncS2C`

## Static checks

**PASS** — wiring and mapper sim OK.
