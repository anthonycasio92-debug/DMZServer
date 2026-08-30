# Fabled Prestige skill purchase fix

## Problem
Players with **Ancient Saiyan** could not buy **Permanent Majin** / **Permanent Mutant**.

Root causes:
1. **GUI slot collision** — `gui.yml` `stPrestige` put Permanent Majin on page-1 slot 4 and Ancient Saiyan on page-2 slot 4. Same slot index blocked purchasing Majin after Ancient.
2. **Class level gate** — Majin/Mutant had `level-base: 3` while Ancient unlocks at class level 1 (with LP), so Ancient holders below Prestige class level 3 could not buy them.

## Intended rules (up to 3 of 4)
| Skill | Exclusive with | Combines with |
|-------|----------------|---------------|
| Permanent Majin | Permanent Mutant only | Ancient Saiyan + Sento Saiyan |
| Permanent Mutant | Permanent Majin only | Ancient Saiyan + Sento Saiyan |
| Ancient Saiyan | (none of these four) | Sento + Majin **or** Mutant |
| Sento Saiyan | (none of these four) | Ancient + Majin **or** Mutant |

Players may hold **Ancient + Sento + (Majin XOR Mutant)** at once.

## Files changed
- `plugins/Fabled/gui.yml` — unique Prestige tree slots (single page)
- `plugins/Fabled/dynamic/skill/Permanent Majin.yml` — `level-base: 1`, incompat Mutant only
- `plugins/Fabled/dynamic/skill/Permanent Mutant.yml` — `level-base: 1`, incompat Majin only
- `plugins/Fabled/dynamic/skill/Ancient Saiyan.yml` — `incompatible: []`
- `plugins/Fabled/dynamic/skill/Sento Saiyan.yml` — `incompatible: []`

Also patch live `plugins/Fabled/dynamic/skills.yml` to match (Fabled may load the aggregate).

## Reload
```
/class reload
```
or restart. Players should reopen the Prestige skill tree.
